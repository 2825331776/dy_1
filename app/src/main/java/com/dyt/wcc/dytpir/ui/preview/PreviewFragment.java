package com.dyt.wcc.dytpir.ui.preview;

import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.common.widget.SwitchMultiButton;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.FragmentPreviewMainBinding;
import com.dyt.wcc.dytpir.databinding.PopCompanyInfoBinding;
import com.dyt.wcc.dytpir.databinding.PopDrawChartBinding;
import com.dyt.wcc.dytpir.databinding.PopSettingBinding;
import com.dyt.wcc.dytpir.databinding.PopTempModeChoiceBinding;
import com.dyt.wcc.libuvccamera.usb.USBMonitor;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:17     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.ui.main     </p>
 */
public class PreviewFragment extends BaseFragment<FragmentPreviewMainBinding> {
	private PreViewViewModel mViewModel;
	private USBMonitor mUsbMonitor ;

	private FrameLayout fl;

	@Override
	protected int bindingLayout () {
		return R.layout.fragment_preview_main;
	}

	@Override
	public void onResume () {
		super.onResume();
		if (isDebug)Log.e(TAG, "onResume: ");
//		if (!mUsbMonitor.isRegistered()){
//			mUsbMonitor.register();
//		}
//
//		List<UsbDevice> mUsbDeviceList = mUsbMonitor.getDeviceList();
//		for (UsbDevice udv : mUsbDeviceList) {
//			//指定设备的连接，获取设备的名字，当前usb摄像头名为Xmodule-S0
//			Log.e(TAG, "udv.getProductName()" + udv.getProductName());
//			if (udv.getProductName().contains("Xtherm") || udv.getProductName().contains("Xmodule") ||
//					udv.getProductName().contains("T3") || udv.getProductName().contains("DL13") || udv.getProductName().contentEquals("DV")) {
//				//CameraAlreadyConnected = true;//标志红外摄像头是否已经连接上
//				BaseApplication.deviceName = udv.getProductName();
//			}
//		}
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
		if (isDebug)Log.e(TAG, "onDestroy: ");
	}

	@Override
	public void onDestroyView () {
		super.onDestroyView();
		if (isDebug)Log.e(TAG, "onDestroyView: ");
//		if (mUsbMonitor.isRegistered()){
//			mUsbMonitor.unregister();
//		}
	}

	USBMonitor.OnDeviceConnectListener deviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
		@Override
		public void onAttach (UsbDevice device) {
			if (isDebug)Log.e(TAG, "onAttach: ");
			if (device.getDeviceClass() == 239 && device.getDeviceSubclass() == 2) {
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (isDebug)Log.e(TAG, "检测到设备罢了");
						mUsbMonitor.requestPermission(device);
					}
				}, 100);
			}
		}

		@Override
		public void onDetach (UsbDevice device) {
			if (isDebug)Log.e(TAG, "onDetach: ");
		}

		@Override
		public void onConnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
			if (isDebug)Log.e(TAG, "onConnect: ");
		}

		@Override
		public void onDisconnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
			if (isDebug)Log.e(TAG, "onDisconnect: ");
		}

		@Override
		public void onCancel (UsbDevice device) {
			if (isDebug)Log.e(TAG, "onCancel: ");
		}
	};

	@Override
	protected void initView () {
		mDataBinding.setPf(this);

		mUsbMonitor = new USBMonitor(mContext.get(),deviceConnectListener);

		DisplayMetrics metrics = new DisplayMetrics();
		metrics = getResources().getDisplayMetrics();
		if (isDebug)Log.e(TAG, "initView: " + metrics.heightPixels + "wid = " + metrics.widthPixels);

		fl = mDataBinding.flPreview;
		mViewModel = new ViewModelProvider(this,new ViewModelProvider.NewInstanceFactory()).get(PreViewViewModel.class);

		//绘制  温度模式 切换  弹窗
		mDataBinding.ivPreviewRightTempMode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {

				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_temp_mode_choice,null);
				PopTempModeChoiceBinding tempModeChoiceBinding = DataBindingUtil.bind(view);
				assert tempModeChoiceBinding != null;
				tempModeChoiceBinding.ivTempModeLine.setOnClickListener(tempModeCheckListener);
				tempModeChoiceBinding.ivTempModePoint.setOnClickListener(tempModeCheckListener);
				tempModeChoiceBinding.ivTempModeRectangle.setOnClickListener(tempModeCheckListener);

				PopupWindow popupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				popupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
				popupWindow.setHeight(popupWindow.getContentView().getMeasuredHeight());
				popupWindow.setWidth(fl.getWidth()- 60);

				popupWindow.setFocusable(true);
//				popupWindow.setBackgroundDrawable(getResources().getDrawable(R.mipmap.temp_mode_bg_tempback));
				popupWindow.setOutsideTouchable(true);
				popupWindow.setTouchable(true);

				popupWindow.showAsDropDown(mDataBinding.flPreview,30,-popupWindow.getHeight()-20, Gravity.CENTER);
			}
		});

		//切换色板
		mDataBinding.ivPreviewLeftPalette.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {

				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_palette_choice,null);


				showPopWindows(view,20,10,20);
