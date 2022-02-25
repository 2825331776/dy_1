package com.dyt.wcc.dytpir.ui.preview;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.dyt.wcc.cameracommon.encoder.MediaMuxerWrapper;
import com.dyt.wcc.cameracommon.usbcameracommon.UVCCameraHandler;
import com.dyt.wcc.cameracommon.utils.ByteUtil;
import com.dyt.wcc.common.base.BaseApplication;
import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.common.utils.DensityUtil;
import com.dyt.wcc.common.utils.FontUtils;
import com.dyt.wcc.common.widget.MyCustomRangeSeekBar;
import com.dyt.wcc.common.widget.SwitchMultiButton;
import com.dyt.wcc.common.widget.dragView.DragTempContainer;
import com.dyt.wcc.common.widget.dragView.DrawLineRecHint;
import com.dyt.wcc.common.widget.dragView.TempWidgetObj;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.constans.DYConstants;
import com.dyt.wcc.dytpir.databinding.FragmentPreviewMainBinding;
import com.dyt.wcc.dytpir.databinding.PopCompanyInfoBinding;
import com.dyt.wcc.dytpir.databinding.PopHighlowcenterTraceBinding;
import com.dyt.wcc.dytpir.databinding.PopPaletteChoiceBinding;
import com.dyt.wcc.dytpir.databinding.PopSettingBinding;
import com.dyt.wcc.dytpir.databinding.PopTempModeChoiceBinding;
import com.dyt.wcc.dytpir.ui.DYTApplication;
import com.dyt.wcc.dytpir.ui.MainActivity;
import com.dyt.wcc.dytpir.ui.gallery.GalleryActivity;
import com.dyt.wcc.dytpir.ui.preview.record.MediaProjectionHelper;
import com.dyt.wcc.dytpir.utils.AssetCopyer;
import com.dyt.wcc.dytpir.utils.ByteUtilsCC;
import com.dyt.wcc.dytpir.utils.CreateBitmap;
import com.dyt.wcc.dytpir.utils.LanguageUtils;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:17     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.main     </p>
 */
public class PreviewFragment extends BaseFragment<FragmentPreviewMainBinding> {
	private PreViewViewModel                   mViewModel;
	private UVCCameraHandler                   mUvcCameraHandler;
	private Surface                            stt;
	private PopupWindow                        PLRPopupWindows;//点线矩形测温弹窗
	private PopupWindow                        allPopupWindows;
	private View popView;
	private Map<String ,Float>                 cameraParams ;
	private SharedPreferences sp;
	private int mVid , mPid; //设备 vid pid

//	private USBMonitor mUsbMonitor ;
	private int mTextureViewWidth,mTextureViewHeight;

	private FrameLayout fl;

	private int mFontSize;
	private int paletteType , isWatermark ,isTempShow;
	private String palettePath;
	//customSeekBar
	private Bitmap tiehong = null, caihong = null, baire = null, heire = null, hongre = null, lenglan = null;
	private SendCommand mSendCommand;

	private int screenWidth ,screenHeight;
	private Bitmap highTempBt, lowTempBt , centerTempBt , normalPointBt;

	private DisplayMetrics metrics;
	private Configuration  configuration;
	private boolean isFirstRun = false;

	@Override
	protected boolean isInterceptBackPress () {
		return false;
	}

	@Override
	protected int bindingLayout () {
		Log.e(TAG, "bindingLayout: "+ System.currentTimeMillis());
		return R.layout.fragment_preview_main;
	}
	@Override
	public void onPause () {
		super.onPause();
		if (isDebug)Log.e(TAG, "onPause: ");

		mDataBinding.textureViewPreviewFragment.onResume();

//		if (mUvcCameraHandler !=null  && mUvcCameraHandler.isRecording()){
//			mUvcCameraHandler.stopRecording();
//			mDataBinding.btPreviewLeftRecord.setSelected(false);
//			stopTimer();
//		}
	}

//	private void doReleaseAll(){
//
//
//	}
	@Override
	public void onStop () {
		super.onStop();
		if (isDebug)Log.e(TAG, "onStop: ");

		if (mUvcCameraHandler!=null){
			mUvcCameraHandler.stopTemperaturing();
		}
//		//停止录像
//		if (mUvcCameraHandler !=null  && mUvcCameraHandler.isRecording()){
//			mUvcCameraHandler.stopRecording();
//			stopTimer();
//		}
		if (Objects.requireNonNull(mViewModel.getMUsbMonitor().getValue()).isRegistered()){
			mViewModel.getMUsbMonitor().getValue().unregister();
		}

		//		if (stt != null ){
		//			stt.release();
		//			stt = null;
		//		}
		if (mDataBinding.textureViewPreviewFragment != null){
			mDataBinding.textureViewPreviewFragment.onPause();
		}

		if (mUvcCameraHandler != null){
			mUvcCameraHandler.close();
			//			mUvcCameraHandler = null;
		}
	}

	/**
	 * 获取相机参数，本地sp保存
	 */
	private void getCameraParams(){//得到返回机芯的参数，128位。返回解析保存在cameraParams 中
		byte [] tempParams = mUvcCameraHandler.getTemperaturePara(128);
		cameraParams = ByteUtilsCC.byte2Float(tempParams);
//		if (cameraParams != null){
			sp.edit().putFloat(DYConstants.setting_correction,
					cameraParams.get(DYConstants.setting_correction) != null ? cameraParams.get(DYConstants.setting_correction) : 0.0f).apply();
			sp.edit().putFloat(DYConstants.setting_emittance,
					cameraParams.get(DYConstants.setting_emittance)).apply();
			sp.edit().putFloat(DYConstants.setting_distance,
					cameraParams.get(DYConstants.setting_distance)).apply();
			sp.edit().putFloat(DYConstants.setting_reflect,
					cameraParams.get(DYConstants.setting_reflect)!=null?cameraParams.get(DYConstants.setting_reflect):0.0f).apply();
			sp.edit().putFloat(DYConstants.setting_environment,
					cameraParams.get(DYConstants.setting_environment)!=null?cameraParams.get(DYConstants.setting_environment):0.0f).apply();
			sp.edit().putFloat(DYConstants.setting_humidity,
					cameraParams.get(DYConstants.setting_humidity)).apply();
//		}
	}

	/**
	 * 获取tinyC 参数
	 *  * 发射率 0-1      emittance  取值 ： （0 -1）
	 * 	 * 反射温度 2-3     reflect  取值 ： （-20 - 120）
	 * 	 * 环境温度 4-5     environment  取值 ： （-20 - 50）
	 * 	 * 湿度 6-7         humidity  取值 ： （0 - 100）
	 * 	 * 多余 8-9
	 */
	private void getTinyCCameraParams(){//得到返回机芯的参数，128位。返回解析保存在cameraParams 中
		byte [] tempParams = mUvcCameraHandler.getTinyCCameraParams(10);
		cameraParams = ByteUtilsCC.tinyCByte2HashMap(tempParams);
		cameraParams.put(DYConstants.setting_correction,sp.getFloat(DYConstants.setting_correction,0.0f));

		if (cameraParams != null){
			sp.edit().putFloat(DYConstants.setting_correction,
					cameraParams.get(DYConstants.setting_correction)!=null?
							cameraParams.get(DYConstants.setting_correction):0.0f).apply();
			sp.edit().putFloat(DYConstants.setting_emittance,
					cameraParams.get(DYConstants.setting_emittance)).apply();
			sp.edit().putFloat(DYConstants.setting_distance,
					cameraParams.get(DYConstants.setting_distance)).apply();
			sp.edit().putFloat(DYConstants.setting_reflect,
					cameraParams.get(DYConstants.setting_reflect)!=null?
							cameraParams.get(DYConstants.setting_reflect):0.0f).apply();
			sp.edit().putFloat(DYConstants.setting_environment,
					cameraParams.get(DYConstants.setting_environment)!=null?
							cameraParams.get(DYConstants.setting_environment):0.0f).apply();
			sp.edit().putFloat(DYConstants.setting_humidity,
					cameraParams.get(DYConstants.setting_humidity)).apply();
		}
	}

	@Override
	public void onDetach () {
		super.onDetach();
		if (isDebug)Log.e(TAG, "onDetach: ");
	}

	@Override
	public void onStart () {
		super.onStart();
		if (isDebug)Log.e(TAG, "onStart: ");
	}

