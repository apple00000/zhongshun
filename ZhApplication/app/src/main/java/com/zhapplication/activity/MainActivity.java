package com.zhapplication.activity;

/*
*   主界面
* */

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.XzjhSystemManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.zhapplication.R;

import com.zhapplication.model.compare.DetecteSeeta;
import com.zhapplication.model.detection.TensorFlowObjectDetectionAPIModel;
import com.zhapplication.utils.Common;
import com.zhapplication.utils.Pic;
import com.zhapplication.utils.fileUtil;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static Boolean initialized = false;  // 只执行一次的控制锁

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_login = findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new btnLoginOnClickListener());

        // 初始化
        Boolean initSuccess = initMainActivity();
        if (!initSuccess){
            finish();
        }

        // 加载本地人脸数据
//        ArrayList<String> file_list = fileUtil.getAllDataFileName(Common.FileFace, "jpg");
//        Bitmap bm_0 = fileUtil.openImage(Common.FileFace + "/" +file_list.get(0));
//        Bitmap bm_1 = fileUtil.openImage(Common.FileFace + "/" +file_list.get(1));
//
//        float y = mDetecteSeeta.getSimilarityNum(bm_0, bm_1);
//        Log.v("xxx1 bm0", bm_0.toString());
//        Log.v("xxx1 bm1", bm_1.toString());
//        Log.v("xxx1", String.valueOf(y));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.optionmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            // 【按钮】人脸录入
            case R.id.face_login:
                Intent intent = new Intent(MainActivity.this, FaceInputActivity.class);
                startActivity(intent);
        }
        return true;
    }

    // 【按钮】登录
    class btnLoginOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }

    // 初始化硬件
    @SuppressLint("WrongConstant")
    private void hardWare() {
        XzjhSystemManager mManager = null;
        // 【硬件API 初始化】
        mManager = (XzjhSystemManager)getSystemService("xzjh_server");
        // 屏幕方向旋转90度，改为竖屏
        mManager.xzjhSetScreenRotation(1);
    }

    // 主界面初始化，只执行一次
    private Boolean initMainActivity() {
        if (initialized){
            return true;
        }

        // 初始化存储路径
        initPath();

        // 初始化人脸检测模型
        Boolean isSuccess = initModel();
        if (!isSuccess){
            return false;
        }

        // 初始化目标检测分类器
        try {
            Common.classifier = TensorFlowObjectDetectionAPIModel.create(this.getAssets(), Common.TF_OD_API_MODEL_FILE, Common.TF_OD_API_LABELS_FILE, Common.TF_OD_API_INPUT_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // 初始化jni
        DetecteSeeta.initJni();

        // 加载本地人脸数据
        Common.resetLocalFaceImageList();

        initialized = true;
        return true;
    }

    // 动态初始化存储路径
    private void initPath() {
        Common.FilePath =  this.getExternalFilesDir(Environment.DIRECTORY_DCIM).getAbsolutePath();
        Common.FileFace = Common.FilePath + "/face";
        Common.modelPath =  Common.FilePath + "/Model";
        Common.model1Path = Common.modelPath + "/seeta_fa_v1.1.bin";
        Common.model2Path = Common.modelPath + "/seeta_fd_frontal_v1.0.bin";
        Common.model3Path = Common.modelPath + "/seeta_fr_v1.0.bin";
    }

    // 初始化模型文件，复制模型到存储位置
    private Boolean initModel() {
        fileUtil.createDir(Common.FileFace);
        if (Common.modelExist()){
            return true;
        }else{
            Boolean copySuccess = fileUtil.copyAssetsToDst(this, "Model", Common.modelPath);
            return copySuccess;
        }
    }
}