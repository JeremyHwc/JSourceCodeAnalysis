package com.tencent.demo_okhttp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * OkHttp同步请求
     */
    public void synRequest(){
        OkHttpClient client=new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url("http://www.baidu.com").get().build();
        Call call = client.newCall(request);
        try{
            Response response = call.execute();
            Log.i("OKHTTP",response.body().string());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * OkHttp异步请求
     */
    public void asyncRequest(){
        OkHttpClient client=new OkHttpClient.Builder()
                .cache(new Cache(new File("cache"),24*1024*1024))
                .build();
        Request request = new Request.Builder().url("http://www.baidu.com").get().build();
        Call call = client.newCall(request);
        try{
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i("OKHTTP",response.body().string());
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
