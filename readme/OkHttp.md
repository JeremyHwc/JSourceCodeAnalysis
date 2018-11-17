# OkHttp源码分析

## 一、OkHttp流程

### 涉及内容
OkHttpClient     
Request     
RealCall        
Dispatcher      
Intercepters:
    包括RetryAndFollowIntercepter,BridgeIntercepter,CacheIntercepter,ConnectIntercepter,CallServerInterceper

#### OkHttp请求流程如下
![](https://github.com/JeremyHwc/JSourceCodeAnalysis/blob/master/demo-okhttp/images/OkHttp%E8%AF%B7%E6%B1%82%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)

#### OkHttp同步请求代码
```
public void synRequest(){
        OkHttpClient client=new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url("http://www.baidu.com").get().build();
        Call call = client.newCall(request);
        try{
            Response response = call.execute();
            Log.i("OKHTTP",response.body().string());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
```

#### OkHttp同步请求的执行流程和源码分析
在执行同步请求我们调用Response response = call.execute();其中call其实是RealCall的实例对象，下面这段
代码是RealCall里面的execute()方法
```
@Override public Response execute() throws IOException {
    synchronized (this) {//1.同一个Http请求只能被执行一次，否则抛出异常，通过executed予以保证，执行过了就是true
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    captureCallStackTrace();
    timeout.enter();
    eventListener.callStart(this);
    try {
      client.dispatcher().executed(this);//2.Dispather将call对象添加到
      Response result = getResponseWithInterceptorChain();//3.
      if (result == null) throw new IOException("Canceled");
      return result;
    } catch (IOException e) {
      e = timeoutExit(e);
      eventListener.callFailed(this, e);
      throw e;
    } finally {
      client.dispatcher().finished(this);
    }
  }
```
```
下面这部分是Dispatcher里面的部分代码：
  /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
  private final Deque<RealCall> runningSyncCalls = new ArrayDeque<>();
    
/** Used by {@code Call#execute} to signal it is in-flight. */
  synchronized void executed(RealCall call) {
    runningSyncCalls.add(call);
  }
```

#### OkHttp同步方法总结
- 创建OkHttpClient和Request对象
- 将Request封装成Call对象
- 调用Call的execute()方法发送同步请求

#### OkHttp异步请求代码
```
public void asynRequest(){
        OkHttpClient client=new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url("http://www.baidu.com").get().build();
        Call call = client.newCall(request);
        try{
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i("OKHTTP",response.body().string());
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }
```

#### OkHttp异步请求的执行流程和源码分析
同样地，异步OkHttp请求，通过RealCall.enqueue(Callback),以下是enqueue()方法
```
@Override public void enqueue(Callback responseCallback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    captureCallStackTrace();
    eventListener.callStart(this);
    client.dispatcher().enqueue(new AsyncCall(responseCallback));
  }
```
从上面的enqueue()方法中，最后调用了Dispatcher的enqueue(new AsyncCall(responseCallback))，其中AsyncCall是
将我们传递进去的CallBack对象封装成了AsyncCall,实质上就是一个Runnable对象，Dispatcher的enqueue方法如下：
```
void enqueue(AsyncCall call) {
    synchronized (this) {
      readyAsyncCalls.add(call);
    }
    promoteAndExecute();
  }
```
直接将AsycCall添加到准备就绪的队列里面，即将被执行,在Dispatcher里面有几个成员变量如下：
```
  private int maxRequests = 64;//最大请求书
  private int maxRequestsPerHost = 5;//每个主机名的最大请求数
  private @Nullable Runnable idleCallback;

  /** Executes calls. Created lazily. */
  private @Nullable ExecutorService executorService;//用于执行calls的线程池

  /** Ready async calls in the order they'll be run. */
  private final Deque<AsyncCall> readyAsyncCalls = new ArrayDeque<>();//准备就绪的异步请求，即将被执行

  /** Running asynchronous calls. Includes canceled calls that haven't finished yet. */
  private final Deque<AsyncCall> runningAsyncCalls = new ArrayDeque<>();//正在执行的异步请求或还未完成但被取消的队列

  /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
  private final Deque<RealCall> runningSyncCalls = new ArrayDeque<>();//正在执行的同步请求或还未完成但被取消的队列
```

#### OkHttp异步方法总结
- 创建OkHttpClient和Request对象
- 将Request封装成Call对象
- 调用Call的enqueue()方法发送异步请求

*注意：OkHttp发送同步请求后，就会进入阻塞状态，直到收到响应，也就是说当前线程发送同步请求后，就会进
入阻塞状态，直到收到数据响应，异步请求会重新创建一个工作线程，不会阻塞当前的线程。
对于异步请求来说，onResponse()和onFailure()都是在子线程当中执行的。*

#### OkHttp同、异步请求的区别
- 发起请求的方法调用
- 阻塞线程与否



    