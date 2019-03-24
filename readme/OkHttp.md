## OkHttp源码分析

### 涉及内容
OkHttpClient     
Request     
RealCall        
Dispatcher      
Intercepters:
    包括RetryAndFollowIntercepter,BridgeIntercepter,CacheIntercepter,ConnectIntercepter,CallServerInterceper
    
以上的这部分内容可以从OkHttp的这段源码中getResponseWithInterceptorChain方法中得出：
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
      interceptors.addAll(client.networkInterceptors());
    }
    interceptors.add(new CallServerInterceptor(forWebSocket));

    Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
        originalRequest, this, eventListener, client.connectTimeoutMillis(),
        client.readTimeoutMillis(), client.writeTimeoutMillis());

    return chain.proceed(originalRequest);
  }
```

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
从上面的图可以看出，拦截器分为Application Interceptors 、OkHttpCore提供的拦截器 以及 Network Interceptors,
第二张图是OkHttp框架给我们提供的系统内部的拦截器。包括：
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

OkHttp内部维持一个清理的线程池，用这个线程池来实现对缓存文件的自动清理和管理工作

```
if (!requestMethod.equals("GET")) {
      // Don't cache non-GET responses. We're technically allowed to cache
      // HEAD requests and some POST requests, but the complexity of doing
      // so is high and the benefit is low.
      return null;
}
```
通过以上的代码块可以看出，OkHttp不会缓存非GET方式的请求数据
```
final InternalCache internalCache = new InternalCache() {
    //从缓存当中获取缓存
    @Override public Response get(Request request) throws IOException {
      return Cache.this.get(request);
    }

    //对数据进行缓存
    @Override public CacheRequest put(Response response) throws IOException {
      return Cache.this.put(response);
    }
    
    //移除缓存
    @Override public void remove(Request request) throws IOException {
      Cache.this.remove(request);
    }
    
    //更新缓存
    @Override public void update(Response cached, Response network) {
      Cache.this.update(cached, network);
    }

    @Override public void trackConditionalCacheHit() {
      Cache.this.trackConditionalCacheHit();
    }

    @Override public void trackResponse(CacheStrategy cacheStrategy) {
      Cache.this.trackResponse(cacheStrategy);
    }
  };
```

##### Cache的put方法分析
```
@Nullable CacheRequest put(Response response) {
    String requestMethod = response.request().method();

    if (HttpMethod.invalidatesCache(response.request().method())) {
      try {
        remove(response.request());
      } catch (IOException ignored) {
        // The cache cannot be written.
      }
      return null;
    }
    if (!requestMethod.equals("GET")) {
      // Don't cache non-GET responses. We're technically allowed to cache
      // HEAD requests and some POST requests, but the complexity of doing
      // so is high and the benefit is low.
      return null;
    }

    if (HttpHeaders.hasVaryAll(response)) {
      return null;
    }

    Entry entry = new Entry(response);
    DiskLruCache.Editor editor = null;
    try {
      editor = cache.edit(key(response.request().url()));
      if (editor == null) {
        return null;
      }
      entry.writeTo(editor);
      return new CacheRequestImpl(editor);
    } catch (IOException e) {
      abortQuietly(editor);
      return null;
    }
  }
```
##### Cache的get方法分析
```
@Nullable Response get(Request request) {
    String key = key(request.url());//更具url获取缓存的key值
    DiskLruCache.Snapshot snapshot;
    Entry entry;
    try {
      snapshot = cache.get(key);
      if (snapshot == null) {
        return null;
      }
    } catch (IOException e) {
      // Give up because the cache cannot be read.
      return null;
    }

    try {
      entry = new Entry(snapshot.getSource(ENTRY_METADATA));
    } catch (IOException e) {
      Util.closeQuietly(snapshot);
      return null;
    }

    Response response = entry.response(snapshot);

    if (!entry.matches(request, response)) {
      Util.closeQuietly(response.body());
      return null;
    }

    return response;
  }
