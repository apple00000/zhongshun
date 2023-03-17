package com.zhapplication.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.XzjhSystemManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.zhapplication.R;

import com.zhapplication.utils.Common;
import com.zhapplication.utils.fileUtil;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    XzjhSystemManager mMamager = null;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_login = findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new btnLoginOnClickListener());

        // 设置存储路径
        Common.FilePath = this.getExternalFilesDir(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/";
        Log.v("xxx1 Common.FilePath", Common.FilePath);

//        // 【硬件API 初始化】
//        mMamager = (XzjhSystemManager)getSystemService("xzjh_server");
//        // 屏幕方向旋转90度，改为竖屏
//        mMamager.xzjhSetScreenRotation(1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.optionmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            // 人脸录入
            case R.id.face_login:
                Intent intent = new Intent(MainActivity.this, FaceInputActivity.class);
                startActivity(intent);
        }
        return true;
    }

    // 登录按钮
    class btnLoginOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }
}