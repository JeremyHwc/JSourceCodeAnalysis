package com.tencent.demo_dagger2;

import dagger.Module;
import dagger.Provides;

@Module
public class Watch1Module {
    @Provides
    public Watch1 provideWatch(){
        return new Watch1();
    }
}