```

#### CacheInterceptor解析
```
 @Override public Response intercept(Chain chain) throws IOException {
    Response cacheCandidate = cache != null
        ? cache.get(chain.request())//获取缓存
        : null;

    long now = System.currentTimeMillis();

    CacheStrategy strategy = new CacheStrategy.Factory(now, chain.request(), cacheCandidate).get();
    Request networkRequest = strategy.networkRequest;
    Response cacheResponse = strategy.cacheResponse;

    if (cache != null) {
      cache.trackResponse(strategy);
    }

    if (cacheCandidate != null && cacheResponse == null) {
      closeQuietly(cacheCandidate.body()); // The cache candidate wasn't applicable. Close it.
    }

    // If we're forbidden from using the network and the cache is insufficient, fail.
    if (networkRequest == null && cacheResponse == null) {//无网络，无缓存就构建一个504的Response返回
      return new Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(504)
          .message("Unsatisfiable Request (only-if-cached)")
          .body(Util.EMPTY_RESPONSE)
          .sentRequestAtMillis(-1L)
          .receivedResponseAtMillis(System.currentTimeMillis())
          .build();
    }

    // If we don't need the network, we're done.
    if (networkRequest == null) {//有缓存直接返回缓存的结果
      return cacheResponse.newBuilder()
          .cacheResponse(stripBody(cacheResponse))
          .build();
    }

    Response networkResponse = null;
    try {
      networkResponse = chain.proceed(networkRequest);//调用拦截器的proceed方法，进行网络数据获取
    } finally {
      // If we're crashing on I/O or otherwise, don't leak the cache body.
      if (networkResponse == null && cacheCandidate != null) {
        closeQuietly(cacheCandidate.body());
      }
    }

    // If we have a cache response too, then we're doing a conditional get.
    if (cacheResponse != null) {//既有缓存，又有网络响应，首先做对比
      if (networkResponse.code() == HTTP_NOT_MODIFIED) {//网络响应码为304，表示数据未变化
        Response response = cacheResponse.newBuilder()
            .headers(combine(cacheResponse.headers(), networkResponse.headers()))
            .sentRequestAtMillis(networkResponse.sentRequestAtMillis())
            .receivedResponseAtMillis(networkResponse.receivedResponseAtMillis())
            .cacheResponse(stripBody(cacheResponse))
            .networkResponse(stripBody(networkResponse))
            .build();
        networkResponse.body().close();

        // Update the cache after combining headers but before stripping the
        // Content-Encoding header (as performed by initContentStream()).
        cache.trackConditionalCacheHit();
        cache.update(cacheResponse, response);
        return response;
      } else {
        closeQuietly(cacheResponse.body());
      }
    }

    Response response = networkResponse.newBuilder()
        .cacheResponse(stripBody(cacheResponse))
        .networkResponse(stripBody(networkResponse))
        .build();

    if (cache != null) {
      if (HttpHeaders.hasBody(response) && CacheStrategy.isCacheable(response, networkRequest)) {
        // Offer this request to the cache.
        CacheRequest cacheRequest = cache.put(response);
        return cacheWritingResponse(cacheRequest, response);
      }

      if (HttpMethod.invalidatesCache(networkRequest.method())) {
        try {
          cache.remove(networkRequest);
        } catch (IOException ignored) {
          // The cache cannot be written.
        }
      }
    }

    return response;
  }
```

#### ConnectInterceptor解析

作用：ConnectInterceptor就是打开与服务器的连接，正式开启网络连接
```
  @Override public Response intercept(Chain chain) throws IOException {
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Request request = realChain.request();
    //在前面分析的RetryAndFollowUpInterceptor中创建StreamAllocation，StreamAllocation是用来建立执行
    //http请求所需要的网络的组件，分配Stream
    StreamAllocation streamAllocation = realChain.streamAllocation();

    // We need the network to satisfy this request. Possibly for validating a conditional GET.
    boolean doExtensiveHealthChecks = !request.method().equals("GET");
    //HttpCodec用于编码Request 和 解码Response
    HttpCodec httpCodec = streamAllocation.newStream(client, chain, doExtensiveHealthChecks);
    //RealConnection用于实际的网络io传输，
    RealConnection connection = streamAllocation.connection();

    return realChain.proceed(request, streamAllocation, httpCodec, connection);
  }
