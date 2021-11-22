package com.dyt.wcc.dytpir.ui.preview;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.dyt.wcc.cameracommon.encoder.MediaMuxerWrapper;
import com.dyt.wcc.cameracommon.usbcameracommon.UVCCameraHandler;
import com.dyt.wcc.cameracommon.utils.ByteUtil;
import com.dyt.wcc.common.base.BaseApplication;
import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.common.utils.AssetCopyer;
import com.dyt.wcc.common.utils.CreateBitmap;
import com.dyt.wcc.common.utils.FontUtils;
import com.dyt.wcc.common.widget.MyCustomRangeSeekBar;
import com.dyt.wcc.common.widget.SwitchMultiButton;
import com.dyt.wcc.common.widget.dragView.DragTempContainer;
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
import com.dyt.wcc.dytpir.ui.preview.record.MediaProjectionHelper;
import com.dyt.wcc.dytpir.ui.preview.record.MediaProjectionNotificationEngine;
import com.dyt.wcc.dytpir.ui.preview.record.MediaRecorderCallback;
import com.dyt.wcc.dytpir.ui.preview.record.NotificationHelper;
import com.dyt.wcc.dytpir.utils.LanguageUtils;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
	private USBMonitor.OnDeviceConnectListener onDeviceConnectListener;
	private UVCCameraHandler                   mUvcCameraHandler;
	private Surface                            stt;
	private PopupWindow                        PLRPopupWindows;//点线矩形测温弹窗
	private PopupWindow                        allPopupWindows;
	private Map<String ,Float>                 cameraParams ;
	private SharedPreferences sp;

