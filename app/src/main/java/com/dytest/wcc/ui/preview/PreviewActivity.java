package com.dytest.wcc.ui.preview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.dytest.wcc.R;
import com.dytest.wcc.cameracommon.usbcameracommon.AbstractUVCCameraHandler;
import com.dytest.wcc.cameracommon.usbcameracommon.UVCCameraHandler;
import com.dytest.wcc.cameracommon.utils.ByteUtil;
import com.dytest.wcc.common.utils.FontUtils;
import com.dytest.wcc.common.widget.dragView.DrawLineRectHint;
import com.dytest.wcc.common.widget.dragView.MeasureTempContainerView;
import com.dytest.wcc.constans.DYConstants;
import com.dytest.wcc.constans.DYTApplication;
import com.dytest.wcc.constans.DYTRobotSingle;
import com.dytest.wcc.databinding.ActivityPreviewBinding;
import com.dytest.wcc.ui.base.BaseActivity;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//@RuntimePermissions
public class PreviewActivity extends BaseActivity<ActivityPreviewBinding> implements SensorEventListener {
	private static final int     MSG_CHECK_UPDATE  = 1;
	private static final int     MSG_CAMERA_PARAMS = 2;
	public static        long    startTime         = 0;
	private              boolean isConnect         = false;

	private UVCCameraHandler mUvcCameraHandler;
	private USBMonitor       mUsbMonitor;
	private int              mFontSize;
	private SendCommand      mSendCommand;
	private int              screenWidth, screenHeight;

