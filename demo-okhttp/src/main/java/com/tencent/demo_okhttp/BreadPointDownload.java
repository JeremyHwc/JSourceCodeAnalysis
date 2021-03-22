package com.tencent.demo_okhttp;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BreadPointDownload {
    public void doBreakPointDownload() {
        URL url;
        try {
            url = new URL("http://www.sjtu.edu.cn.down.zip");
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestProperty("RANGE", "bytes=2000080");
            InputStream input = httpConnection.getInputStream();

            RandomAccessFile oSavedFile = new RandomAccessFile("down.zip", "rw");
            long nPos = 2000080;
            oSavedFile.seek(nPos);
            byte[] b = new byte[1024];
            int nRead;
            while ((nRead = input.read(b, 0, 1024)) > 0) {
                oSavedFile.write(b, 0, nRead);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doDownloadWithOkHttp() {
        InputStream is;
        RandomAccessFile savedFile;
        File file;
        // 记录已经下载的文件长度
        long downloadLength = 0;
        String downloadUrl = "www.xxx.xxx.txt";
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("."));
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        file = new File(directory + fileName);
        if (file.exists()) {
            downloadLength = file.length();
        }
        long contentLength = getContentLength(downloadUrl);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().addHeader("RANGE", "bytes=" + downloadLength + "-").url(downloadUrl).build();
        try {
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadLength);
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    total += len;
                    savedFile.write(b, 0, len);
                    // 计算已经下载的百分比
                    int progress = (int) ((total + downloadLength) * 100 / contentLength);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long getContentLength(String downloadUrl) {
        return 0;
    }
}