//	private USBMonitor mUsbMonitor ;
	private int mTextureViewWidth,mTextureViewHeight;

	private FrameLayout fl;

	private int mFontSize;
	private int paletteType , isWatermark ,isTempShow;
	private String palettePath;
	private Bitmap mCursorBlue, mCursorRed, mCursorYellow, mCursorGreen, mWatermarkLogo;//显示温度的数据(基础温度数据：最高,最低,中心,LOGO)
	//customSeekBar
	private Bitmap tiehong = null, caihong = null, baire = null, heire = null, hongre = null, lenglan = null;
	private SendCommand mSendCommand;

	@Override
	protected boolean isInterceptBackPress () {
		return false;
	}

	@Override
	protected int bindingLayout () {
		return R.layout.fragment_preview_main;
	}
	private void getCameraParams(){//得到返回机芯的参数，128位。返回解析保存在cameraParams 中
		byte [] tempParams = mUvcCameraHandler.getTemperaturePara(128);
		cameraParams = ByteUtilsCC.byte2Float(tempParams);
	}

	@Override
	public void onPause () {

		super.onPause();
		if (isDebug)Log.e(TAG, "onPause: ");
	}

	@Override
	public void onDetach () {
		super.onDetach();
		if (isDebug)Log.e(TAG, "onDetach: ");
	}

	@Override
	public void onDestroy () {
		super.onDestroy();
		if (MediaProjectionHelper.getInstance().getRecord_State() != 0){
			MediaProjectionHelper.getInstance().stopMediaRecorder();
			MediaProjectionHelper.getInstance().stopService(mContext.get());
		}

		if (isDebug)Log.e(TAG, "onDestroy: ");
	}

	@Override
	public void onDestroyView () {
		super.onDestroyView();
		if (isDebug)Log.e(TAG, "onDestroyView: ");

		if (mDataBinding.dragTempContainerPreviewFragment.isHighTempAlarmToggle()){
			mDataBinding.dragTempContainerPreviewFragment.closeHighTempAlarm();
		}

		if (mUvcCameraHandler != null && mUvcCameraHandler.isOpened()){
//			Toast.makeText(mContext.get(),"录制 ", Toast.LENGTH_SHORT).show();
			mUvcCameraHandler.stopTemperaturing();
			mUvcCameraHandler.stopPreview();
			mUvcCameraHandler.release();
		}
		if (mViewModel.getMUsbMonitor().getValue().isRegistered()){
			mViewModel.getMUsbMonitor().getValue().unregister();
		}

	}

	@Override
	public void onResume () {
		super.onResume();
//		if (isDebug)Log.e(TAG, "onResume: ");


		//		Spinner
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

		if (!mViewModel.getMUsbMonitor().getValue().isRegistered()){
			mViewModel.getMUsbMonitor().getValue().register();
		}
	}
	private int setValue(final int flag, final int value) {//设置机芯参数,调用JNI层
		return mUvcCameraHandler != null ? mUvcCameraHandler.setValue(flag, value) : 0;
	}

	private void startPreview () {//打开连接 调用预览图像的设置
		stt = new Surface(mDataBinding.textureViewPreviewFragment.getSurfaceTexture());

		mTextureViewWidth = mDataBinding.textureViewPreviewFragment.getWidth();
		mTextureViewHeight = mDataBinding.textureViewPreviewFragment.getHeight();
		if (isDebug)Log.e(TAG,"height =="+ mTextureViewHeight + " width==" + mTextureViewWidth);

		mDataBinding.textureViewPreviewFragment.iniTempBitmap(mTextureViewWidth, mTextureViewHeight);//初始化画板的值，是控件的像素的宽高
		mDataBinding.textureViewPreviewFragment.setDragTempContainer(mDataBinding.dragTempContainerPreviewFragment);
		mDataBinding.customSeekbarPreviewFragment.setmThumbListener(new MyCustomRangeSeekBar.ThumbListener() {
			@Override
			public void thumbChanged (float maxPercent, float minPercent,float maxValue, float minValue) {
				if (mUvcCameraHandler!= null)mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent,maxValue,minValue);
			}

			@Override
			public void onUpMinThumb (float maxPercent, float minPercent,float maxValue, float minValue) {
//				if (isDebug)Log.e(TAG, "onUpMinThumb: 0-100 percent " + maxPercent + " min == > " +  minPercent);
//				if (isDebug)Log.e(TAG, "onUpMinThumb: value " + maxValue + " min == > " +  minValue);

				if (maxPercent >= 100 && minPercent <= 0) {
					if (mUvcCameraHandler!= null)mUvcCameraHandler.disWenKuan();
				}
			}

			@Override
			public void onUpMaxThumb (float maxPercent, float minPercent,float maxValue, float minValue) {
//				if (isDebug)Log.e(TAG, "onUpMaxThumb: 0-100 percent " + maxPercent + " min == > " +  minPercent);
//				if (isDebug)Log.e(TAG, "onUpMaxThumb: value " + maxValue + " min == > " +  minValue);
				if (maxPercent >= 100 && minPercent <= 0) {
					if (mUvcCameraHandler!= null)mUvcCameraHandler.disWenKuan();
				}
			}

			@Override
			public void onMinMove (float maxPercent, float minPercent,float maxValue, float minValue) {
//				if (isDebug)Log.e(TAG, "onMinMove: 0-100 percent " + maxPercent + " min == > " +  minPercent);
//				if (isDebug)Log.e(TAG, "onMinMove: value " + maxValue + " min == > " +  minValue);
				if (mUvcCameraHandler!= null)mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent,maxValue,minValue);
			}

			@Override
			public void onMaxMove (float maxPercent, float minPercent,float maxValue, float minValue) {
//				if (isDebug)Log.e(TAG, "onMaxMove: 0-100 percent" + maxPercent + " min == > " +  minPercent);
//				if (isDebug)Log.e(TAG, "onMaxMove: value " + maxValue + " min == > " +  minValue);
				if (mUvcCameraHandler!= null)mUvcCameraHandler.seeKBarRangeSlided(maxPercent, minPercent,maxValue,minValue);
			}
		});

		paletteType =1;
		mUvcCameraHandler.PreparePalette(palettePath,paletteType);
		mUvcCameraHandler.setAreaCheck(0);
