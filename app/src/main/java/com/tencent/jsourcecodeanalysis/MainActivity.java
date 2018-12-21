package com.tencent.jsourcecodeanalysis;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.tencent.annotations.BindView;

public class MainActivity extends AppCompatActivity {
    @BindView(value = R.id.tv_text)
    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
