package com.tencent.demo_dagger2;

import java.lang.reflect.Method;

public class AnnotationProcessor {
    public static void main(String[] args){
        Method[] methods = AnnotationTest.class.getDeclaredMethods();
        for (Method method :
                methods) {
            GET get = method.getAnnotation(GET.class);
            System.out.println(get.value());
        }
    }
}
