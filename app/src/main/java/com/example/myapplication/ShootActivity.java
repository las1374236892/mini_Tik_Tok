package com.example.myapplication;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.myapplication.utils.Mp4ParserUtils;
import com.example.myapplication.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static com.example.myapplication.utils.Utils.getOutputMediaFile;

public class ShootActivity extends AppCompatActivity{

    private SurfaceView mSurfaceView;
    private Camera mCamera;

    private int CAMERA_TYPE = Camera.CameraInfo.CAMERA_FACING_BACK;
    //private int CAMERA_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private boolean isRecording = false;
    private File videoFile;
    private File videoFile2;
    private int rotationDegree = 0;
    int position;
    boolean isPreview;
    volatile boolean canceled = false;
    volatile long totalTime = 0;
    int mDelayState = 0;
    volatile ProgressBar progressBar;
    boolean flashOpen = false;
    volatile boolean End = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.shoot);
        mSurfaceView = findViewById(R.id.img);
        progressBar = findViewById(R.id.progress);
        //todo 给SurfaceHolder添加Callback
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                startPreview(surfaceHolder);
                mCamera.startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if(mCamera != null){
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }
            }
        });


        findViewById(R.id.btn_record).setOnClickListener(v -> {
            //todo 录制，第一次点击是start，第二次点击是stop
            //mCamera.stopPreview();

            if (isRecording) {
                //todo 停止录制
                //mMediaRecorder.stop();
                canceled = true;
                releaseMediaRecorder();
                isRecording = false;
                //canceled = true;

                System.out.println(videoFile.getPath() + " "+progressBar.getProgress());
                //releaseCameraAndPreview();
                if(progressBar.getProgress()<100){
                    if(videoFile2 == null){
                        videoFile2 = videoFile;
                    }
                    else{
                        ArrayList<String> strings = new ArrayList<>();
                        strings.add(videoFile2.getAbsolutePath());
                        strings.add(videoFile.getAbsolutePath());
                        File mergeVideoFile = new File(Utils.getOutputMediaFile(2).getAbsolutePath());
                        Mp4ParserUtils.mergeVideo(strings, mergeVideoFile);
                        videoFile.delete();
                        videoFile2.delete();
                        videoFile2 = mergeVideoFile;
                        //System.out.println("合并\n"+v0+"\n"+v1+"成功"+"\n"+videoFile2.getAbsolutePath());
                    }
                }
                else{
                    totalTime=0;
                    progressBar.setProgress(0);
                }

                //playVideo();
            } else {
                //todo 录制
                prepareVideoRecorder();
                System.out.println("11111111");
                isRecording=true;
            }
        });

        findViewById(R.id.btn_facing).setOnClickListener(v -> {
            //todo 切换前后摄像头
            if(CAMERA_TYPE == Camera.CameraInfo.CAMERA_FACING_BACK){
                CAMERA_TYPE = Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
            else{
                CAMERA_TYPE = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            releaseCameraAndPreview();
            startPreview(mSurfaceView.getHolder());
        });

        findViewById(R.id.btn_zoom).setOnClickListener(v -> {
            //todo 调焦，需要判断手机是否支持
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.isZoomSupported()){
                int zoom = parameters.getZoom();
                int max_zoom = parameters.getMaxZoom();
                if(zoom < max_zoom){
                    zoom++;
                }
                parameters.setZoom(zoom);
                mCamera.setParameters(parameters);
            }
            else{
                Toast.makeText(this, "不支持放大", Toast.LENGTH_SHORT).show();
            }
        });


        findViewById(R.id.btn_flash).setOnClickListener(view -> {
            //Toast.makeText(this, ""+mediaPlayer.isPlaying(), Toast.LENGTH_SHORT).show();
           //System.out.println(mediaPlayer.isPlaying());
            if( mCamera != null){
                Camera.Parameters parameters = mCamera.getParameters();
                if(flashOpen){
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);//关闭
                    mCamera.setParameters(parameters);
                }
                else {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//开启
                    mCamera.setParameters(parameters);
                }
                flashOpen=!flashOpen;
            }
        });

        findViewById(R.id.btn_0S).setOnClickListener(v->{
            mDelayState=0;
            Toast.makeText(this, "设定为0S延迟", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_3S).setOnClickListener(v->{
            mDelayState=3;
            Toast.makeText(this, "设定为3S延迟", Toast.LENGTH_SHORT).show();
        });


    }


    void stopRecord(){
        canceled = true;
        releaseMediaRecorder();
        isRecording = false;
        //canceled = true;

        System.out.println(videoFile.getPath() + " "+progressBar.getProgress());
        totalTime=0;
        //System.out.println("EEEEEEEnd "+totalTime);
        //progressBar.setProgress(1);
        progressBar.setProgress(0);
        //mHandler.handleMessage(new Message());
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.handleMessage(new Message());
            }
        }).start();*/
        if(videoFile2 == null){
            videoFile2 = videoFile;
        }
        else{
            ArrayList<String> strings = new ArrayList<>();
            strings.add(videoFile2.getAbsolutePath());
            strings.add(videoFile.getAbsolutePath());
            File mergeVideoFile = new File(Utils.getOutputMediaFile(2).getAbsolutePath());
            Mp4ParserUtils.mergeVideo(strings, mergeVideoFile);
            videoFile.delete();
            videoFile2.delete();
            videoFile2 = mergeVideoFile;
            //System.out.println("合并\n"+v0+"\n"+v1+"成功"+"\n"+videoFile2.getAbsolutePath());
            Looper.prepare();
            Toast.makeText(ShootActivity.this, "文件保存在"+videoFile2.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Looper.loop();
        }


    }

    public Camera getCamera(int position) {
        CAMERA_TYPE = position;
        if (mCamera != null) {
            releaseCameraAndPreview();
        }
        Camera cam = Camera.open(position);

        //todo 摄像头添加属性，例是否自动对焦，设置旋转方向等
        rotationDegree = getCameraDisplayOrientation(position);
        cam.setDisplayOrientation(rotationDegree);
        Camera.Parameters parameters = cam.getParameters();
        if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            cam.setParameters(parameters);
        }

        return cam;
    }


    private static final int DEGREE_90 = 90;
    private static final int DEGREE_180 = 180;
    private static final int DEGREE_270 = 270;
    private static final int DEGREE_360 = 360;

    private int getCameraDisplayOrientation(int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = DEGREE_90;
                break;
            case Surface.ROTATION_180:
                degrees = DEGREE_180;
                break;
            case Surface.ROTATION_270:
                degrees = DEGREE_270;
                break;
            default:
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % DEGREE_360;
            result = (DEGREE_360 - result) % DEGREE_360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + DEGREE_360) % DEGREE_360;
        }
        return result;
    }


    private void releaseCameraAndPreview() {
        //todo 释放camera资源
        if (mCamera != null) {
            //canceled=true;
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            isPreview = false;
            /*try {
                time.wait();
            }catch (Exception e){
                e.printStackTrace();
            }*/

        }
    }

    Camera.Size size;

    private void startPreview(SurfaceHolder holder) {
        //todo 开始预览
        isPreview = true;
        if(mCamera == null){
            mCamera = getCamera(CAMERA_TYPE);
        }
        try {
            size = getOptimalPreviewSize(mCamera.getParameters().getSupportedPictureSizes(),mSurfaceView.getWidth(),mSurfaceView.getHeight());
            mCamera.getParameters().setPictureSize(size.width,size.height);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mCamera.cancelAutoFocus();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private MediaRecorder mMediaRecorder;

    void freshTime(){
        //time =
    }

    private boolean prepareVideoRecorder() {
        //todo 准备MediaRecorder
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        videoFile = new File(Utils.getOutputMediaFile(2).getAbsolutePath());
        //mMediaRecorder.setOutputFile(videoFile);
        mMediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
        mMediaRecorder.setOrientationHint(rotationDegree);

        End = false;
        //mMediaRecorder.setMaxDuration((int)(15000-totalTime));
        //mMediaRecorder.pause();
        try{
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            canceled = false;
            if(totalTime == 0){
                //System.out.println("caonima  "+totalTime);
                //progressBar.setProgress(0);
                /*new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.handleMessage(new Message());
                    }
                }).start();*/
                new Thread(() -> {
                    while(totalTime<15000 ) {
                        try {
                            Thread.sleep(50);
                            if(canceled){
                            }
                            else{
                                totalTime += 50;
                                progressBar.setProgress((int) totalTime / 150);
                                //System.out.println("?????!!!!");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    stopRecord();
                }).start();
            }
            return true;
        }catch (Exception e){
            //releaseMediaRecorder();
            return false;
        }
    }

    private void releaseMediaRecorder() {
        //todo 释放MediaRecorder
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = Math.min(w, h);

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    private static Rect calculateTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        int centerX = (int)(x/previewSize.height*2000)-1000;
        int centerY = (int)(y/previewSize.width*2000)-1000;
        //System.out.println(previewSize.width+" "+previewSize.height+" "+x+" "+y+" "+centerX+" "+centerY+" ");
        int left = clamp(centerX - 100, -1000, 1000);
        int top = clamp(centerY - 100, -1000, 1000);
        int right = clamp(centerX + 100, -1000, 1000);
        int bottom = clamp(centerY + 100 , -1000, 1000);
        //System.out.println("???"+centerX+" "+centerY+" "+left+" "+top);
        RectF rectF = new RectF(left, top, right, bottom);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private static void handleFocus(MotionEvent event, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, previewSize);
        System.out.println( ""+focusRect.bottom+" "+focusRect.top);

        camera.cancelAutoFocus();
        Camera.Area cameraArea = new Camera.Area(focusRect, 1000);
        List<Camera.Area> meteringAreas = new ArrayList<>();
        List<Camera.Area> focusAreas = new ArrayList<>();
        /*if (params.getMaxNumMeteringAreas() > 0) {
            meteringAreas.add(cameraArea);
            focusAreas.add(cameraArea);
        }*/
        if (params.getMaxNumFocusAreas() > 0) {
            //meteringAreas.add(cameraArea);
            focusAreas.add(cameraArea);
        }
        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // 设置对焦模式
        params.setFocusAreas(focusAreas); // 设置对焦区域
        //params.setMeteringAreas(meteringAreas); // 设置测光区域
        try {
            camera.cancelAutoFocus(); // 每次对焦前，需要先取消对焦
            camera.setParameters(params); // 设置相机参数
            //camera.autoFocus(mAutoFocusCallback); // 开启对焦
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Camera.Parameters params = camera.getParameters();
                    params.setFocusMode(currentFocusMode);
                    camera.setParameters(params);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            handleFocus(event, mCamera);
        }
        return true;
    }

    private final MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private final WeakReference<ShootActivity> mActivity;

        public MyHandler(ShootActivity activity) {
            mActivity = new WeakReference<ShootActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ShootActivity activity = mActivity.get();
            if (activity != null) {
                //activity.mClockView.setShowAnalog(activity.mClockView.isShowAnalog());
                activity.progressBar.setProgress(0);
                super.handleMessage(msg);
            }
        }
    }
}
