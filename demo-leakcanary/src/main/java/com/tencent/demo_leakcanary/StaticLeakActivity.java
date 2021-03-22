package com.tencent.demo_leakcanary;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class StaticLeakActivity extends AppCompatActivity {

    private static NoneStaticClass sNoneStaticClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_static_leak);
        if (sNoneStaticClass == null) {
            sNoneStaticClass = new NoneStaticClass();
        }
    }

    private class NoneStaticClass {

    }
}