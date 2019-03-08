package com.tencent.demo_eventbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.tencent.demo_eventbus.handlerthread.HandlerSendMessageActivity1;
import com.tencent.demo_eventbus.handlerthread.HandlerSendMessageActivity2;

import org.greenrobot.eventbus.EventBus;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onChildToMainThread(View view) {
        startActivity(new Intent(this,HandlerSendMessageActivity2.class));
    }

    public void onMainToChildThread(View view) {
        startActivity(new Intent(this,HandlerSendMessageActivity1.class));

    }

    public void onSimpleUse(View view) {
        startActivity(new Intent(this,EventBusSimpleUseActivity.class));
    }
}
