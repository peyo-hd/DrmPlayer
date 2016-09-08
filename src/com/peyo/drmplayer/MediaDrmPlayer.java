package com.peyo.drmplayer;

import android.annotation.TargetApi;
import android.media.DrmInitData;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaCrypto;
import android.media.MediaCryptoException;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;

public class MediaDrmPlayer extends MediaCodecPlayer {
    private static final String TAG = "MediaDrmPlayer";

    private MediaCrypto mCrypto;

    public MediaDrmPlayer(byte[] sessionId) throws MediaCryptoException {
        mCrypto = new MediaCrypto(MediaDrmActivity.CLEARKEY_SCHEME_UUID, sessionId);
    }

    @Override
    protected void addTrack(int trackIndex, MediaFormat format) throws IOException {
        String mime = format.getString(MediaFormat.KEY_MIME);
        MediaCodec codec;

        if (mCrypto.requiresSecureDecoderComponent(mime)) {
            codec = MediaCodec.createByCodecName(
                    getSecureDecoderNameForMime(mime));
        } else {
            codec = MediaCodec.createDecoderByType(mime);
        }

        codec.configure(
                format,
                mSurfaceHolder.getSurface(),
                mCrypto, 0);

        CodecState state = new CodecState(this, getExtractor(),
                trackIndex, format, codec);
        getCodecStates().put(Integer.valueOf(trackIndex), state);
    }

    public void release() {
        super.release();
        if (mCrypto != null) {
            mCrypto.release();
            mCrypto = null;
        }
    }

    protected String getSecureDecoderNameForMime(String mime) {
        MediaCodecInfo[] infos = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
        for(MediaCodecInfo info: infos) {
            if (info.isEncoder()) {
                continue;
            }
            String[] supportedTypes = info.getSupportedTypes();
            for (int j = 0; j < supportedTypes.length; ++j) {
                if (supportedTypes[j].equalsIgnoreCase(mime)) {
                    return info.getName() + ".secure";
                }
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public final byte[] getDrmInitData() {
        for (MediaExtractor ex : new MediaExtractor[]{getExtractor()}) {
            DrmInitData drmInitData = ex.getDrmInitData();
            if (drmInitData != null) {
                DrmInitData.SchemeInitData schemeInitData = drmInitData.get(MediaDrmActivity.CLEARKEY_SCHEME_UUID);
                if (schemeInitData != null && schemeInitData.data != null) {
                    return schemeInitData.data;
                }
            }
        }
        // TODO
        // Should not happen after we get content that has the clear key system id.
        return PSSH;
    }

    private static final byte[] PSSH = hexStringToByteArray(
            "0000003470737368" +  // BMFF box header (4 bytes size + 'pssh')
                    "01000000" +          // Full box header (version = 1 flags = 0)
                    "1077efecc0b24d02" +  // SystemID
                    "ace33c1e52e2fb4b" +
                    "00000001" +          // Number of key ids
                    "60061e017e477e87" +  // Key id
                    "7e57d00d1ed00d1e" +
                    "00000000"            // Size of Data, must be zero
    );

    /**
     * Convert a hex string into byte array.
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