	// 重置参数 返回值
	private       int                                     defaultSettingReturn = -1;
	//更新工具类对象
	//	private       AppUpdater                              mAppUpdater;
	private       int                                     maxIndex             = 0;
	private       List<UpdateObj>                         updateObjList;
	//传感器
	private       AbstractUVCCameraHandler.CameraCallback cameraCallback;
	private final Handler                                 mHandler             = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what) {
				case MSG_CHECK_UPDATE:
					break;
			}
			return false;
		}
	});

	private       Surface                            stt;
	private final USBMonitor.OnDeviceConnectListener onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
		@Override
		public void onAttach (UsbDevice device) {
			isConnect = true;
			//			if (isDebug)Log.e(TAG, "DD  onAttach: "+ device.toString());
			Handler handler = new Handler();
			handler.postDelayed(() -> {
				if (isDebug)
					Log.e(TAG, "检测到设备========");
				if (mUsbMonitor != null)
					mUsbMonitor.requestPermission(device);
			}, 100);
		}

		@Override
		public void onConnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
			isConnect = true;
			//			loadingDialog.show();

			if (mUvcCameraHandler == null || mUvcCameraHandler.isReleased()) {
				mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(), mDataBinding.textureViewPreviewActivity, 1,
						384, 292, 1, null, mDataBinding.dragTempContainerPreviewActivity, 0);
			}
			mUvcCameraHandler.open(ctrlBlock);
			startPreview();

			mHandler.postDelayed(() -> {
				setValue(UVCCamera.CTRL_ZOOM_ABS, DYConstants.CAMERA_DATA_MODE_8004);//切换数据输出8004
				// 原始8005yuv,80ff保存
			}, 300);

		}

		@Override
		public void onDettach (UsbDevice device) {
			//			loadingDialog.dismiss();
			//				mUvcCameraHandler.close();
			isConnect = false;
			if (isDebug)
				Log.e(TAG, "DD  onDetach: ");
		}

		@Override
		public void onDisconnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
			isConnect = false;

			if (mUvcCameraHandler != null) {
				mUvcCameraHandler.disWenKuan();
				mUvcCameraHandler.fixedTempStripChange(false);
			}
			DYTApplication.setRobotSingle(DYTRobotSingle.NO_DEVICE);

			if (mUvcCameraHandler != null && !mUvcCameraHandler.isClosed()) {
				//				mUvcCameraHandler.removeCallback(cameraCallback);
				//				mUvcCameraHandler.stopTemperaturing();//2022年5月18日19:00:15 S0温度失常
				mUvcCameraHandler.close();
			} else {
				Log.e(TAG, "onDisconnect: ==============mUvcCameraHandler == null========");
			}
		}

		@Override
		public void onCancel (UsbDevice device) {
			if (isDebug)
				Log.e(TAG, "DD  onCancel: ");
		}
	};


	private SensorManager mSensorManager;
	private Sensor        mAccelerometerSensor;//加速度传感器
	private Sensor        mMagneticSensor;//重力传感器

	@Override
	protected void onPause () {
		if (isDebug)
			Log.e(TAG, "onPause: ");

		mSensorManager.unregisterListener(this, mAccelerometerSensor);
		mSensorManager.unregisterListener(this, mMagneticSensor);
		super.onPause();
	}

	@Override
	public void onStop () {
		if (isDebug)
			Log.e(TAG, "onStop: ");
		if (mUsbMonitor != null) {
			if (mUsbMonitor.isRegistered()) {
				mUsbMonitor.unregister();
			}
		}
		if (mUvcCameraHandler != null && !mUvcCameraHandler.isClosed()) {
			mUvcCameraHandler.close();
		}
		super.onStop();
	}

	@Override
	protected void onRestart () {


		if (isDebug)
			Log.e(TAG, "onRestart: ");
		//		mDataBinding.textureViewPreviewActivity.onResume();
		super.onRestart();
	}

	@Override
	public void onStart () {
		super.onStart();
		if (isDebug)
			Log.e(TAG, "onStart: ");

	}

	@Override
	public void onCreate (@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isDebug)
			Log.e(TAG, "onCreate: ");
	}

	@Override
	public void onDestroy () {
		if (isDebug)
			Log.e(TAG, "onDestroy: ");
		if (mUvcCameraHandler != null && !mUvcCameraHandler.isClosed()) {
			mUvcCameraHandler.release();
			mUvcCameraHandler = null;
		}
		if (mUsbMonitor != null) {
			mUsbMonitor.destroy();
			mUsbMonitor = null;
		}

		super.onDestroy();
	}

	private int originalOritation = 0;

	@Override
	protected void onResume () {
		mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_UI);
		if (isDebug)
			Log.e(TAG, "onResume: ");
		if (mUsbMonitor != null && !mUsbMonitor.isRegistered()) {
			mUsbMonitor.register();
		}
		super.onResume();
		originalOritation = this.getWindowManager().getDefaultDisplay().getRotation();
	}

	/**
	 * 打开连接 调用预览图像的设置
	 */
	private void startPreview () {

		mUvcCameraHandler.addCallback(cameraCallback);
		if (mDataBinding.textureViewPreviewActivity.getSurfaceTexture() == null) {
			Log.e(TAG, "startPreview: 启动失败,getSurfaceTexture 为null.");
			return;
		}
		stt = new Surface(mDataBinding.textureViewPreviewActivity.getSurfaceTexture());

		int mTextureViewWidth = mDataBinding.textureViewPreviewActivity.getWidth();
		int mTextureViewHeight = mDataBinding.textureViewPreviewActivity.getHeight();

		mDataBinding.textureViewPreviewActivity.iniTempBitmap(mTextureViewWidth, mTextureViewHeight);//初始化画板的值，是控件的像素的宽高
		mDataBinding.textureViewPreviewActivity.initTempFontSize(mFontSize);
		mDataBinding.textureViewPreviewActivity.setDragTempContainer(mDataBinding.dragTempContainerPreviewActivity);

		//是否进行温度的绘制
		//	private int isWatermark;
		int isTempShow = 0;
		//		mUvcCameraHandler.tempShowOnOff(isTempShow);//是否显示绘制的温度
		// 0不显示，1显示。最终调用的是UVCCameraTextureView的绘制线程。

		mUvcCameraHandler.startPreview(stt);
		//tinyC 暂时关闭 温度回调功能
		mUvcCameraHandler.startTemperaturing();//温度回调

		mDataBinding.dragTempContainerPreviewActivity.setBackgroundColor(getColor(R.color.bg_preview_otg_connect));
		mDataBinding.dragTempContainerPreviewActivity.setConnect(true);
		mDataBinding.dragTempContainerPreviewActivity.invalidate();

	}

	private int setValue (final int flag, final int value) {//设置机芯参数,调用JNI层
		return mUvcCameraHandler != null ? mUvcCameraHandler.setValue(flag, value) : 0;
	}


	@Override
	protected int bindingLayout () {
		return R.layout.activity_preview;
	}

	@Override
	protected String getLanguageStr () {
		return "";
	}


	@Override
	protected void initView () {
		cameraCallback = new AbstractUVCCameraHandler.CameraCallback() {
			@Override
			public void onOpen () {
			}

			@Override
			public void onClose () {

			}

			@Override
			public void onStartPreview () {
				if (originalOritation == 3) {
					mUvcCameraHandler.setRotateMatrix_180(true);
				} else {
					mUvcCameraHandler.setRotateMatrix_180(false);
				}
			}

			@Override
			public void onStopPreview () {

			}

			@Override
			public void onStartRecording () {

			}

			@Override
			public void onStopRecording () {

			}

			@Override
			public void onError (Exception e) {

			}

			@Override
			public void onSavePicFinished (boolean isFinish, String picPath) {

			}
		};

		DisplayMetrics dm = getResources().getDisplayMetrics();

		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		mSendCommand = new SendCommand();

		List<String> permissions = new ArrayList<>();
		//		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
		//			permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
		//		} else {
		permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
		permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		//		}
		permissions.add(Manifest.permission.CAMERA);
		permissions.add(Manifest.permission.RECORD_AUDIO);

		XXPermissions.with(this).permission(permissions).request(new OnPermissionCallback() {
			@Override
			public void onGranted (List<String> permissions, boolean all) {
				if (all) {
					mFontSize = FontUtils.adjustFontSize(screenWidth, screenHeight);

					initListener();

					initRecord();


					mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(), mDataBinding.textureViewPreviewActivity,
							1, 384, 292, 1, null, mDataBinding.dragTempContainerPreviewActivity, 0);

					mUsbMonitor = new USBMonitor(mContext.get(), onDeviceConnectListener);
				} else {
					new AlertDialog.Builder(PreviewActivity.this).setMessage(R.string.toast_base_permission_explain).setPositiveButton(R.string.confirm, (dialog, button) -> XXPermissions.startPermissionActivity(PreviewActivity.this, permissions)).setNegativeButton(R.string.cancel, (dialog, button) -> showToast(getString(R.string.toast_dont_have_permission))).show();
				}
			}

			@Override
			public void onDenied (List<String> permissions, boolean never) {
				showToast(getString(R.string.toast_dont_have_permission));
			}
		});

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}


	private boolean isSavePhoto = false;

	/**
	 * 获取当前 语言代码 及其 城市代码
	 *
	 * @return string 格式：xx-rXX
	 */
	private String getLanguageCountryStr () {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
			return Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage() + "-r" + Resources.getSystem().getConfiguration().getLocales().get(0).getCountry();
		} else {
			return Resources.getSystem().getConfiguration().locale.getLanguage() + "-r" + Resources.getSystem().getConfiguration().locale.getCountry();
		}
	}

	/**
	 * 初始化界面的监听器
	 */
	private void initListener () {
		//测试的 监听器
		//		mDataBinding.btTest01.setVisibility(View.VISIBLE);
		mDataBinding.btTest02.setOnClickListener(v -> {
			String path = mContext.get().getExternalFilesDir(null) + File.separator + "dy_test_log.txt";
			File file = new File(path);
			if (file.exists()) {
				file.delete();
				mDataBinding.tvShow.setText("");
			}

		});


		mDataBinding.btTest01.setOnClickListener(v -> {
//			Log.e(TAG, "------------btTest01-----------------------------");

			String filePath = mContext.get().getExternalFilesDir(null) + File.separator+ "dy_test_log.txt";
			File logFile = new File(filePath);
			StringBuilder sb = new StringBuilder();
			if (logFile.exists()){
				FileInputStream fips = null;
				BufferedReader br = null;
				try {
					fips = new FileInputStream(logFile);
					br = new BufferedReader(new InputStreamReader(fips));
					String data ;
					while((data = br.readLine()) != null ){
						sb.append(data);
						sb.append("\n");
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}finally {
					if (br != null){
						try {
							br.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				mDataBinding.tvShow.setText(sb.toString());

			}else {
				Log.e(TAG, "initListener: file dot exists-------------------" + filePath);
			}

			//			CrashReport.testJavaCrash();
			//******************************testJNi***************************************
			//			mUvcCameraHandler.testJNi(Build.MODEL);
			//			Log.e(TAG, "initListener: " + mDataBinding.textureViewPreviewActivity
			//			.getTemperatureCallback());
			//******************************************如何打开PDF文档******************************
			//			String MenuUrl = "/storage/emulated/0/Android/data/com.dyt.wcc
			//			.dytpir/files/SLReadMeCN.pdf";
			//			String googleUrl = "http://docs.google.com/gview?embedded=true&url=";
			//			Log.d(TAG, googleUrl + MenuUrl);
			//			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl
			//			+ MenuUrl));
			//			startActivity(browserIntent);


			//			File file = new File("/storage/emulated/0/Android/data/com.dyt.wcc
			//			.dytpir/files/SLReadMeCN.pdf");
			//
			//			ParcelFileDescriptor pdfFile = null;
			//			try {
			//				pdfFile = ParcelFileDescriptor.open(file, ParcelFileDescriptor
			//				.MODE_READ_ONLY); //以只读的方式打开文件
			//			} catch (FileNotFoundException e) {
			//				e.printStackTrace();
			//			}
			//
			//			PdfRenderer renderer = null;
			//			try {
			//				renderer = new PdfRenderer(pdfFile);//用上面的pdfFile新建PdfRenderer对象
			//			} catch (IOException e) {
			//				e.printStackTrace();
			//			}
			//
			//			final int pageCount = renderer.getPageCount();//获取pdf的页码数
			//			Bitmap[] bitmaps = new Bitmap[pageCount];//新建一个bmp数组用于存放pdf页面
			//
			//			WindowManager wm = this.getWindowManager();//获取屏幕的高和宽，以决定pdf的高和宽
			//			float width = wm.getDefaultDisplay().getWidth();
			//			float height = wm.getDefaultDisplay().getHeight();
			//
			//			for (int i = 0; i < pageCount; i++)
			//			{//这里用循环把pdf所有的页面都写入bitmap数组，真正使用的时候最好不要这样，
			//				//因为一本pdf的书会有很多页，一次性全部打开会非常消耗内存，我打开一本两百多页的书就消耗了1.8G的内存，而且打开速度很慢。
			//				//真正使用的时候要采用动态加载，用户看到哪页才加载附近的几页。而且最好使用多线程在后台打开。
			//
			//				PdfRenderer.Page page = renderer.openPage(i);//根据i的变化打开每一页
			//				bitmap_pdf = Bitmap.createBitmap((int) (width), (int) (page.getHeight
			//				() * width / page.getWidth()), Bitmap.Config.ARGB_8888);
			// 根据屏幕的高宽缩放生成bmp对象
			//				page.render(bitmap_pdf, null, null, PdfRenderer.Page
			//				.RENDER_MODE_FOR_DISPLAY);//将pdf的内容写入bmp中
			//
			//				bitmaps[i] = bitmap_pdf;//将pdf的bmp图像存放进数组中。
			//
			//				// close the page
			//				page.close();
			//			}
			//
			//			// close the renderer
			//			renderer.close();
			//			mDataBinding.ivPreviewPdf.setImageBitmap(bitmap_pdf);
			//						startActivity(getPdfFileIntent
			//						("/storage/emulated/0/Android/data/com.dyt.wcc
			//						.dytpir/files/SLReadMeCN.pdf"));

			//************************检测当前语言设置********************************
			//				if (isDebug){
			//					Log.e(TAG, "onClick: " + Locale.getDefault().getLanguage());
			//					Log.e(TAG, "onClick: " + sp.getInt(DYConstants.LANGUAGE_SETTING,
			//					8));
			//				}
			//				boolean dd = false;
			//				if (jni_status == 0) {
			//					dd = mUvcCameraHandler.javaSendJniOrder(jni_status);
			//					jni_status = 1;
			//					mDataBinding.btTest01.setText("采集中");
			//				} else {
			//					dd = mUvcCameraHandler.javaSendJniOrder(jni_status);
			//					jni_status = 0;
			//					mDataBinding.btTest01.setText("终止");
			//				}
			//				Log.e(TAG, "onClick: " + dd);
			//				mUvcCameraHandler.startTemperaturing();
			//*****************************************************************

			//*****************************旋转180************************************
			//			isRotate = !isRotate;
			//			mUvcCameraHandler.setRotateMatrix_180(isRotate);
		});

		//		initCompanyPop();
		//
		mDataBinding.dragTempContainerPreviewActivity.setAddChildDataListener(new MeasureTempContainerView.onAddChildDataListener() {
			@Override
			public void onIsEventActionMove (DrawLineRectHint hint) {
				mDataBinding.textureViewPreviewActivity.setDrawHint(hint);
			}

			@Override
			public void onIsEventActionUp (DrawLineRectHint hint) {
				hint.setNeedDraw(false);
				mDataBinding.textureViewPreviewActivity.setDrawHint(hint);
			}
		});

	}


	private boolean cameraParamsIsRight (Map<String, Float> params, byte[] p1, byte[] p2) {
		boolean isRight = true;
		for (int i = 0; i < p1.length; i++) {
			if (p1[i] != p2[i]) {
				isRight = false;
			}
		}
		if (params.get(DYConstants.setting_environment) < (-50.f) || params.get(DYConstants.setting_environment) > 500.0f) {
			isRight = false;
		}
		if (params.get(DYConstants.setting_reflect) < (-50.f) || params.get(DYConstants.setting_reflect) > 500.0f) {
			isRight = false;
		}
		return isRight;
	}

	/**
	 * @param value    发送的值
	 * @param position 下标
	 * @return 是否成功
	 */
	public int sendS0Order (float value, int position) {
		int result = -1;
		byte[] iputEm = new byte[4];
		ByteUtil.putFloat(iputEm, value, 0);
		if (mSendCommand != null && mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {
			result = mSendCommand.sendFloatCommand(position, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
		}
		return result;
	}


	/**
	 * 初始化录制的相关方法
	 */
	private void initRecord () {
	}


	private int intFlag    = 0;
	private int oldIntFlag = 0;

	private float[] mLastAccelerometer    = new float[3];
	private float[] mLastMagnetometer     = new float[3];
	private boolean mLastAccelerometerSet = false;
	private boolean mLastMagnetometerSet  = false;

	private float[] mR           = new float[9];
	private float[] mOrientation = new float[3];

	/**
	 * Called when there is a new sensor event.  Note that "on changed"
	 * is somewhat of a misnomer, as this will also be called if we have a
	 * new reading from a sensor with the exact same sensor values (but a
	 * newer timestamp).
	 *
	 * <p>See {@link SensorManager SensorManager}
	 * for details on possible sensor types.
	 * <p>See also {@link SensorEvent SensorEvent}.
	 *
	 * <p><b>NOTE:</b> The application doesn't own the
	 * {@link SensorEvent event}
	 * object passed as a parameter and therefore cannot hold on to it.
	 * The object may be part of an internal pool and may be reused by
	 * the framework.
	 *
	 * @param event the {@link SensorEvent SensorEvent}.
	 */
	@Override
	public void onSensorChanged (SensorEvent event) {
		if (event.sensor == mAccelerometerSensor) {
			System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
			mLastAccelerometerSet = true;
		} else if (event.sensor == mMagneticSensor) {
			System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
			mLastMagnetometerSet = true;
		}
		if (mLastAccelerometerSet && mLastMagnetometerSet) {
			SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
			SensorManager.getOrientation(mR, mOrientation);
			mLastMagnetometerSet = false;
			mLastAccelerometerSet = false;

			double degreeValue = (mOrientation[2]) / Math.PI * 180f;

			if (degreeValue < -26) {
				intFlag = 1;
			} else if (degreeValue > 26) {
				intFlag = -1;
			} else {
				intFlag = 0;
			}
			if (intFlag == -1 && oldIntFlag != -1) {
				mUvcCameraHandler.setRotateMatrix_180(true);
				oldIntFlag = intFlag;
			}
			if (intFlag == 1 && oldIntFlag != 1) {
				if (mUvcCameraHandler != null && mUvcCameraHandler.isPreviewing()) {
					mUvcCameraHandler.setRotateMatrix_180(false);
					oldIntFlag = intFlag;
				}
			}
		}
	}

	/**
	 * Called when the accuracy of the registered sensor has changed.  Unlike
	 * onSensorChanged(), this is only called when this accuracy value changes.
	 *
	 * <p>See the SENSOR_STATUS_* constants in
	 * {@link SensorManager SensorManager} for details.
	 *
	 * @param sensor
	 * @param accuracy The new accuracy of this sensor, one of
	 *                 {@code SensorManager.SENSOR_STATUS_*}
	 */
	@Override
	public void onAccuracyChanged (Sensor sensor, int accuracy) {

	}

	//修改S0机芯参数
	public class SendCommand {
		int psitionAndValue0 = 0, psitionAndValue1 = 0, psitionAndValue2 = 0, psitionAndValue3 = 0;
		int result = -1;

		public int sendFloatCommand (int position, byte value0, byte value1, byte value2, byte value3, int interval0, int interval1,
		                             int interval2, int interval3, int interval4) {
			psitionAndValue0 = (position << 8) | (0x000000ff & value0);
			Handler handler0 = new Handler();

			handler0.postDelayed(() -> result = mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0), interval0);

			psitionAndValue1 = ((position + 1) << 8) | (0x000000ff & value1);
			handler0.postDelayed(() -> result = mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue1), interval1);

			psitionAndValue2 = ((position + 2) << 8) | (0x000000ff & value2);
			handler0.postDelayed(() -> result = mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue2), interval2);

			psitionAndValue3 = ((position + 3) << 8) | (0x000000ff & value3);
			handler0.postDelayed(() -> result = mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue3), interval3);

			handler0.postDelayed(() -> mUvcCameraHandler.whenShutRefresh(), interval4);

			return result;
		}

		private void whenChangeTempPara () {
			if (mUvcCameraHandler != null) {
				mUvcCameraHandler.whenChangeTempPara();
			}
		}

		public void sendShortCommand (int position, byte value0, byte value1, int interval0, int interval1, int interval2) {
			psitionAndValue0 = (position << 8) | (0x000000ff & value0);
			Handler handler0 = new Handler();
			handler0.postDelayed(() -> mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0), interval0);

			psitionAndValue1 = ((position + 1) << 8) | (0x000000ff & value1);
			handler0.postDelayed(() -> mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue1), interval1);

			handler0.postDelayed(() -> mUvcCameraHandler.whenShutRefresh(), interval2);
		}

		public void sendByteCommand (int position, byte value0, int interval0) {
			psitionAndValue0 = (position << 8) | (0x000000ff & value0);
			Handler handler0 = new Handler();
			handler0.postDelayed(() -> mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0), interval0);
			handler0.postDelayed(() -> mUvcCameraHandler.whenShutRefresh(), interval0 + 20);
		}
	}
}