package com.tencent.demo_dagger2;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Diesel {

}
