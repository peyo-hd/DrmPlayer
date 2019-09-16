package com.peyo.drmplayer

import android.net.Uri
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer

class Media2PlayActivity : MediaPlayActivity() {

    lateinit var mMedia2Player : MediaPlayer

    override fun playVideo() {
        mMedia2Player = MediaPlayer(this)
        with (mMedia2Player) {
            setMediaItem(UriMediaItem.Builder(Uri.parse(mediaUrl)).build())
            setSurface(mSurfaceHolder.surface)
            prepare()
            play()
        }
    }

    override fun stopVideo() {
        mMedia2Player.close()
    }
}
