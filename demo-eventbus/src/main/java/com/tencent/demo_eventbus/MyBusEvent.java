package com.tencent.demo_eventbus;

public class MyBusEvent {
    String message;

    public MyBusEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
