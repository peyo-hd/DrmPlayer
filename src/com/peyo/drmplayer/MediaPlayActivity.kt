package com.peyo.drmplayer

import android.app.Activity
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import kotlinx.android.synthetic.main.main.*

open class MediaPlayActivity : Activity(), SurfaceHolder.Callback {

    protected lateinit var mSurfaceHolder: SurfaceHolder
    protected lateinit var mMediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        mSurfaceHolder = surface.holder
        mSurfaceHolder.addCallback(this)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }

    open fun playVideo() {
        mMediaPlayer = MediaPlayer()
        try {
            setDataSource(mMediaPlayer)
            with (mMediaPlayer) {
                setDisplay(mSurfaceHolder)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val mediaUrl = "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review.mp4"

    @Throws(Exception::class)
    protected fun setDataSource(player: MediaPlayer) {
        player.setDataSource(mediaUrl)
    }

    override fun surfaceCreated(arg0: SurfaceHolder) {
        playVideo()
    }

    override fun surfaceChanged(arg0: SurfaceHolder, arg1: Int, arg2: Int, arg3: Int) {}

    override fun surfaceDestroyed(arg0: SurfaceHolder) {}

    open fun stopVideo() {
        mMediaPlayer.release()
    }
    override fun onPause() {
        stopVideo()
        super.onPause()
    }
}