//		mUvcCameraHandler.setPalette(2);

		//是否进行温度的绘制
		isTempShow = 0;
		mUvcCameraHandler.tempShowOnOff(isTempShow);//是否显示绘制的温度 0不显示，1显示。最终调用的是UVCCameraTextureView的绘制线程。

		mUvcCameraHandler.startPreview(stt);
		mUvcCameraHandler.startTemperaturing();//温度回调

	}

	@Override
	protected void initView () {
		sp = mContext.get().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		mSendCommand = new SendCommand();

		DisplayMetrics dm = getResources().getDisplayMetrics();
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;
		mFontSize = FontUtils.adjustFontSize(screenWidth, screenHeight);//
		AssetCopyer.copyAllAssets(requireActivity().getApplicationContext(), requireActivity().getExternalFilesDir(null).getAbsolutePath());
		//		Log.e(TAG,"===========getExternalFilesDir=========="+this.getExternalFilesDir(null).getAbsolutePath());
		palettePath = requireActivity().getExternalFilesDir(null).getAbsolutePath();

		//测温点标志
		mWatermarkLogo = BitmapFactory.decodeResource(getResources(), R.mipmap.logo);//水印
		mCursorYellow = BitmapFactory.decodeResource(getResources(), R.mipmap.cursoryellow);
		mCursorRed = BitmapFactory.decodeResource(getResources(), R.mipmap.cursorred);
		mCursorBlue = BitmapFactory.decodeResource(getResources(), R.mipmap.cursorblue);
		mCursorGreen = BitmapFactory.decodeResource(getResources(), R.mipmap.cursorgreen);

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

		mDataBinding.customSeekbarPreviewFragment.setmProgressBarSelectBgList(bitmaps);
//		Log.e(TAG, "initView: sp.get Palette_Number = " + sp.getInt(DYConstants.PALETTE_NUMBER,0));
		mDataBinding.customSeekbarPreviewFragment.setPalette(0);
		mDataBinding.dragTempContainerPreviewFragment.setmSeekBar(mDataBinding.customSeekbarPreviewFragment);
		mDataBinding.dragTempContainerPreviewFragment.setTempSuffix(sp.getInt(DYConstants.TEMP_UNIT_SETTING,0));

//		mDataBinding.textureViewPreviewFragment.setAspectRatio(256/(float)192);
		mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(),
				mDataBinding.textureViewPreviewFragment,1,
				384,292,1,null,0);

		onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
			@Override
			public void onAttach (UsbDevice device) {
				if (isDebug)Log.e(TAG, "DD  onAttach: "+ device.toString());
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (isDebug)Log.e(TAG, "检测到设备========");
							mViewModel.getMUsbMonitor().getValue().requestPermission(device);
						}
					}, 100);
			}
			@Override
			public void onConnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
				if (isDebug)Log.e(TAG, "onConnect: ");

				if (mUvcCameraHandler != null && !mUvcCameraHandler.isReleased()){
					if (isDebug)Log.e(TAG, "onConnect: != null");

					mUvcCameraHandler.open(ctrlBlock);
					startPreview();

					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							setValue(UVCCamera.CTRL_ZOOM_ABS, DYConstants.CAMERA_DATA_MODE_8004);//切换数据输出8004原始8005yuv,80ff保存
						}
					}, 300);
				}else {
					mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(),
							mDataBinding.textureViewPreviewFragment,1,
							384,292,1,null,0);

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
			}
			@Override
			public void onDettach (UsbDevice device) {
//				mUvcCameraHandler.close();
				if (isDebug)Log.e(TAG, "DD  onDetach: ");

				if (mUvcCameraHandler != null){
					//					Toast.makeText(mContext.get(),"录制 ", Toast.LENGTH_SHORT).show();
					//					SurfaceTexture stt = mDataBinding.textureViewPreviewFragment.getSurfaceTexture();
					mUvcCameraHandler.stopTemperaturing();
					mUvcCameraHandler.stopPreview();
					mUvcCameraHandler.release();//拔出之时没释放掉这个资源。关闭窗口之时必须释放
				}
			}
			@Override
			public void onDisconnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
				if (isDebug)Log.e(TAG, " DD  onDisconnect: ");



			}
			@Override
			public void onCancel (UsbDevice device) {
				if (isDebug)Log.e(TAG, "DD  onCancel: ");
			}
		};

//		mUsbMonitor = new USBMonitor(mContext.get(),deviceConnectListener);

		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE
				,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,Manifest.permission.RECORD_AUDIO).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted){
					initRecord();
				}
			}
		});

		fl = mDataBinding.flPreview;

		//超温警告
		mDataBinding.toggleHighTempAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
								//判断键入的值是否符合规范,或者提示用户 键入值的规范。
				if (TextUtils.isEmpty(mDataBinding.etInputTempRightRLContainer.getText().toString())){
					showToast("请输入高温界限");
//					Toast.makeText(mContext.get(), ,Toast.LENGTH_SHORT).show();
//					Log.e(TAG, "onClick: "+ isChecked);
					mDataBinding.toggleHighTempAlarm.setChecked(false);
					return;
				}

				float temp = Float.parseFloat(mDataBinding.etInputTempRightRLContainer.getText().toString());
				Log.e(TAG, "onClick: temp = >  " + temp);
				if (isChecked){
					mDataBinding.dragTempContainerPreviewFragment.openHighTempAlarm(temp);
				}else {
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

					allPopupWindows.setFocusable(true);
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

				PLRPopupWindows.setFocusable(true);
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

					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceHigh.setChecked(mDataBinding.dragTempContainerPreviewFragment.hasOpenToggleByType(1));
					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceLow.setChecked(mDataBinding.dragTempContainerPreviewFragment.hasOpenToggleByType(2));
					popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceCenter.setChecked(mDataBinding.dragTempContainerPreviewFragment.hasOpenToggleByType(3));

					PLRPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					PLRPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
					PLRPopupWindows.setHeight(mDataBinding.llContainerPreviewSeekbar.getHeight());
					PLRPopupWindows.setWidth(mDataBinding.llContainerPreviewSeekbar.getWidth());

					PLRPopupWindows.setFocusable(true);
					PLRPopupWindows.setOutsideTouchable(true);
					PLRPopupWindows.setTouchable(true);

					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar,0,-mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);

