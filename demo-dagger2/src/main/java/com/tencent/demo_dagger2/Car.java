package com.tencent.demo_dagger2;

import javax.inject.Inject;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
public class Car {
    private Engine engine;

    @Inject
    public Car(@Diesel Engine engine) {
        this.engine = engine;
    }

    public String run(){
        return engine.work();
    }
}
