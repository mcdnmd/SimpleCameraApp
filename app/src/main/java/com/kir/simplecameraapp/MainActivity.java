package com.kir.simplecameraapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.kir.simplecameraapp.utillity.CameraHandler;
import com.kir.simplecameraapp.utillity.CameraMode;
import com.kir.simplecameraapp.utillity.FileManagerHandler;

import java.io.IOException;

/**
 *  \maingape HD Photo Camera Pro
 *
 *  Приложение разработано для учебных целей: Изучить API камеры телефона,
 *  изучить интеграцию модулей рекламы для монетизации приложений
 */

/**
 * Базовый класс главной активности
 * @author Полтораднев Кирилл
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 0;
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 1;

    private CameraMode mCameraMode = CameraMode.VIDEO;

    private ImageButton mRecordImageButton;
    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            CameraHandler.setupCamera(width, height, getSystemService(Context.CAMERA_SERVICE));
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    private AdView mAdView;

    /**
     * Инициализирует UI, камеру устройства, проверку user permissions
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUI();
        setContentView(R.layout.activity_main);
        checkPermission();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mTextureView = findViewById(R.id.textureView);
        CameraHandler.mTextureView = mTextureView;
        mRecordImageButton = findViewById(R.id.recordButton);
        mRecordImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraMode == CameraMode.VIDEO) {
                    if (CameraHandler.mIsRecording) {
                        CameraHandler.mIsRecording = false;
                        mRecordImageButton.setImageResource(R.drawable.camera_btn_ready);
                        CameraHandler.startPreview();
                        FileManagerHandler.mMediaRecorder.stop();
                        FileManagerHandler.mMediaRecorder.reset();
                    } else {
                        CameraHandler.mIsRecording = true;
                        mRecordImageButton.setImageResource(R.drawable.camera_btn_busy);
                        try {
                            FileManagerHandler.createVideoFileName();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        CameraHandler.startRecord(mTextureView);
                        FileManagerHandler.mMediaRecorder.start();
                    }
                } else if (mCameraMode == CameraMode.PHOTO){
                    return;
                } else {
                    return;
                }
            }
        });
    }

    /**
     * Инициализирует проверку пользовательских разрешении и
     * запускает поток камеры после перезапуска приложения
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            CameraHandler.setupCamera(mTextureView.getWidth(), mTextureView.getHeight(), getSystemService(Context.CAMERA_SERVICE));
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    /**
     * Освобождает занятые рессурсы при выходе из приложения
     */
    @Override
    protected void onPause() {
        CameraHandler.closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * Переводит UI в режим полного экрана без панелей управления
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    /**
     * Оповещает пользователя о статусе проверки пользовательских решений
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "permission has been grunted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "[WARN] permission is not grunted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Безопасно подключает камеру устройства
     */
    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(CameraHandler.mCameraId, CameraHandler.mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                cameraManager.openCamera(CameraHandler.mCameraId, CameraHandler.mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Запускает поток камеры
     */
    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("SimpleCamera");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
        CameraHandler.mBackgroundHandler = mBackgroundHandler;
    }

    /**
     * Закрывает поток камеры
     */
    private void stopBackgroundThread(){
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
            CameraHandler.mBackgroundHandler = mBackgroundHandler;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Осуществляет проверку и запрос пользовательских разрешений
     */
    private void checkPermission() {
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    /**
     * Скрывает лишние элементы базового UI
     */
    private void hideSystemUI(){
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}