//					PLRPopupWindows.setOnDismissListener(new PopupWindow.OnDismissListener() {
//						@Override
//						public void onDismiss () {
//							popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceHigh.setChecked(false);
//							popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceLow.setChecked(false);
//							popHighlowcenterTraceBinding.cbMainPreviewHighlowcenterTraceCenter.setChecked(false);
//						}
//					});
				}
			}
		});

		//框内细查
		mDataBinding.toggleAreaCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
				if (isChecked){
					mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
					int [] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();
					mUvcCameraHandler.setArea(areaData);
					mUvcCameraHandler.setAreaCheck(1);
				}else {//close
					mUvcCameraHandler.setAreaCheck(0);
				}
			}
		});

		//固定温度条
		mDataBinding.toggleFixedTempBar
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
						if (isChecked){
							mDataBinding.customSeekbarPreviewFragment.setWidgetMode(1);
						}else {
							mDataBinding.customSeekbarPreviewFragment.setWidgetMode(0);
							if (mUvcCameraHandler!=null)mUvcCameraHandler.disWenKuan();
						}
						if (mUvcCameraHandler!=null)mUvcCameraHandler.fixedTempStripChange(isChecked);
					}
				});


//		//绘制图表的弹窗
//		mDataBinding.ivPreviewRightChart.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick (View v) {
//				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_draw_chart,null);
//				PopDrawChartBinding popDrawChartBinding = DataBindingUtil.bind(view);
//				assert popDrawChartBinding != null;
//				popDrawChartBinding.ivChartModePoint.setOnClickListener(chartModeCheckListener);
//				popDrawChartBinding.ivChartModeRectangle.setOnClickListener(chartModeCheckListener);
//
//				showPopWindows(view,60,30,20);
//			}
//		});
		//设置弹窗
		mDataBinding.ivPreviewRightSetting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				//打开设置第一步：获取机芯数据。
				if (mUvcCameraHandler.isOpened()){
					getCameraParams();//
				}
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_setting,null);

				PopSettingBinding popSettingBinding = DataBindingUtil.bind(view);
				assert popSettingBinding != null;
				//第二步：将获取的数据 展示在输入框内
				if (cameraParams != null) {
					popSettingBinding.etCameraSettingEmittance.setHint(String.valueOf(cameraParams.get("emiss")));
					popSettingBinding.etCameraSettingDistance.setHint(String.valueOf(cameraParams.get("distance")));
					popSettingBinding.etCameraSettingReflect.setHint(String.valueOf((cameraParams.get("Refltmp"))));
					popSettingBinding.etCameraSettingRevise.setHint(String.valueOf(cameraParams.get("Fix")));
					popSettingBinding.etCameraSettingFreeAirTemp.setHint(String.valueOf(cameraParams.get("Airtmp")));
					popSettingBinding.etCameraSettingHumidity.setHint(String.valueOf(cameraParams.get("humi")));

					//设置按钮的监听器
					popSettingBinding.ibCameraSettingDistance.setOnClickListener(v1 -> {// 距离的数值在[0,20]
						//第一步 校验输入数值。
						//第二步 设置机芯。
						//第三步 成功 失败 反馈
//						Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
						if (TextUtils.isEmpty(popSettingBinding.etCameraSettingDistance.getText().toString()))return;
						int value = Integer.parseInt(popSettingBinding.etCameraSettingDistance.getText().toString());
						if (value > 20 || value < 0){
							showToast("取值范围[0,20]");
							return;
						}
						byte[] bIputDi = new byte[4];
						ByteUtil.putInt(bIputDi,value,0);
						if (mUvcCameraHandler!= null) {
							mSendCommand.sendShortCommand(5 * 4, bIputDi[0], bIputDi[1], 20, 40, 60);
//							Toast.makeText(mContext.get(),"距离设置完成",Toast.LENGTH_SHORT).show();
							showToast("距离设置完成");
						}

					});
					popSettingBinding.ibCameraSettingEmittance.setOnClickListener(v1 -> {// 发射率 [0,1]
						if (TextUtils.isEmpty(popSettingBinding.etCameraSettingEmittance.getText().toString()))return;
						float value = Float.parseFloat(popSettingBinding.etCameraSettingEmittance.getText().toString());
						if (value > 1 || value < 0){
//							Toast.makeText(mContext.get(),"取值范围为[0,1]",Toast.LENGTH_SHORT).show();
							showToast("取值范围为[0,1]");
							return;
						}
						byte[] iputEm = new byte[4];
						ByteUtil.putFloat(iputEm,value,0);
						if (mUvcCameraHandler!= null) {
							mSendCommand.sendFloatCommand(4 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
//							Toast.makeText(mContext.get(),"发射率设置完成",Toast.LENGTH_SHORT).show();
							showToast("发射率设置完成");
						}
					});
					popSettingBinding.ibCameraSettingFreeAirTemp.setOnClickListener(v1 -> {//环境温度 [-10,40]
						if (TextUtils.isEmpty(popSettingBinding.etCameraSettingFreeAirTemp.getText().toString()))return;
						float value = Float.parseFloat(popSettingBinding.etCameraSettingFreeAirTemp.getText().toString());
						if (value > 40 || value < -10){
//							Toast.makeText(mContext.get(),"取值范围[-10,40]",Toast.LENGTH_SHORT).show();
							showToast("取值范围[-10,40]");
							return;
						}
						byte[] iputEm = new byte[4];
						ByteUtil.putFloat(iputEm,value,0);
						if (mUvcCameraHandler!= null) {
							mSendCommand.sendFloatCommand(2 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
//							Toast.makeText(mContext.get(),"环境温度设置完成",Toast.LENGTH_SHORT).show();
							showToast("环境温度设置完成");
						}
					});
					popSettingBinding.ibCameraSettingHumidity.setOnClickListener(v1 -> {//湿度 [0,1]
						if (TextUtils.isEmpty(popSettingBinding.etCameraSettingHumidity.getText().toString()))return;
						float value = Float.parseFloat(popSettingBinding.etCameraSettingHumidity.getText().toString());
						if (value > 1 || value < 0){
//							Toast.makeText(mContext.get(),"取值范围[0,1]",Toast.LENGTH_SHORT).show();
							showToast("取值范围[0,1]");
							return;
						}
						byte[] iputEm = new byte[4];
						ByteUtil.putFloat(iputEm,value,0);
						if (mUvcCameraHandler!= null) {
							mSendCommand.sendFloatCommand(3 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
//							Toast.makeText(mContext.get(),"湿度设置完成",Toast.LENGTH_SHORT).show();
							showToast("湿度设置完成");
						}
					});
					popSettingBinding.ibCameraSettingReflect.setOnClickListener(v1 -> {//反射温度 [-10,40]
						if (TextUtils.isEmpty(popSettingBinding.etCameraSettingReflect.getText().toString()))return;
						float value = Float.parseFloat(popSettingBinding.etCameraSettingReflect.getText().toString());
						if (value > 40 || value < -10){
//							Toast.makeText(mContext.get(),"取值范围[-10,40]",Toast.LENGTH_SHORT).show();
							showToast("取值范围[-10,40]");
							return;
						}
						byte[] iputEm = new byte[4];
						ByteUtil.putFloat(iputEm,value,0);
						if (mUvcCameraHandler!= null) {
							mSendCommand.sendFloatCommand(1 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
//							Toast.makeText(mContext.get(),"反射温度设置完成",Toast.LENGTH_SHORT).show();
							showToast("反射温度设置完成");
						}
					});
					popSettingBinding.ibCameraSettingRevise.setOnClickListener(v1 -> {//校正 [-3,3]
						if (TextUtils.isEmpty(popSettingBinding.etCameraSettingRevise.getText().toString()))return;
						float value = Float.parseFloat(popSettingBinding.etCameraSettingRevise.getText().toString());
						if (value > 3 || value < -3){
//							Toast.makeText(mContext.get(),"取值范围[-3,3]",Toast.LENGTH_SHORT).show();
							showToast("取值范围[-3,3]");
							return;
						}
						byte[] iputEm = new byte[4];
						ByteUtil.putFloat(iputEm,value,0);
						if (mUvcCameraHandler!= null) {
							mSendCommand.sendFloatCommand(0 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
//							Toast.makeText(mContext.get(),"校正设置完成",Toast.LENGTH_SHORT).show();
							showToast("校正设置完成");
						}
					});
				}
				int temp_unit = sp.getInt(DYConstants.TEMP_UNIT_SETTING,0);
				//第三步：初始化自定义控件
				popSettingBinding.switchChoiceTempUnit.setText("℃","℉","K").setSelectedTab(temp_unit).setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
					@Override
					public void onSwitch (int position, String tabText) {
						mDataBinding.dragTempContainerPreviewFragment.setTempSuffix(position);
						sp.edit().putInt(DYConstants.TEMP_UNIT_SETTING,position).apply();
					}
				});


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
				//语言spinner
				ArrayAdapter<String> adapterSpinner = new ArrayAdapter<String>(mContext.get(),R.layout.item_select, DYConstants.languageArray);
				adapterSpinner.setDropDownViewResource(R.layout.item_dropdown);
				popSettingBinding.spinnerSettingLanguage.setAdapter(adapterSpinner);
				int language_index = sp.getInt(DYConstants.LANGUAGE_SETTING,0);
				Log.e(TAG, "onClick: language_index   " + language_index);
				popSettingBinding.spinnerSettingLanguage.setSelection(language_index);//从本地拿
				popSettingBinding.spinnerSettingLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
//						Toast.makeText(mContext.get(),"spinner item  = "+position,Toast.LENGTH_SHORT).show();
						Log.e(TAG, "onItemSelected: now " + sp.getInt(DYConstants.LANGUAGE_SETTING,0));
						if (position != sp.getInt(DYConstants.LANGUAGE_SETTING,0)){
							sp.edit().putInt(DYConstants.LANGUAGE_SETTING,position).apply();
							Log.e(TAG, "onItemSelected:  changed position == " + position);
//							Locale primaryLocal;
//							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
//								primaryLocal = getResources().getConfiguration().getLocales().get(0);
//							}else {
//								primaryLocal = getResources().getConfiguration().locale;
//							}
//							String local = primaryLocal.getDisplayName();
//							Log.e(TAG, "onItemSelected: local str " + local);
							toSetLanguage(position);
						}
					}


					@Override
					public void onNothingSelected (AdapterView<?> parent) {
						if (isDebug)Toast.makeText(mContext.get(),"spinner item  = onNothingSelected",Toast.LENGTH_SHORT).show();
					}
				});

			}
		});

		//公司信息弹窗   监听器使用的图表的监听器对象
		mDataBinding.ivPreviewLeftCompanyInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				//请求权限
				PermissionX.init(PreviewFragment.this).permissions(Manifest.permission.INTERNET).request(new RequestCallback() {
					@Override
					public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
						if (allGranted){
							View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_company_info,null);
							PopCompanyInfoBinding popCompanyInfoBinding = DataBindingUtil.bind(view);
							assert popCompanyInfoBinding != null;
							popCompanyInfoBinding.tvCheckVersion.setOnClickListener(chartModeCheckListener);

							showPopWindows(view,80,40,20);
						}
					}
				});
			}
		});

		//测温模式的 工具栏 点击监听器
		mDataBinding.dragTempContainerPreviewFragment.setChildToolsClickListener(new DragTempContainer.OnChildToolsClickListener() {
			@Override
			public void onChildToolsClick (TempWidgetObj child, int position) {

				if (position==0){
					mDataBinding.dragTempContainerPreviewFragment.removeChildByDataObj(child);
					if (child.getType()==3){
//						mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
						int [] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();
						mUvcCameraHandler.setArea(areaData);
					}
				}else {
					if (isDebug)Log.e(TAG, "onChildToolsClick: " + position);
					if (isDebug)showToast("click position ");

				}
			}

			@Override
			public void onRectChangedListener () {//移动框框之后刷新C层控件的设置
				if (mUvcCameraHandler != null){
					mDataBinding.dragTempContainerPreviewFragment.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
					int [] areaData = mDataBinding.dragTempContainerPreviewFragment.getAreaIntArray();
					mUvcCameraHandler.setArea(areaData);
				}
			}
		});


		mDataBinding.setPf(this);
		mViewModel = new ViewModelProvider(getViewModelStore(),
				new ViewModelProvider.AndroidViewModelFactory((Application) mContext.get().getApplicationContext())).get(PreViewViewModel.class);
		mDataBinding.setPreviewViewModel(mViewModel);

		PermissionX.init(this).permissions(Manifest.permission.CAMERA).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted){
					mViewModel.setDeviceConnectListener(onDeviceConnectListener);
				}
			}
		});

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

		allPopupWindows.setFocusable(true);
		allPopupWindows.setOutsideTouchable(true);
		allPopupWindows.setTouchable(true);

		allPopupWindows.showAsDropDown(mDataBinding.flPreview,XOffset,-allPopupWindows.getHeight()-YOffset, Gravity.CENTER);
	}
	CompoundButton.OnCheckedChangeListener highLowCenterCheckListener = (buttonView, isChecked) -> {
		switch (buttonView.getId()){
			case R.id.cb_main_preview_highlowcenter_trace_high:
				if (isChecked){
					mDataBinding.dragTempContainerPreviewFragment.openHighLowTemp(1);
				}else {
					mDataBinding.dragTempContainerPreviewFragment.closeHighLowTemp(1);
				}
				break;
			case R.id.cb_main_preview_highlowcenter_trace_low:
				if (isChecked){
					mDataBinding.dragTempContainerPreviewFragment.openHighLowTemp(2);
				}else {
					mDataBinding.dragTempContainerPreviewFragment.closeHighLowTemp(2);
				}
				break;
			case R.id.cb_main_preview_highlowcenter_trace_center:
				if (isChecked){
					mDataBinding.dragTempContainerPreviewFragment.openHighLowTemp(3);
				}else {
					mDataBinding.dragTempContainerPreviewFragment.closeHighLowTemp(3);
				}
				break;
		}
	};

	//温度度量模式切换
	View.OnClickListener tempModeCheckListener = new View.OnClickListener() {
		@Override
		public void onClick (View v) {
			switch (v.getId()){
				case R.id.iv_temp_mode_point:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewFragment.setDrawTempMode(1);
					if (isDebug)showToast("point ");
					break;
				case R.id.iv_temp_mode_line:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewFragment.setDrawTempMode(2);
					if (isDebug)showToast("line ");
					break;
				case R.id.iv_temp_mode_rectangle:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewFragment.setDrawTempMode(3);
					if (isDebug)showToast("rectangle ");
					break;
			}
		}
	};
	//初始化录制的相关方法
	private void initRecord(){
		if (isDebug)Log.e(TAG, "initRecord: ");

		MediaProjectionHelper.getInstance().setNotificationEngine(new MediaProjectionNotificationEngine() {
			@Override
			public Notification getNotification () {
				String title = getString(R.string.StartRecorderService);
				return NotificationHelper.getInstance().createSystem()
						.setOngoing(true)// 常驻通知栏
						.setTicker(title)
						.setContentText(title)
						.setDefaults(Notification.DEFAULT_ALL)
						.build();
			}
		});
		//录制倒计时方法
		MediaProjectionHelper.getInstance().setMediaRecorderCallback(new MediaRecorderCallback() {
			@Override
			public void onSuccess (File file) {
				super.onSuccess(file);
				mContext.get().runOnUiThread(new Runnable() {
					@Override
					public void run () {
						Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
						intent.setData(Uri.fromFile(file));
						mContext.get().sendBroadcast(intent);
						showToast("录制完成" + file.getName());
					}
				});

			}

			@Override
			public void onFail () {
				super.onFail();
				mContext.get().runOnUiThread(new Runnable() {
					@Override
					public void run () {
						showToast("录制失败");
					}
				});

			}

			@Override
			public void onStartCounting () {
				super.onStartCounting();
				//				Log.e(TAG, "onClick: "+mDataBinding.chronometerShowRecordTimeMainInfo.getBase());
				mContext.get().runOnUiThread(new Runnable() {
					@Override
					public void run () {
//						mDataBinding.chronometerRecordTimeInfo.setVisibility(View.VISIBLE);
//						mDataBinding.ivPreviewLeftRecord.setImageResource(R.drawable.ic_user_vector_stoprecord_24px);
//						mDataBinding.chronometerRecordTimeInfo.setBase(SystemClock.elapsedRealtime());
//						mDataBinding.chronometerRecordTimeInfo.setFormat("%s");
//						mDataBinding.chronometerRecordTimeInfo.start();
					}
				});

			}

			@Override
			public void onStopCounting () {
				super.onStopCounting();

				mContext.get().runOnUiThread(new Runnable() {
					@Override
					public void run () {
//						mDataBinding.chronometerRecordTimeInfo.stop();
//						mDataBinding.ivPreviewLeftRecord.setImageResource(R.drawable.ic_user_vector_record_24px);
//						mDataBinding.chronometerRecordTimeInfo.setVisibility(View.INVISIBLE);
					}
				});

			}
		});
	}

	private void setPalette(int id){
		if (allPopupWindows != null){
			allPopupWindows.dismiss();
		}
		if (mUvcCameraHandler!= null && mUvcCameraHandler.isPreviewing() &&id <6){
//			sp.edit().putInt(DYConstants.PALETTE_NUMBER,id).apply();
			mUvcCameraHandler.setPalette(id+1);
			mDataBinding.customSeekbarPreviewFragment.setPalette(id);
			mDataBinding.customSeekbarPreviewFragment.invalidate();//刷新控件
		}
	}
	//温度度量模式切换
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
					if (isDebug)Toast.makeText(mContext.get(),"iv_chart_mode_point ", Toast.LENGTH_SHORT).show();
					break;
				case R.id.iv_chart_mode_rectangle:
					if (isDebug)Toast.makeText(mContext.get(),"iv_chart_mode_rectangle ", Toast.LENGTH_SHORT).show();
					break;
				case R.id.tv_check_version://版本更新
					if (isDebug)Toast.makeText(mContext.get(),"company check_version ", Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};
	//去多媒体相册
	public void toGallery(View view){
		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE
				,Manifest.permission.WRITE_EXTERNAL_STORAGE).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted){
					Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_previewFg_to_galleryFg);
				}
			}
		});
	}
	//清除
	public void toClear(View view){
		mDataBinding.dragTempContainerPreviewFragment.clearAll();
	}
	//拍照
	public void toImage(View view){

		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE
				,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
				.onExplainRequestReason(new ExplainReasonCallback() {
					@Override
					public void onExplainReason (@NonNull ExplainScope scope, @NonNull List<String> deniedList) {
						scope.showRequestReasonDialog(deniedList,"没有存储权限，无法实现截图，录制等功能。是否重新授权？","确认","取消");
					}
				}).onForwardToSettings(new ForwardToSettingsCallback() {
			@Override
			public void onForwardToSettings (@NonNull ForwardScope scope, @NonNull List<String> deniedList) {
				scope.showForwardToSettingsDialog(deniedList,"没有存储权限，无法实现截图，录制等功能。是否去设置打开？","确认","取消");
			}
		}).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
				if (allGranted){//拿到权限 去C++ 绘制 传入文件路径path， 点线矩阵
					//生成一个当前的图片地址：  然后设置一个标识位，标识正截屏 或者 录像中
					if (!mUvcCameraHandler.isReleased()){
						String picPath = Objects.requireNonNull(MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".jpg")).toString();
						mUvcCameraHandler.captureStill(picPath);
						showToast("保存路径为："+picPath );
//						if (isDebug)Log.e(TAG, "onResult: java path === "+ picPath);
					}else {
						showToast("请先连接相机");
					}
				}else {
					showToast("请授予需要的权限.");
				}

			}
		});

