# Retrofit详解
## Retrofit流程图

![](https://github.com/JeremyHwc/JSourceCodeAnalysis/blob/master/demo-retrofit/images/Retrofit%E6%B5%81%E7%A8%8B%E5%9B%BE.png)

## Retrofit概述
    Retrofit并不是一个网络请求框架，因为其真正的网络请求是由okhttp完成的，其只是对
    OkHttp进行了一个封装。
    
    Application Layer
        |       |
    Retrofit    Layer
        |       |
    OkHttp      Layer
        |       |
         Server
          
    总之，App应用程序通过Retrofit请求网络，实际上是使用Retrofit接口层封装请求参数，
    之后由OkHttp完成后续请求操作。在服务端返回数据之后，OkHttp将原始的结果交给Retrofit,
    Retrofit根据用户的需求对结果进行解析。
    
## Retrofit 官网例子解析

```
//定义接口
public interface GitHubService {

    @GET("users/{user}/repos")
    Call<List<Repo>> listRepos(@Path("user") String user);
}

//创建retrofit
Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

GitHubService gitHubService = retrofit.create(GitHubService.class);

//最终请求还是由okhttp来完成
Call<List<Repo>> repos = gitHubService.listRepos("xxxxx");

repos.enqueue(new Callback<List<Repo>>() {
    @Override
    public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {

    }

    @Override
    public void onFailure(Call<List<Repo>> call, Throwable t) {

    }
});
```

## Retrofit请求过程7步骤详解
    .添加Retrofit库的依赖，添加网络权限
    .创建接收服务器返回的数据的类
    .创建用于描述网络请求的接口(Retrofit是将每一个http请求抽象成了java接口，通过注解来描述配置网络请求的参数，内部实现原理就是用动态代理机制将我们的注解翻译成一个一个的http请求，最后由线程池执行http请求)
    .创建Retrofit实例
    .创建网络请求接口实例
    .发送网络请求（异步/同步）
    .处理服务器返回的数据
    
## 静态代理模式详解
    代理模式：为其他对象提供一种代理，用以控制对这个对象的访问
    例如:海外代购
    
    代码示例
```
public abstract class AbstractObject {
    protected abstract void operation();
}

public class RealObject extends AbstractObject {
    @Override
    protected void operation() {
        System.out.println("do operation...");
    }
}

public class ProxyObject extends AbstractObject {
    private RealObject mRealObject;

    public ProxyObject(RealObject realObject) {
        mRealObject = realObject;
    }

    @Override
    protected void operation() {
        System.out.println("do something before real operation...");
        if (mRealObject==null){
            mRealObject=new RealObject();
        }
        mRealObject.operation();
        System.out.println("do something after real operation...");
    }
}
```
## 动态代理模式详解
    动态代理优势：
        .无侵入
        .通俗：增强方法
        
    动态代理：代理类在程序运行时创建的代理方式。
    
    jdk动态代理
        在动态代理上，只能为接口创建动态代理
        
```
/**
 * 代理类和被代理类都要实现的方法
 */
public interface Subject {
    void shopping();
}

public class Man implements Subject {
    @Override
    public void shopping() {
        System.out.println("Mji要去买东西...");
    }
}

public class Proxy implements InvocationHandler {
    private Object target;//要代理的真实对象

    public Proxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("proxy:"+proxy.getClass().getName());
        System.out.println("before...");
        method.invoke(target,args);
        System.out.println("after...");
        return null;
    }
}

        //动态代理
        Subject man = new Man();
        Proxy p = new Proxy(man);
        //通过java.lang.reflect.newProxyInstance(...)方法获得真实对象的代理对象
        Subject subject =
                (Subject) java.lang.reflect.Proxy.newProxyInstance(
                        man.getClass().getClassLoader(),
                        man.getClass().getInterfaces(),
                        p);
        //通过代理对象调用真实对象相关接口中实现的方法，这个是不就会跳转到这个代理对象所关联的handler的invoke方法
        subject.shopping();
        //获得真实对象的代理对象对应的Class对象的名称，用字符串表示
        System.out.println(subject.getClass().getName());
```
    InvocationHandler
        .每个代理类的对象都会管理一个表示内部处理逻辑的InvocationHandler接口的实现
        .invoke方法的参数中可以获取参数
        .invoke方法的返回值被返回给使用者
        
    CGLIB
    
    总结
        .运行期
        .InvocationHandler接口和Proxy类
        .动态代理与静态代理最大的不同就是动态代理的代理类不需要手动生成，而是在运行期间动态的生成的
        
## Retrofit网络通信流程8步骤和7个关键成员变量解析
    8步骤
        .创建Retrofit实例
        .定义一个网络请求接口并为接口中的方法添加注解
        .通过动态代理生成网络请求对象
        .通过网络请求适配器将网络请求对象进行平台适配
        .通过网络请求执行器发送网络请求，其实就是call对象
        .通过数据转换器解析数据
        .通过回调执行器切换线程
        .用户在主线程处理返回结果
        
    7关键变量
      //缓存一些网络请求的相关配置，网络请求的方法，数据转换器，网络请求适配器
      .private final Map<Method, ServiceMethod<?>> serviceMethodCache = new ConcurrentHashMap<>();
      //请求网络的OkHttp的工厂，
      .final okhttp3.Call.Factory callFactory;
      //请求网络的Url的基地址，
      .final HttpUrl baseUrl;
      //数据转换器工厂的集合，数据转换器是对服务器返回的response进行的转换，生产需要的数据转换器
      .final List<Converter.Factory> converterFactories;
      //网络请求适配器工厂的集合，适配器就是把call对象转换成其他类型，比如：把call转换成RxJava平台的call，CallAdapter.Factory就是用来生产CallAdapter
      .final List<CallAdapter.Factory> callAdapterFactories;
      //用于执行回调的线程池，Android里面默认用的是MainThreadExecutor，可以从Platform的源码中查看到
      .final @Nullable Executor callbackExecutor;
      //是否需要立即解析接口当中的方法，是一个标志位
      .final boolean validateEagerly;
      
## Retrofit中Builder构建者模式和Builder内部类解析
    Retrofit的实例构建是通过Builder模式产生的，Retrofit.Builder源码如下：
```
public static final class Builder {
    private final Platform platform;
    private @Nullable okhttp3.Call.Factory callFactory;
    private @Nullable HttpUrl baseUrl;
    //默认情况下使用的是GsonConverterFactory
    private final List<Converter.Factory> converterFactories = new ArrayList<>();
    //网络请求适配器工厂集合
    private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
    private @Nullable Executor callbackExecutor;
    private boolean validateEagerly;

    Builder(Platform platform) {
      this.platform = platform;
    }

    public Builder() {
      this(Platform.get());
    }

    Builder(Retrofit retrofit) {
      //配置平台对象
      platform = Platform.get();
      //网络适配器工厂
      callFactory = retrofit.callFactory;
      //网络请求基地址
      baseUrl = retrofit.baseUrl;

      // Do not add the default BuiltIntConverters and platform-aware converters added by build().
      for (int i = 1,
          size = retrofit.converterFactories.size() - platform.defaultConverterFactoriesSize();
          i < size; i++) {
        //数据转换器工厂
        converterFactories.add(retrofit.converterFactories.get(i));
      }

      // Do not add the default, platform-aware call adapters added by build().
      for (int i = 0,
          size = retrofit.callAdapterFactories.size() - platform.defaultCallAdapterFactoriesSize();
          i < size; i++) {
        //网络请求适配器工厂
        callAdapterFactories.add(retrofit.callAdapterFactories.get(i));
      }
      //执行异步回调的线程池
      callbackExecutor = retrofit.callbackExecutor;
      validateEagerly = retrofit.validateEagerly;
    }
```

## Retrofit中baseurl/converter/calladapter解析
    .baseUrl必须以“/”结尾；
    .converter就是数据转换器
    .calladapter就是相应平台的转换器
    
## Retrofit中build方法完成Retrofit对象创建流程解析
    build方法代码如下：
```
public Retrofit build() {
      //baseUrl不能为空
      if (baseUrl == null) {
        throw new IllegalStateException("Base URL required.");
      }
      //如果callFactory为空，Retrofit默认使用OkHttpClient进行网络请求
      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }
      //初始化回调方法的执行器，异步请求时会用到，如果callbackExecutor为空，则会默认创建一个MainTheadExecutor
      Executor callbackExecutor = this.callbackExecutor;
      if (callbackExecutor == null) {
        callbackExecutor = platform.defaultCallbackExecutor();
      }
      // Make a defensive copy of the adapters and add the default Call adapter.
      //配置网络请求适配器的工厂
      List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
      //将平台默认的网络请求适配器工厂添加进去
      callAdapterFactories.addAll(platform.defaultCallAdapterFactories(callbackExecutor));
      
      //创建数据转换器的工厂，第一步是初始化大小
      // Make a defensive copy of the converters.
      List<Converter.Factory> converterFactories = new ArrayList<>(
          1 + this.converterFactories.size() + platform.defaultConverterFactoriesSize());

      // Add the built-in converter factory first. This prevents overriding its behavior but also
      // ensures correct behavior when using converters that consume all types.
      converterFactories.add(new BuiltInConverters());
      //添加手动添加的数据转换器工厂
      converterFactories.addAll(this.converterFactories);
      //添加平台默认的数据转换器的工厂
      converterFactories.addAll(platform.defaultConverterFactories());

      return new Retrofit(callFactory, baseUrl, unmodifiableList(converterFactories),
          unmodifiableList(callAdapterFactories), callbackExecutor, validateEagerly);
    }
```

## Retrofit中RxJavaCallAdapterFactory内部构造与工作原理解析
    CallAdapter.Factory是定义在CallAdapter<R, T>这个接口当中的，关于CallAdapter的大致流程，如何在Retrofit当中工作的
    CallAdapter就是把Retrofit当中Call<T>中的泛型对象转换成相应的java对象，然后通过这个java对象进行其他的操作，包括
    UI的显示，后台的数据运作等等。
    
    Call<T> --> java对象
    这个Retrofit当中的Call是对OkHttp里面的Call进行的一个封装，所以说我们通过Retrofit来进行的网络请求，实际上就是通过OkHttp
    来进行的网络请求，只不过Retrofit给我们封装了一下。
    
    Call<T> --发送请求--> 返回数据 --> converter --> java对象，在Retrofit当中默认的converter就是gsonConverter
    
    RxJavaCallAdapterFactory extends CallAdapter.Factory
        .RxJavaCallAdapterFactory继承自CallAdapter.Factory，用来提供具体的适配逻辑
        .通过addCallAdapterFactory(),将CallAdapter添加到Retrofit当中，注册CallAdapter
        .调用Factory中的get方法，获取CallAdapter
        .最后调用CallAdapter当中的 T adapt(Call<R> call) 方法，由这个方法将Call请求转换成每个平台所适用的类型
    
## Retrofit中网络请求接口实例解析
    GitHubService gitHubService = retrofit.create(GitHubService.class);
    create方法如下：
```
public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    if (validateEagerly) {
      eagerlyValidateMethods(service);//提前验证请求方法是否存在，不存在则创建，创建以后保存在serviceMethodCache里面
    }
    //通过动态代理的方式
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();
          private final Object[] emptyArgs = new Object[0];

          @Override public Object invoke(Object proxy, Method method, @Nullable Object[] args)
              throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            return loadServiceMethod(method).invoke(args != null ? args : emptyArgs);
          }
        });
  }
```
## Retrofit中serviceMethod对象解析

## Retrofit中OkHtttpCall对象和adapt 返回对象解析

## Retrofit中同步请求和重要参数解析
    同步请求：OkHttpCall.execute()
        ParameterHandler -> ServiceMethod -> OkHttp发送网络请求 -> converter
    异步请求：OkHttpCall.enqueue()
        
    Retrofit中同步和异步的区别：
        .异步请求会将回调方法交给回调执行器executor,有executor去指定不同的线程去完成不同的操作
        
## Retrofit 设计模式解析-1 ：构建者模式
    构建者模式：将一个复杂对象的构建和表示相分离。
    比如Retrofit的构建过程
```
Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")//设置网络请求的Url地址
                .addConverterFactory(GsonConverterFactory.create())//设置数据解析转换器，其中GsonConverterFactory.create()就是创建了一个含有Gson对象的GsonConverterFactory
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())//网络请求的适配器，支持RxJava平台
                .build();
```

## Retrofit设计模式解析-2 ：工厂模式
    比如：.addConverterFactory(GsonConverterFactory.create())
          .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
    ，其中GsonConverterFactory,RxJavaCallAdapterFactory
    
## Retrofit设计模式解析-3 ：外观模式
    外观模式：外观模式定义了一个高层的功能，为子系统中的多个模块协同的完成某种功能需求提供简单的对外
              功能调用方式，使得这一子系统更加容易被外部使用。
```
public final class Retrofit {
  private final Map<Method, ServiceMethod<?>> serviceMethodCache = new ConcurrentHashMap<>();

  final okhttp3.Call.Factory callFactory;
  final HttpUrl baseUrl;
  final List<Converter.Factory> converterFactories;
  final List<CallAdapter.Factory> callAdapterFactories;
  final @Nullable Executor callbackExecutor;
  final boolean validateEagerly;
  ...................................................
  }
```

## Retrofit设计模式解析-4 ：策略模式
    addCallAdapterFactory(RxJavaCallAdapterFactory.create())
    
    在策略模式（Strategy Pattern）中，一个类的行为或其算法可以在运行时更改。这种类型的设计模式属于行为型模式。
    在策略模式中，我们创建表示各种策略的对象和一个行为随着策略对象改变而改变的 context 对象。策略对象改变 context 对象的执行算法。
    
    介绍
    意图：定义一系列的算法,把它们一个个封装起来, 并且使它们可相互替换。
    主要解决：在有多种算法相似的情况下，使用 if...else 所带来的复杂和难以维护。
    何时使用：一个系统有许多许多类，而区分它们的只是他们直接的行为。
    如何解决：将这些算法封装成一个一个的类，任意地替换。
    关键代码：实现同一个接口。
    应用实例： 1、诸葛亮的锦囊妙计，每一个锦囊就是一个策略。 2、旅行的出游方式，选择骑自行车、坐汽车，每一种旅行方式都是一个策略。 3、JAVA AWT 中的 LayoutManager。
    优点： 1、算法可以自由切换。 2、避免使用多重条件判断。 3、扩展性良好。
    缺点： 1、策略类会增多。 2、所有策略类都需要对外暴露。
    使用场景： 1、如果在一个系统里面有许多类，它们之间的区别仅在于它们的行为，那么使用策略模式可以动态地让一个对象在许多行为中选择一种行为。 2、一个系统需要动态地在几种算法中选择一种。 3、如果一个对象有很多的行为，如果不用恰当的模式，这些行为就只好使用多重的条件选择语句来实现。
    注意事项：如果一个系统的策略多于四个，就需要考虑使用混合模式，解决策略类膨胀的问题。
    
## Retrofit设计模式解析-5 ：适配器模式
    addCallAdapterFactory(RxJavaCallAdapterFactory.create())
    
    适配器模式（Adapter Pattern）是作为两个不兼容的接口之间的桥梁。这种类型的设计模式属于结构型模式，它结合了两个独立接口的功能。
    这种模式涉及到一个单一的类，该类负责加入独立的或不兼容的接口功能。举个真实的例子，读卡器是作为内存卡和笔记本之间的适配器。
    您将内存卡插入读卡器，再将读卡器插入笔记本，这样就可以通过笔记本来读取内存卡。
    我们通过下面的实例来演示适配器模式的使用。其中，音频播放器设备只能播放 mp3 文件，通过使用一个更高级的音频播放器来播放 vlc 和 mp4 文件。
    
    介绍
    意图：将一个类的接口转换成客户希望的另外一个接口。适配器模式使得原本由于接口不兼容而不能一起工作的那些类可以一起工作。
        主要解决：主要解决在软件系统中，常常要将一些"现存的对象"放到新的环境中，而新环境要求的接口是现对象不能满足的。
        何时使用： 1、系统需要使用现有的类，而此类的接口不符合系统的需要。 2、想要建立一个可以重复使用的类，用于与一些彼此之间没有太大关联的一
        些类，包括一些可能在将来引进的类一起工作，这些源类不一定有一致的接口。 3、通过接口转换，将一个类插入另一个类系中。（比如老虎和飞禽，现
        在多了一个飞虎，在不增加实体的需求下，增加一个适配器，在里面包容一个虎对象，实现飞的接口。）
    如何解决：继承或依赖（推荐）。
    关键代码：适配器继承或依赖已有的对象，实现想要的目标接口。
    应用实例： 1、美国电器 110V，中国 220V，就要有一个适配器将 110V 转化为 220V。 
               2、JAVA JDK 1.1 提供了 Enumeration 接口，而在 1.2 中提供了 Iterator 接口，想要使用 1.2 的 JDK，则要将以前系统的 Enumeration 
                 接口转化为 Iterator 接口，这时就需要适配器模式。 
               3、在 LINUX 上运行WINDOWS 程序。 
               4、JAVA 中的 jdbc。
               
    优点： 1、可以让任何两个没有关联的类一起运行。 2、提高了类的复用。 3、增加了类的透明度。 4、灵活性好。
    缺点： 1、过多地使用适配器，会让系统非常零乱，不易整体进行把握。比如，明明看到调用的是 A 接口，其实内部被适配成了 B 接口的实现，一个系
             统如果太多出现这种情况，无异于一场灾难。因此如果不是很有必要，可以不使用适配器，而是直接对系统进行重构。 
           2.由于 JAVA 至多继承一个类，所以至多只能适配一个适配者类，而且目标类必须是抽象类。
    使用场景：有动机地修改一个正常运行的系统的接口，这时应该考虑使用适配器模式。
    注意事项：适配器不是在详细设计时添加的，而是解决正在服役的项目的问题。

## Retrofit设计模式解析-6 ：动态代理模式/观察者模式
    
    观察者模式
    call.enqueue(new Callback<List<Repo>>() {
                @Override
                public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
    
                }
    
                @Override
                public void onFailure(Call<List<Repo>> call, Throwable t) {
    
                }
            });

