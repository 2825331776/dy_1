/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.dyt.wcc.cameracommon.widget;

import static com.serenegiant.glutils.ShaderConst.GL_TEXTURE_2D;
import static com.serenegiant.glutils.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.dyt.wcc.cameracommon.encoder.IVideoEncoder;
import com.dyt.wcc.cameracommon.encoder.MediaEncoder;
import com.dyt.wcc.cameracommon.encoder.MediaVideoEncoder;
import com.dyt.wcc.common.base.BaseApplication;
import com.dyt.wcc.common.utils.TempConvertUtils;
import com.dyt.wcc.common.widget.dragView.DragTempContainer;
import com.dyt.wcc.common.widget.dragView.DrawLineRecHint;
import com.dyt.wcc.common.widget.dragView.TempWidgetObj;
import com.serenegiant.glutils.EGLBase;
import com.serenegiant.usb.ITemperatureCallback;
import com.serenegiant.utils.FpsCounter;

import java.text.DecimalFormat;


/**
 * change the view size with keeping the specified aspect ratio.
 * if you set this view with in a FrameLayout and set property "android:layout_gravity="center",
 * you can show this view in the center of screen and keep the aspect ratio of content
 * XXX it is better that can set the aspect ratio as xml property
 */
public class UVCCameraTextureView extends AspectRatioTextureView    // API >= 14
        implements TextureView.SurfaceTextureListener, CameraViewInterface {

    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "UVCCameraTextureView";


    private boolean mHasSurface;
    private RenderHandler mRenderHandler;
    private final Object mCaptureSync = new Object();
    private Bitmap mTempBitmap;
    private boolean mRequestCaptureStillImage;
    private Callback mCallback;

    /**
     * for calculation of frame rate
     * 用于计算帧率
     */
    private final FpsCounter mFpsCounter = new FpsCounter();

    public UVCCameraTextureView(final Context context) {
        this(context, null, 0);
    }

    public UVCCameraTextureView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UVCCameraTextureView(final Context context, final AttributeSet attrs, final int defStyle) {

        super(context, attrs, defStyle);
        Log.e(TAG, "UVCCameraTextureView: "+ System.currentTimeMillis());
        Log.e("UVCCameraTextureView","===============constructor");
        setSurfaceTextureListener(this);
        Log.e(TAG, "UVCCameraTextureView: "+ System.currentTimeMillis());
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.v(TAG, "onResume:");
        if (mHasSurface) {
            mRenderHandler = RenderHandler.createHandler(mFpsCounter, super.getSurfaceTexture(), getWidth(), getHeight());
        }
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.v(TAG, "onPause:");
        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        if (mTempBitmap != null) {
            mTempBitmap.recycle();
            mTempBitmap = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
        //if (DEBUG) Log.v(TAG, "onSurfaceTextureAvailable:" + surface);
        Log.e(TAG, "onSurfaceTextureAvailable:" + surface);
        if (mRenderHandler == null) {
            mRenderHandler = RenderHandler.createHandler(mFpsCounter, surface, width, height);
        } else {
            mRenderHandler.resize(width, height);
        }
        mHasSurface = true;
        if (mCallback != null) {
            mCallback.onSurfaceCreated(this, getSurface());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
        if (DEBUG) Log.v(TAG, "onSurfaceTextureSizeChanged:" + surface);
        if (mRenderHandler != null) {
            mRenderHandler.resize(width, height);
        }
        if (mCallback != null) {
            mCallback.onSurfaceChanged(this, getSurface(), width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        if (DEBUG) Log.v(TAG, "onSurfaceTextureDestroyed:" + surface);
        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        mHasSurface = false;
        if (mCallback != null) {
            mCallback.onSurfaceDestroy(this, getSurface());
        }
        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
        synchronized (mCaptureSync) {
            if (mRequestCaptureStillImage) {
                mRequestCaptureStillImage = false;
                if (mTempBitmap == null)
                    mTempBitmap = getBitmap();
                else
                    getBitmap(mTempBitmap);
                mCaptureSync.notifyAll();
            }
        }
    }

    @Override
    public boolean hasSurface() {
        return mHasSurface;
    }

    /**
     * capture preview image as a bitmap
     * this method blocks current thread until bitmap is ready
     * if you call this method at almost same time from different thread,
     * the returned bitmap will be changed while you are processing the bitmap
     * (because we return same instance of bitmap on each call for memory saving)
     * if you need to call this method from multiple thread,
     * you should change this method(copy and return)
     */
    @Override
    public Bitmap captureStillImage() {
        synchronized (mCaptureSync) {
            mRequestCaptureStillImage = true;
            try {
                mCaptureSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            return mTempBitmap;
        }
    }

    public void openSysCamera() {
        if (mRenderHandler != null) {
            mRenderHandler.openSysCamera();
        }
    }

    public void closeSysCamera() {
        if (mRenderHandler != null) {
            mRenderHandler.closeSysCamera();
        }
    }

    public float[] GetTemperatureData() {
        return mRenderHandler != null ? mRenderHandler.GetTemperatureData() : null;
    }


    @Override
    public SurfaceTexture getSurfaceTexture() {
        return mRenderHandler != null ? mRenderHandler.getPreviewTexture() : null;
    }

    private Surface mPreviewSurface;

    @Override
    public Surface getSurface() {
        if (DEBUG) Log.v(TAG, "getSurface:hasSurface=" + mHasSurface);
        if (mPreviewSurface == null) {
            final SurfaceTexture st = getSurfaceTexture();
            if (st != null) {
                mPreviewSurface = new Surface(st);
            }
        }
        return mPreviewSurface;
    }

    @Override
    public void setVideoEncoder(final IVideoEncoder encoder) {
        if (mRenderHandler != null)
            mRenderHandler.setVideoEncoder(encoder);


//        Log.e(TAG, "setVideoEncoder: ");
    }

//    public int mSupportWidth;
//    public int mSupportHeight;


//    public void setSupportWidth(int width) {
//        mSupportWidth = width;
//    }
//
//    public void setSupportHeight(int height) {
//        mSupportHeight = height;
//    }

    @Override
    public void setCallback(final Callback callback) {
        mCallback = callback;
    }

    @Override
    public ITemperatureCallback getTemperatureCallback() {
        return mRenderHandler != null ? mRenderHandler.getTemperatureCallback() : null;
    }
    public int getFeaturePointsControl () {
        return mRenderHandler != null ? mRenderHandler.getFeaturePointsControl() : 0;
    }

//    @Override
//    public TcpITemperatureCallback getTcpTemperatureCallback() {
//        return mRenderHandler != null ? mRenderHandler.getTcpTemperatureCallback() : null;
//    }

    public void setVertices(float scale) {
        if (mRenderHandler != null) {
            mRenderHandler.setVertices(scale);
        }
    }

    public void setTemperatureCbing(boolean isTempCbing) {
        if (mRenderHandler != null) {
            mRenderHandler.setTemperatureCbing(isTempCbing);
        }
    }

    public void setSuportWH(int w, int h) {
//        mSupportWidth = w;
//        mSupportHeight = h;
        if (mRenderHandler != null) {
            mRenderHandler.setSuportWH(w, h);
        }
    }

    /**
     * 设置 最高温  最低温 中心点温度 正常点温度 图片 ，及其 每个图片占用的像素大小
     * @param highTemp
     * @param lowTemp
     * @param centerTemp
     * @param normalPointTemp
     * @param halfBitmap
     */
    public void setFrameBitmap(Bitmap highTemp, Bitmap lowTemp, Bitmap centerTemp, Bitmap normalPointTemp,int halfBitmap) {
        if (mRenderHandler != null) {
            mRenderHandler.setFrameBitmap(highTemp, lowTemp, centerTemp, normalPointTemp,halfBitmap);
        }
    }

    public void iniTempBitmap(int w, int h) {
        if (mRenderHandler != null) {
            mRenderHandler.iniTempBitmap(w, h);
        }
    }
    public void setVidPid(int vid ,int pid){
        if (mRenderHandler != null) {
            mRenderHandler.setVidPid(vid, pid);
        }
    }
    public void setTinyCCorrection(float tinyCorrection){
        if (mRenderHandler != null) {
            mRenderHandler.setTinyCCorrection(tinyCorrection);
        }
    }

//    public void setBindSeekBar(CustomRangeSeekBar SeekBar) {
//        if (mRenderHandler != null) {
//            mRenderHandler.setBindSeekBar(SeekBar);
//        }
//    }

    public void setDragTempContainer(DragTempContainer dragTempContainer){
        if (mRenderHandler != null) {
            mRenderHandler.setDragTempContainer(dragTempContainer);
        }
    }

//    public void setBitmap(Bitmap r, Bitmap g, Bitmap b, Bitmap y, Bitmap l) {
//        mRenderHandler.setBitmap(r, g, b, y, l);
//    }
//
//    public void setTouchPoint(CopyOnWriteArrayList<TouchPoint> touchPoint) {
//        mRenderHandler.setTouchPoint(touchPoint);
//    }
//
//    public void setTouchLine(CopyOnWriteArrayList<TouchLine> touchLine) {
//        mRenderHandler.setTouchLine(touchLine);
//    }
//
//    public void setTouchArea(CopyOnWriteArrayList<TouchArea> touchArea) {
//        mRenderHandler.setTouchArea(touchArea);
//    }

    public void setTemperatureAnalysisMode(int mode) {
        mRenderHandler.setTemperatureAnalysisMode(mode);
    }


    public void relayout(int rotate) {
        mRenderHandler.relayout(rotate);
    }

    //UVCCameraTextureView 设置开启 关闭 高低温警告
    public void startTempAlarm(float markAlarmTemp){mRenderHandler.startTempAlarm(markAlarmTemp);}

    public void stopTempAlarm(){mRenderHandler.stopTempAlarm();}

//    public void setOnHighTempChangedCallback(onHighTempChangedCallback callback){ mRenderHandler.setOnHighTempChangedCallback(callback); }

//    public void watermarkOnOff(boolean isWatermaker) {
//        mRenderHandler.watermarkOnOff(isWatermaker);
//    }

    public void tempShowOnOff(boolean isTempShow) {
        mRenderHandler.tempshowOnOff(isTempShow);
    }
    public void setDrawHint(DrawLineRecHint hint){
        mRenderHandler.setDrawHint(hint);
    }
    /**
     * 打开 最高温0  最低温1  中心点温度1 特诊点绘制
     * @param pointType 最高温0  最低温1  中心点温度1
     */
    public void openFeaturePoints(int pointType){
        mRenderHandler.openFeaturePoints(pointType);
    }
    /**
     * 关闭 最高温0  最低温1  中心点温度1 特诊点绘制
     * @param pointType 最高温0  最低温1  中心点温度1
     */
    public void closeFeaturePoints(int pointType){
        mRenderHandler.closeFeaturePoints(pointType);
    }

//    public void setUnitTemperature(int mode) {
//        mRenderHandler.setUnitTemperature(mode);
//    }

    public void resetFps() {
        mFpsCounter.reset();
    }

    /**
     * update frame rate of image processing
     */
    public void updateFps() {
        mFpsCounter.update();
    }

    /**
     * get current frame rate of image processing
     * @return  得到当前 图像的 帧速
     */
    public float getFps() {
        return mFpsCounter.getFps();
    }

    /**
     * get total frame rate from start
     * @return  得到当前图像的 开始帧速
     */
    public float getTotalFps() {
        return mFpsCounter.getTotalFps();
    }

    /**
     * render camera frames on this view on a private thread
     *在私有线程上在此视图上渲染相机帧
     * @author saki
     */
    private static final class RenderHandler extends Handler
            implements SurfaceTexture.OnFrameAvailableListener {

        private static final int MSG_REQUEST_RENDER = 1;
        private static final int MSG_SET_ENCODER = 2;
        private static final int MSG_CREATE_SURFACE = 3;
        private static final int MSG_RESIZE = 4;
        private static final int MSG_TERMINATE = 9;
        private static final int MSG_CHECK_AREA = 5;

        private RenderThread mThread;
        private boolean mIsActive = true;
        private final FpsCounter mFpsCounter;


        public static final RenderHandler createHandler(final FpsCounter counter,
                                                        final SurfaceTexture surface, final int width, final int height) {

            final RenderThread thread = new RenderThread(counter, surface, width, height);
            thread.start();
            return thread.getHandler();
        }

        private RenderHandler(final FpsCounter counter, final RenderThread thread) {
            mThread = thread;
            mFpsCounter = counter;
        }

//        public void setBitmap(Bitmap r, Bitmap g, Bitmap b, Bitmap y, Bitmap l) {
//            mThread.setBitmap(r, g, b, y, l);
//        }

//        public void setTouchPoint(CopyOnWriteArrayList<TouchPoint> touchPoint) {
//            mThread.setTouchPoint(touchPoint);
//        }
//
//        public void setTouchLine(CopyOnWriteArrayList<TouchLine> touchLine) {
//            mThread.setTouchLine(touchLine);
//        }
//
//        public void setTouchArea(CopyOnWriteArrayList<TouchArea> touchArea) {
//            mThread.setTouchArea(touchArea);
//        }

        public void setTemperatureAnalysisMode(int mode) {
            mThread.setTemperatureAnalysisMode(mode);
        }

        public void relayout(int rotate) {
            mThread.relayout(rotate);
        }

        //RenderHandler 设置开启 关闭 高低温警告
        public void startTempAlarm(float markAlarmTemp){mThread.startTempAlarm(markAlarmTemp);}

        public void stopTempAlarm(){mThread.stopTempAlarm();}

//        public void setOnHighTempChangedCallback(onHighTempChangedCallback callback){ mThread.highTempChangedCallback = callback; }

        public void setFrameBitmap(Bitmap highTemp, Bitmap lowTemp, Bitmap centerTemp, Bitmap normalPointTemp,int halfBitmap) {
            mThread.setFrameBitmap(highTemp, lowTemp, centerTemp, normalPointTemp,halfBitmap);
        }
        public void setDrawHint(DrawLineRecHint hint){
           mThread.setDrawHint(hint);
        }

        public void tempshowOnOff(boolean isTempShow) {
            mThread.tempshowOnOff(isTempShow);
        }

        public void openFeaturePoints(int pointType){
            mThread.openFeaturePoints(pointType);
        }
        public void closeFeaturePoints(int pointType){
            mThread.closeFeaturePoints(pointType);
        }

//        public void setUnitTemperature(int mode) {
//            mThread.setUnitTemperature(mode);
//        }

        public final void setVideoEncoder(final IVideoEncoder encoder) {
            if (DEBUG) Log.v(TAG, "setVideoEncoder:");
            if (mIsActive)
                sendMessage(obtainMessage(MSG_SET_ENCODER, encoder));
        }

        public final SurfaceTexture getPreviewTexture() {
            if (DEBUG) Log.v(TAG, "getPreviewTexture:");
            if (mIsActive) {
                synchronized (mThread.mSync) {
                    sendEmptyMessage(MSG_CREATE_SURFACE);
                    try {
                        mThread.mSync.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    return mThread.mPreviewSurface;
                }
            } else {
                return null;
            }
        }

        public void resize(final int width, final int height) {
            if (DEBUG) Log.v(TAG, "resize:");
            if (mIsActive) {
                synchronized (mThread.mSync) {
                    sendMessage(obtainMessage(MSG_RESIZE, width, height));
                    try {
                        mThread.mSync.wait();
                    } catch (final InterruptedException e) {e.printStackTrace();
                    }
                }
            }
        }

        public final void release() {
            if (DEBUG) Log.v(TAG, "release:");
            if (mIsActive) {
                mIsActive = false;
                removeMessages(MSG_REQUEST_RENDER);
                removeMessages(MSG_SET_ENCODER);
                sendEmptyMessage(MSG_TERMINATE);
            }
        }

        @Override
        public final void onFrameAvailable(final SurfaceTexture surfaceTexture) {
            if (mIsActive) {
                mFpsCounter.count();
                //onDrawFrame() 会不断循环显示
                sendEmptyMessage(MSG_REQUEST_RENDER);
            }
        }

        public ITemperatureCallback getTemperatureCallback() {
            return mThread != null ? mThread.getTemperatureCallback() : null;
        }
        public int getFeaturePointsControl () {
            return mThread != null ? mThread.getFeaturePointsControl() : 0;
        }
//        public TcpITemperatureCallback getTcpTemperatureCallback() {
//
//            return mThread != null ? mThread.getTcpTemperatureCallback() : null;
//        }

        public void setVertices(float scale) {
            if (mThread != null) {
                mThread.setVertices(scale);
            }
        }

        public void setTemperatureCbing(boolean isTempCbing) {
            if (mThread != null) {
                mThread.setTemperatureCbing(isTempCbing);
            }
        }

        public float[] GetTemperatureData() {
            return mThread != null ? mThread.GetTemperatureData() : null;
        }


        public void openSysCamera() {
            if (mThread != null) {
                mThread.openSysCamera();
            }
        }

        public void closeSysCamera() {
            if (mThread != null) {
                mThread.closeSysCamera();
            }
        }


        public void setSuportWH(int w, int h) {
            if (mThread != null) {
                mThread.setSuportWH(w, h);
            }
        }

//        public void iniTempFontsize(float fontsize) {
//            if (mThread != null) {
//                mThread.iniTempFontsize(fontsize);
//            }
//        }

        public void iniTempBitmap(int w, int h) {
            if (mThread != null) {
                mThread.iniTempBitmap(w, h);
            }
        }
        public void setVidPid(int vid ,int pid){
            if (mThread != null) {
                mThread.setVidPid(vid, pid);
            }
        }
        public void setTinyCCorrection(float tinyCorrection){
            if (mThread != null) {
                mThread.setTinyCCorrection(tinyCorrection);
            }
        }

//        public void setBindSeekBar(CustomRangeSeekBar SeekBar) {
//            if (mThread != null) {
//                mThread.setBindSeekBar(SeekBar);
//            }
//        }
        public void setDragTempContainer(DragTempContainer dragTempContainer){
            if (mThread != null) {
                mThread.setDragTempContainer(dragTempContainer);
            }
        }

        @Override
        public final void handleMessage(final Message msg) {
            //打印当前线程信息
//            Log.e(TAG, "handleMessage= pid="+android.os.Process.myPid()+" tid="+android.os.Process.myTid());
            if (mThread == null) return;
            //Log.v(TAG, "DrawControl:");
            switch (msg.what) {
                case MSG_REQUEST_RENDER:
                    mThread.DrawControl();
                    mThread.onDrawFrame();
                    break;
                case MSG_SET_ENCODER:
                    mThread.setEncoder((MediaEncoder) msg.obj);
                    break;
                case MSG_CREATE_SURFACE:
                    mThread.updatePreviewSurface();
                    break;
                case MSG_RESIZE:
                    mThread.resize(msg.arg1, msg.arg2);
                    break;
                case MSG_TERMINATE:
                    Looper.myLooper().quit();
                    mThread = null;
                    break;

                default:
                    super.handleMessage(msg);
            }
        }

        private static final class RenderThread extends Thread {
            private final Object mSync = new Object();
            private final SurfaceTexture mSurface;
            private RenderHandler mHandler;
            private EGLBase mEgl;
            /**
             * IEglSurface instance related to this TextureView
             * 与此 TextureView 相关的 IEglSurface 实例
             */
            private EGLBase.IEglSurface mEglSurface;
            private GLDrawer2D1 mDrawer;
            private int mTexId = -1;
            private int[] mTexIds = {-1, -1, -1, -1};
            /**
             * SurfaceTexture instance to receive video images
             * 接收视频图像的 SurfaceTexture 实例
             */
            private SurfaceTexture mPreviewSurface, mCamera2Surface;
            private final float[] mStMatrix = new float[16];
            private MediaEncoder mEncoder;
            private int mViewWidth, mViewHeight;
            private final FpsCounter mFpsCounter;
            private Camera2Helper mCamera2Helper;
//            private CustomRangeSeekBar mBindSeekBar;


            //RenderThread 设置开启 关闭 高低温警告
            public void startTempAlarm(float markAlarmTemp){      //开启高温警告
                isOverTempAlarm = true;
                mAlarmTemp = markAlarmTemp;
                alarmCount = 0;
//                Log.e(TAG,"====================设置开启高温警告=============== "+ mAlarmTemp );
            }
            public void stopTempAlarm(){     //关闭高温警告
//                Log.e(TAG,"====================关闭高温警告=============== " );
                isOverTempAlarm = false;
                mAlarmTemp = 1000.0f;
                alarmCount = 0;
            }
            /**
             * constructor
             *
             * @param surface: drawing surface came from TexureView
             */
            public RenderThread(final FpsCounter fpsCounter, final SurfaceTexture surface, final int width, final int height) {
                mFpsCounter = fpsCounter;
                mSurface = surface;
                mViewWidth = width;
                mViewHeight = height;
                mCamera2Helper = Camera2Helper.getInstance();
                // this.highTempRect,lowTempRect,bounds ;//创建一个指定的新矩形的坐标

                setName("RenderThread");
//                initFrameBitmap();
            }

//            private void initFrameBitmap(){
//                highTempBt = BitmapFactory.decodeResource(this, R.mipmap.ic_higlowtemp_draw_widget_high);
//            }

//            public void iniTempFontsize(float fontsize) {
//                this.photoPaint.setTextSize(fontsize);
//            }

            /**
             * 初始化 OpenGL ES 的画布的宽高 ，并初始化所需要的画笔。
             * OpenGL ES
             * 初始化文字，图片
             * @param w
             * @param h
             */
            public void iniTempBitmap(int w, int h) {
                this.icon = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); //建立一个空的图画板
                this.bitcanvas = new Canvas(icon);//初始化画布绘制的图像到icon上
//                this.mTouchPointLists = new CopyOnWriteArrayList<>();
//                this.mTouchLineLists = new CopyOnWriteArrayList<>();
//                this.mTouchAreaLists = new CopyOnWriteArrayList<>();
                this.photoPaint = new Paint();
                this.photoPaint.setStrokeWidth(5);
                this.photoPaint.setColor(Color.WHITE);
//                this.alarmPaint = new Paint();
                this.tempTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                this.tempTextPaint.setTextSize(50);
                this.tempTextPaint.setStrokeWidth(5);
                this.tempTextPaint.setStyle(Paint.Style.FILL);

                this.linePaint = new Paint();
                this.linePaint.setStrokeWidth(5);
                this.linePaint.setColor(Color.WHITE);
                this.dottedLinePaint  = new Paint();
                this.dottedLinePaint.setStrokeWidth(5);
                this.dottedLinePaint.setPathEffect ( new DashPathEffect( new float [ ] { 40, 40 }, 0 ) );
                this.dottedLinePaint.setColor(Color.BLACK);

                this.tempTextBgTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                this.tempTextBgTextPaint.setTextSize(50);
                this.tempTextBgTextPaint.setStrokeWidth(5);
                this.tempTextBgTextPaint.setColor(Color.WHITE);
                this.tempTextBgTextPaint.setStyle(Paint.Style.STROKE);


                if (bounds == null) {
                    bounds = new Rect();
                }
                if (dstHighTemp == null) {
                    dstHighTemp = new Rect();
                }
                if (dstLowTemp == null) {
                    dstLowTemp = new Rect();
                }
            }
            public void setVidPid(int vid ,int pid){
               this.mVid = vid;
               this.mPid = pid;
            }
            public void setTinyCCorrection(float tinyCorrection){
                   this.tinyCorrection = tinyCorrection;
            }

//            public void setBindSeekBar(CustomRangeSeekBar SeekBar) {
//                this.mBindSeekBar = SeekBar;
//            }

            public void setDragTempContainer(DragTempContainer dragTempContainer){
                    this.mDragTempContainer = dragTempContainer;
            }

            public final RenderHandler getHandler() {
                if (DEBUG) Log.v(TAG, "RenderThread#getHandler:");
                synchronized (mSync) {
                    // create rendering thread
                    if (mHandler == null)
                        try {
                            mSync.wait();
                        } catch (final InterruptedException e) {e.printStackTrace();
                        }
                }
                return mHandler;
            }

            public void resize(final int width, final int height) {
                if (((width > 0) && (width != mViewWidth)) || ((height > 0) && (height != mViewHeight))) {
                    mViewWidth = width;
                    mViewHeight = height;
                    updatePreviewSurface();
                } else {
                    synchronized (mSync) {
                        mSync.notifyAll();
                    }
                }
            }

            public final void updatePreviewSurface() {
                if (DEBUG) Log.i(TAG, "RenderThread#updatePreviewSurface:");
                synchronized (mSync) {
                    if (mPreviewSurface != null) {
                        if (DEBUG) Log.d(TAG, "updatePreviewSurface:release mPreviewSurface");
                        mPreviewSurface.setOnFrameAvailableListener(null);
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    mEglSurface.makeCurrent();
                    //           if (mTexId >= 0) {
                    //				mDrawer.deleteTex(mTexId);
                    //           }
                    if (mTexIds[0] >= 0 || mTexIds[1] >= 0 || mTexIds[2] >= 0 || mTexIds[3] >= 0) {
                        mDrawer.deleteTexes(mTexIds);
                    }
                    // create texture and SurfaceTexture for input from camera
                    //            mTexId = mDrawer.initTex();
                    int[] para = {4,
                            GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE,
                            GL_TEXTURE_2D, GLES20.GL_NEAREST, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE,
                            GL_TEXTURE_2D, GLES20.GL_NEAREST, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE,
                            GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE};
                    mTexIds = mDrawer.initTexes(para);
                    if (DEBUG) Log.v(TAG, "updatePreviewSurface:tex_id=" + mTexId);
                    mPreviewSurface = new SurfaceTexture(mTexIds[0]);
                    mPreviewSurface.setDefaultBufferSize(mViewWidth, mViewHeight);
                    mPreviewSurface.setOnFrameAvailableListener(mHandler);
                    Log.e(TAG, "mHandler=" + mHandler);
                    // notify to caller thread that previewSurface is ready
                    mSync.notifyAll();
                }
            }

            public final void setEncoder(final MediaEncoder encoder) {
                if (DEBUG) Log.v(TAG, "RenderThread#setEncoder:encoder=" + encoder);
                if (encoder != null && (encoder instanceof MediaVideoEncoder)) {
                    ((MediaVideoEncoder) encoder).setEglContext(mEglSurface.getContext(), mTexIds);
                }
                mEncoder = encoder;
            }

            /*
             * Now you can get frame data as ByteBuffer(as YUV/RGB565/RGBX/NV21 pixel format) using IFrameCallback interface
             * with UVCCamera#setFrameCallback instead of using following code samples.
             */
/*			// for part1
 			private static final int BUF_NUM = 1;
			private static final int BUF_STRIDE = 640 * 480;
			private static final int BUF_SIZE = BUF_STRIDE * BUF_NUM;
			int cnt = 0;
			int offset = 0;
			final int pixels[] = new int[BUF_SIZE];
			final IntBuffer buffer = IntBuffer.wrap(pixels); */
/*			// for part2
			private ByteBuffer buf = ByteBuffer.allocateDirect(640 * 480 * 4);


 */
            /**
             * 高温报警相关变量
             */
            //            private MediaPlayer mMediaPlayer;
            private boolean     isOverTempAlarm = false; //是否开启了高温警告
            private int alarmCount = 0;
            private boolean isAlarm = false;//      标记是否要报警
//            private boolean isAlarmRing = false;//      标记需要播放铃声
            private float mAlarmTemp = 10000.0f;//标记的报警温度  摄氏度

            private boolean isCbTemping = false;    // 测温开关
            private boolean isCamera2ing = false;


//            private Bitmap mCursorBlue, mCursorRed, mCursorYellow, mCursorGreen, mWatermakLogo;
            private float[] temperature1 = new float[640 * 512 + 10];
            private Bitmap highTempBt, lowTempBt , centerTempBt , normalPointBt;
            private RectF bpRectF;//绘制图片矩形
            private int mBitmapRectSize;
            private DragTempContainer mDragTempContainer;
            //绘制线条的画笔，图片画笔
            private Paint linePaint , dottedLinePaint, photoPaint;
            //绘制文字画笔，绘制文字背景颜色的画笔
            private TextPaint tempTextPaint,tempTextBgTextPaint;

            private float maxtemperature;
            private float mintemperature;
            //最高温 最低温 中心点 温度开关 控制变量。 0x0fff 从高位到低位 第一个f 是最高温 第二个f 是最低温 第三个f 是中心点温度 开关
            private int featurePointsControl = 0;

//            private Paint photoPaint, alarmPaint;
//            private Rect highTempRect, lowTempRect ;//创建一个指定的新矩形的坐标centerTempRect
            private Rect dstHighTemp, dstLowTemp, bounds;//创建一个指定的新矩形的坐标
            private Bitmap icon;
            private int mVid = 0,mPid = 0;
            private float tinyCorrection = 0.0f;
            private Canvas bitcanvas;//初始化画布绘制的图像到icon上
//            private CopyOnWriteArrayList<TouchPoint> mTouchPointLists;
//            private CopyOnWriteArrayList<TouchLine> mTouchLineLists;
//            private CopyOnWriteArrayList<TouchArea> mTouchAreaLists;
//            private CopyOnWriteArrayList<MyMoveWidget> mMoveWidgets;
            private int temperatureAnalysisMode=-1;//, UnitTemperature
            private int rotate = 0;
            private float widthRatio, heightRatio;

            //added by wupei, we may not need mWatermakLogo and fix it ！！！
//            private boolean isWatermaker = false;
            private boolean         isTempShow    = true;
            private DrawLineRecHint myDrawHint    = null;
            private DecimalFormat   decimalFormat = new DecimalFormat("0.0");//构造方法的字符格式这里如果小数不足2位,会以0补足.

            public void setFrameBitmap(Bitmap highTemp, Bitmap lowTemp, Bitmap centerTemp, Bitmap normalPointTemp, int bpSize) {
                highTempBt = highTemp;
                lowTempBt = lowTemp;
                centerTempBt = centerTemp;
                normalPointBt = normalPointTemp;
                this.mBitmapRectSize = bpSize;
                bpRectF = new RectF(0,0,mBitmapRectSize,mBitmapRectSize);

//                mMediaPlayer = MediaPlayer.create(mContext.get(), R.raw.a2_ding);
            }

            public void setDrawHint(DrawLineRecHint hint){
                myDrawHint = hint;
            }


            public void relayout(int rotate) {
                this.rotate = rotate;
            }

//            public void watermarkOnOff(boolean isWatermaker) {
////                Log.e(TAG, "watermarkOnOff: isWatermaker" + isWatermaker);
//                this.isWatermaker = isWatermaker;
//            }

            public void tempshowOnOff(boolean isTempShow) {
                this.isTempShow = isTempShow;
            }

            public float[] GetTemperatureData() {
                return temperature1;
            }

            /**
             * 打开 最高温0  最低温1  中心点温度1 特诊点绘制
             * @param pointType 最高温0  最低温1  中心点温度1
             */
            public void openFeaturePoints(int pointType){
                switch (pointType){
                    case 0:
                        featurePointsControl =  featurePointsControl | 0x0f00 ;
                        break;
                    case 1:
                        featurePointsControl =  featurePointsControl | 0x00f0 ;
                        break;
                    case 2:
                        featurePointsControl =  featurePointsControl | 0x000f ;
                        break;
                    default:
                        break;
                }
            }

            /**
             * 关闭 最高温  最低温  中心点温度 特诊点绘制
             * @param pointType 最高温0  最低温1  中心点温度1
             */
            public void closeFeaturePoints(int pointType){
                switch (pointType){
                    case 0:
                        featurePointsControl =  featurePointsControl & 0x00ff;
                        break;
                    case 1:
                        featurePointsControl =  featurePointsControl & 0x0f0f;
                        break;
                    case 2:
                        featurePointsControl =  featurePointsControl & 0x0ff0;
                        break;
                    default:
                        break;
                }
            }

            public int getFeaturePointsControl () {
                return featurePointsControl;
            }

            public void openSysCamera() {
                if (mCamera2Helper == null) {
                    mCamera2Helper = mCamera2Helper.getInstance();
                }

                mCamera2Helper.openCamera(640, 480);
                isCamera2ing = true;
            }

            public void closeSysCamera() {
                if (mCamera2Helper != null) {
                    mCamera2Helper.closeCamera();
                }
                isCamera2ing = false;
            }
            //支持的宽高
            public int mSupportWidth;
            public int mSupportHeight;

//            public void setTouchPoint(CopyOnWriteArrayList<TouchPoint> touchPoint) {
//                this.mTouchPointLists = touchPoint;
//            }
//
//            public void setTouchLine(CopyOnWriteArrayList<TouchLine> touchLine) {
//                this.mTouchLineLists = touchLine;
//            }
//
//            public void setTouchArea(CopyOnWriteArrayList<TouchArea> touchArea) {
//                this.mTouchAreaLists = touchArea;
//            }

            public void setTemperatureAnalysisMode(int mode) {
                this.temperatureAnalysisMode = mode;
            }

//            public void setUnitTemperature(int mode) {
//                this.UnitTemperature = mode;
//            }

            //获取全幅温度函数
            public final ITemperatureCallback ahITemperatureCallback = new ITemperatureCallback() {
                //added by wupei
                @Override
                public void onReceiveTemperature(float[] temperature) {
                        // 回调的温度的长度为：256x192  + 10
//                    Log.e(TAG, "ITemperatureCallback.onReceiveTemperature.temperature[].length ==========="+temperature.length);
//                    Log.e(TAG, " 取整 temperature[].length ===========");
//                    Log.e(TAG, " 取余 temperature[].length ==========="+temperature.length%256);
                    System.arraycopy(temperature, 0, temperature1, 0, (mSupportHeight) * mSupportWidth + 10);
//                    if (UnitTemperature == 0) {     //摄氏度
//                        //Log.e(TAG, "000000000");
////                        temperature1 = new float[mSuportHeight*mSuportWidth*4]; 初始化太迟 之前访问会报错
//                    Log.e(TAG, "onReceiveTemperature:  vid === " + mVid + "  ===== mPid === " + mPid + "====== .tinCorrection " + tinyCorrection );
                    if (mPid == 22592 && mVid == 3034){
                        temperature1[3] = temperature[3] + tinyCorrection;
                        temperature1[6] = temperature[6] + tinyCorrection;
                        for (int i = 0; i < mSupportHeight*mSupportWidth;i++){
                            temperature1[10+i] = temperature1[10+i] + tinyCorrection;
                        }
                    }else if(mPid == 1 && mVid == 5396){

                    }
                    maxtemperature = temperature[3];
                    mintemperature = temperature[6];


//                    } else {        //华氏度
//                        temperature1[0] = temperature[0] * 1.8f + 32;//中心温度
//                        temperature1[1] = temperature[1];//MAXX1
//                        temperature1[2] = temperature[2];//MAXY1
//                        temperature1[3] = temperature[3] * 1.8f + 32;//最高温
//                        temperature1[4] = temperature[4];//MINX1
//                        temperature1[5] = temperature[5];//MIXY1
//
//                        for (int i = 6; i < ((mSuportHeight - 4) * mSuportWidth + 10); i++) {
//                            temperature1[i] = temperature[i] * 1.8f + 32;
//                        }
//                        maxtemperature = temperature1[3];
//                        mintemperature = temperature1[6];
//                    }
                }
            };
//            public final TcpITemperatureCallback tcpAhITemperatureCallback = new TcpITemperatureCallback() {
//                @Override
//                public void onReceiveTemperature(float[] temperature) {
//                    Log.e(TAG, "获取Tcp全幅温度函数====w="+mSuportWidth+"==h="+mSuportHeight);
//                    //Log.e(TAG, "ITemperatureCallback center");
//                    //Log.e(TAG, "ITemperatureCallback center"+temperature[0]);
//                    if (UnitTemperature == 0) {
//                        System.arraycopy(temperature, 0, temperature1, 0, (mSuportHeight - 4) * mSuportWidth + 10);
//                        maxtemperature = temperature[3];
//                        mintemperature = temperature[6];
//                    } else {
//                        for (int i = 6; i < ((mSuportHeight - 4) * mSuportWidth + 10); i++) {
//                            temperature1[i] = temperature[i] * 1.8f + 32;
//                        }
//                    }
//                }
//            };

            /**
             * JNI获取的温度回调到Java层
             * @return
             */
            public ITemperatureCallback getTemperatureCallback() {
                //by wp
                //Log.e(TAG, "获取UVC温度回调==== ");
                return ahITemperatureCallback;
            }
//            public TcpITemperatureCallback getTcpTemperatureCallback(){
//                //by wp
//                //Log.e(TAG, "获取Tcp温度回调==== ");
//                return tcpAhITemperatureCallback;
//            }

            public void setTemperatureCbing(boolean isTempCbing) {
                isCbTemping = isTempCbing;
            }

            public void setSuportWH(int w, int h) {
                mSupportHeight = h;
                mSupportWidth = w;
                widthRatio=mSupportWidth*1.0f/icon.getWidth();
                heightRatio=(mSupportHeight)*1.0f/icon.getHeight();//数据写死了
                Log.e(TAG, "widthRatio=" + widthRatio);
                Log.e(TAG, "heightRatio=" + heightRatio);
            }

            public void setVertices(float scale) {
                mDrawer.setVertices(scale);
            }


            private int isFirstCome = 0;

            /**
             * 选择变换
             *
             * @param origin 原图
             * @param alpha  旋转角度，可正可负
             * @return 旋转后的图片
             */
            private Bitmap rotateBitmap(Bitmap origin, float alpha) {
                if (origin == null) {
                    return null;
                }
                int width = origin.getWidth();
                int height = origin.getHeight();
                Matrix matrix = new Matrix();
                matrix.setRotate(alpha);
                // 围绕原地进行旋转
                Bitmap newBM = null;
//                if(isT3){
//////                  newBM = Bitmap.createScaledBitmap(origin,  icon.getWidth() / 2, icon.getHeight() / 2,false);
//                      newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
////                    newBM = skewBitmap(newBM);
////                    newBM = Bitmap.createBitmap(origin,0,0,  icon.getWidth() / 2, icon.getHeight() / 2, matrix, false);
////                    newBM =horverImage(origin,true,true);
//                }else{
                newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
//                }
                if (newBM.equals(origin)) {
                    return newBM;
                }
                origin.recycle();
                return newBM;
            }

            private Rect textRect = new Rect();
            private void xy2DrawText(float x , float y , String tempText , TextPaint tpBg,TextPaint tp,int textDirection){
                if (textDirection != 0){

                }else { //textDirection == 0
                    tp.getTextBounds(tempText,0,tempText.length(),textRect);
                    if (x + tp.measureText(tempText) > icon.getWidth()){
                        if (textRect.height() + y > icon.getHeight()){
                            bitcanvas.drawText(tempText,x - mBitmapRectSize/2.0f - tp.measureText(tempText), y - tpBg.descent(),tpBg);
                            bitcanvas.drawText(tempText,x - mBitmapRectSize/2.0f - tp.measureText(tempText), y - tp.descent(),tp);
                        }else {
                            bitcanvas.drawText(tempText,x - mBitmapRectSize/2.0f - tp.measureText(tempText), y + mBitmapRectSize/2.0f,tpBg);
                            bitcanvas.drawText(tempText,x - mBitmapRectSize/2.0f - tp.measureText(tempText), y + mBitmapRectSize/2.0f,tp);
                        }
                    }else {
                        if (textRect.height() + y > icon.getHeight()){//文字高度 出界。
                            bitcanvas.drawText(tempText,x + mBitmapRectSize/2.0f, y - tpBg.descent(),tpBg);
                            bitcanvas.drawText(tempText,x + mBitmapRectSize/2.0f, y - tp.descent(),tp);
                        }else {
                            bitcanvas.drawText(tempText,x + mBitmapRectSize/2.0f, y + mBitmapRectSize/2.0f,tpBg);
                            bitcanvas.drawText(tempText,x + mBitmapRectSize/2.0f, y + mBitmapRectSize/2.0f,tp);
                        }

                    }
                }
            }

            private Boolean isT3 = BaseApplication.deviceName.contentEquals("T3") || BaseApplication.deviceName.contentEquals("DL13") || BaseApplication.deviceName.contentEquals("DV");

            public void DrawControl()
            {
//                Log.e(TAG,"DrawControl id =============="+calendar.get(Calendar.SECOND));
                //是否绘制边框    = 是够开启高温警告  &&    标记温度小于最高温   &&    时间是否是2的倍数  》》》绘制的是一秒为间隔的闪屏
//                if (isAlarm = isHighTempAlarm && (mMarkAlarmTemp < maxtemperature)){
//                    alarmCounter++;
//                }

                isAlarm = isOverTempAlarm && (mAlarmTemp < maxtemperature);
//                isAlarmRing = isHighTempAlarm && (mMarkAlarmTemp < maxtemperature);

//                mBindSeekBar.Update(maxtemperature,mintemperature);//更新seekbar
                if (mDragTempContainer!= null && temperature1 !=null){
                    mDragTempContainer.upDateTemp(temperature1);
                }
            }

            /**
             * draw a frame (and request to draw for video capturing if it is necessary)
             */
            public final void onDrawFrame() {
//                Log.e(TAG,"thread id ==============",time);
//                String extern;
//                float x, y;
//                if (isT3 && isFirstCome == 0) {
//                    rotate = 180;
//                }

//                if (UnitTemperature == 0) {
//                    extern = "°C";
//                } else {
//                    extern = "°F";
//                }
                bitcanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                if (!isCbTemping) {
                    //                    bitcanvas.save(Canvas.ALL_SAVE_FLAG);//吴长城修改到下面函数
                    bitcanvas.save();
                } else {
//                    bitcanvas.drawText("123456", 200, 200,tempTextPaint );

                    //绘制超温警告的  四边形
                    if (isAlarm && (alarmCount > 15 && alarmCount < 30)){
                        linePaint.setColor(Color.RED);
                        bitcanvas.drawLine(5,5,5 ,mViewHeight -5,linePaint);
                        bitcanvas.drawLine(5,5,mViewWidth - 5 ,5,linePaint);
                        bitcanvas.drawLine(mViewWidth - 5,mViewHeight -5,mViewWidth - 5 ,5,linePaint);
                        bitcanvas.drawLine(mViewWidth - 5,mViewHeight -5,5,mViewHeight -5,linePaint);
                    }
                    //重置警告弹窗 的 帧数计数器
                    if (isAlarm){
                        if (alarmCount >= 30){
                            alarmCount = 0;
                        }else {
                            alarmCount++;
                        }
                    }
                    //是否绘制提示线
                    if (myDrawHint !=null && myDrawHint.isNeedDraw()){
                        if (myDrawHint.getDrawTempMode() ==2){
                            bitcanvas.drawLine(myDrawHint.getStartXCoordinate(),myDrawHint.getStartYCoordinate(),
                                    myDrawHint.getEndXCoordinate(),myDrawHint.getEndYCoordinate(),linePaint);
                            bitcanvas.drawLine(myDrawHint.getStartXCoordinate(),myDrawHint.getStartYCoordinate(),
                                    myDrawHint.getEndXCoordinate(),myDrawHint.getEndYCoordinate(),dottedLinePaint);
                        }else if (myDrawHint.getDrawTempMode() ==3){
                            bitcanvas.drawLine(myDrawHint.getStartXCoordinate(),myDrawHint.getStartYCoordinate(),
                                    myDrawHint.getStartXCoordinate(),myDrawHint.getEndYCoordinate(),linePaint);
                            bitcanvas.drawLine(myDrawHint.getStartXCoordinate(),myDrawHint.getStartYCoordinate(),
                                    myDrawHint.getEndXCoordinate(),myDrawHint.getStartYCoordinate(),linePaint);
                            bitcanvas.drawLine(myDrawHint.getEndXCoordinate(),myDrawHint.getStartYCoordinate(),
                                    myDrawHint.getEndXCoordinate(),myDrawHint.getEndYCoordinate(),linePaint);
                            bitcanvas.drawLine(myDrawHint.getStartXCoordinate(),myDrawHint.getEndYCoordinate(),
                                    myDrawHint.getEndXCoordinate(),myDrawHint.getEndYCoordinate(),linePaint);

                            bitcanvas.drawLine(myDrawHint.getStartXCoordinate(),myDrawHint.getStartYCoordinate(),
                                    myDrawHint.getStartXCoordinate(),myDrawHint.getEndYCoordinate(),dottedLinePaint);
                            bitcanvas.drawLine(myDrawHint.getStartXCoordinate(),myDrawHint.getStartYCoordinate(),
                                    myDrawHint.getEndXCoordinate(),myDrawHint.getStartYCoordinate(),dottedLinePaint);
                            bitcanvas.drawLine(myDrawHint.getEndXCoordinate(),myDrawHint.getStartYCoordinate(),
                                    myDrawHint.getEndXCoordinate(),myDrawHint.getEndYCoordinate(),dottedLinePaint);
                            bitcanvas.drawLine(myDrawHint.getStartXCoordinate(),myDrawHint.getEndYCoordinate(),
                                    myDrawHint.getEndXCoordinate(),myDrawHint.getEndYCoordinate(),dottedLinePaint);
                        }
                    }
                    //绘制用户添加的测温模式
                    if (mDragTempContainer!=null){
                       for (TempWidgetObj tempWidgetObj : mDragTempContainer.getUserAddData()){
                           if (!tempWidgetObj.isSelect()){//未选中
                               //点温度 绘制：全局高温。全局低温、全局中心温、点测试模式（正常温度）
                               if (tempWidgetObj.getType() == 1){
                                   bpRectF.left = tempWidgetObj.getPointTemp().getStartPointX() - mBitmapRectSize / 2.0f;
                                   bpRectF.right = tempWidgetObj.getPointTemp().getStartPointX() + mBitmapRectSize / 2.0f;
                                   bpRectF.top = tempWidgetObj.getPointTemp().getStartPointY() - mBitmapRectSize / 2.0f;
                                   bpRectF.bottom = tempWidgetObj.getPointTemp().getStartPointY() + mBitmapRectSize / 2.0f;
                                   // 点测温
                                   tempTextPaint.setColor(Color.BLACK);
                                   bitcanvas.drawBitmap(normalPointBt,null,bpRectF,photoPaint);
                                   xy2DrawText(tempWidgetObj.getPointTemp().getStartPointX(), tempWidgetObj.getPointTemp().getStartPointY(),
                                           tempWidgetObj.getPointTemp().getTemp(),tempTextBgTextPaint,tempTextPaint,0);
                               }
                               //线测温模式绘制
                               if (tempWidgetObj.getType() ==2 ){
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),
                                           tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),linePaint);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),
                                           tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),dottedLinePaint);

                                   tempTextPaint.setColor(Color.RED);
                                   xy2DrawText(tempWidgetObj.getOtherTemp().getMaxTempX(),tempWidgetObj.getOtherTemp().getMaxTempY(),
                                           tempWidgetObj.getOtherTemp().getMaxTemp(),tempTextBgTextPaint,tempTextPaint,0);

                                   tempTextPaint.setColor(Color.BLUE);
                                   xy2DrawText(tempWidgetObj.getOtherTemp().getMinTempX(),tempWidgetObj.getOtherTemp().getMinTempY(),
                                           tempWidgetObj.getOtherTemp().getMinTemp(),tempTextBgTextPaint,tempTextPaint,0);

                                   bpRectF.left = tempWidgetObj.getOtherTemp().getMaxTempX() - mBitmapRectSize / 2.0f;
                                   bpRectF.right = tempWidgetObj.getOtherTemp().getMaxTempX() + mBitmapRectSize / 2.0f;
                                   bpRectF.top = tempWidgetObj.getOtherTemp().getMaxTempY() - mBitmapRectSize / 2.0f;
                                   bpRectF.bottom = tempWidgetObj.getOtherTemp().getMaxTempY() + mBitmapRectSize / 2.0f;
                                   bitcanvas.drawBitmap(highTempBt,null,bpRectF,photoPaint);
                                   bpRectF.left = tempWidgetObj.getOtherTemp().getMinTempX() - mBitmapRectSize / 2.0f;
                                   bpRectF.right = tempWidgetObj.getOtherTemp().getMinTempX() + mBitmapRectSize / 2.0f;
                                   bpRectF.top = tempWidgetObj.getOtherTemp().getMinTempY() - mBitmapRectSize / 2.0f;
                                   bpRectF.bottom = tempWidgetObj.getOtherTemp().getMinTempY() + mBitmapRectSize / 2.0f;
                                   bitcanvas.drawBitmap(lowTempBt,null,bpRectF,photoPaint);
                               }
                               //矩形测温模式绘制
                               if (tempWidgetObj.getType() == 3 ){
                                   linePaint.setColor(Color.WHITE);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),
                                           tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),linePaint);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),
                                           tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),linePaint);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),
                                           tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),linePaint);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),
                                           tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),linePaint);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),
                                           tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),dottedLinePaint);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),
                                           tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),dottedLinePaint);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),
                                           tempWidgetObj.getOtherTemp().getStartPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),dottedLinePaint);
                                   bitcanvas.drawLine(tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getEndPointY(),
                                           tempWidgetObj.getOtherTemp().getEndPointX(),tempWidgetObj.getOtherTemp().getStartPointY(),dottedLinePaint);

                                   tempTextPaint.setColor(Color.RED);
                                   xy2DrawText(tempWidgetObj.getOtherTemp().getMaxTempX(),tempWidgetObj.getOtherTemp().getMaxTempY(),
                                           tempWidgetObj.getOtherTemp().getMaxTemp(),tempTextBgTextPaint,tempTextPaint,0);
                                   tempTextPaint.setColor(Color.BLUE);
                                   xy2DrawText(tempWidgetObj.getOtherTemp().getMinTempX(),tempWidgetObj.getOtherTemp().getMinTempY(),
                                           tempWidgetObj.getOtherTemp().getMinTemp(),tempTextBgTextPaint,tempTextPaint,0);

                                   //                               bitcanvas.drawBitmap(highTempBt,tempWidgetObj.getOtherTemp().getMaxTempX()-19,tempWidgetObj.getOtherTemp().getMaxTempY()-19,photoPaint);
                                   //                               bitcanvas.drawBitmap(lowTempBt,tempWidgetObj.getOtherTemp().getMinTempX()-19,tempWidgetObj.getOtherTemp().getMinTempY()-19,photoPaint);
                                   bpRectF.left = tempWidgetObj.getOtherTemp().getMaxTempX() - mBitmapRectSize / 2.0f;
                                   bpRectF.right = tempWidgetObj.getOtherTemp().getMaxTempX() + mBitmapRectSize / 2.0f;
                                   bpRectF.top = tempWidgetObj.getOtherTemp().getMaxTempY() - mBitmapRectSize / 2.0f;
                                   bpRectF.bottom = tempWidgetObj.getOtherTemp().getMaxTempY() + mBitmapRectSize / 2.0f;
                                   bitcanvas.drawBitmap(highTempBt,null,bpRectF,photoPaint);
                                   bpRectF.left = tempWidgetObj.getOtherTemp().getMinTempX() - mBitmapRectSize / 2.0f;
                                   bpRectF.right = tempWidgetObj.getOtherTemp().getMinTempX() + mBitmapRectSize / 2.0f;
                                   bpRectF.top = tempWidgetObj.getOtherTemp().getMinTempY() - mBitmapRectSize / 2.0f;
                                   bpRectF.bottom = tempWidgetObj.getOtherTemp().getMinTempY() + mBitmapRectSize / 2.0f;
                                   bitcanvas.drawBitmap(lowTempBt,null,bpRectF,photoPaint);
                               }
                           }
                       }
                    }
                    /**
                     * temperatureData[0]=centerTmp;
                     * 	temperatureData[1]=(float)maxx1;
                     * 	temperatureData[2]=(float)maxy1;
                     * 	temperatureData[3]=maxTmp;
                     * 	temperatureData[4]=(float)minx1;
                     * 	temperatureData[5]=(float)miny1;
                     * 	temperatureData[6]=minTmp;
                     * 	temperatureData[7]=point1Tmp;
                     * 	temperatureData[8]=point2Tmp;
                     * 	temperatureData[9]=point3Tmp;
                     */
                    //绘制 全幅 最高温 最低温  中心点温度 ,并且查找点是否 不在线 和矩形之中
                    if ((featurePointsControl & 0x0f00) > 0 && (true)){//最高点温度
                        bpRectF.left = temperature1[1] * (icon.getWidth() / (float) mSupportWidth) - mBitmapRectSize / 2.0f;
                        bpRectF.right = temperature1[1] * (icon.getWidth() / (float) mSupportWidth) + mBitmapRectSize / 2.0f;
                        bpRectF.top = temperature1[2] * (icon.getHeight() / (float) (mSupportHeight)) - mBitmapRectSize / 2.0f;
                        bpRectF.bottom = temperature1[2] * (icon.getHeight() / (float) (mSupportHeight)) + mBitmapRectSize / 2.0f;
                        bitcanvas.drawBitmap(highTempBt,null,bpRectF,photoPaint);
                        tempTextPaint.setColor(Color.RED);
	                    xy2DrawText(temperature1[1] * (icon.getWidth() / (float) mSupportWidth),
			                    temperature1[2] * (icon.getHeight() / (float) (mSupportHeight)),
                                decimalFormat.format(TempConvertUtils.centigrade2Temp(temperature1[3],mDragTempContainer.getTempSuffixMode()))
                                        + DragTempContainer.tempSuffixList[mDragTempContainer.getTempSuffixMode()],tempTextBgTextPaint,tempTextPaint,0);
                    }
                    if ((featurePointsControl & 0x00f0) > 0 && (true)){//最低点温度
                        bpRectF.left = temperature1[4] * (icon.getWidth() / (float) mSupportWidth) - mBitmapRectSize / 2.0f;
                        bpRectF.right = temperature1[4] * (icon.getWidth() / (float) mSupportWidth) + mBitmapRectSize / 2.0f;
                        bpRectF.top = temperature1[5] * (icon.getHeight() / (float) (mSupportHeight )) - mBitmapRectSize / 2.0f;
                        bpRectF.bottom = temperature1[5] * (icon.getHeight() / (float) (mSupportHeight )) + mBitmapRectSize / 2.0f;
                        bitcanvas.drawBitmap(lowTempBt,null,bpRectF,photoPaint);
                        tempTextPaint.setColor(Color.BLUE);

	                    xy2DrawText(temperature1[4] * (icon.getWidth() / (float) mSupportWidth),
			                    temperature1[5] * (icon.getHeight() / (float) (mSupportHeight)),
			                      decimalFormat.format(TempConvertUtils.centigrade2Temp(temperature1[6],mDragTempContainer.getTempSuffixMode()))
                                          + DragTempContainer.tempSuffixList[mDragTempContainer.getTempSuffixMode()],tempTextBgTextPaint,tempTextPaint,0);
                    }
                    if ((featurePointsControl & 0x000f) > 0){//中心点温度
                        bpRectF.left =  (icon.getWidth() / 2.0f) - mBitmapRectSize / 2.0f;
                        bpRectF.right =  (icon.getWidth() / 2.0f) + mBitmapRectSize / 2.0f;
                        bpRectF.top = (icon.getHeight() / 2.0f) - mBitmapRectSize / 2.0f;
                        bpRectF.bottom = (icon.getHeight() / 2.0f) + mBitmapRectSize / 2.0f;
                        bitcanvas.drawBitmap(centerTempBt,null,bpRectF,photoPaint);
                        tempTextPaint.setColor(Color.YELLOW);
	                    xy2DrawText((icon.getWidth() / 2.0f),(icon.getHeight() / 2.0f),
                                decimalFormat.format(TempConvertUtils.centigrade2Temp(temperature1[0],mDragTempContainer.getTempSuffixMode()))
                                        + DragTempContainer.tempSuffixList[mDragTempContainer.getTempSuffixMode()],tempTextBgTextPaint,tempTextPaint,0);
                    }

