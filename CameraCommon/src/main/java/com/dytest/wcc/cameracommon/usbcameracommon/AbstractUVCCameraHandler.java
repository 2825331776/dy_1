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

package com.dytest.wcc.cameracommon.usbcameracommon;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.RequiresApi;

import com.dytest.wcc.cameracommon.encoder.MediaAudioEncoder;
import com.dytest.wcc.cameracommon.encoder.MediaEncoder;
import com.dytest.wcc.cameracommon.encoder.MediaMuxerWrapper;
import com.dytest.wcc.cameracommon.encoder.MediaSurfaceEncoder;
import com.dytest.wcc.cameracommon.encoder.MediaVideoBufferEncoder;
import com.dytest.wcc.cameracommon.encoder.MediaVideoEncoder;
import com.dytest.wcc.cameracommon.utils.ByteUtil;
import com.dytest.wcc.cameracommon.widget.UVCCameraTextureView;
import com.dytest.wcc.common.BuildConfig;
import com.dytest.wcc.common.widget.dragView.MeasureTempContainerView;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.ITemperatureCallback;
import com.serenegiant.usb.IUVCStatusCallBack;
import com.serenegiant.usb.IUpdateMedia;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


public abstract class AbstractUVCCameraHandler extends Handler {
	private static final boolean                     DEBUG                       = true;    // TODO set false on release
	private static final String                      TAG                         = "AbsUVCCameraHandler";
	private static final int                         MSG_OPEN                    = 0;
	private static final int                         MSG_CLOSE                   = 1;
	private static final int                         MSG_PREVIEW_START           = 2;
	private static final int                         MSG_PREVIEW_STOP            = 3;
	private static final int                         MSG_CAPTURE_STILL           = 4;
	private static final int                         MSG_CAPTURE_START           = 5;
	private static final int                         MSG_CAPTURE_STOP            = 6;
	private static final int                         MSG_MEDIA_UPDATE            = 7;
	private static final int                         MSG_SET_PALETTEPATH         = 8;
	private static final int                         MSG_RELEASE                 = 9;
	private static final int                         MSG_TEMPERATURE_START       = 10;
	private static final int                         MSG_TEMPERATURE_STOP        = 11;
	private static final int                         MSG_ON_RECEIVE_TEMPERATURE  = 12;
	private static final int                         MSG_CHANGE_PALETTE          = 13;
	private static final int                         MSG_SET_TEMPRANGE           = 14;
	private static final int                         MSG_MAKE_REPORT             = 15;
	private static final int                         MSG_OPEN_SYS_CAMERA         = 16;
	private static final int                         MSG_CLOSE_SYS_CAMERA        = 17;
	private static final int                         MSG_SET_HIGHTHROW           = 18;
	private static final int                         MSG_SET_LOWTHROW            = 19;
	private static final int                         MSG_SET_HIGHPLAT            = 20;
	private static final int                         MSG_SET_LOWPLAT             = 21;
	private static final int                         MSG_SET_ORGSUBGSHIGH        = 22;
	private static final int                         MSG_SET_ORGSUBGSLOW         = 23;
	private static final int                         MSG_SET_SIGMAD              = 24;
	private static final int                         MSG_SET_SIGMAR              = 25;
	private static final int                         MSG_RELAYOUT                = 26;
	private static final int                         MSG_WATERMARK_ONOFF         = 27;
	private static final int                         MSG_SET_SHUTTERFIX          = 28;
	private static final int                         MSG_SET_DEV_DIRECT_TCP      = 29;
	private static final int                         MSG_SET_DEV_PALETTE         = 30;
	//added by wupei
	private static final int                         MSG_LA_WENKUAN              = 31;
	private static final int                         MSG_DIS_WENKUAN             = 32;
	private static final int                         MSG_AREA_CHECK              = 33;
	private static final int                         MSG_SET_AREA                = 34;
	private static final int                         MSG_SHOW_TEMP               = 35;
	//????????????
	private static final int                         MSG_FIXED_TEMP_STRIP        = 36;
	private static final int                         MSG_TINY_SAVE_CAMERA_PARAMS = 45;//??????TinyC???????????????
	private static final int                         MSG_SAVEDATA_FIVESECONDS    = 40;//??????????????????
	private static final int                         MSG_SAVE_PICTURE            = 50;//??????
	private static final int                         MSG_SET_VERIFY_SN           = 55;//??????????????????SN
	private final        WeakReference<CameraThread> mWeakThread;

	//	private static final int MSG_SWITCH_GAIN = 57;
	//    private static final int MSG_ISRECORDAUDIO = 37;//??????????????????????????? ??????????????????????????????????????????????????? ?????????????????????????????????
	private volatile boolean mReleased;
	private volatile boolean mClosed;

	protected AbstractUVCCameraHandler (final CameraThread thread) {
		Log.e(TAG, "============ ????????????");
		mWeakThread = new WeakReference<CameraThread>(thread);
	}

	public int getWidth () {
		final CameraThread thread = mWeakThread.get();
		return thread != null ? thread.getWidth() : 0;
	}

	public int getHeight () {
		final CameraThread thread = mWeakThread.get();
		return thread != null ? thread.getHeight() : 0;
	}

	public boolean isOpened () {
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.isCameraOpened();
	}

