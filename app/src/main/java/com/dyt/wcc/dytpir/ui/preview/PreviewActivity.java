package com.dyt.wcc.dytpir.ui.preview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import com.dyt.wcc.cameracommon.encoder.MediaMuxerWrapper;
import com.dyt.wcc.cameracommon.usbcameracommon.UVCCameraHandler;
import com.dyt.wcc.cameracommon.utils.ByteUtil;
import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.common.utils.DensityUtil;
import com.dyt.wcc.common.utils.FontUtils;
import com.dyt.wcc.common.widget.MyCustomRangeSeekBar;
import com.dyt.wcc.common.widget.SwitchMultiButton;
import com.dyt.wcc.common.widget.dragView.DragTempContainer;
import com.dyt.wcc.common.widget.dragView.DrawLineRecHint;
import com.dyt.wcc.common.widget.dragView.TempWidgetObj;
import com.dyt.wcc.dytpir.BuildConfig;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.constans.DYConstants;
import com.dyt.wcc.dytpir.databinding.ActivityPreviewBinding;
import com.dyt.wcc.dytpir.databinding.PopCompanyInfoBinding;
import com.dyt.wcc.dytpir.databinding.PopHighlowcenterTraceBinding;
import com.dyt.wcc.dytpir.databinding.PopPaletteChoiceBinding;
import com.dyt.wcc.dytpir.databinding.PopSettingBinding;
import com.dyt.wcc.dytpir.databinding.PopTempModeChoiceBinding;
import com.dyt.wcc.dytpir.ui.DYTApplication;
import com.dyt.wcc.dytpir.ui.gallery.GlideEngine;
import com.dyt.wcc.dytpir.utils.AssetCopyer;
import com.dyt.wcc.dytpir.utils.ByteUtilsCC;
import com.dyt.wcc.dytpir.utils.CreateBitmap;
import com.dyt.wcc.dytpir.utils.LanguageUtils;
import com.huantansheng.easyphotos.EasyPhotos;
import com.king.app.dialog.AppDialog;
import com.king.app.updater.AppUpdater;
import com.king.app.updater.http.OkHttpManager;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.zhihu.matisse.Matisse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PreviewActivity extends BaseActivity<ActivityPreviewBinding> {
	//jni测试按钮状态记录
	private int jni_status = 0;

	private UVCCameraHandler   mUvcCameraHandler;
	private Surface            stt;
	private PopupWindow        PLRPopupWindows;//点线矩形测温弹窗
	private PopupWindow        allPopupWindows;
	//	private View popView;
	private Map<String, Float> cameraParams;
	private SharedPreferences  sp;
	private int                mVid, mPid; //设备 vid pid

	private Timer      timerEveryTime;
	private USBMonitor mUsbMonitor;
	private int        mTextureViewWidth, mTextureViewHeight;

	private FrameLayout fl;

	private int mFontSize;
	private int paletteType, isWatermark, isTempShow;
	private String palettePath;
	//customSeekBar
	private Bitmap tiehong = null, caihong = null, baire = null, heire = null, hongre = null, lenglan = null;
	private SendCommand mSendCommand;

	private int screenWidth, screenHeight;
	private Bitmap highTempBt, lowTempBt, centerTempBt, normalPointBt;

	private DisplayMetrics metrics;
	private Configuration  configuration;
	private String         locale_language;
	private int            language   = -1;//语言的 索引下标
	private boolean        isFirstRun = false;

	private static final int REQUEST_CODE_CHOOSE  = 23;
	// 重置参数 返回值
	private              int defaultSettingReturn = -1;

	//2022年4月8日15:24:14  TinyC 读取TAU（等效大气透过率）
	private byte[] tau_data;

	//更新工具类对象
	private AppUpdater      mAppUpdater;
	private int             maxIndex = 0;
	private List<UpdateObj> updateObjList;
	//传感器
	private SensorManager   mSensorManager;
	private Sensor          mMagneticSensor, mAccelerometerSensor;//磁场传感器，加速度传感器

	private static final int     MSG_CHECK_UPDATE = 1;
	private              Handler mHandler         = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what) {
				case MSG_CHECK_UPDATE:
					updateObjList = (List<UpdateObj>) msg.obj;

					View view = LayoutInflater.from(mContext.get()).inflate(R.layout.dialog_custom, null);
					TextView tvTitle = view.findViewById(R.id.tvTitle);
					tvTitle.setText(String.format(getString(R.string.pop_version) + "V%s", updateObjList.get(maxIndex).getAppVersionName()));
					TextView tvContent = view.findViewById(R.id.tvContent);
					tvContent.setVisibility(View.GONE);
					//					tvContent.setText("1、新增某某功能、\n2、修改某某问题、\n3、优化某某BUG、");
					View btnCancel = view.findViewById(R.id.btnCancel);
					btnCancel.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick (View v) {
							AppDialog.INSTANCE.dismissDialog();
						}
					});
					View btnConfirm = view.findViewById(R.id.btnConfirm);
					btnConfirm.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick (View v) {
							showToast(getString(R.string.toast_downloading_background));
							mAppUpdater = new AppUpdater.Builder().setUrl(DYConstants.UPDATE_DOWNLOAD_API + updateObjList.get(maxIndex).getPackageName())
									//                        .setApkMD5("3df5b1c1d2bbd01b4a7ddb3f2722ccca")//支持MD5校验，如果缓存APK的MD5与此MD5相同，则直接取本地缓存安装，推荐使用MD5校验的方式
									.setVersionCode(updateObjList.get(maxIndex).getAppVersionCode())//支持versionCode校验，设置versionCode之后，新版本versionCode相同的apk只下载一次,优先取本地缓存,推荐使用MD5校验的方式
									.setFilename(updateObjList.get(maxIndex).getPackageName() + ".apk").setVibrate(true).build(mContext.get());
							mAppUpdater.setHttpManager(OkHttpManager.getInstance()).start();
							AppDialog.INSTANCE.dismissDialog();
						}
					});
					AppDialog.INSTANCE.showDialog(mContext.get(), view, 0.5f);
					break;
			}
			return false;
		}
	});

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
			//mAdapter.setData(Matisse.obtainResult(data), Matisse.obtainPathResult(data));
			Log.e("OnActivityResult ", String.valueOf(Matisse.obtainOriginalState(data)));
		}
	}

	@Override
	protected void onPause () {
		if (isDebug)
			Log.e(TAG, "onPause: ");

		if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {
			mUvcCameraHandler.stopTemperaturing();
			if (BuildConfig.DEBUG)
				Log.e(TAG, "onPause: 停止温度回调");
		}
		mSensorManager.unregisterListener(sensorEventListener, mAccelerometerSensor);
		mSensorManager.unregisterListener(sensorEventListener, mMagneticSensor);
		//解决去图库 拔出机芯闪退 2022年3月24日11:03:49
		//		if (mUvcCameraHandler!=null){
		//			mUvcCameraHandler.stopPreview();
		//		}
		//		mDataBinding.textureViewPreviewActivity.onResume();
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
		//		if (mDataBinding.textureViewPreviewActivity!=null)
		//		mDataBinding.textureViewPreviewActivity.onPause();
		//		if (mUvcCameraHandler!=null){
		//			mUvcCameraHandler.stopTemperaturing();
		//		}
		//		if (mDataBinding.textureViewPreviewActivity != null){
		//			mDataBinding.textureViewPreviewActivity.onPause();
		//		}
		if (mUvcCameraHandler != null) {
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
		if (mUvcCameraHandler != null) {
			mUvcCameraHandler.release();
			mUvcCameraHandler = null;
		}
		if (mUsbMonitor != null) {
			mUsbMonitor.destroy();
			mUsbMonitor = null;
		}
		if (isDebug)
			Log.e(TAG, "onDestroy: ");
		super.onDestroy();

	}
	private int sensorCount = 0;
	private SensorEventListener sensorEventListener = new SensorEventListener() {
		@Override
		public void onSensorChanged (SensorEvent event) {
			if (sensorCount < 5){
				sensorCount ++;
			}else if (event.sensor.getStringType().equals(Sensor.STRING_TYPE_MAGNETIC_FIELD)){
				Log.e(TAG, "onSensorChanged: Sensor.STRING_TYPE_MAGNETIC_FIELD");
				float[] values = event.values;
				//values[0]：方位角，手机绕着Z轴旋转的角度。0表示正北(North)，90表示正东(East)，
				//180表示正南(South)，270表示正西(West)。假如values[0]的值刚好是这四个值的话，
				//并且手机沿水平放置的话，那么当前手机的正前方就是这四个方向，可以利用这一点来
				//写一个指南针。
				Log.e(TAG, "====方位角: ===" + values[0]);
				//			values[1]：倾斜角，手机翘起来的程度，当手机绕着x轴倾斜时该值会发生变化。取值
				//			范围是[-180,180]之间。假如把手机放在桌面上，而桌面是完全水平的话，values1的则应该
				//			是0，当然很少桌子是绝对水平的。从手机顶部开始抬起，直到手机沿着x轴旋转180(此时屏幕
				//					乡下水平放在桌面上)。在这个旋转过程中，values[1]的值会从0到-180之间变化，即手机抬起
				//			时，values1的值会逐渐变小，知道等于-180；而加入从手机底部开始抬起，直到手机沿着x轴
				//			旋转180度，此时values[1]的值会从0到180之间变化。我们可以利用value[1]的这个特性结合
				//			value[2]来实现一个平地尺。
				Log.e(TAG, "====倾斜角: ===" + values[1]);
				//			value[2]：滚动角，沿着Y轴的滚动角度，取值范围为：[-90,90]，假设将手机屏幕朝上水平放在
				//			桌面上，这时如果桌面是平的，values2的值应为0。将手机从左侧逐渐抬起，values[2]的值将
				//			逐渐减小，知道垂直于手机放置，此时values[2]的值为-90，从右侧则是0-90；加入在垂直位置
				//			时继续向右或者向左滚动，values[2]的值将会继续在-90到90之间变化。
				Log.e(TAG, "====滚动角: ===" + values[2]);
				sensorCount = 0;
			}
//			else if (event.sensor.getStringType().equals(Sensor.STRING_TYPE_ACCELEROMETER)){
//				Log.e(TAG, "onSensorChanged: Sensor.STRING_TYPE_ACCELEROMETER");
//				float[] values = event.values;
//				//values[0]：方位角，手机绕着Z轴旋转的角度。0表示正北(North)，90表示正东(East)，
//				//180表示正南(South)，270表示正西(West)。假如values[0]的值刚好是这四个值的话，
//				//并且手机沿水平放置的话，那么当前手机的正前方就是这四个方向，可以利用这一点来
//				//写一个指南针。
//				Log.e(TAG, "====方位角: ===" + values[0]);
//				//			values[1]：倾斜角，手机翘起来的程度，当手机绕着x轴倾斜时该值会发生变化。取值
//				//			范围是[-180,180]之间。假如把手机放在桌面上，而桌面是完全水平的话，values1的则应该
//				//			是0，当然很少桌子是绝对水平的。从手机顶部开始抬起，直到手机沿着x轴旋转180(此时屏幕
//				//					乡下水平放在桌面上)。在这个旋转过程中，values[1]的值会从0到-180之间变化，即手机抬起
//				//			时，values1的值会逐渐变小，知道等于-180；而加入从手机底部开始抬起，直到手机沿着x轴
//				//			旋转180度，此时values[1]的值会从0到180之间变化。我们可以利用value[1]的这个特性结合
//				//			value[2]来实现一个平地尺。
//				Log.e(TAG, "====倾斜角: ===" + values[1]);
//				//			value[2]：滚动角，沿着Y轴的滚动角度，取值范围为：[-90,90]，假设将手机屏幕朝上水平放在
//				//			桌面上，这时如果桌面是平的，values2的值应为0。将手机从左侧逐渐抬起，values[2]的值将
//				//			逐渐减小，知道垂直于手机放置，此时values[2]的值为-90，从右侧则是0-90；加入在垂直位置
//				//			时继续向右或者向左滚动，values[2]的值将会继续在-90到90之间变化。
//				Log.e(TAG, "====滚动角: ===" + values[2]);
//			}
//			}
		}

		@Override
		public void onAccuracyChanged (Sensor sensor, int accuracy) {
			//			Log.e(TAG, "onAccuracyChanged: "+accuracy);
		}
	};

	@Override
	protected void onResume () {
		if (isDebug)
			Log.e(TAG, "onResume: ");
		if (mUsbMonitor != null && !mUsbMonitor.isRegistered()) {
			mUsbMonitor.register();
		}
		if (mSensorManager != null) {
			mSensorManager.registerListener(sensorEventListener, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
			mSensorManager.registerListener(sensorEventListener, mMagneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

		language = sp.getInt(DYConstants.LANGUAGE_SETTING, -1);
		switch (language) {
			case -1:
				if (locale_language.equals("zh")) {
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING, 0).apply();
				} else {
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING, 1).apply();
				}
				break;
			case 0:
				sp.edit().putInt(DYConstants.LANGUAGE_SETTING, 0).apply();
				configuration.locale = Locale.SIMPLIFIED_CHINESE;
				configuration.setLayoutDirection(Locale.SIMPLIFIED_CHINESE);
				getResources().updateConfiguration(configuration, metrics);
				break;
			default:
				sp.edit().putInt(DYConstants.LANGUAGE_SETTING, 1).apply();
				configuration.locale = Locale.ENGLISH;
				configuration.setLayoutDirection(Locale.ENGLISH);
				getResources().updateConfiguration(configuration, metrics);
				break;
		}
		super.onResume();
	}

	private USBMonitor.OnDeviceConnectListener onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
		@Override
		public void onAttach (UsbDevice device) {
			//			if (isDebug)Log.e(TAG, "DD  onAttach: "+ device.toString());
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run () {
					if (isDebug)
						Log.e(TAG, "检测到设备========");
					if (mUsbMonitor != null)
						mUsbMonitor.requestPermission(device);
				}
			}, 100);
		}

		@Override
		public void onConnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
			//			if (isDebug)Log.e(TAG, "onConnect:  SN ========================= 》 " + device.getSerialNumber());
			mVid = device.getVendorId();
			mPid = device.getProductId();

			if (mUvcCameraHandler == null || mUvcCameraHandler.isReleased()) {
				mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(), mDataBinding.textureViewPreviewActivity, 1, 384, 292, 1, null, 0);
			}

			mUvcCameraHandler.open(ctrlBlock);
			//			mDataBinding.textureViewPreviewActivity.onResume();
			startPreview();

			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run () {
					setValue(UVCCamera.CTRL_ZOOM_ABS, DYConstants.CAMERA_DATA_MODE_8004);//切换数据输出8004原始8005yuv,80ff保存
				}
			}, 300);

			timerEveryTime = new Timer();
			timerEveryTime.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run () {
					setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);//每隔一分钟打一次快门
					if (mUvcCameraHandler != null)
						mUvcCameraHandler.whenShutRefresh();
					if (isDebug)
						Log.e(TAG, "每隔60s执行一次操作");
				}
			}, 500, 600000);
		}

		@Override
		public void onDettach (UsbDevice device) {
			//				mUvcCameraHandler.close();
			if (isDebug)
				Log.e(TAG, "DD  onDetach: ");
			runOnUiThread(() -> {
				mDataBinding.dragTempContainerPreviewFragment.setBackgroundColor(getColor(R.color.bg_preview_otg_unconnect));
				mDataBinding.dragTempContainerPreviewFragment.setConnect(false);
				mDataBinding.dragTempContainerPreviewFragment.invalidate();
				onPause();
				onStop();
			});
		}

		@Override
		public void onDisconnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
			if (isDebug)
				Log.e(TAG, " DD == onDisconnect: ");
			runOnUiThread(new Runnable() {
				@Override
				public void run () {
					//断开连接之时, 恢复UI
					mDataBinding.toggleAreaCheck.setSelected(false);
					mDataBinding.toggleHighTempAlarm.setSelected(false);

					mDataBinding.customSeekbarPreviewFragment.setWidgetMode(0);
					mDataBinding.customSeekbarPreviewFragment.setPalette(sp.getInt(DYConstants.PALETTE_NUMBER, 1) - 1);
					mDataBinding.customSeekbarPreviewFragment.invalidate();
					mDataBinding.toggleFixedTempBar.setSelected(false);
					if (mUvcCameraHandler != null) {
						mUvcCameraHandler.disWenKuan();
						mUvcCameraHandler.fixedTempStripChange(false);
					}
				}
			});
			mVid = 0;
			mPid = 0;
			if (mUvcCameraHandler != null) {
				mUvcCameraHandler.stopTemperaturing();
				mUvcCameraHandler.close();
			}
		}

		@Override
		public void onCancel (UsbDevice device) {
			if (isDebug)
				Log.e(TAG, "DD  onCancel: ");
		}
	};

	/**
	 * 打开连接 调用预览图像的设置
	 */
	private void startPreview () {
		if (isDebug)
			Log.e(TAG, "startPreview: ============================");
		//				if (isDebug)Log.e(TAG, "startPreview: flPreview  width == " + mDataBinding.flPreview.getMeasuredWidth()
		//						+ " height == " + mDataBinding.flPreview.getMeasuredHeight());
		stt = new Surface(mDataBinding.textureViewPreviewActivity.getSurfaceTexture());

		mTextureViewWidth = mDataBinding.textureViewPreviewActivity.getWidth();
		mTextureViewHeight = mDataBinding.textureViewPreviewActivity.getHeight();
		//		if (isDebug)Log.e(TAG,"height =="+ mTextureViewHeight + " width==" + mTextureViewWidth);
		mDataBinding.textureViewPreviewActivity.setFrameBitmap(highTempBt, lowTempBt, centerTempBt, normalPointBt, DensityUtil.dp2px(mContext.get(), 30));

		mDataBinding.textureViewPreviewActivity.iniTempBitmap(mTextureViewWidth, mTextureViewHeight);//初始化画板的值，是控件的像素的宽高
		mDataBinding.textureViewPreviewActivity.setVidPid(mVid, mPid);//设置vid  pid
		mDataBinding.textureViewPreviewActivity.initTempFontSize(mFontSize);
		mDataBinding.textureViewPreviewActivity.setTinyCCorrection(sp.getFloat(DYConstants.setting_correction, 0.0f));//设置vid  pid
		mDataBinding.textureViewPreviewActivity.setDragTempContainer(mDataBinding.dragTempContainerPreviewFragment);
		mDataBinding.customSeekbarPreviewFragment.setmThumbListener(new MyCustomRangeSeekBar.ThumbListener() {
			@Override
			public void thumbChanged (float maxPercent, float minPercent, float maxValue, float minValue) {
				maxValue = maxValue - sp.getFloat(DYConstants.setting_correction, 0.0f);
				minValue = minValue - sp.getFloat(DYConstants.setting_correction, 0.0f);
				if (mUvcCameraHandler != null && !Float.isNaN(maxValue) && !Float.isNaN(minValue))
					mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent, maxValue, minValue);
			}

			@Override
			public void onUpMinThumb (float maxPercent, float minPercent, float maxValue, float minValue) {
				//				if (isDebug)Log.e(TAG, "onUpMinThumb: 0-100 percent " + maxPercent + " min == > " +  minPercent);
				//				if (isDebug)Log.e(TAG, "onUpMinThumb: value " + maxValue + " min == > " +  minValue);
				if (maxPercent >= 100 && minPercent <= 0) {
					if (mUvcCameraHandler != null && mUvcCameraHandler.isOpened())
						mUvcCameraHandler.disWenKuan();
				}
			}

			@Override
			public void onUpMaxThumb (float maxPercent, float minPercent, float maxValue, float minValue) {
				//				if (isDebug)Log.e(TAG, "onUpMaxThumb: 0-100 percent " + maxPercent + " min == > " +  minPercent);
				//				if (isDebug)Log.e(TAG, "onUpMaxThumb: value " + maxValue + " min == > " +  minValue);
				if (maxPercent >= 100 && minPercent <= 0) {
					if (mUvcCameraHandler != null && mUvcCameraHandler.isOpened())
						mUvcCameraHandler.disWenKuan();
				}
			}

			@Override
			public void onMinMove (float maxPercent, float minPercent, float maxValue, float minValue) {
				if (isDebug)
					Log.e(TAG, "onMinMove: 0-100 percent " + maxPercent + " min == > " + minPercent);
				maxValue = maxValue - sp.getFloat(DYConstants.setting_correction, 0.0f);
				minValue = minValue - sp.getFloat(DYConstants.setting_correction, 0.0f);
				if (isDebug)
					Log.e(TAG, "onMinMove: value == >" + maxValue + " min == > " + minValue);
				if (mUvcCameraHandler != null && !Float.isNaN(maxValue) && !Float.isNaN(minValue))
					mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent, maxValue, minValue);
			}

			@Override
			public void onMaxMove (float maxPercent, float minPercent, float maxValue, float minValue) {
				//				if (isDebug)Log.e(TAG, "onMaxMove: 0-100 percent" + maxPercent + " min == > " +  minPercent);
				maxValue = maxValue - sp.getFloat(DYConstants.setting_correction, 0.0f);
				minValue = minValue - sp.getFloat(DYConstants.setting_correction, 0.0f);
				if (isDebug)
					Log.e(TAG, "onMaxMove: value == > " + maxValue + " min == > " + minValue);
				// && maxValue != Float.NaN && minValue != Float.NaN
				if (mUvcCameraHandler != null && !Float.isNaN(maxValue) && !Float.isNaN(minValue))
					mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent, maxValue, minValue);
			}
		});

		paletteType = sp.getInt(DYConstants.PALETTE_NUMBER, 1);
		mUvcCameraHandler.PreparePalette(palettePath, paletteType);
		mUvcCameraHandler.setAreaCheck(0);

		//是否进行温度的绘制
		isTempShow = 0;
		mUvcCameraHandler.tempShowOnOff(isTempShow);//是否显示绘制的温度 0不显示，1显示。最终调用的是UVCCameraTextureView的绘制线程。

		mUvcCameraHandler.startPreview(stt);
		//tinyC 暂时关闭 温度回调功能
		mUvcCameraHandler.startTemperaturing();//温度回调
		//		mDataBinding.tvPreviewHint.setVisibility(View.INVISIBLE);

		mDataBinding.dragTempContainerPreviewFragment.setBackgroundColor(getColor(R.color.bg_preview_otg_connect));
		mDataBinding.dragTempContainerPreviewFragment.setConnect(true);
		mDataBinding.dragTempContainerPreviewFragment.invalidate();

		//2022年3月27日12:58:52 吴长城  添加预览的高、低、中心温度
		mDataBinding.textureViewPreviewActivity.openFeaturePoints(0);
		mDataBinding.textureViewPreviewActivity.openFeaturePoints(1);
		mDataBinding.textureViewPreviewActivity.openFeaturePoints(2);
	}

	private int setValue (final int flag, final int value) {//设置机芯参数,调用JNI层
		return mUvcCameraHandler != null ? mUvcCameraHandler.setValue(flag, value) : 0;
	}

	private static final int REQUEST_CODE_UNKNOWN_APP = 10085;

	//	private void installApk (String path) {
	//		File file = new File(path);
	//		if (file.exists()) {
	//			Intent installApkIntent = new Intent();
	//			installApkIntent.setAction(Intent.ACTION_VIEW);
	//			installApkIntent.addCategory(Intent.CATEGORY_DEFAULT);
	//			installApkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	//			//适配8.0需要有权限
	//			//			Log.e(TAG, "installApk:  +Build.VERSION.SDK_INT  " + Build.VERSION.SDK_INT);
	//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	//				boolean hasInstallPermission = getPackageManager().canRequestPackageInstalls();
	//				//				Log.e(TAG, "installApk:  +  " + hasInstallPermission);
	//				if (hasInstallPermission) {
	//					//安装应用
	//					installApkIntent.setDataAndType(FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".FileProvider", file), "application/vnd.android.package-archive");
	//					installApkIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	//					if (getPackageManager().queryIntentActivities(installApkIntent, 0).size() > 0) {
	//						startActivity(installApkIntent);
	//					}
	//				} else {
	//					//跳转至“安装未知应用”权限界面，引导用户开启权限
	//					Uri selfPackageUri = Uri.parse("package:" + this.getPackageName());
	//					Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, selfPackageUri);
	//					startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP);
	//				}
	//			} else {
	//				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
	//					installApkIntent.setDataAndType(FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".FileProvider", file), "application/vnd.android.package-archive");
	//					installApkIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	//				} else {
	//					installApkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
	//				}
	//				if (getPackageManager().queryIntentActivities(installApkIntent, 0).size() > 0) {
	//					startActivity(installApkIntent);
	//				}
	//			}
	//			//			file.delete();
	//		}
	//	}


	/**
	 * 设置机芯参数 为默认值
	 */
	private int toSettingDefault () {
		Handler handler0 = new Handler();
		defaultSettingReturn = -1;
		if (mPid == 1 && mVid == 5396) {
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					defaultSettingReturn = sendS0Order(DYConstants.SETTING_CORRECTION_DEFAULT_VALUE, DYConstants.SETTING_CORRECTION_INT);
				}
			}, 0);

			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					defaultSettingReturn = sendS0Order(DYConstants.SETTING_EMITTANCE_DEFAULT_VALUE, DYConstants.SETTING_EMITTANCE_INT);
				}
			}, 150);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					defaultSettingReturn = sendS0Order(DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE, DYConstants.SETTING_HUMIDITY_INT);
				}
			}, 300);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					defaultSettingReturn = sendS0Order(DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE, DYConstants.SETTING_ENVIRONMENT_INT);
				}
			}, 450);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					defaultSettingReturn = sendS0Order(DYConstants.SETTING_REFLECT_DEFAULT_VALUE, DYConstants.SETTING_REFLECT_INT);
				}
			}, 600);
		} else if (mPid == 22592 && mVid == 3034) {
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					//					result = sendS0Order(DYConstants.SETTING_CORRECTION_DEFAULT_VALUE,DYConstants.SETTING_CORRECTION_INT);
					defaultSettingReturn = mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, DYConstants.SETTING_EMITTANCE_DEFAULT_VALUE, 3);
				}
			}, 0);
			//S0的湿度，对应大气透过率
			//			handler0.postDelayed(new Runnable() {
			//				@Override
			//				public void run () {
			//					//					result = sendS0Order(DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE,DYConstants.SETTING_HUMIDITY_INT);
			//					defaultSettingReturn = mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE, 4);
			//				}
			//			}, 200);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					//					result = sendS0Order(DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE,DYConstants.SETTING_ENVIRONMENT_INT);
					defaultSettingReturn = mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE, 2);
				}
			}, 400);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					//					result = sendS0Order(DYConstants.SETTING_REFLECT_DEFAULT_VALUE,DYConstants.SETTING_REFLECT_INT);
					defaultSettingReturn = mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, DYConstants.SETTING_REFLECT_DEFAULT_VALUE, 1);
				}
			}, 600);
		}
		return defaultSettingReturn;
	}

	/**
	 * 切换语言
	 *
	 * @param type
	 */
	private void toSetLanguage (int type) {//切换语言
		Locale locale;
		Context context = DYTApplication.getInstance();
		if (type == 0) {
			locale = Locale.SIMPLIFIED_CHINESE;
			LanguageUtils.saveAppLocaleLanguage(locale.toLanguageTag());
		} else if (type == 1) {
			locale = Locale.US;
			LanguageUtils.saveAppLocaleLanguage(locale.toLanguageTag());
		} else {
			return;
		}
		if (LanguageUtils.isSimpleLanguage(context, locale)) {
			showToast(R.string.toast_select_same_language);
			return;
		}
		LanguageUtils.updateLanguage(context, locale);//更新语言参数
		Intent intent = new Intent(context, PreviewActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		context.startActivity(intent);
		//		finish();
		//		android.os.Process.killProcess(android.os.Process.myPid());
		//		System.exit(0);
	}


	//修改S0机芯参数
	public class SendCommand {
		int psitionAndValue0 = 0, psitionAndValue1 = 0, psitionAndValue2 = 0, psitionAndValue3 = 0;
		int result = -1;

		public int sendFloatCommand (int position, byte value0, byte value1, byte value2, byte value3, int interval0, int interval1, int interval2, int interval3, int interval4) {
			psitionAndValue0 = (position << 8) | (0x000000ff & value0);
			Handler handler0 = new Handler();

			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					result = mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
				}
			}, interval0);

			psitionAndValue1 = ((position + 1) << 8) | (0x000000ff & value1);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					result = mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue1);
				}
			}, interval1);

			psitionAndValue2 = ((position + 2) << 8) | (0x000000ff & value2);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					result = mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue2);
				}
			}, interval2);

			psitionAndValue3 = ((position + 3) << 8) | (0x000000ff & value3);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					result = mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue3);
				}
			}, interval3);

			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					mUvcCameraHandler.whenShutRefresh();
				}
			}, interval4);

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
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
				}
			}, interval0);

			psitionAndValue1 = ((position + 1) << 8) | (0x000000ff & value1);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue1);
				}
			}, interval1);

			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					mUvcCameraHandler.whenShutRefresh();
				}
			}, interval2);
		}

		public void sendByteCommand (int position, byte value0, int interval0) {
			psitionAndValue0 = (position << 8) | (0x000000ff & value0);
			Handler handler0 = new Handler();
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
				}
			}, interval0);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run () {
					mUvcCameraHandler.whenShutRefresh();
				}
			}, interval0 + 20);
		}
	}

	@Override
	protected int bindingLayout () {
		return R.layout.activity_preview;
	}

	@Override
	protected void initView () {
		sp = mContext.get().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		configuration = getResources().getConfiguration();
		metrics = getResources().getDisplayMetrics();

		locale_language = Locale.getDefault().getLanguage();
		//		if(isDebug)Log.e(TAG, "initView: ===============locale_language==============" + locale_language);
		language = sp.getInt(DYConstants.LANGUAGE_SETTING, -1);
		switch (language) {
			case -1:
				if (locale_language.equals("zh")) {
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING, 0).apply();
				} else {
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING, 1).apply();
				}
				break;
			case 0:
				sp.edit().putInt(DYConstants.LANGUAGE_SETTING, 0).apply();
				configuration.locale = Locale.SIMPLIFIED_CHINESE;
				configuration.setLayoutDirection(Locale.SIMPLIFIED_CHINESE);
				getResources().updateConfiguration(configuration, metrics);
				break;
			default:
				sp.edit().putInt(DYConstants.LANGUAGE_SETTING, 1).apply();
				configuration.locale = Locale.ENGLISH;
				configuration.setLayoutDirection(Locale.ENGLISH);
				getResources().updateConfiguration(configuration, metrics);
				break;
		}

		if (sp.getInt(DYConstants.FIRST_RUN, 1) == 0) {
			isFirstRun = true;
			sp.edit().putInt(DYConstants.FIRST_RUN, 1).apply();
		}
		if (isFirstRun) {//第一次打开应用
			//默认不打开音频录制
			sp.edit().putInt(DYConstants.RECORD_AUDIO_SETTING, 1).apply();
		}

		highTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_high);
		lowTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_low);
		centerTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_center);
		normalPointBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_main_preview_measuretemp_point);

		DisplayMetrics dm = getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		mSendCommand = new SendCommand();

		mSensorManager = (SensorManager) mContext.get().getSystemService(Context.SENSOR_SERVICE);
		mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

