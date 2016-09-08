package com.peyo.drmplayer

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.util.*

class CodecState(
        private val mMediaTimeProvider: MediaTimeProvider,
        private val mExtractor: MediaExtractor,
        private val mTrackIndex: Int,
        private val mFormat: MediaFormat,
        private val mCodec: MediaCodec) {

    private var mSawInputEOS: Boolean = false
    private var mSawOutputEOS: Boolean = false

    private val mOutputBufferIndices: LinkedList<Int>
    private val mOutputBufferInfos: LinkedList<MediaCodec.BufferInfo>

    interface MediaTimeProvider {
        fun getNowUs() : Long
        fun getRealTimeUsForMediaTime(mediaTimeUs: Long): Long
    }

    init {
        mSawOutputEOS = false
        mSawInputEOS = mSawOutputEOS

        mOutputBufferIndices = LinkedList()
        mOutputBufferInfos = LinkedList()

        val mime = mFormat.getString(MediaFormat.KEY_MIME)
        Log.d(TAG, "CodecState() " + mime!!)
    }


    fun release() {
        mCodec.stop()

        mOutputBufferIndices.clear()
        mOutputBufferInfos.clear()

        mCodec.release()
    }

    fun start() {
        mCodec.start()
    }

    fun doSomeWork() {
        while (feedInputBuffer()) {
        }

        val info = MediaCodec.BufferInfo()
        val index = mCodec.dequeueOutputBuffer(info, 0)
        if (index >= 0) {
            mOutputBufferIndices.add(index)
            mOutputBufferInfos.add(info)
        }
        while (drainOutputBuffer()) {
        }
    }

    private fun feedInputBuffer(): Boolean {
        if (mSawInputEOS) {
            return false
        }

        val index = mCodec.dequeueInputBuffer(0)
        if (index < 0) {
            return false
        }

        val codecData = mCodec.getInputBuffer(index)
        val trackIndex = mExtractor.sampleTrackIndex

        if (trackIndex == mTrackIndex) {
            val sampleSize = mExtractor.readSampleData(codecData!!, 0)
            val sampleTime = mExtractor.sampleTime
            val sampleFlags = mExtractor.sampleFlags
            if (sampleSize <= 0) {
                Log.d(TAG, "sampleSize: " + sampleSize + " trackIndex:" + trackIndex +
                        " sampleTime:" + sampleTime + " sampleFlags:" + sampleFlags)
                mSawInputEOS = true
                return false
            }
            if (sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED != 0) {
                val info = MediaCodec.CryptoInfo()
                mExtractor.getSampleCryptoInfo(info)

                mCodec.queueSecureInputBuffer(
                        index, 0 /* offset */, info, sampleTime, 0 /* flags */)
            } else {
                mCodec.queueInputBuffer(
                        index, 0 /* offset */, sampleSize, sampleTime, 0)
            }
            mExtractor.advance()
            return true
        } else if (trackIndex < 0) {
            Log.d(TAG, "saw input EOS on track $mTrackIndex")
            mSawInputEOS = true
            mCodec.queueInputBuffer(
                    index, 0 /* offset */, 0 /* sampleSize */,
                    0 /* sampleTime */, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        return false
    }

    private fun drainOutputBuffer(): Boolean {
        if (mSawOutputEOS || mOutputBufferIndices.isEmpty()) {
            return false
        }

        val index = mOutputBufferIndices.peekFirst()!!.toInt()
        val info = mOutputBufferInfos.peekFirst()

        if (info!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            Log.d(TAG, "saw output EOS on track $mTrackIndex")
            mSawOutputEOS = true
            return false
        }

        val realTimeUs = mMediaTimeProvider.getRealTimeUsForMediaTime(info.presentationTimeUs)
        val nowUs = mMediaTimeProvider.getNowUs()
        val lateUs = nowUs - realTimeUs
        val render: Boolean
        if (lateUs < -45000) {
            // too early;
            return false
        } else if (lateUs > 30000) {
            Log.d(TAG, "video late by $lateUs us.")
            render = false
        } else {
            render = true
        }

        mCodec.releaseOutputBuffer(index, render)
        mOutputBufferIndices.removeFirst()
        mOutputBufferInfos.removeFirst()
        return true
    }

    companion object {
        private val TAG = "CodecState"
    }

}
