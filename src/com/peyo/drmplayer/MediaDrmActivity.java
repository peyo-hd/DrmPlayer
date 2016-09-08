package com.peyo.drmplayer;

import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.Vector;

public class MediaDrmActivity extends MediaCodecActivity {
    private static final String TAG = "MediaDrmActivity";

    private static final byte[] CLEAR_KEY_CENC =
            { 0x1a, (byte)0x8a, 0x20, (byte)0x95, (byte)0xe4, (byte)0xde, (byte)0xb2, (byte)0xd2,
                    (byte)0x9e, (byte)0xc8, 0x16, (byte)0xac, 0x7b, (byte)0xae, 0x20, (byte)0x82 };

    public static final UUID CLEARKEY_SCHEME_UUID =
            new UUID(0x1077efecc0b24d02L, 0xace33c1e52e2fb4bL);

    private MediaDrm mDrm;
    private byte[][] mClearKeys;
    private byte[] mDrmInitData;
    private byte[] mSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClearKeys = new byte[][]{ CLEAR_KEY_CENC };

        try {
            mDrm = new MediaDrm(CLEARKEY_SCHEME_UUID);
            mDrm.setOnEventListener(new MediaDrm.OnEventListener() {
                @Override
                public void onEvent(MediaDrm md, byte[] sessionId, int event,
                                    int extra, byte[] data) {
                    if (event == MediaDrm.EVENT_KEY_REQUIRED) {
                        Log.i(TAG, "MediaDrm event: Key required");
                        getKeys(mDrm, "cenc", mSessionId, mDrmInitData, mClearKeys);
                    } else if (event == MediaDrm.EVENT_KEY_EXPIRED) {
                        Log.i(TAG, "MediaDrm event: Key expired");
                        getKeys(mDrm, "cenc", mSessionId, mDrmInitData, mClearKeys);
                    } else {
                        Log.e(TAG, "Events not supported" + event);
                    }
                }
            });
        } catch (MediaDrmException e) {
            Log.e(TAG, "Failed to create MediaDrm: " + e.getMessage());
            finish();
        }

        if (!mDrm.isCryptoSchemeSupported(CLEARKEY_SCHEME_UUID)) {
            mDrm.release();
            Log.e(TAG, "Crypto scheme is not supported.");
            finish();
        }

        boolean mRetryOpen;
        do {
            try {
                mRetryOpen = false;
                mSessionId = mDrm.openSession();
            } catch (Exception e) {
                mRetryOpen = true;
            }
        } while (mRetryOpen);
    }

    @Override
    public void playVideo() {
        try {
            mMediaCodecPlayer = new MediaDrmPlayer(mSessionId);
            mMediaCodecPlayer.setDisplay(mSurfaceHolder);
            setDataSource(mMediaCodecPlayer);
            mMediaCodecPlayer.prepare();
            mDrmInitData = ((MediaDrmPlayer)mMediaCodecPlayer).getDrmInitData();
            getKeys(mDrm, "cenc", mSessionId, mDrmInitData, mClearKeys);

            mMediaCodecPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void releaseMediaPlayer() {
        if (mMediaCodecPlayer != null) {
            mMediaCodecPlayer.release();
            mMediaCodecPlayer = null;
            mDrm.closeSession(mSessionId);
            mDrm.release();
        }
    }

    @Override
    protected void setDataSource(MediaCodecPlayer player) throws Exception {
        player.setDataSource("http://yt-dash-mse-test.commondatastorage.googleapis.com/media/car_cenc-20120827-88.mp4");
    }

    /**
     * Retrieves clear key ids from getKeyRequest(), create JSON Web Key
     * set and send it to the CDM via provideKeyResponse().
     */
    private void getKeys(MediaDrm drm, String initDataType,
                         byte[] sessionId, byte[] drmInitData, byte[][] clearKeys) {
        MediaDrm.KeyRequest drmRequest = null;;
        try {
            drmRequest = drm.getKeyRequest(sessionId, drmInitData, initDataType,
                    MediaDrm.KEY_TYPE_STREAMING, null);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Failed to get key request: " + e.toString());
        }
        if (drmRequest == null) {
            Log.e(TAG, "Failed getKeyRequest");
            return;
        }

        Vector<String> keyIds = new Vector<String>();
        if (0 == getKeyIds(drmRequest.getData(), keyIds)) {
            Log.e(TAG, "No key ids found in initData");
            return;
        }

        if (clearKeys.length != keyIds.size()) {
            Log.e(TAG, "Mismatch number of key ids and keys: ids=" +
                    keyIds.size() + ", keys=" + clearKeys.length);
            return;
        }

        // Base64 encodes clearkeys. Keys are known to the application.
        Vector<String> keys = new Vector<String>();
        for (int i = 0; i < clearKeys.length; ++i) {
            String clearKey = Base64.encodeToString(clearKeys[i],
                    Base64.NO_PADDING | Base64.NO_WRAP);
            keys.add(clearKey);
        }

        String jwkSet = createJsonWebKeySet(keyIds, keys);
        byte[] jsonResponse = jwkSet.getBytes(Charset.forName("UTF-8"));

        try {
            try {
                drm.provideKeyResponse(sessionId, jsonResponse);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to provide key response: " + e.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to provide key response: " + e.toString());
        }
    }

    /**
     * Creates the JSON Web Key string.
     *
     * @return JSON Web Key string.
     */
    private String createJsonWebKeySet(Vector<String> keyIds, Vector<String> keys) {
        String jwkSet = "{\"keys\":[";
        for (int i = 0; i < keyIds.size(); ++i) {
            String id = new String(keyIds.get(i).getBytes(Charset.forName("UTF-8")));
            String key = new String(keys.get(i).getBytes(Charset.forName("UTF-8")));

            jwkSet += "{\"kty\":\"oct\",\"kid\":\"" + id +
                    "\",\"k\":\"" + key + "\"}";
        }
        jwkSet += "]}";
        return jwkSet;
    }

    /**
     * Extracts key ids from the pssh blob returned by getKeyRequest() and
     * places it in keyIds.
     * keyRequestBlob format (section 5.1.3.1):
     * https://dvcs.w3.org/hg/html-media/raw-file/default/encrypted-media/encrypted-media.html#clear-key
     *
     * @return size of keyIds vector that contains the key ids, 0 for error
     */
    private int getKeyIds(byte[] keyRequestBlob, Vector<String> keyIds) {
        if (0 == keyRequestBlob.length || keyIds == null)
            return 0;

        String jsonLicenseRequest = new String(keyRequestBlob);
        keyIds.clear();

        try {
            JSONObject license = new JSONObject(jsonLicenseRequest);
            final JSONArray ids = license.getJSONArray("kids");
            for (int i = 0; i < ids.length(); ++i) {
                keyIds.add(ids.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Invalid JSON license = " + jsonLicenseRequest);
            return 0;
        }
        return keyIds.size();
    }
}
