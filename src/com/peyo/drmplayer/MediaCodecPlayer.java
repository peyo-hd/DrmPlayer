package com.peyo.drmplayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MediaCodecPlayer implements CodecState.MediaTimeProvider {

    private static final String TAG = "MediaCodecPlayer";

    private static final int STATE_IDLE = 1;
    private static final int STATE_PREPARING = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;


    protected Map<Integer, CodecState> mCodecStates;
    private Thread mThread;
    private boolean mThreadStarted;
    private long mDeltaTimeUs;
    private int mState;
    protected MediaExtractor mExtractor;
    protected SurfaceHolder mSurfaceHolder;


    public MediaCodecPlayer() {
        mState = STATE_IDLE;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
            while(mThreadStarted == true) {
                doSomework();
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread interrupted");
            }
            }
        });
        mExtractor = new MediaExtractor();
    }

    public void setDataSource(String path)  throws Exception {
        mExtractor.setDataSource(path);
    }

    public void setDisplay(SurfaceHolder sh) {
        mSurfaceHolder = sh;
    }

    public void prepare() throws Exception {
        if (null == mCodecStates) {
            mCodecStates = new HashMap<Integer, CodecState>();
        } else {
            mCodecStates.clear();
        }

        for (int i = mExtractor.getTrackCount(); i-- > 0;) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (!mime.startsWith("video/")) {
                continue;
            }
            mExtractor.selectTrack(i);
            addTrack(i, format);
            break;
        }

        mState = STATE_PAUSED;
    }

    protected void addTrack(int trackIndex, MediaFormat format) throws IOException {
        MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        codec.configure(
                format,
                mSurfaceHolder.getSurface(),
                null, 0);

        CodecState state = new CodecState(this, mExtractor,
                    trackIndex, format, codec);
        mCodecStates.put(Integer.valueOf(trackIndex), state);
    }

    public void start() {
        if (mState == STATE_PLAYING || mState == STATE_PREPARING) {
            return;
        } else if (mState == STATE_IDLE) {
            mState = STATE_PREPARING;
            return;
        } else if (mState != STATE_PAUSED) {
            throw new IllegalStateException();
        }

        for (CodecState state : mCodecStates.values()) {
            state.start();
        }

        mDeltaTimeUs = -1;
        mState = STATE_PLAYING;

        mThreadStarted = true;
        mThread.start();
    }

    public void pause() {
        Log.d(TAG, "pause");

        if (mState == STATE_PAUSED) {
            return;
        } else if (mState != STATE_PLAYING) {
            throw new IllegalStateException();
        }

        mState = STATE_PAUSED;
    }

    public void release() {
        if (mState == STATE_PLAYING) {
            mThreadStarted = false;

            try {
                mThread.join();
            } catch (InterruptedException ex) {
                Log.d(TAG, "mThread.join " + ex);
            }

            pause();
        }

        if (mCodecStates != null) {
            for (CodecState state : mCodecStates.values()) {
                state.release();
            }
            mCodecStates = null;
        }

        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }

        mState = STATE_IDLE;
    }

    private void doSomework() {
        for(CodecState state : mCodecStates.values()) {
            state.doSomeWork();
        }
    }

    @Override
    public long getNowUs() {
        return System.currentTimeMillis() * 1000;
    }

    @Override
    public long getRealTimeUsForMediaTime(long mediaTimeUs) {
        if (mDeltaTimeUs == -1) {
            long nowUs = getNowUs();
            mDeltaTimeUs = nowUs - mediaTimeUs;
        }

        return mDeltaTimeUs + mediaTimeUs;
    }

}
