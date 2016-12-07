package com.peyo.drmplayer;

import android.net.Uri;
import android.os.Bundle;

import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;

public class PlayReadyActivity extends ExoPlayActivity {
    private HttpDataSource.Factory mDataSourceFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mDataSourceFactory = new DefaultHttpDataSourceFactory("PlayReadyActivity");
            mDrmSessionManager = DefaultDrmSessionManager.newPlayReadyInstance(
                    new HttpMediaDrmCallback(null, mDataSourceFactory),
                    null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private String URL_SS = "http://xxx";

    protected MediaSource setDataSource() {
        return new SsMediaSource(Uri.parse(URL_SS), mDataSourceFactory,
                new DefaultSsChunkSource.Factory(mDataSourceFactory), null, null);
    }
}
