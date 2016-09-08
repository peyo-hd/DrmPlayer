package com.peyo.drmplayer

class MediaCodecActivity : MediaPlayActivity() {

    protected lateinit var mMediaCodecPlayer: MediaCodecPlayer

    override fun playVideo() {
        mMediaCodecPlayer = MediaCodecPlayer()
        try {
            setDataSource(mMediaCodecPlayer)
            with(mMediaCodecPlayer) {
                setDisplay(mSurfaceHolder)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    protected fun setDataSource(player: MediaCodecPlayer) {
        player.setDataSource(mediaUrl)
    }

    override fun stopVideo() {
         mMediaCodecPlayer.release()
    }
}