```
总结：
1. ConnectInterceptor获取Interceptor传过来的StreamAllocation,streamAllocation.newStream()得到HttpCodec；
2. 将刚才创建的用于网络IO得RealConnection对象，以及对于与服务器交互最为关键的HttpCodec等对象传递给后面的拦截器。

以下分析以下上端代码中的StreamAllocation的newStream(client, chain, doExtensiveHealthChecks)方法
```
public HttpCodec newStream(OkHttpClient client, Interceptor.Chain chain, boolean doExtensiveHealthChecks) {
    int connectTimeout = chain.connectTimeoutMillis();
    int readTimeout = chain.readTimeoutMillis();
    int writeTimeout = chain.writeTimeoutMillis();
    int pingIntervalMillis = client.pingIntervalMillis();
    boolean connectionRetryEnabled = client.retryOnConnectionFailure();

    try {
      RealConnection resultConnection = findHealthyConnection(connectTimeout, readTimeout,
          writeTimeout, pingIntervalMillis, connectionRetryEnabled, doExtensiveHealthChecks);//再分析该方法
      HttpCodec resultCodec = resultConnection.newCodec(client, chain, this);

      synchronized (connectionPool) {
        codec = resultCodec;
        return resultCodec;
      }
    } catch (IOException e) {
      throw new RouteException(e);
    }
  }
  
  /**
     * Finds a connection and returns it if it is healthy. If it is unhealthy the process is repeated
     * until a healthy connection is found.
     * 以上的意思就是说寻找一个健康的RealConnection，其实就是从conectionPool里面去寻找
     */
    private RealConnection findHealthyConnection(int connectTimeout, int readTimeout,
        int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled,
        boolean doExtensiveHealthChecks) throws IOException {
      while (true) {
        RealConnection candidate = findConnection(connectTimeout, readTimeout, writeTimeout,
            pingIntervalMillis, connectionRetryEnabled);//以下在对findConnection进行分析
  
        // If this is a brand new connection, we can skip the extensive health checks.
        synchronized (connectionPool) {
          if (candidate.successCount == 0) {
            return candidate;
          }
        }
  
        // Do a (potentially slow) check to confirm that the pooled connection is still good. If it
        // isn't, take it out of the pool and start again.
        if (!candidate.isHealthy(doExtensiveHealthChecks)) {
          noNewStreams();
          continue;
        }
  
        return candidate;
      }
    }
    
    private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout,
          int pingIntervalMillis, boolean connectionRetryEnabled) throws IOException {
        boolean foundPooledConnection = false;
        RealConnection result = null;
        Route selectedRoute = null;
        Connection releasedConnection;
        Socket toClose;
        synchronized (connectionPool) {
          if (released) throw new IllegalStateException("released");
          if (codec != null) throw new IllegalStateException("codec != null");
          if (canceled) throw new IOException("Canceled");
    
          // Attempt to use an already-allocated connection. We need to be careful here because our
          // already-allocated connection may have been restricted from creating new streams.
          //尝试去使用一个已经分配的连接，也就是复用连接，这里需要注意的是，已经分配的连接可能被限制去创建新的流
          releasedConnection = this.connection;
          toClose = releaseIfNoNewStreams();
          if (this.connection != null) {
            // We had an already-allocated connection and it's good.
            result = this.connection;
            releasedConnection = null;
          }
          if (!reportedAcquired) {
            // If the connection was never reported acquired, don't report it as released!
            releasedConnection = null;
          }
    
          if (result == null) {
            // Attempt to get a connection from the pool.
            // 从连接池里面获取RealConnection
            Internal.instance.get(connectionPool, address, this, null);
            if (connection != null) {
              foundPooledConnection = true;
              result = connection;
            } else {
              selectedRoute = route;
            }
          }
        }
        closeQuietly(toClose);
    
        if (releasedConnection != null) {
          eventListener.connectionReleased(call, releasedConnection);
        }
        if (foundPooledConnection) {
          eventListener.connectionAcquired(call, result);
        }
        if (result != null) {
          // If we found an already-allocated or pooled connection, we're done.
          return result;
        }
    
        // If we need a route selection, make one. This is a blocking operation.
        boolean newRouteSelection = false;
        if (selectedRoute == null && (routeSelection == null || !routeSelection.hasNext())) {
          newRouteSelection = true;
          routeSelection = routeSelector.next();
        }
    
        synchronized (connectionPool) {
          if (canceled) throw new IOException("Canceled");
    
          if (newRouteSelection) {
            // Now that we have a set of IP addresses, make another attempt at getting a connection from
            // the pool. This could match due to connection coalescing.
            List<Route> routes = routeSelection.getAll();
            for (int i = 0, size = routes.size(); i < size; i++) {
              Route route = routes.get(i);
              Internal.instance.get(connectionPool, address, this, route);
              if (connection != null) {
                foundPooledConnection = true;
                result = connection;
                this.route = route;
                break;
              }
            }
          }
    
          if (!foundPooledConnection) {
            if (selectedRoute == null) {
              selectedRoute = routeSelection.next();
            }
    
            // Create a connection and assign it to this allocation immediately. This makes it possible
            // for an asynchronous cancel() to interrupt the handshake we're about to do.
            route = selectedRoute;
            refusedStreamCount = 0;
            result = new RealConnection(connectionPool, selectedRoute);
            acquire(result, false);
          }
        }
    
        // If we found a pooled connection on the 2nd time around, we're done.
        if (foundPooledConnection) {
          eventListener.connectionAcquired(call, result);
          return result;
        }
    
        // Do TCP + TLS handshakes. This is a blocking operation.
        // TCP,TLS握手，这是一个阻塞操作，做实际的网络连接，以下针对该方法进行分析
        result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis,
            connectionRetryEnabled, call, eventListener);
        routeDatabase().connected(result.route());
    
        Socket socket = null;
        synchronized (connectionPool) {
          reportedAcquired = true;
    
          // Pool the connection.
          // 将连接成功后的RealConnection放入连接池ConnectionPool中
          Internal.instance.put(connectionPool, result);
    
          // If another multiplexed connection to the same address was created concurrently, then
          // release this connection and acquire that one.
          if (result.isMultiplexed()) {
            socket = Internal.instance.deduplicate(connectionPool, address, this);
            result = connection;
          }
        }
        closeQuietly(socket);
    
        eventListener.connectionAcquired(call, result);
        return result;
      }
  
  
  public void connect(int connectTimeout, int readTimeout, int writeTimeout,
        int pingIntervalMillis, boolean connectionRetryEnabled, Call call,
        EventListener eventListener) {
        //验证连接是否已经建立，已经建立，就抛出异常
      if (protocol != null) throw new IllegalStateException("already connected");
  
      RouteException routeException = null;
      List<ConnectionSpec> connectionSpecs = route.address().connectionSpecs();
      ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(connectionSpecs);
  
      if (route.address().sslSocketFactory() == null) {
        if (!connectionSpecs.contains(ConnectionSpec.CLEARTEXT)) {
          throw new RouteException(new UnknownServiceException(
              "CLEARTEXT communication not enabled for client"));
        }
        String host = route.address().url().host();
        if (!Platform.get().isCleartextTrafficPermitted(host)) {
          throw new RouteException(new UnknownServiceException(
              "CLEARTEXT communication to " + host + " not permitted by network security policy"));
        }
      } else {
        if (route.address().protocols().contains(Protocol.H2_PRIOR_KNOWLEDGE)) {
          throw new RouteException(new UnknownServiceException(
              "H2_PRIOR_KNOWLEDGE cannot be used with HTTPS"));
        }
      }
  
      while (true) {
        try {
          //是否需要建立tunnel，内部进行的判断是address.sslSocketFactory != null && proxy.type() == Proxy.Type.HTTP
          if (route.requiresTunnel()) {
            //该方法可以完成在代理通道上构建HTTPS连接的所有工作。这里的问题是代理服务器可以发出身份
            //验证挑战，然后关闭连接。
            connectTunnel(connectTimeout, readTimeout, writeTimeout, call, eventListener);
            if (rawSocket == null) {
              // We were unable to connect the tunnel but properly closed down our resources.
              break;
            }
          } else {
            connectSocket(connectTimeout, readTimeout, call, eventListener);
          }
          establishProtocol(connectionSpecSelector, pingIntervalMillis, call, eventListener);
          eventListener.connectEnd(call, route.socketAddress(), route.proxy(), protocol);
          break;
        } catch (IOException e) {
          closeQuietly(socket);
          closeQuietly(rawSocket);
          socket = null;
          rawSocket = null;
          source = null;
          sink = null;
          handshake = null;
          protocol = null;
          http2Connection = null;
  
          eventListener.connectFailed(call, route.socketAddress(), route.proxy(), null, e);
  
          if (routeException == null) {
            routeException = new RouteException(e);
          } else {
            routeException.addConnectException(e);
          }
  
          if (!connectionRetryEnabled || !connectionSpecSelector.connectionFailed(e)) {
            throw routeException;
          }
        }
      }
  
      if (route.requiresTunnel() && rawSocket == null) {
        ProtocolException exception = new ProtocolException("Too many tunnel connections attempted: "
            + MAX_TUNNEL_ATTEMPTS);
        throw new RouteException(exception);
      }
  
      if (http2Connection != null) {
        synchronized (connectionPool) {
          allocationLimit = http2Connection.maxConcurrentStreams();
        }
      }
    }