	//added by wupei,??????????????????mainactivity?????????????????????????????????
	public byte[] getTemperaturePara (int len) {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			return thread.mUVCCamera.getByteArrayTemperaturePara(len);
		} else {
			byte[] para = new byte[len];
			return para;
		}
	}

	/**
	 * add ?????????  ?????????????????? ?????? tinyc ???????????????
	 *
	 * @param arraysLength
	 * @return
	 */
	public byte[] getTinyCCameraParams (int arraysLength) {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			return thread.mUVCCamera.getTinyCCameraParams(arraysLength);
		} else {
			byte[] para = new byte[arraysLength];
			return para;
		}
	}

	//2022???3???24???16:10:35 ????????? sn???????????? ???????????????
	public boolean snRightIsPreviewing () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			return thread.mUVCCamera.snRightIsPreviewing();
		}
		return false;
	}

	//2022???3???24???16:10:35 ????????? sn???????????? ???????????????
	public void setRotateMatrix_180 (boolean isRotate_180) {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			thread.mUVCCamera.setRotateMatrix_180(isRotate_180);
		}
	}

	//2022???4???7???11:53:52 ????????? ????????????JNI??????
	public boolean javaSendJniOrder (int status) {
		if (BuildConfig.DEBUG)
			Log.e(TAG, "javaSendJniOrder: ");
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			return thread.mUVCCamera.javaSendJniOrder(status);
		}
		return false;
	}

	public int getHighThrow () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			//return thread.mUVCCamera.getHighThrow();
		}
		return 0;
	}

	public void setHighThrow (int inputHighThrow) {
		Message message = Message.obtain();
		message.what = MSG_SET_HIGHTHROW;
		message.arg1 = inputHighThrow;
		sendMessage(message);
	}

	public int getLowThrow () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			//return thread.mUVCCamera.getLowThrow();
		}
		return 0;
	}

	public void setLowThrow (int inputLowThrow) {
		Message message = Message.obtain();
		message.what = MSG_SET_LOWTHROW;
		message.arg1 = inputLowThrow;
		sendMessage(message);
	}

	public int getHighPlat () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			//return thread.mUVCCamera.getHighPlat();
		}
		return 0;
	}

	public void setHighPlat (int inputHighPlat) {
		Message message = Message.obtain();
		message.what = MSG_SET_HIGHPLAT;
		message.arg1 = inputHighPlat;
		sendMessage(message);
	}

	public int getLowPlat () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			//return thread.mUVCCamera.getLowPlat();
		}
		return 0;
	}

	public void setLowPlat (int inputLowPlat) {
		Message message = Message.obtain();
		message.what = MSG_SET_LOWPLAT;
		message.arg1 = inputLowPlat;
		sendMessage(message);
	}

	public int getOrgSubGsHigh () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			//return thread.mUVCCamera.getOrgSubGsHigh();
		}
		return 0;
	}

	public void setOrgSubGsHigh (int inputOrgSubGsHigh) {
		Message message = Message.obtain();
		message.what = MSG_SET_ORGSUBGSHIGH;
		message.arg1 = inputOrgSubGsHigh;
		sendMessage(message);
	}

	public int getOrgSubGsLow () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			//return thread.mUVCCamera.getOrgSubGsLow();
		}
		return 0;
	}

	public void setOrgSubGsLow (int inputOrgSubGsLow) {
		Message message = Message.obtain();
		message.what = MSG_SET_ORGSUBGSLOW;
		message.arg1 = inputOrgSubGsLow;
		sendMessage(message);
	}

	public float getSigmaD () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			//return thread.mUVCCamera.getSigmaD();
		}
		return 0;
	}

	public void setSigmaD (int inputSigmaD) {
		Message message = Message.obtain();
		message.what = MSG_SET_SIGMAD;
		message.arg1 = inputSigmaD;
		sendMessage(message);
	}

	public float getSigmaR () {
		final CameraThread thread = mWeakThread.get();
		if ((thread != null) && (thread.mUVCCamera) != null) {
			//return thread.mUVCCamera.getSigmaR();
		}
		return 0;
	}

	public void setSigmaR (int inputSigmaR) {
		Message message = Message.obtain();
		message.what = MSG_SET_SIGMAR;
		message.arg1 = inputSigmaR;
		sendMessage(message);
	}

	public boolean isPreviewing () {
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.isPreviewing();
	}

	public boolean isRecording () {
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.isRecording();
	}

	public boolean isTemperaturing () {
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.isTemperaturing();
	}

	public boolean isEqual (final UsbDevice device) {
		final CameraThread thread = mWeakThread.get();
		return (thread != null) && thread.isEqual(device);
	}

	protected boolean isCameraThread () {
		final CameraThread thread = mWeakThread.get();
		return thread != null && (thread.getId() == Thread.currentThread().getId());
	}

	public boolean isReleased () {
		final CameraThread thread = mWeakThread.get();
		return mReleased || (thread == null);
	}

	public boolean isClosed () {
		return mClosed;
	}

	protected void checkReleased () {
		if (isReleased()) {
			throw new IllegalStateException("already released");
		}
	}

	public void open (final USBMonitor.UsbControlBlock ctrlBlock) {
		checkReleased();
		sendMessage(obtainMessage(MSG_OPEN, ctrlBlock));
	}

	public void PreparePalette (String path, int type,String configName) {
		checkReleased();
		Message message = Message.obtain();
		message.what = MSG_SET_PALETTEPATH;
		message.obj = path +"/"+ configName;
		message.arg1 = type;
		sendMessage(message);

	}

	public void close () {
		if (mClosed != true){
			mClosed = true;
			if (DEBUG)
				Log.v(TAG, "AbsUVCCameraHandler???close:");
			if (isOpened()) {
				sendEmptyMessage(MSG_CLOSE);
			}
		}
	}

	public void resize (final int width, final int height) {
		checkReleased();
		throw new UnsupportedOperationException("does not support now");
	}

	//?????????????????????????????????
	public void setRecordData (String externalPath) {
		Message message = Message.obtain();
		message.obj = externalPath;
		//        message.arg1 = seconds;
		message.what = MSG_SAVEDATA_FIVESECONDS;
		sendMessage(message);
	}

	/**
	 * ??????
	 *
	 * @param picPath ?????????????????????
	 */
	public void setSavePicture (String picPath) {
		Message message = Message.obtain();
		message.obj = picPath;
		message.what = MSG_SAVE_PICTURE;
		sendMessage(message);
	}

	protected void startPreview (final Object surface) {
		mClosed = false;
		checkReleased();
		//	if (!((surface instanceof SurfaceHolder) || (surface instanceof Surface) || (surface instanceof SurfaceTexture))) {
		//		throw new IllegalArgumentException("surface should be one of SurfaceHolder, Surface or SurfaceTexture");
		//	}
		sendMessage(obtainMessage(MSG_PREVIEW_START, surface));
	}

	public void stopPreview () {
		if (DEBUG)
			Log.v(TAG, "stopPreview:");
		removeMessages(MSG_PREVIEW_START);
		stopRecording();
		if (isPreviewing()) {
			final CameraThread thread = mWeakThread.get();
			if (thread == null)
				return;
			synchronized (thread.mSync) {
				sendEmptyMessage(MSG_PREVIEW_STOP);
				if (!isCameraThread()) {
					// wait for actually preview stopped to avoid releasing Surface/SurfaceTexture
					// while preview is still running.
					// therefore this method will take a time to execute
					try {
						thread.mSync.wait();
					} catch (final InterruptedException e) {
					}
				}
			}
		}
		if (DEBUG)
			Log.v(TAG, "stopPreview:finished");
	}

	protected void captureStill () {
		checkReleased();
		sendEmptyMessage(MSG_CAPTURE_STILL);
	}

	protected boolean captureStill (final String path) {
		//        checkReleased();
		if (isReleased())
			return false;
		sendMessage(obtainMessage(MSG_CAPTURE_STILL, path));
		return true;
	}

	public void makeReport () {
		checkReleased();
		sendEmptyMessage(MSG_MAKE_REPORT);
	}

	public void setAreaCheck (int isAreaCheck) {
		Message message = Message.obtain();
		message.what = MSG_AREA_CHECK;
		message.arg1 = isAreaCheck;
		sendMessage(message);
	}

	public void setArea (int[] areaData) {
		Message message = Message.obtain();
		message.what = MSG_SET_AREA;
		message.obj = areaData;
		sendMessage(message);
	}

	public void startRecording (int isRecordAudio) {//0 ???????????????????????????1????????????????????????
		checkReleased();
		Message msg = Message.obtain();
		msg.what = MSG_CAPTURE_START;
		msg.obj = isRecordAudio;
		sendMessage(msg);
	}

	public void stopRecording () {
		sendEmptyMessage(MSG_CAPTURE_STOP);
	}

	public void startTemperaturing () {
		checkReleased();
		sendEmptyMessage(MSG_TEMPERATURE_START);
	}

	public void setVerifySn () {
		checkReleased();
		sendEmptyMessage(MSG_SET_VERIFY_SN);
	}

	// ???Tcp????????????
	public void SetDevMsg (int msgCode) {
		Message message = Message.obtain();
		message.what = MSG_SET_DEV_DIRECT_TCP;
		message.arg1 = msgCode;
		sendMessage(message);
	}

	public void SetDevPalette (int paletteCode) {
		Message message = Message.obtain();
		message.what = MSG_SET_DEV_PALETTE;
		message.arg1 = paletteCode;
		sendMessage(message);
	}

	public void setTempRange (int range) {
		Message message = Message.obtain();
		message.what = MSG_SET_TEMPRANGE;
		message.arg1 = range;
		sendMessage(message);
	}

	public void setShutterFix (float mShutterFix) {
		Message message = Message.obtain();
		message.what = MSG_SET_SHUTTERFIX;
		message.obj = mShutterFix;
		sendMessage(message);
	}

	public void relayout (int rotate) {
		Message message = Message.obtain();
		message.what = MSG_RELAYOUT;
		message.arg1 = rotate;
		sendMessage(message);
	}

	public void tempShowOnOff (int isTempShow) {
		Message message = Message.obtain();
		message.what = MSG_SHOW_TEMP;
		message.arg1 = isTempShow;
		sendMessage(message);
	}

	public void stopTemperaturing () {
		sendEmptyMessage(MSG_TEMPERATURE_STOP);
	}

	public void changePalette (int typeOfPalette) {
		Message message = Message.obtain();
		message.what = MSG_CHANGE_PALETTE;
		message.arg1 = typeOfPalette;
		sendMessage(message);
	}

	/**
	 * SeekBar ????????? ?????????????????????
	 *
	 * @param maxPercent ?????????????????? ?????????
	 * @param minPercent ?????????????????? ?????????
	 * @param maxValue   ?????????????????? ??????????????????????????????
	 * @param minValue   ?????????????????? ??????????????????????????????
	 */
	public void seeKBarRangeSlided (float maxPercent, float minPercent, float maxValue, float minValue) {
		Log.e(TAG, "seeKBarRangeSlided: ");
		Message message = Message.obtain();
		message.what = MSG_LA_WENKUAN;
		message.obj = new float[]{maxPercent, minPercent, maxValue, minValue};
		sendMessage(message);
	}

	//added by wupei
	public void disWenKuan () {
		checkReleased();
		sendEmptyMessage(MSG_DIS_WENKUAN);
	}

	//    public boolean checkSnRight(){
	//
	//    }
	//???????????????
	public void fixedTempStripChange (boolean state) {
		//        checkReleased();
		Message message = Message.obtain();
		message.what = MSG_FIXED_TEMP_STRIP;
		message.obj = state;
		sendMessage(message);
	}

	//??????TinyC ???????????? ??????
	public void tinySaveCameraParams () {
		//		Message message = Message.obtain();
		sendEmptyMessage(MSG_TINY_SAVE_CAMERA_PARAMS);
	}

	public void openSystemCamera () {
		sendEmptyMessage(MSG_OPEN_SYS_CAMERA);
	}

	public void closeSystemCamera () {
		sendEmptyMessage(MSG_CLOSE_SYS_CAMERA);
	}

	public void release () {
		mReleased = true;
		close();
		mClosed = true;
		sendEmptyMessage(MSG_RELEASE);
	}

	public void addCallback (final CameraCallback callback) {
		checkReleased();
		if (!mReleased && (callback != null)) {
			final CameraThread thread = mWeakThread.get();
			if (thread != null) {
				thread.mCallbacks.add(callback);
			}
		}
	}

	public void removeCallback (final CameraCallback callback) {
		if (callback != null) {
			final CameraThread thread = mWeakThread.get();
			if (thread != null) {
				thread.mCallbacks.remove(callback);
			}
		}
	}

	protected void updateMedia (final String path) {
		sendMessage(obtainMessage(MSG_MEDIA_UPDATE, path));
	}

	public boolean checkSupportFlag (final long flag) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.mUVCCamera != null && thread.mUVCCamera.checkSupportFlag(flag);
	}

	public int getValue (final int flag) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
		if (camera != null) {
			if (flag == UVCCamera.PU_BRIGHTNESS) {
				return camera.getBrightness();
			} else if (flag == UVCCamera.PU_CONTRAST) {
				return camera.getContrast();
			}
		}
		throw new IllegalStateException();
	}

	public int setValue (final int flag, final int value) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			if (flag == UVCCamera.PU_BRIGHTNESS) {
				camera.setBrightness(value);
				return camera.getBrightness();
			} else if (flag == UVCCamera.PU_CONTRAST) {
				camera.setContrast(value);
				return camera.getContrast();
			} else if (flag == UVCCamera.CTRL_ZOOM_ABS) {
				return camera.setZoom(value);
				//                return 1;
			}
		}
		return -10;
	}

	public void testJNi (String str) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			camera.testJNi(str);
		} else {
			Log.e(TAG, "testJNi: CameraThread UVCCamera is null!!==========");
		}
	}

	/**
	 * ???????????????????????? ????????????
	 *
	 * @param flag
	 * @param value
	 * @param mark
	 * @return
	 */
	public byte[] getMachineSettingArray (final int flag, final int value, final int mark, int len) {
		byte[] result = null;
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			if (flag == UVCCamera.CTRL_ZOOM_ABS) {

			} else if (flag == UVCCamera.CTRL_AE) {

			}
			if (result == null) {
				result = new byte[len];
			}
		}
		return result;
	}

	/**
	 * ???????????????????????? ????????????
	 *
	 * @param flag
	 * @param value
	 * @param mark
	 * @return
	 */
	public float getMachineSetting (final int flag, final int value, final int mark) {
		float result = 0;
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			if (flag == UVCCamera.CTRL_ZOOM_ABS) {
				result = camera.getMachineSetting(flag, value, mark);
			}
		}
		return result;
	}

	/**
	 * ??????????????????
	 *
	 * @param flag
	 * @param value
	 * @param mark
	 * @return
	 */
	public boolean setMachineSetting (final int flag, final int value, final int mark) {
		boolean result = false;
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			if (flag == UVCCamera.CTRL_ZOOM_ABS) {
				result = camera.setMachineSetting(flag, value, mark);
			}
		}
		return result;
	}

	/**
	 * ???????????? ??? ??????????????? ???????????? Int ???????????????????????????float ??????????????? ??????
	 *
	 * @param flag
	 * @param value
	 * @param mark  ????????? ??? ????????????????????? ?????????????????????????????????????????? ????????????
	 * @return
	 */
	public int sendOrder (final int flag, final float value, final int mark) {
		checkReleased();
		int result = -1;
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			if (flag == UVCCamera.CTRL_ZOOM_ABS) {
				result = camera.sendOrder(value, mark);
				return result;
			}
		}
		return result;
	}

	public void whenShutRefresh () {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			camera.whenShutRefresh();
		}

	}

	public void whenChangeTempPara () {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			camera.whenChangeTempPara();
		}
	}

	//added by wupei to change palette
	public int setPalette (int typeOfPalette) {
		checkReleased();
		int result = 1;
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = (thread != null ? thread.mUVCCamera : null);
		if (camera != null) {
			camera.changePalette(typeOfPalette);
			result = 0;
		}
		return result;
	}

	public int resetValue (final int flag) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
		if (camera != null) {
			if (flag == UVCCamera.PU_BRIGHTNESS) {
				camera.resetBrightness();
				return camera.getBrightness();
			} else if (flag == UVCCamera.PU_CONTRAST) {
				camera.resetContrast();
				return camera.getContrast();
			}
		}
		throw new IllegalStateException();
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	@Override
	public void handleMessage (final Message msg) {
		final CameraThread thread = mWeakThread.get();//mWeakThead???UVCCameraHandler.CreateHandler????????????
		if (thread == null)
			return;
		switch (msg.what) {
			case MSG_OPEN:
				thread.handleOpen((USBMonitor.UsbControlBlock) msg.obj);
				break;
			case MSG_CLOSE:
				thread.handleClose();
				break;
			case MSG_PREVIEW_START:
				//                Log.e(TAG, "handleStartPreview :MSG_PREVIEW_START");
				thread.handleStartPreview(msg.obj);
				break;
			case MSG_PREVIEW_STOP:
				thread.handleStopPreview();
				break;
			case MSG_CAPTURE_STILL:
				thread.handleCaptureStill((String) msg.obj);
				break;
			case MSG_MAKE_REPORT:
				thread.handleMakeReport();
				break;
			case MSG_AREA_CHECK:
				thread.handleSetAreaCheck(msg.arg1);
				break;
			case MSG_SET_AREA:
				thread.handleSetArea(msg.obj);
				break;
			case MSG_SET_PALETTEPATH:
				thread.handlePreparePalette((String) msg.obj, msg.arg1);
				break;
			case MSG_CAPTURE_START:
				int value = (int) msg.obj;
				boolean s = (value == 0);//??????0??? ??????????????????????????? ??????????????????
				thread.handleStartRecording(s);
				break;
			case MSG_CAPTURE_STOP:
				thread.handleStopRecording();
				break;
			case MSG_TEMPERATURE_START:
				thread.handleStartTemperaturing();
				break;
			case MSG_SET_VERIFY_SN:
				thread.setVerifySn();
				break;
			//			case MSG_SWITCH_GAIN:
			//				thread.setMachineSetting
			//				break;
			case MSG_SET_TEMPRANGE:
				int range = msg.arg1;
				thread.handleSetTempRange(range);
				break;
			case MSG_SET_SHUTTERFIX:
				float mShutterFix = (float) msg.obj;
				thread.handleSetShutterFix(mShutterFix);
				break;
			case MSG_RELAYOUT:
				int rotate = msg.arg1;
				thread.handleRelayout(rotate);
				break;
			//            case MSG_WATERMARK_ONOFF:
			//                boolean isWatermaker;
			//                isWatermaker = (msg.arg1 > 0);
			//                thread.handleWatermarkOnOff(isWatermaker);
			//                break;
			case MSG_SHOW_TEMP:
				boolean isTempShow;
				isTempShow = (msg.arg1 > 0);
				thread.handleTempShowOnOff(isTempShow);
				break;
			case MSG_SET_HIGHTHROW:
				int inputHighThrow = msg.arg1;
				thread.handleSetHighThrow(inputHighThrow);
				break;
			case MSG_SET_LOWTHROW:
				int inputLowThrow = msg.arg1;
				thread.handleSetLowThrow(inputLowThrow);
				break;
			case MSG_SET_HIGHPLAT:
				int inputHighPlat = msg.arg1;
				thread.handleSetHighPlat(inputHighPlat);
				break;
			case MSG_SET_LOWPLAT:
				int inputLowPlat = msg.arg1;
				thread.handleSetLowPlat(inputLowPlat);
				break;
			case MSG_SET_ORGSUBGSHIGH:
				int inputOrgSubGsHigh = msg.arg1;
				thread.handleSetOrgSubGsHigh(inputOrgSubGsHigh);
				break;
			case MSG_SET_ORGSUBGSLOW:
				int inputOrgSubGsLow = msg.arg1;
				thread.handleSetOrgSubGsLow(inputOrgSubGsLow);
				break;
			case MSG_SET_SIGMAD:
				int sigmaD = msg.arg1;
				float inputSigmaD = sigmaD / 10.0f;
				thread.handleSetSigmaD(inputSigmaD);
				break;
			case MSG_SET_SIGMAR:
				int sigmaR = msg.arg1;
				float inputSigmaR = sigmaR / 10.0f;
				thread.handleSetSigmaR(inputSigmaR);
				break;

			case MSG_TEMPERATURE_STOP:
				thread.handleStopTemperaturing();
				break;
			case MSG_MEDIA_UPDATE:
				thread.handleUpdateMedia((String) msg.obj);

				break;
			case MSG_RELEASE:
				thread.handleRelease();
				break;
			case MSG_CHANGE_PALETTE:
				int typeOfPalette = msg.arg1;
				thread.handleChangePalette(typeOfPalette);
				break;
			case MSG_LA_WENKUAN:
				float[] result = (float[]) msg.obj;
				float maxPercent = result[0];
				float minPercent = result[1];
				float maxValue = result[2];
				float minValue = result[3];
				thread.handleLaWenKuan(maxPercent, minPercent, maxValue, minValue);
				break;
			case MSG_DIS_WENKUAN:
				thread.handleDisWenKuan();
				break;
			case MSG_FIXED_TEMP_STRIP://???????????????
				boolean state = (boolean) msg.obj;
				thread.handleFixedTempStrip(state);
				break;

			case MSG_TINY_SAVE_CAMERA_PARAMS:
				thread.handleTinySaveCameraParams();
				break;
			case MSG_OPEN_SYS_CAMERA:
				thread.handleOpenSysCamera();
				break;
			case MSG_CLOSE_SYS_CAMERA:
				thread.handleCloseSysCamera();
				break;
			//            case MSG_SET_DEV_DIRECT_TCP:
			//                // tcp??????
			//                int msgCode = msg.arg1;
			//                thread.handleSetDevMsg(msgCode);
			//                break;
			case MSG_SET_DEV_PALETTE:
				// ????????????
				int palette = msg.arg1;
				thread.handleSetPalette(palette);
				break;
			//?????????????????????????????????
			case MSG_SAVEDATA_FIVESECONDS:
				String path = msg.obj.toString();
				//                int seconds = msg.arg1;
				thread.handleSaveFiveSeconds(path);
				break;
			case MSG_SAVE_PICTURE://??????
				String picPath = msg.obj.toString();
				thread.handleSavePicture(picPath);
				break;
			//            case MSG_ISRECORDAUDIO://??????????????????
			//                boolean isRecordAudio = (boolean) msg.obj;
			//                thread.handleIsRecordAudio(isRecordAudio);
			//                break;

			default:
				throw new RuntimeException("unsupported message:what=" + msg.what);
		}
	}

	public interface CameraCallback {
		public void onOpen ();

		public void onClose ();

		public void onStartPreview ();

		public void onStopPreview ();

		public void onStartRecording ();

		public void onStopRecording ();

		public void onError (final Exception e);

		//  add  ????????? 2022???4???20???17:18:06
		public void onSavePicFinished (boolean isFinish, String picPath);
	}

	static final class CameraThread extends Thread {
		private static final String                                    TAG_THREAD = "CameraThread";
		private final        Object                                    mSync      = new Object();
		private final        Class<? extends AbstractUVCCameraHandler> mHandlerClass;
		private final        WeakReference<Activity>                   mWeakParent;
		private final        WeakReference<UVCCameraTextureView>       mWeakCameraView;
		private final        int                                       mEncoderType;
		private final        Set<CameraCallback>                       mCallbacks = new CopyOnWriteArraySet<CameraCallback>();
		//        private boolean mIsRecordAudio = false;//??????????????????
		public               ITemperatureCallback                      CameraThreadTemperatureCallback;
		MediaScannerConnection.OnScanCompletedListener ScanCompletedListener = new MediaScannerConnection.OnScanCompletedListener() {
			@Override
			public void onScanCompleted (String path, Uri uri) {
					/*	final Activity parent = mWeakParent.get();
						final boolean released = (mHandler == null) || mHandler.mReleased;
						if (parent != null && parent.getApplicationContext() != null) {
							if (released || parent.isDestroyed()) {
								handleRelease();
							}
							if(mIsTemperaturing) {
								String[] SplitArray=path.split("\\.");
								String NewPath = SplitArray[0]+"IR.png";
								try {
									PngUtil.wirteByteArrayToPng(path, ByteTemperatureData, NewPath);
										try {
											MediaScannerConnection.scanFile(parent.getApplicationContext(), new String[]{ NewPath }, null, null);
										} catch (final Exception e) {
										}
								} catch (final Exception e) {
									Log.e(TAG, "handleUpdateMedia wirteByteArrayToPng:", e);
								}
								File OldPhoto=new File(path);
								if(OldPhoto.isFile() && OldPhoto.exists()) {
									Boolean succeedDelete = OldPhoto.delete();
									if(succeedDelete){
										try {
											MediaScannerConnection.scanFile(parent.getApplicationContext(), new String[]{ path }, null, null);
										} catch (final Exception e) {
										}
									}

								}
							}
						} else {
							Log.w(TAG, "MainActivity already destroyed");
							// give up to add this movie to MediaStore now.
							// Seeing this movie on Gallery app etc. will take a lot of time.
							handleRelease();
						}*/
			}
		};
		private int mWidth, mHeight, mPreviewMode, mPalettetype;//mWidth mHeight ????????????????????????
		private float                                   mBandwidthFactor;
		private int                                     currentAndroidVersion;
		private boolean                                 mIsPreviewing;    // ???????????????????????????
		private boolean                                 mIsTemperaturing;    // ????????????????????????
		//        private boolean mIsCapturing;    // ????????????????????????
		private boolean                                 mIsRecording;    // ??????????????????
		/**
		 * shutter sound
		 */
		//        private SoundPool            mSoundPool;
		private int                                     mSoundId;
		private AbstractUVCCameraHandler                mHandler;
		/**
		 * for accessing UVC camera
		 */
		private UVCCamera                               mUVCCamera;
		/**
		 * ????????? MeasureTempContainerView
		 */
		private WeakReference<MeasureTempContainerView> mContainerView;
		/**
		 * muxer for audio/video recording
		 */
		private MediaMuxerWrapper                       mMuxer;

		/************* wifi???????????? ************/
		private       MediaVideoBufferEncoder mVideoEncoder;
		//        private byte[] FrameData = new byte[640 * 512 * 4];
		private final IFrameCallback          mIFrameCallback = new IFrameCallback() {
			@Override
			public void onFrame (final ByteBuffer frameData) {
				//Log.e(TAG, "mIFrameCallback ");
				Log.e(TAG, "the frame frameData.capacity ==== " + frameData.capacity());//196608= 256*192*4 RGBA????????????;
				final MediaVideoBufferEncoder videoEncoder;
				synchronized (mSync) {
					videoEncoder = mVideoEncoder;
				}
				if (videoEncoder != null) {
					videoEncoder.frameAvailableSoon();
					videoEncoder.encode(frameData);
				}
				//                if (frameData != null){
				//
				////                    frameData.get(FrameData, 0, frameData.capacity());
				//
				//                    final MediaVideoBufferEncoder videoEncoder;
				//                    synchronized (mSync) {
				//                        videoEncoder = mVideoEncoder;
				//                    }
				//                    if (videoEncoder != null) {
				//                        videoEncoder.frameAvailableSoon();
				//                        videoEncoder.encode(frameData);
				//                    }
				//                }

				//Log.e(TAG, "mIFrameCallback frameData[384*288*4/2]:"+ (int)FrameData[384*288*4/2]);
				//Log.e(TAG, "mIFrameCallback frameData[384*288*4/2]:"+ (int)FrameData[384*288*4/2]);
			}
		};

		/************************/

		//        private float[] temperatureData = new float[640 * 512 + 10];
		/**
		 * tpcCamera
		 **/
		//        private TcpClient mTcpClient;
		private       int                               cameraType            = 1; // 1???uvc,2???tcp
		private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
			@Override
			public void onPrepared (final MediaEncoder encoder) {
				if (DEBUG)
					Log.e(TAG, "onPrepared:encoder=" + encoder);
				Log.e(TAG, "onPrepared: mIsRecording:" + mIsRecording);
				mIsRecording = true;
				if (encoder instanceof MediaVideoEncoder)
					try {
						mWeakCameraView.get().setVideoEncoder((MediaVideoEncoder) encoder);
					} catch (final Exception e) {
						Log.e(TAG, "onPrepared:", e);
					}
				/*if (encoder instanceof MediaSurfaceEncoder)
					try {
						mWeakCameraView.get().setVideoEncoder((MediaSurfaceEncoder)encoder);
						mUVCCamera.startCapture(((MediaSurfaceEncoder)encoder).getInputSurface());
					} catch (final Exception e) {
						Log.e(TAG, "onPrepared:", e);
					}*/
			}

			@Override
			public void onStopped (final MediaEncoder encoder) {
				if (DEBUG)
					Log.e(TAG_THREAD, "onStopped:encoder=" + encoder);
				if ((encoder instanceof MediaVideoEncoder) || (encoder instanceof MediaSurfaceEncoder))
					try {
						mIsRecording = false;
						final Activity parent = mWeakParent.get();
						mWeakCameraView.get().setVideoEncoder(null);
						//                        synchronized (mSync) {
						//                            if (mUVCCamera != null) {
						//                                Log.e(TAG, "onStopped:stopCapture ");
						//                                mUVCCamera.stopCapture();
						//                            }
						//                        }
						final String path = encoder.getOutputPath();
						if (!TextUtils.isEmpty(path)) {
							mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_MEDIA_UPDATE, path), 200);
						} else {
							final boolean released = (mHandler == null) || mHandler.mReleased;
							if (released || parent == null || parent.isDestroyed()) {
								handleRelease();
							}
						}
					} catch (final Exception e) {
						Log.e(TAG, "onPrepared:", e);
					}
			}
		};
		private       int                               mVid, mPid;
		//        private byte[] ByteTemperatureData = new byte[(640 * 512 + 10) * 4];
		//        private short[] ShortTemperatureData = new short[640 * 512 + 10];
		private Handler mMySubHandler;
		/**
		 * UVC????????????
		 */
		private int     UVCStatus         = -1;
		private int     autoSaveCount     = 0;
		private boolean needSendSaveOrder = false;
		public final IUVCStatusCallBack iuvcStatusCallBack = new IUVCStatusCallBack() {
			@Override
			public void onUVCCurrentStatus (int uvcStatus) {
//				Log.e(TAG, "onUVCCurrentStatus:  ======??????=====??????????????????=========111111111111111111==uvcStatus===>"+uvcStatus );
				if (uvcStatus != UVCStatus) {
					UVCStatus = uvcStatus;
				} else {
					if (autoSaveCount <= 80 && UVCStatus == 3 && !needSendSaveOrder) {
						autoSaveCount++;
					} else if (autoSaveCount > 80 && UVCStatus == 3 && !needSendSaveOrder) {
						if (mUVCCamera!=null){
							needSendSaveOrder = true;
							Log.e(TAG, "onUVCCurrentStatus:  ==================??????????????????================");
							mHandler.postDelayed(() -> {
								if (mUVCCamera != null) {
									mUVCCamera.setZoom(0x8000);
								}
							}, 200);
							mHandler.postDelayed(() -> {
								if (mUVCCamera != null){
									mUVCCamera.whenShutRefresh();
								}
							}, 500);
							mUVCCamera.TinySaveCameraParams();
						}
					}
				}
			}

//			@Override
//			public void onUpdateMedia (String path) {
//				MediaScannerConnection.scanFile(mWeakParent.get().getApplicationContext(), new String[]{path}, null, ScanCompletedListener);
//			}
		};

		/**
		 * @param clazz               Class extends AbstractUVCCameraHandler
		 * @param parent              parent Activity
		 * @param cameraView          for still capturing
		 * @param encoderType         0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
		 * @param width
		 * @param height
		 * @param format              either FRAME_FORMAT_YUYV(0) or FRAME_FORMAT_MJPEG(1)
		 * @param bandwidthFactor
		 * @param temperatureCallback ??????????????????
		 * @param androidVersion      ???????????????Camera
		 */
		CameraThread (final Class<? extends AbstractUVCCameraHandler> clazz, final Activity parent, final UVCCameraTextureView cameraView, final int encoderType, final int width, final int height, final int format, final float bandwidthFactor, ITemperatureCallback temperatureCallback, final MeasureTempContainerView containerview, int androidVersion) {

			super("CameraThread");
			//by wp
			//Log.e(TAG, "CameraThread: ================start");
			mHandlerClass = clazz;
			mEncoderType = encoderType;
			Log.e(TAG, "??????????????? mEncodertype======================");
			//            mEncoderType=2;
			mWidth = width;//??????????????????
			mHeight = height;
			mVid = 0;
			mPid = 0;

			System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
			System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
			System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");
			CameraThreadTemperatureCallback = temperatureCallback;
			currentAndroidVersion = androidVersion;
			mPreviewMode = format;
			mBandwidthFactor = bandwidthFactor;
			mWeakParent = new WeakReference<Activity>(parent);
			mWeakCameraView = new WeakReference<UVCCameraTextureView>(cameraView);
			//            loadShutterSound(parent);
			mContainerView = new WeakReference<>(containerview);
			Log.e(TAG, "CameraThread: ================end");
		}

		@Override
		protected void finalize () throws Throwable {
			Log.i(TAG, "CameraThread#finalize");
			super.finalize();
		}

		public AbstractUVCCameraHandler getHandler () {
			if (DEBUG)
				Log.e(TAG_THREAD, "getHandler:");
			synchronized (mSync) {
				if (mHandler == null)
					try {
						mSync.wait();
					} catch (final InterruptedException e) {
					}
			}
			return mHandler;
		}

		public int getWidth () {
			synchronized (mSync) {
				return mWidth;
			}
		}

		public int getHeight () {
			synchronized (mSync) {
				return mHeight;
			}
		}

		public boolean isCameraOpened () {
			synchronized (mSync) {
				//                if (cameraType == 1) {
				return mUVCCamera != null;
				//                }
				//                else {
				//                    return mTcpClient != null;
				//                }
			}
		}

		public boolean isTemperaturing () {
			synchronized (mSync) {
				//                if (cameraType == 1) {
				return mUVCCamera != null && mIsTemperaturing;
				//                }
				//                else {
				//                    return mTcpClient != null && mIsTemperaturing;
				//                }
			}
		}

		public boolean isPreviewing () {
			synchronized (mSync) {
				//                if (cameraType == 1) {
				return mUVCCamera != null && mIsPreviewing;
				//                }
				//                else {
				//                    return mTcpClient != null && mIsPreviewing;
				//                }
			}
		}

		public boolean isRecording () {
			synchronized (mSync) {
				//                if (cameraType == 1) {
				return (mUVCCamera != null) && (mMuxer != null);
				//                }
				//                else {
				//                    return (mTcpClient != null) && (mMuxer != null);
				//                }
			}
		}

		public boolean isEqual (final UsbDevice device) {
			return (mUVCCamera != null) && (mUVCCamera.getDevice() != null) && mUVCCamera.getDevice().equals(device);
		}

		public void handleOpen (final USBMonitor.UsbControlBlock ctrlBlock) {
			if (DEBUG)
				Log.v(TAG_THREAD, "handleOpen:");
			if (cameraType == 1) {
				try {
					final UVCCamera camera;
					camera = new UVCCamera(currentAndroidVersion);
					camera.open(ctrlBlock);
					mVid = ctrlBlock.getVenderId();
					mPid = ctrlBlock.getProductId();
					//                    Log.e(TAG, "handleOpen:================= + ctrlBlock Vid ====  " + ctrlBlock.getVenderId() + " ==== pid ===" + ctrlBlock.getProductId());
					synchronized (mSync) {
						mUVCCamera = camera;
					}
					callOnOpen();
				} catch (final Exception e) {
					callOnError(e);
				}

				if (mUVCCamera != null) {
					String mSupportedSize = mUVCCamera.getSupportedSize();
					if (DEBUG)
						Log.e(TAG, "==================mSupportedSize==============" + mSupportedSize);

					int find_str_postion = mSupportedSize.indexOf("384x292");
					//					Log.e(TAG, "handleOpen: find_str_postion" +find_str_postion);
					if (find_str_postion >= 0) {
						mWidth = 384;
						mHeight = 292;
						if (DEBUG)
							Log.e(TAG, "handleOpen: 384*292 DEVICE ");
					}

					find_str_postion = mSupportedSize.indexOf("256x196");
					//					Log.e(TAG, "handleOpen: find_str_postion" +find_str_postion);
					if (find_str_postion >= 0) {
						mWidth = 256;
						mHeight = 196;
						if (DEBUG)
							Log.e(TAG, "handleOpen: 256*196 DEVICE ");
					}
					find_str_postion = mSupportedSize.indexOf("256x192");
					//					Log.e(TAG, "handleOpen: find_str_postion" +find_str_postion);
					if (find_str_postion >= 0) {
						mWidth = 256;
						mHeight = 192;
						if (DEBUG)
							Log.e(TAG, "handleOpen: 256*192 DEVICE ");
					}
					find_str_postion = mSupportedSize.indexOf("160x120");
					//					Log.e(TAG, "handleOpen: find_str_postion" +find_str_postion);
					if (find_str_postion >= 0) {
						mWidth = 160;
						mHeight = 120;
						if (DEBUG)
							Log.e(TAG, "handleOpen: 160*120 DEVICE ");
					}
				}
			}
		}

		public void handleClose () {
			if (cameraType == 1) {
				//if (DEBUG)
				Log.e(TAG_THREAD, "camerathread - handleClose:");
				//handleStopTemperaturing();
				//handleStopRecording();
				//                if (mIsCapturing) {           //2021-8-11 16:22:54  ???????????????
				//                    mIsCapturing = false;
				//                    Log.e(TAG, "handleClose: stopCapture");
				//                    mUVCCamera.stopCapture();
				//                }
				if (mIsRecording) {
					mIsRecording = false;
					handleStopRecording();
				}

				if (mIsTemperaturing) {
					mIsTemperaturing = false;
					handleStopTemperaturing();
				}
				//mIsTemperaturing disable by wupei

				//                if(mIsTemperaturing){
				//                    mIsTemperaturing=false;
				//                    handleStopTemperaturing();
				//                }
				//                synchronized (mSync){
				//                    mUVCCamera.stopPreview();
				//                    mIsPreviewing=false;
				//                    mUVCCamera.destroy();
				//                    callOnClose();
				//                    mUVCCamera=null;
				//                }

				final UVCCamera camera;
				synchronized (mSync) {
					camera = mUVCCamera;
					mUVCCamera = null;
				}
				if (camera != null) {
					//					camera.stopPreview();
					mIsPreviewing = false;
					camera.destroy();
					callOnClose();
				}
			}
			//            else {
			//                if (mTcpClient != null) {
			//                    mTcpClient.nativeCloseTcp();
			//                    mTcpClient = null;
			//                }
			//                mIsPreviewing = false;
			//
			//            }

		}
		public final IUpdateMedia iUpdateMedia = new IUpdateMedia() {
			@Override
			public void onUpdateMedia (String path) {
				Log.e(TAG, "onUpdateMedia: =========================================");
				MediaScannerConnection.scanFile(mWeakParent.get().getApplicationContext(), new String[]{path}, null, ScanCompletedListener);
			}
		};


		public void handleStartPreview (final Object surface) {
			//            Log.e(TAG, "handleStartPreview:mUVCCamera" + mUVCCamera + " mIsPreviewing:" + mIsPreviewing);
			if (DEBUG)
				Log.v(TAG_THREAD, "handleStartPreview:");

			if (cameraType == 1) {
				if ((mUVCCamera == null) || mIsPreviewing)
					return;
				try {
					//??????????????? MeasureTempContainerView ,??????????????? mFrameWidth ??? mFrameHeight
					if (mContainerView.get() != null) {
						mContainerView.get().setFrameWH(mWidth, mHeight);
					}
//					mUVCCamera.setUVCStatusCallBack(iuvcStatusCallBack);
					mUVCCamera.setPreviewSize(mWidth, mHeight, 1, 25, mPreviewMode, mBandwidthFactor, currentAndroidVersion);
					//Log.e(TAG, "handleStartPreview3 mWidth: " + mWidth + "mHeight:" + mHeight);
				} catch (final IllegalArgumentException e) {
					callOnError(e);
					return;
				}

				if (surface instanceof SurfaceHolder) {
					Log.e(TAG, "================SurfaceHolder:");
					mUVCCamera.setPreviewDisplay((SurfaceHolder) surface);
				} else if (surface instanceof Surface) {
					Log.e(TAG, "=================Surface:");
					mUVCCamera.setPreviewDisplay((Surface) surface);
				} else if (surface instanceof SurfaceTexture) {
					Log.e(TAG, "================SurfaceTexture:");
					mUVCCamera.setPreviewTexture((SurfaceTexture) surface);
				}

//				mUVCCamera.setIUpdateMedia(iUpdateMedia);

				//Log.e(TAG, "handleStartPreview: startPreview1");
				mUVCCamera.startPreview();


				/*===========================================================================
				 * if need rgba callback
				 *set this setFrameCallback(...) function
				 *==========================================================================*/
				//2022???1???13???10:26:00  ??????  ??????????????????  ??????native ????????????????????????JNI???????????????????????? ?????????????????????
				//    			mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX);
				//                mIsCapturing=true;
				//    			mUVCCamera.startCapture();

				/*===========================================================================
				 * if need Temperature callback
				 *set this setTemperatureCallback(...) function
				 *==========================================================================*/
				if (mVid == 5396 && mPid == 1) {
					mWeakCameraView.get().setSuportWH(mWidth, mHeight - 4);
				} else if (mPid == 22592 && mVid == 3034) {
					mWeakCameraView.get().setSuportWH(mWidth, mHeight);
				}
				// ???????????? ??????
//				ITemperatureCallback mTempCb = mWeakCameraView.get().getTemperatureCallback();
//				mUVCCamera.setTemperatureCallback(mTempCb);//????????????????????????  ???????????????
				//				mWeakCameraView.get().setTemperatureCbing(false);//????????????

				mUVCCamera.updateCameraParams();
				synchronized (mSync) {
					mIsPreviewing = true;
				}
				callOnStartPreview();

			}
			//            else {
			//                Log.e(TAG, "cameraType ================= 2==" + mIsPreviewing);
			//                if ((mTcpClient == null) || mIsPreviewing) return;
			//                Log.e(TAG, "handleStartPreview======tcp ");
			//                try {
			//                    mWidth = 256;
			//                    mHeight = 196;
			//                    mTcpClient.nativeSetPreviewSize(mWidth, mHeight, 1, 26, mPreviewMode, mBandwidthFactor, currentAndroidVersion);
			//                    Log.e(TAG, "handleStartPreview3 mWidth: " + mWidth + "mHeight:" + mHeight);
			//                } catch (final IllegalArgumentException e) {
			//                    callOnError(e);
			//                    return;
			//                }
			//
			//
			//                if (surface instanceof SurfaceHolder) {
			//                    Log.e(TAG, "SurfaceHolder:");
			//                    Log.e(TAG, "tcp11111111111111111111321");
			//                    mUVCCamera.setPreviewDisplay((SurfaceHolder) surface);
			//                } else if (surface instanceof Surface) {
			//                    mTcpClient.nativeSetPreviewDisplay((Surface) surface);
			//                    Log.e(TAG, "tcp22222222222222222222123+++++");
			//                } else if (surface instanceof SurfaceTexture) {
			//                    Log.e(TAG, "SurfaceTexture:");
			//                    Log.e(TAG, "tcp3333333333333333333123");
			//                    mUVCCamera.setPreviewTexture((SurfaceTexture) surface);
			//                }
			//
			//
			//                Log.e(TAG, "handleStartPreview: startPreview1");
			//                // ??????tcp??????????????????
			//                int ret = mTcpClient.nativeStartClientSocket();
			//                if (ret == 1) {
			//                    Log.e(TAG, "??????TCP?????????");
			//                    return;
			//                }
			//                Log.e(TAG, "handleStartPreview: startPreview2");
			//
			//
			//                mWeakCameraView.get().setSuportWH(mWidth, mHeight);
			//                TcpITemperatureCallback mTcpTempCb = mWeakCameraView.get().getTcpTemperatureCallback();
			//                // ??????????????????
			//                Log.e(TAG, "===============: nativeSetTemperatureCallback   start");
			//                mTcpClient.nativeSetTemperatureCallback(mTcpTempCb);
			//                Log.e(TAG, "================: nativeSetTemperatureCallback   end");
			//                //mWeakCameraView.get().setTemperatureCbing(false);
			//
			//                //mWeakCameraView.get().setRotation(180);
			//
			//                synchronized (mSync) {
			//                    mIsPreviewing = true;
			//                }
			//                //callOnStartPreview();
			//
			//            }


		}

		public void handleStopPreview () {
			if (DEBUG)
				Log.v(TAG_THREAD, "handleStopPreview:");
			if (mIsPreviewing) {
				if (cameraType == 1) {
					if (mUVCCamera != null) {
						mUVCCamera.stopPreview();
					}
					synchronized (mSync) {
						mIsPreviewing = false;
						mSync.notifyAll();
					}
					callOnStopPreview();

				} else {
					//					if (mUVCCamera != null) {
					//						mUVCCamera.stopPreview();
					//					}
					//					synchronized (mSync) {
					//						mIsPreviewing = false;
					//						mSync.notifyAll();
					//					}
					//callOnStopPreview();

				}

			}
			if (DEBUG)
				Log.v(TAG_THREAD, "handleStopPreview:finished");
		}

		//added by wupei
		public void handlePreparePalette (final String path, final int type) {
			//            if (DEBUG) Log.v(TAG_THREAD, "handleSetPalettePath:");
			if (cameraType == 1) {
				if (mUVCCamera != null) {
					mUVCCamera.setPath(path);
					mUVCCamera.setPalette(type);
				}
			}
			//            else {
			//                if (mTcpClient != null) {
			//                    mTcpClient.nativeSetPalettePath(path);
			//                }
			//            }
		}

		//?????? ??????
		public void handleCaptureStill (final String path) {
			if (DEBUG)
				Log.v(TAG_THREAD, "handleCaptureStill:");
			final Activity parent = mWeakParent.get();
			if (parent == null)
				return;
			//??????????????????
			//            mSoundPool.play(mSoundId, 1f, 1f, 1, 1, 1.0f);    // play shutter sound
			try {
				final Bitmap bitmap = mWeakCameraView.get().captureStillImage();
				//                if (mIsTemperaturing) {
				//                    temperatureData = mWeakCameraView.get().GetTemperatureData();
				//                    for (int j = 10; j < (mWidth * (mHeight - 4) + 10); j++) {
				//                        ShortTemperatureData[j] = (short) (temperatureData[j] * 10 + 2731);
				//                    }
				//                    ShortTemperatureData[0] = (short) mWidth;
				//                    ShortTemperatureData[1] = (short) (mHeight - 4);
				//                    for (int i = 0; i < (mWidth * (mHeight - 4) + 10); i++) {
				//                        short curshort = ShortTemperatureData[i];
				//                        ByteTemperatureData[2 * i] = (byte) ((curshort >> 0) & 0b1111_1111);
				//                        ByteTemperatureData[2 * i + 1] = (byte) ((curshort >> 8) & 0b1111_1111);
				//                    }
				//                }
				// get buffered output stream for saving a captured still image as a file on external storage.
				// the file name is came from current time.
				// You should use extension name as same as CompressFormat when calling Bitmap#compress.
				// add  wcc  ?????????????????? ?????????  ???????????? ?????????????????? outputFile
				final File outputFile = TextUtils.isEmpty(path) ? MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".jpg") : new File(path);
				final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
				try {
					try {
						bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
						//                        if (mIsTemperaturing) {
						//                            Log.e(TAG,"===========is mIsTemperaturing============");
						//                            os.write(ByteTemperatureData, 0, mWidth * (mHeight - 4) * 2 + 20);//??????????????????
						//                        }
						os.flush();
						//????????? add
						mHandler.setSavePicture(outputFile.getPath());
//						mHandler.sendMessage(mHandler.obtainMessage(MSG_MEDIA_UPDATE, outputFile.getPath()));
					} catch (final IOException e) {
					}
				} finally {
					os.close();
				}
				//	if(mIsTemperaturing) {
				//		String NewPath = outputFile.getPath();
				//		PngUtil.wirteByteArrayToPng(NewPath, ByteTemperatureData,NewPath );
				//	}
			} catch (final Exception e) {
				callOnError(e);
			}
		}

		public void handleMakeReport () {
			String data = MediaMuxerWrapper.getDateTimeString();
			String title = "Report" + data;
			final File dirs = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Xtherm");
			dirs.mkdirs();
			final File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + "Xtherm", title + ".docx");
			XWPFDocument m_Docx = new XWPFDocument();
			XWPFParagraph p1 = m_Docx.createParagraph();
			p1.setAlignment(ParagraphAlignment.CENTER);
			//p1.setBorderBottom(Borders.DOUBLE);
			//p1.setBorderTop(Borders.DOUBLE);

			//p1.setBorderRight(Borders.DOUBLE);
			//p1.setBorderLeft(Borders.DOUBLE);
			//p1.setBorderBetween(Borders.SINGLE);

			p1.setVerticalAlignment(TextAlignment.TOP);
			XWPFRun r1 = p1.createRun();
			r1.setFontSize(16);
			r1.setBold(true);
			r1.setText("Temperature Inspection Report\n");
			r1.setFontFamily("Courier");
			//r1.addBreak(BreakType.COLUMN);
			//r1.setUnderline(UnderlinePatterns.DOT_DOT_DASH);
			//r1.setTextPosition(100);


			XWPFParagraph p4 = m_Docx.createParagraph();
			p4.setAlignment(ParagraphAlignment.LEFT);
			XWPFRun r4 = p4.createRun();
			r4.setFontSize(12);
			r4.setFontFamily("Courier");
			r4.setBold(true);
			r4.setText("Data:");
			XWPFRun r401 = p4.createRun();
			r401.setFontSize(12);
			r401.setFontFamily("Courier");
			r401.setBold(false);
			r401.setText(data);
			//r401.addBreak(BreakType.COLUMN);


			XWPFParagraph p2 = m_Docx.createParagraph();
			p2.setAlignment(ParagraphAlignment.CENTER);
			XWPFRun r2 = p2.createRun();
			//r2.addBreak();
			int format = XWPFDocument.PICTURE_TYPE_PNG;

			byte[] tempPara = mUVCCamera.getByteArrayTemperaturePara(128);

			ByteUtil mByteUtil = new ByteUtil();
			float Fix = ByteUtil.getFloat(tempPara, 0);
			float Refltmp = mByteUtil.getFloat(tempPara, 4);
			float Airtmp = mByteUtil.getFloat(tempPara, 8);
			float humi = mByteUtil.getFloat(tempPara, 12);
			float emiss = mByteUtil.getFloat(tempPara, 16);
			float distance = mByteUtil.getShort(tempPara, 20);
			String stFix = String.valueOf(Fix);
			String stRefltmp = String.valueOf(Refltmp);
			String stAirtmp = String.valueOf(Airtmp);
			String stHumi = String.valueOf(humi);
			String stEmiss = String.valueOf(emiss);
			String stDistance = String.valueOf(distance);

			ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
			float[] TempData = mWeakCameraView.get().GetTemperatureData();
			final Bitmap bitmap = mWeakCameraView.get().captureStillImage();


			float center = TempData[0];//center
			float max = TempData[3];//max
			float min = TempData[6];//min


			bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngOut);
			//ByteArrayInputStream pngIn=new ByteArrayInputStream(pngOut.toByteArray());
			InputStream pngIn = new ByteArrayInputStream(pngOut.toByteArray());


			try {
				r2.addPicture(pngIn, format, "aaa", Units.toEMU(384), Units.toEMU(288)); // 200x200 pixels
			} catch (InvalidFormatException e) {
				Log.e(TAG, "handleMakeReport:", e);
			} catch (IOException e) {
				Log.e(TAG, "handleMakeReport:", e);
			}
			XWPFRun r201 = p2.createRun();
			r201.setFontSize(12);
			r201.setFontFamily("Courier");
			r201.setBold(false);
			r201.setText("\n");

			XWPFParagraph p3 = m_Docx.createParagraph();
			p3.setAlignment(ParagraphAlignment.LEFT);
			XWPFRun r3 = p3.createRun();
			r3.setFontSize(12);
			r3.setFontFamily("Courier");
			r3.setBold(true);
			r3.setText("Parameter:\n");
			XWPFRun r301 = p3.createRun();
			String summary = "Correction:" + stFix + ",Reflection:" + stRefltmp + ",AmbTemp:" + stAirtmp + ",Humidity:" + stHumi + ",Emissivity:" + stEmiss + ",Distance:" + stDistance;
			r301.setFontSize(12);
			r301.setBold(false);
			r301.setFontFamily("Courier");
			r301.setText(summary);
			XWPFParagraph p5 = m_Docx.createParagraph();
			p5.setAlignment(ParagraphAlignment.LEFT);
			XWPFRun r5 = p5.createRun();
			r5.setFontSize(12);
			r5.setFontFamily("Courier");
			r5.setBold(true);
			r5.setText("Throughout the scene:\n");
			XWPFRun r501 = p5.createRun();
			r501.setFontSize(12);
			r501.setFontFamily("Courier");
			r501.setBold(false);
			String scene = "Center:" + center + ",Max:" + max + ",Min:" + min + "\n";
			r501.setText(scene + "\n");


			try (FileOutputStream out = new FileOutputStream(dir)) {
				try {
					m_Docx.write(out);
					out.flush();
					mHandler.sendMessage(mHandler.obtainMessage(MSG_MEDIA_UPDATE, dir.getPath()));
				} finally {
					out.close();
				}
			} catch (IOException e) {
				Log.e(TAG, "handleMakeReport:", e);
			}


		}

		//added by wupei
		public void handleSetAreaCheck (int isAreaCheck) {
			//            if (DEBUG) Log.v(TAG_THREAD, "handleSetPalettePath:");
			if (mUVCCamera != null) {
				mUVCCamera.setAreaCheck(isAreaCheck);
			}
		}

		public void handleSetArea (final Object area) {
			//            if (DEBUG) Log.v(TAG_THREAD, "handleSetPalettePath:");
			if (mUVCCamera != null) {
				int[] data = (int[]) area;
				//                int [] areainfo = (int []) area;
				//                int[] areainfo = new int[4 * area1.size()];
				//                int i = 0;
				//                for (TouchArea mRect : area1) {//????????????????????????????????????????????????????????????
				//                    // ?????????????????? x1y1  x2y2????????? ???????????????????????????????????????areainfo[ 0 1 2 3]= x1 x2 y1 y2
				//                    int point1x = (int) (mRect.touchPoint1.x / width * 256);
				//                    int point1y = (int) (mRect.touchPoint1.y / height * 192);
				//                    int point2x = (int) (mRect.touchPoint2.x / width * 256);
				//                    int point2y = (int) (mRect.touchPoint2.y / height * 192);
				//
				//                    if (point1x <= point2x) {
				//                        areainfo[4 * i] = point1x;
				//                        areainfo[4 * i + 1] = point2x;
				//                        if (point1y <= point2y) {
				//                            areainfo[4 * i + 2] = point1y;
				//                            areainfo[4 * i + 3] = point2y;
				//                        } else {
				//                            areainfo[4 * i + 2] = point2y;
				//                            areainfo[4 * i + 3] = point1y;
				//                        }
				//                    } else {
				//                        areainfo[4 * i] = point2x;
				//                        areainfo[4 * i + 1] = point1x;
				//                        if (point1y <= point2y) {
				//                            areainfo[4 * i + 2] = point1y;
				//                            areainfo[4 * i + 3] = point2y;
				//                        } else {
				//                            areainfo[4 * i + 2] = point2y;
				//                            areainfo[4 * i + 3] = point1y;
				//                        }
				//                    }
				//                    i++;
				//                }
				//                Log.e(TAG_THREAD, "handleSetArea:"  + Arrays.toString(data));
				mUVCCamera.setArea(data);
			}
		}

		public void handleStartRecording (boolean isRecordAudio) {
			//			Log.e(TAG_THREAD, "handleStartRecording:");
			try {
				if ((mUVCCamera == null) || (mMuxer != null) || (!isTemperaturing()))
					return;
				final MediaMuxerWrapper muxer = new MediaMuxerWrapper(".mp4");    // if you record audio only, ".m4a" is also OK.
				MediaVideoBufferEncoder videoEncoder = null;
				//				Log.e(TAG_THREAD, " =================mEncoderType=========================    " + mEncoderType);
				switch (mEncoderType) {
					case 1:    // for video capturing using MediaVideoEncoder
						//						Log.e(TAG_THREAD, "===========case 1:========================:");
						new MediaVideoEncoder(muxer, mWeakCameraView.get().getWidth(), mWeakCameraView.get().getHeight(), mMediaEncoderListener);
						break;
					case 2:    // for video capturing using MediaVideoBufferEncoder
						videoEncoder = new MediaVideoBufferEncoder(muxer, getWidth(), getHeight(), mMediaEncoderListener);
						break;
					// case 0:	// for video capturing using MediaSurfaceEncoder
					default:
						new MediaSurfaceEncoder(muxer, getWidth(), getHeight(), mMediaEncoderListener);
						break;
				}
				if (isRecordAudio) {
					//for audio capturing
					//					Log.e(TAG, "=============new MediaAudioEncoder=========== ");
					new MediaAudioEncoder(muxer, mMediaEncoderListener);
				}
				muxer.prepare();
				muxer.startRecording();
				if (videoEncoder != null) {
					//					Log.e(TAG, "setFrameCallback ");
					//                    mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_YUV);
				}
				synchronized (mSync) {
					mMuxer = muxer;
					mVideoEncoder = videoEncoder;
				}
				callOnStartRecording();
			} catch (final IOException e) {
				callOnError(e);
				//				Log.e(TAG, "startCapture error =====:", e);
			}
		}

		public void handleSetPalette (int paletteCode) {
			//            if (mTcpClient == null) {
			//                return;
			//            }
			//            mTcpClient.nativeSetPalette(paletteCode);
		}

		// ????????????????????????
		public void handleStartTemperaturing () {
			Log.e(TAG, "handleStartTemperaturing: ================================" + cameraType);
			if (DEBUG)
				Log.v(TAG_THREAD, "handleStartTemperaturing:   cameraType === > " + cameraType);

			if (cameraType == 1) {//UVCCamera???TCPClient?????????????????????????????????????????????????????????????????????????????????????????????
				Log.e(TAG_THREAD, "  mUVCCamera == null:   " + (mUVCCamera == null) + "  == mIsTemperaturing  === >" + mIsTemperaturing);
				//                if ((mUVCCamera == null) || mIsTemperaturing) return;
				if ((mUVCCamera == null))
					return;
			}
			//            else {
			//                if ((mTcpClient == null) || mIsTemperaturing) return;
			//            }
			mIsTemperaturing = true;
			if (cameraType == 1) {
				mUVCCamera.startTemp();
			}
			//            else {
			//                mTcpClient.nativeStartTempCallback(0);
			//            }
			mWeakCameraView.get().setTemperatureCbing(true);
		}

		//        public void handleWatermarkOnOff(boolean isWatermaker) {
		//            Log.e(TAG, "handleWatermarkOnOff isWatermaker: " + isWatermaker);
		//            mWeakCameraView.get().watermarkOnOff(isWatermaker);
		//        }

		public void setVerifySn () {
			if (mUVCCamera == null)
				return;
			mUVCCamera.setVerifySn();
		}

		public void handleRelayout (int rotate) {
			if (mUVCCamera == null)
				return;
			mWeakCameraView.get().relayout(rotate);
		}

		public void handleTempShowOnOff (boolean isTempShow) {
			Log.e(TAG, "handleTempShowOnOff isTempShow: " + isTempShow);
			if (mWeakCameraView.get() != null)
				mWeakCameraView.get().tempShowOnOff(isTempShow);
		}

		public void handleOpenSysCamera () {
			mWeakCameraView.get().openSysCamera();
		}

		public void handleCloseSysCamera () {
			mWeakCameraView.get().closeSysCamera();
		}

		// ??????????????????
		public void handleStopTemperaturing () {
			if (DEBUG)
				Log.v(TAG_THREAD, "handleStopTemperaturing:");

			//			if (cameraType == 1) {
			if ((mUVCCamera == null)) {
				return;
			}
			//			}
			//            else {
			//                if ((mTcpClient == null)) {
			//                    return;
			//                }
			//            }
			mIsTemperaturing = false;
			mWeakCameraView.get().setTemperatureCbing(false);
			//			if (cameraType == 1) {
			mUVCCamera.stopTemp();
			//			}
			//            else {
			//                mTcpClient.nativeStopTmepCallback(1);
			//            }
		}

		public void handleStopRecording () {
			Log.e(TAG_THREAD, "handleStopRecording:mMuxer=" + mMuxer);
			final MediaMuxerWrapper muxer;
			synchronized (mSync) {
				muxer = mMuxer;
				mMuxer = null;
				mVideoEncoder = null;
				//                if (mUVCCamera != null) {
				//                	mUVCCamera.stopCapture();
				//                }
			}
			try {
				mWeakCameraView.get().setVideoEncoder(null);
			} catch (final Exception e) {
				// ignore
			}
			if (muxer != null) {
				Log.e(TAG_THREAD, "handleStopRecording:muxer != null");
				muxer.stopRecording();
				mUVCCamera.setFrameCallback(null, 0);
				// you should not wait here
				callOnStopRecording();
			}
		}

		//??????????????????
		public void handleUpdateMedia (final String path) {
			if (DEBUG)
				Log.v(TAG_THREAD, "handleUpdateMedia:path=" + path);
			final Activity parent = mWeakParent.get();
			final boolean released = (mHandler == null) || mHandler.mReleased;
			if (parent != null && parent.getApplicationContext() != null) {
				try {
					if (DEBUG)
						Log.i(TAG, "MediaScannerConnection#scanFile");
//					mHandler.postDelayed(new Runnable() {
//						@Override
//						public void run () {
							MediaScannerConnection.scanFile(parent.getApplicationContext(), new String[]{path}, null, ScanCompletedListener);
//						}
//					},1000);


					for (final CameraCallback callback : mCallbacks) {
						try {
							callback.onSavePicFinished(true, path);
						} catch (final Exception e) {
							mCallbacks.remove(callback);
							Log.w(TAG, e);
						}
					}
				} catch (final Exception e) {
					Log.e(TAG, "handleUpdateMedia:", e);
				}
				if (released || parent.isDestroyed()) {
					handleRelease();
				}
				/*	if(mIsTemperaturing) {
						String NewPath = "storage/emulated/0/DCIM/Xtherm/out.png";
						try {
							PngUtil.wirteByteArrayToPng(path, ByteTemperatureData, path);
							try {
								MediaScannerConnection.scanFile(parent.getApplicationContext(), new String[]{ path }, null, null);
							} catch (final Exception e) {
							}
						} catch (final Exception e) {
					Log.e(TAG, "handleUpdateMedia wirteByteArrayToPng:", e);
				}
					}*/
			} else {
				Log.w(TAG, "MainActivity already destroyed");
				// give up to add this movie to MediaStore now.
				// Seeing this movie on Gallery app etc. will take a lot of time.
				handleRelease();
			}
		}

		public void handleRelease () {
			if (DEBUG)
				Log.v(TAG_THREAD, "handleRelease:mIsRecording=" + mIsRecording);
//			handleClose();
			mCallbacks.clear();
			if (!mIsRecording) {
				mHandler.mReleased = true;
				Looper.myLooper().quit();
			}
			if (DEBUG)
				Log.v(TAG_THREAD, "handleRelease:finished");
		}

		/**
		 * prepare and load shutter sound for still image capturing
		 */
		//        @SuppressWarnings("deprecation")
		//        private void loadShutterSound(final Context context) {
		//            // get system stream type using reflection
		//            int streamType;
		//            try {
		//                final Class<?> audioSystemClass = Class.forName("android.media.AudioSystem");
		//                final Field sseField = audioSystemClass.getDeclaredField("STREAM_SYSTEM_ENFORCED");
		//                streamType = sseField.getInt(null);
		//            } catch (final Exception e) {
		//                streamType = AudioManager.STREAM_SYSTEM;    // set appropriate according to your app policy
		//            }
		////            if (mSoundPool != null) {
		////                try {
		////                    mSoundPool.release();
		////                } catch (final Exception e) {
		////                }
		////                mSoundPool = null;
		////            }
		//            //added by wupei,??????????????????
		//            // load shutter sound from resource
		////            mSoundPool = new SoundPool(2, streamType, 0);
		////            mSoundId = mSoundPool.load(context, R.raw.camera_click, 1);
		//        }
		@Override
		public void run () {
			Looper.prepare();
			AbstractUVCCameraHandler handler = null;
			try {
				Log.e(TAG, "run: ============ start id" + getId());
				//?????? UvcCameraHandler??? UvcCameraHandler(CameraThread cameraThread)???????????? ????????????UvcCameraHandler??? ??????????????????
				final Constructor<? extends AbstractUVCCameraHandler> constructor = mHandlerClass.getDeclaredConstructor(CameraThread.class);
				//?????? ????????? ?????????????????? ???????????????????????????
				handler = constructor.newInstance(this);
				Log.e(TAG, "run: ============ end " + getId());

			} catch (final NoSuchMethodException e) {
				Log.w(TAG, e);
			} catch (final IllegalAccessException e) {
				Log.w(TAG, e);
			} catch (final InstantiationException e) {
				Log.w(TAG, e);
			} catch (final InvocationTargetException e) {
				Log.w(TAG, e);
			}
			if (handler != null) {
				synchronized (mSync) {
					mHandler = handler;
					mSync.notifyAll();
				}
				Looper.loop();
				//                if (mSoundPool != null) {
				//                    mSoundPool.release();
				//                    mSoundPool = null;
				//                }
				if (mHandler != null) {
					mHandler.mReleased = true;
				}
			}
			mCallbacks.clear();
			synchronized (mSync) {
				mHandler = null;
				mSync.notifyAll();
			}
		}

		private void callOnOpen () {
			for (final CameraCallback callback : mCallbacks) {
				try {
					callback.onOpen();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnClose () {
			for (final CameraCallback callback : mCallbacks) {
				try {
					callback.onClose();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnStartPreview () {
			for (final CameraCallback callback : mCallbacks) {
				try {
					callback.onStartPreview();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnStopPreview () {
			for (final CameraCallback callback : mCallbacks) {
				try {
					callback.onStopPreview();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnStartRecording () {
			for (final CameraCallback callback : mCallbacks) {
				try {
					callback.onStartRecording();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnStopRecording () {
			for (final CameraCallback callback : mCallbacks) {
				try {
					callback.onStopRecording();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnError (final Exception e) {
			for (final CameraCallback callback : mCallbacks) {
				try {
					callback.onError(e);
				} catch (final Exception e1) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		//added by wupei
		public void handleLaWenKuan (float maxPercent, float minPercent, float maxValue, float minValue) {
			if ((mUVCCamera == null)) {
				return;
			}
			mUVCCamera.laWenKuan(maxPercent, minPercent, maxValue, minValue);
		}

		public void handleSaveFiveSeconds (String path) {//???????????? ????????????
			if ((mUVCCamera == null)) {
				return;
			}
			mUVCCamera.SaveFiveSeconds(path);
		}

		public void handleSavePicture (String path) {//??????
			if ((mUVCCamera == null)) {
				return;
			}
//			if (mUVCCamera.savePicture(path) ==1){
			mUVCCamera.savePicture(path);
//				Log.e(TAG, "handleSavePicture: ===============================================================11111111111111=====");
				mHandler.sendMessage(mHandler.obtainMessage(MSG_MEDIA_UPDATE, path));
//			}
			
		}

		public void handleDisWenKuan () {
			if ((mUVCCamera == null)) {
				return;
			}
			mUVCCamera.DisWenKuan();
		}

		//???????????????
		public void handleFixedTempStrip (boolean state) {
			if ((mUVCCamera == null)) {
				return;
			}
			mUVCCamera.FixedTempStrip(state);
		}

		/**
		 * ??????TinyC ????????????
		 * TinyC?????? ???????????? ??????
		 */
		public void handleTinySaveCameraParams () {
			if (mUVCCamera == null) {
				return;
			}
			mUVCCamera.TinySaveCameraParams();
		}

		public void handleChangePalette (int typeOfPalette) {
			if ((mUVCCamera == null)) {
				return;
			}
			mUVCCamera.changePalette(typeOfPalette);
		}

		public void handleSetTempRange (int range) {
			if ((mUVCCamera == null)) {
				return;
			}
			mUVCCamera.setTempRange(range);
		}

		public void handleSetShutterFix (float mShutterFix) {
			if ((mUVCCamera == null)) {
				return;
			}
			mUVCCamera.setShutterFix(mShutterFix);
		}

		public void handleSetHighThrow (int inputHighThrow) {
			if ((mUVCCamera == null)) {
				return;
			}
			//mUVCCamera.setHighThrow(inputHighThrow);
		}

		public void handleSetLowThrow (int inputLowThrow) {
			if ((mUVCCamera == null)) {
				return;
			}
			//mUVCCamera.setLowThrow(inputLowThrow);
		}

		public void handleSetLowPlat (int inputLowPlat) {
			if ((mUVCCamera == null)) {
				return;
			}
			//mUVCCamera.setLowPlat(inputLowPlat);
		}

		public void handleSetHighPlat (int inputHighPlat) {
			if ((mUVCCamera == null)) {
				return;
			}
			//mUVCCamera.setHighPlat(inputHighPlat);
		}

		public void handleSetOrgSubGsHigh (int inputOrgSubGsHigh) {
			if ((mUVCCamera == null)) {
				return;
			}
			//mUVCCamera.setOrgSubGsHigh(inputOrgSubGsHigh);
		}

		public void handleSetOrgSubGsLow (int inputOrgSubGsLow) {
			if ((mUVCCamera == null)) {
				return;
			}
			//mUVCCamera.setOrgSubGsLow(inputOrgSubGsLow);
		}

		public void handleSetSigmaD (float inputSigmaD) {
			if ((mUVCCamera == null)) {
				return;
			}
			//mUVCCamera.setSigmaD(inputSigmaD);
		}

		public void handleSetSigmaR (float inputSigmaR) {
			if ((mUVCCamera == null)) {
				return;
			}
			//            mUVCCamera.setSigmaR(inputSigmaR);
		}
	}
}
