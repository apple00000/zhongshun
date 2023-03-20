package com.zhapplication.utils;

/*
*   系统路径变量
* */

import android.graphics.Bitmap;
import android.util.Log;

import com.zhapplication.model.compare.DetecteSeeta;
import com.zhapplication.model.detection.Classifier;

import java.io.File;
import java.util.ArrayList;

public class Common {
    // app存储根路径，需要在MainActivity中动态初始化
    public static String FilePath;

    // 人脸图片存储位置
    public static String FileFace;

    // 人脸比对模型
    // 模型文件存储位置
    public static String modelPath;
    // 三个模型文件
    public static String model1Path;
    public static String model2Path;
    public static String model3Path;
    // 人脸相似度临界值
    public static final float FaceSimilarityValue = 0.8f;

    // 目标检测模型
    public static final String TF_OD_API_MODEL_FILE = "file:///android_asset/frozen_inference_graph_v6.pb";
    public static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    public static final int TF_OD_API_INPUT_SIZE = 500;
    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    // 本地的bitmap人脸数据
    public static ArrayList<Bitmap> localFaceBitmapList = new ArrayList<>();
    // 目标检测分类器
    public static DetecteSeeta mDetecteSeeta = new DetecteSeeta();
    public static Classifier classifier;

    // 判断模型文件是否已经存在
    public static Boolean modelExist(){
        if (new File(model1Path).exists()&&new File(model2Path).exists()&&new File(model3Path).exists()){
            return true;
        }else{
            return false;
        }
    }

    // 验证人脸（通过本地人脸数据）
    public static Boolean verifyLoginFace(Bitmap b) {
        for (int i = 0; i < localFaceBitmapList.size(); i++){
            Bitmap localB = localFaceBitmapList.get(i);
            float y = mDetecteSeeta.getSimilarityNum(localB, b);
            Log.v("[verifyLoginFace]", i + "-" + String.valueOf(y));
            if (y > FaceSimilarityValue){
                return true;
            }
        }
        return false;
    }

    // 重置本地人脸文件
    public static void resetLocalFaceImageList(){
        ArrayList<Bitmap> tmpFaceList = new ArrayList<>();
        // 加载本地人脸数据
        ArrayList<String> file_list = fileUtil.getAllDataFileName(FileFace, "jpg");
        for (int i = 0;i<file_list.size();i++){
            Bitmap bm = fileUtil.openImage(FileFace + "/" +file_list.get(i));
            Bitmap bm2 = Pic.imageScale(bm, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);
            tmpFaceList.add(bm2);
        }

        localFaceBitmapList = tmpFaceList;
    }
}
