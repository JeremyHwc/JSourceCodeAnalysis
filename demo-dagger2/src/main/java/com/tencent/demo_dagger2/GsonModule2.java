package com.tencent.demo_dagger2;

import com.google.gson.Gson;

import dagger.Module;
import dagger.Provides;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
@Module
public class GsonModule2 {
    @ApplicationScope
    @Provides
    public Gson provideGson(){
        return new Gson();
    }
}
