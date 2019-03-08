package com.tencent.demo_eventbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(View v) {
        EventBus.getDefault().post(null);
    }

    public void onMainToMain(View view) {
        EventBus.getDefault().post(new MessageEvent("main","main"));
    }

    public void onMainToChild(View view) {

    }

    public void onChildToMain(View view) {
    }

    public void onChildToChild(View view) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){

    }

    public void onChildToMainThread(View view) {
        startActivity(new Intent(this,HandlerSendMessageActivity2.class));

    }

    public void onMainToChildThread(View view) {
        startActivity(new Intent(this,HandlerSendMessageActivity1.class));

    }
}
