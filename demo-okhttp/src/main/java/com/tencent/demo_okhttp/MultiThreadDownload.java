package com.tencent.demo_okhttp;

import android.text.TextUtils;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultiThreadDownload {
    public void download(String urlPath, String targetFilePath, String child, int threadCount) throws Exception {
        URL url = new URL(urlPath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        int code = connection.getResponseCode();
        if (code == 200) {
            // 获取资源大小
            int contentLength = connection.getContentLength();
            // 在本地创建一个与资源同样大小的文件来占位
            RandomAccessFile randomAccessFile = new RandomAccessFile(new File(targetFilePath, child), "rw");
            randomAccessFile.setLength(contentLength);

            int blockSize = contentLength / threadCount;
            for (int threadId = 0; threadId < threadCount; threadId++) {
                // 左闭右开
                int startIndex = threadId * blockSize;
                int endIndex = (threadId + 1) * blockSize - 1;
                if (threadId == threadCount - 1) {
                    endIndex = contentLength - 1;
                }
                new DownloadThread(urlPath, targetFilePath, threadId, startIndex, endIndex).start();// 开启线程下载
            }
        }
    }

    private class DownloadThread extends Thread {
        private final String urlPath;
        private final String targetFilePath;
        private final int threadId;
        private int startIndex;
        private final int endIndex;

        public DownloadThread(String urlPath, String targetFilePath, int threadId, int startIndex, int endIndex) {
            this.urlPath = urlPath;
            this.targetFilePath = targetFilePath;
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public void run() {
            System.out.println("线程" + threadId + "开始下载");
            try {
                URL url = new URL(urlPath);
                File downloadThreadFile = new File(targetFilePath, "download_" + threadId + ".dt");
                RandomAccessFile downThreadStream;
                if (downloadThreadFile.exists()) {
                    downThreadStream = new RandomAccessFile(downloadThreadFile, "rwd");
                    String startIndex_str = downThreadStream.readLine();
                    if (!TextUtils.isEmpty(startIndex_str)) {
                        this.startIndex = Integer.parseInt(startIndex_str) - 1;
                    }
                } else {
                    downThreadStream = new RandomAccessFile(downloadThreadFile, "rwd");
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setRequestProperty("RANGE", "bytes=" + startIndex + "_" + endIndex);
                // 206代表部分资源请求成功
                if (connection.getResponseCode() == 206) {
                    InputStream inputStream = connection.getInputStream();
                    RandomAccessFile randomAccessFile = new RandomAccessFile(new File(targetFilePath, getFileName(url)), "rw");
                    randomAccessFile.seek(startIndex);

                    byte[] buffer = new byte[1024];
                    int length = -1;
                    int total = 0;// 记录本次下载的文件大小
                    while ((length = inputStream.read(buffer)) > 0) {
                        randomAccessFile.write(buffer, 0, length);
                        total += length;
                        downThreadStream.seek(0);
                        downThreadStream.write((startIndex + total + "").getBytes("UTF-8"));
                    }
                    downThreadStream.close();
                    inputStream.close();
                    randomAccessFile.close();
                    cleanTemp(downloadThreadFile);
                }
            } catch (Exception e) {

            }
        }

        private void cleanTemp(File downloadThreadFile) {

        }

        private String getFileName(URL url) {
            return null;
        }
    }
}
