package com.example.myapplication;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.myapplication.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.myapplication.bean.PostVideoResponse;
import com.example.myapplication.network.IMiniDouyinService;
import com.example.myapplication.utils.ResourceUtils;

public class SendActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;

    private File videoFile;
    private int rotationDegree = 0;
    MediaPlayer mediaPlayer;
    int position;
    boolean isPreview;

    public Uri mSelectedImage;
    private Uri mSelectedVideo;
    ImageButton play;
    Button select;
    Button post;
    private static final String TAG = "SendActivity";
    boolean isSelect = false;
    MultipartBody.Part video;
    MultipartBody.Part cover_image;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.send);
        mediaPlayer = new MediaPlayer();
        mSurfaceView = findViewById(R.id.img);
        play = findViewById(R.id.play);
        select = findViewById(R.id.select);
        post = findViewById(R.id.post);
        //todo 给SurfaceHolder添加Callback
        videoFile = new File(getIntent().getStringExtra("VideoFile"));
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                playVideo();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    position = mediaPlayer.getCurrentPosition();
                    mediaPlayer.stop();
                }
            }
        });

        play.setOnClickListener(v->{
            if(mediaPlayer.isPlaying()){
                play.setImageResource(R.drawable.play);
                mediaPlayer.pause();

            }
            else{
                play.setImageResource(R.drawable.pause);
                mediaPlayer.start();
            }
        });

        select.setOnClickListener(v->{
            chooseImage();
        });

        post.setOnClickListener(v->{
            if(isSelect){
                postVideo();
            }
            else{
                Toast.makeText(this, "没有选择封面！", Toast.LENGTH_SHORT).show();
            }
        });


        findViewById(R.id.go_back).setOnClickListener(v->{
            Intent intent = new Intent(SendActivity.this,ShootActivity.class);
            //intent.putExtra("VideoFile",videoFile2.getAbsolutePath());
            startActivity(intent);
        });

        //mSelectedVideo = getMediaUriFromPath(this,videoFile.getAbsolutePath());
       // Log.d(TAG, "mSelectedVideo = " + mSelectedVideo);
        //mBtn.setText(R.string.post_it);
        //video = getMultipartFromUri("video", mSelectedVideo);
        //

        System.out.println(videoFile.getAbsolutePath());
        // File f = new File(ResourceUtils.getRealPath(SendActivity.this, uri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile);
        try {
            video = MultipartBody.Part.createFormData("video", URLEncoder.encode(videoFile.getName(),"UTF-8") , requestFile);
        }catch (Exception e){
            e.getStackTrace();
            video = MultipartBody.Part.createFormData("video", videoFile.getName(), requestFile);
        }
    }

    public void chooseImage() {
        // TODO-C2 (4) Start Activity to select an image
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    public static Uri getMediaUriFromPath(Context context, String path) {
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(mediaUri,
                null,
                MediaStore.Images.Media.DISPLAY_NAME + "= ?",
                new String[] {path.substring(path.lastIndexOf("/") + 1)},
                null);

        Uri uri = null;
        if(cursor.moveToFirst()) {
            uri = ContentUris.withAppendedId(mediaUri,
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
        }
        cursor.close();
        return uri;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() called with: requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");

        if (resultCode == RESULT_OK && null != data) {

            if (requestCode == 1) {
                mSelectedImage = data.getData();
                Log.d(TAG, "selectedImage = " + mSelectedImage);
                cover_image = getMultipartFromUri("cover_image", mSelectedImage);
                isSelect = true;
            }
        }
    }


    private MultipartBody.Part getMultipartFromUri(String name, Uri uri) {
        // if NullPointerException thrown, try to allow storage permission in system settings
        File f = new File(ResourceUtils.getRealPath(SendActivity.this, uri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), f);
        try {
            return MultipartBody.Part.createFormData(name, URLEncoder.encode(f.getName(),"UTF-8") , requestFile);
        }catch (Exception e){
            e.getStackTrace();
            return MultipartBody.Part.createFormData(name, f.getName(), requestFile);
        }
    }

    public void playVideo() {
        try {
            mediaPlayer.reset();//重置
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //raw文件夹下面的内容
//            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.baishi);
//            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setDataSource(videoFile.getPath());
            mediaPlayer.setDisplay(mSurfaceView.getHolder());
            mediaPlayer.setLooping(true);
            //mediaPlayer.setDataSource(URL);
            //视频输出到SurfaceView上
            mediaPlayer.prepare();//使用同步方式
            mediaPlayer.start();//开始播放

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(SendActivity.this, "WTF", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onPause() {
        super.onPause();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            position = mediaPlayer.getCurrentPosition();

        }
    }

    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    private void postVideo() {
       // mBtn.setText("POSTING...");
        //mBtn.setEnabled(false);

        // TODO-C2 (6) Send Request to post a video with its cover image
        // if success, make a text Toast and show
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://test.androidcamp.bytedance.com/mini_douyin/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        IMiniDouyinService service = retrofit.create(IMiniDouyinService.class);

        Call<PostVideoResponse> call  = service.postVideo("16061030","Aya",cover_image,video);
        call.enqueue(new Callback<PostVideoResponse>() {
            @Override
            public void onResponse(Call<PostVideoResponse> call, Response<PostVideoResponse> response) {
                System.out.println("success");
                System.out.println(response.body());
                Toast.makeText(SendActivity.this, "上传成功",Toast.LENGTH_LONG).show();
                //mBtn.setText(R.string.success_try_refresh);
                //mBtn.setEnabled(true);
            }
            @Override
            public void onFailure(Call<PostVideoResponse> call, Throwable t) {
                System.out.println("fail");
                Toast.makeText(SendActivity.this, "上传失败",Toast.LENGTH_LONG).show();
            }
        });
    }

}