//				PopupWindow popupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//				popupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
//				popupWindow.setHeight(popupWindow.getContentView().getMeasuredHeight());
//				popupWindow.setWidth(fl.getWidth()-20);
//
//				popupWindow.setFocusable(true);
////				popupWindow.setBackgroundDrawable(getResources().getDrawable(R.mipmap.temp_mode_bg_tempback));
//				popupWindow.setOutsideTouchable(true);
//				popupWindow.setTouchable(true);
//
//				popupWindow.showAsDropDown(mDataBinding.flPreview,10,-popupWindow.getHeight()-20, Gravity.CENTER);

			}
		});
		//公司信息弹窗   监听器使用的图表的监听器对象
		mDataBinding.ivPreviewLeftCompanyInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_company_info,null);
				PopCompanyInfoBinding popCompanyInfoBinding = DataBindingUtil.bind(view);
				assert popCompanyInfoBinding != null;
				popCompanyInfoBinding.tvCheckVersion.setOnClickListener(chartModeCheckListener);

				showPopWindows(view,80,40,20);
			}
		});

		//绘制图表的弹窗
		mDataBinding.ivPreviewRightChart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_draw_chart,null);
				PopDrawChartBinding popDrawChartBinding = DataBindingUtil.bind(view);
				assert popDrawChartBinding != null;
				popDrawChartBinding.ivChartModePoint.setOnClickListener(chartModeCheckListener);
				popDrawChartBinding.ivChartModeRectangle.setOnClickListener(chartModeCheckListener);

				showPopWindows(view,60,30,20);
			}
		});
		//设置弹窗
		mDataBinding.ivPreviewRightSetting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_setting,null);

				PopSettingBinding popSettingBinding = DataBindingUtil.bind(view);
				assert popSettingBinding != null;
				popSettingBinding.switchChoiceTempUnit.setText(new String[]{"℃","℉","K"}).setSelectedTab(0).setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
					@Override
					public void onSwitch (int position, String tabText) {
						Toast.makeText(mContext.get(),""+position,Toast.LENGTH_SHORT).show();
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

				popupWindow.showAsDropDown(mDataBinding.flPreview,15,-popupWindow.getHeight()-20, Gravity.CENTER);
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
		PopupWindow popupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		popupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
		popupWindow.setHeight(popupWindow.getContentView().getMeasuredHeight());
		popupWindow.setWidth(fl.getWidth()- widthMargin);

		popupWindow.setFocusable(true);
		popupWindow.setOutsideTouchable(true);
		popupWindow.setTouchable(true);

		popupWindow.showAsDropDown(mDataBinding.flPreview,XOffset,-popupWindow.getHeight()-YOffset, Gravity.CENTER);
	}

	//温度度量模式切换
	View.OnClickListener tempModeCheckListener = new View.OnClickListener() {
		@Override
		public void onClick (View v) {
			switch (v.getId()){
				case R.id.iv_temp_mode_point:
					Toast.makeText(mContext.get(),"point ", Toast.LENGTH_SHORT).show();
					break;
				case R.id.iv_temp_mode_line:
					Toast.makeText(mContext.get(),"line ", Toast.LENGTH_SHORT).show();
					break;
				case R.id.iv_temp_mode_rectangle:
					Toast.makeText(mContext.get(),"rectangle ", Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};
	//绘制图表的监听器
	View.OnClickListener chartModeCheckListener = new View.OnClickListener() {
		@Override
		public void onClick (View v) {
			switch (v.getId()){
				case R.id.iv_chart_mode_point:
					Toast.makeText(mContext.get(),"iv_chart_mode_point ", Toast.LENGTH_SHORT).show();
					break;
				case R.id.iv_chart_mode_rectangle:
					Toast.makeText(mContext.get(),"iv_chart_mode_rectangle ", Toast.LENGTH_SHORT).show();
					break;
				case R.id.tv_check_version:
					Toast.makeText(mContext.get(),"company check_version ", Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};

	public void toGallery(View view){
		Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_previewFg_to_galleryFg);
	}

}
