# EventBus异步框架源码解析

## 一、EventBus框架核心概念：事件传递/EventBus的有点/传统Handler通信的两种方式
1. Android事件发布/订阅框架
2. 事件传递既可用于Android四大组件间通讯
3. EventBus的优点是代码简洁，使用简单，并将事件发布和订阅充分解耦
4. Handler消息通信流程图

    <img src="https://github.com/JeremyHwc/JSourceCodeAnalysis/blob/master/demo-eventbus/pics/Handler%E6%B6%88%E6%81%AF%E9%80%9A%E4%BF%A1%E6%B5%81%E7%A8%8B%E5%9B%BE.png" width ="400" height="400"/>
    
    总的来说：Handler负责消息的发送与接收处理，Handler通过sendMessage将Message加入到MessageQueue里面，
    Looper负责从MessageQueue里面不断获取Message交给handleMessage(msg)来处理
    
## 二、EventBus框架基本用法
1. EventBus架构图

    <img src="https://github.com/JeremyHwc/JSourceCodeAnalysis/blob/master/demo-eventbus/pics/EventBus-Publish-Subscribe.png" width ="600" height="200"/>
    
2. 简单使用步骤
    （1）定义事件Event    
    （2）准备订阅者    
    （3）订阅者同时需要在总线上注册和注销自己   
    （4）发送事件 
```
    public class EventBusSimpleUseActivity extends AppCompatActivity {
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_event_bus_simple_use);
        }
    
        @Override
        protected void onStart() {
            super.onStart();
            EventBus.getDefault().register(this);//注册
        }
    
        @Override
        protected void onStop() {
            super.onStop();
            EventBus.getDefault().unregister(this);//反注册
        }
    
        @Subscribe(threadMode = ThreadMode.MAIN)//定义接收事件后的处理线程
        public void onMessageEvent(MyBusEvent event){
            Toast.makeText(this,event.getMessage(),Toast.LENGTH_LONG).show();
        }
    
        //点击事件发送事件
        public void onSendMessage(View view) {
            EventBus.getDefault().post(new MyBusEvent("我是来自EventBus发送的消息！"));
        }
    }
```    

    