//                    String centerTemp = decimalFormat.format(temperature1[0]) + extern;//format 返回的是字符串
//                    String maxTemp = decimalFormat.format(temperature1[3]) + extern;
//                    String minTemp = decimalFormat.format(temperature1[6]) + extern;
//                    float maxx1 = temperature1[1];
//                    float maxy1 = temperature1[2];
//                    float minx1 = temperature1[4];
//                    float miny1 = temperature1[5];

//                    photoPaint.setStrokeWidth(3);


//                    if (isAlarm){//显示高温报警
////                        CountDownTimer timer = new
//
//                        alarmPaint.setStrokeWidth(4.0f);
//                        alarmPaint.setColor(Color.RED);
//                        bitcanvas.drawLine(5,5,icon.getWidth()-5,5,alarmPaint);
//                        bitcanvas.drawLine(5,5,5,icon.getHeight()-5,alarmPaint);
//
//                        bitcanvas.drawLine(icon.getWidth()-5,icon.getHeight()-5,5,icon.getHeight()-5,alarmPaint);
//                        bitcanvas.drawLine(icon.getWidth()-5,icon.getHeight()-5,icon.getWidth()-5,5,alarmPaint);
//                    }

//                    if(isTempShow){
//                        //绘制温度从这里开始
//                        //设置画笔粗细
//                        //photoPaint.setTextAlign(Paint.Align.LEFT);
//
//                        //bitcanvas.drawText(centerTemp, icon.getWidth() / 2.0f + mCursorYellow.getWidth() / 2, icon.getHeight() / 2.0f + mCursorYellow.getHeight() / 2, photoPaint);
//                        photoPaint.getTextBounds(centerTemp, 0, centerTemp.length(), bounds);//绘制中心点温度
//                        photoPaint.setColor(Color.YELLOW);
//                        x = (float) icon.getWidth() / 2.0f;
//                        y = icon.getHeight() / 2.0f;
//
//
//
//
//                        photoPaint.getTextBounds(maxTemp, 0, maxTemp.length(), dstHighTemp);//绘制最高温
//                        photoPaint.setColor(Color.RED);
//                        x = (float) icon.getWidth() / (float) mSuportWidth * maxx1;
//                        y = (float) icon.getHeight() / (float) (mSuportHeight - 4) * maxy1;
//                        if (x<(icon.getWidth()-mCursorBlue.getWidth())){
//                            bitcanvas.drawText(maxTemp, x + (float) mCursorRed.getWidth() / 2.0f, y + (float) mCursorRed.getHeight() / 2.0f, photoPaint);
//                        }else {
//                            bitcanvas.drawText(maxTemp, x - (float) mCursorRed.getWidth()*2, y + (float) mCursorRed.getHeight() , photoPaint);
//                        }
//
//                        photoPaint.getTextBounds(minTemp, 0, minTemp.length(), dstLowTemp);//绘制最低温
//                        photoPaint.setColor(Color.BLUE);
//                        x = (float) icon.getWidth() / (float) mSuportWidth * minx1;
//                        y = (float) icon.getHeight() / (float) (mSuportHeight - 4) * miny1;
//                        if (x<(icon.getWidth()-mCursorBlue.getWidth())){
//                            bitcanvas.drawText(minTemp, x + (float) mCursorBlue.getWidth() / 2.0f, y + (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                        }else {
//                            bitcanvas.drawText(minTemp, x - (float) mCursorBlue.getWidth() *2, y + (float) mCursorBlue.getHeight() , photoPaint);
//                        }
//
//                        bitcanvas.drawBitmap(mCursorRed, (float) icon.getWidth() / (float) mSuportWidth * maxx1 - (float) mCursorRed.getWidth() / 2.0f, (float) icon.getHeight() / (float) (mSuportHeight - 4) * maxy1 - (float) mCursorRed.getHeight() / 2.0f, photoPaint);
//                        bitcanvas.drawBitmap(mCursorBlue, (float) icon.getWidth() / (float) mSuportWidth * minx1 - (float) mCursorBlue.getWidth() / 2.0f, (float) icon.getHeight() / (float) (mSuportHeight - 4) * miny1 - (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                        bitcanvas.drawBitmap(mCursorYellow, (float) icon.getWidth() / 2.0f - (float) mCursorYellow.getWidth() / 2.0f, (float) icon.getHeight() / 2.0f - (float) mCursorYellow.getHeight() / 2.0f, photoPaint);
//
//                        //绘制标志性温度在这里结束
//                    }

                    /**
                     * 点测温绘制
                     */
