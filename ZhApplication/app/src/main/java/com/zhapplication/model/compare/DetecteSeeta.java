package com.zhapplication.model.compare;

/*
* 人脸比对方法封装，调用java.com.seetaface接口
* */

import android.graphics.Bitmap;
import android.util.Log;

import seetaface.CMSeetaFace;
import seetaface.SeetaFace;

import com.zhapplication.utils.Common;

public class DetecteSeeta {
    int face_num1, face_num2;
    SeetaFace jni;
    Bitmap mFaceBmp1,mFaceBmp2;//小脸

    // 检测人脸相似度，face1、face2为两张脸的bitmap，返回相似度值，介于0-1直接
    public float getSimilarityNum(Bitmap face1, Bitmap face2) {
        if (!Common.modelExist()){
            Log.v("[getSimilarityNum]","人脸正面检测模型不存在");
            return 0.0f;
        }

        jni = new SeetaFace();//实例化检测对象
        jni.init(Common.modelPath+"/");
        mFaceBmp1 = Bitmap.createBitmap(256,256, Bitmap.Config.ARGB_8888);
        mFaceBmp2 = Bitmap.createBitmap(256,256, Bitmap.Config.ARGB_8888);
        CMSeetaFace[] tRetFaces1, tRetFaces2;
        tRetFaces1 = jni.DetectFaces(face1, mFaceBmp1);
        tRetFaces2 = jni.DetectFaces(face2, mFaceBmp2);
        if (tRetFaces2==null) {
            Log.v("[getSimilarityNum]", "tRetFaces2 null");
            return 0.0f;
        }
        if (tRetFaces1==null) {
            Log.v("[getSimilarityNum]", "tRetFaces1 null");
            return 0.0f;
        }

        face_num1 =  tRetFaces1.length;
        face_num2 =  tRetFaces2.length;
        if(face_num1 > 0 && face_num2 > 0){
            float r = jni.CalcSimilarity(tRetFaces1[0].features, tRetFaces2[0].features);
            return r;
        }else {
            return 0.0f;
        }
    }
}
