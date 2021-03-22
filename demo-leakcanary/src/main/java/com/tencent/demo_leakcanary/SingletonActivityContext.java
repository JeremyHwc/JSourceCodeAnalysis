package com.tencent.demo_leakcanary;

import android.content.Context;

public class SingletonActivityContext {
    private static SingletonActivityContext sInstance;
    private Context mContext;

    private SingletonActivityContext(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static SingletonActivityContext getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SingletonActivityContext(context);
        }
        return sInstance;
    }
}
