package com.dyt.wcc.ui.preview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import com.dyt.wcc.BuildConfig;
import com.dyt.wcc.R;
import com.dyt.wcc.cameracommon.encoder.MediaMuxerWrapper;
import com.dyt.wcc.cameracommon.usbcameracommon.AbstractUVCCameraHandler;
import com.dyt.wcc.cameracommon.usbcameracommon.UVCCameraHandler;
import com.dyt.wcc.cameracommon.utils.ByteUtil;
import com.dyt.wcc.common.utils.DensityUtil;
import com.dyt.wcc.common.utils.FontUtils;
import com.dyt.wcc.common.widget.MyCustomRangeSeekBar;
import com.dyt.wcc.common.widget.dragView.DrawLineRectHint;
import com.dyt.wcc.common.widget.dragView.MeasureEntity;
import com.dyt.wcc.common.widget.dragView.MeasureTempContainerView;
import com.dyt.wcc.constans.DYConstants;
import com.dyt.wcc.constans.DYTApplication;
import com.dyt.wcc.constans.DYTRobotSingle;
import com.dyt.wcc.customize.CustomizeCompany;
import com.dyt.wcc.customize.LanguageFactory;
import com.dyt.wcc.customize.dyt.DytCompanyView;
import com.dyt.wcc.customize.dyt.DytLanguageFactory;
import com.dyt.wcc.customize.jms.JMSCompanyView;
import com.dyt.wcc.customize.jms.JMSLanguageFactory;
import com.dyt.wcc.customize.mailseey.MileSeeYCompanyView;
import com.dyt.wcc.customize.mailseey.MileSeeYLanguageFactory;
import com.dyt.wcc.customize.neutral.NeutralCompanyView;
import com.dyt.wcc.customize.neutral.NeutralLanguageFactory;
import com.dyt.wcc.customize.qianli.QianLiLanguageFactory;
import com.dyt.wcc.customize.qianli.QianliCompanyView;
import com.dyt.wcc.customize.qianli.ReadPdfActivity;
import com.dyt.wcc.customize.teslong.TeslongCompanyView;
import com.dyt.wcc.customize.teslong.TeslongLanguageFactory;
import com.dyt.wcc.customize.victor.PdfActivity;
import com.dyt.wcc.customize.victor.VictorCompanyView;
import com.dyt.wcc.customize.victor.VictorLanguageFactory;
import com.dyt.wcc.databinding.ActivityPreviewBinding;
import com.dyt.wcc.databinding.PopHighlowcenterTraceBinding;
import com.dyt.wcc.databinding.PopPaletteChoiceBinding;
import com.dyt.wcc.databinding.PopSettingBinding;
import com.dyt.wcc.databinding.PopTempModeChoiceBinding;
import com.dyt.wcc.ui.base.BaseActivity;
import com.dyt.wcc.ui.gallery.GlideEngine;
import com.dyt.wcc.utils.AssetCopyer;
import com.dyt.wcc.utils.ByteUtilsCC;
import com.dyt.wcc.utils.CreateBitmap;
import com.dyt.wcc.utils.LanguageUtils;
import com.dyt.wcc.widget.MyNumberPicker;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.ui.dialog.LoadingDialog;
import com.king.app.dialog.AppDialog;
import com.king.app.updater.AppUpdater;
import com.king.app.updater.http.OkHttpManager;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//@RuntimePermissions
public class PreviewActivity extends BaseActivity<ActivityPreviewBinding> {
	//jni测试按钮状态记录
	//	private final int jni_status = 0;

