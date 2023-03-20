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
import android.media.FaceDetector;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Pic {
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

    // 保存图片
    public static void SaveImage(Image image) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);
        File file = new File(Common.FileFace);
        //判断当前的文件目录是否存在，如果不存在就创建这个文件目录
        if (!file.exists()) {
            file.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String fileName = Common.FileFace + "/IMG_" + timeStamp + ".jpg";
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(fileName);
            fileOutputStream.write(data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 检测是否有人脸，并返回人脸坐标
    public static int[] getFace(Bitmap bitmap) {
        int[] res = new int[4];

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

        float eyesDistance = 0f;
        PointF eyeMidPoint = new PointF();
        mFace[0].getMidPoint(eyeMidPoint);
        eyesDistance = (float) (mFace[0].eyesDistance()*1.3);
        res[0] = (int)(eyeMidPoint.x-eyesDistance);
        res[1] = (int)(eyeMidPoint.y-eyesDistance);
        res[2] = (int)(eyeMidPoint.x+eyesDistance);
        res[3] = (int)(eyeMidPoint.y+eyesDistance);
        return res;
    }

    // 矩形画
    public static Bitmap drawRect(int x1, int y1, int x2, int y2) {
        Bitmap croppedBitmap = Bitmap.createBitmap((int) 300, (int) 300, Bitmap.Config.ARGB_8888);
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
    public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,(float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
            }
        }
        return b;
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
}
