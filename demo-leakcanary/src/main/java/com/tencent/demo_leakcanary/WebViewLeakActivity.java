package com.tencent.demo_leakcanary;

import android.os.Bundle;
import android.os.Process;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * WebView 造成的内存泄漏
 *
 * WebView加载Html页面，WebView在解析网页时，会申请native堆内存去保存页面的元素
 * 当加载的页面非常复杂时，就会占用非常多的内存。如果页面中存在很多图片，那么内存占用就更加严重
 *
 */
public class WebViewLeakActivity extends AppCompatActivity {

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_leak);
        mWebView = findViewById(R.id.wv_show);
        mWebView.loadUrl("http://www.baidu.com");
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        Process.killProcess(Process.myPid());
        super.onDestroy();
    }

    private void destroyWebView() {
        if (mWebView != null) {
            mWebView.pauseTimers();
            mWebView.removeAllViews();
            mWebView.destroy();
        }
    }
}