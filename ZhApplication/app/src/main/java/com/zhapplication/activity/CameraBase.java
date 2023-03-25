package com.zhapplication.activity;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.widget.ImageView;

import com.zhapplication.utils.AutoFitTextureView;

public class CameraBase {
    public AutoFitTextureView textureView;
    public ImageView imageView;
    public HandlerThread handlerThread;
    public Handler mCameraHandler;
    public CameraManager cameraManager;
    //最佳的预览尺寸
    public Size previewSize;
    //最佳的拍照尺寸
    public Size mCaptureSize;
    public String mCameraId;
    public CameraDevice cameraDevice;
    public CaptureRequest.Builder captureRequestBuilder;
    public CaptureRequest captureRequest;
    public CameraCaptureSession mCameraCaptureSession;
    public ImageReader imageReader;
    public static final SparseArray ORIENTATION = new SparseArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0, 0);
        ORIENTATION.append(Surface.ROTATION_90, 90);
        ORIENTATION.append(Surface.ROTATION_180, 180);
        ORIENTATION.append(Surface.ROTATION_270, 270);
    }
}
