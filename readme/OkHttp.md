# OkHttp源码分析

## 一、OkHttp流程

### 涉及内容
OkHttpClient     
Request     
RealCall        
Dispatcher      
Intercepters:
    包括RetryAndFollowIntercepter,BridgeIntercepter,CacheIntercepter,ConnectIntercepter,CallServerInterceper

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

#### OkHttp同步方法总结
- 创建OkHttpClient和Request对象
- 将Request封装成Call对象
- 调用Call的enqueue()方法发送异步请求

*注意：OkHttp发送同步请求后，就会进入阻塞状态，直到收到响应，也就是说当前线程发送同步请求后，就会进
入阻塞状态，直到收到数据响应，异步请求会重新创建一个工作线程，不会阻塞当前的线程。
对于异步请求来说，onResponse()和onFailure()都是在子线程当中执行的。*

#### OkHttp同、异步请求的区别
- 发起请求的方法调用
- 阻塞线程与否

#### OkHttp请求流程如下
![](https://github.com/JeremyHwc/JSourceCodeAnalysis/blob/master/demo-okhttp/images/OkHttp%E8%AF%B7%E6%B1%82%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)

#### OkHttp同步请求的执行流程和源码分析
    