	@Override
	public void onCreate (@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isDebug)Log.e(TAG, "onCreate: ");
	}

	@Override
	public void onDestroy () {
		if (mUvcCameraHandler != null) {
			mUvcCameraHandler.release();
			mUvcCameraHandler = null;
		}
		if ( mViewModel.getMUsbMonitor().getValue() != null) {
			mViewModel.getMUsbMonitor().getValue().destroy();
			mViewModel.getMUsbMonitor().setValue(null);
		}
//		mDataBinding.textureViewPreviewFragment = null;
		super.onDestroy();
		if (isDebug)Log.e(TAG, "onDestroy: ");
	}

	@Override
	public void onDestroyView () {
		super.onDestroyView();
		if (isDebug)Log.e(TAG, "onDestroyView: ");

//		if (mDataBinding.dragTempContainerPreviewFragment.isHighTempAlarmToggle()){
//			mDataBinding.dragTempContainerPreviewFragment.closeHighTempAlarm();
//		}
		mDataBinding.textureViewPreviewFragment.destroyDrawingCache();
		if (mViewModel.getMUsbMonitor().getValue() !=null ){
			mViewModel.getMUsbMonitor().getValue().destroy();
		}

		if (MediaProjectionHelper.getInstance().getRecord_State() != 0){
			MediaProjectionHelper.getInstance().stopMediaRecorder();
			MediaProjectionHelper.getInstance().stopService(mContext.get());
		}

//		if (mUvcCameraHandler != null) {
//			mUvcCameraHandler.release();
//			mUvcCameraHandler = null;
//		}

//		if (mUvcCameraHandler != null){
////			Toast.makeText(mContext.get(),"录制 ", Toast.LENGTH_SHORT).show();
////			mUvcCameraHandler.stopTemperaturing();
////			mUvcCameraHandler.stopPreview();
//			mUvcCameraHandler.release();
//			mUvcCameraHandler = null;
//		}


	}



	private USBMonitor.OnDeviceConnectListener onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
		@Override
		public void onAttach (UsbDevice device) {
//			if (isDebug)Log.e(TAG, "DD  onAttach: "+ device.toString());
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (isDebug)Log.e(TAG, "检测到设备========");
//					mUvcCameraHandler.release();
					mViewModel.getMUsbMonitor().getValue().requestPermission(device);
				}
			}, 100);
		}
		@Override
		public void onConnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
//			if (isDebug)Log.e(TAG, "onConnect:  SN ========================= 》 " + device.getSerialNumber());
			mVid = device.getVendorId();
			mPid = device.getProductId();

			if (mUvcCameraHandler == null || mUvcCameraHandler.isReleased()){
				mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(),
						mDataBinding.textureViewPreviewFragment,1,
						384,292,1,null,0);
			}

			mUvcCameraHandler.open(ctrlBlock);
			startPreview();

			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					setValue(UVCCamera.CTRL_ZOOM_ABS, DYConstants.CAMERA_DATA_MODE_8004);//切换数据输出8004原始8005yuv,80ff保存
				}
			}, 300);
		}
		@Override
		public void onDettach (UsbDevice device) {
			//				mUvcCameraHandler.close();
			if (isDebug)Log.e(TAG, "DD  onDetach: ");
			onPause();
			onStop();
			onDestroy();
//			android.os.Process.killProcess(android.os.Process.myPid());
//			System.exit(0);
		}
		@Override
		public void onDisconnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
			if (isDebug)Log.e(TAG, " DD  onDisconnect: ");

			mVid = 0;
			mPid = 0;
			if (mUvcCameraHandler != null) {
					mUvcCameraHandler.stopTemperaturing();
				mUvcCameraHandler.close();
				}
//			if (mUvcCameraHandler != null){
//				if (mUvcCameraHandler.isRecording()){
//					stopTimer();
//					mDataBinding.ivPreviewLeftGallery.setVisibility(View.VISIBLE);
//					mUvcCameraHandler.stopRecording();
//					mDataBinding.btPreviewLeftRecord.setSelected(false);
//				}
//				//				mUvcCameraHandler.stopTemperaturing();
//				//				mUvcCameraHandler.stopPreview();
//				mUvcCameraHandler.close();
//				//				mUvcCameraHandler.release();//拔出之时没释放掉这个资源。关闭窗口之时必须释放
//			}

		}
		@Override
		public void onCancel (UsbDevice device) {
			if (isDebug)Log.e(TAG, "DD  onCancel: ");
		}
	};

	@Override
	public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
//		Log.e(TAG, "onViewCreated:  width  == > " + mDataBinding.flPreview.getLayoutParams().width);

	}

	@Override
	public void onResume () {
		super.onResume();
		Log.e(TAG, "onResume: ");
		//初始化语言，解决onStop 但没有OnDestroy 时 设置了英语锁屏又 返回 系统默认中文 的BUG
		if (sp.getInt(DYConstants.LANGUAGE_SETTING,0) == 0){
			configuration.locale = Locale.SIMPLIFIED_CHINESE;
			configuration.setLayoutDirection(Locale.SIMPLIFIED_CHINESE);
		}else {
			configuration.locale = Locale.ENGLISH;
			configuration.setLayoutDirection(Locale.ENGLISH);
		}
		getResources().updateConfiguration(configuration,metrics);

//		Log.e(TAG, "onResume:  mViewModel ===  " + (mViewModel == null));

		if (!mViewModel.getMUsbMonitor().getValue().isRegistered()
				&& hasPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE)){
			mViewModel.getMUsbMonitor().getValue().register();
		}

		List<UsbDevice> mUsbDeviceList = mViewModel.getMUsbMonitor().getValue().getDeviceList();
		for (UsbDevice udv : mUsbDeviceList) {
			//指定设备的连接，获取设备的名字，当前usb摄像头名为Xmodule-S0
			if (isDebug)Log.e(TAG, "usb devices  == " + udv.toString());
			if (isDebug)Log.e(TAG, "udv.getProductName()" + udv.getProductName());
			if (udv.getVendorId() == 5396 && udv.getProductId() ==1 ) {
				//				if (isDebug)Log.e(TAG, "onResume: "+ " S0 " + udv.getProductId() + " "  + udv.getVendorId() );
				BaseApplication.deviceName = udv.getProductName();
			}
		}
