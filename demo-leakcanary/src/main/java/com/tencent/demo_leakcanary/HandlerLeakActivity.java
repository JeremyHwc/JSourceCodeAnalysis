package com.tencent.demo_leakcanary;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Handler造成内存泄漏
 *
 * Handler、Message、MessageQueue
 *
 * TLS
 */
public class HandlerLeakActivity extends AppCompatActivity {

    private final Handler mLeakHandler = new Handler(){
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handler_leak);
        mLeakHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        },10*60*1000);
        finish();
    }

    /**
     * 解决办法
     *
     * 1. 将Handler声明为静态的
     * 2. 通过弱引用的方式引入Activity
     */
}