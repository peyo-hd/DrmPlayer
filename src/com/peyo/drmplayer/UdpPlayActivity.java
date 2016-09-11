package com.peyo.drmplayer;

import android.net.Uri;

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.UdpDataSource;

public class UdpPlayActivity extends ExoPlayActivity {
    private String URL_UDP = "udp://xxx:yyy";

    protected MediaSource setDataSource() {
        return new ExtractorMediaSource(Uri.parse(URL_UDP),
                new DataSource.Factory() {
                    public DataSource createDataSource() {
                        return new UdpDataSource(null); } },
                new DefaultExtractorsFactory(), null, null);
    }
}
