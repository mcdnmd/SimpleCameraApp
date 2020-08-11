package com.kir.simplecameraapp.utillity;

import android.annotation.SuppressLint;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Набор функций для взаимодействия с файловой системой устройства
 */
public class FileManagerHandler {
    /** Handler файловой системы */
    public static MediaRecorder mMediaRecorder = new MediaRecorder();
    /** Путь к папке для сохранения файлов */
    public static File mVideoFolder;
    /** Название файла для сохранения */
    public static String mVideoFileName;

    public static String TAG = "Test_TAG";

    /**
     * Создает папку для сохранения файлов. По умолчанию папка Camera
     */
    public static void createVideoFolder(){
        mVideoFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if(!mVideoFolder.exists()){
            boolean dirCreated = mVideoFolder.mkdirs();
            if (!dirCreated)
                Log.e(TAG, "Directory creation failed");
        }
    }

    /**
     * Создает название файла для сохранения
     * @return Название файла
     * @throws IOException Ошибка ввода/вывода
     */
    public static File createVideoFileName() throws IOException {
        createVideoFolder();
        @SuppressLint("SimpleDateFormat") String timestamp = new SimpleDateFormat("yyyyMMdd__HHmmsss").format(new Date());
        String prepend = "VID_" + timestamp;
        File videoFile = new File(mVideoFolder, prepend + ".mp4");
        mVideoFileName = videoFile.getAbsolutePath();
        return videoFile;
    }

    /**
     * Подключает handler файловой системы
     * @throws IOException Ошибка ввода/вывода
     */
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