//		if(isDebug)Log.e(TAG, "onResume: before  ===  " +System.currentTimeMillis());
	}
	private int setValue(final int flag, final int value) {//设置机芯参数,调用JNI层
		return mUvcCameraHandler != null ? mUvcCameraHandler.setValue(flag, value) : 0;
	}

	/**
	 * 打开连接 调用预览图像的设置
	 */
	private void startPreview () {
//				if (isDebug)Log.e(TAG, "startPreview: flPreview  width == " + mDataBinding.flPreview.getMeasuredWidth()
//						+ " height == " + mDataBinding.flPreview.getMeasuredHeight());

		stt = new Surface(mDataBinding.textureViewPreviewFragment.getSurfaceTexture());

		mTextureViewWidth = mDataBinding.textureViewPreviewFragment.getWidth();
		mTextureViewHeight = mDataBinding.textureViewPreviewFragment.getHeight();
//		if (isDebug)Log.e(TAG,"height =="+ mTextureViewHeight + " width==" + mTextureViewWidth);
		mDataBinding.textureViewPreviewFragment.setFrameBitmap(highTempBt,lowTempBt,centerTempBt,normalPointBt,DensityUtil.dp2px(mContext.get(),30));

		mDataBinding.textureViewPreviewFragment.iniTempBitmap(mTextureViewWidth, mTextureViewHeight);//初始化画板的值，是控件的像素的宽高
		mDataBinding.textureViewPreviewFragment.setVidPid(mVid,mPid);//设置vid  pid
		mDataBinding.textureViewPreviewFragment.setTinyCCorrection(sp.getFloat(DYConstants.setting_correction,0.0f));//设置vid  pid
		mDataBinding.textureViewPreviewFragment.setDragTempContainer(mDataBinding.dragTempContainerPreviewFragment);
		mDataBinding.customSeekbarPreviewFragment.setmThumbListener(new MyCustomRangeSeekBar.ThumbListener() {
			@Override
			public void thumbChanged (float maxPercent, float minPercent,float maxValue, float minValue) {
				maxValue = maxValue - sp.getFloat(DYConstants.setting_correction,0.0f);
				minValue = minValue - sp.getFloat(DYConstants.setting_correction,0.0f);
				if (mUvcCameraHandler!= null &&!Float.isNaN(maxValue) && !Float.isNaN(minValue))mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent,maxValue,minValue);
			}

			@Override
			public void onUpMinThumb (float maxPercent, float minPercent,float maxValue, float minValue) {
//				if (isDebug)Log.e(TAG, "onUpMinThumb: 0-100 percent " + maxPercent + " min == > " +  minPercent);
//				if (isDebug)Log.e(TAG, "onUpMinThumb: value " + maxValue + " min == > " +  minValue);

				if (maxPercent >= 100 && minPercent <= 0) {
					if (mUvcCameraHandler!= null &&  mUvcCameraHandler.isOpened())mUvcCameraHandler.disWenKuan();
				}
			}

			@Override
			public void onUpMaxThumb (float maxPercent, float minPercent,float maxValue, float minValue) {
//				if (isDebug)Log.e(TAG, "onUpMaxThumb: 0-100 percent " + maxPercent + " min == > " +  minPercent);
//				if (isDebug)Log.e(TAG, "onUpMaxThumb: value " + maxValue + " min == > " +  minValue);
				if (maxPercent >= 100 && minPercent <= 0) {
					if (mUvcCameraHandler!= null &&  mUvcCameraHandler.isOpened())mUvcCameraHandler.disWenKuan();
				}
			}

			@Override
			public void onMinMove (float maxPercent, float minPercent,float maxValue, float minValue) {
				if (isDebug)Log.e(TAG, "onMinMove: 0-100 percent " + maxPercent + " min == > " +  minPercent);
				maxValue = maxValue - sp.getFloat(DYConstants.setting_correction,0.0f);
				minValue = minValue - sp.getFloat(DYConstants.setting_correction,0.0f);
				if (isDebug)Log.e(TAG, "onMinMove: value == >" + maxValue + " min == > " +  minValue);
				if (mUvcCameraHandler!= null && !Float.isNaN(maxValue) && !Float.isNaN(minValue))mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent,maxValue,minValue);
			}

			@Override
			public void onMaxMove (float maxPercent, float minPercent,float maxValue, float minValue) {
//				if (isDebug)Log.e(TAG, "onMaxMove: 0-100 percent" + maxPercent + " min == > " +  minPercent);
				maxValue = maxValue - sp.getFloat(DYConstants.setting_correction,0.0f);
				minValue = minValue - sp.getFloat(DYConstants.setting_correction,0.0f);
				if (isDebug)Log.e(TAG, "onMaxMove: value == > " + maxValue + " min == > " +  minValue);
				// && maxValue != Float.NaN && minValue != Float.NaN
				if (mUvcCameraHandler!= null&& !Float.isNaN(maxValue) && !Float.isNaN(minValue))
					mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent,maxValue,minValue);
			}
		});

		paletteType =1;
		mUvcCameraHandler.PreparePalette(palettePath,paletteType);
		mUvcCameraHandler.setAreaCheck(0);
//		mUvcCameraHandler.setPalette(0);

		//是否进行温度的绘制
		isTempShow = 0;
		mUvcCameraHandler.tempShowOnOff(isTempShow);//是否显示绘制的温度 0不显示，1显示。最终调用的是UVCCameraTextureView的绘制线程。

		mUvcCameraHandler.startPreview(stt);
		//tinyC 暂时关闭 温度回调功能
		mUvcCameraHandler.startTemperaturing();//温度回调


//		new Handler().postDelayed(new Runnable() {
//			@Override
//			public void run () {
//				if (mUvcCameraHandler!=null && mUvcCameraHandler.isOpened()){
////					getCameraParams();
//				}
//				//清除所有的控件
////				mDataBinding.dragTempContainerPreviewFragment.clearAll();
//			}
//		},3000);
	}

	/**
	 * 初始化界面的监听器
	 */
	private void initListener(){
		//测试的 监听器
		mDataBinding.btTest01.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
//				if (mUvcCameraHandler!= null ){
		//					mUvcCameraHandler.stopTemperaturing();
//								Log.e(TAG, "initView: not null ==========================================" );
//								int a[] = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();
//								Log.e(TAG, "initView: aa === > " + Arrays.toString(a));
//					mUvcCameraHandler.setVerifySn();
//				}
//				onStop();
				Intent intent = new Intent(getActivity(), GalleryActivity.class);
				startActivity(intent);
//				mUvcCameraHandler.stopTemperaturing();
//				mUvcCameraHandler.stopPreview();
//				EasyPhotos.createAlbum(getActivity(), false, false, GlideEngine.getInstance())
//						//				.setFileProviderAuthority("com.huantansheng.easyphotos.demo.fileprovider")
//						.setFileProviderAuthority("com.dyt.wcc.dytpir.FileProvider")
//						.setCount(9)
//						.setVideo(true)
//						.setGif(false)
//						.start(101);


						//				else if (mUvcCameraHandler !=null && !mUvcCameraHandler.isTemperaturing()){
		//					mUvcCameraHandler.startTemperaturing();
		//				}
			}
		});
