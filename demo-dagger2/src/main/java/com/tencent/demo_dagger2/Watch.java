package com.tencent.demo_dagger2;

import android.util.Log;

import javax.inject.Inject;

/**
 * author: Jeremy
 * date: 2018/7/5
 * desc:
 */
public class Watch {
    @Inject
    public Watch(){

    }

    public void work(){
        Log.d("DAGGER2","手表工作");
    }
}
