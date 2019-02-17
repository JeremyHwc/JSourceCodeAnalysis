package com.tencent.demo_retrofit;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.tencent.demo_retrofit.dynamic_proxy.Man;
import com.tencent.demo_retrofit.dynamic_proxy.Proxy;
import com.tencent.demo_retrofit.dynamic_proxy.Subject;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")//设置网络请求的Url地址
                .addConverterFactory(GsonConverterFactory.create())//设置数据解析转换器，其中GsonConverterFactory.create()就是创建了一个含有Gson对象的GsonConverterFactory
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())//网络请求的适配器，支持RxJava平台
                .build();

        GitHubService gitHubService = retrofit.create(GitHubService.class);

        Call<List<Repo>> repos = gitHubService.listRepos("xxxxx");

        //同步请求
        try {
            Response<List<Repo>> listResponse = repos.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //异步请求
        repos.enqueue(new Callback<List<Repo>>() {
            @Override
            public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {

            }

            @Override
            public void onFailure(Call<List<Repo>> call, Throwable t) {

            }
        });

        //动态代理
        Subject man = new Man();
        Proxy p = new Proxy(man);
        //通过java.lang.reflect.newProxyInstance(...)方法获得真实对象的代理对象
        Subject subject =
                (Subject) java.lang.reflect.Proxy.newProxyInstance(
                        man.getClass().getClassLoader(),
                        man.getClass().getInterfaces(),
                        p);
        //通过代理对象调用真实对象相关接口中实现的方法，这个是不就会跳转到这个代理对象所关联的handler的invoke方法
        subject.shopping();
        //获得真实对象的代理对象对应的Class对象的名称，用字符串表示
        System.out.println(subject.getClass().getName());
    }
}
