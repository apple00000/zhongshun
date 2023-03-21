package com.zhapplication.utils;

/*
*   文件相关操作
* */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class fileUtil {
    // 创建文件夹
    public static String createDir(String path){
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs(); // 创建父文件夹路径
        }
        if (!file.exists()){
            file.mkdirs();
        }
        return path;
    }

    // 拷贝到sd卡
    public static boolean copyAssetsToDst(Context context, String srcPath, String dstPath) {
        try {
            String fileNames[] = context.getAssets().list(srcPath);
            if (fileNames.length > 0) {
                File file = new File(dstPath);
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
                File outFile = new File(dstPath);
                InputStream is = context.getAssets().open(srcPath);
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 从指定路径的图片文件中读取位图数据
    public static Bitmap openImage(String path) {
        Bitmap bitmap = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            bitmap = BitmapFactory.decodeStream(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return bitmap;
    }

    // 生成文件名
    public static String getFileName() {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String fileName = Common.FileFace + "/" + timeStamp;
        return fileName;
    }

    // 保存图片
    public static void SaveImage(Image image, String fileName) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);
        File file = new File(Common.FileFace);
        //判断当前的文件目录是否存在，如果不存在就创建这个文件目录
        if (!file.exists()) {
            file.mkdir();
        }

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

    // 写文件
    public static void writeStrToFile(String filepath, String str) {
        File f=new File(filepath);
        if(!f.exists()) {
            try{
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream fos=null;
        DataOutputStream dos=null;
        try {
            fos = new FileOutputStream(f);
            dos = new DataOutputStream(fos);
            dos.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 读文件
    public static String readStrFromFile(String filepath) {
        String result = null;
        File f=new File(filepath);
        if(!f.exists()) {
            return "";
        } else {
            FileInputStream fis = null;
            DataInputStream dis = null;
            try {
                fis = new FileInputStream(f);//创建对应f的文件输入流
                dis = new DataInputStream(fis);
                result = dis.readUTF();//只有先用DataOutputStream的write方法写入合规则的数据后才能正常读出内容
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (result!=null) {
            return result;
        } else {
            return "";
        }
    }

    // 获取文件夹下所有指定类型文件
    public static ArrayList<String> getAllDataFileName(String folderPath, String type){
        ArrayList<String> fileList = new ArrayList<>();
        File file = new File(folderPath);
        File[] tempList = file.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
                String fileName = tempList[i].getName();
                if (fileName.endsWith(type)){
                    fileList.add(fileName);
                }
            }
        }
        return fileList;
    }
}