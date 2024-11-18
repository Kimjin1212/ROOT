package com.example.photoblogapp;

import static android.content.ContentValues.TAG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CloudImage extends AsyncTask<String, Integer, String> {

    public interface MyCallBack {
        public void doresult(List<Photo> re);
    }

    MyCallBack myCallBack;

    public CloudImage(MyCallBack callBack) {
        myCallBack = callBack;
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            String apiUrl = urls[0];
            URL urlAPI = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d("ServerResponse", "Connected successfully");
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                is.close();

                Log.d(TAG, "doInBackground: " + result.toString());
                return result.toString();

            } else {
                Log.e("ServerResponse", "Connection failed, code: " + responseCode);
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "onPreExecute: ");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(TAG, "onCancelled: ");
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, "onPostExecute: ");

        try {

            List<Photo> bitmapList = new ArrayList<>();
            JSONArray json = new JSONArray(result);
            for (int i = 0; i < json.length(); i++) {
                JSONObject obj = json.getJSONObject(i);
                if (obj.has("photos")) {
                    JSONArray photosarray = obj.getJSONArray("photos");
                    for (int j = 0; j < photosarray.length(); j++) {
                        String imageUrl = photosarray.getJSONObject(j).has("image") ? photosarray.getJSONObject(j).getString("image") : "";
                        String tages = photosarray.getJSONObject(j).has("tags") ? photosarray.getJSONObject(j).getString("tags") : "";
                        int id = photosarray.getJSONObject(j).getInt("id");
                        Photo photo = new Photo();
                        photo.image = imageUrl;
                        photo.id = id;
                        photo.tags = tages;
                        bitmapList.add(photo);
                    }
                }
            }
            myCallBack.doresult(bitmapList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
