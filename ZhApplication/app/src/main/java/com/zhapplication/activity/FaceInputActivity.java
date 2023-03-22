package com.zhapplication.activity;

/*
*   人脸录入界面
*   人脸捕捉成功的处理逻辑在 setUpImageReader -> onImageAvailable 中
* */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zhapplication.R;
import android.view.Surface;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.zhapplication.model.compare.DetecteSeeta;
import com.zhapplication.utils.AutoFitTextureView;
import com.zhapplication.utils.Common;
import com.zhapplication.utils.Pic;
import com.zhapplication.utils.fileUtil;

import org.json.JSONException;

import seetaface.CMSeetaFace;

public class FaceInputActivity extends AppCompatActivity {
    private AutoFitTextureView textureView;
    private ImageView imageView;
    private HandlerThread handlerThread;
    private Handler mCameraHandler;
    private CameraManager cameraManager;
    //最佳的预览尺寸
    private Size previewSize;
    //最佳的拍照尺寸
    private Size mCaptureSize;
    private String mCameraId;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private Button btn_photo;
    private ImageReader imageReader;
    private static final SparseArray ORIENTATION = new SparseArray();
    static {
//        ORIENTATION.append(Surface.ROTATION_0, 270);
        ORIENTATION.append(Surface.ROTATION_0, 0);
        ORIENTATION.append(Surface.ROTATION_90, 90);
        ORIENTATION.append(Surface.ROTATION_180, 180);
        ORIENTATION.append(Surface.ROTATION_270, 270);
    }

    private boolean runClassifier = false;
    private final Object lock = new Object();

    private Pic.PicData faceLocSave = null;   // 人脸特征临时缓存
    private int faceNullCnt = 0;              // 清空人脸特征的计数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 左上角返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_face_input);
        textureView =  (AutoFitTextureView) findViewById(R.id.textureView_faceInput);
        imageView = (ImageView) findViewById(R.id.imageView_faceInput);
        btn_photo = findViewById(R.id.btn_photo);
        btn_photo.setOnClickListener(OnClick);
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

    // 拍照
    private final View.OnClickListener OnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // 点击拍照按钮时，需要有人脸才可以
            if (null == faceLocSave){
                Toast.makeText(FaceInputActivity.this, "请对准正脸", Toast.LENGTH_SHORT).show();
                return;
            }

            //获取摄像头的请求
            try {
                CaptureRequest.Builder cameraDeviceCaptureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                cameraDeviceCaptureRequest.addTarget(imageReader.getSurface());
                //获取摄像头的方向
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                    }
                };

                //设置拍照方向
                cameraDeviceCaptureRequest.set(CaptureRequest.JPEG_ORIENTATION, (Integer) ORIENTATION.get(rotation));
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.capture(cameraDeviceCaptureRequest.build(), mCaptureCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            //获取图像的缓冲区
            //获取文件的存储权限及操作
        }
    };

    private void unLockFocus() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startCameraThread();
        if (!textureView.isAvailable()) {
            textureView.setSurfaceTextureListener(mTextureListener);
            imageView.getLayoutParams().width = textureView.getWidth();
            imageView.getLayoutParams().height = textureView.getHeight();
        } else {
            startPreview();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            //SurfaceTexture组件可用的时候,设置相机参数，并打开摄像头
            //设置摄像头参数
            setUpCamera(width, height);
            //打开摄像头
            openCamera();
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

    private void setUpCamera(int width, int height) {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //拿到摄像头的id
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                //得到摄像头的参数
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map != null) { //找到摄像头能够输出的，最符合我们当前屏幕能显示的最小分辨率
                    previewSize = Pic.getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                    mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                        @Override
                        public int compare(Size o1, Size o2) {
                            return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                        }
                    });
                }
                setUpImageReader();
                mCameraId = cameraId;
