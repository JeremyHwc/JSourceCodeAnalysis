package com.tencent.demo_dagger2;

import android.app.Application;
import android.content.Context;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
public class App extends Application {

    private ActivityComponent mActivityComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        mActivityComponent = DaggerActivityComponent.create();
    }

    public static App get(Context context){
        return (App)context.getApplicationContext();
    }

    public ActivityComponent getActivityComponent() {
        return mActivityComponent;
    }
}
