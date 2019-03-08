package com.tencent.demo_eventbus.handlerthread;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.tencent.demo_eventbus.R;

public class HandlerSendMessageActivity1 extends AppCompatActivity {
    private static final String TAG = "HandlerSendMessage";
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handler_send_message1);
        MyThread myThread = new MyThread();
        myThread.setName("我是子线程1");
        myThread.start();
    }

    public void sendMsgToChild(View view) {
        Message message = new Message();
        message.obj = "我来自主线程的消息！";
        mHandler.sendMessage(message);
    }


    class MyThread extends Thread {
        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Toast.makeText(HandlerSendMessageActivity1.this,msg.obj.toString() + "\n" + Thread.currentThread().getName(),Toast.LENGTH_LONG).show();
                }
            };
            Looper.loop();
            super.run();
        }
    }
}