//                mCameraId = "109"; // 0后置 1前置 109板子
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpImageReader() {
        imageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                String fileName = fileUtil.getFileName();
                // 检测这个人是否已经存在于图片库中
                if (Common.verifyLoginFace(faceLocSave.cm)){
                    Toast.makeText(FaceInputActivity.this, "人脸已经存在", Toast.LENGTH_SHORT).show();
                    backToMain();
                }else{
                    fileUtil.SaveImage(reader.acquireNextImage(), fileName+".jpg");
                    String s1 = DetecteSeeta.CMSeetaFace2String(faceLocSave.cm);
                    fileUtil.writeStrToFile(fileName+".txt", s1);
                    Common.resetLocalFaceImageList();

                    //【录入成功】
                    faceLoginSuccessHandle();
                }
            }
        }, mCameraHandler);
    }

    // 人脸录入成功时的处理逻辑
    private void faceLoginSuccessHandle() {
        // 提示
        Toast.makeText(FaceInputActivity.this, "人脸录入成功", Toast.LENGTH_SHORT).show();
        unLockFocus();

        // 回到主界面
        backToMain();
    }

    // 返回主界面
    private void backToMain(){
        Intent intent1 = new Intent();
        intent1.setClass(FaceInputActivity.this, MainActivity.class);
        startActivity(intent1);
        finish();
    }

    // 人脸检测
    private Runnable periodicClassify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runClassifier) {
                            Bitmap bitmap = textureView.getBitmap(Common.TF_OD_API_INPUT_SIZE_WIDTH, Common.TF_OD_API_INPUT_SIZE_HEIGHT);
                            Bitmap croppedBitmap = Bitmap.createBitmap((int) Common.TF_OD_API_INPUT_SIZE_WIDTH, (int) Common.TF_OD_API_INPUT_SIZE_HEIGHT, Bitmap.Config.ARGB_8888);
                            if (bitmap!=null) {
                                Pic.PicData faceLoc = Pic.getFace(bitmap);
                                if (faceLoc!=null){
                                    faceLocSave = faceLoc;

                                    // 预检测是否存在
                                    if (Common.verifyLoginFace(faceLoc.cm)){
                                        Toast.makeText(FaceInputActivity.this, "人脸已经存在", Toast.LENGTH_SHORT).show();
                                        backToMain();
                                    }

                                    croppedBitmap = Pic.drawRect(
                                            faceLoc.rect[0],
                                            faceLoc.rect[1],
                                            faceLoc.rect[2],
                                            faceLoc.rect[3]
                                    );
                                }else{
                                    faceNullCnt++;
                                    if (25==faceNullCnt){
                                        faceNullCnt=0;
                                        faceLocSave=null;
                                    }
                                }
//                                bitmap.recycle();
                            }else{
                                faceNullCnt++;
                                if (25==faceNullCnt){
                                    faceNullCnt=0;
                                    faceLocSave=null;
                                }
                            }

                            Bitmap finalCroppedBitmap = croppedBitmap;
                            imageView.post(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageBitmap(finalCroppedBitmap);
                                }
                            });
                        }
                    }

                    mCameraHandler.post(periodicClassify);
                }
            };

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }
        try {
            cameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        imageView.getLayoutParams().width = textureView.getWidth();
        imageView.getLayoutParams().height = textureView.getHeight();
    }

    private void closeCamera() {
//        try {
//            cameraOpenCloseLock.acquire();
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
//        } finally {
//            cameraOpenCloseLock.release();
//        }
    }

    CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) { //摄像头打开
            cameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) { //摄像头关闭
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {//摄像头出现错误
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    // 预览
    private void startPreview() {
        //建立图像缓冲区
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        //得到界面的显示对象
        Surface surface = new Surface(surfaceTexture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            //建立通道(CaptureRequest和CaptureSession会话)
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureRequest = captureRequestBuilder.build();
                    mCameraCaptureSession = session;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(captureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //开启摄像头线程
    private void startCameraThread() {
        handlerThread = new HandlerThread("myHandlerThread");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
        synchronized (lock) {
            runClassifier = true;
        }
        mCameraHandler.post(periodicClassify);
    }

    // 停止后台线程
    private void stopBackgroundThread() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            mCameraHandler = null;
            synchronized (lock) {
                runClassifier = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