## Retrofit面试题：retrofit线程切换（异步机制Looper）
    问题：Retrofit中如何完成线程切换
    答案：子线程到主线程之间的切换，MainThreadExecutor()
    
## Retrofit面试题：RxJava和Retrofit如何结合进行网络请求
    .Hook:通过某种手段对一件事物进行盖头换面，从而劫持Hook的目标来以达到控制目标的行为的目的
     DroidPlugin:Hook了很多Android底层的内容
    .Android底层源码
    .技术角度：反射/动态代理
    
## Retrofit面试题：Hook与动态代理

## Retrofit面试题：Android MVC架构优势与缺点
    MVP,相比于MVC
    
    MVC;Model View Controller,即数据、视图、业务逻辑
        架构图：View(Xml) <--> Controller(Activity/Fragment) <--> Model(数据处理) --> View
        MVC优势：
            .技术需求：更换网络库
        MVC劣势：
            XML表示View层，Actvity/Fragment表示Controller层，所以大量的业务逻辑都写在其中，其实就是充当了Controller层和View层
            
## Retrofit面试题 ：MVP有点和缺点
    MVP架构图：
        Model <--> Presenter <--> View
    MVP劣势：
        .P层老问题，也会随着业务逻辑越来越复杂
        .View层和Presenter层的耦合，相互持有引用，View层如果有需要改变时，对应的接口也要做相应的改变
        .接口颗粒度。接口数量的控制不好控制
        
## Retrofit面试题：SP跨进程和apply、commit方法
    .SharedPrefrences跨进程读取
     其跨进程读取是有安全问题的，
     apply方法：
        apply方法将我们修改的数据提交到内存，再异步的将数据保存到硬盘
     commit方法：
        同步的将数据保存到硬盘当中，线程阻塞的
    
    
    
    