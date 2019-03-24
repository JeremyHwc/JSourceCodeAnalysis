package com.tencent.demo_glide;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class BitmapDemo {
    /**
     * 压缩Bitmap图片质量
     *
     * @param image Bitmap
     * @return 压缩以后的Bitmap
     */
    private Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {
            baos.reset();
            ;
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    /**
     * 根据宽高比例来压缩
     * @param image 待压缩的Bitmap
     * @return 返回压缩以后的Bitmap
     */
    private Bitmap compBitmapInSize(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream isBm;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//只返回宽和高，不必分配内存
        Bitmap bitmap;
        int w = options.outWidth;
        int h = options.outHeight;
        float hh = 1280f;//这里设置高度为1280f
        float ww = 720f;//这里设置宽度为720f
        int be = 1;
        if (w > h && 2 > ww) {
            be = (int) (options.outWidth / ww);

        } else if (w < h && h > hh) {
            be = (int) (options.outHeight / hh);
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        options.inJustDecodeBounds = false;
        isBm = new ByteArrayInputStream(baos.toByteArray());
        bitmap = BitmapFactory.decodeStream(isBm, null, options);
        return bitmap;
    }

    /**
     * 伪代码，捕获异常
     */
    public void handleBitmapCrash(){
        Bitmap bitmap;
        String path ="xxx/xxx/xxx";
        try {
            //实例化Bitmap
            bitmap=BitmapFactory.decodeFile(path);
        }catch (OutOfMemoryError e){
            e.printStackTrace();
        }
    }

}
