# Dagger2源码分析

## 注解简介
1. 定义注解 

    定义新的注解类型使用@interface,注解只有成员变量，没有方法。成员变量在注解定义中以“无形参的方法”
    形式来声明，其“方法名”定义了该成员变量的名字，其返回值定义了该成员变量的类型；定义了成员变量后，
    使用该注解时就应该为该注解的成员变量指定值，也可以使用default关键字为期指定默认值,如果注解定义了
    默认值，所以使用时可以不为指定了默认值的成员变量指定值。
    ```
    public @interface Swordsman {
        String name() default "Swordsman";
        int age();
    }
    ```
2. 定义运行时注解
    通过@Retention来定义注解的保留策略。
    （1）一般如果需要在运行时去动态获取注解信息，就用RetentionPolicy.RUNTIME；
    （2）如果要在编译时进行一些预处理操作，比如生成一些辅助代码，就用RetentionPolicy.CLASS；
    （3）如果只是做一些检查性的操作，比如@Override、@SuppressWarnings,就用RetentionPolicy.SOURCE。
    
    RetentionPolicy.RUNTIME：运行时注解。当运行Java程序时，JVM也会保留该注解信息，可以通过反射获取该注解信息。
    RetentionPolicy.CLASS：编译时注解。注解信息会保留在.java源码和.class文件中，当运行Java程序时，JVM会丢弃该注解信息，不会保留在JVM中。
    RetentionPolicy.SOURCE：源码级注解。注解信息只会保留在.java源码中，源码在编译后，注解信息被丢弃，不会保留在.class中。
    ```
    @Retention(RetentionPolicy.RUNTIME)
    @Retention(RetentionPolicy.CLASS)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Swordsman {
        String name() default "Swordsman";
        int age();
    }
    ```  





## Dagger2简介
    什么是依赖注入？
    就是目标类中所以来的其他的类的初始化过程，不是通过手动编码的方式创建。
    需要在一个对象里去创建另一个对象的实例。
    Picasso/Glide
    ```
    public class FruitContainer{
        Banana banana;
        public FruitContainer(){
            banana=new Banana();
        }
    }
    ```
    以上的代码里面的Banana是通过代码来初始化Banana的，如果Banana的构造方法发生变化，则依赖于Banana的
    类都需要进行变动，而在其他的类里面我们其实只关心Banana的实例引用，所以这个时候如果用Dagger2进行
    依赖的注入，即时Banana的构造方法发生变化时，也不用修改代码，这样易于代码解耦。
##　Dagger2四种注入方式和依赖注入总结
    
    Dagger2就是一个帮你写工厂代码的工具
    不要在需要依赖的类中通过new来创建依赖，而是通过方法提供的参数注入进来
    
## Dagger2的四种基本注解
1.@inject
* 标记在需要依赖的变量
* 使用在构造函数上

依赖注入是依赖的对象实例 --> 需要注入的实例属性
新建工厂实例并调用成员属性注入类完成相应的实例注入




