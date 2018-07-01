package com.example.miyamotojunpei.crack;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.View;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

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

import java.util.ArrayList;

import static org.opencv.core.CvType.CV_8UC4;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "Crack::Activity";
    private MyCameraView mOpenCvCameraView;

    FaceDetector detector;
    double fist = 1000000.0;
    Bitmap crack;
    Mat crackMat;
    Bitmap crack1;
    Mat crackMat1;
    Bitmap crack2;
    Mat crackMat2;
    int cracked = 0;
    SoundPool soundPool;
    private int sound_crack;
    private int sound_explode;
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
                    Imgproc.resize(crackMat, crackMat, new Size(864, 480));
                    Imgproc.resize(crackMat1, crackMat1, new Size(864, 480));
                    Imgproc.resize(crackMat2, crackMat2, new Size(864, 480));
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
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (MyCameraView) findViewById(R.id.activity_main);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mOpenCvCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cracked = 0;
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

    public void onCameraViewStarted(int width, int height) {


    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat src = inputFrame.rgba();//入力画像
        Mat dst = src.clone();
        RectF rect = faceDetect(src);
        Imgproc.rectangle(src, new Point(rect.left, rect.top), new Point(rect.right, rect.bottom), new Scalar(0, 0, 0, 0), -1);
        double maxArea = getMaxSkinArea(src);
        if(maxArea - fist > 200000){
            cracked++;
            soundPool.play(sound_crack, 1.0f, 1.0f, 0, 0, 1);
           if(cracked == 3){
                soundPool.play(sound_explode, 1.0f, 1.0f, 0, 0, 1);
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
        fist = maxArea;
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
}
