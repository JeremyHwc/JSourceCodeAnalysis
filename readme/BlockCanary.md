# BlockCanary UI卡顿优化框架源码解析

## 1.BlockCanary背景/UI卡顿原理/UI卡顿常见原因
1. 背景   
    .复杂的项目  
    .ANR,我们才能得到当前堆栈信息
2. BlockCanary  
    .非侵入式的性能监控组件    
    .UI卡顿问题
3. UI卡顿原理   
    .60fps -> 16ms/帧
    .准则：尽量保证每次在16ms内处理完所有的CPU与GPU计算、绘制、渲染等操作，否则会造成丢帧卡顿的问题
4. UI差吨常见原因     
    (1)UI线程做了耗时的操作
    
        .主线程的作用？把时间分发给合适的view或者widget   
        .Handler    
        .Activity.runOnUiThread(Runnable)
        .View.post(Runnable)    
        .View.postDelayed(Runnable,long)   
         
    (2)布局Layout过于复杂，无法在16ms内完成渲染    
    (3)View过度绘制
    (4)View频繁的出发measure、layout
    (5)内存频繁出发GC过多，比如频繁创建临时变量（虚拟机进行GC的时候，所有的线程都会暂停，就会造成UI卡顿）
    
## 2.BlockCanary使用/阈值参数
1. 添加依赖     
```
    dependencies {
            compile 'com.github.markzhai:blockcanary-android:1.5.0'
        
            // 仅在debug包启用BlockCanary进行卡顿监控和提示的话，可以这么用
            debugCompile 'com.github.markzhai:blockcanary-android:1.5.0'
            releaseCompile 'com.github.markzhai:blockcanary-no-op:1.5.0'
        }
```
2. 初始化
```java
    public class DemoBlockApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            // 在主进程初始化调用哈
            BlockCanary.install(this, new AppBlockCanaryContext()).start();
        }
    }
    
    
    public class AppBlockCanaryContext extends BlockCanaryContext {
        // 实现各种上下文，包括应用标示符，用户uid，网络类型，卡慢判断阙值，Log保存位置等
    
        /**
         * Implement in your project.
         *
         * @return Qualifier which can specify this installation, like version + flavor.
         */
        public String provideQualifier() {
            return "unknown";
        }
    
        /**
         * Implement in your project.
         *
         * @return user id
         */
        public String provideUid() {
            return "uid";
        }
    
        /**
         * Network type
         *
         * @return {@link String} like 2G, 3G, 4G, wifi, etc.
         */
        public String provideNetworkType() {
            return "unknown";
        }
    
        /**
         * Config monitor duration, after this time BlockCanary will stop, use
         * with {@code BlockCanary}'s isMonitorDurationEnd
         *
         * @return monitor last duration (in hour)
         */
        public int provideMonitorDuration() {
            return -1;
        }
    
        /**
         * Config block threshold (in millis), dispatch over this duration is regarded as a BLOCK. You may set it
         * from performance of device.
         *
         * @return threshold in mills
         */
        public int provideBlockThreshold() {
            return 1000;
        }
    
        /**
         * Thread stack dump interval, use when block happens, BlockCanary will dump on main thread
         * stack according to current sample cycle.
         * <p>
         * Because the implementation mechanism of Looper, real dump interval would be longer than
         * the period specified here (especially when cpu is busier).
         * </p>
         *
         * @return dump interval (in millis)
         */
        public int provideDumpInterval() {
            return provideBlockThreshold();
        }
    
        /**
         * Path to save log, like "/blockcanary/", will save to sdcard if can.
         *
         * @return path of log files
         */
        public String providePath() {
            return "/blockcanary/";
        }
    
        /**
         * If need notification to notice block.
         *
         * @return true if need, else if not need.
         */
        public boolean displayNotification() {
            return true;
        }
    
        /**
         * Implement in your project, bundle files into a zip file.
         *
         * @param src  files before compress
         * @param dest files compressed
         * @return true if compression is successful
         */
        public boolean zip(File[] src, File dest) {
            return false;
        }
    
        /**
         * Implement in your project, bundled log files.
         *
         * @param zippedFile zipped file
         */
        public void upload(File zippedFile) {
            throw new UnsupportedOperationException();
        }
    
    
        /**
         * Packages that developer concern, by default it uses process name,
         * put high priority one in pre-order.
         *
         * @return null if simply concern only package with process name.
         */
        public List<String> concernPackages() {
            return null;
        }
    
        /**
         * Filter stack without any in concern package, used with @{code concernPackages}.
         *
         * @return true if filter, false it not.
         */
        public boolean filterNonConcernStack() {
            return false;
        }
    
        /**
         * Provide white list, entry in white list will not be shown in ui list.
         *
         * @return return null if you don't need white-list filter.
         */
        public List<String> provideWhiteList() {
            LinkedList<String> whiteList = new LinkedList<>();
            whiteList.add("org.chromium");
            return whiteList;
        }
    
        /**
         * Whether to delete files whose stack is in white list, used with white-list.
         *
         * @return true if delete, false it not.
         */
        public boolean deleteFilesInWhiteList() {
            return true;
        }
    
        /**
         * Block interceptor, developer may provide their own actions.
         */
        public void onBlock(Context context, BlockInfo blockInfo) {
    
        }
    }
```

