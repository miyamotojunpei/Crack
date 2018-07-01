package com.example.miyamotojunpei.crack;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
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
    boolean cracked = false;
    SoundPool soundPool;
    private int sound_crack;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    //mOpenCvCameraView.setMaxFrameSize(864, 480);
                    mOpenCvCameraView.setResolution(864, 480);
                    crack = BitmapFactory.decodeResource(getResources(), R.drawable.crack);
                    crackMat = new Mat(crack.getHeight(), crack.getWidth(), CV_8UC4);
                    Utils.bitmapToMat(crack, crackMat);
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (MyCameraView) findViewById(R.id.activity_main);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cracked = false;
            }
        });
        mOpenCvCameraView.setCvCameraViewListener(this);
        AudioAttributes attr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attr)
                .setMaxStreams(1)
                .build();
        sound_crack = soundPool.load(this, R.raw.crack, 1);
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
        Imgproc.resize(crackMat, crackMat, src.size());
        RectF rect = faceDetect(src);
        Imgproc.rectangle(src, new Point(rect.left, rect.top), new Point(rect.right, rect.bottom), new Scalar(0, 0, 0, 0), -1);
        MatOfPoint maxArea = getMaxSkinArea(src);
        if (maxArea != null) {
            Rect rectOfArea = Imgproc.boundingRect(maxArea);
            if(rectOfArea.area() - fist > 300000){
                cracked = true;
                soundPool.play(sound_crack, 1.0f, 1.0f, 0, 0, 1);
            }
            if(cracked){
                Core.addWeighted(src, 1.0, crackMat, 1.0, 0, src);
            }
            else {
                Imgproc.rectangle(src, rectOfArea.tl(), rectOfArea.br(), new Scalar(0, 255, 0), 3);
            }
            fist = rectOfArea.area();
        }
            /*Mat dst = Mat.zeros(src.width(),src.height(),CV_8U);//初期化
               Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2HSV);//HSVに変換
               Mat src2 = dst;//HSV画像を代入
               Mat dst2 = Mat.zeros(src.width(),src.height(),CV_8U);//初期化
               Mat dst3 = Mat.zeros(src.width(),src.height(),CV_8U);//初期化
               Scalar low = new Scalar( 0,58,88);//下限(H,S,V)
               Scalar high = new Scalar(25,173,229);//上限(H,S,V)
               Imgproc.medianBlur(src2, src2, 3);
               Core.inRange( src2,  low,  high , dst2);//肌色抽出
               //Imgproc.cvtColor(dst2, dst3, Imgproc.COLOR_HSV2BGR);
               return dst2;*///return inputFrame.rgba();
        return src;
    }
    public static MatOfPoint getMaxSkinArea(Mat rgba) {
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

        if(contours.size() > 0) {
            double maxArea = 0.0f;
            int maxIdx = 0;
            for (int i = 0; i < contours.size(); i++) {
                double tmpArea = Imgproc.contourArea(contours.get(i));
                if (maxArea < tmpArea) {
                    maxArea = tmpArea;
                    maxIdx = i;
                }
            }

            return contours.get(maxIdx);
        }
        else{
            return null;
        }
    }

    RectF faceDetect(Mat src){
        detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();
        RectF faceRect = new RectF(0, 0, 100 ,100);
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
                            position.y + face.getHeight());
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
