package com.tencent.demo_retrofit.dynamic_proxy;


import org.junit.jupiter.api.Test;

public class ProxyTest {

    /**
     * <result>
     *     proxy:com.sun.proxy.$Proxy9
     *     before...
     *     Mji要去买东西...
     *     after...
     *     com.sun.proxy.$Proxy9
     * </result>
     */
    @Test
    public void invoke() {
        //动态代理
        Subject man = new Man();
        Proxy p = new Proxy(man);
        //通过java.lang.reflect.newProxyInstance(...)方法获得真实对象的代理对象
        Subject subject =
                (Subject) java.lang.reflect.Proxy.newProxyInstance(
                        man.getClass().getClassLoader(),
                        man.getClass().getInterfaces(),
                        p);
        //通过代理对象调用真实对象相关接口中实现的方法，这个是不就会跳转到这个代理对象所关联的handler的invoke方法
        subject.shopping();
        //获得真实对象的代理对象对应的Class对象的名称，用字符串表示
        System.out.println(subject.getClass().getName());
    }
}