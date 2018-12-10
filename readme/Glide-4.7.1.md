# Glide V4.7.1 源码分析

## with(@NonNull Activity activity)
    --> getRetriever(activity)
    -->得到RequestManagerRetriever
    --> get(activity)
    --(1)--> get(activity.getApplicationContext())
         --> getApplicationManager(context)
         --> applicationManager =factory.build(glide,new ApplicationLifecycle(),new EmptyRequestManagerTreeNode(),context.getApplicationContext())
         --> 得到 RequestManager applicationManager

    --(2)--> fragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity))
         --> requestManager = factory.build(glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
         --> 得到和RequestManagerFragment绑定的RequestManager

    总结：with方法最终得到的是RequestManager

## load(@Nullable String string)
    以下代码都是在RequestManager
    --> asDrawable()
    --> as(Drawable.class)
    --> new RequestBuilder<>(glide, this, resourceClass, context) --> 得到RequestBuilder
    --> load(@Nullable String string)
    --> loadGeneric(string)
    --> 返回RequestBuilder

### RequestBuilder里常用方法有：
* RequestBuilder<TranscodeType> apply(@NonNull RequestOptions requestOptions)
* RequestBuilder<TranscodeType> error(@Nullable RequestBuilder<TranscodeType> errorBuilder)
* RequestBuilder<TranscodeType> load(@Nullable String string)
* RequestBuilder<TranscodeType> thumbnail(@Nullable RequestBuilder<TranscodeType> thumbnailRequest)
* RequestBuilder<File> getDownloadOnlyRequest()
* ViewTarget<ImageView, TranscodeType> into(@NonNull ImageView view)

#### RequestBuilder<TranscodeType> apply(@NonNull RequestOptions requestOptions)