//		if (mUvcCameraHandler != null){
//			Toast.makeText(mContext.get(),"toImage ", Toast.LENGTH_SHORT).show();
//			mUvcCameraHandler.startPreview(stt);
//			mUvcCameraHandler.startTemperaturing();
//		}
		//		NavHostFragment.findNavController(PreviewFragment.this).popBackStack();
		//		Navigation.findNavController(mDataBinding.getRoot()).popBackStack();

	}
	//录制
	public void toRecord(View view){
		Log.e(TAG, "toRecord: ");
		/**
		 * 录制业务逻辑：点击开始录制，判断 是否在录制？ no-> 开始录制，更改录制按钮"结束录制" & 开始计时器
		 * yes->结束录制。更改录制按钮"录制" （刷新媒体库）& 重置计时器
		 */
//				Log.e(TAG, "toRecord: " + MediaProjectionHelper.getInstance().getRecord_State());
//				if (MediaProjectionHelper.getInstance().getRecord_State() !=0 ){//停止录制
//					MediaProjectionHelper.getInstance().stopMediaRecorder();
//					MediaProjectionHelper.getInstance().stopService(mContext.get());
//				}else {//开始录制
//					MediaProjectionHelper.getInstance().startService(mContext.get());
//				}
	}
	private void toSetLanguage(int type) {//切换语言
		Locale locale;
//		Resources resources = getResources();
//		DisplayMetrics dm = resources.getDisplayMetrics();
//		Configuration configuration = resources.getConfiguration();

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
//		configuration.locale = locale;
//		resources.updateConfiguration(configuration,dm);

		if (LanguageUtils.isSimpleLanguage(context, locale)) {
			Toast.makeText(context, "选择的语言和当前语言相同", Toast.LENGTH_SHORT).show();
			return;
		}
		LanguageUtils.updateLanguage(context, locale);//更新语言参数

		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		//		this.finish();
		context.startActivity(intent);
//
//		android.os.Process.killProcess(android.os.Process.myPid());
//		System.exit(0);
	}

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
