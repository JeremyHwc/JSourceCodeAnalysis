package com.tencent.demo_retrofit.static_proxy;

public class ProxyObject extends AbstractObject {
    private RealObject mRealObject;

    public ProxyObject(RealObject realObject) {
        mRealObject = realObject;
    }

    @Override
    protected void operation() {
        System.out.println("do something before real operation...");
        if (mRealObject == null) {
            mRealObject = new RealObject();
        }
        mRealObject.operation();
        System.out.println("do something after real operation...");
    }
}
