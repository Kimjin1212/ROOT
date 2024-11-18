package com.example.photoblogapp;
//

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Api {
    public static String ipaddr = "10.0.2.2";
    public static String port = "8000";
    public static String baseaddr = "http://" + ipaddr + ":" + port;
    public static String baseaddr_port = "http://" + ipaddr + ":" + port;
    public static String basehttpaddr = "http://" + ipaddr + ":" + port + "/";

    public static String getposts = baseaddr +  "/api/posts/";
    public static String addphotos = baseaddr + "/api/photos/";
    public static String settages = baseaddr + "/api/settages/";
    public static String deletephotos = baseaddr + "/api/deletephotos/";


    public static void Get(Context context, String url, Callback callback) {
        Log.d(TAG, "Get: "  +url);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("authorization", context.getSharedPreferences("app", Context.MODE_PRIVATE).getString("token", ""))
                .get().build();
        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    public static void Post(Context context, String url, JSONObject jsonObject, Callback callback) {
        OkHttpClient okHttpClient = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request
                .Builder()
                .addHeader("authorization", context.getSharedPreferences("app", Context.MODE_PRIVATE).getString("token", ""))
                .post(requestBody)  // Post请求的参数传递
                .url(url)
                .build();
        okHttpClient.newCall(request).enqueue(callback);
    }

}
