package com.tencent.demo_dagger2;

import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
@Module
public class GsonModule {
    @Singleton
    @Provides
    public Gson provideGson(){
        return new Gson();
    }
}
