package com.tencent.demo_glide;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String url = null;
        ImageView imageView = null;
        Glide.with(this).load(url).into(imageView);
    }

    public void loadImageSimple(ImageView view, String url) {
        Glide.with(this).load(url).into(view);
    }

    @SuppressWarnings("AlibabaAvoidCommentBehindStatement")
    public void loadImage(ImageView view, String url) {
        Glide
                .with(getApplicationContext()) // 指定Context，决定整个图片加载流程的生命周期
                .load(url) // 指定图片的URL

                .placeholder(R.mipmap.ic_launcher) // 指定图片未成功加载前显示的图片，本地资源
                .error(R.mipmap.ic_launcher) // 指定图片加载失败显示的图片，当然还有加载失败的回调

                .override(300, 300) // 指定图片的尺寸
                .fitCenter() // 指定图片缩放类型为fitCenter
                .centerCrop() // 指定图片缩放类型为centerCrop

                .skipMemoryCache(true) // 跳过内存缓存，还是会用磁盘缓存，避免频繁的网络请求图片数据
                .crossFade(1000) // 设置渐变式显示的时间
                .diskCacheStrategy(DiskCacheStrategy.NONE) // 跳过磁盘缓存
                .diskCacheStrategy(DiskCacheStrategy.SOURCE) // 仅仅只缓存原来的全分辨率的图像
                .diskCacheStrategy(DiskCacheStrategy.RESULT) // 仅仅缓存最终的图像
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 缓存所有版本的图像
                .priority(Priority.HIGH) // 指定优先级 Glide 将会用他们作为一个准则，并尽可能的处理这些请求，但并不一定按照这个顺序
                .into(view);
    }
}
