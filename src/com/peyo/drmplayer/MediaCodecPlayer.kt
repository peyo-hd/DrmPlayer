package com.peyo.drmplayer

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.SurfaceHolder
import java.io.IOException
import java.util.HashMap

open class MediaCodecPlayer : CodecState.MediaTimeProvider {
    private var mCodecStates: MutableMap<Int, CodecState>? = null
    private val mThread: Thread
    private var mThreadStarted: Boolean = false
    private var mDeltaTimeUs: Long = 0
    private var mState: Int = 0
    private val mExtractor: MediaExtractor
    protected lateinit var mSurfaceHolder: SurfaceHolder

    init {
        mState = STATE_IDLE
        mThread = Thread(Runnable {
            while (mThreadStarted == true) {
                doSomework()
            }
            try {
                Thread.sleep(5)
            } catch (e: InterruptedException) {
                Log.d(TAG, "Thread interrupted")
            }
        })
        mExtractor = MediaExtractor()
    }

    fun getExtractor() : MediaExtractor {
        return mExtractor
    }

    fun getCodecStates() : MutableMap<Int, CodecState>? {
        return mCodecStates
    }

    @Throws(Exception::class)
    fun setDataSource(path: String) {
        mExtractor.setDataSource(path)
    }

    fun setDisplay(sh: SurfaceHolder) {
        mSurfaceHolder = sh
    }

    @Throws(Exception::class)
    fun prepare() {
        if (null == mCodecStates) {
            mCodecStates = HashMap()
        } else {
            mCodecStates!!.clear()
        }

        var i = mExtractor.trackCount
        while (i-- > 0) {
            val format = mExtractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (!mime!!.startsWith("video/")) {
                continue
            }
            mExtractor.selectTrack(i)
            addTrack(i, format)
            break
        }

        mState = STATE_PAUSED
    }

    @Throws(IOException::class)
    protected open fun addTrack(trackIndex: Int, format: MediaFormat) {
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(
                format,
                mSurfaceHolder.surface, null, 0)

        val state = CodecState(this, mExtractor,
                trackIndex, format, codec)
        mCodecStates!![Integer.valueOf(trackIndex)] = state
    }

    fun start() {
        if (mState == STATE_PLAYING || mState == STATE_PREPARING) {
            return
        } else if (mState == STATE_IDLE) {
            mState = STATE_PREPARING
            return
        } else check(mState == STATE_PAUSED)

        for (state in mCodecStates!!.values) {
            state.start()
        }

        mDeltaTimeUs = -1
        mState = STATE_PLAYING

        mThreadStarted = true
        mThread.start()
    }

    fun pause() {
        Log.d(TAG, "pause")

        if (mState == STATE_PAUSED) {
            return
        } else check(mState == STATE_PLAYING)

        mState = STATE_PAUSED
    }

    open fun release() {
        if (mState == STATE_PLAYING) {
            mThreadStarted = false

            try {
                mThread.join()
            } catch (ex: InterruptedException) {
                Log.d(TAG, "mThread.join $ex")
            }

            pause()
        }

        if (mCodecStates != null) {
            for (state in mCodecStates!!.values) {
                state.release()
            }
            mCodecStates = null
        }

        mExtractor.release()
        mState = STATE_IDLE
    }

    private fun doSomework() {
        for (state in mCodecStates!!.values) {
            state.doSomeWork()
        }
    }

    override fun getNowUs(): Long {
        return System.currentTimeMillis() * 1000
    }

    override fun getRealTimeUsForMediaTime(mediaTimeUs: Long): Long {
        if (mDeltaTimeUs == -1L) {
            val nowUs = getNowUs()
            mDeltaTimeUs = nowUs - mediaTimeUs
        }

        return mDeltaTimeUs + mediaTimeUs
    }

    companion object {

        private val TAG = "MediaCodecPlayer"

        private val STATE_IDLE = 1
        private val STATE_PREPARING = 2
        private val STATE_PLAYING = 3
        private val STATE_PAUSED = 4
    }

}
