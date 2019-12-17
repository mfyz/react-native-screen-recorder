package com.screenrecorder;

import com.facebook.react.ReactActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;

public class MainActivity extends ReactActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private File cacheDir;
    private File filesDir;
    private String videoPath;
    private String gifPath;

    private FFmpeg ffmpeg;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cacheDir = getApplicationContext().getCacheDir();
        filesDir = getApplicationContext().getFilesDir();

        ffmpeg = FFmpeg.getInstance(this);
        if (ffmpeg.isSupported()) {
            Log.d("FFMPEG-SUPPORT", "YES - FFMPEG IS SUPPORTED");
        } else {
            Log.d("FFMPEG-SUPPORT", "NO - FFMPEG IS __NOT__ SUPPORTED");
        }

        RecorderManager.updateActivity(this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = null;

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

//        loadFFMpegBinary();
    }

//    private void loadFFMpegBinary() {
//        try {
//            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
//                @Override
//                public void onFailure() {
//                    Log.d("FFMPEG-INIT", "Failed! onFailure");
//                }
//            });
//        } catch (FFmpegNotSupportedException e) {
//            Log.d("FFMPEG-INIT", "Failed! Exception!");
//        }
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            mMediaRecorder = null;
            mMediaProjection = null;
            return;
        }

        try {
            mMediaProjectionCallback = new MediaProjectionCallback();
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            mMediaProjection.registerCallback(mMediaProjectionCallback, null);
            mVirtualDisplay = createVirtualDisplay();
            mMediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startRecording() {

        try {
            initRecorder();
            shareScreen();
        } catch (Exception e) {
            e.printStackTrace();
            mMediaRecorder = null;
            mMediaProjection = null;
        }
    }

    public void stopRecording() {

        if (mMediaRecorder == null) {
            return;
        }
        try {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            convertMovieToGif();
            stopScreenSharing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void convertMovieToGif() {
//        videoPath
        /*
            -i
            /inout/file/path.mp4
            -ss 15 (starting second) - if we want to trim the video
            -t 20 (seconds after starting second) - if we want to trim the video
            -r 10 (frame per second)
            -vf scale=160:90 (resize)
            -vf "crop=in_w:in_h-50"
            /output/gif/file/path.gif
            -hide_banner - disable copyright banner
        */

        // Scale/resize
        String filters = "scale=500:-1";

        // High quality gif export
        filters += ",split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse";

        String cmdStr = "-i " + videoPath + " -vf " + filters + " -r 10 " + gifPath;
        Log.d("FFMPEG-EXEC", "Command: " + cmdStr);

        String[] cmd = cmdStr.split(" ");
        ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
            @Override
            public void onStart() {
                Log.d("FFMPEG-INS START", "Starting the ffmpeg command");
            }

            @Override
            public void onProgress(String message) {}

            @Override
            public void onFailure(String message) {
                Log.d("FFMPEG-INS FAIL", message);
            }

            @Override
            public void onSuccess(String message) {
                Log.d("FFMPEG-INS SUCCESS", message);
            }

            @Override
            public void onFinish() {
                Log.d("FFMPEG-INS FINISH", "DONE!");
            }
        });
    }

    public String getVideoPath() {
        return videoPath;
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        try {
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            File outputFile = File.createTempFile("recording", ".mp4", cacheDir);
            videoPath = outputFile.getAbsolutePath();
//            videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/video.mp4";

            String[] videoPathArr = videoPath.split("/");
            String[] videoFileNameArr = videoPathArr[videoPathArr.length - 1].split("\\.");
            String outputFileName = videoFileNameArr[0] + ".gif";
            gifPath = filesDir.getAbsolutePath() + "/" + outputFileName;
            Log.d("PATHS DEFINED", ">>>>>>>>> IN videoPath: " + videoPath);
            Log.d("PATHS DEFINED", "<<<<<<<<< OUT gifPath: " + gifPath);

            mMediaRecorder.setOutputFile(videoPath);

            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            int orientation = ORIENTATIONS.get(rotation + 90);
//            mMediaRecorder.setOrientationHint(orientation);

            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            stopRecording();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        mMediaRecorder.release();
        mMediaRecorder = null;
        destroyMediaProjection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    /**
     * Returns the name of the main component registered from JavaScript.
     * This is used to schedule rendering of the component.
     */
    @Override
    protected String getMainComponentName() {
        return "screenrecorder";
    }
}
