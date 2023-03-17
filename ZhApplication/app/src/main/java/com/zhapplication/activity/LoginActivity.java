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

import com.zhapplication.model.Classifier;
import com.zhapplication.model.TensorFlowObjectDetectionAPIModel;
import com.zhapplication.utils.AutoFitTextureView;
import com.zhapplication.utils.Camera;
import com.zhapplication.utils.Common;
import com.zhapplication.utils.fileUtil;

public class LoginActivity extends AppCompatActivity {
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
        ORIENTATION.append(Surface.ROTATION_0, 0);
        ORIENTATION.append(Surface.ROTATION_90, 90);
        ORIENTATION.append(Surface.ROTATION_180, 180);
        ORIENTATION.append(Surface.ROTATION_270, 270);
    }

    private boolean runClassifier = false;
    private final Object lock = new Object();
    private boolean hasFace = false;
    private int signStatus = 0; // 认证状态，0：未认证，1：已认证
    // 本地的bitmap人脸数据
    private ArrayList<Bitmap> localFaceBitmapList = new ArrayList<>();

    // 分类器
    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/frozen_inference_graph_v6.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 左上角返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_login);
        textureView =  (AutoFitTextureView) findViewById(R.id.textureView_login);
        imageView = (ImageView) findViewById(R.id.imageView_login);

        // 初始化分类器
        try {
            classifier = TensorFlowObjectDetectionAPIModel.create(this.getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (IOException e) {
            Log.e("xxx1","classifier err");
            e.printStackTrace();
        }

        // 加载本地人脸数据
        ArrayList<String> file_list = fileUtil.getAllDataFileName(Common.FilePath, "jpg");
        for (int i = 0;i<file_list.size();i++){
            Bitmap bm = fileUtil.openImage(Common.FilePath + file_list.get(i));
            localFaceBitmapList.add(bm);
        }
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
        if (!textureView.isAvailable()) {
            textureView.setSurfaceTextureListener(mTextureListener);
            imageView.getLayoutParams().width = textureView.getWidth();
            imageView.getLayoutParams().height = textureView.getHeight();
        } else {
            startPreview();
        }
    }

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
                    previewSize = Camera.getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                    mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                        @Override
                        public int compare(Size o1, Size o2) {
                            return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                        }
                    });
                }
                setUpImageReader();
//                mCameraId = cameraId;
                mCameraId = "1"; // 0后置 1前置 109板子
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
                mCameraHandler.post(new Camera.ImageSaver(reader.acquireNextImage()));
            }

        }, mCameraHandler);
    }

    // 人脸检测
    private Runnable periodicClassify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runClassifier) {
                            Bitmap bitmap = textureView.getBitmap(300, 300);
                            Bitmap croppedBitmap = Bitmap.createBitmap((int) 300, (int) 300, Bitmap.Config.ARGB_8888);
                            if (bitmap!=null) {
                                int[] faceLoc = Camera.getFace(bitmap);
                                if (faceLoc!=null){
                                    // 【认证】处理登录成功逻辑，目前默认成功
                                    if (signStatus==0) {
                                        if (true) { // 默认成功
                                            signStatus = 1;
                                            imageView.setImageBitmap(croppedBitmap);
                                            Toast.makeText(LoginActivity.this, "认证成功", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    hasFace = true;
                                    if (signStatus==0){
                                        croppedBitmap = Camera.drawRect(faceLoc[0], faceLoc[1], faceLoc[2], faceLoc[3]);
                                    }else {
                                        // 检测抽烟
                                        croppedBitmap = classifyFrame(bitmap);
                                    }
                                }else{
                                    hasFace = false;
                                }
                            }else{
                                hasFace = false;
                            }

                            // 画图
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

    /**
     * tensorFlow识别
     */
    private Bitmap classifyFrame(Bitmap bitmap) {
        if (classifier == null ) {
            return null;
        }

        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

        Bitmap croppedBitmap = Bitmap.createBitmap((int) 300, (int) 300, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                Paint paint = new Paint();
                Paint paint1 = new Paint();
                if (result.getTitle().equals("openeyes")) {
                    paint.setColor(Color.GREEN);
                    paint1.setColor(Color.GREEN);
                } else if (result.getTitle().equals("closeeyes")) {
                    paint.setColor(Color.RED);
                    paint1.setColor(Color.RED);

                } else if (result.getTitle().equals("phone")) {
                    paint.setColor(0xFFFF9900);
                    paint1.setColor(0xFFFF9900);

                } else if (result.getTitle().equals("smoke")) {
                    paint.setColor(Color.YELLOW);
                    paint1.setColor(Color.YELLOW);
                } else
                    paint.setColor(Color.WHITE);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5.0f);
                paint.setAntiAlias(true);
                paint1.setStyle(Paint.Style.FILL);
                paint1.setAlpha(125);
                canvas.drawRect(location, paint);
//                canvas.drawRect(canvasWidth * location.left / TF_OD_API_INPUT_SIZE,
//                        canvasHeight * location.top / TF_OD_API_INPUT_SIZE,
//                        canvasWidth * location.right / TF_OD_API_INPUT_SIZE,
//                        canvasHeight * location.bottom / TF_OD_API_INPUT_SIZE, paint);
//                canvas.drawRect(canvasWidth * location.left / TF_OD_API_INPUT_SIZE,
//                        canvasHeight * location.top / TF_OD_API_INPUT_SIZE,
//                        canvasWidth * location.right / TF_OD_API_INPUT_SIZE,
//                        canvasHeight * location.bottom / TF_OD_API_INPUT_SIZE, paint1);
            }

        }

        return croppedBitmap;
    }

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
