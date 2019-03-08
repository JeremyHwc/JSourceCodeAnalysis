package com.tencent.demo_eventbus;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class EventBusSimpleUseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_bus_simple_use);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);//注册
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);//反注册
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MyBusEvent event){
        Toast.makeText(this,event.getMessage(),Toast.LENGTH_LONG).show();
    }

    public void onSendMessage(View view) {
        EventBus.getDefault().post(new MyBusEvent("我是来自EventBus发送的消息！"));
    }
}
