package com.peyo.drmplayer;

import android.net.Uri;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

public class ExoPlayActivity extends MediaPlayActivity {

    protected SimpleExoPlayer mExoPlayer;
    protected DrmSessionManager mDrmSessionManager = null;

    @Override
    protected void playVideo() {
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(this,
                new DefaultTrackSelector(null),
                new DefaultLoadControl(),
                mDrmSessionManager);
        mExoPlayer.setVideoSurfaceHolder(mSurfaceHolder);
        MediaSource source = setDataSource();
        mExoPlayer.prepare(source);
        mExoPlayer.setPlayWhenReady(true);
    }

    private String URL_MP4 = "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review.mp4";

    protected MediaSource setDataSource() {
        return new ExtractorMediaSource(Uri.parse(URL_MP4),
                new DefaultDataSourceFactory(this,"ExoPlayActivity"),
                new DefaultExtractorsFactory(), null, null);
    }

    @Override
    protected void releaseMediaPlayer() {
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }
}