//
//				mDataBinding.btTest02.setOnClickListener(new View.OnClickListener() {
//					@Override
//					public void onClick (View v) {
////						if (mUvcCameraHandler!= null && mUvcCameraHandler.isPreviewing()){
////							Log.e(TAG, "onClick: btFresh");
////							setValue(UVCCamera.CTRL_ZOOM_ABS,0x8000);
////							mUvcCameraHandler.whenShutRefresh();
////						}
//		//				else if (mUvcCameraHandler !=null && !mUvcCameraHandler.isPreviewing()){
//		//					mUvcCameraHandler.stopTemperaturing();
//		//					mUvcCameraHandler.stopPreview();
//		//					mUvcCameraHandler.release();
//		//				}
//					}
//				});

		/**
		 * 超温警告 ， 预览层去绘制框， DragTempContainer 控件去播放声音
		 */
		mDataBinding.toggleHighTempAlarm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				mDataBinding.toggleHighTempAlarm.setSelected(!mDataBinding.toggleHighTempAlarm.isSelected());
				if (mDataBinding.toggleHighTempAlarm.isSelected()){
					OverTempDialog dialog = new OverTempDialog(mContext.get(),sp.getFloat("overTemp",0.0f),
							mDataBinding.dragTempContainerPreviewFragment.getTempSuffixMode());

					dialog.getWindow().setGravity(Gravity.LEFT);
					WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
					params.x =  mDataBinding.flPreview.getMeasuredWidth()/2 - DensityUtil.dp2px(mContext.get(), 100);

					dialog.setListener(new OverTempDialog.SetCompleteListener() {
						@Override
						public void onSetComplete (float setValue) {
//							if (isDebug)Log.e(TAG, "onSetComplete: " + "confirm value = == > " + setValue  );
							mDataBinding.dragTempContainerPreviewFragment.openHighTempAlarm(setValue);

							mDataBinding.textureViewPreviewFragment.startTempAlarm(setValue);
							sp.edit().putFloat("overTemp",setValue).apply();
						}
						@Override
						public void onCancelListener () {
							//							Log.e(TAG, "onCancelListener: " + "cancel "  );
							mDataBinding.toggleHighTempAlarm.setSelected(false);
						}
					});
					dialog.setCancelable(false);
					dialog.show();
				}else {
					mDataBinding.textureViewPreviewFragment.stopTempAlarm();
					mDataBinding.dragTempContainerPreviewFragment.closeHighTempAlarm();
				}
			}
		});

		//切换色板
		mDataBinding.ivPreviewLeftPalette.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				if (isResumed()){
					View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_palette_choice,null);
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
					allPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
					allPopupWindows.setHeight(mDataBinding.llContainerPreviewSeekbar.getHeight());
					allPopupWindows.setWidth(mDataBinding.llContainerPreviewSeekbar.getWidth());

					allPopupWindows.setFocusable(false);
					allPopupWindows.setOutsideTouchable(true);
					allPopupWindows.setTouchable(true);

					allPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar,0,-mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
				}
			}
		});

		//测温模式。绘制  温度模式 切换  弹窗
		mDataBinding.ivPreviewRightTempMode.setOnClickListener(v -> {
			if (isResumed()){
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_temp_mode_choice,null);
				PopTempModeChoiceBinding tempModeChoiceBinding = DataBindingUtil.bind(view);
				assert tempModeChoiceBinding != null;
				tempModeChoiceBinding.ivTempModeLine.setOnClickListener(tempModeCheckListener);
				tempModeChoiceBinding.ivTempModePoint.setOnClickListener(tempModeCheckListener);
				tempModeChoiceBinding.ivTempModeRectangle.setOnClickListener(tempModeCheckListener);

				PLRPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				PLRPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
				PLRPopupWindows.setHeight(mDataBinding.llContainerPreviewSeekbar.getHeight());
				PLRPopupWindows.setWidth(mDataBinding.llContainerPreviewSeekbar.getWidth());

				PLRPopupWindows.setFocusable(false);
				//				popupWindow.setBackgroundDrawable(getResources().getDrawable(R.mipmap.temp_mode_bg_tempback));
				PLRPopupWindows.setOutsideTouchable(true);
				PLRPopupWindows.setTouchable(true);

				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar,0,-mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			}
		});

		//高低中心点温度追踪 弹窗
		mDataBinding.toggleShowHighLowTemp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
				if (isResumed()){
					View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_highlowcenter_trace,null);
					PopHighlowcenterTraceBinding popHighlowcenterTraceBinding = DataBindingUtil.bind(view);
					assert popHighlowcenterTraceBinding != null;
					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceHigh.setOnCheckedChangeListener(highLowCenterCheckListener);
					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceLow.setOnCheckedChangeListener(highLowCenterCheckListener);
					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceCenter.setOnCheckedChangeListener(highLowCenterCheckListener);

					int toggleState = mDataBinding.textureViewPreviewFragment.getFeaturePointsControl();
					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceHigh.setChecked((toggleState & 0x0f00)>0);
					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceLow.setChecked((toggleState & 0x00f0)>0);
					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceCenter.setChecked((toggleState & 0x000f)>0);

					PLRPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					PLRPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
					PLRPopupWindows.setHeight(mDataBinding.llContainerPreviewSeekbar.getHeight());
					PLRPopupWindows.setWidth(mDataBinding.llContainerPreviewSeekbar.getWidth());

					PLRPopupWindows.setFocusable(false);
					PLRPopupWindows.setOutsideTouchable(true);
					PLRPopupWindows.setTouchable(true);

					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar,0,-mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);

				}
			}
		});

		//框内细查
		mDataBinding.toggleAreaCheck.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				if (!mDataBinding.toggleAreaCheck.isSelected()){
					mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),
							mDataBinding.textureViewPreviewFragment.getHeight());
					int [] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();

					//					Log.e(TAG, "onCheckedChanged: checked  ==== >  " + isChecked + " ==================" + Arrays.toString(areaData));
					if (mUvcCameraHandler ==null)return;
					mUvcCameraHandler.setArea(areaData);
					mUvcCameraHandler.setAreaCheck(1);
				}else {//close
					if (mUvcCameraHandler ==null)return;
					mUvcCameraHandler.setAreaCheck(0);
				}

				mDataBinding.toggleAreaCheck.setSelected(!mDataBinding.toggleAreaCheck.isSelected());
			}
		});

		//固定温度条
		mDataBinding.toggleFixedTempBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				mDataBinding.toggleFixedTempBar.setSelected(!mDataBinding.toggleFixedTempBar.isSelected());
				if (mDataBinding.toggleFixedTempBar.isSelected()){
					mDataBinding.customSeekbarPreviewFragment.setWidgetMode(1);
				}else {
					mDataBinding.customSeekbarPreviewFragment.setWidgetMode(0);if (mUvcCameraHandler!=null)mUvcCameraHandler.disWenKuan();
				}
				if (mUvcCameraHandler!=null)mUvcCameraHandler.fixedTempStripChange(mDataBinding.toggleFixedTempBar.isSelected());
			}
		});

		//设置弹窗
		mDataBinding.ivPreviewRightSetting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				//打开设置第一步：获取机芯数据。
				if (mUvcCameraHandler == null)return;
				if (mUvcCameraHandler.isOpened()){
					if (mPid == 1 && mVid == 5396){
						getCameraParams();//
					}else if (mPid == 22592 && mVid == 3034){
						getTinyCCameraParams();//

					}
				}
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_setting,null);

				PopSettingBinding popSettingBinding = DataBindingUtil.bind(view);
				//设置单位
				popSettingBinding.tvCameraSettingReviseUnit.setText("("+DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)]+")");
				popSettingBinding.tvCameraSettingReflectUnit.setText("("+DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)]+")");
				popSettingBinding.tvCameraSettingFreeAirTempUnit.setText("("+DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)]+")");

				assert popSettingBinding != null;
				//第二步：将获取的数据 展示在输入框内
				if (cameraParams != null) {
					if (mPid == 22592 && mVid == 3034){
//						popSettingBinding.etCameraSettingReflect.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
//						popSettingBinding.etCameraSettingFreeAirTemp.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
						popSettingBinding.etCameraSettingDistance.setEnabled(false);
						mDataBinding.textureViewPreviewFragment.setTinyCCorrection(sp.getFloat(DYConstants.setting_correction,0.0f));
					}else if (mPid == 1 && mVid == 5396) {
						popSettingBinding.etCameraSettingDistance.setEnabled(true);
					}
					popSettingBinding.etCameraSettingEmittance.setText(String.valueOf(cameraParams.get(DYConstants.setting_emittance)));//发射率 0-1
					popSettingBinding.etCameraSettingDistance.setText(String.valueOf(cameraParams.get(DYConstants.setting_distance)));//距离 0-5
					popSettingBinding.etCameraSettingHumidity.setText(String.valueOf((int) (cameraParams.get(DYConstants.setting_humidity)*100)));//湿度 0-100
					popSettingBinding.etCameraSettingRevise.setText(String.valueOf(cameraParams.get(DYConstants.setting_correction)));//校正  -20 - 20
					popSettingBinding.etCameraSettingReflect.setText(String.valueOf((int) (cameraParams.get(DYConstants.setting_reflect)*1)));//反射温度 -10-40
					popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf((int)(cameraParams.get(DYConstants.setting_environment)*1)));//环境温度 -10 -40
					//把值同步到 sp中
					//					sp.edit().putFloat(DYConstants.setting_emittance,cameraParams.get(DYConstants.setting_emittance)).apply();
					//发射率
					popSettingBinding.etCameraSettingEmittance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
							if (actionId == EditorInfo.IME_ACTION_DONE){
								if (TextUtils.isEmpty(v.getText().toString()))return true;
								float value = Float.parseFloat(v.getText().toString());
								if (value > 1 || value < 0){
									showToast(getString(R.string.toast_range_int,0,1));
									return true;
								}
								v.clearFocus();
								byte[] iputEm = new byte[4];
								ByteUtil.putFloat(iputEm,value,0);
								if (mUvcCameraHandler != null) {
									if (mPid == 1 && mVid == 5396) {
										mSendCommand.sendFloatCommand(4 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3],
												20, 40, 60, 80, 120);
//										mUvcCameraHandler.startTemperaturing();
									} else if (mPid == 22592 && mVid == 3034){
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS,value,3);
									}
									sp.edit().putFloat(DYConstants.setting_emittance,value).apply();
									showToast(R.string.toast_complete_Emittance);
								}
								hideInput(v.getWindowToken());
							}
							return true;
						}
					});
					//距离设置
					popSettingBinding.etCameraSettingDistance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE){
								if (TextUtils.isEmpty(v.getText().toString()))return true;
								int value = Math.round(Float.parseFloat(v.getText().toString()));
								if (value > 5 || value < 0){
									showToast(getString(R.string.toast_range_int,0,5));
									return true;
								}
								byte[] bIputDi = new byte[4];
								ByteUtil.putInt(bIputDi,value,0);
								if (mUvcCameraHandler!= null) {
									if (mPid == 1 && mVid == 5396) {
										mSendCommand.sendShortCommand(5 * 4, bIputDi[0], bIputDi[1], 20, 40, 60);
									}
									sp.edit().putFloat(DYConstants.setting_distance,value).apply();
									showToast(R.string.toast_complete_Distance);
								}
								hideInput(v.getWindowToken());
							}
							return true;
						}
					});
					//反射温度设置  -20 - 120 ℃
					popSettingBinding.etCameraSettingReflect.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE){
								if (TextUtils.isEmpty(v.getText().toString()))return true;
								float value = inputValue2Temp(Integer.parseInt(v.getText().toString()));//拿到的都是摄氏度
								if (value > getBorderValue(120.0f) || value < getBorderValue(-20.0f)){//带上 温度单位
									showToast(getString(R.string.toast_range_float,getBorderValue(-20.0f),getBorderValue(120.0f)));
//									showToast("取值范围("+getBorderValue(-20.0f)+"-"+getBorderValue(120.0f)+")");
									return true;
								}
								byte[] iputEm = new byte[4];
								ByteUtil.putFloat(iputEm,value,0);
								if (mUvcCameraHandler!= null) {
									if (mPid == 1 && mVid == 5396) {
										mSendCommand.sendFloatCommand(1 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3],
												20, 40, 60, 80, 120);
									} else if (mPid == 22592 && mVid == 3034){
										Log.e(TAG, "onEditorAction: 反射温度 set value = " + value);
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS,(int)value,1);
									}
									sp.edit().putFloat(DYConstants.setting_reflect,value).apply();
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
							if (actionId == EditorInfo.IME_ACTION_DONE){
								if (TextUtils.isEmpty(v.getText().toString()))return true;
								float value = inputValue2Temp(Float.parseFloat(v.getText().toString()));
								if (value > getBorderValue(20.0f) || value < getBorderValue(-20.0f)){
//									showToast("取值范围("+getBorderValue(-20.0f)+"-"+getBorderValue(20.0f)+")");
									showToast(getString(R.string.toast_range_float,getBorderValue(-20.0f),getBorderValue(20.0f)));
									return true;

								}
								byte[] iputEm = new byte[4];
								ByteUtil.putFloat(iputEm,value,0);
								if (mUvcCameraHandler!= null) {
									if (mPid == 1 && mVid == 5396) {
										mSendCommand.sendFloatCommand(0 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
										sp.edit().putFloat(DYConstants.setting_correction, value).apply();
									} else if (mPid == 22592 && mVid == 3034){//校正 TinyC
										sp.edit().putFloat(DYConstants.setting_correction, value).apply();
										mDataBinding.textureViewPreviewFragment.setTinyCCorrection(sp.getFloat(DYConstants.setting_correction,0.0f));
									}
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
							if (actionId == EditorInfo.IME_ACTION_DONE){
								if (TextUtils.isEmpty(v.getText().toString()))return true;
								float value = inputValue2Temp(Integer.parseInt(v.getText().toString()));
								if (value > getBorderValue(50.0f) || value < getBorderValue(-20.0f)){
//									showToast("取值范围("+getBorderValue(-20.0f)+"-"+getBorderValue(50.0f)+")");
									showToast(getString(R.string.toast_range_float,getBorderValue(-20.0f),getBorderValue(50.0f)));
									return true;
								}
								byte[] iputEm = new byte[4];
								ByteUtil.putFloat(iputEm,value,0);
								if (mUvcCameraHandler!= null) {
									if (mPid == 1 && mVid == 5396) {
										mSendCommand.sendFloatCommand(2 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
									} else if (mPid == 22592 && mVid == 3034){
										Log.e(TAG, "onEditorAction: 环境温度 set value = " + value);
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS,(int)value,2);
									}

									sp.edit().putFloat(DYConstants.setting_environment,value).apply();
									showToast(R.string.toast_complete_FreeAirTemp);
								}
								hideInput(v.getWindowToken());
							}
							return true;
						}
					});
					//湿度设置
					popSettingBinding.etCameraSettingHumidity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE){
								if (TextUtils.isEmpty(v.getText().toString()))return true;
								int value = Integer.parseInt(v.getText().toString());
								if (value > 100 || value < 0){
//									showToast("取值范围(0-100)");
									showToast(getString(R.string.toast_range_int,0,100));
									return true;
								}
								float fvalue = value/100.0f;
								byte[] iputEm = new byte[4];
								ByteUtil.putFloat(iputEm,fvalue,0);
								if (mUvcCameraHandler!= null) {
									if (mPid == 1 && mVid == 5396) {
										mSendCommand.sendFloatCommand(3 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3],
												20, 40, 60, 80, 120);
									} else if (mPid == 22592 && mVid == 3034){
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS,fvalue,4);
									}
									sp.edit().putFloat(DYConstants.setting_humidity,fvalue).apply();
									showToast(R.string.toast_complete_Humidity);
								}
								hideInput(v.getWindowToken());
							}
							return true;
						}
					});
				}else {//未连接机芯
					popSettingBinding.etCameraSettingEmittance.setText(String.valueOf(sp.getFloat(DYConstants.setting_emittance,0)));//发射率 0-1
					popSettingBinding.etCameraSettingDistance.setText(String.valueOf(sp.getFloat(DYConstants.setting_distance,0)));//距离 0-5
					popSettingBinding.etCameraSettingHumidity.setText(String.valueOf((int)(sp.getFloat(DYConstants.setting_humidity,0)*100)));//湿度 0-100
					popSettingBinding.etCameraSettingRevise.setText(String.valueOf(sp.getFloat(DYConstants.setting_correction,0)));//修正 -3 -3
					popSettingBinding.etCameraSettingReflect.setText(String.valueOf(sp.getFloat(DYConstants.setting_reflect,0)));//反射温度 -10-40
					popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf(sp.getFloat(DYConstants.setting_environment,0)));//环境温度 -10 -40
				}

				int temp_unit = sp.getInt(DYConstants.TEMP_UNIT_SETTING,0);
				//第三步：初始化自定义控件 温度单位 设置
				popSettingBinding.switchChoiceTempUnit.setText(DYConstants.tempUnit).setSelectedTab(temp_unit).setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
					@Override
					public void onSwitch (int position, String tabText) {//切换 温度单位 监听器
						mDataBinding.dragTempContainerPreviewFragment.setTempSuffix(position);
						sp.edit().putInt(DYConstants.TEMP_UNIT_SETTING,position).apply();
						//切换 温度单位 需要更改 输入框的 单位
						popSettingBinding.tvCameraSettingReviseUnit.setText("("+DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)]+")");
						popSettingBinding.tvCameraSettingReflectUnit.setText("("+DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)]+")");
						popSettingBinding.tvCameraSettingFreeAirTempUnit.setText("("+DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)]+")");
						//切换 温度单位 需要更改 里面已经输入的值。
						popSettingBinding.etCameraSettingRevise.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_correction,0.0f))));
						popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_environment,0.0f))));
						popSettingBinding.etCameraSettingReflect.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_reflect,0.0f))));
					}
				});
				//显示设置 弹窗
				PopupWindow popupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				popupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
				if (popupWindow.getContentView().getMeasuredHeight()>fl.getHeight()/3*2){
					popupWindow.setHeight(fl.getHeight()/3*2);
				}else {
					popupWindow.setHeight(popupWindow.getContentView().getMeasuredHeight());
				}
				popupWindow.setWidth(fl.getWidth()- 30);
				popupWindow.setFocusable(true);
				popupWindow.setOutsideTouchable(true);
				popupWindow.setTouchable(true);
				//第四步：显示控件
				popupWindow.showAsDropDown(mDataBinding.flPreview,15,-popupWindow.getHeight()-20, Gravity.CENTER);
				//弹窗消失，TinyC需要执行保存指令。
				popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
					@Override
					public void onDismiss () {
						if (mUvcCameraHandler!= null) {
							//TinyC 保存
							if (mPid == 22592 && mVid == 3034){
								mUvcCameraHandler.tinySaveCameraParams();
								//S0 断电保存
							}if (mPid == 1 && mVid == 5396){
								setValue(UVCCamera.CTRL_ZOOM_ABS, 0x80ff);
							}
						}
					}
				});

				popSettingBinding.switchChoiceRecordAudio.setSelectedTab(sp.getInt(DYConstants.RECORD_AUDIO_SETTING,1));
				popSettingBinding.switchChoiceRecordAudio.setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
					@Override
					public void onSwitch (int position, String tabText) {
						sp.edit().putInt(DYConstants.RECORD_AUDIO_SETTING,position).apply();
						Log.e(TAG, "onSwitch: " + "=============================");
					}
				});


				// 切换语言 spinner
				popSettingBinding.btShowChoiceLanguage.setText(DYConstants.languageArray[sp.getInt(DYConstants.LANGUAGE_SETTING,0)]);
				popSettingBinding.btShowChoiceLanguage.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick (View v) {
						AlertDialog.Builder builder = new AlertDialog.Builder(mContext.get());
						//						AlertDialog dialog =
						//						AlertDialog alertDialog =
						builder.setSingleChoiceItems(DYConstants.languageArray, sp.getInt(DYConstants.LANGUAGE_SETTING, 0), new DialogInterface.OnClickListener() {
							@Override
							public void onClick (DialogInterface dialog, int which) {
								if (which != sp.getInt(DYConstants.LANGUAGE_SETTING,0)){
									sp.edit().putInt(DYConstants.LANGUAGE_SETTING,which).apply();
									popupWindow.dismiss();

									if (mUvcCameraHandler != null ){
										mUvcCameraHandler.close();
										mUvcCameraHandler.release();
										mUvcCameraHandler = null;
										stt.release();
										stt = null;
									}
									if (mViewModel.getMUsbMonitor().getValue().isRegistered()){
										mViewModel.getMUsbMonitor().getValue().unregister();
										mViewModel.getMUsbMonitor().getValue().destroy();
									}
//									onStop();
//									onDestroyView();
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
//				PermissionX.init(PreviewFragment.this).permissions(Manifest.permission.INTERNET).request(new RequestCallback() {
//					@Override
//					public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
//						if (allGranted){
							View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_company_info,null);
							PopCompanyInfoBinding popCompanyInfoBinding = DataBindingUtil.bind(view);
							assert popCompanyInfoBinding != null;
							//							popCompanyInfoBinding.tvCheckVersion.setOnClickListener(chartModeCheckListener);
							popCompanyInfoBinding.tvVersionName.setText(""+ LanguageUtils.getVersionName(mContext.get()));
							popCompanyInfoBinding.tvContactusEmail.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
							popCompanyInfoBinding.tvContactusEmail.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick (View v) {
									Intent emailIntent= new Intent(Intent.ACTION_SENDTO);
									emailIntent.setData(Uri.parse( getResources().getString(R.string.contactus_email_head)+getResources().getString(R.string.ContactEmail)));
									emailIntent.putExtra(Intent.EXTRA_SUBJECT,  "反馈标题" );
									emailIntent.putExtra(Intent.EXTRA_TEXT,  "反馈内容" );
									//没有默认的发送邮件应用
									startActivity(Intent.createChooser(emailIntent,
											getResources().getString(R.string.contactus_choice_email)));
								}
							});
							//popupWindow.showAsDropDown(mDataBinding.flPreview,15,-popupWindow.getHeight()-20, Gravity.CENTER);
							showPopWindows(view,30,15,20);
//						}
//					}
//				});
			}
		});
		mDataBinding.dragTempContainerPreviewFragment.setAddChildDataListener(new DragTempContainer.onAddChildDataListener() {
			@Override
			public void onIsEventActionMove (DrawLineRecHint hint) {
				mDataBinding.textureViewPreviewFragment.setDrawHint(hint);
			}

			@Override
			public void onIsEventActionUp (DrawLineRecHint hint) {
				hint.setNeedDraw(false);
				mDataBinding.textureViewPreviewFragment.setDrawHint(hint);
			}
		});

		//测温模式中 工具栏 点击监听器（删除，等工具栏）
		mDataBinding.dragTempContainerPreviewFragment.setChildToolsClickListener(new DragTempContainer.OnChildToolsClickListener() {
			@Override
			public void onChildToolsClick (TempWidgetObj child, int position) {
				Log.e(TAG, "onChildToolsClick: ======preview tools position == > " + position);
				if (position==0){
					mDataBinding.dragTempContainerPreviewFragment.deleteChildView(child);
					//底层重新设置 矩形框的 数据
					if (child.getType()==3){
						//						mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
						int [] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();
						Log.e(TAG, "onChildToolsClick: ======setArea data  == >" + Arrays.toString(areaData));
						if (areaData != null){
						} else {
							areaData = new int[0];
						}
						mUvcCameraHandler.setArea(areaData);
					}
				}else {
					if (isDebug)Log.e(TAG, "onChildToolsClick: " + position);
					if (isDebug)showToast("click position ");

				}
			}

			@Override
			public void onRectChangedListener () {//移动框框之后刷新C层控件的设置
				Log.e(TAG, "onRectChangedListener:  ===== ");
				if (mUvcCameraHandler != null && mUvcCameraHandler.isOpened()){
//					mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
					int [] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();
					if (areaData != null){
					} else {
						areaData = new int[0];
					}
					mUvcCameraHandler.setArea(areaData);
				}
			}

			@Override
			public void onClearAllListener () {
				if (mUvcCameraHandler != null){
					mUvcCameraHandler.setArea(new int[0]);
				}
			}

			@Override
			public void onSetParentUnselect () {
				mDataBinding.dragTempContainerPreviewFragment.setAllChildUnSelect();
			}
		});
	}

	@Override
	protected void initView () {
		sp = mContext.get().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		configuration = getResources().getConfiguration();
		metrics = getResources().getDisplayMetrics();

		if (sp.getInt(DYConstants.FIRST_RUN,1) == 0){
			isFirstRun = true;
			sp.edit().putInt(DYConstants.FIRST_RUN,1).apply();
		}
		if (isFirstRun){//第一次打开应用
			//默认不打开音频录制
			sp.edit().putInt(DYConstants.RECORD_AUDIO_SETTING,1).apply();
		}

		highTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_high);
		lowTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_low);
		centerTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_center);
		normalPointBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_main_preview_measuretemp_point);

		DisplayMetrics dm = getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		mSendCommand = new SendCommand();

		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE
		,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO)
				.onExplainRequestReason(new ExplainReasonCallback() {
					@Override
					public void onExplainReason (@NonNull ExplainScope scope, @NonNull List<String> deniedList) {
						scope.showRequestReasonDialog(deniedList,getResources().getString(R.string.toast_base_permission_explain),
								getResources().getString(R.string.confirm),getResources().getString(R.string.cancel));
					}
				}).onForwardToSettings(new ForwardToSettingsCallback() {
			@Override
			public void onForwardToSettings (@NonNull ForwardScope scope, @NonNull List<String> deniedList) {
				scope.showForwardToSettingsDialog(deniedList,getResources().getString(R.string.toast_base_permission_tosetting),
						getResources().getString(R.string.confirm),getResources().getString(R.string.cancel));
			}
		}).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted){//基本权限被授予之后才能初始化监听器。
					mFontSize = FontUtils.adjustFontSize(screenWidth, screenHeight);//
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
					mDataBinding.customSeekbarPreviewFragment.setPalette(0);

					initListener();

					initRecord();
				}else {
					showToast(getResources().getString(R.string.toast_dont_have_permission));
				}
			}
		});

		mDataBinding.dragTempContainerPreviewFragment.setmSeekBar(mDataBinding.customSeekbarPreviewFragment);
		mDataBinding.dragTempContainerPreviewFragment.setTempSuffix(sp.getInt(DYConstants.TEMP_UNIT_SETTING,0));

