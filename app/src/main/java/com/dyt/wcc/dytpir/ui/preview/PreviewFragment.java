package com.dyt.wcc.dytpir.ui.preview;

import android.Manifest;
import android.app.Application;
import android.hardware.usb.UsbDevice;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.dyt.wcc.common.base.BaseApplication;
import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.common.widget.SwitchMultiButton;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.constans.DYConstants;
import com.dyt.wcc.dytpir.databinding.FragmentPreviewMainBinding;
import com.dyt.wcc.dytpir.databinding.PopCompanyInfoBinding;
import com.dyt.wcc.dytpir.databinding.PopDrawChartBinding;
import com.dyt.wcc.dytpir.databinding.PopSettingBinding;
import com.dyt.wcc.dytpir.databinding.PopTempModeChoiceBinding;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:17     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.main     </p>
 */
public class PreviewFragment extends BaseFragment<FragmentPreviewMainBinding> {
	private PreViewViewModel mViewModel;

//	private USBMonitor mUsbMonitor ;

	private FrameLayout fl;

	@Override
	protected boolean isInterceptBackPress () {
		return false;
	}

	@Override
	protected int bindingLayout () {
//		Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_previewFg_to_galleryFg);
		return R.layout.fragment_preview_main;
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
		if (mViewModel.getMUsbMonitor().getValue().isRegistered()){
			mViewModel.getMUsbMonitor().getValue().unregister();
		}
	}

	@Override
	public void onResume () {
		super.onResume();
		if (isDebug)Log.e(TAG, "onResume: ");

		if (!mViewModel.getMUsbMonitor().getValue().isRegistered()){
			mViewModel.getMUsbMonitor().getValue().register();
		}
		//		Spinner
		//
		List<UsbDevice> mUsbDeviceList = mViewModel.getMUsbMonitor().getValue().getDeviceList();
		for (UsbDevice udv : mUsbDeviceList) {
			//指定设备的连接，获取设备的名字，当前usb摄像头名为Xmodule-S0
//			if (isDebug)Log.e(TAG, "udv.getProductName()" + udv.getProductName());
			if (udv.getProductName().contains("S0") ) {
				//CameraAlreadyConnected = true;//标志红外摄像头是否已经连接上
				BaseApplication.deviceName = udv.getProductName();
			}
		}
	}

	@Override
	protected void initView () {
		mDataBinding.setPf(this);
		mViewModel = new ViewModelProvider(getViewModelStore(),
				new ViewModelProvider.AndroidViewModelFactory((Application) mContext.get().getApplicationContext())).get(PreViewViewModel.class);
		mDataBinding.setPreviewViewModel(mViewModel);

//		mUsbMonitor = new USBMonitor(mContext.get(),deviceConnectListener);

		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE
				,Manifest.permission.WRITE_EXTERNAL_STORAGE).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
			}
		});


		DisplayMetrics metrics = new DisplayMetrics();
		metrics = getResources().getDisplayMetrics();
		if (isDebug)Log.e(TAG, "initView: " + metrics.heightPixels + "wid = " + metrics.widthPixels);

		fl = mDataBinding.flPreview;
//		mViewModel = new ViewModelProvider(this,new ViewModelProvider.NewInstanceFactory()).get(PreViewViewModel.class);

		mDataBinding.toggleShowHighLowTemp.setOnClickChangedState(checkState -> Toast.makeText(mContext.get(), "v"+ checkState,Toast.LENGTH_SHORT).show());
		mDataBinding.toggleAreaCheck.setOnClickChangedState(checkState -> Toast.makeText(mContext.get(), "v"+ checkState,Toast.LENGTH_SHORT).show());
		mDataBinding.toggleFixedTempBar.setOnClickChangedState(checkState -> Toast.makeText(mContext.get(), "v"+ checkState,Toast.LENGTH_SHORT).show());
		mDataBinding.toggleHighTempAlarm.setOnClickChangedState(checkState -> Toast.makeText(mContext.get(), "v"+ checkState,Toast.LENGTH_SHORT).show());

		//绘制  温度模式 切换  弹窗
		mDataBinding.ivPreviewRightTempMode.setOnClickListener(v -> {

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
//						Toast.makeText(mContext.get(),""+position,Toast.LENGTH_SHORT).show();


					}
				});

//				popSettingBinding.btLanguage.setOnClickListener(new View.OnClickListener() {
//					@Override
//					public void onClick (View v) {
//						Toast.makeText(mContext.get(), "btLanguage ",Toast.LENGTH_SHORT).show();
//						AlertDialog alertDialog = new AlertDialog.Builder(mContext.get()).setTitle("ssss").setMessage("1111").create();
//
//						alertDialog.show();
//						LayoutInflater inflater = (LayoutInflater) mContext.get().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//						View viewBt = inflater.from(mContext.get()).inflate(R.layout.pop_palette_choice,null );
//						PopupWindow popBt = new PopupWindow(viewBt,LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//						popBt.setFocusable(true);
//						popBt.setOutsideTouchable(true);
//						popBt.setTouchable(true);
//
//						popBt.showAsDropDown(popSettingBinding.btLanguage,15,-popBt.getHeight()-20, Gravity.CENTER);
//					}
//				});
//				popSettingBinding.spinnerLanguage.attachDataSource(DYConstants.languageArray);
//				popSettingBinding.spinnerLanguage.setOnSpinnerItemSelectedListener(new OnSpinnerItemSelectedListener() {
//					@Override
//					public void onItemSelected (MySpinner parent, View view, int position, long id) {
////						Toast.makeText(mContext.get(), " "+position,Toast.LENGTH_SHORT).show();
//					}
//				});

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

				ArrayAdapter<String> adapterSpinner = new ArrayAdapter<String>(mContext.get(),R.layout.item_select, DYConstants.languageArray);
				adapterSpinner.setDropDownViewResource(R.layout.item_dropdown);
				popSettingBinding.spinnerSettingLanguage.setAdapter(adapterSpinner);
				popSettingBinding.spinnerSettingLanguage.setSelection(0);//从本地拿
				popSettingBinding.spinnerSettingLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
//						Toast.makeText(mContext.get(),"spinner item  = "+position,Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onNothingSelected (AdapterView<?> parent) {
//						if (isDebug)Toast.makeText(mContext.get(),"spinner item  = onNothingSelected",Toast.LENGTH_SHORT).show();
					}
				});

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

	public void toClear(View view){
		Toast.makeText(mContext.get(),"toClear ", Toast.LENGTH_SHORT).show();
//		NavHostFragment.findNavController(PreviewFragment.this).popBackStack();
//		Navigation.findNavController(mDataBinding.getRoot()).popBackStack();

	}


}
