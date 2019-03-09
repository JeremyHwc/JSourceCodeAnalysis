package com.tencent.demo_blockcanary;

/**
 * 饿汉单例
 */
public class Singleton {
    private static Singleton INSTANCE = new Singleton();

    private Singleton() {

    }

    public static Singleton getInstance() {
        return INSTANCE;
    }
}
