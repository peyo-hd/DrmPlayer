package com.peyo.drmplayer;

import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfoEvent;
import android.drm.DrmInfoRequest;
import android.drm.DrmManagerClient;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;

public class WvmPlayActivity extends MediaPlayActivity {
    private static String TAG = "WvmPlayActivity";

    private String DRM_SERVER = "https://xxx";
    private String CONTENT_URI = "widevine://xxx.wvm";
    private String PORTAL_KEY = "xxx";
    private String DEVICE_ID = "xxx";

    DrmManagerClient mDrmManagerClient;
    DrmInfoRequest mDrmInfoRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrmManagerClient = new DrmManagerClient(this);
        mDrmManagerClient.setOnErrorListener(mErrorListener);
        mDrmManagerClient.setOnEventListener(mEventListener);
        mDrmManagerClient.setOnInfoListener(mInfoListener);
        checkDrmEngine();
        if (!mDrmManagerClient.canHandle(CONTENT_URI, "video/wvm")) {
            finish();
        }
        mDrmInfoRequest = new DrmInfoRequest(DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO, "video/wvm");
        mDrmInfoRequest.put("WVDRMServerKey", DRM_SERVER);
        mDrmInfoRequest.put("WVAssetURIKey", CONTENT_URI);
        mDrmInfoRequest.put("WVPortalKey", PORTAL_KEY);
        mDrmInfoRequest.put("WVDeviceIDKey", DEVICE_ID);

        mDrmManagerClient.acquireRights(mDrmInfoRequest);
    }

    @Override
    protected void releaseMediaPlayer() {
        super.releaseMediaPlayer();
        mVideoStarted = false;
    }

    @Override
    protected void setDataSource(MediaPlayer player) throws Exception {
        player.setDataSource(CONTENT_URI);
    }

    boolean mSurfaceCreated = false;
    boolean mDrmReady = false;
    boolean mVideoStarted = false;

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        mSurfaceCreated = true;
        super.surfaceCreated(arg0);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        super.surfaceDestroyed(arg0);
        mSurfaceCreated = false;
    }

    @Override
    protected void playVideo() {
        if (!mSurfaceCreated) return;
        if (!mDrmReady) return;
        if (mVideoStarted) return;
        super.playVideo();
        mVideoStarted = true;
    }

    private void checkDrmEngine() {
        String[] engines = mDrmManagerClient.getAvailableDrmEngines();
        for (String engine : engines) {
            Log.i(TAG, "getAvailableDrmEngines() " + engine);
        }
    }

    DrmManagerClient.OnEventListener mEventListener = new DrmManagerClient.OnEventListener() {
        @Override
        public void onEvent(DrmManagerClient client, DrmEvent event) {
            switch (event.getType()) {
                case DrmEvent.TYPE_DRM_INFO_PROCESSED:
                    Log.i(TAG, "OnEventListener() DrmEvent.TYPE_DRM_INFO_PROCESSED");
                    mDrmReady = true;
                    playVideo();
                    break;
                default:
                    Log.i(TAG, "OnEventListener()  " + event.getType());
                    break;
            }
        }
    };

    DrmManagerClient.OnInfoListener mInfoListener = new DrmManagerClient.OnInfoListener() {
        @Override
        public void onInfo(DrmManagerClient client, DrmInfoEvent info) {
            switch (info.getType()) {
                case DrmInfoEvent.TYPE_RIGHTS_INSTALLED:
                    Log.i(TAG, "OnInfoListener() DrmInfoEvent.TYPE_RIGHTS_INSTALLED");
                    break;
                default:
                    Log.i(TAG, "OnInfoListener() " + info.getType());
                    break;
            }
        }
    };

    DrmManagerClient.OnErrorListener mErrorListener = new DrmManagerClient.OnErrorListener() {
        @Override
        public void onError(DrmManagerClient client, DrmErrorEvent event) {
            Log.i(TAG, "onError() " + event.getType());
            finish();
        }
    };

}