	//	private static final int REQUEST_CODE_CHOOSE  = 23;
	private static final int  MSG_CHECK_UPDATE  = 1;
	private static final int  MSG_CAMERA_PARAMS = 2;
	public static        long startTime         = 0;
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
	private UVCCameraHandler mUvcCameraHandler;
	//点线矩形测温弹窗
	private PopupWindow      PLRPopupWindows;
	//用户添加 测温模式 切换
	View.OnClickListener tempModeCheckListener = new View.OnClickListener() {
		@Override
		public void onClick (View v) {
			switch (v.getId()) {
				case R.id.iv_temp_mode_point:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewActivity.setDrawTempMode(1);
					//					mDataBinding.dragTempContainerPreviewActivity.getDrawTempMode();
					//					if (isDebug)showToast("point ");
					break;
				case R.id.iv_temp_mode_line:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewActivity.setDrawTempMode(2);
					//					if (isDebug)showToast("line ");
					break;
				case R.id.iv_temp_mode_rectangle:
					PLRPopupWindows.dismiss();
					mDataBinding.dragTempContainerPreviewActivity.setDrawTempMode(3);
					//					if (isDebug)showToast("rectangle ");
					break;
			}
		}
	};
	private PopupWindow        settingPopWindows;
	private PopupWindow        companyPopWindows;
	//	private View popView;
	private Map<String, Float> cameraParams;
	private SharedPreferences  sp;
	//色板切换
	View.OnClickListener paletteChoiceListener = v -> {
		switch (v.getId()) {
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
	//设备 vid pid
	private int mVid, mPid;
	private USBMonitor mUsbMonitor;
	private int        mFontSize;
	private String     palettePath;
	private Bitmap     tiehong = null, caihong = null, baire = null, heire = null, hongre = null, lenglan = null;
	private SendCommand mSendCommand;
	private int         screenWidth, screenHeight;
	private Bitmap highTempBt, lowTempBt, centerTempBt, normalPointBt;
	//	private DisplayMetrics metrics;
	//	private Configuration  configuration;
	private       boolean                                 isFirstRun              = false;
	//	//校正输入框 过滤器
	//	private TextWatcher reviseTextWatcher;
	//	//发射率输入框 拦截器
	//	private TextWatcher emittanceTextWatcher;
	//	//反射温度输入框 拦截器
	//	private TextWatcher reflectTextWatcher;
	//	//环境温度 输入框 拦截器
	//	private TextWatcher environmentTextWatcher;
	// 重置参数 返回值
	private       int                                     defaultSettingReturn    = -1;
	//更新工具类对象
	private       AppUpdater                              mAppUpdater;
	private       int                                     maxIndex                = 0;
	private       List<UpdateObj>                         updateObjList;
	//传感器
	private       AbstractUVCCameraHandler.CameraCallback cameraCallback;
	private       LoadingDialog                           loadingDialog;
	private final Handler                                 mHandler                = new Handler(new Handler.Callback() {
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
					btnCancel.setOnClickListener(v -> AppDialog.INSTANCE.dismissDialog());
					View btnConfirm = view.findViewById(R.id.btnConfirm);
					btnConfirm.setOnClickListener(v -> {
						showToast(getString(R.string.toast_downloading_background));
						mAppUpdater = new AppUpdater.Builder().setUrl(DYConstants.UPDATE_DOWNLOAD_API + updateObjList.get(maxIndex).getPackageName())
								//                        .setApkMD5("3df5b1c1d2bbd01b4a7ddb3f2722ccca")//支持MD5校验，如果缓存APK的MD5与此MD5相同，则直接取本地缓存安装，推荐使用MD5校验的方式
								.setVersionCode(updateObjList.get(maxIndex).getAppVersionCode())//支持versionCode校验，设置versionCode之后，新版本versionCode相同的apk只下载一次,优先取本地缓存,推荐使用MD5校验的方式
								.setFilename(updateObjList.get(maxIndex).getPackageName() + ".apk").setVibrate(true).build(mContext.get());
						mAppUpdater.setHttpManager(OkHttpManager.getInstance()).start();
						AppDialog.INSTANCE.dismissDialog();
					});
					AppDialog.INSTANCE.showDialog(mContext.get(), view, 0.5f);
					break;
				case MSG_CAMERA_PARAMS:
					View pop_View = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_setting, null);

					PopSettingBinding popSettingBinding = DataBindingUtil.bind(pop_View);
					assert popSettingBinding != null;
					popSettingBinding.tvCheckVersionInfo.setText(String.format(getString(R.string.setting_check_version_info), BuildConfig.VERSION_NAME));
					//设置单位
					popSettingBinding.tvCameraSettingReviseUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
					popSettingBinding.tvCameraSettingReflectUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
					popSettingBinding.tvCameraSettingFreeAirTempUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));

					//初始化 每个输入框的过滤器 ,切换 温度单位，需要重新设置一遍过滤器（先移除，再添加）

					if (mPid == 1 && mVid == 5396) {//S0有湿度参数
						popSettingBinding.tvCameraSettingHumidity.setVisibility(View.VISIBLE);
					} else if (mPid == 22592 && mVid == 3034) {//TinyC 无湿度参数
						popSettingBinding.tvCameraSettingHumidity.setVisibility(View.GONE);
					}
					popSettingBinding.btCheckVersion.setVisibility(View.GONE);
					if (DYConstants.COMPANY_DYT.equals(BuildConfig.FLAVOR)) {
						popSettingBinding.btCheckVersion.setVisibility(View.VISIBLE);
						popSettingBinding.btCheckVersion.setOnClickListener(v1 -> {
							OkHttpClient client = new OkHttpClient();
							new Thread(() -> {
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
									int currentVersionCode;
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
							}).start();
						});
					}


					//第二步：将获取的数据 展示在输入框内
					if (cameraParams != null) {
						popSettingBinding.etCameraSettingEmittance.setText(String.valueOf(cameraParams.get(DYConstants.setting_emittance)));//发射率 0-1

						popSettingBinding.etCameraSettingRevise.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_correction, 0.0f))));//校正  -20 - 20
						popSettingBinding.etCameraSettingReflect.setText(String.valueOf((int) refreshValueByTempUnit(sp.getFloat(DYConstants.setting_reflect, 0.0f))));//反射温度 -10-40
						popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf((int) refreshValueByTempUnit(sp.getFloat(DYConstants.setting_environment, 0.0f))));//环境温度 -10 -40

						if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
							popSettingBinding.etCameraSettingHumidity.setText(String.valueOf((int) (cameraParams.get(DYConstants.setting_humidity) * 100)));//湿度 0-100
							//湿度设置
							popSettingBinding.etCameraSettingHumidity.setOnEditorActionListener((v12, actionId, event) -> {
								if (actionId == EditorInfo.IME_ACTION_DONE) {
									if (TextUtils.isEmpty(v12.getText().toString()))
										return true;
									int value = Integer.parseInt(v12.getText().toString());
									if (value > 100 || value < 0) {
										showToast(getString(R.string.toast_range_int, 0, 100));
										return true;
									}
									float fvalue = value / 100.0f;
									if (mUvcCameraHandler != null) {
										if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
											sendS0Order(fvalue, DYConstants.SETTING_HUMIDITY_INT);
										}
										popSettingBinding.etCameraSettingHumidity.setText(String.format(Locale.US, "%d", value));
										sp.edit().putFloat(DYConstants.setting_humidity, fvalue).apply();
										showToast(R.string.toast_complete_Humidity);
									}
									hideInput(v12.getWindowToken());
								}
								return true;
							});
						} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {

						}

						// 设置机芯的默认值;
						popSettingBinding.btSettingDefault.setOnClickListener(v13 -> {
							toSettingDefault();
							if (mPid == 1 && mVid == 5396) {
								popSettingBinding.etCameraSettingHumidity.setText(String.format(Locale.CHINESE, "%s", (int) (100 * DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE)));
								sp.edit().putFloat(DYConstants.setting_humidity, DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE).apply();
							}
							if (true) {
								popSettingBinding.etCameraSettingReflect.setText(String.format(Locale.CHINESE, "%d", (int) refreshValueByTempUnit(DYConstants.SETTING_REFLECT_DEFAULT_VALUE)));
								popSettingBinding.etCameraSettingRevise.setText(String.format(Locale.CHINESE, "%s", refreshValueByTempUnit(DYConstants.SETTING_CORRECTION_DEFAULT_VALUE)));
								popSettingBinding.etCameraSettingEmittance.setText(String.format(Locale.CHINESE, "%s", DYConstants.SETTING_EMITTANCE_DEFAULT_VALUE));
								popSettingBinding.etCameraSettingFreeAirTemp.setText(String.format(Locale.CHINESE, "%d", (int) refreshValueByTempUnit(DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE)));

								sp.edit().putFloat(DYConstants.setting_environment, DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE).apply();
								sp.edit().putFloat(DYConstants.setting_reflect, DYConstants.SETTING_REFLECT_DEFAULT_VALUE).apply();
								sp.edit().putFloat(DYConstants.setting_emittance, DYConstants.SETTING_EMITTANCE_DEFAULT_VALUE).apply();
								sp.edit().putFloat(DYConstants.setting_correction, DYConstants.SETTING_CORRECTION_DEFAULT_VALUE).apply();

								mDataBinding.textureViewPreviewActivity.setTinyCCorrection(DYConstants.SETTING_CORRECTION_DEFAULT_VALUE);
							}
						});

						//发射率
						popSettingBinding.etCameraSettingEmittance.addTextChangedListener(new DecimalInputTextWatcher(popSettingBinding.etCameraSettingEmittance, 5, 2));
						popSettingBinding.etCameraSettingEmittance.setOnEditorActionListener((v14, actionId, event) -> {
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v14.getText().toString()))
									return true;
								float value = Float.parseFloat(v14.getText().toString());
								if (value > 1.00 || value < 0.01) {
									showToast(getString(R.string.toast_range_float, 0.01f, 1.00f));
									return true;
								}
								v14.clearFocus();
								if (mUvcCameraHandler != null) {
									if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
										sendS0Order(value, DYConstants.SETTING_EMITTANCE_INT);
									} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, value, 3);
									}
									sp.edit().putFloat(DYConstants.setting_emittance, value).apply();
									showToast(R.string.toast_complete_Emittance);
								}
								hideInput(v14.getWindowToken());
							}
							return true;
						});
						//校正设置 -20 - 20
						popSettingBinding.etCameraSettingRevise.addTextChangedListener(new DecimalInputTextWatcher(popSettingBinding.etCameraSettingRevise, 6, 2));
						popSettingBinding.etCameraSettingRevise.setOnEditorActionListener((v16, actionId, event) -> {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v16.getText().toString()) || "-".equals(v16.getText().toString()) || "-.".equals(v16.getText().toString()))
									return true;
								float value = inputValue2Temp(Float.parseFloat(v16.getText().toString()));
								if (value > DYConstants.REVISE_MAX || value < DYConstants.REVISE_MIN) {
									showToast(getString(R.string.toast_range_float, getBorderValue(DYConstants.REVISE_MIN), getBorderValue(DYConstants.REVISE_MAX)));
									return true;
								}
								if (mUvcCameraHandler != null) {
									if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
										sendS0Order(value, DYConstants.SETTING_CORRECTION_INT);
									} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {//校正 TinyC
										mDataBinding.textureViewPreviewActivity.setTinyCCorrection(value);
									}
									sp.edit().putFloat(DYConstants.setting_correction, value).apply();
									showToast(R.string.toast_complete_Revise);
								}
								hideInput(v16.getWindowToken());
							}
							return true;
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
						popSettingBinding.etCameraSettingReflect.addTextChangedListener(new DecimalInputTextWatcher(popSettingBinding.etCameraSettingReflect, 7, 2));
						popSettingBinding.etCameraSettingReflect.setOnEditorActionListener((v15, actionId, event) -> {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v15.getText().toString()) || "-".equals(v15.getText().toString()) || "-.".equals(v15.getText().toString()))
									return true;
								float value = inputValue2Temp(Integer.parseInt(v15.getText().toString()));//拿到的都是摄氏度
								if (value > DYConstants.REFLECT_MAX || value < DYConstants.REFLECT_MIN) {//带上 温度单位
									showToast(getString(R.string.toast_range_int, Math.round(getBorderValue(DYConstants.REFLECT_MIN)), Math.round(getBorderValue(DYConstants.REFLECT_MAX))));
									return true;
								}
								if (mUvcCameraHandler != null) {
									if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
										sendS0Order(value, DYConstants.SETTING_REFLECT_INT);
									} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
										//										Log.e(TAG, "onEditorAction: 反射温度 set value = " + value);
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, (int) value, 1);
									}
									sp.edit().putFloat(DYConstants.setting_reflect, value).apply();
									showToast(R.string.toast_complete_Reflect);
								}
								hideInput(v15.getWindowToken());
							}
							return true;
						});


						//环境温度设置  -20 -50
						popSettingBinding.etCameraSettingFreeAirTemp.addTextChangedListener(new DecimalInputTextWatcher(popSettingBinding.etCameraSettingFreeAirTemp, 7, 2));
						popSettingBinding.etCameraSettingFreeAirTemp.setOnEditorActionListener((v17, actionId, event) -> {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v17.getText().toString()) || "-".equals(v17.getText().toString()) || "-.".equals(v17.getText().toString()))
									return true;
								float value = inputValue2Temp(Integer.parseInt(v17.getText().toString()));
								if (value > DYConstants.ENVIRONMENT_MAX || value < DYConstants.ENVIRONMENT_MIN) {
									showToast(getString(R.string.toast_range_int, Math.round(getBorderValue(DYConstants.ENVIRONMENT_MIN)), Math.round(getBorderValue(DYConstants.ENVIRONMENT_MAX))));
									return true;
								}
								if (mUvcCameraHandler != null) {
									if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
										sendS0Order(value, DYConstants.SETTING_ENVIRONMENT_INT);
									} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
										//										Log.e(TAG, "onEditorAction: 环境温度 set value = " + value);
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, (int) value, 2);
									}
									sp.edit().putFloat(DYConstants.setting_environment, value).apply();
									showToast(R.string.toast_complete_FreeAirTemp);
								}
								hideInput(v17.getWindowToken());
							}
							return true;
						});
					}
					int temp_unit = sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0);
					//第三步：初始化自定义控件 温度单位 设置
					popSettingBinding.switchChoiceTempUnit.setText(DYConstants.tempUnit).setSelectedTab(temp_unit, false).setOnSwitchListener((position, tabText) -> {//切换 温度单位 监听器
						mDataBinding.dragTempContainerPreviewActivity.setTempSuffix(position);
						sp.edit().putInt(DYConstants.TEMP_UNIT_SETTING, position).apply();
						//切换 温度单位 需要更改 输入框的 单位
						popSettingBinding.tvCameraSettingReviseUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
						popSettingBinding.tvCameraSettingReflectUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
						popSettingBinding.tvCameraSettingFreeAirTempUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
						//切换 温度单位 需要更改 里面已经输入的值。
						popSettingBinding.etCameraSettingRevise.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_correction, 0.0f))));
						popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf((int) refreshValueByTempUnit(sp.getFloat(DYConstants.setting_environment, 0.0f))));
						popSettingBinding.etCameraSettingReflect.setText(String.valueOf((int) refreshValueByTempUnit(sp.getFloat(DYConstants.setting_reflect, 0.0f))));
					});
					//显示设置的弹窗
					settingPopWindows = new PopupWindow(pop_View, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					settingPopWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
					settingPopWindows.setHeight(mDataBinding.clPreviewActivity.getHeight() / 2 + DensityUtil.dp2px(mContext.get(), 10));
					settingPopWindows.setWidth(mDataBinding.clPreviewActivity.getWidth() - DensityUtil.dp2px(mContext.get(), 20));

					settingPopWindows.setFocusable(true);
					settingPopWindows.setOutsideTouchable(true);
					settingPopWindows.setTouchable(true);

					//第四步：显示控件
					//					if (oldRotation == 90 || oldRotation == 180) {
					//						int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
					//						settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() + settingPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 5), Gravity.CENTER);
					//						settingPopWindows.getContentView().setRotation(180);
					//					}
					//					if (oldRotation == 0 || oldRotation == 270) {
					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() / 2 - DensityUtil.dp2px(mContext.get(), 15), Gravity.CENTER);
					settingPopWindows.getContentView().setRotation(0);
					//					}

					//弹窗消失，机芯执行保存指令。
					settingPopWindows.setOnDismissListener(() -> {
						if (mUvcCameraHandler != null) {
							new Thread(() -> {
								//TinyC 断电保存
								if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
									mUvcCameraHandler.tinySaveCameraParams();
								}
								//S0 断电保存
								if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
									setValue(UVCCamera.CTRL_ZOOM_ABS, 0x80ff);
								}
							}).start();
						}
					});
					//录制声音
					popSettingBinding.switchChoiceRecordAudio.setSelectedTab(sp.getInt(DYConstants.RECORD_AUDIO_SETTING, 1), false);
					popSettingBinding.switchChoiceRecordAudio.setOnSwitchListener((position, tabText) -> {
						sp.edit().putInt(DYConstants.RECORD_AUDIO_SETTING, position).apply();
						//						if (isDebug)Log.e(TAG, "onSwitch: " + "=============================");
					});
					//高低温增益切换
					//tinyc set gain mode:Data_L = 0x0, low gain， Data_L = 0x1,  high gain
					//拔出之后默认为低温模式。
					popSettingBinding.switchHighlowGain.setSelectedTab(sp.getInt(DYConstants.GAIN_TOGGLE_SETTING, 0), false);
					popSettingBinding.switchHighlowGain.setOnSwitchListener((position, tabText) -> {
						//						if (doubleGainClick()) {
						//							return;
						//						}
						sp.edit().putInt(DYConstants.GAIN_TOGGLE_SETTING, position).apply();
						Log.e(TAG, "handleMessage: 111111111============" + position);
						if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
							if (position == 0) {
								mHandler.postDelayed(() -> setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8020), 200); //最高200度
							} else {
								mHandler.postDelayed(() -> setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8021), 200);//最高599度
							}
							mHandler.postDelayed(() -> setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000), 1000);
							mHandler.postDelayed(() -> mUvcCameraHandler.whenShutRefresh(), 1800);
						} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
							loadingDialog.show();

							if (position == 0) {
								new Thread(() -> {
									final boolean isSuccess = mUvcCameraHandler.setMachineSetting(UVCCamera.CTRL_ZOOM_ABS, 1, 1);
									runOnUiThread(new Runnable() {
										@Override
										public void run () {
											if (!isSuccess) {
												//													showToast(getString(R.string.toast_setting_HL_swtich_success));
												//												}else {
												//													showToast(getString(R.string.toast_setting_HL_swtich_fail));
												popSettingBinding.switchHighlowGain.setSelectedTab(1, false);
											}
											loadingDialog.dismiss();
										}
									});
								}).start();

							} else {
								new Thread(new Runnable() {
									@Override
									public void run () {
										final boolean isSuccess = mUvcCameraHandler.setMachineSetting(UVCCamera.CTRL_ZOOM_ABS, 0, 0);
										runOnUiThread(new Runnable() {
											@Override
											public void run () {
												if (!isSuccess) {
													//													showToast(getString(R.string.toast_setting_HL_swtich_success));
													//												}else {
													//													showToast(getString(R.string.toast_setting_HL_swtich_fail));
													popSettingBinding.switchHighlowGain.setSelectedTab(0, false);
												}
												loadingDialog.dismiss();
											}
										});
									}
								}).start();
							}
						}
					});
					//					sp.getString(DYConstants.LANGUAGE_SETTING, "ch");
					// 切换语言 spinner
					final LanguageFactory factory;
					switch (BuildConfig.FLAVOR) {
						case DYConstants.COMPANY_JMS:
							factory = new JMSLanguageFactory(mContext.get());
							break;
						case DYConstants.COMPANY_VICTOR:
							factory = new VictorLanguageFactory(mContext.get());
							break;
						case DYConstants.COMPANY_QIANLI:
							factory = new QianLiLanguageFactory(mContext.get());
							break;
						case DYConstants.COMPANY_TESLONG:
							factory = new TeslongLanguageFactory(mContext.get());
							break;
						case DYConstants.COMPANY_NEUTRAL:
							factory = new NeutralLanguageFactory(mContext.get());
							break;
						case DYConstants.COMPANY_MAILSEEY:
							factory = new MileSeeYLanguageFactory(mContext.get());
							break;
						default:
							factory = new DytLanguageFactory(mContext.get());
							break;
					}
					popSettingBinding.btShowChoiceLanguage.setText(factory.getLanguageByIndex(sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0)));
					//					popSettingBinding.btShowChoiceLanguage.setText(DYConstants.languageArray[sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0)]);
					popSettingBinding.btShowChoiceLanguage.setOnClickListener(v18 -> {
						AlertDialog alertDialog;
						factory.setListener((dialog, index) -> {
							if (index != sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0)) {
								sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, index).apply();
								sp.edit().putString(DYConstants.LANGUAGE_SETTING, factory.getLanguageArray()[index]).apply();
								if (settingPopWindows != null)
									settingPopWindows.dismiss();
								toSetLanguage(index);
							}
							dialog.dismiss();
						});
						factory.createDialogListener();
						alertDialog = factory.createAlertDialog();
						//						AlertDialog.Builder builder = new AlertDialog.Builder(mContext.get());
						//						builder.setSingleChoiceItems(DYConstants.languageArray, sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0), (dialog, which) -> {
						//							if (which != sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0)) {
						//								sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, which).apply();
						//								sp.edit().putString(DYConstants.LANGUAGE_SETTING, DYConstants.languageArray[which]).apply();
						//								if (settingPopWindows != null)
						//									settingPopWindows.dismiss();
						//								toSetLanguage(which);
						//							}
						//							dialog.dismiss();
						//						}).create();
						alertDialog.show();
					});
					break;
			}
			return false;
		}
	});
	private       Surface                                 stt;
	private final USBMonitor.OnDeviceConnectListener      onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
		@Override
		public void onAttach (UsbDevice device) {
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
			//			if (isDebug)Log.e(TAG, "onConnect:  SN ========================= 》 " + device.getSerialNumber());
			mVid = device.getVendorId();
			mPid = device.getProductId();

			if (mUvcCameraHandler == null || mUvcCameraHandler.isReleased()) {
				mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(), mDataBinding.textureViewPreviewActivity, 1, 384, 292, 1, null, mDataBinding.dragTempContainerPreviewActivity, 0);
			}

			mUvcCameraHandler.open(ctrlBlock);
			startPreview();

			mHandler.postDelayed(() -> {
				setValue(UVCCamera.CTRL_ZOOM_ABS, DYConstants.CAMERA_DATA_MODE_8004);//切换数据输出8004原始8005yuv,80ff保存
			}, 300);

			//6.5s之后给 机芯强制设置为 低温模式（测试 发现不是所有设备都生效）
			//			if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
			//				mHandler.postDelayed(new Runnable() {
			//					@Override
			//					public void run () {
			//						mUvcCameraHandler.setMachineSetting(UVCCamera.CTRL_ZOOM_ABS, 1, 1);
			//					}
			//				}, 5000);
			//				mHandler.postDelayed(new Runnable() {
			//					@Override
			//					public void run () {
			//						setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
			//					}
			//				}, 6000);
			//				mHandler.postDelayed(new Runnable() {
			//					@Override
			//					public void run () {
			//						if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
			//							mUvcCameraHandler.tinySaveCameraParams();
			//					}
			//				}, 6500);
			//			}
		}

		@Override
		public void onDettach (UsbDevice device) {
			//				mUvcCameraHandler.close();
			if (isDebug)
				Log.e(TAG, "DD  onDetach: ");
			runOnUiThread(() -> {
				mDataBinding.dragTempContainerPreviewActivity.setBackgroundColor(getColor(R.color.bg_preview_otg_unconnect));
				mDataBinding.dragTempContainerPreviewActivity.setConnect(false);
				mDataBinding.dragTempContainerPreviewActivity.invalidate();
				onPause();
				onStop();
			});
		}

		@Override
		public void onDisconnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
			if (isDebug)
				Log.e(TAG, " DD == onDisconnect: ");
			runOnUiThread(() -> {
				//拔出设备之后，重置为低温增益模式
				sp.edit().putInt(DYConstants.GAIN_TOGGLE_SETTING, 0).apply();
				//断开连接之时, 恢复UI
				mDataBinding.toggleAreaCheck.setSelected(false);
				mDataBinding.toggleHighTempAlarm.setSelected(false);

				mDataBinding.customSeekbarPreviewFragment.setWidgetMode(0);
				mDataBinding.customSeekbarPreviewFragment.setPalette(sp.getInt(DYConstants.PALETTE_NUMBER, 1) - 1);
				mDataBinding.customSeekbarPreviewFragment.invalidate();
				mDataBinding.toggleFixedTempBar.setSelected(false);
				if (stt != null) {
					stt.release();
					stt = null;
				}

			});

			if (mUvcCameraHandler != null) {
				mUvcCameraHandler.disWenKuan();
				mUvcCameraHandler.fixedTempStripChange(false);
			}
			DYTApplication.setRobotSingle(DYTRobotSingle.NO_DEVICE);

			if (mUvcCameraHandler != null) {
				//				mUvcCameraHandler.removeCallback(cameraCallback);
				//				mUvcCameraHandler.stopTemperaturing();//2022年5月18日19:00:15 S0温度失常
				mUvcCameraHandler.close();
			} else {
				Log.e(TAG, "onDisconnect: ==============mUvcCameraHandler == null========");
			}
			mVid = 0;
			mPid = 0;
		}

		@Override
		public void onCancel (UsbDevice device) {
			if (isDebug)
				Log.e(TAG, "DD  onCancel: ");
		}
	};
	private       MyNumberPicker                          myNumberPicker;

	//	private final SensorEventListener sensorEventListener = new SensorEventListener() {
	//		@Override
	//		public void onSensorChanged (SensorEvent event) {
	//			if (event.sensor.getStringType().equals(Sensor.STRING_TYPE_ACCELEROMETER)) {//								Log.e(TAG, "onSensorChanged: Sensor.STRING_TYPE_ACCELEROMETER");
	//				float x = event.values[0];
	//				float y = event.values[1];
	//				float z = event.values[2];
	////				relayout(x, y, z);
	//			}
	//		}
	//
	//		@Override
	//		public void onAccuracyChanged (Sensor sensor, int accuracy) {
	//		}
	//	};
	//	private       int                 oldRotation         = 0;

	//	private void relayout (float x, float y, float z) {
	//		if (x > -2.5 && x <= 2.5 && y > 7.5 && y <= 10 && oldRotation != 270) {
	//			oldRotation = 270;
	//			mDataBinding.clMainPreview.setRotation(0);
	//			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
	//				mUvcCameraHandler.setRotateMatrix_180(false);
	//			mDataBinding.textureViewPreviewActivity.S0_RotateMatrix_180(false);
	//
	//			mDataBinding.dragTempContainerPreviewActivity.setAllChildViewRotate(false);
	//
	//			if (PLRPopupWindows != null && PLRPopupWindows.isShowing()) {
	//				PLRPopupWindows.dismiss();
	//				if (oldRotation == 0 || oldRotation == 270) {
	//					int offsetX = 0;
	//					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
	//					PLRPopupWindows.getContentView().setRotation(0);
	//				}
	//			}
	//
	//			if (settingPopWindows != null && settingPopWindows.isShowing()) {
	//				settingPopWindows.dismiss();
	//				if (oldRotation == 0 || oldRotation == 270) {
	//					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
	//					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() / 2 - DensityUtil.dp2px(mContext.get(), 15), Gravity.CENTER);
	//					settingPopWindows.getContentView().setRotation(0);
	//				}
	//			}
	//
	//			if (companyPopWindows != null && companyPopWindows.isShowing()) {
	//				companyPopWindows.dismiss();
	//				if (oldRotation == 0 || oldRotation == 270) {
	//					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
	//					companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -companyPopWindows.getHeight() - DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
	//					companyPopWindows.getContentView().setRotation(0);
	//				}
	//			}
	//			if (myNumberPicker != null && myNumberPicker.isVisible()) {
	//				myNumberPicker.dismiss();
	//				myNumberPicker.setRotation(false);
	//				myNumberPicker.show(getSupportFragmentManager(), null);
	//			}
	//			setMRotation(0);
	//		} else if (x > 7.5 && x <= 10 && y > -2.5 && y <= 2.5 && oldRotation != 0) {
	//			oldRotation = 0;
	//			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
	//				mUvcCameraHandler.setRotateMatrix_180(false);
	//			mDataBinding.textureViewPreviewActivity.S0_RotateMatrix_180(false);
	//
	//			mDataBinding.clMainPreview.setRotation(0);
	//			mDataBinding.dragTempContainerPreviewActivity.setAllChildViewRotate(false);
	//			if (PLRPopupWindows != null && PLRPopupWindows.isShowing()) {
	//				PLRPopupWindows.dismiss();
	//				if (oldRotation == 0 || oldRotation == 270) {
	//					int offsetX = 0;
	//					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
	//					PLRPopupWindows.getContentView().setRotation(0);
	//				}
	//			}
	//			if (settingPopWindows != null && settingPopWindows.isShowing()) {
	//				settingPopWindows.dismiss();
	//				if (oldRotation == 0 || oldRotation == 270) {
	//					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
	//					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() / 2 - DensityUtil.dp2px(mContext.get(), 15), Gravity.CENTER);
	//					settingPopWindows.getContentView().setRotation(0);
	//				}
	//			}
	//
	//			if (companyPopWindows != null && companyPopWindows.isShowing()) {
	//				companyPopWindows.dismiss();
	//				if (oldRotation == 0 || oldRotation == 270) {
	//					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
	//					companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -companyPopWindows.getHeight() - DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
	//					companyPopWindows.getContentView().setRotation(0);
	//				}
	//			}
	//
	//			if (myNumberPicker != null && myNumberPicker.isVisible()) {
	//				myNumberPicker.dismiss();
	//				myNumberPicker.setRotation(false);
	//				myNumberPicker.show(getSupportFragmentManager(), null);
	//			}
	//			setMRotation(0);
	//		} else if (x > -2.5 && x <= 2.5 && y > -10 && y <= -7.5 && oldRotation != 90) {
	//			oldRotation = 90;
	//			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
	//				mUvcCameraHandler.setRotateMatrix_180(true);
	//			mDataBinding.textureViewPreviewActivity.S0_RotateMatrix_180(true);
	//			//			mDataBinding.dragTempContainerPreviewActivity.setRotation(180);
	//			//			mDataBinding.textureViewPreviewActivity.setRotation(180);
	//			mDataBinding.clMainPreview.setRotation(180);
	//			//			mDataBinding.dragTempContainerPreviewActivity.setRotation(180);
	//			mDataBinding.dragTempContainerPreviewActivity.setAllChildViewRotate(true);
	//			if (PLRPopupWindows != null && PLRPopupWindows.isShowing()) {
	//				PLRPopupWindows.dismiss();
	//				if (oldRotation == 90 || oldRotation == 180) {
	//					int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
	//					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
	//					PLRPopupWindows.getContentView().setRotation(180);
	//				}
	//			}
	//			if (settingPopWindows != null && settingPopWindows.isShowing()) {
	//				settingPopWindows.dismiss();
	//				//第四步：显示控件
	//				if (oldRotation == 90 || oldRotation == 180) {
	//					int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
	//					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() + settingPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 5), Gravity.CENTER);
	//					settingPopWindows.getContentView().setRotation(180);
	//				}
	//			}
	//
	//			if (companyPopWindows != null && companyPopWindows.isShowing()) {
	//				companyPopWindows.dismiss();
	//				if (oldRotation == 90 || oldRotation == 180) {
	//					int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
	//					companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.clPreviewActivity.getHeight() + companyPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
	//					companyPopWindows.getContentView().setRotation(180);
	//				}
	//			}
	//
	//			if (myNumberPicker != null && myNumberPicker.isVisible()) {
	//				myNumberPicker.dismiss();
	//				myNumberPicker.setRotation(true);
	//				myNumberPicker.show(getSupportFragmentManager(), null);
	//			}
	//			setMRotation(180);
	//		} else if (x > -10 && x <= -7.5 && y > -2.5 && y < 2.5 && oldRotation != 180) {
	//			oldRotation = 180;
	//			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
	//				mUvcCameraHandler.setRotateMatrix_180(true);
	//			mDataBinding.textureViewPreviewActivity.S0_RotateMatrix_180(true);
	//			//			mDataBinding.dragTempContainerPreviewActivity.setRotation(180);
	//			//			mDataBinding.textureViewPreviewActivity.setRotation(180);
	//			mDataBinding.clMainPreview.setRotation(180);
	//			mDataBinding.dragTempContainerPreviewActivity.setAllChildViewRotate(true);
	//			if (PLRPopupWindows != null && PLRPopupWindows.isShowing()) {
	//				PLRPopupWindows.dismiss();
	//				if (oldRotation == 90 || oldRotation == 180) {
	//					int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
	//					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
	//					PLRPopupWindows.getContentView().setRotation(180);
	//				}
	//			}
	//			if (settingPopWindows != null && settingPopWindows.isShowing()) {
	//				settingPopWindows.dismiss();
	//				//第四步：显示控件
	//				if (oldRotation == 90 || oldRotation == 180) {
	//					int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
	//					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() + settingPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 5), Gravity.CENTER);
	//					settingPopWindows.getContentView().setRotation(180);
	//				}
	//			}
	//
	//			if (companyPopWindows != null && companyPopWindows.isShowing()) {
	//				companyPopWindows.dismiss();
	//				if (oldRotation == 90 || oldRotation == 180) {
	//					int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
	//					companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.clPreviewActivity.getHeight() + companyPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
	//					companyPopWindows.getContentView().setRotation(180);
	//				}
	//			}
	//
	//			if (myNumberPicker != null && myNumberPicker.isVisible()) {
	//				myNumberPicker.dismiss();
	//				myNumberPicker.setRotation(true);
	//				myNumberPicker.show(getSupportFragmentManager(), null);
	//			}
	//			setMRotation(180);
	//		}
	//	}

	/**
	 * <p>判断是否连续点击</p>
	 *
	 * @return boolean
	 */
	public static boolean doubleClick () {
		long now = System.currentTimeMillis();
		if (now - startTime < 1500) {
			return true;
		}
		startTime = now;
		return false;
	}

	/**
	 * <p>判断是否连续点击</p>
	 *
	 * @return boolean
	 */
	public static boolean doubleGainClick () {
		long now = System.currentTimeMillis();
		if (now - startTime < 2000) {
			return true;
		}
		startTime = now;
		return false;
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//		if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
		//		}
	}

	@Override
	protected void onPause () {
		if (isDebug)
			Log.e(TAG, "onPause: ");

		if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {
			//			mUvcCameraHandler.stopTemperaturing();
			//			if (BuildConfig.DEBUG)
			//				Log.e(TAG, "onPause: 停止温度回调");
		}
		//		mSensorManager.unregisterListener(sensorEventListener, mAccelerometerSensor);
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

	@Override
	protected void onResume () {
		if (isDebug)
			Log.e(TAG, "onResume: ");
		if (mUsbMonitor != null && !mUsbMonitor.isRegistered()) {
			mUsbMonitor.register();
		}
		super.onResume();
	}

	/**
	 * 打开连接 调用预览图像的设置
	 */
	private void startPreview () {
		if (isDebug)
			Log.e(TAG, "startPreview: ============================");

		mUvcCameraHandler.addCallback(cameraCallback);
		//				if (isDebug)Log.e(TAG, "startPreview: flPreview  width == " + mDataBinding.flPreview.getMeasuredWidth()
		//						+ " height == " + mDataBinding.flPreview.getMeasuredHeight());
		if (mDataBinding.textureViewPreviewActivity.getSurfaceTexture() == null) {
			Log.e(TAG, "startPreview: 启动失败,getSurfaceTexture 为null.");
			return;
		}
		stt = new Surface(mDataBinding.textureViewPreviewActivity.getSurfaceTexture());

		int mTextureViewWidth = mDataBinding.textureViewPreviewActivity.getWidth();
		int mTextureViewHeight = mDataBinding.textureViewPreviewActivity.getHeight();
		//		if (isDebug)Log.e(TAG,"height =="+ mTextureViewHeight + " width==" + mTextureViewWidth);
		mDataBinding.textureViewPreviewActivity.setFrameBitmap(highTempBt, lowTempBt, centerTempBt, normalPointBt, DensityUtil.dp2px(mContext.get(), 30));

		mDataBinding.textureViewPreviewActivity.iniTempBitmap(mTextureViewWidth, mTextureViewHeight);//初始化画板的值，是控件的像素的宽高
		mDataBinding.textureViewPreviewActivity.setVidPid(mVid, mPid);//设置vid  pid
		mDataBinding.textureViewPreviewActivity.initTempFontSize(mFontSize);
		mDataBinding.textureViewPreviewActivity.setTinyCCorrection(sp.getFloat(DYConstants.setting_correction, 0.0f));//设置vid  pid
		mDataBinding.textureViewPreviewActivity.setDragTempContainer(mDataBinding.dragTempContainerPreviewActivity);
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

		int paletteType = sp.getInt(DYConstants.PALETTE_NUMBER, 1);
		mUvcCameraHandler.PreparePalette(palettePath, paletteType, BuildConfig.CONFIGS_NAME);
		mUvcCameraHandler.setAreaCheck(0);

		//是否进行温度的绘制
		//	private int isWatermark;
		int isTempShow = 0;
		mUvcCameraHandler.tempShowOnOff(isTempShow);//是否显示绘制的温度 0不显示，1显示。最终调用的是UVCCameraTextureView的绘制线程。

		mUvcCameraHandler.startPreview(stt);
		//tinyC 暂时关闭 温度回调功能
		mUvcCameraHandler.startTemperaturing();//温度回调

		mDataBinding.dragTempContainerPreviewActivity.setBackgroundColor(getColor(R.color.bg_preview_otg_connect));
		mDataBinding.dragTempContainerPreviewActivity.setConnect(true);
		mDataBinding.dragTempContainerPreviewActivity.invalidate();

		//2022年3月27日12:58:52 吴长城  添加预览的高、低、中心温度
		mDataBinding.textureViewPreviewActivity.openFeaturePoints(0);
		mDataBinding.textureViewPreviewActivity.openFeaturePoints(1);
		mDataBinding.textureViewPreviewActivity.openFeaturePoints(2);

		if (mPid == 1 && mVid == 5396) {
			DYTApplication.setRobotSingle(DYTRobotSingle.S0_256_196);
		} else if (mPid == 22592 && mVid == 3034) {
			DYTApplication.setRobotSingle(DYTRobotSingle.TinYC_256_192);
		}
	}

	private int setValue (final int flag, final int value) {//设置机芯参数,调用JNI层
		return mUvcCameraHandler != null ? mUvcCameraHandler.setValue(flag, value) : 0;
	}

	/**
	 * 设置机芯参数 为默认值
	 */
	private int toSettingDefault () {
		Handler handler0 = new Handler();
		defaultSettingReturn = -1;
		if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
			handler0.postDelayed(() -> defaultSettingReturn = sendS0Order(DYConstants.SETTING_CORRECTION_DEFAULT_VALUE, DYConstants.SETTING_CORRECTION_INT), 0);

			handler0.postDelayed(() -> defaultSettingReturn = sendS0Order(DYConstants.SETTING_EMITTANCE_DEFAULT_VALUE, DYConstants.SETTING_EMITTANCE_INT), 150);
			handler0.postDelayed(() -> defaultSettingReturn = sendS0Order(DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE, DYConstants.SETTING_HUMIDITY_INT), 300);
			handler0.postDelayed(() -> defaultSettingReturn = sendS0Order(DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE, DYConstants.SETTING_ENVIRONMENT_INT), 450);
			handler0.postDelayed(() -> defaultSettingReturn = sendS0Order(DYConstants.SETTING_REFLECT_DEFAULT_VALUE, DYConstants.SETTING_REFLECT_INT), 600);
		} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
			handler0.postDelayed(() -> {
				//					result = sendS0Order(DYConstants.SETTING_CORRECTION_DEFAULT_VALUE,DYConstants.SETTING_CORRECTION_INT);
				defaultSettingReturn = mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, DYConstants.SETTING_EMITTANCE_DEFAULT_VALUE, 3);
			}, 0);
			//S0的湿度，对应大气透过率
			//			handler0.postDelayed(new Runnable() {
			//				@Override
			//				public void run () {
			//					//					result = sendS0Order(DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE,DYConstants.SETTING_HUMIDITY_INT);
			//					defaultSettingReturn = mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, DYConstants.SETTING_HUMIDITY_DEFAULT_VALUE, 4);
			//				}
			//			}, 200);
			handler0.postDelayed(() -> {
				//					result = sendS0Order(DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE,DYConstants.SETTING_ENVIRONMENT_INT);
				defaultSettingReturn = mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, DYConstants.SETTING_ENVIRONMENT_DEFAULT_VALUE, 2);
			}, 400);
			handler0.postDelayed(() -> {
				//					result = sendS0Order(DYConstants.SETTING_REFLECT_DEFAULT_VALUE,DYConstants.SETTING_REFLECT_INT);
				defaultSettingReturn = mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, DYConstants.SETTING_REFLECT_DEFAULT_VALUE, 1);
			}, 600);
		}
		return defaultSettingReturn;
	}

	//
	//	private ContextWrapper wrap (Context context) {
	//		Resources res = context.getResources();
	//		Configuration configuration = res.getConfiguration();
	//		//获得你想切换的语言，可以用SharedPreferences保存读取
	//		Locale newLocale = new Locale(getLanguageStr());
	//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
	//			configuration.setLocale(newLocale);
	//			LocaleList localeList = new LocaleList(newLocale);
	//			LocaleList.setDefault(localeList);
	//			configuration.setLocales(localeList);
	//			context = context.createConfigurationContext(configuration);
	//		} else {
	//			configuration.locale = newLocale;
	//			res.updateConfiguration(configuration, res.getDisplayMetrics());
	//		}
	//		return new ContextWrapper(context);
	//	}

	//	@Override
	//	public void onConfigurationChanged (@NonNull Configuration newConfig) {
	//		Log.e(TAG, "onConfigurationChanged: ===========");
	//		//		DisplayMetrics metrics = getResources().getDisplayMetrics();
	//		////		Configuration configuration = getResources().getConfiguration();
	//		//		if (getApplicationContext().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE)
	//		//				.getInt(DYConstants.LANGUAGE_SETTING, 0) == 0) {
	//		//			newConfig.setLocale(Locale.SIMPLIFIED_CHINESE);
	//		//		} else {
	//		//			newConfig.setLocale(Locale.US);
	//		//		}
	//		//		getResources().updateConfiguration(newConfig, metrics);
	//		// 这边只是切回英文，可以执行编写自己的业务逻辑
	//		// super.onConfigurationChanged(newConfig);	  改成以下即可
	//		super.onConfigurationChanged(newConfig);
	//		String languageToLoad = "en";
	//
	//		Locale locale = new Locale(languageToLoad);
	//
	//		Locale.setDefault(locale);
	//
	//		Configuration config = getResources().getConfiguration();
	//
	//		DisplayMetrics metrics = getResources().getDisplayMetrics();
	//
	//		config.locale = Locale.ENGLISH;
	//
	//		getResources().updateConfiguration(config, metrics);
	//
	//	}

	/**
	 * 切换语言
	 * <p>  法语fr-rFR、西班牙语es-rES、芬兰语fi-rFI、波兰语pl-rPL、葡萄牙语pt-rPT</p>
	 * <p> "Français","Español","Suomalainen","Polski","Português"</p>
	 * @param type 语言下标
	 */
	private void toSetLanguage (int type) {//切换语言
		Locale locale;
		Context context = DYTApplication.getInstance();
		Log.e(TAG, "toSetLanguage: type===========" + type);
		switch (type) {
			case 0://中文
				locale = Locale.SIMPLIFIED_CHINESE;
				break;
			case 1://英语
				locale = Locale.US;
				break;
			case 2://俄文 ru-rRU
				locale =  new Locale("ru", "RU");
				break;
			case 3://德语 ge-rGE
				locale = Locale.GERMAN;
				break;
			case 4://意大利语   it-rIT
				locale = Locale.ITALY;
				break;
			case 5://韩语
				locale = Locale.KOREA;
				break;
			case 6://日语
				locale = Locale.JAPAN;
				break;
			case 7://法语
				locale = Locale.FRANCE;
				break;
			case 8://西班牙语
				locale = new Locale("es","ES");;
				break;
			case 9://芬兰语
				locale = new Locale("fi","FI");
				break;
			case 10://波兰语
				locale = new Locale("pl","PL");
				break;
			case 11://葡萄牙语
				locale = new Locale("pt","PT");
				break;
			default:
				locale = Locale.SIMPLIFIED_CHINESE;
				break;
		}
		LanguageUtils.saveAppLocaleLanguage(locale.getLanguage());
		//		if (type == 0) {
		//			locale = Locale.SIMPLIFIED_CHINESE;
		//			LanguageUtils.saveAppLocaleLanguage(locale.getLanguage());
		//		} else if (type == 1) {
		//			locale = Locale.US;
		//			LanguageUtils.saveAppLocaleLanguage(locale.getLanguage());
		//		} else {
		//			return;
		//		}
		if (LanguageUtils.isSimpleLanguage(context, locale)) {
			showToast(R.string.toast_select_same_language);
			return;
		}
		LanguageUtils.updateLanguage(context, locale);//更新语言参数
		Intent intent = new Intent(context, PreviewActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		context.startActivity(intent);
	}

	@Override
	protected int bindingLayout () {
		return R.layout.activity_preview;
	}

	@Override
	protected String getLanguageStr () {
		if (sp != null) {
			return sp.getString(DYConstants.LANGUAGE_SETTING, Locale.getDefault().getLanguage());
		} else {
			return Locale.getDefault().getLanguage();
		}
	}

	@Override
	protected void initView () {
		//		Log.e(TAG, "initView: ==========================手机型号为：===" + Build.MODEL);
		sp = mContext.get().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		//		configuration = getResources().getConfiguration();
		//		metrics = getResources().getDisplayMetrics();
		loadingDialog = LoadingDialog.get(mContext.get());


		cameraCallback = new AbstractUVCCameraHandler.CameraCallback() {
			@Override
			public void onOpen () {
			}

			@Override
			public void onClose () {

			}

			@Override
			public void onStartPreview () {
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
				Log.e(TAG, "onSavePicFinished: =========");
				if (isFinish) {
					runOnUiThread(() -> {
						//拍照成功，并显示动画效果
						mDataBinding.ivSaveImgAnimator.setVisibility(View.VISIBLE);
						//							mDataBinding.ivSaveImgAnimator.bringToFront();
						Bitmap bitmap = BitmapFactory.decodeFile(picPath);
						mDataBinding.ivSaveImgAnimator.setImageBitmap(bitmap);

						AnimationSet setAnimation = new AnimationSet(true);
						// 特别说明以下情况
						// 因为在下面的旋转动画设置了无限循环(RepeatCount = INFINITE)
						// 所以动画不会结束，而是无限循环
						// 所以组合动画的下面两行设置是无效的， 以后设置的为准
						setAnimation.setRepeatMode(Animation.RESTART);
						setAnimation.setRepeatCount(1);// 设置了循环一次,但无效
						// 透明度动画
						Animation alpha = new AlphaAnimation(1, 0f);
						alpha.setDuration(500);
						alpha.setStartOffset(0);
						//				Log.e(TAG, "onClick: ===============" + mDataBinding.ivPreviewLeftGallery.getX());
						// 缩放动画
						Animation scale1 = new ScaleAnimation(0.9f, 0f, 0.9f, 0f, Animation.ABSOLUTE, mDataBinding.ivPreviewLeftGallery.getX(), Animation.ABSOLUTE, (mDataBinding.ivPreviewLeftGallery.getY() + mDataBinding.ivPreviewLeftGallery.getHeight()));
						scale1.setDuration(500);
						scale1.setStartOffset(0);
						//
						//				// 将创建的子动画添加到组合动画里
						setAnimation.addAnimation(alpha);
						setAnimation.addAnimation(scale1);
						//				// 使用
						mDataBinding.ivSaveImgAnimator.startAnimation(setAnimation);
						setAnimation.setAnimationListener(new Animation.AnimationListener() {
							@Override
							public void onAnimationStart (Animation animation) {

							}

							@Override
							public void onAnimationEnd (Animation animation) {
								mDataBinding.ivSaveImgAnimator.setVisibility(View.GONE);
							}

							@Override
							public void onAnimationRepeat (Animation animation) {

							}
						});
					});
				}

			}
		};


		if (sp.getInt(DYConstants.FIRST_RUN, -1) == -1) {
			isFirstRun = true;
			sp.edit().putInt(DYConstants.FIRST_RUN, 1).apply();
		}
		if (isFirstRun) {//第一次打开应用
			//默认不打开音频录制
			sp.edit().putInt(DYConstants.RECORD_AUDIO_SETTING, 1).apply();

			String language_local_str;
			//		if (isDebug)
			//			Log.e(TAG, "initView: ===============language_local_str==============" + language_local_str);
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
				language_local_str = sp.getString(DYConstants.LANGUAGE_SETTING, Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage());
			} else {
				language_local_str = sp.getString(DYConstants.LANGUAGE_SETTING, Resources.getSystem().getConfiguration().locale.getLanguage());
			}
			//	private DisplayMetrics metrics;
			//	private Configuration  configuration;
			//语言的 索引下标
			int language_index = sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, -1);
			if (new Locale("zh").getLanguage().equals(language_local_str)) {
				language_index = 0;
			} else {
				language_index = 1;
			}
			switch (language_index) {
				case 0:
					LanguageUtils.updateLanguage(mContext.get(), Locale.SIMPLIFIED_CHINESE);
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 0).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 1:
					LanguageUtils.updateLanguage(mContext.get(), Locale.UK);
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 1).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 2:
					LanguageUtils.updateLanguage(mContext.get(), new Locale("ru","RU"));
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 2).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 3:
					LanguageUtils.updateLanguage(mContext.get(), Locale.GERMAN);
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 3).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 4:
					LanguageUtils.updateLanguage(mContext.get(), Locale.ITALY);
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 4).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 5:
					LanguageUtils.updateLanguage(mContext.get(), Locale.KOREA);
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 5).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 6:
					LanguageUtils.updateLanguage(mContext.get(), Locale.JAPAN);
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 6).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 7://法语
					LanguageUtils.updateLanguage(mContext.get(), Locale.FRANCE);
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 7).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 8://西班牙语
					LanguageUtils.updateLanguage(mContext.get(), new Locale("es","ES"));
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 8).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 9://芬兰语
					LanguageUtils.updateLanguage(mContext.get(), new Locale("fi","FI"));
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 9).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 10://波兰语
					LanguageUtils.updateLanguage(mContext.get(), new Locale("pl","PL"));
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 10).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
				case 11://葡萄牙语
					LanguageUtils.updateLanguage(mContext.get(), new Locale("pt","pT"));
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 11).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;

				default:
					LanguageUtils.updateLanguage(mContext.get(), Locale.SIMPLIFIED_CHINESE);
					sp.edit().putInt(DYConstants.LANGUAGE_SETTING_INDEX, 0).apply();
					sp.edit().putString(DYConstants.LANGUAGE_SETTING, language_local_str).apply();
					break;
			}
		}

		highTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_high);
		lowTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_low);
		centerTempBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_higlowtemp_draw_widget_center);
		normalPointBt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_main_preview_measuretemp_point);

		DisplayMetrics dm = getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		mSendCommand = new SendCommand();


		XXPermissions.with(this).permission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).request(new OnPermissionCallback() {
			@Override
			public void onGranted (List<String> permissions, boolean all) {
				if (all) {
					mFontSize = FontUtils.adjustFontSize(screenWidth, screenHeight);//
					//					if (isDebug)Log.e(TAG, "onResult: mFontSize ==========> " + mFontSize);
					//					switch (BuildConfig.FLAVOR) {
					//						case DYConstants.COMPANY_DYT:
					//							Log.e(TAG, "onGranted: =========调用 COMPANY_DYT 的函数====================");
					//							//							com.dyt.wcc.dytpir.DYTDO.PrintSomething();
					//							break;
					//						case DYConstants.COMPANY_JMS:
					//							Log.e(TAG, "onGranted: ===========调用 COMPANY_JMS 的函数====================");
					//							//							com.dyt.wcc.jms.JMSTODO.jmstodo();
					//							break;
					//						case DYConstants.COMPANY_VICTOR:
					//							Log.e(TAG, "onGranted: ============调用 COMPANY_VICTOR 的函数====================");
					//							break;
					//					}

					AssetCopyer.copyAllAssets(DYTApplication.getInstance(), mContext.get().getExternalFilesDir(null).getAbsolutePath());
					Log.e(TAG, "===========getExternalFilesDir==========" + mContext.get().getExternalFilesDir(null).getAbsolutePath());
					palettePath = mContext.get().getExternalFilesDir(null).getAbsolutePath();
					Log.e(TAG, "onGranted: ===========palettePath=============》" + palettePath);
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

					mDataBinding.dragTempContainerPreviewActivity.setmSeekBar(mDataBinding.customSeekbarPreviewFragment);
					mDataBinding.dragTempContainerPreviewActivity.setTempSuffix(sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0));

					mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(), mDataBinding.textureViewPreviewActivity, 1, 384, 292, 1, null, mDataBinding.dragTempContainerPreviewActivity, 0);

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
	}

	/**
	 * 初始化界面的监听器
	 */
	private void initListener () {
		//测试的 监听器
		//		mDataBinding.btTest01.setVisibility(View.VISIBLE);
		mDataBinding.btTest01.setOnClickListener(v -> {
			//			CrashReport.testJavaCrash();
			//******************************testJNi***************************************
			//			mUvcCameraHandler.testJNi(Build.MODEL);
			//			Log.e(TAG, "initListener: " + mDataBinding.textureViewPreviewActivity.getTemperatureCallback());
			//******************************************如何打开PDF文档******************************
			//			String MenuUrl = "/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/SLReadMeCN.pdf";
			//			String googleUrl = "http://docs.google.com/gview?embedded=true&url=";
			//			Log.d(TAG, googleUrl + MenuUrl);
			//			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl + MenuUrl));
			//			startActivity(browserIntent);


			//			File file = new File("/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/SLReadMeCN.pdf");
			//
			//			ParcelFileDescriptor pdfFile = null;
			//			try {
			//				pdfFile = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY); //以只读的方式打开文件
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
			//			for (int i = 0; i < pageCount; i++) {//这里用循环把pdf所有的页面都写入bitmap数组，真正使用的时候最好不要这样，
			//				//因为一本pdf的书会有很多页，一次性全部打开会非常消耗内存，我打开一本两百多页的书就消耗了1.8G的内存，而且打开速度很慢。
			//				//真正使用的时候要采用动态加载，用户看到哪页才加载附近的几页。而且最好使用多线程在后台打开。
			//
			//				PdfRenderer.Page page = renderer.openPage(i);//根据i的变化打开每一页
			//				bitmap_pdf = Bitmap.createBitmap((int) (width), (int) (page.getHeight() * width / page.getWidth()), Bitmap.Config.ARGB_8888);//根据屏幕的高宽缩放生成bmp对象
			//				page.render(bitmap_pdf, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);//将pdf的内容写入bmp中
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
			//						startActivity(getPdfFileIntent("/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/SLReadMeCN.pdf"));
			//****************************动画开始*************************************
			//读取一张图片到某个控件，然后把图片缩小 给相册这个按钮。透明度逐渐变低
			//				Animator animator = new ObjectAnimator();
			//				Animation animation = new TranslateAnimation();

			//************************检测当前语言设置********************************
			//				if (isDebug){
			//					Log.e(TAG, "onClick: " + Locale.getDefault().getLanguage());
			//					Log.e(TAG, "onClick: " + sp.getInt(DYConstants.LANGUAGE_SETTING,8));
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
		});
		//
		/**
		 * 超温警告 ， 预览层去绘制框， DragTempContainer 控件去播放声音
		 */
		mDataBinding.toggleHighTempAlarm.setOnClickListener(v -> {
			mDataBinding.toggleHighTempAlarm.setSelected(!mDataBinding.toggleHighTempAlarm.isSelected());
			if (mDataBinding.toggleHighTempAlarm.isSelected()) {
				if (myNumberPicker == null) {
					myNumberPicker = new MyNumberPicker(mContext.get(), sp.getFloat("overTemp", 0.0f), mDataBinding.dragTempContainerPreviewActivity.getTempSuffixMode());
				}
				myNumberPicker.setListener(new MyNumberPicker.SetCompleteListener() {
					@Override
					public void onSetComplete (float setValue) {
						//							if (isDebug)Log.e(TAG, "onSetComplete: " + "confirm value = == > " + setValue  );
						mDataBinding.dragTempContainerPreviewActivity.openHighTempAlarm(setValue);
						mDataBinding.textureViewPreviewActivity.startTempAlarm(setValue);
						sp.edit().putFloat("overTemp", setValue).apply();
					}

					@Override
					public void onCancelListener () {
						//							Log.e(TAG, "onCancelListener: " + "cancel "  );
						mDataBinding.toggleHighTempAlarm.setSelected(false);
					}
				});
				myNumberPicker.setCancelable(false);

				//				myNumberPicker.setRotation(oldRotation != 0 && oldRotation != 270);
				myNumberPicker.show(getSupportFragmentManager(), null);
			} else {
				mDataBinding.textureViewPreviewActivity.stopTempAlarm();
				mDataBinding.dragTempContainerPreviewActivity.closeHighTempAlarm();
			}
		});

		//切换色板
		mDataBinding.ivPreviewLeftPalette.setOnClickListener(v -> {
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
			PLRPopupWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			PLRPopupWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
			PLRPopupWindows.setHeight(mDataBinding.llContainerPreviewSeekbar.getHeight());
			PLRPopupWindows.setWidth(mDataBinding.llContainerPreviewSeekbar.getWidth());

			PLRPopupWindows.setFocusable(false);
			PLRPopupWindows.setOutsideTouchable(true);
			PLRPopupWindows.setTouchable(true);

			//				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, 0, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			//			if (oldRotation == 90 || oldRotation == 180) {
			//				int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
			//				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			//				PLRPopupWindows.getContentView().setRotation(180);
			//			}
			//			if (oldRotation == 0 || oldRotation == 270) {
			int offsetX = 0;
			PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			PLRPopupWindows.getContentView().setRotation(0);
			//			}
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

			//			if (oldRotation == 90 || oldRotation == 180) {
			//				int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
			//				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			//				PLRPopupWindows.getContentView().setRotation(180);
			//			}
			//			if (oldRotation == 0 || oldRotation == 270) {
			int offsetX = 0;
			PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			PLRPopupWindows.getContentView().setRotation(0);
			//			}
			//			PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, 0, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
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

			//			if (oldRotation == 90 || oldRotation == 180) {
			//				Log.e(TAG, "initListener: ===oldRotation == 90 || oldRotation == 180=====");
			//				int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
			//				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			//				PLRPopupWindows.getContentView().setRotation(180);
			//			}
			//			if (oldRotation == 0 || oldRotation == 270) {
			Log.e(TAG, "initListener: oldRotation == 0 || oldRotation == 270===");
			int offsetX = 0;
			PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
			PLRPopupWindows.getContentView().setRotation(0);
			//			}

			//			PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, 0, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);

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
			if (mUvcCameraHandler == null || (!mUvcCameraHandler.snRightIsPreviewing()))
				return;
			if (!mDataBinding.toggleAreaCheck.isSelected()) {
				mDataBinding.dragTempContainerPreviewActivity.openAreaCheck(mDataBinding.textureViewPreviewActivity.getWidth(), mDataBinding.textureViewPreviewActivity.getHeight());
				int[] areaData = mDataBinding.dragTempContainerPreviewActivity.getAreaIntArray();
				mUvcCameraHandler.setArea(areaData);
				mUvcCameraHandler.setAreaCheck(1);
			} else {//close
				mUvcCameraHandler.setAreaCheck(0);
			}
			mDataBinding.toggleAreaCheck.setSelected(!mDataBinding.toggleAreaCheck.isSelected());
		});

		//固定温度条
		mDataBinding.toggleFixedTempBar.setOnClickListener(v -> {
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
		});

		//重置  按钮
		mDataBinding.ivPreviewLeftReset.setOnClickListener(v -> {
			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {
				//重置色板
				setPalette(0);
				//重置绘制界面
				mDataBinding.dragTempContainerPreviewActivity.clearAll();
				//关闭框内细查
				if (mDataBinding.toggleAreaCheck.isSelected()) {
					mDataBinding.toggleAreaCheck.setSelected(false);
					mUvcCameraHandler.setAreaCheck(0);
				}
				//关闭 超温警告
				mDataBinding.toggleHighTempAlarm.setSelected(false);
				mDataBinding.textureViewPreviewActivity.stopTempAlarm();
				mDataBinding.dragTempContainerPreviewActivity.closeHighTempAlarm();
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
				mHandler.postDelayed(() -> {
					setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
				}, 200);
				mHandler.postDelayed(() -> {
					mUvcCameraHandler.whenShutRefresh();
				}, 500);
			}
		});
		//拍照按钮
		mDataBinding.ivPreviewLeftTakePhoto.setOnClickListener(v -> {
			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {
				String picPath = Objects.requireNonNull(MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".jpg")).toString();
				//					if (mUvcCameraHandler.captureStill(picPath))
				//						showToast(getResources().getString(R.string.toast_save_path) + picPath);
				mUvcCameraHandler.captureStill(picPath);

				//						if (isDebug)Log.e(TAG, "onResult: java path === "+ picPath);
			} else {
				showToast(getResources().getString(R.string.toast_need_connect_camera));
			}
		});
		//录制 按钮
		mDataBinding.btPreviewLeftRecord.setOnClickListener(v -> {
			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing()) {//mUvcCameraHandler.isOpened()
				if (mDataBinding.btPreviewLeftRecord.isSelected() && mUvcCameraHandler.isRecording()) {//停止录制
					stopTimer();
					mUvcCameraHandler.stopRecording();
				} else if (!mDataBinding.btPreviewLeftRecord.isSelected() && !mUvcCameraHandler.isRecording() && mUvcCameraHandler.snRightIsPreviewing()) {//开始录制
					startTimer();
					mUvcCameraHandler.startRecording(sp.getInt(DYConstants.RECORD_AUDIO_SETTING, 1));
				}  //						if (isDebug)Log.e(TAG, "Record Error: error record state !");

				mDataBinding.btPreviewLeftRecord.setSelected(!mDataBinding.btPreviewLeftRecord.isSelected());
			} else {
				stopTimer();
				showToast(getResources().getString(R.string.toast_need_connect_camera));
			}
		});
		//相册按钮
		mDataBinding.ivPreviewLeftGallery.setOnClickListener(v -> {
			if (!mUvcCameraHandler.snRightIsPreviewing()) {
				return;
			}
			if (mDataBinding.btPreviewLeftRecord.isSelected()) {
				showToast(getResources().getString(R.string.toast_is_recording));
			} else {
				mUvcCameraHandler.close();
				mUsbMonitor.unregister();
				String fileProviderAuthority = BuildConfig.APPLICATION_ID + ".FileProvider";
				EasyPhotos.createAlbum(PreviewActivity.this, false, false, GlideEngine.getInstance()).setFileProviderAuthority(fileProviderAuthority).setCount(1000).setVideo(true).setGif(false).start(101);
			}
		});

		//设置弹窗
		mDataBinding.ivPreviewRightSetting.setOnClickListener(v -> {
			//打开设置第一步：获取机芯数据。
			if (mUvcCameraHandler == null)
				return;
			//是否出图
			if (!mUvcCameraHandler.snRightIsPreviewing()) {
				return;
			}
			//是否连续打开
			if (doubleClick())
				return;
			if (mUvcCameraHandler.isOpened()) {
				if (mPid == 1 && mVid == 5396) {
					getCameraParams();
					mHandler.sendEmptyMessage(MSG_CAMERA_PARAMS);
				} else if (mPid == 22592 && mVid == 3034) {
					new Thread(new Runnable() {
						@Override
						public void run () {
							getTinyCCameraParams();
							mHandler.sendEmptyMessage(MSG_CAMERA_PARAMS);
						}
					}).start();
				}
				//				mHandler.sendEmptyMessage(MSG_CAMERA_PARAMS);
			}
		});
		//		initCompanyPop();
		//公司信息弹窗   监听器使用的图表的监听器对象
		mDataBinding.ivPreviewLeftCompanyInfo.setOnClickListener(v -> {
			//请求权限
			CustomizeCompany customizeCompany;
			View view;
			switch (BuildConfig.FLAVOR) {
				case DYConstants.COMPANY_JMS:
					customizeCompany = new JMSCompanyView();
					view = customizeCompany.getCompanyView(mContext.get());
					break;
				case DYConstants.COMPANY_VICTOR:
					customizeCompany = new VictorCompanyView();
					view = customizeCompany.getCompanyView(mContext.get());
					view.findViewById(R.id.tv_about_main_user_manual_info).setOnClickListener(v2 -> {
						if (companyPopWindows != null && companyPopWindows.isShowing()) {
							companyPopWindows.dismiss();
						}
						startActivity(new Intent(PreviewActivity.this, PdfActivity.class));
					});
					break;
				case DYConstants.COMPANY_QIANLI:
					customizeCompany = new QianliCompanyView();
					view = customizeCompany.getCompanyView(mContext.get());
					view.findViewById(R.id.tv_about_main_user_manual_info_qianli).setOnClickListener(v3 -> {
						if (companyPopWindows != null && companyPopWindows.isShowing()) {
							companyPopWindows.dismiss();
						}
						startActivity(new Intent(PreviewActivity.this, ReadPdfActivity.class));
					});
					break;
				case DYConstants.COMPANY_TESLONG:
					customizeCompany = new TeslongCompanyView();
					view = customizeCompany.getCompanyView(mContext.get());
					break;
				case DYConstants.COMPANY_NEUTRAL:
					customizeCompany = new NeutralCompanyView();
					view = customizeCompany.getCompanyView(mContext.get());
					break;
				case DYConstants.COMPANY_MAILSEEY:
					customizeCompany = new MileSeeYCompanyView();
					view = customizeCompany.getCompanyView(mContext.get());
					view.findViewById(R.id.tv_about_main_user_manual_info_mileseey).setOnClickListener(v3 -> {
						if (companyPopWindows != null && companyPopWindows.isShowing()) {
							companyPopWindows.dismiss();
						}
						if (mUvcCameraHandler != null && !mUvcCameraHandler.snRightIsPreviewing()) {
							mUvcCameraHandler.close();
						}
						if (mUsbMonitor!=null && mUsbMonitor.isRegistered()) {
							mUsbMonitor.unregister();
						}
						startActivity(new Intent(PreviewActivity.this, com.dyt.wcc.customize.mailseey.PdfActivity.class));
					});
					break;
				default:
					customizeCompany = new DytCompanyView();
					view = customizeCompany.getCompanyView(mContext.get());
					break;
			}
			customizeCompany.initListener(view);
			showPopWindows(view);
		});
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

		//测温模式中 工具栏 点击监听器（删除，等工具栏）
		mDataBinding.dragTempContainerPreviewActivity.setChildToolsClickListener(new MeasureTempContainerView.OnChildToolsClickListener() {
			@Override
			public void onChildToolsClick (MeasureEntity child, int position) {
				//				if (isDebug)Log.e(TAG, "onChildToolsClick: ======preview tools position == > " + position);
				if (position == 0) {
					mDataBinding.dragTempContainerPreviewActivity.deleteChildView(child);
					//底层重新设置 矩形框的 数据
					if (child.getType() == 3) {
						//						mDataBinding.dragTempContainerPreviewActivity.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
						int[] areaData = mDataBinding.dragTempContainerPreviewActivity.getAreaIntArray();
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
					//					mDataBinding.dragTempContainerPreviewActivity.openAreaCheck(mDataBinding.textureViewPreviewFragment.getWidth(),mDataBinding.textureViewPreviewFragment.getHeight());
					int[] areaData = mDataBinding.dragTempContainerPreviewActivity.getAreaIntArray();
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
				mDataBinding.dragTempContainerPreviewActivity.setAllChildUnSelect();
			}
		});
	}

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
	 * * 湿度 6-7         humidity  取值 ： （0 - 100） 弃用
	 * * 多余 8-9             弃用
	 */
	private void getTinyCCameraParams () {//得到返回机芯的参数，128位。返回解析保存在cameraParams 中
		byte[] tempParams = mUvcCameraHandler.getTinyCCameraParams(20);
		byte[] tempisRight = mUvcCameraHandler.getTinyCCameraParams(20);
		cameraParams = ByteUtilsCC.tinyCByte2HashMap(tempParams);
		if (!cameraParamsIsRight(cameraParams, tempParams, tempisRight)) {
			getTinyCCameraParams();
			return;
		}
		//判断获取的参数是否是对的,否则再次请求。
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
	 * 显示pop弹窗
	 *
	 * @param view
	 */
	private void showPopWindows (View view) {
		companyPopWindows = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		companyPopWindows.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		companyPopWindows.setHeight(DensityUtil.dp2px(mContext.get(), BuildConfig.COMPANY_H));
		companyPopWindows.setWidth(mDataBinding.clPreviewActivity.getWidth() - DensityUtil.dp2px(mContext.get(), 20));

		companyPopWindows.setFocusable(false);
		companyPopWindows.setOutsideTouchable(true);
		companyPopWindows.setTouchable(true);

		//		if (oldRotation == 90 || oldRotation == 180) {
		//			//			if (isDebug)Log.e(TAG, "==================showPopWindows: 90 /180");
		//			int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
		//			companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.clPreviewActivity.getHeight() + companyPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
		//			companyPopWindows.getContentView().setRotation(180);
		//		}
		//		if (oldRotation == 0 || oldRotation == 270) {
		//			if (isDebug)Log.e(TAG, "============showPopWindows: 0 /270");
		int offsetX = DensityUtil.dp2px(mContext.get(), 10);
		companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -companyPopWindows.getHeight() - DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
		companyPopWindows.getContentView().setRotation(0);
		//		}
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
		if (PLRPopupWindows != null && PLRPopupWindows.isShowing())
			PLRPopupWindows.dismiss();
		super.onBackPressed();
		Log.e(TAG, "onBackPressed: ");
	}

	//修改S0机芯参数
	public class SendCommand {
		int psitionAndValue0 = 0, psitionAndValue1 = 0, psitionAndValue2 = 0, psitionAndValue3 = 0;
		int result = -1;

		public int sendFloatCommand (int position, byte value0, byte value1, byte value2, byte value3, int interval0, int interval1, int interval2, int interval3, int interval4) {
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