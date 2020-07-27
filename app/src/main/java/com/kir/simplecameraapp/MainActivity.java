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
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.kir.simplecameraapp.utillity.CameraHandler;
import com.kir.simplecameraapp.utillity.FileManagerHandler;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 0;
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 1;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        mTextureView = findViewById(R.id.textureView);
        CameraHandler.mTextureView = mTextureView;
        mRecordImageButton = findViewById(R.id.recordButton);
        mRecordImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CameraHandler.mIsRecording) {
                    CameraHandler.mIsRecording = false;
                    mRecordImageButton.setImageResource(R.drawable.btn_camera_ready);
                    CameraHandler.startPreview();
                    FileManagerHandler.mMediaRecorder.stop();
                    FileManagerHandler.mMediaRecorder.reset();
                } else {
                    CameraHandler.mIsRecording = true;
                    mRecordImageButton.setImageResource(R.drawable.btn_camera_busy);
                    try {
                        FileManagerHandler.createVideoFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    CameraHandler.startRecord(mTextureView);
                    FileManagerHandler.mMediaRecorder.start();
                }
            }
        });
    }

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

    @Override
    protected void onPause() {
        CameraHandler.closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

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

    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("SimpleCamera");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
        CameraHandler.mBackgroundHandler = mBackgroundHandler;
    }

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

    private void checkPermission() {
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
    }
}