```

总结以上调用流程：
* 弄一个RealConnection对象
* 根据是否需要隧道连接，选择不同连接方式，一种是隧道连接，一种是原始的socket连接
* CallServerInterceptor

##### ConnectionPool解析
```
Manages reuse of HTTP and HTTP/2 connections for reduced network latency. HTTP requests that
share the same {@link Address} may share a {@link Connection}. This class implements the policy
of which connections to keep open for future use.
```
以上这部分大致意思就是：
    对HTTP和HTTP/2的连接进行管理复用，以便减少网络请求的延迟。那些共用同一个Address的Http 请求，
    可以共用一个连接。ConnectionPool这个类实现了保持那些Connection 为open状态，以便未来复用。
    
```
//这是一个用于清楚过期链接的线程池，每个线程池最多只能运行一个线程，并且这个线程池允许被垃圾回收
private static final Executor executor = new ThreadPoolExecutor(0 /* corePoolSize */,
      Integer.MAX_VALUE /* maximumPoolSize */, 60L /* keepAliveTime */, TimeUnit.SECONDS,
      new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp ConnectionPool", true));

  /** The maximum number of idle connections for each address. */
  //每个address的最大空闲连接数。
  private final int maxIdleConnections;
  private final long keepAliveDurationNs;
  //清理任务
  private final Runnable cleanupRunnable = new Runnable() {
    @Override public void run() {
      while (true) {
        //调用cleanup方法执行清理，并等待一段时间，持续清理，其中cleanup方法返回的值来来决定而等待的时间长度
        long waitNanos = cleanup(System.nanoTime());
        if (waitNanos == -1) return;
        if (waitNanos > 0) {
          long waitMillis = waitNanos / 1000000L;
          waitNanos -= (waitMillis * 1000000L);
          synchronized (ConnectionPool.this) {
            try {
              ConnectionPool.this.wait(waitMillis, (int) waitNanos);
            } catch (InterruptedException ignored) {
            }
          }
        }
      }
    }
  };
  //链接的双向队列
  private final Deque<RealConnection> connections = new ArrayDeque<>();
  //路由的数据库,用来记录不可用的route
  final RouteDatabase routeDatabase = new RouteDatabase();
  //清理任务正在执行的标志
  boolean cleanupRunning;
  
  //创建一个适用于单个应用程序的新连接池。
  //该连接池的参数将在未来的okhttp中发生改变
  //目前最多可容乃5个空闲的连接，存活期是5分钟
  public ConnectionPool() {
      this(5, 5, TimeUnit.MINUTES);
    }
    
```

```
public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
      this.maxIdleConnections = maxIdleConnections;
      this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);
    
      // Put a floor on the keep alive duration, otherwise cleanup will spin loop.
      //保持活着的时间，否则清理将旋转循环
      if (keepAliveDuration <= 0) {
        throw new IllegalArgumentException("keepAliveDuration <= 0: " + keepAliveDuration);
      }
}
```
```
    /**
     * Returns a recycled connection to {@code address}, or null if no such connection exists. The
     * route is null if the address has not yet been routed.
     */
    @Nullable RealConnection get(Address address, StreamAllocation streamAllocation, Route route) {
      //判断线程是不是被自己锁住了
      assert (Thread.holdsLock(this));
      // 遍历已有连接集合
      for (RealConnection connection : connections) {
       //如果connection和需求中的"地址"和"路由"匹配
        if (connection.isEligible(address, route)) {
        //复用这个连接
          streamAllocation.acquire(connection, true);
          //返回这个连接
          return connection;
        }
      }
      return null;
    }
