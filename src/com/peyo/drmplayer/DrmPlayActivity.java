package com.peyo.drmplayer;

import android.net.Uri;
import android.os.Bundle;

import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;

public class DrmPlayActivity extends ExoPlayActivity {

    private HttpDataSource.Factory mDataSourceFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mDataSourceFactory = new DefaultHttpDataSourceFactory("DrmPlayActivity");
            mDrmSessionManager = StreamingDrmSessionManager.newWidevineInstance(
                    new HttpMediaDrmCallback(WIDEVINE_LICENSE, mDataSourceFactory),
                    null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private String URL_DASH = "https://xxx";
    private String WIDEVINE_LICENSE = "https://xxx";

    protected MediaSource setDataSource() {
        return new DashMediaSource(Uri.parse(URL_DASH), mDataSourceFactory,
                new DefaultDashChunkSource.Factory(mDataSourceFactory), null, null);
    }
}
