package com.kir.simplecameraapp.utillity;

import android.annotation.SuppressLint;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileManagerHandler {
    public static MediaRecorder mMediaRecorder = new MediaRecorder();
    public static File mVideoFolder;
    public static String mVideoFileName;

    public static String TAG = "Test_TAG";

    public static void createVideoFolder(){
        mVideoFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "scVideo");
        if(!mVideoFolder.exists()){
            boolean dirCreated = mVideoFolder.mkdirs();
            if (!dirCreated)
                Log.e(TAG, "Directory creation failed");
        }
    }

    public static File createVideoFileName() throws IOException {
        createVideoFolder();
        @SuppressLint("SimpleDateFormat") String timestamp = new SimpleDateFormat("yyyyMMdd__HHmmsss").format(new Date());
        String prepend = "VIDEO_" + timestamp + "_";
        File videoFile = File.createTempFile(prepend, ".mp4", mVideoFolder);
        mVideoFileName = videoFile.getAbsolutePath();
        return videoFile;
    }

    public static void setupMediaRecorder() throws IOException {
        mMediaRecorder.reset();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setVideoEncodingBitRate(1000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(CameraHandler.mVideoSize.getWidth(), CameraHandler.mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setOrientationHint(0);
        mMediaRecorder.prepare();
    }
}