//                    if(mTouchPointLists.size()>0){
//                        String pointTemp;//= decimalFormat.format(temperature[7]);
//
//                        //Log.e(TAG, "icon.getWidth()" + icon.getWidth());
//                        //Log.e(TAG, "icon.getHeight()" + icon.getHeight());
//                        photoPaint.setColor(Color.GREEN);
//                        try {
//                            for (TouchPoint j : mTouchPointLists) {
//                                float pointx = j.x - mCursorGreen.getWidth() / 2.0f;
//                                float pointy = j.y - mCursorGreen.getHeight() / 2.0f;
//                                if (isT3) {
//                                    bitcanvas.drawBitmap(mCursorGreen, icon.getWidth() - pointx - mCursorGreen.getWidth(), icon.getHeight() - pointy - mCursorGreen.getWidth(), photoPaint);
//                                } else {
//                                    bitcanvas.drawBitmap(mCursorGreen, pointx, pointy, photoPaint);
//                                }
//                                int index = 0;
//                                if (isT3) {
//                                    //index = (((int) ((1 - j.y) * (mSuportHeight - 4))) * mSuportWidth + (int) ((1 - j.x) * mSuportWidth) + 10);
//                                    index = ((int) (mSuportHeight - 4 - j.y*heightRatio) * mSuportWidth + (int) (mSuportWidth - j.x*widthRatio) + 10);
//                                } else {
//                                    //index = (((int) (j.y * (mSuportHeight - 4))) * mSuportWidth + (int) (j.x * mSuportWidth) + 10);
//                                    index = (((int) (j.y * heightRatio)) * mSuportWidth + (int) (j.x * widthRatio) + 10);
//                                }
//                                x = j.x;
//                                y = j.y;
//                                pointTemp = decimalFormat.format(temperature1[index]) + extern;
//                                photoPaint.getTextBounds(pointTemp, 0, pointTemp.length(), bounds);
//                                if (isT3) {
//                                    bitcanvas.drawText(pointTemp, icon.getWidth() - x + (float) mCursorGreen.getWidth() / 2.0f, icon.getHeight() - y + (float) mCursorGreen.getHeight() / 2.0f, photoPaint);
//                                } else {
//                                    if (icon.getWidth() - x >(mCursorGreen.getWidth()/2.0f + bounds.width())){//边界绘制温度 在左边
//                                        bitcanvas.drawText(pointTemp, x + (float) mCursorGreen.getWidth() / 2.0f, y + (float) mCursorGreen.getHeight() / 2.0f, photoPaint);
//                                    }else {
//                                        bitcanvas.drawText(pointTemp, x - (float) mCursorGreen.getWidth() / 2.0f - bounds.width(), y + (float) mCursorGreen.getHeight() / 2.0f, photoPaint);
//                                    }
//
//                                }
//                            }
//                        } catch (Exception e) {
//
//                        }
//
//                    }


                    /**
                     * 1:线测温绘制
                     */
