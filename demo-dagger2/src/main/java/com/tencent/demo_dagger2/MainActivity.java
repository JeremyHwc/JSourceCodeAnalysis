package com.tencent.demo_dagger2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {
    @Inject
    Watch mWatch;
    @Inject
    Gson mGson;
    @Inject
    Gson mGson2;

    @Inject
    Car mCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DaggerMainActivityComponent.create().inject(this);
        mWatch.work();

        String jsonData="{'name':'zhangwuji','age':20}";
        Man man = mGson.fromJson(jsonData, Man.class);
        Log.d("DAGGER2",man.toString());

        String run = mCar.run();
        Log.d("DAGGER2",run);

        Log.d("DAGGER2",mGson.hashCode()+"");
        Log.d("DAGGER2",mGson2.hashCode()+"");


    }
}
