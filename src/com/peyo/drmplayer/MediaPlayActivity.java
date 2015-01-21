package com.peyo.drmplayer;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class MediaPlayActivity extends Activity implements Callback {

	private SurfaceView mSurfaceView;
	protected SurfaceHolder mSurfaceHolder;
	protected MediaPlayer mMediaPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	protected void playVideo() {
        mMediaPlayer = new MediaPlayer();
        try {
	        mMediaPlayer.setDisplay(mSurfaceHolder);
	        setDataSource(mMediaPlayer);
	        mMediaPlayer.prepare();
	        mMediaPlayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setDataSource(MediaPlayer player) throws Exception {
		player.setDataSource("http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review.mp4");
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		playVideo();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {}

    @Override
    protected void onPause() {
        releaseMediaPlayer();
		super.onPause();
    }

    @Override
    protected void onDestroy() {
        releaseMediaPlayer();
		super.onDestroy();
    }

    protected void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