//                    if(mTouchLineLists.size()>0){
//                        for(int m=0; m<mTouchLineLists.size(); m++){
//                            TouchPoint TouchPoint1, TouchPoint2;
//                            int point1x, point2x, point1y, point2y, leftPointx, rightPointx, leftPointy, rightPointy;
//                            int maxPointX, maxPointY, minPointX, minPointY;
//                            float maxTempL, minTempL;
//                            //bitcanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//
//                            TouchPoint1 = mTouchLineLists.get(m).touchPoint1;
//                            TouchPoint2 = mTouchLineLists.get(m).touchPoint2;
//                            if (isT3) {
//                                /*
//                                point1x = (int) ((1 - TouchPoint1.x) * mSuportWidth);
//                                point2x = (int) ((1 - TouchPoint2.x) * mSuportWidth);
//                                point1y = (int) ((1 - TouchPoint1.y) * (mSuportHeight - 4));
//                                point2y = (int) ((1 - TouchPoint2.y) * (mSuportHeight - 4));
//                                 */
//                                point1x = (int) (mSuportWidth - TouchPoint1.x * widthRatio);
//                                point2x = (int) (mSuportWidth - TouchPoint2.x * widthRatio);
//                                point1y = (int) (mSuportHeight - 4 - TouchPoint1.y * heightRatio);
//                                point2y = (int) (mSuportHeight - 4 - TouchPoint2.y * heightRatio);
//                            } else {
//                                /*
//                                point1x = (int) (TouchPoint1.x * mSuportWidth);
//                                point2x = (int) (TouchPoint2.x * mSuportWidth);
//                                point1y = (int) (TouchPoint1.y * (mSuportHeight - 4));
//                                point2y = (int) (TouchPoint2.y * (mSuportHeight - 4));
//                                 */
//                                point1x = (int) (TouchPoint1.x * widthRatio);
//                                point2x = (int) (TouchPoint2.x * widthRatio);
//                                point1y = (int) (TouchPoint1.y * heightRatio);
//                                point2y = (int) (TouchPoint2.y * heightRatio);
//                            }
//
//                            if (point1x <= point2x) {
//                                leftPointx = point1x;
//                                leftPointy = point1y;
//                                rightPointx = point2x;
//                                rightPointy = point1y;
//                            } else {
//                                leftPointx = point2x;
//                                leftPointy = point1y;
//                                rightPointx = point1x;
//                                rightPointy = point1y;
//                            }
//                            int leftIndex = (leftPointy * mSuportWidth + leftPointx + 10);
//                            int rightIndex = (rightPointy * mSuportWidth + rightPointx + 10);
//                            maxTempL = temperature1[leftIndex];
//                            minTempL = temperature1[rightIndex];
//                            maxPointX = leftPointx;//对应数据源  画布 上的坐标。
//                            maxPointY = leftPointy;
//                            minPointX = rightPointx;
//                            minPointY = rightPointy;
//                            for (int k = leftPointx; k < rightPointx; k++) {
//                                float te = temperature1[leftPointy * mSuportWidth + k + 10];
//                                if (maxTempL <= te) {
//                                    maxTempL = te;
//                                    maxPointX = k;
//                                    maxPointY = leftPointy;
//                                }
//                                if (minTempL >= te) {
//                                    minTempL = te;
//                                    minPointX = k;
//                                    minPointY = leftPointy;
//                                }
//                            }
//                            photoPaint.setColor(Color.BLUE);
//                            if (isT3) {
//                                //bitcanvas.drawLine(icon.getWidth() - TouchPoint1.x * icon.getWidth(), icon.getHeight() - TouchPoint1.y * icon.getHeight(), icon.getWidth() - TouchPoint2.x * icon.getWidth(), icon.getHeight() - TouchPoint1.y * icon.getHeight(), photoPaint);
//                                bitcanvas.drawLine(icon.getWidth() - TouchPoint1.x, icon.getHeight() - TouchPoint1.y, icon.getWidth() - TouchPoint2.x, icon.getHeight() - TouchPoint1.y, photoPaint);
//                            } else {
//                                bitcanvas.drawLine(TouchPoint1.x, TouchPoint1.y, TouchPoint2.x, TouchPoint1.y, photoPaint);
//                            }
//                            String maxTempString = decimalFormat.format(maxTempL) + extern;
//                            String minTempString = decimalFormat.format(minTempL) + extern;
//
//
//                            photoPaint.getTextBounds(maxTempString, 0, maxTempString.length(), dstHighTemp);
//                            photoPaint.setColor(Color.RED);
//                            x = (float) icon.getWidth() / (float) mSuportWidth * maxPointX;//把数据源上画布的坐标  转化成 屏幕上画布的坐标
//                            y = (float) icon.getHeight() / (float) (mSuportHeight - 4) * maxPointY;
//
//                            if (icon.getWidth() - x > (mCursorRed.getWidth()+dstHighTemp.width())){//边界高温 绘制在 标志的前边
//                                bitcanvas.drawText(maxTempString, x + (float) mCursorRed.getWidth() / 2.0f, y + (float) mCursorRed.getHeight() / 2.0f, photoPaint);
//                            }else {
//                                bitcanvas.drawText(maxTempString, x - (float) mCursorRed.getWidth() - dstHighTemp.width(), y + (float) mCursorRed.getHeight() / 2.0f, photoPaint);
//                            }
//                            photoPaint.getTextBounds(minTempString, 0, minTempString.length(), dstLowTemp);
//                            photoPaint.setColor(Color.BLUE);
//                            x = (float) icon.getWidth() / (float) mSuportWidth * minPointX;//最小温度的x坐标
//                            y = (float) icon.getHeight() / (float) (mSuportHeight - 4) * minPointY;
//
////                                bitcanvas.drawText(minTempString, x + (float) mCursorBlue.getWidth() / 2.0f, y + (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                            if (icon.getWidth() - x > (mCursorBlue.getWidth()+dstLowTemp.width())){//边界高温 绘制在 标志的前边
//                                bitcanvas.drawText(minTempString, x + (float) mCursorBlue.getWidth() / 2.0f, y + (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                            }else {
//                                bitcanvas.drawText(minTempString, x - (float) mCursorBlue.getWidth() / 2.0f - dstLowTemp.width(), y + (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                            }
//
//                            bitcanvas.drawBitmap(mCursorRed, (float) icon.getWidth() / (float) mSuportWidth * maxPointX - (float) mCursorRed.getWidth() / 2.0f,
//                                    (float) icon.getHeight() / (float) (mSuportHeight - 4) * maxPointY - (float) mCursorRed.getHeight() / 2.0f, photoPaint);
//                            bitcanvas.drawBitmap(mCursorBlue, (float) icon.getWidth() / (float) mSuportWidth * minPointX - (float) mCursorBlue.getWidth() / 2.0f,
//                                    (float) icon.getHeight() / (float) (mSuportHeight - 4) * minPointY - (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//
//                        }
//                    }

                    /**
                     * 2:矩形测温绘制
                     */
