package com.tencent.demo_okhttp;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketDemo {

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    private void connect(){
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url("www.websocket.com").build();

        EchoWebSocketListener listener = new EchoWebSocketListener();
        client.newWebSocket(request,listener);

        client.dispatcher().executorService().shutdown();
    }

    private class EchoWebSocketListener extends WebSocketListener{
        public EchoWebSocketListener() {
            super();
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            webSocket.send("hello world");
            webSocket.close(1000,"再见");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            handler.sendEmptyMessage(0);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(1000,null);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {

        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        }
    }
}
