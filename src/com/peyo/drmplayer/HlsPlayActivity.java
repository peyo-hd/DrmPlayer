package com.peyo.drmplayer;

import android.net.Uri;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

public class HlsPlayActivity extends ExoPlayActivity {

    private String URL_HLS = "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8";

    protected MediaSource setDataSource() {
        return new HlsMediaSource(Uri.parse(URL_HLS),
                new DefaultDataSourceFactory(this,"HlsPlayActivity"),
                null, null);
    }

}
