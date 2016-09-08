package com.peyo.drmplayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class CodecState {
    private static final String TAG = "CodecState";

    private MediaCodec mCodec;
    private MediaExtractor mExtractor;
    private MediaFormat mFormat;

    private boolean mSawInputEOS, mSawOutputEOS;

    private LinkedList<Integer> mOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> mOutputBufferInfos;

    private int mTrackIndex;
    private MediaTimeProvider mMediaTimeProvider;

    public interface MediaTimeProvider {
        long getNowUs();
        long getRealTimeUsForMediaTime(long mediaTimeUs);
    }

    public CodecState(
            MediaTimeProvider mediaTimeProvider,
            MediaExtractor extractor,
            int trackIndex,
            MediaFormat format,
            MediaCodec codec) {
        mMediaTimeProvider = mediaTimeProvider;
        mExtractor = extractor;
        mTrackIndex = trackIndex;
        mFormat = format;
        mSawInputEOS = mSawOutputEOS = false;
        mCodec = codec;

        mOutputBufferIndices = new LinkedList<Integer>();
        mOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();

        String mime = mFormat.getString(MediaFormat.KEY_MIME);
        Log.d(TAG, "CodecState() " + mime);
    }


    public void release() {
        mCodec.stop();

        mOutputBufferIndices.clear();
        mOutputBufferInfos.clear();

        mOutputBufferIndices = null;
        mOutputBufferInfos = null;

        mCodec.release();
        mCodec = null;
    }

    public void start() {
        mCodec.start();
    }

    public void doSomeWork() {
        while (feedInputBuffer()) {
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int index = mCodec.dequeueOutputBuffer(info, 0);
        if (index >= 0) {
            mOutputBufferIndices.add(index);
            mOutputBufferInfos.add(info);
        }
        while (drainOutputBuffer()) {
        }
    }

    private boolean feedInputBuffer() {
        if (mSawInputEOS) {
            return false;
        }

        int index = mCodec.dequeueInputBuffer(0);
        if (index < 0) {
            return false;
        }

        ByteBuffer codecData = mCodec.getInputBuffer(index);
        int trackIndex = mExtractor.getSampleTrackIndex();

        if (trackIndex == mTrackIndex) {
            int sampleSize =
                    mExtractor.readSampleData(codecData, 0);
            long sampleTime = mExtractor.getSampleTime();
            int sampleFlags = mExtractor.getSampleFlags();
            if (sampleSize <= 0) {
                Log.d(TAG, "sampleSize: " + sampleSize + " trackIndex:" + trackIndex +
                        " sampleTime:" + sampleTime + " sampleFlags:" + sampleFlags);
                mSawInputEOS = true;
                return false;
            }
            if ((sampleFlags & MediaExtractor.SAMPLE_FLAG_ENCRYPTED) != 0) {
                MediaCodec.CryptoInfo info = new MediaCodec.CryptoInfo();
                mExtractor.getSampleCryptoInfo(info);

                mCodec.queueSecureInputBuffer(
                        index, 0 /* offset */, info, sampleTime, 0 /* flags */);
            } else {
                mCodec.queueInputBuffer(
                        index, 0 /* offset */, sampleSize, sampleTime, 0);
            }
            mExtractor.advance();
            return true;
        } else if (trackIndex < 0) {
            Log.d(TAG, "saw input EOS on track " + mTrackIndex);
            mSawInputEOS = true;
            mCodec.queueInputBuffer(
                    index, 0 /* offset */, 0 /* sampleSize */,
                    0 /* sampleTime */, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }
        return false;
    }

    private boolean drainOutputBuffer() {
        if (mSawOutputEOS || mOutputBufferIndices.isEmpty()) {
            return false;
        }

        int index = mOutputBufferIndices.peekFirst().intValue();
        MediaCodec.BufferInfo info = mOutputBufferInfos.peekFirst();

        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d(TAG, "saw output EOS on track " + mTrackIndex);
            mSawOutputEOS = true;
            return false;
        }

        long realTimeUs =
                mMediaTimeProvider.getRealTimeUsForMediaTime(info.presentationTimeUs);
        long nowUs = mMediaTimeProvider.getNowUs();
        long lateUs = nowUs - realTimeUs;
        boolean render;
        if (lateUs < -45000) {
            // too early;
            return false;
        } else if (lateUs > 30000) {
            Log.d(TAG, "video late by " + lateUs + " us.");
            render = false;
        } else {
            render = true;
        }

        mCodec.releaseOutputBuffer(index, render);
        mOutputBufferIndices.removeFirst();
        mOutputBufferInfos.removeFirst();
        return true;
    }

}
