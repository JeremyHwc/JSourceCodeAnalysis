# Glide源码分析

    Glide几个基本概念
    （1）Model --- 表示的是数据来源，url，文件，资源id
    （2）Data --- 数据源中获取到model后，会加工成原始数据，一般是inputstream，而负责从数据源中获取原始数据的叫做ModelLoader
    （3）Resource --- 负责从刚才得到的原始数据进行解码，解码之后的资源就叫做Resource，负责解码的就叫做ResourceDecoder
    （4）TransformedResource --- 把Resource进行变换，ResourceTransform来进行转换，转换后的资源就叫做TransformedResource
    （5）TranscodedResource --- 转码，glide除了能加载静态图之外，还能加载动态图，但是解码之后的bitmap和gifDrawable,其类型不是统一的，
                                为了方便处理，glide就会把bitmap转换成glideBitmapDrawable,这样类型就统一了，负责转码的角色叫
                                transcode,而转码之后的就叫做TranscodeResource
    （6）将图片显示在目标上，glide将我们显示的目标封装成target
    
    流程图：
        Model --ModelLoader--> Data --Decoder--> Resource --Transform--> TransformedResource --Transcode -->TranscodeResource -->Target

    Glide.with(Context context)
        其内部通过获取到一个无界面的fragment添加到activity，glide无法监听到activity的生命周期，只能通过fragment来监听，从而完成绑定activity
        生命周期，来选择图片加载的过程
        
        with方法是为了获取到RequestManager对象,管理我们的图片请求流程
        
    load(String url)
        这个方法返回DrawableTypeRequest，这个对象代表着所有Glide中加载图片的所有request请求;
        这个方法其实都是做的一些初始化工作，返回一个DrawableTypeRequest对象
    RequestManager作用：
        （1）管理我们的请求
        （2）完成Glide对象的构造，控制glide请求过程中的各种方法
        （3）通过RequestManager，我们就可以去控制整个界面的生命周期的监听，也就是说，它是用来监听整个组件的生命周期的
             根据生命周期进行图片的操作
    RequestTracker作用：
        负责跟踪整个图片请求周期，也可以作为取消、重启一些失败的图片请求的作用
    DrawableTypeRequest作用：
        主要将我们要加载的图片强制转换成bitmap或者gif，方法是asBitmap(),asGif()
    
    into(ImageView view)
        into方法的实现最终都是在GenericRequestBuilder当中