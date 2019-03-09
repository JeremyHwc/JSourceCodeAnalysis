package com.tencent.demo_blockcanary;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread() {
            @Override
            public void run() {
                super.run();
            }
        }.start();
    }
}