```

```
/**
*异步触发清理任务，然后将连接添加到队列中
*/
void put(RealConnection connection) {
    assert (Thread.holdsLock(this));
    if (!cleanupRunning) {
      cleanupRunning = true;
      executor.execute(cleanupRunnable);
    }
    connections.add(connection);
  }
```

```
private final Runnable cleanupRunnable = new Runnable() {
    @Override public void run() {
      while (true) {
        //调用cleanup方法执行清理，并等待一段时间，持续清理，其中cleanup方法返回的值来来决定而等待的时间长度
        long waitNanos = cleanup(System.nanoTime());
        if (waitNanos == -1) return;
        if (waitNanos > 0) {
          long waitMillis = waitNanos / 1000000L;
          waitNanos -= (waitMillis * 1000000L);
          synchronized (ConnectionPool.this) {
            try {
              ConnectionPool.this.wait(waitMillis, (int) waitNanos);
            } catch (InterruptedException ignored) {
            }
          }
        }
      }
    }
  };
```

```
  /**
   * Performs maintenance on this pool, evicting the connection that has been idle the longest if
   * either it has exceeded the keep alive limit or the idle connections limit.
   *
   * <p>Returns the duration in nanos to sleep until the next scheduled call to this method. Returns
   * -1 if no further cleanups are required.
   */
  long cleanup(long now) {
    int inUseConnectionCount = 0;
    int idleConnectionCount = 0;
    RealConnection longestIdleConnection = null;
    long longestIdleDurationNs = Long.MIN_VALUE;

    // Find either a connection to evict, or the time that the next eviction is due.
    // 找到即将被清理的 连接 或者下一次清理的 时间
    synchronized (this) {
      //iterator返回connections队列的RealConnection，顺序为队头到队尾
      for (Iterator<RealConnection> i = connections.iterator(); i.hasNext(); ) {
        RealConnection connection = i.next();

        // If the connection is in use, keep searching.
        //使用中的连接，则找一下个Connection
        if (pruneAndGetAllocationCount(connection, now) > 0) {
          inUseConnectionCount++;
          continue;
        }
        //空闲连接数
        idleConnectionCount++;

        // If the connection is ready to be evicted, we're done.
        //该Connection空闲的总时间
        long idleDurationNs = now - connection.idleAtNanos;
        //找出空闲时间最长的连接以及对应的空闲时间
        if (idleDurationNs > longestIdleDurationNs) {
          longestIdleDurationNs = idleDurationNs;
          longestIdleConnection = connection;
        }
      }

      if (longestIdleDurationNs >= this.keepAliveDurationNs || idleConnectionCount > this.maxIdleConnections) {
        // We've found a connection to evict. Remove it from the list, then close it below (outside of the synchronized block).
        //在符合清理条件下，清理空闲时间最长的连接
        connections.remove(longestIdleConnection);
      } else if (idleConnectionCount > 0) {
        // A connection will be ready to evict soon.
        //不符合清理条件，则返回下次需要执行清理的等待时间，也就是此连接即将到期的时间
        return keepAliveDurationNs - longestIdleDurationNs;
      } else if (inUseConnectionCount > 0) {
        // All connections are in use. It'll be at least the keep alive duration 'til we run again.
        //没有空闲的连接，则隔keepAliveDuration(分钟)之后再次执行
        return keepAliveDurationNs;
      } else {
        // No connections, idle or in use.
        //清理结束
        cleanupRunning = false;
      }
    }

    closeQuietly(longestIdleConnection.socket());

    // Cleanup again immediately.
    return 0;
  }
