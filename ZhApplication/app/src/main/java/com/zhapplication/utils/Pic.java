package com.zhapplication.utils;

/*
    图片相关操作
* */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import com.zhapplication.model.compare.DetecteSeeta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import seetaface.CMSeetaFace;

import com.zhapplication.model.detection.Classifier;
import com.zhapplication.utils.Common;

import org.json.JSONException;
import org.json.JSONObject;

public class Pic {
    // 人脸特征
    public static class PicData {
        public int[] rect;     // android人脸检测边框属性
        public CMSeetaFace cm; // seetaFace人脸特征
    }

    // 得到最佳的预览尺寸
    public static Size getOptimalSize(Size[] sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    // 存储人脸特征
    public static void SaveCM(CMSeetaFace cm) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.putOpt("1", cm);
        String raw = obj.toString();
        Log.v("SaveCM", raw);
    }

    // 检测是否有人脸，并返回人脸坐标
    public static PicData getFace(Bitmap bitmap) {
        PicData res = new PicData();
        res.rect = new int[4];
        res.cm = new CMSeetaFace();

        if (null == bitmap){
            return null;
        }

        int numOfFaces;
        Bitmap tmpBmp = bitmap.copy(Bitmap.Config.RGB_565, true);
        FaceDetector mFaceDetector = new FaceDetector(tmpBmp.getWidth(), tmpBmp.getHeight(), 1);
        FaceDetector.Face[] mFace = new FaceDetector.Face[1];
        numOfFaces = mFaceDetector.findFaces(tmpBmp, mFace);
        // 未检测到人脸
        if (0==numOfFaces){
            return null;
        }

        CMSeetaFace cm = DetecteSeeta.getSeetaFaceValue(bitmap);
        if (null==cm){
            return null;
        }
        res.cm = cm;

        float eyesDistance = 0f;
        PointF eyeMidPoint = new PointF();
        mFace[0].getMidPoint(eyeMidPoint);
        eyesDistance = (float) (mFace[0].eyesDistance());
        res.rect[0] = (int)(eyeMidPoint.x-eyesDistance);
        res.rect[1] = (int)(eyeMidPoint.y-eyesDistance);
        res.rect[2] = (int)(eyeMidPoint.x+eyesDistance);
        res.rect[3] = (int)(eyeMidPoint.y+eyesDistance);

        return res;
    }

    // 矩形画
    public static Bitmap drawRect(int x1, int y1, int x2, int y2) {
        Bitmap croppedBitmap = Bitmap.createBitmap((int) Common.TF_OD_API_INPUT_SIZE_HEIGHT, (int) Common.TF_OD_API_INPUT_SIZE_HEIGHT, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
        paint.setAntiAlias(true);
        canvas.drawRect(x1,y1,x2,y2, paint);
        return croppedBitmap;
    }

    // bitmap 旋转
    public static Bitmap bitmapRotate(Bitmap bm, int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }

        final float[] values = new float[9];
        m.getValues(values);

        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];

        m.postTranslate(targetX - x1, targetY - y1);

        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);

        return bm1;
    }

    // bitmap尺寸变化
    public static Bitmap imageScale(Bitmap bitmap, int dst_w, int dst_h) {
        int src_w = bitmap.getWidth();
        int src_h = bitmap.getHeight();
        float scale_w = ((float) dst_w) / src_w;
        float scale_h = ((float) dst_h) / src_h;
        Matrix matrix = new Matrix();
        matrix.postScale(scale_w, scale_h);
        return Bitmap.createBitmap(bitmap, 0, 0, src_w, src_h, matrix,true);
    }

    // 识别 抽烟电话疲劳
    public static Bitmap classifyFrame(Bitmap bitmap, Bitmap croppedBitmap) {
        if (Common.classifier == null ) {
            return null;
        }

        final List<Classifier.Recognition> results = Common.classifier.recognizeImage(bitmap);

        // 获取眼睛位置
        float maxOpenEyes = 0;
        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (result.getConfidence() >= 0.9f) {
                if (result.getTitle().equals("openeyes")) {
                    maxOpenEyes = result.getConfidence();
                    break;
                }
            }
        }

        Boolean has_smoke = false;
        Boolean has_phone = false;

        final Canvas canvas = new Canvas(croppedBitmap);

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();

            if (result.getTitle().equals("closeeyes") && result.getConfidence() >= 0.95f && result.getConfidence()>maxOpenEyes+0.03){
                Log.v("qqq1", "closeeyes "+ result.getConfidence());
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5.0f);
                paint.setAntiAlias(true);
                paint.setColor(Color.RED);
                canvas.drawRect(location, paint);
            }

            if (result.getTitle().equals("smoke") && result.getConfidence() >= 0.05f && (!has_smoke)){
                has_smoke = true;
                Log.v("qqq1", "smoke "+ result.getConfidence());
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5.0f);
                paint.setAntiAlias(true);
                paint.setColor(Color.YELLOW);
                canvas.drawRect(location, paint);
            }

            if (result.getTitle().equals("phone") && result.getConfidence() >= 0.06f && (!has_phone)){
                has_phone = true;
                Log.v("qqq1", "phone "+ result.getConfidence());
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5.0f);
                paint.setAntiAlias(true);
                paint.setColor(Color.BLUE);
                canvas.drawRect(location, paint);
            }
        }

        return croppedBitmap;
    }
}
