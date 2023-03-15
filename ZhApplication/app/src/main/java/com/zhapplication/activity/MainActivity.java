package com.zhapplication.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.zhapplication.R;
import com.zhapplication.utils.DrawView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_login = findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new btnLoginOnClickListener());

//        // 创建画布
//        Bitmap croppedBitmap = Bitmap.createBitmap((int) 600, (int) 600, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(croppedBitmap);
//
//        // 创建画笔
//        Paint paint = new Paint();
//        paint.setColor(Color.RED);
//        paint.setStyle(Paint.Style.FILL);
//
//        // 画矩形
//        RectF rect = new RectF(0, 0, 100, 100);
//        canvas.drawRect(rect, paint);

//        DrawView customView = new DrawView(this);
//        setContentView(customView);
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