//		//获取当前设备支持的传感器列表
//		List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//		StringBuilder sb = new StringBuilder();
//		sb.append("当前设备支持传感器数：" + allSensors.size() + "   分别是：\n\n");
//		for(Sensor s:allSensors){
//			switch (s.getType()){
//				case Sensor.TYPE_ACCELEROMETER:
//					sb.append("加速度传感器(Accelerometer sensor)" + "\n");
//					break;
//				case Sensor.TYPE_GYROSCOPE:
//					sb.append("陀螺仪传感器(Gyroscope sensor)" + "\n");
//					break;
//				case Sensor.TYPE_LIGHT:
//					sb.append("光线传感器(Light sensor)" + "\n");
//					break;
//				case Sensor.TYPE_MAGNETIC_FIELD:
//					sb.append("磁场传感器(Magnetic field sensor)" + "\n");
//					break;
//				case Sensor.TYPE_ORIENTATION:
//					sb.append("方向传感器(Orientation sensor)" + "\n");
//					break;
//				case Sensor.TYPE_PRESSURE:
//					sb.append("气压传感器(Pressure sensor)" + "\n");
//					break;
//				case Sensor.TYPE_PROXIMITY:
//					sb.append("距离传感器(Proximity sensor)" + "\n");
//					break;
//				case Sensor.TYPE_TEMPERATURE:
//					sb.append("温度传感器(Temperature sensor)" + "\n");
//					break;
//				default:
//					sb.append("其他传感器" + "\n");
//					break;
//			}
//			sb.append("设备名称：" + s.getName() + "\n 设备版本：" + s.getVersion() + "\n 供应商："
//					+ s.getVendor() + "\n\n");
//		}
//		Log.e("TAG","sb.toString()----:"+sb.toString());

		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).onExplainRequestReason(new ExplainReasonCallback() {
			@Override
			public void onExplainReason (@NonNull ExplainScope scope, @NonNull List<String> deniedList) {
				scope.showRequestReasonDialog(deniedList, getResources().getString(R.string.toast_base_permission_explain), getResources().getString(R.string.confirm), getResources().getString(R.string.cancel));
			}
		}).onForwardToSettings(new ForwardToSettingsCallback() {
			@Override
			public void onForwardToSettings (@NonNull ForwardScope scope, @NonNull List<String> deniedList) {
				scope.showForwardToSettingsDialog(deniedList, getResources().getString(R.string.toast_base_permission_tosetting), getResources().getString(R.string.confirm), getResources().getString(R.string.cancel));
			}
		}).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted) {//基本权限被授予之后才能初始化监听器。
					mFontSize = FontUtils.adjustFontSize(screenWidth, screenHeight);//
					//					if (isDebug)Log.e(TAG, "onResult: mFontSize ==========> " + mFontSize);
					//					Log.e(TAG, "initView:  ==444444= " + System.currentTimeMillis());
					AssetCopyer.copyAllAssets(DYTApplication.getInstance(), mContext.get().getExternalFilesDir(null).getAbsolutePath());
					//		Log.e(TAG,"===========getExternalFilesDir=========="+this.getExternalFilesDir(null).getAbsolutePath());
					palettePath = mContext.get().getExternalFilesDir(null).getAbsolutePath();
					//					Log.e(TAG, "initView:  ==333333= " + System.currentTimeMillis());
					CreateBitmap createBitmap = new CreateBitmap();
					try {
						tiehong = createBitmap.GenerateBitmap(mContext.get(), "1.dat");
						caihong = createBitmap.GenerateBitmap(mContext.get(), "2.dat");
						hongre = createBitmap.GenerateBitmap(mContext.get(), "3.dat");
						heire = createBitmap.GenerateBitmap(mContext.get(), "4.dat");
						baire = createBitmap.GenerateBitmap(mContext.get(), "5.dat");
						lenglan = createBitmap.GenerateBitmap(mContext.get(), "6.dat");
					} catch (IOException e) {
						e.printStackTrace();
					}
					List<Bitmap> bitmaps = new ArrayList<>();
					bitmaps.add(tiehong);
					bitmaps.add(caihong);
					bitmaps.add(hongre);
					bitmaps.add(heire);
					bitmaps.add(baire);
					bitmaps.add(lenglan);
					//		Log.e(TAG, "initView:  ==222222= " + System.currentTimeMillis());
					mDataBinding.customSeekbarPreviewFragment.setmProgressBarSelectBgList(bitmaps);
					//		Log.e(TAG, "initView: sp.get Palette_Number = " + sp.getInt(DYConstants.PALETTE_NUMBER,0));
					mDataBinding.customSeekbarPreviewFragment.setPalette(sp.getInt(DYConstants.PALETTE_NUMBER, 1) - 1);
					initListener();

					initRecord();

					mDataBinding.dragTempContainerPreviewFragment.setmSeekBar(mDataBinding.customSeekbarPreviewFragment);
					mDataBinding.dragTempContainerPreviewFragment.setTempSuffix(sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0));

					mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(), mDataBinding.textureViewPreviewActivity, 1, 384, 292, 1, null, 0);

					fl = mDataBinding.flPreview;

					mUsbMonitor = new USBMonitor(mContext.get(), onDeviceConnectListener);
				} else {
					showToast(getResources().getString(R.string.toast_dont_have_permission));
				}
			}
		});
	}


	/**
	 * 初始化界面的监听器
	 */
	private void initListener () {
		//测试的 监听器
		mDataBinding.btTest01.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				//************************检测当前语言设置********************************
				//				if (isDebug){
				//					Log.e(TAG, "onClick: " + Locale.getDefault().getLanguage());
				//					Log.e(TAG, "onClick: " + sp.getInt(DYConstants.LANGUAGE_SETTING,8));
				//				}
				boolean dd = false;
				if (jni_status == 0) {
					dd = mUvcCameraHandler.javaSendJniOrder(jni_status);
					jni_status = 1;
				} else {
					dd = mUvcCameraHandler.javaSendJniOrder(jni_status);
					jni_status = 0;
				}
				Log.e(TAG, "onClick: " + dd);

				//*****************************************************************
				//				Button bt = null;
				//				bt.setText("111");
				//				new Thread(new Runnable() {
				//					@Override
				//					public void run () {
				//						AssetManager am = getAssets();
				//						InputStream is;
				//						try {
				//							is = am.open("tau_H.bin");
				//							int length = is.available();
				//							tau_data = new byte[length];
				//							if (is.read(tau_data) != length) {
				//								Log.d(TAG, "read file fail ");
				//							}
				//							Log.d(TAG, "read file lenth " + length);
				//						} catch (IOException e) {
				//							e.printStackTrace();
				//						}
				//						//						for (byte data : tau_data) {
				//						//							Log.i(TAG, "run: tau_data => " + data);
				//						//						}
				////						String binPath = mContext.get().getExternalFilesDir(null).getAbsolutePath() + File.separator+ "tau_H.bin";
				////						Log.e(TAG, "run:binPath =  "+ binPath);
				////						File file = new File(binPath);
				////						Log.e(TAG, "run: "+file.exists());
				////						char [] path = binPath.toCharArray();
				////						Log.e(TAG, "run:  path [] =" + Arrays.toString(path));
				////						char [] data = TinyCUtils.toChar(tau_data);
				//						short[] shortArrayData = TinyCUtils.byte2Short(tau_data);
				//						float hum = 100;
				//						float oldTemp = 100;
				//						float distance = 100;
				////						char [] re = new char[4];
				////						Libirtemp.read_tau(data,hum,oldTemp, distance,re);
				////						Log.e(TAG, "run: ======re =" + Arrays.toString(re));
				//						short value = TinyCUtils.getLUT(oldTemp, hum, distance, shortArrayData);
				//						Log.e(TAG, "run: ======value =" + value);
				////						Log.e(TAG, "run:  a 1= > " + (int) (re[0]));
				////						Log.e(TAG, "run:  a 2= > " + (int) (re[1]));
				////						Log.e(TAG, "run:  a 3= > " + (int) (re[2]));
				////						Log.e(TAG, "run:  a 4= > " + (int) (re[3]));
				////						for (int a : re) {
				////							Log.e(TAG, "run:  a = > " + (int) a);
				////						}
				////						runOnUiThread(new Runnable() {
				////							@Override
				////							public void run () {
				////								Toast.makeText(PreviewActivity.this, "read nuc success" + tau_data.length, Toast.LENGTH_SHORT).show();
				////							}
				////						});
				//					}
				//				}).start();
				//				setValue(UVCCamera.CTRL_ZOOM_ABS,0x8000);

			}
		});
		//
		/**
		 * 超温警告 ， 预览层去绘制框， DragTempContainer 控件去播放声音
		 */
		mDataBinding.toggleHighTempAlarm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				mDataBinding.toggleHighTempAlarm.setSelected(!mDataBinding.toggleHighTempAlarm.isSelected());
				if (mDataBinding.toggleHighTempAlarm.isSelected()) {
					OverTempDialog dialog = new OverTempDialog(mContext.get(), sp.getFloat("overTemp", 0.0f), mDataBinding.dragTempContainerPreviewFragment.getTempSuffixMode());

					dialog.getWindow().setGravity(Gravity.CENTER);
					//					WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
					//					params.x =  mDataBinding.flPreview.getMeasuredWidth()/2 - DensityUtil.dp2px(mContext.get(), 50);
					dialog.setListener(new OverTempDialog.SetCompleteListener() {
						@Override
						public void onSetComplete (float setValue) {
							//							if (isDebug)Log.e(TAG, "onSetComplete: " + "confirm value = == > " + setValue  );
							mDataBinding.dragTempContainerPreviewFragment.openHighTempAlarm(setValue);
							mDataBinding.textureViewPreviewActivity.startTempAlarm(setValue);
							sp.edit().putFloat("overTemp", setValue).apply();
						}

						@Override
						public void onCancelListener () {
							//							Log.e(TAG, "onCancelListener: " + "cancel "  );
							mDataBinding.toggleHighTempAlarm.setSelected(false);
						}
					});
					dialog.setCancelable(false);
					dialog.show();
				} else {
					mDataBinding.textureViewPreviewActivity.stopTempAlarm();
					mDataBinding.dragTempContainerPreviewFragment.closeHighTempAlarm();
				}
			}
		});

		//切换色板
		mDataBinding.ivPreviewLeftPalette.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				//				if (PreviewActivity.this.isre()){
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_palette_choice, null);
				PopPaletteChoiceBinding popPaletteChoice = DataBindingUtil.bind(view);
				assert popPaletteChoice != null;
				popPaletteChoice.paletteLayoutTiehong.setOnClickListener(paletteChoiceListener);
				popPaletteChoice.paletteLayoutCaihong.setOnClickListener(paletteChoiceListener);
				popPaletteChoice.paletteLayoutHongre.setOnClickListener(paletteChoiceListener);
				popPaletteChoice.paletteLayoutHeire.setOnClickListener(paletteChoiceListener);
				popPaletteChoice.paletteLayoutBaire.setOnClickListener(paletteChoiceListener);
				popPaletteChoice.paletteLayoutLenglan.setOnClickListener(paletteChoiceListener);
				//				showPopWindows(view,20,10,20);
				allPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				allPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
				allPopupWindows.setHeight(mDataBinding.llContainerPreviewSeekbar.getHeight());
				allPopupWindows.setWidth(mDataBinding.llContainerPreviewSeekbar.getWidth());

				allPopupWindows.setFocusable(false);
				allPopupWindows.setOutsideTouchable(true);
				allPopupWindows.setTouchable(true);

				allPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, 0, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
				//				}
			}
		});

		//测温模式。绘制  温度模式 切换  弹窗
		mDataBinding.ivPreviewRightTempMode.setOnClickListener(v -> {
			//			if (isResumed()){
			View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_temp_mode_choice, null);
			PopTempModeChoiceBinding tempModeChoiceBinding = DataBindingUtil.bind(view);
			assert tempModeChoiceBinding != null;
			tempModeChoiceBinding.ivTempModeLine.setOnClickListener(tempModeCheckListener);
			tempModeChoiceBinding.ivTempModePoint.setOnClickListener(tempModeCheckListener);
			tempModeChoiceBinding.ivTempModeRectangle.setOnClickListener(tempModeCheckListener);

			PLRPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			PLRPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			PLRPopupWindows.setHeight(mDataBinding.llContainerPreviewSeekbar.getHeight());
			PLRPopupWindows.setWidth(mDataBinding.llContainerPreviewSeekbar.getWidth());

			PLRPopupWindows.setFocusable(false);
			//				popupWindow.setBackgroundDrawable(getResources().getDrawable(R.mipmap.temp_mode_bg_tempback));
			PLRPopupWindows.setOutsideTouchable(true);
			PLRPopupWindows.setTouchable(true);

			PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, 0, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			//			}
		});

		//高低中心点温度追踪 弹窗
		mDataBinding.toggleShowHighLowTemp.setOnCheckedChangeListener((buttonView, isChecked) -> {
			//				if (isResumed()){
			View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_highlowcenter_trace, null);
			PopHighlowcenterTraceBinding popHighlowcenterTraceBinding = DataBindingUtil.bind(view);
			assert popHighlowcenterTraceBinding != null;
			popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceHigh.setOnCheckedChangeListener(highLowCenterCheckListener);
			popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceLow.setOnCheckedChangeListener(highLowCenterCheckListener);
			popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceCenter.setOnCheckedChangeListener(highLowCenterCheckListener);

			int toggleState = mDataBinding.textureViewPreviewActivity.getFeaturePointsControl();
			popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceHigh.setChecked((toggleState & 0x0f00) > 0);
			popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceLow.setChecked((toggleState & 0x00f0) > 0);
			popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceCenter.setChecked((toggleState & 0x000f) > 0);

			PLRPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			PLRPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			PLRPopupWindows.setHeight(mDataBinding.llContainerPreviewSeekbar.getHeight());
			PLRPopupWindows.setWidth(mDataBinding.llContainerPreviewSeekbar.getWidth());

			PLRPopupWindows.setFocusable(false);
			PLRPopupWindows.setOutsideTouchable(true);
			PLRPopupWindows.setTouchable(true);

			PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, 0, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);

			//				}
		});

		//框内细查
		mDataBinding.toggleAreaCheck.setOnClickListener(v -> {
			if (mDataBinding.toggleFixedTempBar.isSelected()) {
				mDataBinding.toggleFixedTempBar.setSelected(false);
				mDataBinding.customSeekbarPreviewFragment.setWidgetMode(0);
				if (mUvcCameraHandler != null)
					mUvcCameraHandler.disWenKuan();
			}
			if (!mDataBinding.toggleAreaCheck.isSelected()) {
				mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewActivity.getWidth(), mDataBinding.textureViewPreviewActivity.getHeight());
				int[] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();

				//					Log.e(TAG, "onCheckedChanged: checked  ==== >  " + isChecked + " ==================" + Arrays.toString(areaData));
				if (mUvcCameraHandler == null)
					return;
				mUvcCameraHandler.setArea(areaData);
				mUvcCameraHandler.setAreaCheck(1);
			} else {//close
				if (mUvcCameraHandler == null)
					return;
				mUvcCameraHandler.setAreaCheck(0);
			}
			mDataBinding.toggleAreaCheck.setSelected(!mDataBinding.toggleAreaCheck.isSelected());
		});

		//固定温度条
		mDataBinding.toggleFixedTempBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				if (mDataBinding.toggleAreaCheck.isSelected()) {
					mDataBinding.toggleAreaCheck.setSelected(false);

					if (mUvcCameraHandler == null)
						return;
					mUvcCameraHandler.setAreaCheck(0);
				}
				mDataBinding.toggleFixedTempBar.setSelected(!mDataBinding.toggleFixedTempBar.isSelected());
				if (mDataBinding.toggleFixedTempBar.isSelected()) {
					mDataBinding.customSeekbarPreviewFragment.setWidgetMode(1);
				} else {
					mDataBinding.customSeekbarPreviewFragment.setWidgetMode(0);
					if (mUvcCameraHandler != null)
						mUvcCameraHandler.disWenKuan();
				}
				if (mUvcCameraHandler != null)
					mUvcCameraHandler.fixedTempStripChange(mDataBinding.toggleFixedTempBar.isSelected());
			}
		});

		//重置  按钮
		mDataBinding.ivPreviewLeftReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {
					//重置色板
					setPalette(0);
					//重置绘制界面
					mDataBinding.dragTempContainerPreviewFragment.clearAll();
					//关闭框内细查
					if (mDataBinding.toggleAreaCheck.isSelected()) {
						mDataBinding.toggleAreaCheck.setSelected(false);
						mUvcCameraHandler.setAreaCheck(0);
					}
					//关闭 超温警告
					mDataBinding.toggleHighTempAlarm.setSelected(false);
					mDataBinding.textureViewPreviewActivity.stopTempAlarm();
					mDataBinding.dragTempContainerPreviewFragment.closeHighTempAlarm();
					//关闭 固定温度条
					if (mDataBinding.toggleFixedTempBar.isSelected()) {
						mDataBinding.toggleFixedTempBar.setSelected(false);
						mDataBinding.customSeekbarPreviewFragment.setWidgetMode(0);
						mUvcCameraHandler.disWenKuan();
						mUvcCameraHandler.fixedTempStripChange(mDataBinding.toggleFixedTempBar.isSelected());
					}
					//打开高低中心测温
					mDataBinding.textureViewPreviewActivity.openFeaturePoints(0);
					mDataBinding.textureViewPreviewActivity.openFeaturePoints(1);
					mDataBinding.textureViewPreviewActivity.openFeaturePoints(2);

					//打挡  并 刷新温度对照表
					setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
					mUvcCameraHandler.whenShutRefresh();
				}
			}
		});
		//拍照按钮
		mDataBinding.ivPreviewLeftTakePhoto.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {
					String picPath = Objects.requireNonNull(MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".jpg")).toString();
					if (mUvcCameraHandler.captureStill(picPath))
						showToast(getResources().getString(R.string.toast_save_path) + picPath);
					//						if (isDebug)Log.e(TAG, "onResult: java path === "+ picPath);
				} else {
					showToast(getResources().getString(R.string.toast_need_connect_camera));
				}
			}
		});
		//录制 按钮
		mDataBinding.btPreviewLeftRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {//mUvcCameraHandler.isOpened()
					if (mDataBinding.btPreviewLeftRecord.isSelected() && mUvcCameraHandler.isRecording()) {//停止录制
						stopTimer();
						mUvcCameraHandler.stopRecording();
					} else if (!mDataBinding.btPreviewLeftRecord.isSelected() && !mUvcCameraHandler.isRecording() && mUvcCameraHandler.snRightIsPreviewing()) {//开始录制
						startTimer();
						mUvcCameraHandler.startRecording(sp.getInt(DYConstants.RECORD_AUDIO_SETTING, 1));
					} else {
						//						if (isDebug)Log.e(TAG, "Record Error: error record state !");
					}
					mDataBinding.btPreviewLeftRecord.setSelected(!mDataBinding.btPreviewLeftRecord.isSelected());
				} else {
					showToast(getResources().getString(R.string.toast_need_connect_camera));
				}
			}
		});
		//相册按钮
		mDataBinding.ivPreviewLeftGallery.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				if (!mUvcCameraHandler.snRightIsPreviewing()) {
					return;
				}
				if (mDataBinding.btPreviewLeftRecord.isSelected()) {
					showToast(getResources().getString(R.string.toast_is_recording));
					return;
				} else {
					mUvcCameraHandler.close();
					mUsbMonitor.unregister();
					EasyPhotos.createAlbum(PreviewActivity.this, false, false, GlideEngine.getInstance()).setFileProviderAuthority("com.dyt.wcc.dytpir.FileProvider").setCount(1000).setVideo(true).setGif(false).start(101);
				}
			}
		});

		//设置弹窗
		mDataBinding.ivPreviewRightSetting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				//打开设置第一步：获取机芯数据。
				if (mUvcCameraHandler == null)
					return;
				if (!mUvcCameraHandler.snRightIsPreviewing()) {
					return;
				}
				if (mUvcCameraHandler.isOpened()) {
					//					new Thread(new Runnable() {
					//						@Override
					//						public void run () {
					if (mPid == 1 && mVid == 5396) {
						getCameraParams();//
					} else if (mPid == 22592 && mVid == 3034) {
						getTinyCCameraParams();
					}
				}
				//					}).start();
				//				}
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_setting, null);

				PopSettingBinding popSettingBinding = DataBindingUtil.bind(view);
				assert popSettingBinding != null;
				popSettingBinding.tvCheckVersionInfo.setText(String.format(getString(R.string.setting_check_version_info), BuildConfig.VERSION_NAME));
				//设置单位
				popSettingBinding.tvCameraSettingReviseUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
				popSettingBinding.tvCameraSettingReflectUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
				popSettingBinding.tvCameraSettingFreeAirTempUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));

				if (mPid == 1 && mVid == 5396) {
					popSettingBinding.tvCameraSettingHumidity.setVisibility(View.VISIBLE);
				} else if (mPid == 22592 && mVid == 3034) {
					popSettingBinding.tvCameraSettingHumidity.setVisibility(View.GONE);
				}

				popSettingBinding.tvCheckVersion.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick (View v) {
						//点击之后 立即wifi 或者 移动数据 是否打开，给提示。  连接超时 也给提示
						//下载完成之后回调方法，保存文件，之后读取该文件夹下的zip文件。解压，安装apk，之后删除apk zip 文件
						//读取 更新info 的接口，获取信息，分割之后校验，是否存在更新。  如果存在，一个提示版本窗口，
						// (先检查本地是否存在同名文件，有则跳过下载直接安装)
						// 否则确认之后则去下载地址去下载，然后安装
						OkHttpClient client = new OkHttpClient();
						new Thread(new Runnable() {
							@Override
							public void run () {
								Request request = new Request.Builder().url(DYConstants.UPDATE_CHECK_INFO).get().build();
								try {
									Response response = client.newCall(request).execute();
									byte[] responseByte = response.body().bytes();
									String checkAPIData = new String(responseByte, StandardCharsets.UTF_8);
									//									if (isDebug)Log.i(TAG, "run:responseByte ==========>  " + checkAPIData);
									//分割 checkAPIData 得到具体信息
									String[] AppVersionNameList = checkAPIData.split(";");//切割
									updateObjList = new ArrayList<>(AppVersionNameList.length);
									//去掉apk
									for (int i = 0; i < AppVersionNameList.length; i++) {
										UpdateObj updateObj = new UpdateObj();
										AppVersionNameList[i] = AppVersionNameList[i].replace(".apk", "");
										updateObj.setPackageName(AppVersionNameList[i]);
										updateObjList.add(updateObj);
									}
									//									if (isDebug)Log.i(TAG, "run: =====AppVersionNameList====" + Arrays.toString(AppVersionNameList));
									String[] AppVersionCodeList = new String[AppVersionNameList.length];//保存app 版本号
									//筛选最大 VersionCode 的index
									int maxCode = 0;
									int currentVersionCode = 0;
									for (int i = 0; i < AppVersionNameList.length; i++) {
										AppVersionCodeList[i] = AppVersionNameList[i].split("_b")[1].split("_")[0];
										currentVersionCode = Integer.parseInt(AppVersionCodeList[i]);

										updateObjList.get(i).setAppVersionCode(currentVersionCode);
										updateObjList.get(i).setAppVersionName(AppVersionNameList[i].split("_v")[1].split("_b")[0]);

										if (maxCode == 0) {
											maxCode = currentVersionCode;
											maxIndex = 0;
										}
										if (maxCode < currentVersionCode) {
											maxCode = currentVersionCode;
											maxIndex = i;
										}
									}
									//									if (isDebug)Log.i(TAG, "run: 最大的 VersionCode 为： " + maxCode + " codeIndex = " + maxIndex + " 完成版本为： " + AppVersionNameList[maxIndex]);
									//									if (isDebug)Log.i(TAG, "run: ======AppVersionCodeList===" + Arrays.toString(AppVersionCodeList));
									int thisVersionCode = mContext.get().getPackageManager().getPackageInfo(mContext.get().getPackageName(), 0).versionCode;
									//									if (isDebug)Log.i(TAG, "run: versionName === 》" + thisVersionCode);
									//判断是否需要更新
									if (thisVersionCode < updateObjList.get(maxIndex).getAppVersionCode()) {
										Message message = mHandler.obtainMessage();
										message.what = MSG_CHECK_UPDATE;
										message.obj = updateObjList;
										mHandler.sendMessage(message);
									} else {//已经为最新版本
										runOnUiThread(new Runnable() {
											@Override
											public void run () {
												showToast(R.string.toast_already_latest_version);
											}
										});
									}
								} catch (IOException | PackageManager.NameNotFoundException e) {
									e.printStackTrace();
								}
							}
						}).start();
					}
				});
				//第二步：将获取的数据 展示在输入框内
				if (cameraParams != null) {
					popSettingBinding.etCameraSettingEmittance.setText(String.valueOf(cameraParams.get(DYConstants.setting_emittance)));//发射率 0-1

					popSettingBinding.etCameraSettingRevise.setText(String.valueOf(cameraParams.get(DYConstants.setting_correction)));//校正  -20 - 20
					popSettingBinding.etCameraSettingReflect.setText(String.valueOf((int) (cameraParams.get(DYConstants.setting_reflect) * 1)));//反射温度 -10-40
					popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf((int) (cameraParams.get(DYConstants.setting_environment) * 1)));//环境温度 -10 -40

					if (mPid == 1 && mVid == 5396) {
						popSettingBinding.etCameraSettingHumidity.setText(String.valueOf((int) (cameraParams.get(DYConstants.setting_humidity) * 100)));//湿度 0-100
						//湿度设置
						popSettingBinding.etCameraSettingHumidity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
							@Override
							public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
								if (actionId == EditorInfo.IME_ACTION_DONE) {
									if (TextUtils.isEmpty(v.getText().toString()))
										return true;
									int value = Integer.parseInt(v.getText().toString());
									if (value > 100 || value < 0) {
										showToast(getString(R.string.toast_range_int, 0, 100));
										return true;
									}
									float fvalue = value / 100.0f;
									if (mUvcCameraHandler != null) {
										if (mPid == 1 && mVid == 5396) {
											sendS0Order(fvalue, DYConstants.SETTING_HUMIDITY_INT);
										}
										//									else if (mPid == 22592 && mVid == 3034) {
										//										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, fvalue, 4);
										//									}
										popSettingBinding.etCameraSettingHumidity.setText(String.format(Locale.US, "%d", value));
										sp.edit().putFloat(DYConstants.setting_humidity, fvalue).apply();
										showToast(R.string.toast_complete_Humidity);
									}
									hideInput(v.getWindowToken());
								}
								return true;
							}
						});
					} else if (mPid == 22592 && mVid == 3034) {

					}

					//TODO 设置机芯的默认值;
					popSettingBinding.btSettingDefault.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick (View v) {
							toSettingDefault();
							if (mPid == 1 && mVid == 5396) {
								popSettingBinding.etCameraSettingHumidity.setText(String.format(Locale.CHINESE, "%s", (int) (100 * DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE)));
								sp.edit().putFloat(DYConstants.setting_humidity, DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE).apply();
							}
							if (true) {
								popSettingBinding.etCameraSettingReflect.setText(String.format(Locale.CHINESE, "%d", DYConstants.SETTING_REFLECT_DEFAULT_VALUE));
								popSettingBinding.etCameraSettingRevise.setText(String.format(Locale.CHINESE, "%s", DYConstants.SETTING_CORRECTION_DEFAULT_VALUE));
								popSettingBinding.etCameraSettingEmittance.setText(String.format(Locale.CHINESE, "%s", DYConstants.SETTING_EMITTANCE_DEFAULT_VALUE));
								popSettingBinding.etCameraSettingFreeAirTemp.setText(String.format(Locale.CHINESE, "%d", DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE));

								sp.edit().putFloat(DYConstants.setting_environment, DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE).apply();
								sp.edit().putFloat(DYConstants.setting_reflect, DYConstants.SETTING_REFLECT_DEFAULT_VALUE).apply();
								sp.edit().putFloat(DYConstants.setting_emittance, DYConstants.SETTING_EMITTANCE_DEFAULT_VALUE).apply();
								sp.edit().putFloat(DYConstants.setting_correction, DYConstants.SETTING_CORRECTION_DEFAULT_VALUE).apply();

								mDataBinding.textureViewPreviewActivity.setTinyCCorrection(DYConstants.SETTING_CORRECTION_DEFAULT_VALUE);
							}
						}
					});

					//发射率
					popSettingBinding.etCameraSettingEmittance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v.getText().toString()))
									return true;
								float value = Float.parseFloat(v.getText().toString());
								if (value > 1 || value < 0) {
									showToast(getString(R.string.toast_range_int, 0, 1));
									return true;
								}
								v.clearFocus();
								//								byte[] iputEm = new byte[4];
								//								ByteUtil.putFloat(iputEm,value,0);
								if (mUvcCameraHandler != null) {
									if (mPid == 1 && mVid == 5396) {
										sendS0Order(value, DYConstants.SETTING_EMITTANCE_INT);
										//										mSendCommand.sendFloatCommand(DYConstants.SETTING_EMITTANCE_INT, iputEm[0], iputEm[1], iputEm[2], iputEm[3],
										//												20, 40, 60, 80, 120);
										//										mUvcCameraHandler.startTemperaturing();
									} else if (mPid == 22592 && mVid == 3034) {
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, value, 3);
									}
									sp.edit().putFloat(DYConstants.setting_emittance, value).apply();
									showToast(R.string.toast_complete_Emittance);
								}
								hideInput(v.getWindowToken());
							}
							return true;
						}
					});
					//距离设置
					//					popSettingBinding.etCameraSettingDistance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					//						@Override
					//						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
					//							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
					//							if (actionId == EditorInfo.IME_ACTION_DONE){
					//								if (TextUtils.isEmpty(v.getText().toString()))return true;
					//								int value = Math.round(Float.parseFloat(v.getText().toString()));
					//								if (value > 5 || value < 0){
					//									showToast(getString(R.string.toast_range_int,0,5));
					//									return true;
					//								}
					//								byte[] bIputDi = new byte[4];
					//								ByteUtil.putInt(bIputDi,value,0);
					//								if (mUvcCameraHandler!= null) {
					//									if (mPid == 1 && mVid == 5396) {
					//										mSendCommand.sendShortCommand(5 * 4, bIputDi[0], bIputDi[1], 20, 40, 60);
					//									}
					//									sp.edit().putFloat(DYConstants.setting_distance,value).apply();
					//									showToast(R.string.toast_complete_Distance);
					//								}
					//								hideInput(v.getWindowToken());
					//							}
					//							return true;
					//						}
					//					});
					//反射温度设置  -20 - 120 ℃
					popSettingBinding.etCameraSettingReflect.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v.getText().toString()))
									return true;
								float value = inputValue2Temp(Integer.parseInt(v.getText().toString()));//拿到的都是摄氏度
								if (value > getBorderValue(120.0f) || value < getBorderValue(-20.0f)) {//带上 温度单位
									showToast(getString(R.string.toast_range_float, getBorderValue(-20.0f), getBorderValue(120.0f)));
									//									showToast("取值范围("+getBorderValue(-20.0f)+"-"+getBorderValue(120.0f)+")");
									return true;
								}
								//								byte[] iputEm = new byte[4];
								//								ByteUtil.putFloat(iputEm,value,0);
								if (mUvcCameraHandler != null) {
									if (mPid == 1 && mVid == 5396) {
										sendS0Order(value, DYConstants.SETTING_REFLECT_INT);
										//										mSendCommand.sendFloatCommand(DYConstants.SETTING_REFLECT_INT, iputEm[0], iputEm[1], iputEm[2], iputEm[3],
										//												20, 40, 60, 80, 120);
									} else if (mPid == 22592 && mVid == 3034) {
										Log.e(TAG, "onEditorAction: 反射温度 set value = " + value);
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, (int) value, 1);
									}
									sp.edit().putFloat(DYConstants.setting_reflect, value).apply();
									showToast(R.string.toast_complete_Reflect);
								}
								hideInput(v.getWindowToken());
							}
							return true;
						}
					});
					//校正设置 -20 - 20
					popSettingBinding.etCameraSettingRevise.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v.getText().toString()))
									return true;
								float value = inputValue2Temp(Float.parseFloat(v.getText().toString()));
								if (value > getBorderValue(20.0f) || value < getBorderValue(-20.0f)) {
									//									showToast("取值范围("+getBorderValue(-20.0f)+"-"+getBorderValue(20.0f)+")");
									showToast(getString(R.string.toast_range_float, getBorderValue(-20.0f), getBorderValue(20.0f)));
									return true;

								}
								//								byte[] iputEm = new byte[4];
								//								ByteUtil.putFloat(iputEm,value,0);
								if (mUvcCameraHandler != null) {
									if (mPid == 1 && mVid == 5396) {
										sendS0Order(value, DYConstants.SETTING_CORRECTION_INT);
										//										mSendCommand.sendFloatCommand(DYConstants.SETTING_CORRECTION_INT, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);

									} else if (mPid == 22592 && mVid == 3034) {//校正 TinyC
										//										sp.edit().putFloat(DYConstants.setting_correction, value).apply();
										mDataBinding.textureViewPreviewActivity.setTinyCCorrection(value);
									}
									sp.edit().putFloat(DYConstants.setting_correction, value).apply();
									showToast(R.string.toast_complete_Revise);
								}
								hideInput(v.getWindowToken());
							}
							return true;
						}
					});
					//环境温度设置  -20 -50
					popSettingBinding.etCameraSettingFreeAirTemp.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v.getText().toString()))
									return true;
								float value = inputValue2Temp(Integer.parseInt(v.getText().toString()));
								if (value > getBorderValue(50.0f) || value < getBorderValue(-20.0f)) {
									//									showToast("取值范围("+getBorderValue(-20.0f)+"-"+getBorderValue(50.0f)+")");
									showToast(getString(R.string.toast_range_float, getBorderValue(-20.0f), getBorderValue(50.0f)));
									return true;
								}
								//								byte[] iputEm = new byte[4];
								//								ByteUtil.putFloat(iputEm,value,0);
								if (mUvcCameraHandler != null) {
									if (mPid == 1 && mVid == 5396) {
										sendS0Order(value, DYConstants.SETTING_ENVIRONMENT_INT);

										//										mSendCommand.sendFloatCommand(DYConstants.SETTING_ENVIRONMENT_INT, iputEm[0], iputEm[1], iputEm[2], iputEm[3],
										//												20, 40, 60, 80, 120);
									} else if (mPid == 22592 && mVid == 3034) {
										//										Log.e(TAG, "onEditorAction: 环境温度 set value = " + value);
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, (int) value, 2);
									}
									popSettingBinding.etCameraSettingFreeAirTemp.setText(String.format(Locale.US, "%f", value));
									sp.edit().putFloat(DYConstants.setting_environment, value).apply();
									showToast(R.string.toast_complete_FreeAirTemp);
								}
								hideInput(v.getWindowToken());
							}
							return true;
						}
					});

				}
				//				else {//未连接机芯
				//					popSettingBinding.etCameraSettingEmittance.setText(String.valueOf(sp.getFloat(DYConstants.setting_emittance, 0)));//发射率 0-1
				//					//					popSettingBinding.etCameraSettingDistance.setText(String.valueOf(sp.getFloat(DYConstants.setting_distance,0)));//距离 0-5
				//					popSettingBinding.etCameraSettingHumidity.setText(String.valueOf((int) (sp.getFloat(DYConstants.setting_humidity, 0) * 100)));//湿度 0-100
				//					popSettingBinding.etCameraSettingRevise.setText(String.valueOf(sp.getFloat(DYConstants.setting_correction, 0)));//修正 -3 -3
				//					popSettingBinding.etCameraSettingReflect.setText(String.valueOf(sp.getFloat(DYConstants.setting_reflect, 0)));//反射温度 -10-40
				//					popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf(sp.getFloat(DYConstants.setting_environment, 0)));//环境温度 -10 -40
				//				}

				int temp_unit = sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0);
				//第三步：初始化自定义控件 温度单位 设置
				popSettingBinding.switchChoiceTempUnit.setText(DYConstants.tempUnit).setSelectedTab(temp_unit).setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
					@Override
					public void onSwitch (int position, String tabText) {//切换 温度单位 监听器
						mDataBinding.dragTempContainerPreviewFragment.setTempSuffix(position);
						sp.edit().putInt(DYConstants.TEMP_UNIT_SETTING, position).apply();
						//切换 温度单位 需要更改 输入框的 单位
						popSettingBinding.tvCameraSettingReviseUnit.setText("(" + DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)] + ")");
						popSettingBinding.tvCameraSettingReflectUnit.setText("(" + DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)] + ")");
						popSettingBinding.tvCameraSettingFreeAirTempUnit.setText("(" + DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)] + ")");
						//切换 温度单位 需要更改 里面已经输入的值。
						popSettingBinding.etCameraSettingRevise.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_correction, 0.0f))));
						popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_environment, 0.0f))));
						popSettingBinding.etCameraSettingReflect.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_reflect, 0.0f))));
					}
				});
				//显示设置 弹窗
				PopupWindow popupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				popupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
				if (popupWindow.getContentView().getMeasuredHeight() > fl.getHeight() / 3 * 2) {
					popupWindow.setHeight(fl.getHeight() / 3 * 2);
				} else {
					popupWindow.setHeight(popupWindow.getContentView().getMeasuredHeight());
				}
				popupWindow.setWidth(fl.getWidth() - 30);
				popupWindow.setFocusable(true);
				popupWindow.setOutsideTouchable(true);
				popupWindow.setTouchable(true);
				//第四步：显示控件
				popupWindow.showAsDropDown(mDataBinding.flPreview, 15, -popupWindow.getHeight() - 20, Gravity.CENTER);
				//弹窗消失，TinyC需要执行保存指令。
				popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
					@Override
					public void onDismiss () {
						if (mUvcCameraHandler != null) {
							//TinyC 断电保存
							if (mPid == 22592 && mVid == 3034) {
								mUvcCameraHandler.tinySaveCameraParams();
							}
							//S0 断电保存
							if (mPid == 1 && mVid == 5396) {
								setValue(UVCCamera.CTRL_ZOOM_ABS, 0x80ff);
							}
						}
					}
				});

				popSettingBinding.switchChoiceRecordAudio.setSelectedTab(sp.getInt(DYConstants.RECORD_AUDIO_SETTING, 1));
				popSettingBinding.switchChoiceRecordAudio.setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
					@Override
					public void onSwitch (int position, String tabText) {
						sp.edit().putInt(DYConstants.RECORD_AUDIO_SETTING, position).apply();
						//						if (isDebug)Log.e(TAG, "onSwitch: " + "=============================");
					}
				});


				// 切换语言 spinner
				popSettingBinding.btShowChoiceLanguage.setText(DYConstants.languageArray[sp.getInt(DYConstants.LANGUAGE_SETTING, 0)]);
				popSettingBinding.btShowChoiceLanguage.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick (View v) {
						AlertDialog.Builder builder = new AlertDialog.Builder(mContext.get());
						//						AlertDialog dialog =
						//						AlertDialog alertDialog =
						builder.setSingleChoiceItems(DYConstants.languageArray, sp.getInt(DYConstants.LANGUAGE_SETTING, 0), new DialogInterface.OnClickListener() {
							@Override
							public void onClick (DialogInterface dialog, int which) {
								if (which != sp.getInt(DYConstants.LANGUAGE_SETTING, 0)) {
									sp.edit().putInt(DYConstants.LANGUAGE_SETTING, which).apply();
									popupWindow.dismiss();

									toSetLanguage(which);
								}
								dialog.dismiss();
							}
						}).create();
						builder.show();
					}
				});
			}
		});
		//公司信息弹窗   监听器使用的图表的监听器对象
		mDataBinding.ivPreviewLeftCompanyInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				//请求权限
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_company_info, null);
				PopCompanyInfoBinding popCompanyInfoBinding = DataBindingUtil.bind(view);
				assert popCompanyInfoBinding != null;
				//				popCompanyInfoBinding.tvVersionName.setText(String.format("%s", LanguageUtils.getVersionName(mContext.get())));
				popCompanyInfoBinding.tvContactusEmail.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
				popCompanyInfoBinding.tvContactusEmail.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick (View v) {
						Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
						emailIntent.setData(Uri.parse(getResources().getString(R.string.contactus_email_head) + getResources().getString(R.string.ContactEmail)));
						emailIntent.putExtra(Intent.EXTRA_SUBJECT, "反馈标题");
						emailIntent.putExtra(Intent.EXTRA_TEXT, "反馈内容");
						//没有默认的发送邮件应用
						startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.contactus_choice_email)));
					}
				});
				//popupWindow.showAsDropDown(mDataBinding.flPreview,15,-popupWindow.getHeight()-20, Gravity.CENTER);
				showPopWindows(view, 30, 15, 20);
			}
		});
		//
		mDataBinding.dragTempContainerPreviewFragment.setAddChildDataListener(new DragTempContainer.onAddChildDataListener() {
			@Override
			public void onIsEventActionMove (DrawLineRecHint hint) {
				mDataBinding.textureViewPreviewActivity.setDrawHint(hint);
			}

			@Override
			public void onIsEventActionUp (DrawLineRecHint hint) {
				hint.setNeedDraw(false);
				mDataBinding.textureViewPreviewActivity.setDrawHint(hint);
			}
		});

		//测温模式中 工具栏 点击监听器（删除，等工具栏）
		mDataBinding.dragTempContainerPreviewFragment.setChildToolsClickListener(new DragTempContainer.OnChildToolsClickListener() {
			@Override
			public void onChildToolsClick (TempWidgetObj child, int position) {
				//				if (isDebug)Log.e(TAG, "onChildToolsClick: ======preview tools position == > " + position);
				if (position == 0) {
					mDataBinding.dragTempContainerPreviewFragment.deleteChildView(child);
					//底层重新设置 矩形框的 数据
					if (child.getType() == 3) {
						//						mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
						int[] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();
						//						if (isDebug)Log.e(TAG, "onChildToolsClick: ======setArea data  == >" + Arrays.toString(areaData));
						if (areaData != null) {
						} else {
							areaData = new int[0];
						}
						mUvcCameraHandler.setArea(areaData);
					}
				} else {
					//					if (isDebug)
					//						if (isDebug)Log.e(TAG, "onChildToolsClick: " + position);
					if (isDebug)
						showToast("click position ");

				}
			}

			@Override
			public void onRectChangedListener () {//移动框框之后刷新C层控件的设置
				//				if (isDebug)Log.e(TAG, "onRectChangedListener:  ===== ");
				if (mUvcCameraHandler != null && mUvcCameraHandler.isOpened()) {
					//					mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
					int[] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();
					if (areaData != null) {
					} else {
						areaData = new int[0];
					}
					mUvcCameraHandler.setArea(areaData);
				}
			}

			@Override
			public void onClearAllListener () {
				if (mUvcCameraHandler != null) {
					mUvcCameraHandler.setArea(new int[0]);
				}
			}

			@Override
			public void onSetParentUnselect () {
				mDataBinding.dragTempContainerPreviewFragment.setAllChildUnSelect();
			}
		});
	}

	//色板切换
	View.OnClickListener                   paletteChoiceListener      = v -> {
		switch (v.getId()) {
			case R.id.palette_layout_Tiehong:
				//				sp.edit().putInt(DYConstants.PALETTE_NUMBER,1).apply();
				setPalette(0);

				//					if (isDebug)showToast("palette_layout_Tiehong ");
				break;
			case R.id.palette_layout_Caihong:
				//				sp.edit().putInt(DYConstants.PALETTE_NUMBER,2).apply();
				setPalette(1);
				//					if (isDebug)showToast("palette_layout_Caihong ");
				break;
			case R.id.palette_layout_Hongre:
				//				sp.edit().putInt(DYConstants.PALETTE_NUMBER,3).apply();
				setPalette(2);
				//					if (isDebug)showToast("palette_layout_Hongre ");
				break;
			case R.id.palette_layout_Heire:
				//				sp.edit().putInt(DYConstants.PALETTE_NUMBER,4).apply();
				setPalette(3);
				//					if (isDebug)showToast("palette_layout_Heire ");
				break;
			case R.id.palette_layout_Baire:
				//				sp.edit().putInt(DYConstants.PALETTE_NUMBER,5).apply();
				setPalette(4);
				//					if (isDebug)showToast("palette_layout_Baire ");
				break;
			case R.id.palette_layout_Lenglan:
				//				sp.edit().putInt(DYConstants.PALETTE_NUMBER,6).apply();
				setPalette(5);
				//					if (isDebug)showToast("palette_layout_Lenglan ");
				break;
		}
	};
	//用户添加 测温模式 切换
	View.OnClickListener                   tempModeCheckListener      = new View.OnClickListener() {
		@Override
		public void onClick (View v) {
			switch (v.getId()) {
				case R.id.iv_temp_mode_point:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewFragment.setDrawTempMode(1);
					//					mDataBinding.dragTempContainerPreviewFragment.getDrawTempMode();
					//					if (isDebug)showToast("point ");
					break;
				case R.id.iv_temp_mode_line:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewFragment.setDrawTempMode(2);
					//					if (isDebug)showToast("line ");
					break;
				case R.id.iv_temp_mode_rectangle:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewFragment.setDrawTempMode(3);
					//					if (isDebug)showToast("rectangle ");
					break;
			}
		}
	};
	CompoundButton.OnCheckedChangeListener highLowCenterCheckListener = (buttonView, isChecked) -> {
		switch (buttonView.getId()) {
			case R.id.cb_main_preview_highlowcenter_trace_high:
				if (isChecked) {
					mDataBinding.textureViewPreviewActivity.openFeaturePoints(0);
				} else {
					mDataBinding.textureViewPreviewActivity.closeFeaturePoints(0);
				}
				break;
			case R.id.cb_main_preview_highlowcenter_trace_low:
				if (isChecked) {
					mDataBinding.textureViewPreviewActivity.openFeaturePoints(1);
				} else {
					mDataBinding.textureViewPreviewActivity.closeFeaturePoints(1);
				}
				break;
			case R.id.cb_main_preview_highlowcenter_trace_center:
				if (isChecked) {
					mDataBinding.textureViewPreviewActivity.openFeaturePoints(2);
				} else {
					mDataBinding.textureViewPreviewActivity.closeFeaturePoints(2);
				}
				break;
		}
	};


	/**
	 * 获取相机参数，本地sp保存
	 */
	private void getCameraParams () {//得到返回机芯的参数，128位。返回解析保存在cameraParams 中
		byte[] tempParams = mUvcCameraHandler.getTemperaturePara(128);
		cameraParams = ByteUtilsCC.byte2Float(tempParams);
		//		if (cameraParams != null){
		sp.edit().putFloat(DYConstants.setting_correction, cameraParams.get(DYConstants.setting_correction) != null ? cameraParams.get(DYConstants.setting_correction) : 0.0f).apply();
		sp.edit().putFloat(DYConstants.setting_emittance, cameraParams.get(DYConstants.setting_emittance)).apply();
		sp.edit().putFloat(DYConstants.setting_distance, cameraParams.get(DYConstants.setting_distance)).apply();
		sp.edit().putFloat(DYConstants.setting_reflect, cameraParams.get(DYConstants.setting_reflect) != null ? cameraParams.get(DYConstants.setting_reflect) : 0.0f).apply();
		sp.edit().putFloat(DYConstants.setting_environment, cameraParams.get(DYConstants.setting_environment) != null ? cameraParams.get(DYConstants.setting_environment) : 0.0f).apply();
		sp.edit().putFloat(DYConstants.setting_humidity, cameraParams.get(DYConstants.setting_humidity)).apply();
		//		}
	}

	/**
	 * 获取tinyC 参数
	 * * 发射率 0-1      emittance  取值 ： （0 -1）
	 * * 反射温度 2-3     reflect  取值 ： （-20 - 120）
	 * * 环境温度 4-5     environment  取值 ： （-20 - 50）
	 * * 湿度 6-7         humidity  取值 ： （0 - 100）
	 * * 多余 8-9
	 */
	private void getTinyCCameraParams () {//得到返回机芯的参数，128位。返回解析保存在cameraParams 中
		byte[] tempParams = mUvcCameraHandler.getTinyCCameraParams(10);
		cameraParams = ByteUtilsCC.tinyCByte2HashMap(tempParams);
		cameraParams.put(DYConstants.setting_correction, sp.getFloat(DYConstants.setting_correction, 0.0f));
		if (cameraParams != null) {
			sp.edit().putFloat(DYConstants.setting_correction, cameraParams.containsKey(DYConstants.setting_correction) ? cameraParams.get(DYConstants.setting_correction) : DYConstants.SETTING_CORRECTION_DEFAULT_VALUE).apply();
			sp.edit().putFloat(DYConstants.setting_emittance, cameraParams.get(DYConstants.setting_emittance)).apply();
			sp.edit().putFloat(DYConstants.setting_distance, cameraParams.get(DYConstants.setting_distance)).apply();
			sp.edit().putFloat(DYConstants.setting_reflect, cameraParams.get(DYConstants.setting_reflect) != null ? cameraParams.get(DYConstants.setting_reflect) : 0.0f).apply();
			sp.edit().putFloat(DYConstants.setting_environment, cameraParams.get(DYConstants.setting_environment) != null ? cameraParams.get(DYConstants.setting_environment) : 0.0f).apply();
			sp.edit().putFloat(DYConstants.setting_humidity, cameraParams.get(DYConstants.setting_humidity)).apply();
		}
	}

	/**
	 * @param value
	 * @param position
	 * @return
	 */
	public int sendS0Order (float value, int position) {
		int result = -1;
		byte[] iputEm = new byte[4];
		ByteUtil.putFloat(iputEm, value, 0);
		if (mSendCommand != null && mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {
			//			switch (position){
			//				case 0:
			//
			//					break;
			//			}
			result = mSendCommand.sendFloatCommand(position, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
		}
		return result;
	}

	/**
	 * 显示pop弹窗
	 *
	 * @param view
	 * @param widthMargin
	 * @param XOffset
	 * @param YOffset
	 */
	private void showPopWindows (View view, int widthMargin, int XOffset, int YOffset) {
		allPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		allPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		allPopupWindows.setHeight(allPopupWindows.getContentView().getMeasuredHeight());
		allPopupWindows.setWidth(fl.getWidth() - widthMargin);

		allPopupWindows.setFocusable(false);
		allPopupWindows.setOutsideTouchable(true);
		allPopupWindows.setTouchable(true);

		allPopupWindows.showAsDropDown(mDataBinding.flPreview, XOffset, -allPopupWindows.getHeight() - YOffset, Gravity.CENTER);
	}

	/**
	 * 设置页面里面的数据
	 */
	private float refreshValueByTempUnit (float temp) {
		float result = 0.0f;
		switch (sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)) {
			case 0:
				result = temp;
				break;
			case 1:
				result = temp * 1.8f + 32;
				break;
			case 2:
				result = temp + 273.15f;
				break;
		}
		return result;
	}

	/**
	 * 计算各个边界的值
	 *
	 * @param value
	 * @return 边界的 真实值（带有温度单位的值）
	 */
	private float inputValue2Temp (float value) {
		switch (sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)) {
			case 0:
				value = value;
				break;
			case 1:
				value = (value - 32) / 1.8f;
				break;
			case 2:
				value = value - 273.15f;
				break;
		}
		return value;
	}

	/**
	 * 通过单位 去 计算各个边界的实际值
	 *
	 * @param value
	 * @return 边界的 真实值（带有温度单位的值）
	 */
	private float getBorderValue (float value) {
		switch (sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)) {
			case 0:
				value = value;
				break;
			case 1:
				value = value * 1.8f + 32;
				break;
			case 2:
				value = value + 273.15f;
				break;
		}
		return value;
	}

	//设置画板
	private void setPalette (int id) {
		if (allPopupWindows != null) {
			allPopupWindows.dismiss();
		}
		if (mUvcCameraHandler != null && mUvcCameraHandler.isPreviewing() && id < 6) {
			sp.edit().putInt(DYConstants.PALETTE_NUMBER, id + 1).apply();
			mUvcCameraHandler.setPalette(id + 1);
			mDataBinding.customSeekbarPreviewFragment.setPalette(id);
			mDataBinding.customSeekbarPreviewFragment.invalidate();//刷新控件
		}
	}

	/**
	 * 开始计时
	 */
	private void startTimer () {
		Log.e(TAG, "startTimer: ");
		mDataBinding.chronometerRecordTimeInfo.setVisibility(View.VISIBLE);
		mDataBinding.chronometerRecordTimeInfo.setBase(SystemClock.elapsedRealtime());
		mDataBinding.chronometerRecordTimeInfo.setFormat("%s");
		mDataBinding.chronometerRecordTimeInfo.start();
	}

	/**
	 * 停止计时
	 */
	private void stopTimer () {
		Log.e(TAG, "stopTimer: ");
		mDataBinding.chronometerRecordTimeInfo.stop();
		mDataBinding.chronometerRecordTimeInfo.setVisibility(View.INVISIBLE);
	}

	/**
	 * 初始化录制的相关方法
	 */
	private void initRecord () {
	}

	@Override
	public void onBackPressed () {
		//
		if (PLRPopupWindows != null && PLRPopupWindows.isShowing())
			PLRPopupWindows.dismiss();
		if (allPopupWindows != null && allPopupWindows.isShowing())
			allPopupWindows.dismiss();
		super.onBackPressed();
		Log.e(TAG, "onBackPressed: ");
	}
}