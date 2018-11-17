# OkHttp源码分析

## 一、OkHttp流程

### 涉及内容：
OkHttpClient     
Request     
RealCall        
Dispatcher      
Intercepters:
    包括RetryAndFollowIntercepter,BridgeIntercepter,CacheIntercepter,ConnectIntercepter,CallServerInterceper
    
### OkHttp同步方法总结：
- 创建OkHttpClient和Request对象
- 将Request封装成Call对象
- 调用Call的execute()方法发送同步请求

*注意：OkHttp发送同步请求后，就会进入阻塞状态，直到收到响应，也就是说当前线程发送同步请求后，就会进
入阻塞状态，直到收到数据响应，异步请求会重新创建一个工作线程，不会阻塞当前的线程*

### OkHttp请求流程如下：
![](https://github.com/JeremyHwc/JSourceCodeAnalysis/blob/master/demo-okhttp/images/OkHttp%E8%AF%B7%E6%B1%82%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)