```

总结：
1. 产生一个StreamAllocation对象
2. StreamAllocation对象的弱引用添加到RealConnection对象的allocations集合
3. 从连接池中获取
4. OkHttp使用了GC回收算法
5. StreamAllocation的数量会渐渐变成0
6.倍线程池检测到并回收，这样就可以保持多个健康的keep-alive连接

在ConnectInterceptor拦截器里面已经成功连接到服务器。

#### CallServerInterceptor解析

作用：发送请求，读取响应
```
@Override public Response intercept(Chain chain) throws IOException {
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    HttpCodec httpCodec = realChain.httpStream();
    StreamAllocation streamAllocation = realChain.streamAllocation();
    RealConnection connection = (RealConnection) realChain.connection();
    Request request = realChain.request();

    long sentRequestMillis = System.currentTimeMillis();

    realChain.eventListener().requestHeadersStart(realChain.call());
    //通过HttpCodec写入请求头
    httpCodec.writeRequestHeaders(request);
    realChain.eventListener().requestHeadersEnd(realChain.call(), request);

    Response.Builder responseBuilder = null;
    if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {
      // If there's a "Expect: 100-continue" header on the request, wait for a "HTTP/1.1 100
      // Continue" response before transmitting the request body. If we don't get that, return
      // what we did get (such as a 4xx response) without ever transmitting the request body.
      if ("100-continue".equalsIgnoreCase(request.header("Expect"))) {
        httpCodec.flushRequest();
        realChain.eventListener().responseHeadersStart(realChain.call());
        responseBuilder = httpCodec.readResponseHeaders(true);
      }

      if (responseBuilder == null) {
        // Write the request body if the "Expect: 100-continue" expectation was met.
        realChain.eventListener().requestBodyStart(realChain.call());
        long contentLength = request.body().contentLength();
        //通过HttpCodec写入请求体
        CountingSink requestBodyOut = new CountingSink(httpCodec.createRequestBody(request, contentLength));
        BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);

        request.body().writeTo(bufferedRequestBody);
        bufferedRequestBody.close();
        realChain.eventListener()
            .requestBodyEnd(realChain.call(), requestBodyOut.successfulCount);
      } else if (!connection.isMultiplexed()) {
        // If the "Expect: 100-continue" expectation wasn't met, prevent the HTTP/1 connection
        // from being reused. Otherwise we're still obligated to transmit the request body to
        // leave the connection in a consistent state.
        streamAllocation.noNewStreams();
      }
    }
    //请求编码完毕，表示整个网络请求的写入工作已经完成
    httpCodec.finishRequest();

    if (responseBuilder == null) {
      realChain.eventListener().responseHeadersStart(realChain.call());
      //通过HttpCodec读取网络请求的响应头信息
      responseBuilder = httpCodec.readResponseHeaders(false);
    }

    Response response = responseBuilder
        .request(request)
        .handshake(streamAllocation.connection().handshake())
        .sentRequestAtMillis(sentRequestMillis)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build();

    int code = response.code();
    if (code == 100) {
      // server sent a 100-continue even though we did not request one.
      // try again to read the actual response
      responseBuilder = httpCodec.readResponseHeaders(false);

      response = responseBuilder
              .request(request)
              .handshake(streamAllocation.connection().handshake())
              .sentRequestAtMillis(sentRequestMillis)
              .receivedResponseAtMillis(System.currentTimeMillis())
              .build();

      code = response.code();
    }

    realChain.eventListener()
            .responseHeadersEnd(realChain.call(), response);

    if (forWebSocket && code == 101) {
      // Connection is upgrading, but we need to ensure interceptors see a non-null response body.
      response = response.newBuilder()
          .body(Util.EMPTY_RESPONSE)
          .build();
    } else {
      response = response.newBuilder()
          //通过HttpCodec读取响应体信息
          .body(httpCodec.openResponseBody(response))
          .build();
    }

    if ("close".equalsIgnoreCase(response.request().header("Connection"))
        || "close".equalsIgnoreCase(response.header("Connection"))) {
      streamAllocation.noNewStreams();
    }

    if ((code == 204 || code == 205) && response.body().contentLength() > 0) {
      throw new ProtocolException(
          "HTTP " + code + " had non-zero Content-Length: " + response.body().contentLength());
    }

    return response;
  }
```

#### OkHttp一次网络请求的大致过程
1. Call对象请求的封装
2. Dispatcher对请求的分发
3. getResponseWithInterceptors()方法

## 参考文章
**[OKHttp源码解析](https://www.jianshu.com/p/82f74db14a18)**





    