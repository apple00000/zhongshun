package com.zhapplication.model.compare;

/*
* 人脸比对方法封装，调用java.com.seetaface接口
* */

import android.graphics.Bitmap;
import android.util.Log;

import seetaface.CMSeetaFace;
import seetaface.SeetaFace;

import com.zhapplication.utils.Common;
import com.zhapplication.utils.Pic;

import java.util.Arrays;

public class DetecteSeeta {
    private static SeetaFace jni;

    // 初始化
    public static void initJni(){
        jni = new SeetaFace();//实例化检测对象
        jni.init(Common.modelPath+"/");
    }

    // 获取bitmap旋转后的有效特征
//    public static CMSeetaFace getSeetaFaceValueWithRotate(Bitmap face) {
//        CMSeetaFace c = getSeetaFaceValue(face);
//        if (c!=null){
//            Log.v("CMSeetaFace", "0");
//            return c;
//        }
//        Bitmap f90 = Pic.bitmapRotate(face, 90);
//        c = getSeetaFaceValue(f90);
//        if (c!=null){
//            Log.v("CMSeetaFace", "90");
//            return c;
//        }
//        Bitmap f180 = Pic.bitmapRotate(face, 180);
//        c = getSeetaFaceValue(f180);
//        if (c!=null){
//            Log.v("CMSeetaFace", "180");
//            return c;
//        }
//        Bitmap f270 = Pic.bitmapRotate(face, 270);
//        c = getSeetaFaceValue(f270);
//        if (c!=null){
//            Log.v("CMSeetaFace", "270");
//            return c;
//        }
//        return null;
//    }

    // 获取face特征
    public static CMSeetaFace getSeetaFaceValue(Bitmap face) {
        CMSeetaFace[] f = jni.DetectFaces(face, face);
        if (f!=null){
            if (f.length>0){
                return f[0];
            }
        }
        return null;
    }

    // 检测人脸相似度，face1、face2为两张脸的bitmap，返回相似度值，介于0-1直接
    public static float getSimilarityNum(CMSeetaFace face1, CMSeetaFace face2) {
        if (null==face1){
            Log.v("getSimilarityNum","f1 null");
        }
        if (null==face2){
            Log.v("getSimilarityNum","f2 null");
        }

        return jni.CalcSimilarity(face1.features, face2.features);
    }

    // 【格式转化】CMSeetaFace -> String
    public static String CMSeetaFace2String(CMSeetaFace cm) {
        String s = "";
        s+=cm.left+","+cm.right+","+cm.top+","+cm.bottom+",";
        s+=cm.roll_angle+","+cm.pitch_angle+","+cm.yaw_angle+",";
        for (int i=0;i<cm.landmarks.length;i++){
            s+=cm.landmarks[i]+",";
        }
        for (int i=0;i<cm.features.length-1;i++){
            s+=cm.features[i]+",";
        }
        s+=cm.features[cm.features.length-1];
        Log.v("CMSeetaFace2String", s);
        return s;
    }

    // 【格式转化】 String -> CMSeetaFace
    public static CMSeetaFace String2CMSeetaFace(String s) {
        CMSeetaFace cm = new CMSeetaFace();
        String[] ss = s.split(",");
        if (ss.length!=2065){
            Log.v("String2CMSeetaFace","not 2065"+"   "+ss.length+" "+s);
            return null;
        }
        cm.left= Integer.parseInt(ss[0]);
        cm.right= Integer.parseInt(ss[1]);
        cm.top= Integer.parseInt(ss[2]);
        cm.bottom= Integer.parseInt(ss[3]);
        cm.roll_angle= Float.parseFloat(ss[4]);
        cm.pitch_angle= Float.parseFloat(ss[5]);
        cm.yaw_angle= Float.parseFloat(ss[6]);
        for (int i=0;i<10;i++){
            cm.landmarks[i]=Integer.parseInt(ss[i+7]);
        }
        for (int i=0;i<2048;i++){
            cm.features[i]=Float.parseFloat(ss[i+17]);
        }

        return cm;
    }
}