//                    if(mTouchAreaLists.size()>0){
//                        for(int m=0; m<mTouchAreaLists.size(); m++){
//                            TouchPoint TouchPoint1, TouchPoint2;
//                            int point1x, point2x, point1y, point2y, leftPointx, rightPointx, leftPointy, rightPointy;
//                            int maxPointX, maxPointY, minPointX, minPointY;
//                            float maxTempR, minTempR;
//
////                            Log.e(TAG,"ThreadId ===>"+this.getId());
//
//                            TouchPoint1 = mTouchAreaLists.get(m).touchPoint1;
//                            TouchPoint2 = mTouchAreaLists.get(m).touchPoint2;
//                            if (isT3) {
//                                point1x = (int) ((1 - TouchPoint1.x) * mSuportWidth);
//                                point2x = (int) ((1 - TouchPoint2.x) * mSuportWidth);
//                                point1y = (int) ((1 - TouchPoint1.y) * (mSuportHeight - 4));
//                                point2y = (int) ((1 - TouchPoint2.y) * (mSuportHeight - 4));
//                            } else {
//                                point1x = (int) (TouchPoint1.x * widthRatio);
//                                point2x = (int) (TouchPoint2.x * widthRatio);
//                                point1y = (int) (TouchPoint1.y * heightRatio);
//                                point2y = (int) (TouchPoint2.y * heightRatio);
//                            }
//                            if (point1x <= point2x) {
//                                leftPointx = point1x;
//                                leftPointy = point1y;
//                                rightPointx = point2x;
//                                rightPointy = point2y;
//                            } else {
//                                leftPointx = point2x;
//                                leftPointy = point2y;
//                                rightPointx = point1x;
//                                rightPointy = point1y;
//                            }
//                            int leftIndex = (leftPointy * mSuportWidth + leftPointx + 10);
//                            int rightIndex = (leftPointy * mSuportWidth + rightPointx + 10);
//                            maxTempR = temperature1[leftIndex];
//                            minTempR = temperature1[leftIndex];
//                            maxPointX = leftPointx;
//                            maxPointY = leftPointy;
//                            minPointX = leftPointx;
//                            minPointY = leftPointy;
//
//                            for (int i = leftPointx; i < rightPointx; i++) {
//                                for (int j = (leftPointy <= rightPointy ? leftPointy : rightPointy); j < (leftPointy >= rightPointy ? leftPointy : rightPointy); j++) {
//                                    float te = temperature1[j * mSuportWidth + i + 10];
//                                    if (maxTempR <= te) {
//                                        maxTempR = te;
//                                        maxPointX = i;
//                                        maxPointY = j;
//                                    }
//                                    if (minTempR >= te) {
//                                        minTempR = te;
//                                        minPointX = i;
//                                        minPointY = j;
//                                    }
//                                }
//                            }
//                            photoPaint.setColor(Color.YELLOW);
//                            if (isT3) {
//                                bitcanvas.drawLine(icon.getWidth() - TouchPoint1.x, icon.getHeight() - TouchPoint1.y, icon.getWidth() - TouchPoint2.x, icon.getHeight() - TouchPoint1.y, photoPaint);
//                                bitcanvas.drawLine(icon.getWidth() - TouchPoint1.x, icon.getHeight() - TouchPoint1.y, icon.getWidth() - TouchPoint1.x, icon.getHeight() - TouchPoint2.y, photoPaint);
//                                bitcanvas.drawLine(icon.getWidth() - TouchPoint1.x, icon.getHeight() - TouchPoint2.y, icon.getWidth() - TouchPoint2.x, icon.getHeight() - TouchPoint2.y, photoPaint);
//                                bitcanvas.drawLine(icon.getWidth() - TouchPoint2.x, icon.getHeight() - TouchPoint2.y, icon.getWidth() - TouchPoint2.x, icon.getHeight() - TouchPoint1.y, photoPaint);
//                            } else {
//                                bitcanvas.drawLine(TouchPoint1.x, TouchPoint1.y, TouchPoint2.x, TouchPoint1.y, photoPaint);
//                                bitcanvas.drawLine(TouchPoint1.x, TouchPoint1.y, TouchPoint1.x, TouchPoint2.y, photoPaint);
//                                bitcanvas.drawLine(TouchPoint1.x, TouchPoint2.y, TouchPoint2.x, TouchPoint2.y, photoPaint);
//                                bitcanvas.drawLine(TouchPoint2.x, TouchPoint2.y, TouchPoint2.x, TouchPoint1.y, photoPaint);
//                            }
//                            String maxTempString = decimalFormat.format(maxTempR) + extern;
//                            String minTempString = decimalFormat.format(minTempR) + extern;
//
//                            photoPaint.getTextBounds(maxTempString, 0, maxTempString.length(), dstHighTemp);
//                            photoPaint.setColor(Color.RED);
//                            x = (float) icon.getWidth() / (float) mSuportWidth * maxPointX;
//                            y = (float) icon.getHeight() / (float) (mSuportHeight - 4) * maxPointY;
//                            //x = maxPointX;
//                            //y = maxPointY;
//                            if (icon.getWidth() - x > (mCursorRed.getWidth()+dstHighTemp.width())){//边界高温 绘制在 标志的前边
//                                bitcanvas.drawText(maxTempString, x + (float) mCursorRed.getWidth() / 2.0f, y + (float) mCursorRed.getHeight() / 2.0f, photoPaint);
//                            }else {
//                                bitcanvas.drawText(maxTempString, x - (float) mCursorRed.getWidth() /2.0f - dstHighTemp.width(), y + (float) mCursorRed.getHeight() / 2.0f, photoPaint);
//                            }
//
//                            photoPaint.getTextBounds(minTempString, 0, minTempString.length(), dstLowTemp);
//                            photoPaint.setColor(Color.BLUE);
//                            x = (float) icon.getWidth() / (float) mSuportWidth * minPointX;
//                            y = (float) icon.getHeight() / (float) (mSuportHeight - 4) * minPointY;
//
//                            if (icon.getWidth() - x > (mCursorBlue.getWidth()+dstLowTemp.width())){//边界高温 绘制在 标志的前边
//                                bitcanvas.drawText(minTempString, x + (float) mCursorBlue.getWidth() / 2.0f, y + (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                            }else {
//                                bitcanvas.drawText(minTempString, x - (float) mCursorBlue.getWidth() /2.0f - dstLowTemp.width(), y + (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                            }
////                            bitcanvas.drawText(minTempString, x + (float) mCursorBlue.getWidth() / 2.0f, y + (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                            bitcanvas.drawBitmap(mCursorRed, (float) icon.getWidth() / (float) mSuportWidth * maxPointX - (float) mCursorRed.getWidth() / 2.0f, (float) icon.getHeight() / (float) (mSuportHeight - 4) * maxPointY - (float) mCursorRed.getHeight() / 2.0f, photoPaint);
//                            bitcanvas.drawBitmap(mCursorBlue, (float) icon.getWidth() / (float) mSuportWidth * minPointX - (float) mCursorBlue.getWidth() / 2.0f, (float) icon.getHeight() / (float) (mSuportHeight - 4) * minPointY - (float) mCursorBlue.getHeight() / 2.0f, photoPaint);
//                        }
//                        //                        bitcanvas.save(Canvas.ALL_SAVE_FLAG);//吴长城修改到下面函数
//                        bitcanvas.save();
//                        isCbTemping = true;
//                    }

                    //added by wupei
                    //                    if (temperatureAnalysisMode == 3) {//3:多边形测温模式
                    //                        try{
                    //                            for (TouchPoint j : mTouchPoint) {
                    //                                float point1x = j.x * icon.getWidth();
                    //                                float point1y = j.y * icon.getHeight();
                    //                                float point2x = mTouchPoint.get(j.numOfPoint+1).x * icon.getWidth();
                    //                                float point2y = mTouchPoint.get(j.numOfPoint+1).y * icon.getHeight();
                    //                                bitcanvas.drawLine(point1x, point1y, point2x, point2y, photoPaint);
                    //                            }
                    //
                    //                        }catch (Exception e){
                    //
                    //                        }
                    //                    }
                }
                mEglSurface.makeCurrent();
                // update texture(came from camera)
                mPreviewSurface.updateTexImage();
                //mCamera2Surface.updateTexImage();

                // get texture matrix
                mPreviewSurface.getTransformMatrix(mStMatrix);
                // notify video encoder if it exist
                if (mEncoder != null) {
                    // notify to capturing thread that the camera frame is available.
                    if (mEncoder instanceof MediaVideoEncoder)
                        ((MediaVideoEncoder) mEncoder).frameAvailableSoon(mStMatrix, icon);
                    else
                        mEncoder.frameAvailableSoon();
                }
                // draw to preview screen
                mDrawer.draw(mTexIds, mStMatrix, 0, icon);//屏幕的draw

                mEglSurface.swap();
