package com.tencent.demo_dagger2;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 定义新的注解类型使用@interface
 *
 * 注意：注解只有成员变量，没有方法。
 * 定义注解的成员变量：成员变量在注解定义中以“无形参的方法”形式来声明，其“方法名”定义了该成员变
 *                     量的名字，其返回值定义了该成员变量的类型；
 *                     定义了成员变量后，使用该注解时就应该为该注解的成员变量指定值，
 *                     也可以使用default关键字为期指定默认值,如果注解定义了默认值，所以使用时可以不为指定了
 *                     默认值的成员变量指定值
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Swordsman {
    String name() default "Swordsman";
    int age();
}
