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

package com.dytest.wcc.cameracommon.widget;

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

import com.dytest.wcc.cameracommon.encoder.IVideoEncoder;
import com.dytest.wcc.cameracommon.encoder.MediaEncoder;
import com.dytest.wcc.cameracommon.encoder.MediaVideoEncoder;
import com.dytest.wcc.common.base.BaseApplication;
import com.dytest.wcc.common.utils.TempConvertUtils;
import com.dytest.wcc.common.widget.dragView.DrawLineRectHint;
import com.dytest.wcc.common.widget.dragView.MeasureEntity;
import com.dytest.wcc.common.widget.dragView.MeasureTempContainerView;
import com.serenegiant.glutils.EGLBase;
import com.serenegiant.usb.ITemperatureCallback;
import com.serenegiant.utils.FpsCounter;

import java.text.DecimalFormat;
import java.util.Locale;


/**
 * change the view size with keeping the specified aspect ratio.
 * if you set this view with in a FrameLayout and set property "android:layout_gravity="center",
 * you can show this view in the center of screen and keep the aspect ratio of content
 * XXX it is better that can set the aspect ratio as xml property
 */
public class UVCCameraTextureView extends AspectRatioTextureView    // API >= 14
		implements TextureView.SurfaceTextureListener, CameraViewInterface {

	private static final boolean       DEBUG        = true;    // TODO set false on release
	private static final String        TAG          = "UVCCameraTextureView";
	private final        Object        mCaptureSync = new Object();
	/**
	 * for calculation of frame rate
	 * ??????????????????
	 */
	private final        FpsCounter    mFpsCounter  = new FpsCounter();
	private              boolean       mHasSurface;
	private              RenderHandler mRenderHandler;
	private              Bitmap        mTempBitmap;
	private              boolean       mRequestCaptureStillImage;
	private              Callback      mCallback;
	private              Surface       mPreviewSurface;

	public UVCCameraTextureView (final Context context) {
		this(context, null, 0);
	}

	public UVCCameraTextureView (final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public UVCCameraTextureView (final Context context, final AttributeSet attrs, final int defStyle) {

		super(context, attrs, defStyle);
		//        Log.e(TAG, "UVCCameraTextureView: "+ System.currentTimeMillis());
		//        Log.e("UVCCameraTextureView","===============constructor");
		setSurfaceTextureListener(this);
		//        Log.e(TAG, "UVCCameraTextureView: "+ System.currentTimeMillis());
	}

	@Override
	public void onResume () {
		if (DEBUG)
			Log.v(TAG, "onResume:");
		if (mHasSurface) {
			mRenderHandler = RenderHandler.createHandler(mFpsCounter, super.getSurfaceTexture(), getWidth(), getHeight());
		}
	}

	@Override
	public void onPause () {
		if (DEBUG)
			Log.e(TAG, "onPause:");
		if (mRenderHandler != null) {
			mRenderHandler.release();
			mRenderHandler = null;
		}
		if (mTempBitmap != null) {
			mTempBitmap.recycle();
			mTempBitmap = null;
		}
	}

//	@Override
//	public boolean onTouchEvent (MotionEvent event) {
//		Log.e(TAG, "=========UVCCameraTextureView onTouchEvent: ");
//		return super.onTouchEvent(event);
//	}

	@Override
	public void onSurfaceTextureAvailable (final SurfaceTexture surface, final int width, final int height) {
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
	public void onSurfaceTextureSizeChanged (final SurfaceTexture surface, final int width, final int height) {
		if (DEBUG)
			Log.v(TAG, "onSurfaceTextureSizeChanged:" + surface);
		if (mRenderHandler != null) {
			mRenderHandler.resize(width, height);
		}
		if (mCallback != null) {
			mCallback.onSurfaceChanged(this, getSurface(), width, height);
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed (final SurfaceTexture surface) {
		if (DEBUG)
			Log.v(TAG, "onSurfaceTextureDestroyed:" + surface);
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
	public void onSurfaceTextureUpdated (final SurfaceTexture surface) {
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
	public boolean hasSurface () {
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
	public Bitmap captureStillImage () {
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

	public void openSysCamera () {
		if (mRenderHandler != null) {
			mRenderHandler.openSysCamera();
		}
	}

	public void closeSysCamera () {
		if (mRenderHandler != null) {
			mRenderHandler.closeSysCamera();
		}
	}

	public float[] GetTemperatureData () {
		return mRenderHandler != null ? mRenderHandler.GetTemperatureData() : null;
	}

	@Override
	public SurfaceTexture getSurfaceTexture () {
		return mRenderHandler != null ? mRenderHandler.getPreviewTexture() : null;
	}

	@Override
	public Surface getSurface () {
		if (DEBUG)
			Log.v(TAG, "getSurface:hasSurface=" + mHasSurface);
		if (mPreviewSurface == null) {
			final SurfaceTexture st = getSurfaceTexture();
			if (st != null) {
				mPreviewSurface = new Surface(st);
			}
		}
		return mPreviewSurface;
	}

	@Override
	public void setVideoEncoder (final IVideoEncoder encoder) {
		if (mRenderHandler != null)
			mRenderHandler.setVideoEncoder(encoder);
		//        Log.e(TAG, "setVideoEncoder: ");
	}


	@Override
	public void setCallback (final Callback callback) {
		mCallback = callback;
	}

	@Override
	public ITemperatureCallback getTemperatureCallback () {
		return mRenderHandler != null ? mRenderHandler.getTemperatureCallback() : null;
	}

	public int getFeaturePointsControl () {
		return mRenderHandler != null ? mRenderHandler.getFeaturePointsControl() : 0;
	}

	//    @Override
	//    public TcpITemperatureCallback getTcpTemperatureCallback() {
	//        return mRenderHandler != null ? mRenderHandler.getTcpTemperatureCallback() : null;
	//    }

	public void setVertices (float scale) {
		if (mRenderHandler != null) {
			mRenderHandler.setVertices(scale);
		}
	}

	public void setTemperatureCbing (boolean isTempCbing) {
		if (mRenderHandler != null) {
			mRenderHandler.setTemperatureCbing(isTempCbing);
		}
	}

	public void setSuportWH (int w, int h) {
		if (mRenderHandler != null) {
			mRenderHandler.setSuportWH(w, h);
		}
	}

	public void initTempFontSize (float fontSize) {
		if (mRenderHandler != null) {
			mRenderHandler.initTempFontSize(fontSize);
		}
	}

	public void S0_RotateMatrix_180 (boolean isRotate) {
		if (mRenderHandler != null) {
			mRenderHandler.S0_RotateMatrix_180(isRotate);
		}
	}


	/**
	 * ?????? ?????????  ????????? ??????????????? ??????????????? ?????? ????????? ?????????????????????????????????
	 *
	 * @param highTemp
	 * @param lowTemp
	 * @param centerTemp
	 * @param normalPointTemp
	 * @param halfBitmap
	 */
	public void setFrameBitmap (Bitmap highTemp, Bitmap lowTemp, Bitmap centerTemp, Bitmap normalPointTemp, int halfBitmap) {
		if (mRenderHandler != null) {
			mRenderHandler.setFrameBitmap(highTemp, lowTemp, centerTemp, normalPointTemp, halfBitmap);
		}
	}

	public void iniTempBitmap (int w, int h) {
		if (mRenderHandler != null) {
			mRenderHandler.iniTempBitmap(w, h);
		}
	}

	public void setVidPid (int vid, int pid) {
		if (mRenderHandler != null) {
			mRenderHandler.setVidPid(vid, pid);
		}
	}

	public void setTinyCCorrection (float tinyCorrection) {
		if (mRenderHandler != null) {
			mRenderHandler.setTinyCCorrection(tinyCorrection);
		}
	}


	public void setDragTempContainer (MeasureTempContainerView measureTempContainerView) {
		if (mRenderHandler != null) {
			mRenderHandler.setDragTempContainer(measureTempContainerView);
		}
	}


	public void setTemperatureAnalysisMode (int mode) {
		mRenderHandler.setTemperatureAnalysisMode(mode);
	}


	public void relayout (int rotate) {
		mRenderHandler.relayout(rotate);
	}

	//UVCCameraTextureView ???????????? ?????? ???????????????
	public void startTempAlarm (float markAlarmTemp) {
		mRenderHandler.startTempAlarm(markAlarmTemp);
	}

	public void stopTempAlarm () {
		mRenderHandler.stopTempAlarm();
	}

	//    public void setOnHighTempChangedCallback(onHighTempChangedCallback callback){ mRenderHandler.setOnHighTempChangedCallback(callback); }

	//    public void watermarkOnOff(boolean isWatermaker) {
	//        mRenderHandler.watermarkOnOff(isWatermaker);
	//    }

	public void tempShowOnOff (boolean isTempShow) {
		if (mRenderHandler != null)
			mRenderHandler.tempshowOnOff(isTempShow);
	}

	public void setDrawHint (DrawLineRectHint hint) {
		mRenderHandler.setDrawHint(hint);
	}

	/**
	 * ?????? ?????????0  ?????????1  ???????????????1 ???????????????
	 *
	 * @param pointType ?????????0  ?????????1  ???????????????1
	 */
	public void openFeaturePoints (int pointType) {
		mRenderHandler.openFeaturePoints(pointType);
	}

	/**
	 * ?????? ?????????0  ?????????1  ???????????????1 ???????????????
	 *
	 * @param pointType ?????????0  ?????????1  ???????????????1
	 */
	public void closeFeaturePoints (int pointType) {
		mRenderHandler.closeFeaturePoints(pointType);
	}

	//    public void setUnitTemperature(int mode) {
	//        mRenderHandler.setUnitTemperature(mode);
	//    }

	public void resetFps () {
		mFpsCounter.reset();
	}

	/**
	 * update frame rate of image processing
	 */
	public void updateFps () {
		mFpsCounter.update();
	}

	/**
	 * get current frame rate of image processing
	 *
	 * @return ???????????? ????????? ??????
	 */
	public float getFps () {
		return mFpsCounter.getFps();
	}

	/**
	 * get total frame rate from start
	 *
	 * @return ????????????????????? ????????????
	 */
	public float getTotalFps () {
		return mFpsCounter.getTotalFps();
	}

	/**
	 * render camera frames on this view on a private thread
	 * ????????????????????????????????????????????????
	 *
	 * @author saki
	 */
	private static final class RenderHandler extends Handler implements SurfaceTexture.OnFrameAvailableListener {

		private static final int          MSG_REQUEST_RENDER = 1;
		private static final int          MSG_SET_ENCODER    = 2;
		private static final int          MSG_CREATE_SURFACE = 3;
		private static final int          MSG_RESIZE         = 4;
		private static final int          MSG_TERMINATE      = 9;
		private static final int          MSG_CHECK_AREA     = 5;
		private final        FpsCounter   mFpsCounter;
		private              RenderThread mThread;
		private              boolean      mIsActive          = true;


		private RenderHandler (final FpsCounter counter, final RenderThread thread) {
			mThread = thread;
			mFpsCounter = counter;
		}

		public static final RenderHandler createHandler (final FpsCounter counter, final SurfaceTexture surface, final int width, final int height) {

			final RenderThread thread = new RenderThread(counter, surface, width, height);
			thread.start();
			return thread.getHandler();
		}

		public void setTemperatureAnalysisMode (int mode) {
			mThread.setTemperatureAnalysisMode(mode);
		}

		public void relayout (int rotate) {
			mThread.relayout(rotate);
		}

		//RenderHandler ???????????? ?????? ???????????????
		public void startTempAlarm (float markAlarmTemp) {
			mThread.startTempAlarm(markAlarmTemp);
		}

		public void stopTempAlarm () {
			mThread.stopTempAlarm();
		}


		public void setFrameBitmap (Bitmap highTemp, Bitmap lowTemp, Bitmap centerTemp, Bitmap normalPointTemp, int halfBitmap) {
			mThread.setFrameBitmap(highTemp, lowTemp, centerTemp, normalPointTemp, halfBitmap);
		}

		public void setDrawHint (DrawLineRectHint hint) {
			mThread.setDrawHint(hint);
		}

		public void tempshowOnOff (boolean isTempShow) {
			mThread.tempshowOnOff(isTempShow);
		}

		public void openFeaturePoints (int pointType) {
			mThread.openFeaturePoints(pointType);
		}

		public void closeFeaturePoints (int pointType) {
			mThread.closeFeaturePoints(pointType);
		}

		public final void setVideoEncoder (final IVideoEncoder encoder) {
			if (DEBUG)
				Log.v(TAG, "setVideoEncoder:");
			if (mIsActive)
				sendMessage(obtainMessage(MSG_SET_ENCODER, encoder));
		}

		public final SurfaceTexture getPreviewTexture () {
			if (DEBUG)
				Log.v(TAG, "getPreviewTexture:");
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

		public void resize (final int width, final int height) {
			if (DEBUG)
				Log.v(TAG, "resize:");
			if (mIsActive) {
				synchronized (mThread.mSync) {
					sendMessage(obtainMessage(MSG_RESIZE, width, height));
					try {
						mThread.mSync.wait();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		public final void release () {
			if (DEBUG)
				Log.v(TAG, "release:");
			if (mIsActive) {
				mIsActive = false;
				removeMessages(MSG_REQUEST_RENDER);
				removeMessages(MSG_SET_ENCODER);
				sendEmptyMessage(MSG_TERMINATE);
			}
		}

		@Override
		public final void onFrameAvailable (final SurfaceTexture surfaceTexture) {
			if (mIsActive) {
				mFpsCounter.count();
				//onDrawFrame() ?????????????????????
				sendEmptyMessage(MSG_REQUEST_RENDER);
			}
		}

		public ITemperatureCallback getTemperatureCallback () {
			return mThread != null ? mThread.getTemperatureCallback() : null;
		}

		public int getFeaturePointsControl () {
			return mThread != null ? mThread.getFeaturePointsControl() : 0;
		}

		public void setVertices (float scale) {
			if (mThread != null) {
				mThread.setVertices(scale);
			}
		}

		public void setTemperatureCbing (boolean isTempCbing) {
			if (mThread != null) {
				mThread.setTemperatureCbing(isTempCbing);
			}
		}

		public float[] GetTemperatureData () {
			return mThread != null ? mThread.GetTemperatureData() : null;
		}


		public void openSysCamera () {
			if (mThread != null) {
				mThread.openSysCamera();
			}
		}

		public void closeSysCamera () {
			if (mThread != null) {
				mThread.closeSysCamera();
			}
		}


		public void setSuportWH (int w, int h) {
			if (mThread != null) {
				mThread.setSuportWH(w, h);
			}
		}

		public void initTempFontSize (float fontsize) {
			if (mThread != null) {
				mThread.initTempFontSize(fontsize);
			}
		}

		public void S0_RotateMatrix_180 (boolean isRotate) {
			if (mThread != null) {
				mThread.S0_RotateMatrix_180(isRotate);
			}
		}

		public void iniTempBitmap (int w, int h) {
			if (mThread != null) {
				mThread.iniTempBitmap(w, h);
			}
		}

		public void setVidPid (int vid, int pid) {
			if (mThread != null) {
				mThread.setVidPid(vid, pid);
			}
		}

		public void setTinyCCorrection (float tinyCorrection) {
			if (mThread != null) {
				mThread.setTinyCCorrection(tinyCorrection);
			}
		}

		public void setDragTempContainer (MeasureTempContainerView measureTempContainerView) {
			if (mThread != null) {
				mThread.setDragTempContainer(measureTempContainerView);
			}
		}

		@Override
		public final void handleMessage (final Message msg) {
			//????????????????????????
			//            Log.e(TAG, "handleMessage= pid="+android.os.Process.myPid()+" tid="+android.os.Process.myTid());
			if (mThread == null)
				return;
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
			private final Object              mSync     = new Object();
			private final SurfaceTexture      mSurface;
			private final float[]             mStMatrix = new float[16];
			private final FpsCounter          mFpsCounter;
			//???????????????
			public        int                 mSupportWidth;
			public        int                 mSupportHeight;
			private       RenderHandler       mHandler;
			private       EGLBase             mEgl;
			/**
			 * IEglSurface instance related to this TextureView
			 * ?????? TextureView ????????? IEglSurface ??????
			 */
			private       EGLBase.IEglSurface mEglSurface;
			private       GLDrawer2D1         mDrawer;
			private       int                 mTexId    = -1;
			private       int[]               mTexIds   = {-1, -1, -1, -1};
			/**
			 * SurfaceTexture instance to receive video images
			 * ????????????????????? SurfaceTexture ??????
			 */
			private       SurfaceTexture      mPreviewSurface, mCamera2Surface;
			private MediaEncoder mEncoder;
			//            private CustomRangeSeekBar mBindSeekBar;
			private int          mViewWidth, mViewHeight;
			private Camera2Helper mCamera2Helper;
			/**
			 * ????????????????????????
			 */
			//            private MediaPlayer mMediaPlayer;
			private boolean       isOverTempAlarm = false; //???????????????????????????
			private int           alarmCount      = 0;
			private boolean       isAlarm         = false;//      ?????????????????????
			//            private boolean isAlarmRing = false;//      ????????????????????????
			private float         mAlarmTemp      = 10000.0f;//?????????????????????  ?????????
			private boolean       isCbTemping     = true;    // ????????????
			private boolean       isCamera2ing    = false;
			//            private Bitmap mCursorBlue, mCursorRed, mCursorYellow, mCursorGreen, mWatermakLogo;
			private float[]       temperature1    = new float[640 * 512 + 10];
			private Bitmap        highTempBt, lowTempBt, centerTempBt, normalPointBt;
			private RectF bpRectF;//??????????????????
			private int   mBitmapRectSize;

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
			private MeasureTempContainerView mMeasureTempContainerView;
			//????????????????????????????????????
			private Paint                    linePaint, dottedLinePaint, photoPaint;
			//??????????????????????????????????????????????????????
			private TextPaint tempTextPaint, tempTextBgTextPaint;
			private float maxtemperature;
			private float mintemperature;
			//????????? ????????? ????????? ???????????? ??????????????? 0x0fff ?????????????????? ?????????f ???????????? ?????????f ???????????? ?????????f ?????????????????? ??????
			private int   featurePointsControl = 0;
			private Rect  dstHighTemp, dstLowTemp, bounds;//???????????????????????????????????????
			private Bitmap icon;
			private int    mVid = 0, mPid = 0;
			private float  tinyCorrection          = 0.0f;
			private Canvas bitcanvas;//?????????????????????????????????icon???
			private int    temperatureAnalysisMode = -1;//, UnitTemperature
			private int    rotate                  = 0;
			private float  widthRatio, heightRatio;
			//?????? ?????? ?????????
			private boolean highTempToggle = false, lowTempToggle = false;
			private      boolean              isTempShow             = true;
			private      DrawLineRectHint     myDrawHint             = null;
			private      DecimalFormat        decimalFormat     ;
//					= new DecimalFormat("0.0");//???????????????????????????????????????????????????2???,??????0??????.
			//            public void setUnitTemperature(int mode) {
			//                this.UnitTemperature = mode;
			//            }
			private      boolean              isRotate_180           = false;
			//????????????????????????
			public final ITemperatureCallback ahITemperatureCallback = new ITemperatureCallback() {
				@Override
				public void onReceiveTemperature (float[] temperature) {
					//					Log.e(TAG, "onReceiveTemperature: ==============ahITemperatureCallback========right==============" + mSupportHeight);
					// ??????????????????????????????256x192  + 10
					if (temperature != null && (temperature.length == (mSupportHeight) * mSupportWidth + 10)) {
						//						Log.e(TAG, "=====onReceiveTemperature: temperature.length == " + temperature.length);
						System.arraycopy(temperature, 0, temperature1, 0, (mSupportHeight) * mSupportWidth + 10);
						if (mPid == 22592 && mVid == 3034) {
							temperature1[0] = temperature[0] + tinyCorrection;
							temperature1[3] = temperature[3] + tinyCorrection;
							temperature1[6] = temperature[6] + tinyCorrection;
							for (int i = 0; i < mSupportHeight * mSupportWidth; i++) {
								temperature1[10 + i] = temperature1[10 + i] + tinyCorrection;
							}
						} else if (mPid == 1 && mVid == 5396) {
							if (isRotate_180) {
								S0_RotateMatrix_180_MaxMin(temperature1, mSupportWidth, mSupportHeight);
							}
						}
						maxtemperature = temperature[3];
						mintemperature = temperature[6];
						//						Log.e(TAG, "==========================temperature==================");
					} else {
						Log.e(TAG, "onReceiveTemperature: ===========temperature============?????????=======");
					}
				}
			};
			private      int                  isFirstCome            = 0;
			private      Rect                 textRect               = new Rect();
			private      Boolean              isT3                   = BaseApplication.deviceName.contentEquals("T3") || BaseApplication.deviceName.contentEquals("DL13") || BaseApplication.deviceName.contentEquals("DV");

			/**
			 * constructor
			 *
			 * @param surface: drawing surface came from TexureView
			 */
			public RenderThread (final FpsCounter fpsCounter, final SurfaceTexture surface, final int width, final int height) {
				mFpsCounter = fpsCounter;
				mSurface = surface;
				mViewWidth = width;
				mViewHeight = height;
				mCamera2Helper = Camera2Helper.getInstance();
				decimalFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.CHINA);
				decimalFormat.applyPattern("0.0");
				// this.highTempRect,lowTempRect,bounds ;//???????????????????????????????????????

				setName("RenderThread");
				//                initFrameBitmap();
			}

			//RenderThread ???????????? ?????? ???????????????
			public void startTempAlarm (float markAlarmTemp) {      //??????????????????
				isOverTempAlarm = true;
				mAlarmTemp = markAlarmTemp;
				alarmCount = 0;
				//                Log.e(TAG,"====================????????????????????????=============== "+ mAlarmTemp );
			}

			public void stopTempAlarm () {     //??????????????????
				//                Log.e(TAG,"====================??????????????????=============== " );
				isOverTempAlarm = false;
				mAlarmTemp = 10000.0f;
				alarmCount = 0;
			}

			public void initTempFontSize (float fontsize) {
				this.photoPaint.setTextSize(fontsize);
				this.tempTextPaint.setTextSize(fontsize);
				this.tempTextBgTextPaint.setTextSize(fontsize);
			}

			/**
			 * ????????? OpenGL ES ?????????????????? ????????????????????????????????????
			 * OpenGL ES
			 * ????????????????????????
			 *
			 * @param w
			 * @param h
			 */
			public void iniTempBitmap (int w, int h) {
				this.icon = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); //???????????????????????????
				this.bitcanvas = new Canvas(icon);//?????????????????????????????????icon???
				//                this.mTouchPointLists = new CopyOnWriteArrayList<>();
				//                this.mTouchLineLists = new CopyOnWriteArrayList<>();
				//                this.mTouchAreaLists = new CopyOnWriteArrayList<>();
				this.photoPaint = new Paint();
				this.photoPaint.setStrokeWidth(5);
				this.photoPaint.setColor(Color.WHITE);
				//                this.alarmPaint = new Paint();
				this.tempTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
				//                this.tempTextPaint.setTextSize(50);
				this.tempTextPaint.setStrokeWidth(5);
				this.tempTextPaint.setStyle(Paint.Style.FILL);

				this.linePaint = new Paint();
				this.linePaint.setStrokeWidth(5);
				this.linePaint.setColor(Color.WHITE);
				this.dottedLinePaint = new Paint();
				this.dottedLinePaint.setStrokeWidth(5);
				this.dottedLinePaint.setPathEffect(new DashPathEffect(new float[]{40, 40}, 0));
				this.dottedLinePaint.setColor(Color.BLACK);

				this.tempTextBgTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
				//                this.tempTextBgTextPaint.setTextSize(50);
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

			public void setVidPid (int vid, int pid) {
				this.mVid = vid;
				this.mPid = pid;
			}

			public void setTinyCCorrection (float tinyCorrection) {
				this.tinyCorrection = tinyCorrection;
			}

			public void setDragTempContainer (MeasureTempContainerView measureTempContainerView) {
				this.mMeasureTempContainerView = measureTempContainerView;
			}

			public final RenderHandler getHandler () {
				if (DEBUG)
					Log.v(TAG, "RenderThread#getHandler:");
				synchronized (mSync) {
					// create rendering thread
					if (mHandler == null)
						try {
							mSync.wait();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
				}
				return mHandler;
			}

			public void resize (final int width, final int height) {
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

			public final void updatePreviewSurface () {
				if (DEBUG)
					Log.i(TAG, "RenderThread#updatePreviewSurface:");
				synchronized (mSync) {
					//					if (mPreviewSurface != null) {
					//						if (DEBUG)
					//							Log.d(TAG, "updatePreviewSurface:release mPreviewSurface");
					//						mPreviewSurface.setOnFrameAvailableListener(null);
					//						mPreviewSurface.release();//??????????????????????????????
					//						mPreviewSurface = null;
					//					}
					mEglSurface.makeCurrent();
					//           if (mTexId >= 0) {
					//				mDrawer.deleteTex(mTexId);
					//           }
					if (mTexIds[0] >= 0 || mTexIds[1] >= 0 || mTexIds[2] >= 0 || mTexIds[3] >= 0) {
						mDrawer.deleteTexes(mTexIds);
					}
					// create texture and SurfaceTexture for input from camera
					//            mTexId = mDrawer.initTex();
					int[] para = {4, GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GL_TEXTURE_2D, GLES20.GL_NEAREST, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GL_TEXTURE_2D, GLES20.GL_NEAREST, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE};
					mTexIds = mDrawer.initTexes(para);
					if (DEBUG)
						Log.v(TAG, "updatePreviewSurface:tex_id=" + mTexId);
					mPreviewSurface = new SurfaceTexture(mTexIds[0]);
					mPreviewSurface.setDefaultBufferSize(mViewWidth, mViewHeight);
					mPreviewSurface.setOnFrameAvailableListener(mHandler);
					//					if (DEBUG)Log.e(TAG, "mHandler=====" + mHandler);
					// notify to caller thread that previewSurface is ready
					mSync.notifyAll();
				}
			}

			public final void setEncoder (final MediaEncoder encoder) {
				if (DEBUG)
					Log.v(TAG, "RenderThread#setEncoder:encoder=" + encoder);
				if (encoder != null && (encoder instanceof MediaVideoEncoder)) {
					((MediaVideoEncoder) encoder).setEglContext(mEglSurface.getContext(), mTexIds);
				}
				mEncoder = encoder;
			}

			public void setFrameBitmap (Bitmap highTemp, Bitmap lowTemp, Bitmap centerTemp, Bitmap normalPointTemp, int bpSize) {
				highTempBt = highTemp;
				lowTempBt = lowTemp;
				centerTempBt = centerTemp;
				normalPointBt = normalPointTemp;
				this.mBitmapRectSize = bpSize;
				bpRectF = new RectF(0, 0, mBitmapRectSize, mBitmapRectSize);

				//                mMediaPlayer = MediaPlayer.create(mContext.get(), R.raw.a2_ding);
			}

			public void setDrawHint (DrawLineRectHint hint) {
				myDrawHint = hint;
			}

			public void relayout (int rotate) {
				this.rotate = rotate;
			}

			public void tempshowOnOff (boolean isTempShow) {
				this.isTempShow = isTempShow;
			}

			public float[] GetTemperatureData () {
				return temperature1;
			}

			/**
			 * ?????? ?????????0  ?????????1  ???????????????1 ???????????????
			 *
			 * @param pointType ?????????0  ?????????1  ???????????????1
			 */
			public void openFeaturePoints (int pointType) {
				switch (pointType) {
					case 0:
						featurePointsControl = featurePointsControl | 0x0f00;
						break;
					case 1:
						featurePointsControl = featurePointsControl | 0x00f0;
						break;
					case 2:
						featurePointsControl = featurePointsControl | 0x000f;
						break;
					default:
						break;
				}
			}

			/**
			 * ?????? ?????????  ?????????  ??????????????? ???????????????
			 *
			 * @param pointType ?????????0  ?????????1  ???????????????1
			 */
			public void closeFeaturePoints (int pointType) {
				switch (pointType) {
					case 0:
						featurePointsControl = featurePointsControl & 0x00ff;
						break;
					case 1:
						featurePointsControl = featurePointsControl & 0x0f0f;
						break;
					case 2:
						featurePointsControl = featurePointsControl & 0x0ff0;
						break;
					default:
						break;
				}
			}

			public int getFeaturePointsControl () {
				return featurePointsControl;
			}

			public void openSysCamera () {
				if (mCamera2Helper == null) {
					mCamera2Helper = mCamera2Helper.getInstance();
				}

				mCamera2Helper.openCamera(640, 480);
				isCamera2ing = true;
			}

			public void closeSysCamera () {
				if (mCamera2Helper != null) {
					mCamera2Helper.closeCamera();
				}
				isCamera2ing = false;
			}

			public void setTemperatureAnalysisMode (int mode) {
				this.temperatureAnalysisMode = mode;
			}

			public void S0_RotateMatrix_180 (boolean isRotate) {
				isRotate_180 = isRotate;
			}

			private void S0_RotateMatrix_180_MaxMin (float[] tempArray, int width, int height) {
				tempArray[1] = width - tempArray[1];
				tempArray[2] = height - tempArray[2];

				tempArray[4] = width - tempArray[4];
				tempArray[5] = height - tempArray[5];
			}

			/**
			 * JNI????????????????????????Java???
			 *
			 * @return
			 */
			public ITemperatureCallback getTemperatureCallback () {
				//				Log.e(TAG, "========??????UVC????????????===getTemperatureCallback= "+ahITemperatureCallback);
				return ahITemperatureCallback;
			}

			public void setTemperatureCbing (boolean isTempCbing) {
				isCbTemping = isTempCbing;
			}

			public void setSuportWH (int w, int h) {
				mSupportHeight = h;
				mSupportWidth = w;
				widthRatio = mSupportWidth * 1.0f / icon.getWidth();
				heightRatio = (mSupportHeight) * 1.0f / icon.getHeight();//???????????????
				//				Log.i(TAG, "widthRatio=" + widthRatio);
				//				Log.i(TAG, "heightRatio=" + heightRatio);
			}

			public void setVertices (float scale) {
				mDrawer.setVertices(scale);
			}

			/**
			 * ????????????
			 *
			 * @param origin ??????
			 * @param alpha  ???????????????????????????
			 * @return ??????????????????
			 */
			private Bitmap rotateBitmap (Bitmap origin, float alpha) {
				if (origin == null) {
					return null;
				}
				int width = origin.getWidth();
				int height = origin.getHeight();
				Matrix matrix = new Matrix();
				matrix.setRotate(alpha);
				// ????????????????????????
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

			/**
			 * @param x             X??????
			 * @param y             Y??????
			 * @param tempText      ???????????????String
			 * @param tpBg          ?????????????????????
			 * @param tp            ??????String??????
			 * @param textDirection ?????????????????????
			 */
			private void xy2DrawText (float x, float y, String tempText, TextPaint tpBg, TextPaint tp, int textDirection) {
				if (textDirection != 0) {

				} else { //textDirection == 0
					//????????????????????? ??????
					tp.getTextBounds(tempText, 0, tempText.length(), textRect);
					if (x + tp.measureText(tempText) > icon.getWidth()) {
						if (textRect.height() + y > icon.getHeight()) {
							bitcanvas.drawText(tempText, x - mBitmapRectSize / 2.0f - tp.measureText(tempText), y - tpBg.descent(), tpBg);
							bitcanvas.drawText(tempText, x - mBitmapRectSize / 2.0f - tp.measureText(tempText), y - tp.descent(), tp);
						} else {
							bitcanvas.drawText(tempText, x - mBitmapRectSize / 2.0f - tp.measureText(tempText), y + mBitmapRectSize / 2.0f, tpBg);
							bitcanvas.drawText(tempText, x - mBitmapRectSize / 2.0f - tp.measureText(tempText), y + mBitmapRectSize / 2.0f, tp);
						}
					} else {
						if (textRect.height() + y > icon.getHeight()) {//???????????? ?????????
							bitcanvas.drawText(tempText, x + mBitmapRectSize / 2.0f, y - tpBg.descent(), tpBg);
							bitcanvas.drawText(tempText, x + mBitmapRectSize / 2.0f, y - tp.descent(), tp);
						} else {
							bitcanvas.drawText(tempText, x + mBitmapRectSize / 2.0f, y + mBitmapRectSize / 2.0f, tpBg);
							bitcanvas.drawText(tempText, x + mBitmapRectSize / 2.0f, y + mBitmapRectSize / 2.0f, tp);
						}

					}
				}
			}

			//??????????????????
			private float updatePointTemp (float x, float y) {
				//?????????????????????
				int index = (10 + (int) (y * heightRatio) * mSupportWidth + (int) (x * widthRatio));
				return temperature1[index];
			}

			public void DrawControl () {
				//                Log.e(TAG,"DrawControl id =============="+calendar.get(Calendar.SECOND));

				//                isAlarmRing = isHighTempAlarm && (mMarkAlarmTemp < maxtemperature);

				//                mBindSeekBar.Update(maxtemperature,mintemperature);//??????seekbar

				isAlarm = false;
				if (mMeasureTempContainerView != null && temperature1 != null) {
					mMeasureTempContainerView.upDateTemp(temperature1);
					if (mMeasureTempContainerView.getUserAddData().size() > 0) {
						for (int i = 0; i < mMeasureTempContainerView.getUserAddData().size(); i++) {
							if (mMeasureTempContainerView.getUserAddData().get(i).getType() == 1) {
								isAlarm = isAlarm | (updatePointTemp(mMeasureTempContainerView.getUserAddData().get(i).getPointTemp().getStartPointX(), mMeasureTempContainerView.getUserAddData().get(i).getPointTemp().getStartPointY()) > mAlarmTemp);
							} else {
								isAlarm = isAlarm | (temperature1[(int) ((mMeasureTempContainerView.getUserAddData().get(i).getOtherTemp().getMaxTempX() * widthRatio) + ((mMeasureTempContainerView.getUserAddData().get(i).getOtherTemp().getMaxTempY() * heightRatio) * mSupportWidth) + 10)] > mAlarmTemp);
							}
						}
					} else {
						isAlarm = isAlarm | (mAlarmTemp < maxtemperature);
					}
				} else {
					isAlarm = false;
				}
				isAlarm = isAlarm && isOverTempAlarm;
			}

			//            private

			/**
			 * draw a frame (and request to draw for video capturing if it is necessary)
			 */
			public final void onDrawFrame () {
				bitcanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
				if (!isCbTemping) {
					//                    bitcanvas.save(Canvas.ALL_SAVE_FLAG);//??????????????????????????????
					bitcanvas.save();
				} else {
					//					bitcanvas.restore();
					//					bitcanvas.rotate(180);
					//					bitcanvas.save();
					//					if (rotate == 90 || rotate == 180){
					//						bitcanvas.save();
					//						bitcanvas.rotate(180);
					//					}else {
					//						bitcanvas.save();
					//						bitcanvas.rotate(0);
					//					}
					//					bitcanvas.rotate(180);
					//?????????????????????  ?????????
					if (isAlarm && (alarmCount > 15 && alarmCount < 30)) {
						linePaint.setColor(Color.RED);
						bitcanvas.drawLine(5, 5, 5, mViewHeight - 5, linePaint);
						bitcanvas.drawLine(5, 5, mViewWidth - 5, 5, linePaint);
						bitcanvas.drawLine(mViewWidth - 5, mViewHeight - 5, mViewWidth - 5, 5, linePaint);
						bitcanvas.drawLine(mViewWidth - 5, mViewHeight - 5, 5, mViewHeight - 5, linePaint);
					}
					//?????????????????? ??? ???????????????
					if (isAlarm) {
						if (alarmCount >= 30) {
							alarmCount = 0;
						} else {
							alarmCount++;
						}
					}
					//?????????????????????
					if (myDrawHint != null && myDrawHint.isNeedDraw()) {
						if (myDrawHint.getDrawTempMode() == 2) {
							bitcanvas.drawLine(myDrawHint.getStartXCoordinate(), myDrawHint.getStartYCoordinate(), myDrawHint.getEndXCoordinate(), myDrawHint.getEndYCoordinate(), linePaint);
							bitcanvas.drawLine(myDrawHint.getStartXCoordinate(), myDrawHint.getStartYCoordinate(), myDrawHint.getEndXCoordinate(), myDrawHint.getEndYCoordinate(), dottedLinePaint);
						} else if (myDrawHint.getDrawTempMode() == 3) {
							bitcanvas.drawLine(myDrawHint.getStartXCoordinate(), myDrawHint.getStartYCoordinate(), myDrawHint.getStartXCoordinate(), myDrawHint.getEndYCoordinate(), linePaint);
							bitcanvas.drawLine(myDrawHint.getStartXCoordinate(), myDrawHint.getStartYCoordinate(), myDrawHint.getEndXCoordinate(), myDrawHint.getStartYCoordinate(), linePaint);
							bitcanvas.drawLine(myDrawHint.getEndXCoordinate(), myDrawHint.getStartYCoordinate(), myDrawHint.getEndXCoordinate(), myDrawHint.getEndYCoordinate(), linePaint);
							bitcanvas.drawLine(myDrawHint.getStartXCoordinate(), myDrawHint.getEndYCoordinate(), myDrawHint.getEndXCoordinate(), myDrawHint.getEndYCoordinate(), linePaint);

							bitcanvas.drawLine(myDrawHint.getStartXCoordinate(), myDrawHint.getStartYCoordinate(), myDrawHint.getStartXCoordinate(), myDrawHint.getEndYCoordinate(), dottedLinePaint);
							bitcanvas.drawLine(myDrawHint.getStartXCoordinate(), myDrawHint.getStartYCoordinate(), myDrawHint.getEndXCoordinate(), myDrawHint.getStartYCoordinate(), dottedLinePaint);
							bitcanvas.drawLine(myDrawHint.getEndXCoordinate(), myDrawHint.getStartYCoordinate(), myDrawHint.getEndXCoordinate(), myDrawHint.getEndYCoordinate(), dottedLinePaint);
							bitcanvas.drawLine(myDrawHint.getStartXCoordinate(), myDrawHint.getEndYCoordinate(), myDrawHint.getEndXCoordinate(), myDrawHint.getEndYCoordinate(), dottedLinePaint);
						}
					}
					//?????????????????????????????????
					if (mMeasureTempContainerView != null) {
						for (MeasureEntity measureEntity : mMeasureTempContainerView.getUserAddData()) {
							if (!measureEntity.isSelect()) {//?????????
								//????????? ??????????????????????????????????????????????????????????????????????????????????????????
								if (measureEntity.getType() == 1) {
									bpRectF.left = measureEntity.getPointTemp().getStartPointX() - mBitmapRectSize / 2.0f;
									bpRectF.right = measureEntity.getPointTemp().getStartPointX() + mBitmapRectSize / 2.0f;
									bpRectF.top = measureEntity.getPointTemp().getStartPointY() - mBitmapRectSize / 2.0f;
									bpRectF.bottom = measureEntity.getPointTemp().getStartPointY() + mBitmapRectSize / 2.0f;
									// ?????????
									tempTextPaint.setColor(Color.BLACK);
									bitcanvas.drawBitmap(normalPointBt, null, bpRectF, photoPaint);
									xy2DrawText(measureEntity.getPointTemp().getStartPointX(), measureEntity.getPointTemp().getStartPointY(), measureEntity.getPointTemp().getTemp(), tempTextBgTextPaint, tempTextPaint, 0);
								}
								//?????????????????????
								if (measureEntity.getType() == 2) {
									linePaint.setColor(Color.WHITE);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getStartPointY(), measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getEndPointY(), linePaint);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getStartPointY(), measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getEndPointY(), dottedLinePaint);

									if (highTempToggle) {
										tempTextPaint.setColor(Color.RED);
										xy2DrawText(measureEntity.getOtherTemp().getMaxTempX(), measureEntity.getOtherTemp().getMaxTempY(), measureEntity.getOtherTemp().getMaxTemp(), tempTextBgTextPaint, tempTextPaint, 0);

										bpRectF.left = measureEntity.getOtherTemp().getMaxTempX() - mBitmapRectSize / 2.0f;
										bpRectF.right = measureEntity.getOtherTemp().getMaxTempX() + mBitmapRectSize / 2.0f;
										bpRectF.top = measureEntity.getOtherTemp().getMaxTempY() - mBitmapRectSize / 2.0f;
										bpRectF.bottom = measureEntity.getOtherTemp().getMaxTempY() + mBitmapRectSize / 2.0f;
										bitcanvas.drawBitmap(highTempBt, null, bpRectF, photoPaint);
									}

									if (lowTempToggle) {
										tempTextPaint.setColor(Color.BLUE);
										xy2DrawText(measureEntity.getOtherTemp().getMinTempX(), measureEntity.getOtherTemp().getMinTempY(), measureEntity.getOtherTemp().getMinTemp(), tempTextBgTextPaint, tempTextPaint, 0);

										bpRectF.left = measureEntity.getOtherTemp().getMinTempX() - mBitmapRectSize / 2.0f;
										bpRectF.right = measureEntity.getOtherTemp().getMinTempX() + mBitmapRectSize / 2.0f;
										bpRectF.top = measureEntity.getOtherTemp().getMinTempY() - mBitmapRectSize / 2.0f;
										bpRectF.bottom = measureEntity.getOtherTemp().getMinTempY() + mBitmapRectSize / 2.0f;
										bitcanvas.drawBitmap(lowTempBt, null, bpRectF, photoPaint);
									}
								}
								//????????????????????????
								if (measureEntity.getType() == 3) {
									linePaint.setColor(Color.WHITE);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getStartPointY(), measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getStartPointY(), linePaint);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getStartPointY(), measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getEndPointY(), linePaint);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getEndPointY(), measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getEndPointY(), linePaint);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getEndPointY(), measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getStartPointY(), linePaint);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getStartPointY(), measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getStartPointY(), dottedLinePaint);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getStartPointY(), measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getEndPointY(), dottedLinePaint);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getEndPointY(), measureEntity.getOtherTemp().getStartPointX(), measureEntity.getOtherTemp().getEndPointY(), dottedLinePaint);
									bitcanvas.drawLine(measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getEndPointY(), measureEntity.getOtherTemp().getEndPointX(), measureEntity.getOtherTemp().getStartPointY(), dottedLinePaint);

									if (highTempToggle) {
										tempTextPaint.setColor(Color.RED);
										xy2DrawText(measureEntity.getOtherTemp().getMaxTempX(), measureEntity.getOtherTemp().getMaxTempY(), measureEntity.getOtherTemp().getMaxTemp(), tempTextBgTextPaint, tempTextPaint, 0);

										bpRectF.left = measureEntity.getOtherTemp().getMaxTempX() - mBitmapRectSize / 2.0f;
										bpRectF.right = measureEntity.getOtherTemp().getMaxTempX() + mBitmapRectSize / 2.0f;
										bpRectF.top = measureEntity.getOtherTemp().getMaxTempY() - mBitmapRectSize / 2.0f;
										bpRectF.bottom = measureEntity.getOtherTemp().getMaxTempY() + mBitmapRectSize / 2.0f;
										bitcanvas.drawBitmap(highTempBt, null, bpRectF, photoPaint);
									}
									if (lowTempToggle) {
										tempTextPaint.setColor(Color.BLUE);
										xy2DrawText(measureEntity.getOtherTemp().getMinTempX(), measureEntity.getOtherTemp().getMinTempY(), measureEntity.getOtherTemp().getMinTemp(), tempTextBgTextPaint, tempTextPaint, 0);

										bpRectF.left = measureEntity.getOtherTemp().getMinTempX() - mBitmapRectSize / 2.0f;
										bpRectF.right = measureEntity.getOtherTemp().getMinTempX() + mBitmapRectSize / 2.0f;
										bpRectF.top = measureEntity.getOtherTemp().getMinTempY() - mBitmapRectSize / 2.0f;
										bpRectF.bottom = measureEntity.getOtherTemp().getMinTempY() + mBitmapRectSize / 2.0f;
										bitcanvas.drawBitmap(lowTempBt, null, bpRectF, photoPaint);
									}
									//                               bitcanvas.drawBitmap(highTempBt,tempWidgetObj.getOtherTemp().getMaxTempX()-19,tempWidgetObj.getOtherTemp().getMaxTempY()-19,photoPaint);
									//                               bitcanvas.drawBitmap(lowTempBt,tempWidgetObj.getOtherTemp().getMinTempX()-19,tempWidgetObj.getOtherTemp().getMinTempY()-19,photoPaint);
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
					//?????? ?????? ????????? ?????????  ??????????????? ,????????????????????? ????????? ???????????????
					if ((featurePointsControl & 0x0f00) > 0) {//???????????????
						highTempToggle = true;
						bpRectF.left = temperature1[1] * (icon.getWidth() / (float) mSupportWidth) - mBitmapRectSize / 2.0f;
						bpRectF.right = temperature1[1] * (icon.getWidth() / (float) mSupportWidth) + mBitmapRectSize / 2.0f;
						bpRectF.top = temperature1[2] * (icon.getHeight() / (float) (mSupportHeight)) - mBitmapRectSize / 2.0f;
						bpRectF.bottom = temperature1[2] * (icon.getHeight() / (float) (mSupportHeight)) + mBitmapRectSize / 2.0f;
						bitcanvas.drawBitmap(highTempBt, null, bpRectF, photoPaint);
						tempTextPaint.setColor(Color.RED);
						xy2DrawText(temperature1[1] * (icon.getWidth() / (float) mSupportWidth), temperature1[2] * (icon.getHeight() / (float) (mSupportHeight)), decimalFormat.format(TempConvertUtils.Celsius2Temp(temperature1[3], mMeasureTempContainerView.getTempSuffixMode())) + MeasureTempContainerView.tempSuffixList[mMeasureTempContainerView.getTempSuffixMode()], tempTextBgTextPaint, tempTextPaint, 0);
					} else {
						highTempToggle = false;
					}
					if ((featurePointsControl & 0x00f0) > 0) {//???????????????
						lowTempToggle = true;
						bpRectF.left = temperature1[4] * (icon.getWidth() / (float) mSupportWidth) - mBitmapRectSize / 2.0f;
						bpRectF.right = temperature1[4] * (icon.getWidth() / (float) mSupportWidth) + mBitmapRectSize / 2.0f;
						bpRectF.top = temperature1[5] * (icon.getHeight() / (float) (mSupportHeight)) - mBitmapRectSize / 2.0f;
						bpRectF.bottom = temperature1[5] * (icon.getHeight() / (float) (mSupportHeight)) + mBitmapRectSize / 2.0f;
						bitcanvas.drawBitmap(lowTempBt, null, bpRectF, photoPaint);
						tempTextPaint.setColor(Color.BLUE);

						xy2DrawText(temperature1[4] * (icon.getWidth() / (float) mSupportWidth), temperature1[5] * (icon.getHeight() / (float) (mSupportHeight)), decimalFormat.format(TempConvertUtils.Celsius2Temp(temperature1[6], mMeasureTempContainerView.getTempSuffixMode())) + MeasureTempContainerView.tempSuffixList[mMeasureTempContainerView.getTempSuffixMode()], tempTextBgTextPaint, tempTextPaint, 0);
					} else {
						lowTempToggle = false;
					}
					if ((featurePointsControl & 0x000f) > 0) {//???????????????
						bpRectF.left = (icon.getWidth() / 2.0f) - mBitmapRectSize / 2.0f;
						bpRectF.right = (icon.getWidth() / 2.0f) + mBitmapRectSize / 2.0f;
						bpRectF.top = (icon.getHeight() / 2.0f) - mBitmapRectSize / 2.0f;
						bpRectF.bottom = (icon.getHeight() / 2.0f) + mBitmapRectSize / 2.0f;
						bitcanvas.drawBitmap(centerTempBt, null, bpRectF, photoPaint);
						tempTextPaint.setColor(Color.YELLOW);
						xy2DrawText((icon.getWidth() / 2.0f), (icon.getHeight() / 2.0f), decimalFormat.format(TempConvertUtils.Celsius2Temp(temperature1[0], mMeasureTempContainerView.getTempSuffixMode())) + MeasureTempContainerView.tempSuffixList[mMeasureTempContainerView.getTempSuffixMode()], tempTextBgTextPaint, tempTextPaint, 0);
					}
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
				mDrawer.draw(mTexIds, mStMatrix, 0, icon);//?????????draw

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
			public final void run () {
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

			private final void init () {
				if (DEBUG)
					Log.v(TAG, "RenderThread#init:");
				// create EGLContext for this thread
				mEgl = EGLBase.createFrom(null, false, false);
				mEglSurface = mEgl.createFromSurface(mSurface);
				mEglSurface.makeCurrent();
				// create drawing object
				mDrawer = new GLDrawer2D1(true);
			}

			private final void release () {
				if (DEBUG)
					Log.v(TAG, "RenderThread#release:");
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