## 3.BlockCanary核心原理实现和流程图简述
1. 核心原理
    其实BlockCanary的核心实现原理离不开主线程，Android当中的主线程是ActivityThread，以及它会利用到我
    们常用的线程处理工具Handler和Looper。下面分析其源码看其是如何打印出我们所需要的信息展示给用户的。 
```java
    public final class ActivityThread{
    //...........................................省略了其他的代码
    public static void main(String[] args) {
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "ActivityThreadMain");
            SamplingProfilerIntegration.start();
    
            // CloseGuard defaults to true and can be quite spammy.  We
            // disable it here, but selectively enable it later (via
            // StrictMode) on debug builds, but using DropBox, not logs.
            CloseGuard.setEnabled(false);
    
            Environment.initForCurrentUser();
    
            // Set the reporter for event logging in libcore
            EventLogger.setReporter(new EventLoggingReporter());
    
            // Make sure TrustedCertificateStore looks in the right place for CA certificates
            final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
            TrustedCertificateStore.setDefaultUserDirectory(configDir);
    
            Process.setArgV0("<pre-initialized>");
    
            Looper.prepareMainLooper();
    
            ActivityThread thread = new ActivityThread();
            thread.attach(false);
    
            if (sMainThreadHandler == null) {
                sMainThreadHandler = thread.getHandler();
            }
    
            if (false) {
                Looper.myLooper().setMessageLogging(new
                        LogPrinter(Log.DEBUG, "ActivityThread"));
            }
    
            // End of event ActivityThreadMain.
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            Looper.loop();
    
            throw new RuntimeException("Main thread loop unexpectedly exited");
        }
    }
```
```java
    public final class Looper{
    //.....................................省略其他代码
    /**
         * Run the message queue in this thread. Be sure to call
         * quit() to end the loop.
         */
        public static void loop() {
            final Looper me = myLooper();
            if (me == null) {
                throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
            }
            final MessageQueue queue = me.mQueue;
    
            // Make sure the identity of this thread is that of the local process,
            // and keep track of what that identity token actually is.
            Binder.clearCallingIdentity();
            final long ident = Binder.clearCallingIdentity();
    
            for (;;) {
                Message msg = queue.next(); // might block
                if (msg == null) {
                    // No message indicates that the message queue is quitting.
                    return;
                }
    
                // This must be in a local variable, in case a UI event sets the logger
                final Printer logging = me.mLogging;
                if (logging != null) {
                    logging.println(">>>>> Dispatching to " + msg.target + " " +
                            msg.callback + ": " + msg.what);
                }
    
                final long slowDispatchThresholdMs = me.mSlowDispatchThresholdMs;
    
                final long traceTag = me.mTraceTag;
                if (traceTag != 0 && Trace.isTagEnabled(traceTag)) {
                    Trace.traceBegin(traceTag, msg.target.getTraceName(msg));
                }
                final long start = (slowDispatchThresholdMs == 0) ? 0 : SystemClock.uptimeMillis();
                final long end;
                try {
                    msg.target.dispatchMessage(msg);
                    end = (slowDispatchThresholdMs == 0) ? 0 : SystemClock.uptimeMillis();
                } finally {
                    if (traceTag != 0) {
                        Trace.traceEnd(traceTag);
                    }
                }
                if (slowDispatchThresholdMs > 0) {
                    final long time = end - start;
                    if (time > slowDispatchThresholdMs) {
                        Slog.w(TAG, "Dispatch took " + time + "ms on "
                                + Thread.currentThread().getName() + ", h=" +
                                msg.target + " cb=" + msg.callback + " msg=" + msg.what);
                    }
                }
    
                if (logging != null) {
                    logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
                }
    
                // Make sure that during the course of dispatching the
                // identity of the thread wasn't corrupted.
                final long newIdent = Binder.clearCallingIdentity();
                if (ident != newIdent) {
                    Log.wtf(TAG, "Thread identity changed from 0x"
                            + Long.toHexString(ident) + " to 0x"
                            + Long.toHexString(newIdent) + " while dispatching to "
                            + msg.target.getClass().getName() + " "
                            + msg.callback + " what=" + msg.what);
                }
    
                msg.recycleUnchecked();
            }
        }
    }
```
可以看到，在应用程序启动过程中，ActivityThread作为程序的UI线程，在ActivityThread的main函数当中调用了Looper.loop()
从而开启一个for(;;)进而不断从MessageQueue里面获取到msg，从而调用msg.target.dispatchMessage(msg)，其中msg.target是
Handler,进而进入到Handler的dispatchMessage()方法，
```java
    public class Handler{
    //..................................省略其他的代码
        /**
         * Handle system messages here.
         */
        public void dispatchMessage(Message msg) {
            if (msg.callback != null) {//这里的callback其实就是runnable,
                handleCallback(msg);
            } else {
                if (mCallback != null) {
                    if (mCallback.handleMessage(msg)) {
                        return;
                    }
                }
                handleMessage(msg);//Handler处理消息，如果这个里面进行了耗时的操作，肯定就会阻塞UI线程
            }
        }
    }
```
其中BlockCanary的原理就是在msg.target.dispatchMessage(msg)方法的上下方去分别打印方法执行的时间，然后根据上下两个
时间差来判定dispatchMessage当中是否产生了耗时的操作，也就是dispatchMessage当中是否有UI卡顿，如果上下两个时间差大
于了我们设定的阈值，我们就需要dump出我们所需要的导致UI卡顿的堆栈信息


