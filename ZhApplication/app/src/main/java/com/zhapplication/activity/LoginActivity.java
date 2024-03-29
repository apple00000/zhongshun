package com.zhapplication.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.TextureView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zhapplication.R;
import android.view.Surface;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.zhapplication.model.detection.Classifier;
import com.zhapplication.model.detection.TensorFlowObjectDetectionAPIModel;
import com.zhapplication.utils.AutoFitTextureView;
import com.zhapplication.utils.Pic;
import com.zhapplication.utils.Common;
import com.zhapplication.utils.fileUtil;
import com.zhapplication.model.compare.DetecteSeeta;

public class LoginActivity extends AppCompatActivity {
    private CameraBase camera1 = new CameraBase();

    private boolean runClassifier = false;
    private final Object lock = new Object();
    private int signStatus = 0; // 认证状态，0：未认证，1：已认证

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 左上角返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_login);
        camera1.textureView =  (AutoFitTextureView) findViewById(R.id.textureView_login);
        camera1.imageView = (ImageView) findViewById(R.id.imageView_login);
        camera1.mCameraId = "109";
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        startCameraThread();
        if (!camera1.textureView.isAvailable()) {
            camera1.textureView.setSurfaceTextureListener(mTextureListener);
            camera1.imageView.getLayoutParams().width = camera1.textureView.getWidth();
            camera1.imageView.getLayoutParams().height = camera1.textureView.getHeight();
        } else {
            startPreview(camera1);
        }
    }

    @Override
    public void onPause() {
        closeCamera(camera1);
        stopBackgroundThread();
        super.onPause();
    }

    TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            //SurfaceTexture组件可用的时候,设置相机参数，并打开摄像头
            //设置摄像头参数
            setUpCamera(width, height, camera1);
            //打开摄像头
            openCamera(camera1);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            //尺寸发生变化的时候
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            //组件被销毁的时候
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            //组件更新的时候
        }
    };

    private Boolean setUpCamera(int width, int height, CameraBase mCamera) {
        mCamera.cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //拿到摄像头的id
        try {
            //得到摄像头的参数
            CameraCharacteristics cameraCharacteristics = mCamera.cameraManager.getCameraCharacteristics(mCamera.mCameraId);
            Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return false;
            }
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) { //找到摄像头能够输出的，最符合我们当前屏幕能显示的最小分辨率
                mCamera.previewSize = Pic.getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCamera.mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size o1, Size o2) {
                        return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                    }
                });
            }
            setUpImageReader(mCamera);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void setUpImageReader(CameraBase mCamera) {
        mCamera.imageReader = ImageReader.newInstance(mCamera.mCaptureSize.getWidth(), mCamera.mCaptureSize.getHeight(), ImageFormat.JPEG, 2);
        mCamera.imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
            }
        }, mCamera.mCameraHandler);
    }

    // 人脸检测
    private Runnable periodicClassify =
        new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    if (runClassifier) {
                        Bitmap bitmap = camera1.textureView.getBitmap(Common.TF_OD_API_INPUT_SIZE_WIDTH, Common.TF_OD_API_INPUT_SIZE_HEIGHT);
                        Bitmap croppedBitmap = Bitmap.createBitmap((int) Common.TF_OD_API_INPUT_SIZE_WIDTH, (int) Common.TF_OD_API_INPUT_SIZE_HEIGHT, Bitmap.Config.ARGB_8888);
                        if (bitmap!=null) {
                            if (1==signStatus) {
                                // 【检测】抽烟、电话、疲劳
                                croppedBitmap = Pic.classifyFrame(bitmap, croppedBitmap);
                            }else{
                                Pic.PicData faceLoc = Pic.getFace(bitmap);
                                if (faceLoc!=null){
                                    // 【认证】处理登录成功逻辑，目前默认成功
                                    if (0==signStatus) {
                                        if (Common.verifyLoginFace(faceLoc.cm)) {
                                            signStatus = 1;
                                            camera1.imageView.setImageBitmap(croppedBitmap);
                                            Toast.makeText(LoginActivity.this, "认证成功", Toast.LENGTH_SHORT).show();
                                        }

                                        croppedBitmap = Pic.drawRect(faceLoc.rect[0], faceLoc.rect[1], faceLoc.rect[2], faceLoc.rect[3]);
                                    }else {

                                    }

                                    bitmap.recycle();
                                }else{

                                }
                            }
                        }else{

                        }

                        // 画图
                        Bitmap finalCroppedBitmap = croppedBitmap;
                        camera1.imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                camera1.imageView.setImageBitmap(finalCroppedBitmap);
                            }
                        });
                    }
                }

                camera1.mCameraHandler.post(periodicClassify);
            }
        };


    private void openCamera(CameraBase mCamera) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }
        try {
            mCamera.cameraManager.openCamera(mCamera.mCameraId, mStateCallback_1, mCamera.mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mCamera.imageView.getLayoutParams().width = mCamera.textureView.getWidth();
        mCamera.imageView.getLayoutParams().height = mCamera.textureView.getHeight();
    }

    private void closeCamera(CameraBase mCamera) {
        if (null != mCamera.mCameraCaptureSession) {
            mCamera.mCameraCaptureSession.close();
            mCamera.mCameraCaptureSession = null;
        }
        if (null != mCamera.cameraDevice) {
            mCamera.cameraDevice.close();
            mCamera.cameraDevice = null;
        }
        if (null != mCamera.imageReader) {
            mCamera.imageReader.close();
            mCamera.imageReader = null;
        }
    }

    // 主摄像头
    CameraDevice.StateCallback mStateCallback_1 = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) { //摄像头打开
            camera1.cameraDevice = camera;
            startPreview(camera1);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) { //摄像头关闭
            camera1.cameraDevice.close();
            camera1.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {//摄像头出现错误
            camera1.cameraDevice.close();
            camera1.cameraDevice = null;
        }
    };

    // 预览
    private void startPreview(CameraBase mCamera) {
        //建立图像缓冲区
        SurfaceTexture surfaceTexture = mCamera.textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mCamera.previewSize.getWidth(), mCamera.previewSize.getHeight());

        //得到界面的显示对象
        Surface surface = new Surface(surfaceTexture);
        try {
            mCamera.captureRequestBuilder = mCamera.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCamera.captureRequestBuilder.addTarget(surface);
            //建立通道(CaptureRequest和CaptureSession会话)
            mCamera.cameraDevice.createCaptureSession(Arrays.asList(surface, mCamera.imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCamera.captureRequest = mCamera.captureRequestBuilder.build();
                    mCamera.mCameraCaptureSession = session;
                    try {
                        mCamera.mCameraCaptureSession.setRepeatingRequest(mCamera.captureRequest, null, mCamera.mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCamera.mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //开启摄像头线程
    private void startCameraThread() {
        camera1.handlerThread = new HandlerThread("myHandlerThread");
        camera1.handlerThread.start();
        camera1.mCameraHandler = new Handler(camera1.handlerThread.getLooper());
        synchronized (lock) {
            runClassifier = true;
        }
        camera1.mCameraHandler.post(periodicClassify);
    }

    // 停止后台线程
    private void stopBackgroundThread() {
        camera1.handlerThread.quitSafely();
        try {
            camera1.handlerThread.join();
            camera1.handlerThread = null;
            camera1.mCameraHandler = null;
            synchronized (lock) {
                runClassifier = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
