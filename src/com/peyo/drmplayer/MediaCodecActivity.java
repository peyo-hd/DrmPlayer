package com.peyo.drmplayer;

public class MediaCodecActivity extends MediaPlayActivity{

	protected MediaCodecPlayer mMediaCodecPlayer;

        @Override
	protected void playVideo() {
        mMediaCodecPlayer = new MediaCodecPlayer();
        try {
	        mMediaCodecPlayer.setDisplay(mSurfaceHolder);
	        setDataSource(mMediaCodecPlayer);
	        mMediaCodecPlayer.prepare();
	        mMediaCodecPlayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
        }

	protected void setDataSource(MediaCodecPlayer player) throws Exception {
		player.setDataSource("http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review.mp4");
	}

    @Override
    protected void releaseMediaPlayer() {
        if (mMediaCodecPlayer != null) {
            mMediaCodecPlayer.release();
            mMediaCodecPlayer = null;
        }
    }
}