//		Log.e(TAG, "initView:  ==111111= " + System.currentTimeMillis());
//		mDataBinding.textureViewPreviewFragment.setAspectRatio(256/(float)192);
		mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(),
				mDataBinding.textureViewPreviewFragment,1,
				384,292,1,null,0);
//		Log.e(TAG, "initView: before  " +System.currentTimeMillis());

		fl = mDataBinding.flPreview;

		mDataBinding.setPf(this);
		mViewModel = new ViewModelProvider(getViewModelStore(),
				new ViewModelProvider.AndroidViewModelFactory((Application) mContext.get().getApplicationContext())).get(PreViewViewModel.class);

		mViewModel.setDeviceConnectListener(onDeviceConnectListener);

		mDataBinding.setPreviewViewModel(mViewModel);

//		Log.e(TAG, "initView: ============================================== ");
	}

	/**
	 * 设置页面里面的数据
	 */
	private float refreshValueByTempUnit(float temp){
		float result = 0.0f;
		switch (sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)){
			case 0:
				result = temp;
				break;
			case 1:
				result = temp* 1.8f +32;
				break;
			case 2:
				result = temp + 273.15f;
				break;
		}
		return  result;
	}

	/**
	 * 通过单位 去 计算各个边界的实际值
	 * @param value
	 * @return 边界的 真实值（带有温度单位的值）
	 */
	private float getBorderValue(float value){
		switch (sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)){
			case 0:
				value = value;
				break;
			case 1:
				value = value* 1.8f +32;
				break;
			case 2:
				value = value + 273.15f;
				break;
		}
		return value;
	}

	/**
	 * 计算各个边界的值
	 * @param value
	 * @return 边界的 真实值（带有温度单位的值）
	 */
	private float inputValue2Temp(float value){
		switch (sp.getInt(DYConstants.TEMP_UNIT_SETTING,0)){
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
	 *显示pop弹窗
	 * @param view
	 * @param widthMargin
	 * @param XOffset
	 * @param YOffset
	 */
	private void showPopWindows(View view,int widthMargin,int XOffset, int YOffset){
		allPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		allPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
		allPopupWindows.setHeight(allPopupWindows.getContentView().getMeasuredHeight());
		allPopupWindows.setWidth(fl.getWidth()- widthMargin);

		allPopupWindows.setFocusable(false);
		allPopupWindows.setOutsideTouchable(true);
		allPopupWindows.setTouchable(true);

		allPopupWindows.showAsDropDown(mDataBinding.flPreview,XOffset,-allPopupWindows.getHeight()-YOffset, Gravity.CENTER);
	}
	CompoundButton.OnCheckedChangeListener highLowCenterCheckListener = (buttonView, isChecked) -> {
		switch (buttonView.getId()){
			case R.id.cb_main_preview_highlowcenter_trace_high:
				if (isChecked){
					mDataBinding.textureViewPreviewFragment.openFeaturePoints(0);
				}else {
					mDataBinding.textureViewPreviewFragment.closeFeaturePoints(0);
				}
				break;
			case R.id.cb_main_preview_highlowcenter_trace_low:
				if (isChecked){
					mDataBinding.textureViewPreviewFragment.openFeaturePoints(1);
				}else {
					mDataBinding.textureViewPreviewFragment.closeFeaturePoints(1);
				}
				break;
			case R.id.cb_main_preview_highlowcenter_trace_center:
				if (isChecked){
					mDataBinding.textureViewPreviewFragment.openFeaturePoints(2);
				}else {
					mDataBinding.textureViewPreviewFragment.closeFeaturePoints(2);
				}
				break;
		}
	};

	//用户添加 测温模式 切换
	View.OnClickListener tempModeCheckListener = new View.OnClickListener() {
		@Override
		public void onClick (View v) {
			switch (v.getId()){
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

	/**
	 * 初始化录制的相关方法
	 */
	private void initRecord(){
//		if (isDebug)Log.e(TAG, "initRecord: ");
//		MediaProjectionHelper.getInstance().setNotificationEngine(new MediaProjectionNotificationEngine() {
//			@Override
//			public Notification getNotification () {
//				String title = getString(R.string.StartRecorderService);
//				return NotificationHelper.getInstance().createSystem()
//						.setOngoing(true)// 常驻通知栏
//						.setTicker(title)
//						.setContentText(title)
//						.setDefaults(Notification.DEFAULT_ALL)
//						.build();
//			}
//		});
//		//录制倒计时方法
//		MediaProjectionHelper.getInstance().setMediaRecorderCallback(new MediaRecorderCallback() {
//			@Override
//			public void onSuccess (File file) {
//				super.onSuccess(file);
//				mContext.get().runOnUiThread(new Runnable() {
//					@Override
//					public void run () {
//						Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//						intent.setData(Uri.fromFile(file));
//						mContext.get().sendBroadcast(intent);
//						showToast(getResources().getString(R.string.toast_save_path) + file.getName());
//					}
//				});
//
//			}
//			@Override
//			public void onFail () {
//				super.onFail();
//				mContext.get().runOnUiThread(new Runnable() {
//					@Override
//					public void run () {
//						showToast("录制失败");
//					}
//				});
//			}
//			@Override
//			public void onStartCounting () {
//				super.onStartCounting();
//				//				Log.e(TAG, "onClick: "+mDataBinding.chronometerShowRecordTimeMainInfo.getBase());
//				mContext.get().runOnUiThread(new Runnable() {
//					@Override
//					public void run () {
//						mDataBinding.chronometerRecordTimeInfo.setVisibility(View.VISIBLE);
////						mDataBinding.ivPreviewLeftRecord.setChecked(true);
//						mDataBinding.chronometerRecordTimeInfo.setBase(SystemClock.elapsedRealtime());
//						mDataBinding.chronometerRecordTimeInfo.setFormat("%s");
//						mDataBinding.chronometerRecordTimeInfo.start();
//					}
//				});
//
//			}
//
//			@Override
//			public void onStopCounting () {
//				super.onStopCounting();
//				mContext.get().runOnUiThread(new Runnable() {
//					@Override
//					public void run () {
//						mDataBinding.chronometerRecordTimeInfo.stop();
////						mDataBinding.ivPreviewLeftRecord.setChecked(false);
//						mDataBinding.chronometerRecordTimeInfo.setVisibility(View.INVISIBLE);
//					}
//				});
//
//			}
//		});
	}
	//设置画板
	private void setPalette(int id){
		if (allPopupWindows != null){
			allPopupWindows.dismiss();
		}
		if (mUvcCameraHandler!= null && mUvcCameraHandler.isPreviewing() &&id <6){
			mUvcCameraHandler.setPalette(id+1);
			mDataBinding.customSeekbarPreviewFragment.setPalette(id);
			mDataBinding.customSeekbarPreviewFragment.invalidate();//刷新控件
		}
	}

	//色板切换
	View.OnClickListener paletteChoiceListener = v -> {
		switch (v.getId()){
			case R.id.palette_layout_Tiehong:
				setPalette(0);
//					if (isDebug)showToast("palette_layout_Tiehong ");
				break;
			case R.id.palette_layout_Caihong:
				setPalette(1);
//					if (isDebug)showToast("palette_layout_Caihong ");
				break;
			case R.id.palette_layout_Hongre:
				setPalette(2);
//					if (isDebug)showToast("palette_layout_Hongre ");
				break;
			case R.id.palette_layout_Heire:
				setPalette(3);
//					if (isDebug)showToast("palette_layout_Heire ");
				break;
			case R.id.palette_layout_Baire:
				setPalette(4);
//					if (isDebug)showToast("palette_layout_Baire ");
				break;
			case R.id.palette_layout_Lenglan:
				setPalette(5);
//					if (isDebug)showToast("palette_layout_Lenglan ");
				break;
		}
	};
	//绘制图表的监听器
	View.OnClickListener chartModeCheckListener = new View.OnClickListener() {
		@Override
		public void onClick (View v) {
			switch (v.getId()){
				case R.id.iv_chart_mode_point:
//					if (isDebug)Toast.makeText(mContext.get(),"iv_chart_mode_point ", Toast.LENGTH_SHORT).show();
					break;
				case R.id.iv_chart_mode_rectangle:
//					if (isDebug)Toast.makeText(mContext.get(),"iv_chart_mode_rectangle ", Toast.LENGTH_SHORT).show();
					break;
//				case R.id.tv_check_version://版本更新
//					if (isDebug)Toast.makeText(mContext.get(),"company check_version ", Toast.LENGTH_SHORT).show();
//					break;
			}
		}
	};
	//相册 按钮
	public void toGallery(View view){
		if (mDataBinding.btPreviewLeftRecord.isSelected()){
			showToast(getResources().getString(R.string.toast_is_recording));
			return;
		}
		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE
				,Manifest.permission.WRITE_EXTERNAL_STORAGE).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted){
					if (mUvcCameraHandler != null){
						mUvcCameraHandler.release();
						mUvcCameraHandler = null;
					}
					if (stt != null ){stt.release();stt = null;}


					if (mViewModel.getMUsbMonitor().getValue().isRegistered()){
						mViewModel.getMUsbMonitor().getValue().unregister();
					}
//					if (mUvcCameraHandler!=null){
//					}
					Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_previewFg_to_galleryFg);
				}
			}
		});
	}
	//清除 按钮
	public void toClear(View view){
		mDataBinding.dragTempContainerPreviewFragment.clearAll();
	}
	//拍照 按钮
	public void toImage(View view){
		if (mUvcCameraHandler == null)return;
		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE
				,Manifest.permission.WRITE_EXTERNAL_STORAGE)
				.onExplainRequestReason(new ExplainReasonCallback() {
					@Override
					public void onExplainReason (@NonNull ExplainScope scope, @NonNull List<String> deniedList) {
						scope.showRequestReasonDialog(deniedList,getResources().getString(R.string.toast_base_permission_explain),
								getResources().getString(R.string.confirm),getResources().getString(R.string.cancel));
					}
				}).onForwardToSettings(new ForwardToSettingsCallback() {
			@Override
			public void onForwardToSettings (@NonNull ForwardScope scope, @NonNull List<String> deniedList) {
				//这个需要
				scope.showForwardToSettingsDialog(deniedList,getResources().getString(R.string.toast_base_permission_tosetting),
						getResources().getString(R.string.confirm),getResources().getString(R.string.cancel));
			}
		}).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted){//拿到权限 去C++ 绘制 传入文件路径path， 点线矩阵
					//生成一个当前的图片地址：  然后设置一个标识位，标识正截屏 或者 录像中
					if (mUvcCameraHandler != null && mUvcCameraHandler.isOpened()){

						String picPath = Objects.requireNonNull(MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".jpg")).toString();
						if (mUvcCameraHandler.captureStill(picPath))showToast(getResources().getString(R.string.toast_save_path)+picPath );
//						if (isDebug)Log.e(TAG, "onResult: java path === "+ picPath);
					}else {
						showToast(getResources().getString(R.string.toast_need_connect_camera));
					}
				}else {
					showToast(getResources().getString(R.string.toast_dont_have_permission));
				}
			}
		});
	}

	private boolean hasPermissions(String ... permissions){
		if (checkPermission(permissions)) {
			return true;
		}else {
			return false;
		}
	}

