package com.tencent.demo_blockcanary;

public class SingletonLazy {
    private static volatile SingletonLazy INSTANCE = null;

    private SingletonLazy() {
    }

    /**
     * 线程不安全的懒汉模式
     */
    public static SingletonLazy getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SingletonLazy();
        }
        return INSTANCE;
    }

    /**
     * 线程安全的懒汉模式,但是缺点是非必要的同步
     */
    public static synchronized SingletonLazy getInstance1() {
        if (INSTANCE == null) {
            INSTANCE = new SingletonLazy();
        }
        return INSTANCE;
    }

    /**
     * DCL,指令重排序
     */
    public static SingletonLazy getInstance2() {
        if (INSTANCE == null) {
            synchronized (SingletonLazy.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SingletonLazy();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 静态内部类
     */

    private static class SingletonLazyHolder {
        private static final SingletonLazy sInstance = new SingletonLazy();
    }

    public static SingletonLazy getInstance3() {
        return SingletonLazyHolder.sInstance;
    }
}
