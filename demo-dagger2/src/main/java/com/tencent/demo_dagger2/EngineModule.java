package com.tencent.demo_dagger2;

import dagger.Module;
import dagger.Provides;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
@Module
public class EngineModule {

    @Provides
    @Gasoline
    public Engine provideGasoline(){
        return new GasolineEngine();
    }

    @Provides
    @Diesel
    public Engine provideDiesel(){
        return new DieselEngine();
    }
}