//	public static class Check {
//		// 两次点击按钮之间的点击间隔不能少于1000毫秒
//		private static final int MIN_CLICK_DELAY_TIME = 1000;
//		private static long firstClick;
//		private static long lastClickTime;
//
//		public static boolean isFastClick() {
//			boolean flag = false;
//			long curClickTime = System.currentTimeMillis();
//			if (firstClick==0)firstClick = curClickTime;
//			lastClickTime = curClickTime;
//			//最后一次点击 减去 刚开始点击的 大于间隔
//			if ((lastClickTime - firstClick) >= MIN_CLICK_DELAY_TIME) {
//				flag = true;
//			}
//			return flag;
//		}
//	}
	//录制
	public void toRecord(View view){
		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE
				,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
				.onExplainRequestReason(new ExplainReasonCallback() {
					@Override
					public void onExplainReason (@NonNull ExplainScope scope, @NonNull List<String> deniedList) {
						scope.showRequestReasonDialog(deniedList,getResources().getString(R.string.toast_base_permission_explain),
								getResources().getString(R.string.confirm),getResources().getString(R.string.cancel));
					}
				}).onForwardToSettings(new ForwardToSettingsCallback() {
			@Override
			public void onForwardToSettings (@NonNull ForwardScope scope, @NonNull List<String> deniedList) {
				//这个需要
				scope.showForwardToSettingsDialog(deniedList,getResources().getString(R.string.toast_base_permission_tosetting),
						getResources().getString(R.string.confirm),getResources().getString(R.string.cancel));
			}
		}).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted){
					/**
					 * 录制业务逻辑：点击开始录制，判断 是否在录制？ no-> 开始录制，更改录制按钮"结束录制" & 开始计时器
					 * yes->结束录制。更改录制按钮"录制" （刷新媒体库）& 重置计时器
					 */
//					if ( !Check.isFastClick() && mDataBinding.btPreviewLeftRecord.isSelected()){
//						showToast("录制时长应大于1S");
//						return;
//					}
					if (mUvcCameraHandler != null && mUvcCameraHandler.isTemperaturing() ){//mUvcCameraHandler.isOpened()
						if (mDataBinding.btPreviewLeftRecord.isSelected() && mUvcCameraHandler.isRecording()){//停止录制
							stopTimer();
							mUvcCameraHandler.stopRecording();
//							mDataBinding.ivPreviewLeftGallery.setEnabled(true);
//							Check.firstClick = 0;
						}else if (!mDataBinding.btPreviewLeftRecord.isSelected() && !mUvcCameraHandler.isRecording()&& mUvcCameraHandler.isPreviewing()){//开始录制
							startTimer();
							mUvcCameraHandler.startRecording(sp.getInt(DYConstants.RECORD_AUDIO_SETTING,1));
//							mDataBinding.ivPreviewLeftGallery.setEnabled(false);
						}else {
							Log.e(TAG, "Record Error: error record state !");
						}
						mDataBinding.btPreviewLeftRecord.setSelected(!mDataBinding.btPreviewLeftRecord.isSelected());
					}else {
						showToast(getResources().getString(R.string.toast_need_connect_camera));
					}
				}else {
					showToast(getResources().getString(R.string.toast_dont_have_permission));
				}
			}
		});
		Log.e(TAG, "toRecord: ");