## 6.BlockCanary面试一：ANR/原因/解决
    (1)ANR造成原因？
        .ANR:Application Not Responding
        .Activity Manager 和 WindowManager
        
        .ANR分类  
            .Service TimeOut
            .BroadCast TimeOut
            .InputDispaching TimeOut
        
        .ANR造成原因
            .主线程在做一些耗时的工作
            .主线程被其他线程锁
            .CPU被其他进程占用
            
        .ANR解决方式
            .主线程读取数据（Android现在是规定我们不能从主线程中去获得我们的网络数据，但是主线程中可以从数据库获取数据，系统没规定）
            .Tip:SharedPreference的commit()/apply()方法，其中commit()方法是同步的，会阻塞主线程，最好用apply()方法
            .不要在BroadCastReceiver的onReceive()方法做耗时操作（如果有耗时操作直接开启一个子线程或通过IntentService来完成操作）
            .Activity的声明周期函数中都不应该有太耗时的操作
        
## 7.BlockCanary面试二：WatchDog-ANR如何监控ANR
    (1)ANR原理
        .创建一个检测线程；
        .该线程不断往UI线程post一个任务；
        .睡眠固定时间；
        .等检测线程重新起来后检测之前post的任务是否执行了。

    (2)使用
        .implementation 'com.github.anrwatchdog:anrwatchdog:1.4.0'
        
