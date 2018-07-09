package com.example.miyamotojunpei.crack;

import android.content.Context;
import android.util.AttributeSet;


import org.opencv.android.JavaCameraView;

public class MyCameraView extends JavaCameraView{

    public MyCameraView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    public void setResolution(int width, int height) {
        disconnectCamera();
        mMaxHeight = height;
        mMaxWidth = width;
        connectCamera(getWidth(), getHeight());
    }

}