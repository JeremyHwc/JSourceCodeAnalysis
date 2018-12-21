package com.tencent.demo_dagger2;

import dagger.Component;

@Component(modules = Watch1Module.class)
public interface Watch1Component {
    void inject(Watch1Activity activity);
}
