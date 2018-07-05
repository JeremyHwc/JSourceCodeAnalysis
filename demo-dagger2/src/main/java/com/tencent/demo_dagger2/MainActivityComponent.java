package com.tencent.demo_dagger2;

import javax.inject.Singleton;

import dagger.Component;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
@Singleton
@Component(modules = {GsonModule.class, EngineModule.class})
public interface MainActivityComponent {
    void inject(MainActivity mainActivity);
}
