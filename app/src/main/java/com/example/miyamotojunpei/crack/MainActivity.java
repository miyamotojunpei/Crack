package com.example.miyamotojunpei.crack;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import javax.net.ssl.SSLException;

import static android.view.Window.FEATURE_ACTION_BAR_OVERLAY;
import static org.opencv.core.CvType.CV_8UC4;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "Crack::Activity";
    private MyCameraView mOpenCvCameraView;
    private int cameraId = 0;
    FaceDetector detector;
    private int mode = 0;
    private Mat src;
    private double fist = 1000000.0;
    private Bitmap crack;
    private Mat crackMat;
    private Bitmap crack1;
    private Mat crackMat1;
    private Bitmap crack2;
    private Mat crackMat2;
    private Bitmap flower;
    private Mat flowerMat;
    private int res_width = 864;
    private int res_height = 480;
    private int cracked = 0;
    private SoundPool soundPool;
    private int sound_crack;
    private int sound_explode;
    private int sound_bloom;
    private String gender = "?";
    private double beauty = 0;
    private int age = 0;
    private boolean isBeauty = false;
    private boolean isDetected = false;
    private byte[] jsonByte;
    boolean isConnected = false;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) { //OpenCVのマネージャへの接続
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setResolution(res_width, res_height);

                    crack = BitmapFactory.decodeResource(getResources(), R.drawable.crack); //画像をOpenCVで扱えるように処理
                    crackMat = new Mat(crack.getHeight(), crack.getWidth(), CV_8UC4);
                    Utils.bitmapToMat(crack, crackMat);
                    crack1 = BitmapFactory.decodeResource(getResources(), R.drawable.crack1);
                    crackMat1 = new Mat(crack1.getHeight(), crack1.getWidth(), CV_8UC4);
                    Utils.bitmapToMat(crack1, crackMat1);
                    crack2 = BitmapFactory.decodeResource(getResources(), R.drawable.crack2);
                    crackMat2 = new Mat(crack2.getHeight(), crack2.getWidth(), CV_8UC4);
                    Utils.bitmapToMat(crack2, crackMat2);
                    flower = BitmapFactory.decodeResource(getResources(), R.drawable.flower);
                    flowerMat = new Mat(flower.getHeight(), flower.getWidth(), CV_8UC4);
                    Utils.bitmapToMat(flower, flowerMat);

                    Imgproc.resize(crackMat, crackMat, new Size(res_width, res_height));
                    Imgproc.resize(crackMat1, crackMat1, new Size(res_width, res_height));
                    Imgproc.resize(crackMat2, crackMat2, new Size(res_width, res_height));
                    Imgproc.resize(flowerMat, flowerMat, new Size(res_width, res_height));
                } break;
                default:
                {
                    Log.i(TAG, "OpenCV load failed");
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(FEATURE_ACTION_BAR_OVERLAY);
        ActionBar action = getActionBar();
        action.hide();
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (MyCameraView) findViewById(R.id.activity_main);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        //フルスクリーンにする
        mOpenCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mOpenCvCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cracked = 0;
                ActionBar action = getActionBar();
                action.show();
            }
        });
        mOpenCvCameraView.setCvCameraViewListener(this);

        AudioAttributes attr = new AudioAttributes.Builder() //音の登録
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attr)
                .setMaxStreams(2)
                .build();
        sound_crack = soundPool.load(this, R.raw.crack, 1);
        sound_explode = soundPool.load(this, R.raw.explode, 1);
        sound_bloom = soundPool.load(this, R.raw.bloom, 1);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean ret = super.onKeyUp(keyCode, event);
        if(keyCode == KeyEvent.KEYCODE_CAMERA) {
            isConnected = true;
            faceDetect2(src);
        }
        return ret;
    }

    @Override
    public void onWindowFocusChanged( boolean hasFocus ) {
        super.onWindowFocusChanged(hasFocus);
        Log.i(TAG, "onWindowFocusChanged1()"); //操作をした後フルスクリーンに戻す処理
        mOpenCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.item_punch:
                mode = 0; //パンチモード
                break;
            case R.id.item_face:
                mode = 1; //顔認識モード(女性とお年寄りに優しい)
                break;
            case R.id.item_equal:
                mode = 2; //顔認識モード(全人類平等)
                break;
            case R.id.item_flip:
                //bitwise not operation to flip 1 to 0 and vice versa
                cameraId ^= 1;
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.setCameraIndex(cameraId);
                mOpenCvCameraView.enableView();
                break;
        }
        isDetected = false;
        isBeauty = false;
        gender = "?";
        jsonByte = null;
        isConnected = false;
        ActionBar action = getActionBar();
        action.hide();
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        src = inputFrame.rgba();//入力画像
        Mat dst = src.clone();
        RectF rect = faceDetect(src); //AndroidのAPIで顔認識
        if(mode == 0) { //パンチモード
            Imgproc.rectangle(src, new Point(rect.left, rect.top), new Point(rect.right, rect.bottom), new Scalar(0, 0, 0, 0), -1);
            double maxArea = getMaxSkinArea(src); //顔を除いて最も大きい肌色領域(手を想定)を検出
            if (maxArea - fist > 200000) { //近づく速度が閾値を超えたら割れる(3回まで)
                cracked++;
                soundPool.play(sound_crack, 1.0f, 1.0f, 0, 0, 1);
                if (cracked == 3) {
                    soundPool.play(sound_explode, 1.0f, 1.0f, 0, 0, 1);
                }
            }
            fist = maxArea;
        }
        else{ //美顔認識モード
            if(!isDetected) {
                Log.i(TAG, "face mode");
                //faceDetect2(src); //Face++の顔認識APIをたたく
                getScore(jsonByte);
                //beautyが一定以下だと割れる，一定以上だと花の演出
                if(gender.equals("Male") && beauty < 65 && (mode == 2 || age < 40) || mode == 2 && gender.equals("Female") && beauty < 65){
                    soundPool.play(sound_explode, 1.0f, 1.0f, 0, 0, 1);
                    soundPool.play(sound_crack, 1.0f, 1.0f, 0, 0, 1);
                    cracked = 3;
                    isBeauty = false;
                }
                else if(!isBeauty && (gender.equals("Female") || gender.equals("Male") && (beauty >= 65 || age >= 40))){
                    soundPool.play(sound_bloom, 1.0f, 1.0f, 0, 0, 1);
                    cracked = 0;
                    isBeauty = true;
                }
                else{
                    if(isConnected){
                        Imgproc.rectangle(dst, new Point(rect.left, rect.top), new Point(rect.right, rect.bottom), new Scalar(255, 0, 0, 255), 3);
                    }
                    else {
                        Imgproc.rectangle(dst, new Point(rect.left, rect.top), new Point(rect.right, rect.bottom), new Scalar(0, 255, 0, 255), 3);
                    }
                    isBeauty = false;
                }
            }
        }
        if(cracked > 2){
            Imgproc.rectangle(dst, new Point(0, 0), new Point(dst.width(), dst.height()), new Scalar(0, 0, 0, 0), -1);
            Core.addWeighted(dst, 1.0, crackMat2, 1.0, 0, dst);
        }
        if(cracked > 1){
            Core.addWeighted(dst, 1.0, crackMat, 1.0, 0, dst);
        }
        if(cracked > 0){
            Core.addWeighted(dst, 1.0, crackMat1, 1.0, 0, dst);
        }
        if(isBeauty){
            Core.addWeighted(dst, 1.0, flowerMat, 1.0, 0, dst);
        }
        return dst;
    }
    public static double getMaxSkinArea(Mat rgba) { //最大肌色領域を検出する関数
        if (rgba == null) {
            throw new IllegalArgumentException("parameter must not be null");
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV);
        Scalar low = new Scalar( 0,0,88);//下限(H,S,V)
        Scalar high = new Scalar(30,255,255);//上限(H,S,V)
        Core.inRange(hsv, low, high, hsv);
        Imgproc.medianBlur(hsv, hsv, 3);

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat(hsv.cols(), hsv.rows(), CvType.CV_32SC1);
        Imgproc.findContours(hsv, contours, hierarchy, Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_NONE);

        double maxArea = 0.0f;
        for (int i = 0; i < contours.size(); i++) {
            double tmpArea = Imgproc.contourArea(contours.get(i));
            if (maxArea < tmpArea) {
                maxArea = tmpArea;
            }
        }
        return maxArea;
    }

    public RectF faceDetect(Mat src){ //AndroidのAPIで顔認識，一番大きい顔をとる
        detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        RectF faceRect = new RectF(0, 0, 0 ,0);
        Bitmap bmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(src, bmp);
        Frame frame = new Frame.Builder().setBitmap(bmp).build();
        SparseArray<Face> faces = detector.detect(frame);
        Log.d(TAG, "size:"+Integer.toString(faces.size()));
        if (!detector.isOperational()) {
            return faceRect;
        }
        for (int i = 0; i < faces.size(); i++) {
            Face face = faces.valueAt(i);
            if(face != null){
                final PointF position = face.getPosition();
                if (faceRect.width() * faceRect.height() < face.getWidth() * face.getHeight()) {
                    faceRect = new RectF(position.x,
                            position.y,
                            position.x + face.getWidth(),
                            position.y + face.getHeight()*3/2);
                }
            }
            else {
                Log.d(TAG, Integer.toString(i));
            }
        }
        detector.release();
        return faceRect;
    }
    
    private static class FaceDetectTask extends AsyncTask<String, Void, byte[]> { //Face++APIに接続する非同期タスク
        private WeakReference<MainActivity> activityRef;
        private final String facepp_url;
        private final String facepp_api_key;
        private final String facepp_api_secret;
        FaceDetectTask(MainActivity activity) {
            activityRef = new WeakReference<>(activity);
            facepp_url = activity.getResources().getString(R.string.facepp_url);
            facepp_api_key = activity.getResources().getString(R.string.facepp_api_key);
            facepp_api_secret = activity.getResources().getString(R.string.facepp_api_secret);
        }

        @Override
        protected byte[] doInBackground(String ... bitmapStr) { //カメラで撮った画像をBase64にして送信
            String boundary = getBoundary();
            HttpURLConnection conn = null;
            String error = null;
            try {
                URL url = new URL(facepp_url);
                conn = (HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("accept", "*/*");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("connection", "Keep-Alive");
                DataOutputStream obos = new DataOutputStream(conn.getOutputStream());


                String key = "api_key";
                String value = facepp_api_key;
                obos.writeBytes("--" + boundary + "\r\n");
                obos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n");
                obos.writeBytes("\r\n");
                obos.writeBytes(value + "\r\n");

                key = "api_secret";
                value = facepp_api_secret;
                obos.writeBytes("--" + boundary + "\r\n");
                obos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n");
                obos.writeBytes("\r\n");
                obos.writeBytes(value + "\r\n");

                obos.writeBytes("--" + boundary + "\r\n");
                obos.writeBytes("Content-Disposition: form-data; name=\"" + "return_attributes" + "\"\r\n");
                obos.writeBytes("\r\n");
                obos.writeBytes("gender,beauty,age" + "\r\n");

                obos.writeBytes("--" + boundary + "\r\n");
                obos.writeBytes("Content-Disposition: form-data; name=\"" + "image_base64" + "\"\r\n");
                obos.writeBytes("\r\n");
                obos.writeBytes(bitmapStr[0]);
                obos.writeBytes("\r\n");

                obos.writeBytes("--" + boundary + "--" + "\r\n");
                obos.writeBytes("\r\n");
                obos.flush();
                obos.close();
                InputStream ins = null;
                int code = conn.getResponseCode();
                try{
                    if(code == 200){
                        Log.i(TAG, Integer.toString(code));
                        ins = conn.getInputStream();
                    }
                    else{
                        Log.i(TAG, Integer.toString(code));
                        ins = conn.getErrorStream();
                        activityRef.get().isConnected = false;
                        }
                }catch (SSLException e)
                {
                    e.printStackTrace();
                    return null;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buff = new byte[4096];
                int len;
                while((len = ins.read(buff)) != -1){
                    baos.write(buff, 0, len);
                }
                byte[] bytes = baos.toByteArray();
                Log.i(TAG, new String(bytes));
                ins.close();
                return bytes;
            }
            catch (Exception e) {
                error = e.toString();
            }
            finally {
                if (conn != null) conn.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(byte[] result) {
            MainActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing())
                return;
            activity.jsonByte = result;
        }
    }

    public void faceDetect2(Mat src){ //非同期タスクを起動する関数
        Bitmap bmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(src, bmp);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String bitmapStr = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        Log.i(TAG, "here");
        new FaceDetectTask(MainActivity.this).execute(bitmapStr);
    }

    public static String getBoundary() { //httpリクエストの境界を生成する
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i = 0; i < 32; ++i) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".charAt(random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_".length())));
        }
        return sb.toString();
    }

    public void getScore(byte[] jsonByte) { //Face++の結果をパースする関数
        if(jsonByte != null) {
            try {
                JSONObject json = new JSONObject(new String(jsonByte));
                gender = json.getJSONArray("faces").getJSONObject(0).getJSONObject("attributes").getJSONObject("gender").getString("value");
                if (gender.equals("Male")) {
                    beauty = json.getJSONArray("faces").getJSONObject(0).getJSONObject("attributes").getJSONObject("beauty").getDouble("male_score");
                    isDetected = true;
                } else if (gender.equals("Female")) {
                    beauty = json.getJSONArray("faces").getJSONObject(0).getJSONObject("attributes").getJSONObject("beauty").getDouble("female_score");
                    isDetected = true;
                } else {
                    gender = "?";
                }
                age = json.getJSONArray("faces").getJSONObject(0).getJSONObject("attributes").getJSONObject("age").getInt("value");
            } catch (JSONException e) {
                gender = "?";
            }
            Log.i(TAG, gender);
        }
    }
}