/*				// sample code to read pixels into Buffer and save as a Bitmap (part1)
				buffer.position(offset);
				GLES20.glReadPixels(0, 0, 640, 480, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
				if (++cnt == 100) { // save as a Bitmap, only once on this sample code
					// if you save every frame as a Bitmap, app will crash by Out of Memory exception...
					Log.i(TAG, "Capture image using glReadPixels:offset=" + offset);
					final Bitmap bitmap = createBitmap(pixels,offset,  640, 480);
					final File outputFile = MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".png");
					try {
						final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
						try {
							try {
								bitmap.compress(CompressFormat.PNG, 100, os);
								os.flush();
								bitmap.recycle();
							} catch (IOException e) {
							}
						} finally {
							os.close();
						}
					} catch (FileNotFoundException e) {
					} catch (IOException e) {
					}
				}
				offset = (offset + BUF_STRIDE) % BUF_SIZE;
*/
/*				// sample code to read pixels into Buffer and save as a Bitmap (part2)
		        buf.order(ByteOrder.LITTLE_ENDIAN);	// it is enough to call this only once.
		        GLES20.glReadPixels(0, 0, 640, 480, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
		        buf.rewind();
				if (++cnt == 100) {	// save as a Bitmap, only once on this sample code
					// if you save every frame as a Bitmap, app will crash by Out of Memory exception...
					final File outputFile = MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".png");
			        BufferedOutputStream os = null;
					try {
				        try {
				            os = new BufferedOutputStream(new FileOutputStream(outputFile));
				            Bitmap bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
				            bmp.copyPixelsFromBuffer(buf);
				            bmp.compress(Bitmap.CompressFormat.PNG, 90, os);
				            bmp.recycle();
				        } finally {
				            if (os != null) os.close();
				        }
					} catch (FileNotFoundException e) {
					} catch (IOException e) {
					}
				}
*/
            }

