package com.kir.simplecameraapp.utillity;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Набор методов для взаимодействия с камерой устройства
 */
public class CameraHandler {
    /** id текущей камеры */
    public static String mCameraId;
    /** Текущее состояния камеры */
    public static boolean mIsRecording = false;

    /** Handler текущей камеры */
    public static CameraDevice mCameraDevice;
    /**
     * Обрабатывает события камеры
     */
    public static CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        /**
         * Исполняет инструкции при открытии камеры
         * @param cameraDevice Handler текущей камеры
         */
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        /**
         * Исполняте инструкции при отключении камеры
         * @param cameraDevice Handler текущей камеры
         */
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = null;
            cameraDevice.close();
        }

        /**
         * Исполняет инструкции при закрытии камеры
         * @param cameraDevice Handler текущей камеры
         * @param i Error code
         */
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            mCameraDevice = null;
            cameraDevice.close();
        }
    };

    /** Размер превью UI элемента */
    public static Size mPreviewSize;
    /** Разрешение видео */
    public static Size mVideoSize;

    public static CaptureRequest.Builder mCaptureRequestBuilder;

    /** Handler потока камеры */
    public static Handler mBackgroundHandler;
    /** Handler элемента отображения превью */
    public static TextureView mTextureView;

    /**
     * Предоставляет программе доступ к задней камере
     * @param width Ширина изображения
     * @param height Высота изображения
     * @param CameraService Информация о камерах на устройсвтве
     */
    public static void setupCamera (int width, int height, Object CameraService){
        CameraManager cameraManager = (CameraManager) CameraService;
        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT)
                    continue;
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), width, height);
                mCameraId = cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Запускает запись видео
     * @param mTextureView  Handler элемента для отображения изображения
     */
    public static void startRecord(TextureView mTextureView){
        try {
            FileManagerHandler.setupMediaRecorder();
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = FileManagerHandler.mMediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            try {
                                cameraCaptureSession.setRepeatingRequest(
                                        mCaptureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    }, null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Отображает изображение с камеры в пользовательский интерфейс
     */
    public static void startPreview(){
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            try {
                                cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Выбирает оптимальный размер изображения для экрана устройства
     * @param choices Возможные разрешения устройства
     * @param width Ширина экрана
     * @param height Высота экрана
     * @return Оптимальное разрешение изображения
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices){
            if(option.getHeight() == option.getWidth() * height / width
                    && option.getWidth() >= width
                    && option.getHeight() >= height)
                bigEnough.add(option);
        }
        if(bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    /**
     * Закрывает камеру, освобождая ресурсы
     */
    public static void closeCamera(){
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    /**
     * Структура для сравнения разрешений изображения
     */
    private static class CompareSizeByArea implements Comparator<Size> {

        /**
         * Сравнивает разрешения по кол-ву пикилей
         * @param size Разрешение первого изображения
         * @param t1 Разрешение второго изображения
         * @return Наименьшее изображение по кол-ву пикселей
         */
        @Override
        public int compare(Size size, Size t1) {
            return Long.signum((long) size.getWidth() * size.getHeight() /
                    (long) t1.getWidth() * t1.getHeight());
        }
    }
}
