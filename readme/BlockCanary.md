# BlockCanary ui卡顿优化框架源码解析

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
    
## BlockCanary使用/阈值参数
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
```
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
        
## 7.BlockCanary面试二：WatchDog-anr如何监控ANR
