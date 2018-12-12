package com.tencent.demo_dagger2;

public class AnnotationTest {

    @GET(value = "http://ip.taobao.com/59.108.54.37")
    public String getIpMsg(){
        return "";
    }

    @GET(value = "http://ip.taobao.com/")
    public String getIp(){
        return "";
    }
}
