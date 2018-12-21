package com.tencent.demo_dagger2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class Watch1Activity extends AppCompatActivity {
    @Inject
    Watch1 mWatch1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DaggerWatch1Component.create().inject(this);
        String work = mWatch1.work();

    }
}
