package com.tencent.demo_retrofit.static_proxy;

public class RealObject extends AbstractObject {
    @Override
    protected void operation() {
        System.out.println("do operation...");
    }
}