/*			// sample code to read pixels into IntBuffer and save as a Bitmap (part1)
			private static Bitmap createBitmap(final int[] pixels, final int offset, final int width, final int height) {
				final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
				paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[] {
						0, 0, 1, 0, 0,
						0, 1, 0, 0, 0,
						1, 0, 0, 0, 0,
						0, 0, 0, 1, 0
					})));

				final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				final Canvas canvas = new Canvas(bitmap);

				final Matrix matrix = new Matrix();
				matrix.postScale(1.0f, -1.0f);
				matrix.postTranslate(0, height);
				canvas.concat(matrix);

				canvas.drawBitmap(pixels, offset, width, 0, 0, width, height, false, paint);

				return bitmap;
			} */

            @Override
            public final void run() {
                Log.d(TAG, getName() + " started");
                init();
                Looper.prepare();
                synchronized (mSync) {
                    mHandler = new RenderHandler(mFpsCounter, this);
                    mSync.notify();
                }

                Looper.loop();

                Log.d(TAG, getName() + " finishing");
                release();
                synchronized (mSync) {
                    mHandler = null;
                    mSync.notify();
                }
            }

            private final void init() {
                if (DEBUG) Log.v(TAG, "RenderThread#init:");
                // create EGLContext for this thread
                mEgl = EGLBase.createFrom(null, false, false);
                mEglSurface = mEgl.createFromSurface(mSurface);
                mEglSurface.makeCurrent();
                // create drawing object
                mDrawer = new GLDrawer2D1(true);
            }

            private final void release() {
                if (DEBUG) Log.v(TAG, "RenderThread#release:");
                if (mDrawer != null) {
                    mDrawer.release();
                    mDrawer = null;
                }
                if (mPreviewSurface != null) {
                    mPreviewSurface.release();
                    mPreviewSurface = null;
                }
                if (mTexId >= 0) {
                    GLHelper1.deleteTex(mTexId);
                    mTexId = -1;
                }
                if (mEglSurface != null) {
                    mEglSurface.release();
                    mEglSurface = null;
                }
                if (mEgl != null) {
                    mEgl.release();
                    mEgl = null;
                }
            }
        }
    }
}
