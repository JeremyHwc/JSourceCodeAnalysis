## OkHttp源码分析

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
直接将AsycCall添加到准备就绪的队列里面，即将被执行,而后紧接着调用pormoteAndExecute()方法
```
/**
   * Promotes eligible calls from {@link #readyAsyncCalls} to {@link #runningAsyncCalls} and runs
   * them on the executor service. Must not be called with synchronization because executing calls
   * can call into user code.
   *
   * @return true if the dispatcher is currently running calls.
   */
  private boolean promoteAndExecute() {
    assert (!Thread.holdsLock(this));

    List<AsyncCall> executableCalls = new ArrayList<>();//可执行Call集合
    boolean isRunning;
    synchronized (this) {
      for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
        AsyncCall asyncCall = i.next();

        if (runningAsyncCalls.size() >= maxRequests) break; // Max capacity.
        if (runningCallsForHost(asyncCall) >= maxRequestsPerHost) continue; // Host max capacity.

        i.remove();
        executableCalls.add(asyncCall);
        runningAsyncCalls.add(asyncCall);
      }
      isRunning = runningCallsCount() > 0;
    }

    for (int i = 0, size = executableCalls.size(); i < size; i++) {
      AsyncCall asyncCall = executableCalls.get(i);
      asyncCall.executeOn(executorService());//在线程池上面执行异步请求
    }

    return isRunning;
  }
```
从promoteAndExecute方法的注释可以看出，从readyAsyncCalls集合里面挑出符合条件的加入到runningAsyncCalls，
这里的条件就是正在执行的异步请求数小于maxRequests，并且最大的相同域名的请求数小于maxRequestsPerHost，
线程池中执行的AsyncCall继承自NamedRunnable，在NamedRunnable里面的run()方法，仅仅是做了简单的封装，调用
子类的execute()方法
```
@Override public final void run() {
    String oldName = Thread.currentThread().getName();
    Thread.currentThread().setName(name);
    try {
      execute();
    } finally {
      Thread.currentThread().setName(oldName);
    }
  }
```
所以我们直接看AsyncCall的execute()方法
```
@Override protected void execute() {
      boolean signalledCallback = false;
      timeout.enter();
      try {
        Response response = getResponseWithInterceptorChain();//这个在后面会讲，OkHttp请求流程就靠这个
        //从下面的代码可以看出，CallBack的onFailure和onResponse是在子线程当中调用的，所以我们如果有
        //更新UI等操作，一定记住要切换到主线程
        if (retryAndFollowUpInterceptor.isCanceled()) {
          signalledCallback = true;
          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
        } else {
          signalledCallback = true;
          responseCallback.onResponse(RealCall.this, response);
        }
      } catch (IOException e) {
        e = timeoutExit(e);
        if (signalledCallback) {
          // Do not signal the callback twice!
          Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
        } else {
          eventListener.callFailed(RealCall.this, e);
          responseCallback.onFailure(RealCall.this, e);
        }
      } finally {
      //将已经执行的call从runningAsyncCalls中移除，并再次调用promoteAndExecute(),执行readyAsyncCalls
      //里面的Call,readyAsyncCalls没有请求任务，就会idleCallback的run方法
        client.dispatcher().finished(this);
      }
    }
```

在Dispatcher里面有几个成员变量如下：
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

#### OkHttp的任务调度（Dispatcher）
疑问1：OkHttp如何实现同步异步请求？
解疑：发送的同步/异步请求都会在dispatcher中管理其状态
疑问2：到底什么是dispatcher
解疑：dispatcher的作用为维护请求的状态，并维护一个线程池，用于执行请求
疑问3：异步请求为什么需要两个队列？readyAsyncCalls和runningAsyncCalls
解疑：Dispatcher可以看做是生产者，ExecutorService看做消费者池，那么readyAsyncCalls就是生产者生产的，
      用于缓存，runningAsyncCalls就是消费者正在消费的仓库
    
