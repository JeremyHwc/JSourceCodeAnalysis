package com.tencent.demo_eventbus;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class HandlerSendMessageActivity2 extends AppCompatActivity {
    private static final String TAG = "HandlerSendMessage";
    private Handler mHandler;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handler_send_message2);
        Thread.currentThread().setName("我是主线程！");
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Toast.makeText(HandlerSendMessageActivity2.this, msg.obj.toString() + "\n" + Thread.currentThread().getName(), Toast.LENGTH_LONG).show();
            }
        };
    }

    public void sendMsgToMain(View view) {
        MyThread myThread = new MyThread();
        myThread.start();
    }

    class MyThread extends Thread {
        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            Message message = new Message();
            message.obj = "我来自子线程的消息！";
            mHandler.sendMessage(message);
        }
    }
}
