# LeakCanary解析

[toc]

## ActivityRefWatcher如何监视Activity

- 首先创建一个RefWatcher，启动一个ActivityRefWatcher；
- 通过ActivityLifecycleCallbacks把Activity的onDestroy生命周期关联；
- 最后在线程池中去开始分析内存泄漏。

## Hprof转换为Snapshot

- 把Hprof文件转换为Snapshot
- 优化GcRoots
- 找出泄漏的对象/找出泄漏对象的最短路径 

## 查找内存泄漏引用和最短泄漏路径

- 在Snapshot快照中找到第一个弱引用；
- 遍历这个对象的所有实例；
- 如果key值和最开始定义封装的key值相同，那么返回这个泄漏对象。

## 总结

- 解析Hprof文件，把这个文件封装成Snapshot；
- 根据弱引用和前面定义的key值，确定泄漏的对象；
- 找到最短泄漏路径，作为结果反馈出来。



# 面试题

1、Application应用场景。

- 初始化全局对象、环境配置变量；
- 获取应用程序当前的内存使用情况；
- 监听应用程序内所有Activity的生命周期；
- 监听应用程序配置信息的改变。

2、Android性能数据上报。

- 性能解决思路

  - 监控性能情况，包括CPU、内存、卡顿、网络流量等
  - 根据上报数据统计信息
  - 持续监控并观察

- 应用性能种类

  - 资源耗费，如电量、流量等
  - 流畅度，如数据加载速度、页面绘制、启动时间等

- 各个性能数据指标

  - 网络请求流量

    解决办法：

    - 在日常开发中可以通过TcpDump + WireShark抓包测试法，注意TcpDump抓包需要获得手机root权限；
    - TrafficStats类，该类其实就是读取linux相应文件，对文本内容进行解析。	

  - 冷启动

    - 冷启动、热启动的区别

    - 获取首页启动时间

      ```shell
      adb shell am start -W packagename/MainActivity
      ```

      日志打印：起点 -> 终点。其中起点定位为Application的onCreate方法，终点是首页Activity的onCreate方法。

  - 卡顿
    - FPS（帧率）
      - 通过Choreographer获取帧率信息，通过这个类可以设置FrameCallback；
      - VSYNC同步信号
      - 流畅度：实际帧率/理论帧率
    - 主线程消息处理时长
      - 主线程消息前后，都会用logging打印消息
      - 根据设备不同设定不同的阈值
      - 注意：根据logging来匹配，由于是做字符串匹配，会产生很多临时字符串，导致频繁GC，GC又会导致卡顿。
  - 内存占用
    - RAM：一般意义上的内存；
    - PSS：应用占用的实际物理内存；
    - Heap：虚拟机堆内存。