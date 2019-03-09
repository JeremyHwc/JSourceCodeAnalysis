package com.tencent.demo_blockcanary;


import android.app.Application;

import com.github.anrwatchdog.ANRWatchDog;
import com.github.moduth.blockcanary.BlockCanary;

public class DemoBlockApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 在主进程初始化调用哈
        BlockCanary.install(this, new AppBlockCanaryContext()).start();

        //ANR_WatchDog
        new ANRWatchDog().start();
    }
}
