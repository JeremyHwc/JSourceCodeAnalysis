package com.tencent.demo_dagger2;

import dagger.Component;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
@ApplicationScope
@Component(modules = {GsonModule2.class})
public interface ActivityComponent {
    void inject(SecondActivity activity);
    void inject(ThirdActivity activity);
}
