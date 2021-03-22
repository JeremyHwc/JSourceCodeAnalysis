package com.tencent.demo_leakcanary;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

/**
 * 线程造成的内存泄漏
 *
 * 将AsyncTask都定义为静态内部类
 */
public class ThreadLeakActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_leak);
        testThreadLeak();
    }

    private void testThreadLeak() {
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                return null;
            }
        }.execute();

        new Thread(() -> SystemClock.sleep(10000)).start();
    }

    private static class MyAsyncTask extends AsyncTask<Void,Void,Void> {
        private WeakReference<Context> mContextWeakReference;

        public MyAsyncTask(Context context) {
            mContextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }

    private static class MyRunnable implements Runnable{
        @Override
        public void run() {
            SystemClock.sleep(10000);
        }
    }
}