#### OkHttp拦截器
OkHttp拦截器是OkHttp中提供一种强大机制，他可以实现网络监听、请求以及响应重写、请求失败重试等功能
![](https://github.com/JeremyHwc/JSourceCodeAnalysis/blob/master/demo-okhttp/images/OkHttp%E6%8B%A6%E6%88%AA%E5%99%A81.jpg) 
![](https://github.com/JeremyHwc/JSourceCodeAnalysis/blob/master/demo-okhttp/images/OkHttp%E6%8B%A6%E6%88%AA%E5%99%A82.jpg)
从上面的图可以看出，拦截器分为Application Interceptors 、OkHttpCore提供的拦截器 以及 Network Interceptors,第二张图是OkHttp框架给
我们提供的系统内部的拦截器。包括：
- RetryAndFollowUpInterceptor：初始化工作，创建StreamAllocation对象用来传递给后面的拦截器
- BridgeInterceptor：桥接和适配，用于补充一些Http请求当中缺少的Http的请求头
- CacheInterceptor：处理缓存
- ConnectInterceptor：建立可用的连接，是ConnectInterceptor的基础
- CallServerInterceptor：将Http请求写进网络得io流当中，并且从读取从服务端返回的io流当中的数据

#### OkHttp拦截器执行流程源码分析
从前面的分析中可以看出，不管是同步请求，还是异步请求，最终我们得到从服务器的响应都是通过
Response result = getResponseWithInterceptorChain()来得到，下面我们来看看getResponseWithInterceptorChain方法
```
Response getResponseWithInterceptorChain() throws IOException {
    // Build a full stack of interceptors.
    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.addAll(client.interceptors());//应用程序拦截器
    interceptors.add(retryAndFollowUpInterceptor);
    interceptors.add(new BridgeInterceptor(client.cookieJar()));
    interceptors.add(new CacheInterceptor(client.internalCache()));
    interceptors.add(new ConnectInterceptor(client));
    if (!forWebSocket) {
      interceptors.addAll(client.networkInterceptors());//网络拦截器
    }
    interceptors.add(new CallServerInterceptor(forWebSocket));

    Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
        originalRequest, this, eventListener, client.connectTimeoutMillis(),
        client.readTimeoutMillis(), client.writeTimeoutMillis());

    return chain.proceed(originalRequest);
  }
```
从getResponseWithInterceptorChain方法可以看出，这个方法就是形成一个拦截器的链，然后依次执行每一个不同
功能的拦截器来获取服务器的响应来返回

#### OkHttp拦截链RealInterceptorChain中proceed方法代码
```
public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec,
      RealConnection connection) throws IOException {
    if (index >= interceptors.size()) throw new AssertionError();

    calls++;

    // If we already have a stream, confirm that the incoming request will use it.
    if (this.httpCodec != null && !this.connection.supportsUrl(request.url())) {
      throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
          + " must retain the same host and port");
    }

    // If we already have a stream, confirm that this is the only call to chain.proceed().
    if (this.httpCodec != null && calls > 1) {
      throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
          + " must call proceed() exactly once");
    }

    // Call the next interceptor in the chain.
    RealInterceptorChain next = new RealInterceptorChain(interceptors, streamAllocation, httpCodec,
        connection, index + 1, request, call, eventListener, connectTimeout, readTimeout,
        writeTimeout);
    Interceptor interceptor = interceptors.get(index);
    Response response = interceptor.intercept(next);

    // Confirm that the next interceptor made its required call to chain.proceed().
    if (httpCodec != null && index + 1 < interceptors.size() && next.calls != 1) {
      throw new IllegalStateException("network interceptor " + interceptor
          + " must call proceed() exactly once");
    }

    // Confirm that the intercepted response isn't null.
    if (response == null) {
      throw new NullPointerException("interceptor " + interceptor + " returned null");
    }

    if (response.body() == null) {
      throw new IllegalStateException(
          "interceptor " + interceptor + " returned a response with no body");
    }

    return response;
  }
```
#### OkHttp拦截器总结
- 创建一系列的拦截器，并将其放入一个拦截器list中
- 创建一个拦截器链RealInterceptorChain，并执行拦截器链的proceed方法

#### OkHttp拦截器的intercept方法执行总结
- 在发起请求前对request进行处理
- 调用下一个拦截器，获取response
- 对response进行处理，返回给上一个拦截器

#### RetryAndFollowUpInterceptor解析
RetryAndFollowUpInterceptor中intercept方法如下：
```
@Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Call call = realChain.call();
    EventListener eventListener = realChain.eventListener();

    StreamAllocation streamAllocation = new StreamAllocation(client.connectionPool(),
        createAddress(request.url()), call, eventListener, callStackTrace);
    this.streamAllocation = streamAllocation;

    int followUpCount = 0;
    Response priorResponse = null;
    while (true) {//根据异常结果或者响应结果判断是否要进行重新请求
      if (canceled) {
        streamAllocation.release();
        throw new IOException("Canceled");
      }

      Response response;
      boolean releaseConnection = true;
      try {
        response = realChain.proceed(request, streamAllocation, null, null);
        releaseConnection = false;
      } catch (RouteException e) {
        // The attempt to connect via a route failed. The request will not have been sent.
        if (!recover(e.getLastConnectException(), streamAllocation, false, request)) {
          throw e.getFirstConnectException();
        }
        releaseConnection = false;
        continue;
      } catch (IOException e) {
        // An attempt to communicate with a server failed. The request may have been sent.
        boolean requestSendStarted = !(e instanceof ConnectionShutdownException);
        if (!recover(e, streamAllocation, requestSendStarted, request)) throw e;
        releaseConnection = false;
        continue;
      } finally {
        // We're throwing an unchecked exception. Release any resources.
        if (releaseConnection) {
          streamAllocation.streamFailed(null);
          streamAllocation.release();
        }
      }

      // Attach the prior response if it exists. Such responses never have a body.
      if (priorResponse != null) {
        response = response.newBuilder()
            .priorResponse(priorResponse.newBuilder()
                    .body(null)
                    .build())
            .build();
      }

      Request followUp;
      try {
        followUp = followUpRequest(response, streamAllocation.route());
      } catch (IOException e) {
        streamAllocation.release();
        throw e;
      }

      if (followUp == null) {
        streamAllocation.release();
        return response;
      }

      closeQuietly(response.body());

      if (++followUpCount > MAX_FOLLOW_UPS) {
        streamAllocation.release();
        throw new ProtocolException("Too many follow-up requests: " + followUpCount);
      }

      if (followUp.body() instanceof UnrepeatableRequestBody) {
        streamAllocation.release();
        throw new HttpRetryException("Cannot retry streamed HTTP body", response.code());
      }

      if (!sameConnection(response, followUp.url())) {
        streamAllocation.release();
        streamAllocation = new StreamAllocation(client.connectionPool(),
            createAddress(followUp.url()), call, eventListener, callStackTrace);
        this.streamAllocation = streamAllocation;
      } else if (streamAllocation.codec() != null) {
        throw new IllegalStateException("Closing the body of " + response
            + " didn't close its backing stream. Bad interceptor?");
      }

      request = followUp;
      priorResponse = response;
    }
  }
```
在上面代码中创建了StreamAllocation对象，这个对象实质上是用来建立执行Http请求所需要的那些网络组件，这里
我们注意到StreamAllocation虽然是在RetryAndFollowUpInterceptor中创建，但在这个拦截器中并没有使用到，真正
要使用到会在我们后面的ConnectInterceptor中，主要用于获取连接服务端Connection,连接和用于服务端进行数据传输
的输入输出流，通过拦截器链传递给ConnectInterceptor

#### RetryAndFollowUpInterceptor总结
- 创建StreamAllocation对象
- 调用RealInterceptorChain.proceed()方法进行网络请求
- 根据异常结果或者响应结果判断是否要进行重新请求
- 调用下一个拦截器，对response进行处理，返回给上一个拦截器

#### BridgeInterceptor解析
BridgeInterceptor的intercept方法如下：
```
@Override public Response intercept(Chain chain) throws IOException {
    Request userRequest = chain.request();
    Request.Builder requestBuilder = userRequest.newBuilder();

    RequestBody body = userRequest.body();
    if (body != null) {
      MediaType contentType = body.contentType();
      if (contentType != null) {
        requestBuilder.header("Content-Type", contentType.toString());
      }

      long contentLength = body.contentLength();
      if (contentLength != -1) {
        requestBuilder.header("Content-Length", Long.toString(contentLength));
        requestBuilder.removeHeader("Transfer-Encoding");
      } else {
        requestBuilder.header("Transfer-Encoding", "chunked");
        requestBuilder.removeHeader("Content-Length");
      }
    }

    if (userRequest.header("Host") == null) {
      requestBuilder.header("Host", hostHeader(userRequest.url(), false));
    }

    if (userRequest.header("Connection") == null) {
      requestBuilder.header("Connection", "Keep-Alive");
    }

    //告知服务端客户端支持gzip压缩
    // If we add an "Accept-Encoding: gzip" header field we're responsible for also decompressing
    // the transfer stream.
    boolean transparentGzip = false;
    if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
      transparentGzip = true;
      requestBuilder.header("Accept-Encoding", "gzip");
    }

    List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
    if (!cookies.isEmpty()) {
      requestBuilder.header("Cookie", cookieHeader(cookies));
    }

    if (userRequest.header("User-Agent") == null) {
      requestBuilder.header("User-Agent", Version.userAgent());
    }

    Response networkResponse = chain.proceed(requestBuilder.build());
    
    //对响应头中的cookie进行处理
    HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());

    Response.Builder responseBuilder = networkResponse.newBuilder()
        .request(userRequest);

    //对后端返回的response如果是经过gzip压缩过的，那么这里会对其进行解压
    if (transparentGzip
        && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
        && HttpHeaders.hasBody(networkResponse)) {
      GzipSource responseBody = new GzipSource(networkResponse.body().source());
      Headers strippedHeaders = networkResponse.headers().newBuilder()
          .removeAll("Content-Encoding")
          .removeAll("Content-Length")
          .build();
      responseBuilder.headers(strippedHeaders);
      String contentType = networkResponse.header("Content-Type");
      responseBuilder.body(new RealResponseBody(contentType, -1L, Okio.buffer(responseBody)));
    }

    return responseBuilder.build();
  }
```
从上面intercept方法可以看出，BridgeInterceptor方法主要负责用于设置内容长度，编码方式，压缩等等，主要是
添加头部的作用,其中有一个Keep-Alive，这是实现连接复用的基础，在连接池中会详细讲解，当我们开启一个TCP连接
以后，不会关闭链接，而是在一定时间内保持连接状态。

#### BridgeInterceptor总结
- 负责将用户构建的一个Request请求转化为能够进行网络访问的请求
- 将这个符合网络请求的Request进行网络请求
- 将网络请求回来的响应Response转化为用户可用的response,包括gzip解压

#### OkHttp缓存策略分析
OkHttp设置缓存
```
OkHttpClient client=new OkHttpClient.Builder()
                .cache(new Cache(new File("cache"),24*1024*1024))
                .build();
```
其中cache的参数是一个cache目录及其大小




    