//				Log.e(TAG, "toRecord: " + MediaProjectionHelper.getInstance().getRecord_State());
	}

	/**
	 * 开始计时
	 */
	private void startTimer(){
		Log.e(TAG, "startTimer: ");
		mDataBinding.chronometerRecordTimeInfo.setVisibility(View.VISIBLE);
		mDataBinding.chronometerRecordTimeInfo.setBase(SystemClock.elapsedRealtime());
		mDataBinding.chronometerRecordTimeInfo.setFormat("%s");
		mDataBinding.chronometerRecordTimeInfo.start();
	}
	/**
	 * 停止计时
	 */
	private void stopTimer(){
		Log.e(TAG, "stopTimer: ");
		mDataBinding.chronometerRecordTimeInfo.stop();
		mDataBinding.chronometerRecordTimeInfo.setVisibility(View.INVISIBLE);
	}



	/**
	 * 设置语言
	 * @param type
	 */
	private void toSetLanguage(int type) {//切换语言
		Locale locale;

		Context context = DYTApplication.getInstance();
		if (type == 0) {
			locale = Locale.SIMPLIFIED_CHINESE;
			LanguageUtils.saveAppLocaleLanguage(locale.toLanguageTag());
		}  else if (type == 1) {
			locale = Locale.US;
			LanguageUtils.saveAppLocaleLanguage(locale.toLanguageTag());
		} else {
			return;
		}
		if (LanguageUtils.isSimpleLanguage(context, locale)) {
			Toast.makeText(context, "选择的语言和当前语言相同", Toast.LENGTH_SHORT).show();
			return;
		}
		LanguageUtils.updateLanguage(context, locale);//更新语言参数

		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		context.startActivity(intent);
//		android.os.Process.killProcess(android.os.Process.myPid());
//		System.exit(0);
	}
	//修改机芯参数
	public class SendCommand {
		int psitionAndValue0 = 0, psitionAndValue1 = 0, psitionAndValue2 = 0, psitionAndValue3 = 0;

		public void sendFloatCommand(int position, byte value0, byte value1, byte value2, byte value3, int interval0, int interval1, int interval2, int interval3, int interval4) {
			psitionAndValue0 = (position << 8) | (0x000000ff & value0);
			Handler handler0 = new Handler();
			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
				}
			}, interval0);

			psitionAndValue1 = ((position + 1) << 8) | (0x000000ff & value1);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue1);
				}
			}, interval1);
			psitionAndValue2 = ((position + 2) << 8) | (0x000000ff & value2);

			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue2);
				}
			}, interval2);

			psitionAndValue3 = ((position + 3) << 8) | (0x000000ff & value3);

			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue3);
				}
			}, interval3);

			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.whenShutRefresh();
				}
			}, interval4);
		}

		private void whenChangeTempPara() {
			if (mUvcCameraHandler != null) {
				mUvcCameraHandler.whenChangeTempPara();
			}
		}

		public void sendShortCommand(int position, byte value0, byte value1, int interval0, int interval1, int interval2) {
			psitionAndValue0 = (position << 8) | (0x000000ff & value0);
			Handler handler0 = new Handler();
			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
				}
			}, interval0);

			psitionAndValue1 = ((position + 1) << 8) | (0x000000ff & value1);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue1);
				}
			}, interval1);

			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.whenShutRefresh();
				}
			}, interval2);

		}

		public void sendByteCommand(int position, byte value0, int interval0) {
			psitionAndValue0 = (position << 8) | (0x000000ff & value0);
			Handler handler0 = new Handler();
			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
				}
			}, interval0);
			handler0.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUvcCameraHandler.whenShutRefresh();
				}
			}, interval0 + 20);
		}
	}

}
