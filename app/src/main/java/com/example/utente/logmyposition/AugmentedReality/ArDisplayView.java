package com.example.utente.logmyposition.AugmentedReality;

import android.app.Activity;
import android.content.Context;
import android.graphics.Camera;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by utente on 31/07/2016.
 */
public class ArDisplayView extends SurfaceView implements SurfaceHolder.Callback {
    public static final String DEBUG_TAG = "ArDisplayView Log";
    //Camera mCamera;
    SurfaceHolder mHolder;
    Activity mActivity;

    public ArDisplayView(Context context, Activity activity) {
        super(context);

        mActivity = activity;
        mHolder = getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(this);

    }

    public void surfaceCreated(SurfaceHolder holder) {
        //mCamera = Camera.open();

        //android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        //Camera.getCameraInfo(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        //cam.setDisplayOrientation((info.orientation - degrees + 360) % 360);

//        try {
//            mCamera.setPreviewDisplay(mHolder);
//        } catch (IOException e) {
//            Log.e(DEBUG_TAG, "surfaceCreated exception: ", e);
//        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
//        Camera.Parameters params = mCamera.getParameters();
//        List<Size> prevSizes = params.getSupportedPreviewSizes();
//        for (Size s : prevSizes)
//        {
//            if((s.height <= height) && (s.width <= width))
//            {
//                params.setPreviewSize(s.width, s.height);
//                break;
//            }
//        }
//
//        mCamera.setParameters(params);
//        mCamera.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
//        cam.stopPreview();
//        cam.release();
    }
}