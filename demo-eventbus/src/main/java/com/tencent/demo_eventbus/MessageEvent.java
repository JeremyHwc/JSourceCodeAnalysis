package com.tencent.demo_eventbus;

public class MessageEvent {
    String postThreadName;
    String receiveThreadName;

    public MessageEvent(String postThreadName, String receiveThreadName) {
        this.postThreadName = postThreadName;
        this.receiveThreadName = receiveThreadName;
    }

    public String getPostThreadName() {
        return postThreadName;
    }

    public void setPostThreadName(String postThreadName) {
        this.postThreadName = postThreadName;
    }

    public String getReceiveThreadName() {
        return receiveThreadName;
    }

    public void setReceiveThreadName(String receiveThreadName) {
        this.receiveThreadName = receiveThreadName;
    }
}
