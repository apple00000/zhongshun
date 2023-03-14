package com.zhapplication.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.zhapplication.R;

import java.io.File;
import java.io.IOException;

public class FaceLoginActivity extends AppCompatActivity {
    final int TAKE_PHOTO=1;
    ImageView iv_photo;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_login);

        // 左上角返回箭头
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button btn_1=findViewById(R.id.btn_takephoto);
        iv_photo=findViewById(R.id.img_photo);
        btn_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File output=new File(getExternalCacheDir(),"output_image.jpg");
                try {
                    if (output.exists()){
                        output.delete();
                    }
                    output.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT>=24){
//图片的保存路径
                    imageUri= FileProvider.getUriForFile(FaceLoginActivity.this,"com.zhapplication", output);
                }
                else { imageUri=Uri.fromFile(output);}
                //跳转界面到系统自带的拍照界面
                Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,TAKE_PHOTO);
            }
        });
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
}