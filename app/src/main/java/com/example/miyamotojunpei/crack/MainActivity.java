package com.example.miyamotojunpei.crack;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONArray;
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
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.SSLException;

import static android.view.Window.FEATURE_ACTION_BAR_OVERLAY;
import static org.opencv.core.CvType.CV_8UC4;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "Crack::Activity";
    private MyCameraView mOpenCvCameraView;

    FaceDetector detector;
    int mode = 0;
    double fist = 1000000.0;
    Bitmap crack;
    Mat crackMat;
    Bitmap crack1;
    Mat crackMat1;
    Bitmap crack2;
    Mat crackMat2;
    Bitmap flower;
    Mat flowerMat;
    int cracked = 0;
    SoundPool soundPool;
    private int sound_crack;
    private int sound_explode;
    private int sound_bloom;
    String gender = "?";
    double beauty = 0;
    int age = 0;
    boolean isFemale = false;
    boolean isDetected = false;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setResolution(864, 480);
                    crack = BitmapFactory.decodeResource(getResources(), R.drawable.crack);
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

                    Imgproc.resize(crackMat, crackMat, new Size(864, 480));
                    Imgproc.resize(crackMat1, crackMat1, new Size(864, 480));
                    Imgproc.resize(crackMat2, crackMat2, new Size(864, 480));
                    Imgproc.resize(flowerMat, flowerMat, new Size(864, 480));
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
        AudioAttributes attr = new AudioAttributes.Builder()
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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
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
    public void onWindowFocusChanged( boolean hasFocus ) {
        super.onWindowFocusChanged(hasFocus);
        Log.i(TAG, "onWindowFocusChanged1()");
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
                mode = 0;
                break;
            case R.id.item_face:
                mode = 1;
                break;
            case R.id.item_equal:
                mode = 2;
                break;
        }
        isDetected = false;
        isFemale = false;
        gender = "?";
        ActionBar action = getActionBar();
        action.hide();
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat src = inputFrame.rgba();//入力画像
        Mat dst = src.clone();
        RectF rect = faceDetect(src);
        if(mode == 0) {
            Imgproc.rectangle(src, new Point(rect.left, rect.top), new Point(rect.right, rect.bottom), new Scalar(0, 0, 0, 0), -1);
            double maxArea = getMaxSkinArea(src);
            if (maxArea - fist > 200000) {
                cracked++;
                soundPool.play(sound_crack, 1.0f, 1.0f, 0, 0, 1);
                if (cracked == 3) {
                    soundPool.play(sound_explode, 1.0f, 1.0f, 0, 0, 1);
                }
            }
            fist = maxArea;
        }
        else{
            if(rect.height() > 0 && !isDetected) {
                faceDetect2(src);
                if(gender.equals("Male") && beauty < 65 && (mode == 2 || age < 40) || mode == 2 && gender.equals("Female") && beauty < 65){
                    soundPool.play(sound_explode, 1.0f, 1.0f, 0, 0, 1);
                    soundPool.play(sound_crack, 1.0f, 1.0f, 0, 0, 1);
                    cracked = 3;
                    isFemale = false;
                    isDetected = true;
                }
                else if(!isFemale && (gender.equals("Female") || gender.equals("Male") && beauty >= 65)){
                    isFemale = true;
                    cracked = 0;
                    soundPool.play(sound_bloom, 1.0f, 1.0f, 0, 0, 1);
                    isDetected = true;
                }
                else{
                    isFemale = false;
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
        if(isFemale){
            Core.addWeighted(dst, 1.0, flowerMat, 1.0, 0, dst);
        }
        return dst;
    }
    public static double getMaxSkinArea(Mat rgba) {
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

    RectF faceDetect(Mat src){
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
    private static class FaceDetectTask extends AsyncTask<String, Void, byte[]> {
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
        protected byte[] doInBackground(String ... bitmapStr) {
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
                        ins = conn.getInputStream();
                        Log.i(TAG, Integer.toString(code));
                    }
                    else{
                        Log.i(TAG, Integer.toString(code));
                        ins = conn.getErrorStream();
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
            activity.getScore(result);
        }
    }
    public void faceDetect2(Mat src){
        Bitmap bmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(src, bmp);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String bitmapStr = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        Log.i(TAG, "here");
        new FaceDetectTask(MainActivity.this).execute(bitmapStr);
    }

    private static String getBoundary() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i = 0; i < 32; ++i) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".charAt(random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_".length())));
        }
        return sb.toString();
    }

    public void getScore(byte[] result) {
        try{
            Log.i(TAG, "getgender");
            JSONObject json = new JSONObject(new String(result));
            gender = json.getJSONArray("faces").getJSONObject(0).getJSONObject("attributes").getJSONObject("gender").getString("value");
            if(gender.equals("Male")){
                beauty = json.getJSONArray("faces").getJSONObject(0).getJSONObject("attributes").getJSONObject("beauty").getDouble("male_score");
            }
            else if(gender.equals("Female")){
                beauty = json.getJSONArray("faces").getJSONObject(0).getJSONObject("attributes").getJSONObject("beauty").getDouble("female_score");
            }
            age = json.getJSONArray("faces").getJSONObject(0).getJSONObject("attributes").getJSONObject("age").getInt("value");
        }
        catch (JSONException e){
            gender =  "?";
        }
        Log.i(TAG,gender);
    }
}
