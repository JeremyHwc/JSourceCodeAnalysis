package com.tencent.demo_butterknife;

import java.lang.reflect.Field;

public class ReflectMain {
    @TestInterface(value = 12)
    public int age;

    public static void main(String[] args) {
        ReflectMain main = new ReflectMain();
        TestInterface testInterface = null;
        Class<? extends ReflectMain> clazz = main.getClass();
        try {
            Field field = clazz.getField("age");
            testInterface = field.getAnnotation(TestInterface.class);
            System.out.println(testInterface.value());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
