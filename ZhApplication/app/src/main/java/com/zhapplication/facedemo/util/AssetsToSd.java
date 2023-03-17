package com.zhapplication.facedemo.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 将Assets下的文件复制到SD卡
 * Created by shugu on 2018
 */

public class AssetsToSd {
    private static AssetsToSd instance;
    private static final int SUCCESS = 1;
    private static final int FAILED = 0;
    private Context context;
    private FileOperateCallback callback;
    private volatile boolean isSuccess;
    private String errorStr;

    public static AssetsToSd getInstance(Context context) {
        if (instance == null)
            instance = new AssetsToSd(context);
        return instance;
    }

    private AssetsToSd(Context context) {
        this.context = context;
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (callback != null) {
                if (msg.what == SUCCESS) {
                    callback.onSuccess();
                }
                if (msg.what == FAILED) {
                    callback.onFailed(msg.obj.toString());
                }
            }
        }
    };

    /**
     *
     * @param srcPath assets文件夹下的某个文件夹名
     * @param sdPath 你希望将文件复制到的位置
     * @return
     */
    public AssetsToSd copyAssetsToSD(final String srcPath, final String sdPath) {
//        System.out.println("srcPath ======> " + srcPath);
//        System.out.println("sdPath ======> " + sdPath);
        new Thread(new Runnable() {
            @Override
            public void run() {
                copyAssetsToDst(context, srcPath, sdPath);
                if (isSuccess)
                    handler.obtainMessage(SUCCESS).sendToTarget();
                else
                    handler.obtainMessage(FAILED, errorStr).sendToTarget();
            }
        }).start();
        return this;
    }

    public void setFileOperateCallback(FileOperateCallback callback) {
        this.callback = callback;
    }

    private void copyAssetsToDst(Context context, String srcPath, String dstPath) {
        try {
            String fileNames[] = context.getAssets().list(srcPath);
//            System.out.println("=======> "+Arrays.toString(fileNames));
            if (fileNames.length > 0) {
//                File file = new File(Environment.getExternalStorageDirectory(), dstPath);
                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM).getAbsolutePath(), dstPath);
                if (!file.exists()) file.mkdirs();
                System.out.println("-----> "+file);
                for (String fileName : fileNames) {
                    if (!srcPath.equals("")) { // assets 文件夹下的目录
                        copyAssetsToDst(context, srcPath + File.separator + fileName, dstPath + File.separator + fileName);
                    } else { // assets 文件夹
                        copyAssetsToDst(context, fileName, dstPath + File.separator + fileName);
                    }
                }
            } else {
//                File outFile = new File(Environment.getExternalStorageDirectory(), dstPath);
                File outFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM).getAbsolutePath(), dstPath);
                System.out.println("outfile =====>   "+outFile.toString());
                InputStream is = context.getAssets().open(srcPath);

//                this.getExternalFilesDir(Environment.DIRECTORY_DCIM).getAbsolutePath()
//                context.getExternalFilesDir()
                FileOutputStream fos = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            errorStr = e.getMessage();
            isSuccess = false;
        }
    }

    public interface FileOperateCallback {
        void onSuccess();

        void onFailed(String error);
    }
}