## 8.BlockCanary面试三：new Thread开启线程的4个弊端
    (1)代码
```
    new Thread() {
                @Override
                public void run() {
                    super.run();
                }
            }.start();
```
    (2)问题
        .多个耗时任务时就会开启多个新线程
        .如果在线程中执行循环任务，只能通过一个Flag来控制它的停止
        .没有线程切换的接口
        .如果从UI线程启动，则该线程优先级默认为Default
        .Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGOUND)
        
## 9.BlockCanary面试四：线程间通信：子线程-->UI线程
    (1)多线程编程时的两大原则
        .不要阻塞UI线程
        .不要再UI线程之外访问UI组件
    (2)线程通信
        .将任务从工作线程抛到主线程
        .将任务从主线程抛到工作线程
        .Handler.sendMessage();
        .Handler.post(Runnable);
        .runOnUiThread(Runnable);注意：调用这句话的时候，如果当前线程是UI线程,那么该Runnable会被立即执行，如果调用时不时UI线程，那么Runnable不会被立即执行，而是将Runnable放进消息队列，按照一定策略来执行
        .AsyncTask();注意：AsyncTask会默认持有外部类的引用，所以使用时记得自定义AsyncTask并申明为static，在其内部持有外部类的弱引用，防止内存泄漏
                           AsyncTask在3.0之后执行任务是串行的，主要原因在doInBackground里面，在一个进程当中我们如果开启了多个AsyncTask线程的时候，其会使用同一个线程池去
                           执行任务，如果在doInBackground当中需要去访问相同的资源，这时候就有可能出现数据不安全的现象。所以AsyncTask采用串行执行任务。
                           
               
## 10.BlockCanary面试五：主线程--> 子线程（HandlerThread - IntentService）
    (1)HandlerThread
        (1.1)Thread/Runnable
            .Runnable作为匿名内部类会持有外部类的引用
        (1.2)HandlerThread
            .继承了Thread
            .有自己的内部Looper对象，通过Looper.loop()进行looper循环
            .HandlerThread的looper对象传递给Handler对象，然后在handleMessage方法中执行异步任务
    (2)IntentService
        .IntentService是Service类的子类
        .单独开启了一个线程来出来所有的Intent请求所对应的任务
        .当IntentService处理完所有的任务后，它会在适当的时候自动结束服务
        .IntentServie是使用HandlerThread来实现异步执行任务的
        
## 11.BlockCanary面试六：多进程的4点好处与问题/volatile关键字
    (1)多进程好处
        .解决OOM问题
        .合理利用内存（在适当的时候生成进程，在不需要的时候杀掉进程）
        .单一进程奔溃不会影响整体应用
        .项目解耦、模块化开发
        
    (2)多进程问题
        .Application会多次创建（根据进程名区分不同进程，进行不同的初始化，所以不要再Application当中做过多的初始化）
        .文件读写潜在的问题（并发访问文件，包括我们的SP，本地文件，数据库文件等等，在java当中，文件锁都是基于进程，java虚拟机级别的，
         所以从不同进程访问文件，这时候这个文件锁是不管用的，特别是SP）
        .静态变量和单例模式完全失效
        .线程同步机制完全失效
        
    (3)synchronized和volatile
        .阻塞线程与否（volatile是告诉虚拟机当前变量在寄存器当中的值是不确定的，应该从主存中去读取，在单例当中使用很多，synchronized关键字会阻塞线程，只有获取到锁的线程可以访问，如果其他线程同时也在访问，那么其他线程将会被阻塞）
        .使用范围（volatile只是作用在变量上面，synchronized不仅可以作用在变量上面，也可以作用于方法上）
        .操作原子性（volatile没有原子性，而synchronized具有原子性）
        
## 12.BlockCanary面试七：volatile关键字和单例的写法
```java
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
```