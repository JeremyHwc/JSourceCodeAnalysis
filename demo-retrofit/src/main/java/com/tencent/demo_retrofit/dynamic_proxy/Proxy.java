package com.tencent.demo_retrofit.dynamic_proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * InvocationHandler是由代理实例的调用处理程序实现的接口。
 * 每个代理实例都有一个关联的调用处理程序。
 * 当在代理实例上调用方法时，该方法调用将被编码并分配到其调用处理程序的invoke方法。
 */
public class Proxy implements InvocationHandler {
    private Object target;//要代理的真实对象

    public Proxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("proxy:" + proxy.getClass().getName());
        System.out.println("before...");
        method.invoke(target, args);
        System.out.println("after...");
        return null;
    }
}
