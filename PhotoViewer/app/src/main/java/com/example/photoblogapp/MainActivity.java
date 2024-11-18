package com.example.photoblogapp;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int MULTIPLE_PERMISSIONS_REQUEST_CODE = 2901;
    private RecyclerView recyclerView;
    private TextView textView;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Button btnUpload, btnSync;
    private List<Photo> imageList = new ArrayList<>();
    private ImageAdapter adapter;

    private void getdata() {
        CloudImage cloudImage = new CloudImage(re -> {
            updateUI(re);
        });
        cloudImage.execute("http://10.0.2.2:8000/api/posts/");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 确保 layout 文件名和实际一致


        recyclerView = findViewById(R.id.recyclerView);
        textView = findViewById(R.id.textView);
        btnUpload = findViewById(R.id.btn_upload); // 确保 ID 和布局文件一致
        btnSync = findViewById(R.id.btn_sync); // 确保 ID 和布局文件一致

        adapter = new ImageAdapter(imageList, MainActivity.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 上传按钮监听器
        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
        // 同步按钮监听器
        btnSync.setOnClickListener(v -> {
            getdata();
        });

        adapter.setOnItemClickListener(new ImageAdapter.ImageCallBack() {
            @Override
            public void doClick(Photo photo) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Add Tag");

                final EditText input = new EditText(MainActivity.this);
                builder.setView(input);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    if (!input.getText().toString().isEmpty()) {

                        String tag = input.getText().toString();

                        Api.Get(MainActivity.this, Api.settages + "" + photo.id + "/" + tag + "/", new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {

                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Tag added: " + tag, Toast.LENGTH_SHORT).show());

                                getdata();
                            }
                        });

                        getdata();
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Input tage name !", Toast.LENGTH_SHORT).show());
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                builder.show();
            }

            @Override
            public void doLongClick(Photo photo) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Confirm delete the photo ?")
                        .setPositiveButton("sure", (dialogInterface, i) -> {
                            Api.Get(MainActivity.this, Api.deletephotos + "" + photo.id + "/", new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                                }

                                @Override
                                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                                }
                            });
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "delete success", Toast.LENGTH_SHORT).show());


                            getdata();
                        })
                        .setNegativeButton("No", null)
                        .create().show();

            }
        });
        initpremission();
    }

    private void initpremission() {

        String[] permissions = {
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION, // 粗略位置权限
                Manifest.permission.READ_PHONE_STATE, // 读取电话状态权限
        };

        if (!PermissionUtils.hasPermissions(this, permissions)) {
            PermissionUtils.requestPermissions(this, permissions, MULTIPLE_PERMISSIONS_REQUEST_CODE);
        } else {

        }

    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(proj[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        } else {
            return null; // 如果无法获取路径，返回 null
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                uploadImage(new File(getRealPathFromURI(imageUri)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void uploadImage(File file) {
        OkHttpClient httpClient = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/octet-stream");//设置类型，类型为八位字节流
        RequestBody requestBody = RequestBody.create(mediaType, file);//把文件与类型放入请求体

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(), requestBody)//文件名
                .addFormDataPart("post", "1")
                .build();
        Log.d(TAG, "uploadImage: " + Api.addphotos);
        Request request = new Request.Builder()
                .url(Api.addphotos)
                .post(multipartBody)
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Upload fail", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //此方法运行在子线程中，不能在此方法中进行UI操作。
                String result = response.body().string();
                Log.d("upload file :", result);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Upload success", Toast.LENGTH_SHORT).show());
            }
        });
    }

    public void updateUI(List<Photo> images) {
        if (images.isEmpty()) {
            textView.setText("불러온 이미지가 없습니다.");
        } else {
            textView.setText("이미지 로드 성공!");
            imageList.clear();
            imageList.addAll(images);
            adapter.notifyDataSetChanged();
        }
    }
}
