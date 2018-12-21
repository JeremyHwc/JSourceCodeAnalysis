package com.tencent.demo_butterknife;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_content1)
    TextView tvContent1;

    @BindViews({R.id.tv_content1, R.id.tv_content2})
    List<TextView> tvList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }


    @OnClick({R.id.tv_content1, R.id.tv_content2})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_content1:
                break;
            case R.id.tv_content2:
                break;
        }
    }
}
