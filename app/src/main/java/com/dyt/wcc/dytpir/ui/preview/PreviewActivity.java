package com.dyt.wcc.dytpir.ui.preview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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

import com.dyt.wcc.cameracommon.encoder.MediaMuxerWrapper;
import com.dyt.wcc.cameracommon.usbcameracommon.AbstractUVCCameraHandler;
import com.dyt.wcc.cameracommon.usbcameracommon.UVCCameraHandler;
import com.dyt.wcc.cameracommon.utils.ByteUtil;
import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.common.utils.DensityUtil;
import com.dyt.wcc.common.utils.FontUtils;
import com.dyt.wcc.common.widget.MyCustomRangeSeekBar;
import com.dyt.wcc.common.widget.dragView.DrawLineRectHint;
import com.dyt.wcc.common.widget.dragView.MeasureEntity;
import com.dyt.wcc.common.widget.dragView.MeasureTempContainerView;
import com.dyt.wcc.dytpir.BuildConfig;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.constans.DYConstants;
import com.dyt.wcc.dytpir.constans.DYTRobotSingle;
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
import com.huantansheng.easyphotos.ui.dialog.LoadingDialog;
import com.king.app.dialog.AppDialog;
import com.king.app.updater.AppUpdater;
import com.king.app.updater.http.OkHttpManager;
import com.permissionx.guolindev.PermissionX;
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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PreviewActivity extends BaseActivity<ActivityPreviewBinding> {
	//jni测试按钮状态记录
	//	private final int jni_status = 0;

	private UVCCameraHandler   mUvcCameraHandler;
	//点线矩形测温弹窗
	private PopupWindow        PLRPopupWindows;
	private PopupWindow        settingPopWindows;
	private PopupWindow        companyPopWindows;
	//	private View popView;
	private Map<String, Float> cameraParams;
	private SharedPreferences  sp;
	//设备 vid pid
	private int                mVid, mPid;

	private USBMonitor mUsbMonitor;

	private int    mFontSize;
	private String palettePath;
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

	//更新工具类对象
	private AppUpdater                              mAppUpdater;
	private int                                     maxIndex = 0;
	private List<UpdateObj>                         updateObjList;
	//传感器
	private SensorManager                           mSensorManager;
	//磁场传感器，加速度传感器 mMagneticSensor,
	private Sensor                                  mAccelerometerSensor;
	private AbstractUVCCameraHandler.CameraCallback cameraCallback;
	private LoadingDialog                           loadingDialog;

	private static final int     MSG_CHECK_UPDATE  = 1;
	private static final int     MSG_CAMERA_PARAMS = 2;
	private final        Handler mHandler          = new Handler(new Handler.Callback() {
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

					if (mPid == 1 && mVid == 5396) {//S0有湿度参数
						popSettingBinding.tvCameraSettingHumidity.setVisibility(View.VISIBLE);
					} else if (mPid == 22592 && mVid == 3034) {//TinyC 无湿度参数
						popSettingBinding.tvCameraSettingHumidity.setVisibility(View.GONE);
					}
					popSettingBinding.btCheckVersion.setOnClickListener(v1 -> {
						//点击之后 立即wifi 或者 移动数据 是否打开，给提示。  连接超时 也给提示
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
							}
						}).start();
					});
					//第二步：将获取的数据 展示在输入框内
					if (cameraParams != null) {
						popSettingBinding.etCameraSettingEmittance.setText(String.valueOf(cameraParams.get(DYConstants.setting_emittance)));//发射率 0-1

						popSettingBinding.etCameraSettingRevise.setText(String.valueOf(cameraParams.get(DYConstants.setting_correction)));//校正  -20 - 20
						popSettingBinding.etCameraSettingReflect.setText(String.valueOf((int) (cameraParams.get(DYConstants.setting_reflect) * 1)));//反射温度 -10-40
						popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf((int) (cameraParams.get(DYConstants.setting_environment) * 1)));//环境温度 -10 -40

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
									hideInput(v12.getWindowToken());
								}
								return true;
							});
						} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {

						}

						//TODO 设置机芯的默认值;
						popSettingBinding.btSettingDefault.setOnClickListener(v13 -> {
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
						});

						//发射率
						popSettingBinding.etCameraSettingEmittance.setOnEditorActionListener((v14, actionId, event) -> {
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v14.getText().toString()))
									return true;
								float value = Float.parseFloat(v14.getText().toString());
								if (value > 1 || value < 0) {
									showToast(getString(R.string.toast_range_int, 0, 1));
									return true;
								}
								v14.clearFocus();
								if (mUvcCameraHandler != null) {
									if (mPid == 1 && mVid == 5396) {
										sendS0Order(value, DYConstants.SETTING_EMITTANCE_INT);
									} else if (mPid == 22592 && mVid == 3034) {
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, value, 3);
									}
									sp.edit().putFloat(DYConstants.setting_emittance, value).apply();
									showToast(R.string.toast_complete_Emittance);
								}
								hideInput(v14.getWindowToken());
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
						popSettingBinding.etCameraSettingReflect.setOnEditorActionListener((v15, actionId, event) -> {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v15.getText().toString()))
									return true;
								float value = inputValue2Temp(Integer.parseInt(v15.getText().toString()));//拿到的都是摄氏度
								if (value > getBorderValue(120.0f) || value < getBorderValue(-20.0f)) {//带上 温度单位
									showToast(getString(R.string.toast_range_float, getBorderValue(-20.0f), getBorderValue(120.0f)));
									return true;
								}
								if (mUvcCameraHandler != null) {
									if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
										sendS0Order(value, DYConstants.SETTING_REFLECT_INT);
									} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
										Log.e(TAG, "onEditorAction: 反射温度 set value = " + value);
										mUvcCameraHandler.sendOrder(UVCCamera.CTRL_ZOOM_ABS, (int) value, 1);
									}
									sp.edit().putFloat(DYConstants.setting_reflect, value).apply();
									showToast(R.string.toast_complete_Reflect);
								}
								hideInput(v15.getWindowToken());
							}
							return true;
						});
						//校正设置 -20 - 20
						popSettingBinding.etCameraSettingRevise.setOnEditorActionListener((v16, actionId, event) -> {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v16.getText().toString()))
									return true;
								float value = inputValue2Temp(Float.parseFloat(v16.getText().toString()));
								if (value > getBorderValue(20.0f) || value < getBorderValue(-20.0f)) {
									showToast(getString(R.string.toast_range_float, getBorderValue(-20.0f), getBorderValue(20.0f)));
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
						//环境温度设置  -20 -50
						popSettingBinding.etCameraSettingFreeAirTemp.setOnEditorActionListener((v17, actionId, event) -> {
							//		Log.e(TAG, "Distance: " + popSettingBinding.etCameraSettingDistance.getText().toString());
							if (actionId == EditorInfo.IME_ACTION_DONE) {
								if (TextUtils.isEmpty(v17.getText().toString()))
									return true;
								float value = inputValue2Temp(Integer.parseInt(v17.getText().toString()));
								if (value > getBorderValue(50.0f) || value < getBorderValue(-20.0f)) {
									showToast(getString(R.string.toast_range_float, getBorderValue(-20.0f), getBorderValue(50.0f)));
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
					popSettingBinding.switchChoiceTempUnit.setText(DYConstants.tempUnit).setSelectedTab(temp_unit).setOnSwitchListener((position, tabText) -> {//切换 温度单位 监听器
						mDataBinding.dragTempContainerPreviewActivity.setTempSuffix(position);
						sp.edit().putInt(DYConstants.TEMP_UNIT_SETTING, position).apply();
						//切换 温度单位 需要更改 输入框的 单位
						popSettingBinding.tvCameraSettingReviseUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
						popSettingBinding.tvCameraSettingReflectUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
						popSettingBinding.tvCameraSettingFreeAirTempUnit.setText(String.format("(%s)", DYConstants.tempUnit[sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0)]));
						//切换 温度单位 需要更改 里面已经输入的值。
						popSettingBinding.etCameraSettingRevise.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_correction, 0.0f))));
						popSettingBinding.etCameraSettingFreeAirTemp.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_environment, 0.0f))));
						popSettingBinding.etCameraSettingReflect.setText(String.valueOf(refreshValueByTempUnit(sp.getFloat(DYConstants.setting_reflect, 0.0f))));
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
					if (oldRotation == 90 || oldRotation == 180) {
						int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
						settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() + settingPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 5), Gravity.CENTER);
						settingPopWindows.getContentView().setRotation(180);
					}
					if (oldRotation == 0 || oldRotation == 270) {
						int offsetX = DensityUtil.dp2px(mContext.get(), 10);
						settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() / 2 - DensityUtil.dp2px(mContext.get(), 15), Gravity.CENTER);
						settingPopWindows.getContentView().setRotation(0);
					}

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
					popSettingBinding.switchChoiceRecordAudio.setSelectedTab(sp.getInt(DYConstants.RECORD_AUDIO_SETTING, 1));
					popSettingBinding.switchChoiceRecordAudio.setOnSwitchListener((position, tabText) -> {
						sp.edit().putInt(DYConstants.RECORD_AUDIO_SETTING, position).apply();
						//						if (isDebug)Log.e(TAG, "onSwitch: " + "=============================");
					});
					//高低温增益切换
					//tinyc set gain mode:Data_L = 0x0, low gain， Data_L = 0x1,  high gain
					//拔出之后默认为低温模式。
					popSettingBinding.switchHighlowGain.setSelectedTab(sp.getInt(DYConstants.GAIN_TOGGLE_SETTING, 0));
					popSettingBinding.switchHighlowGain.setOnSwitchListener((position, tabText) -> {
						//						if (doubleGainClick()) {
						//							return;
						//						}
						sp.edit().putInt(DYConstants.GAIN_TOGGLE_SETTING, position).apply();
						Log.e(TAG, "handleMessage: 111111111============" + position);
						if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
							if (position == 0) {
								mHandler.postDelayed(() -> setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8021), 200); //最高200度
							} else {
								mHandler.postDelayed(() -> setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8020), 200);//最高599度
							}
							mHandler.postDelayed(() -> setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000), 1000);
							mHandler.postDelayed(() -> mUvcCameraHandler.whenShutRefresh(), 1800);
						} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
							loadingDialog.show();
							if (position == 0) {
								new Thread(new Runnable() {
									@Override
									public void run () {
										mUvcCameraHandler.setMachineSetting(UVCCamera.CTRL_ZOOM_ABS, 1, 1);
									}
								}).start();

								mHandler.postDelayed(new Runnable() {
									@Override
									public void run () {
										setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
										runOnUiThread(new Runnable() {
											@Override
											public void run () {
												loadingDialog.dismiss();
											}
										});
									}
								}, 2000);
							} else {
								new Thread(new Runnable() {
									@Override
									public void run () {
										boolean status = mUvcCameraHandler.setMachineSetting(UVCCamera.CTRL_ZOOM_ABS, 0, 0);
									}
								}).start();
								mHandler.postDelayed(new Runnable() {
									@Override
									public void run () {
										setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
										runOnUiThread(new Runnable() {
											@Override
											public void run () {
												loadingDialog.dismiss();
											}
										});
									}
								}, 2000);
							}
						}
					});


					// 切换语言 spinner
					popSettingBinding.btShowChoiceLanguage.setText(DYConstants.languageArray[sp.getInt(DYConstants.LANGUAGE_SETTING, 0)]);
					popSettingBinding.btShowChoiceLanguage.setOnClickListener(v18 -> {
						AlertDialog.Builder builder = new AlertDialog.Builder(mContext.get());
						//						AlertDialog dialog =
						//						AlertDialog alertDialog =
						builder.setSingleChoiceItems(DYConstants.languageArray, sp.getInt(DYConstants.LANGUAGE_SETTING, 0), (dialog, which) -> {
							if (which != sp.getInt(DYConstants.LANGUAGE_SETTING, 0)) {
								sp.edit().putInt(DYConstants.LANGUAGE_SETTING, which).apply();
								PLRPopupWindows.dismiss();
								toSetLanguage(which);
							}
							dialog.dismiss();
						}).create();
						builder.show();
					});
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

	private final SensorEventListener sensorEventListener = new SensorEventListener() {
		@Override
		public void onSensorChanged (SensorEvent event) {
			if (event.sensor.getStringType().equals(Sensor.STRING_TYPE_ACCELEROMETER)) {//								Log.e(TAG, "onSensorChanged: Sensor.STRING_TYPE_ACCELEROMETER");
				float x = event.values[0];
				float y = event.values[1];
				float z = event.values[2];
				relayout(x, y, z);
			}
		}

		@Override
		public void onAccuracyChanged (Sensor sensor, int accuracy) {
		}
	};
	private       int                 oldRotation         = 0;

	private void relayout (float x, float y, float z) {
		if (x > -2.5 && x <= 2.5 && y > 7.5 && y <= 10 && oldRotation != 270) {
			oldRotation = 270;
			mDataBinding.clMainPreview.setRotation(0);
			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
				mUvcCameraHandler.setRotateMatrix_180(false);
			mDataBinding.textureViewPreviewActivity.S0_RotateMatrix_180(false);

			mDataBinding.dragTempContainerPreviewActivity.setAllChildViewRotate(false);

			if (PLRPopupWindows != null && PLRPopupWindows.isShowing()) {
				PLRPopupWindows.dismiss();
				if (oldRotation == 0 || oldRotation == 270) {
					int offsetX = 0;
					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
					PLRPopupWindows.getContentView().setRotation(0);
				}
			}

			if (settingPopWindows != null && settingPopWindows.isShowing()) {
				settingPopWindows.dismiss();
				if (oldRotation == 0 || oldRotation == 270) {
					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() / 2 - DensityUtil.dp2px(mContext.get(), 15), Gravity.CENTER);
					settingPopWindows.getContentView().setRotation(0);
				}
			}

			if (companyPopWindows != null && companyPopWindows.isShowing()) {
				companyPopWindows.dismiss();
				if (oldRotation == 0 || oldRotation == 270) {
					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
					companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -companyPopWindows.getHeight() - DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
					companyPopWindows.getContentView().setRotation(0);
				}
			}
			if (myNumberPicker != null && myNumberPicker.isVisible()) {
				myNumberPicker.dismiss();
				myNumberPicker.setRotation(false);
				myNumberPicker.show(getSupportFragmentManager(), null);
			}
			setMRotation(0);
		} else if (x > 7.5 && x <= 10 && y > -2.5 && y <= 2.5 && oldRotation != 0) {
			oldRotation = 0;
			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
				mUvcCameraHandler.setRotateMatrix_180(false);
			mDataBinding.textureViewPreviewActivity.S0_RotateMatrix_180(false);

			mDataBinding.clMainPreview.setRotation(0);
			mDataBinding.dragTempContainerPreviewActivity.setAllChildViewRotate(false);
			if (PLRPopupWindows != null && PLRPopupWindows.isShowing()) {
				PLRPopupWindows.dismiss();
				if (oldRotation == 0 || oldRotation == 270) {
					int offsetX = 0;
					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
					PLRPopupWindows.getContentView().setRotation(0);
				}
			}
			if (settingPopWindows != null && settingPopWindows.isShowing()) {
				settingPopWindows.dismiss();
				if (oldRotation == 0 || oldRotation == 270) {
					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() / 2 - DensityUtil.dp2px(mContext.get(), 15), Gravity.CENTER);
					settingPopWindows.getContentView().setRotation(0);
				}
			}

			if (companyPopWindows != null && companyPopWindows.isShowing()) {
				companyPopWindows.dismiss();
				if (oldRotation == 0 || oldRotation == 270) {
					int offsetX = DensityUtil.dp2px(mContext.get(), 10);
					companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -companyPopWindows.getHeight() - DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
					companyPopWindows.getContentView().setRotation(0);
				}
			}

			if (myNumberPicker != null && myNumberPicker.isVisible()) {
				myNumberPicker.dismiss();
				myNumberPicker.setRotation(false);
				myNumberPicker.show(getSupportFragmentManager(), null);
			}
			setMRotation(0);
		} else if (x > -2.5 && x <= 2.5 && y > -10 && y <= -7.5 && oldRotation != 90) {
			oldRotation = 90;
			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
				mUvcCameraHandler.setRotateMatrix_180(true);
			mDataBinding.textureViewPreviewActivity.S0_RotateMatrix_180(true);
			//			mDataBinding.dragTempContainerPreviewActivity.setRotation(180);
			//			mDataBinding.textureViewPreviewActivity.setRotation(180);
			mDataBinding.clMainPreview.setRotation(180);
			//			mDataBinding.dragTempContainerPreviewActivity.setRotation(180);
			mDataBinding.dragTempContainerPreviewActivity.setAllChildViewRotate(true);
			if (PLRPopupWindows != null && PLRPopupWindows.isShowing()) {
				PLRPopupWindows.dismiss();
				if (oldRotation == 90 || oldRotation == 180) {
					int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
					PLRPopupWindows.getContentView().setRotation(180);
				}
			}
			if (settingPopWindows != null && settingPopWindows.isShowing()) {
				settingPopWindows.dismiss();
				//第四步：显示控件
				if (oldRotation == 90 || oldRotation == 180) {
					int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() + settingPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 5), Gravity.CENTER);
					settingPopWindows.getContentView().setRotation(180);
				}
			}

			if (companyPopWindows != null && companyPopWindows.isShowing()) {
				companyPopWindows.dismiss();
				if (oldRotation == 90 || oldRotation == 180) {
					int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
					companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.clPreviewActivity.getHeight() + companyPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
					companyPopWindows.getContentView().setRotation(180);
				}
			}

			if (myNumberPicker != null && myNumberPicker.isVisible()) {
				myNumberPicker.dismiss();
				myNumberPicker.setRotation(true);
				myNumberPicker.show(getSupportFragmentManager(), null);
			}
			setMRotation(180);
		} else if (x > -10 && x <= -7.5 && y > -2.5 && y < 2.5 && oldRotation != 180) {
			oldRotation = 180;
			if (mUvcCameraHandler != null && mUvcCameraHandler.snRightIsPreviewing())
				mUvcCameraHandler.setRotateMatrix_180(true);
			mDataBinding.textureViewPreviewActivity.S0_RotateMatrix_180(true);
			//			mDataBinding.dragTempContainerPreviewActivity.setRotation(180);
			//			mDataBinding.textureViewPreviewActivity.setRotation(180);
			mDataBinding.clMainPreview.setRotation(180);
			mDataBinding.dragTempContainerPreviewActivity.setAllChildViewRotate(true);
			if (PLRPopupWindows != null && PLRPopupWindows.isShowing()) {
				PLRPopupWindows.dismiss();
				if (oldRotation == 90 || oldRotation == 180) {
					int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
					PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
					PLRPopupWindows.getContentView().setRotation(180);
				}
			}
			if (settingPopWindows != null && settingPopWindows.isShowing()) {
				settingPopWindows.dismiss();
				//第四步：显示控件
				if (oldRotation == 90 || oldRotation == 180) {
					int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
					settingPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight() + settingPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 5), Gravity.CENTER);
					settingPopWindows.getContentView().setRotation(180);
				}
			}

			if (companyPopWindows != null && companyPopWindows.isShowing()) {
				companyPopWindows.dismiss();
				if (oldRotation == 90 || oldRotation == 180) {
					int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
					companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.clPreviewActivity.getHeight() + companyPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
					companyPopWindows.getContentView().setRotation(180);
				}
			}

			if (myNumberPicker != null && myNumberPicker.isVisible()) {
				myNumberPicker.dismiss();
				myNumberPicker.setRotation(true);
				myNumberPicker.show(getSupportFragmentManager(), null);
			}
			setMRotation(180);
		}
	}

	@Override
	protected void onResume () {
		if (isDebug)
			Log.e(TAG, "onResume: ");
		if (mUsbMonitor != null && !mUsbMonitor.isRegistered()) {
			mUsbMonitor.register();
		}
		if (mSensorManager != null) {
			mSensorManager.registerListener(sensorEventListener, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
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

	private final USBMonitor.OnDeviceConnectListener onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
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
				mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(), mDataBinding.textureViewPreviewActivity, 1, 384, 292, 1, null, 0);
			}

			mUvcCameraHandler.open(ctrlBlock);
			startPreview();

			mHandler.postDelayed(() -> {
				setValue(UVCCamera.CTRL_ZOOM_ABS, DYConstants.CAMERA_DATA_MODE_8004);//切换数据输出8004原始8005yuv,80ff保存
			}, 300);
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
			}else {
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

	private Surface stt;

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
		mUvcCameraHandler.PreparePalette(palettePath, paletteType);
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

		//初始化 设置为低温模式
		//		if (DYTApplication.getRobotSingle() == DYTRobotSingle.S0_256_196) {
		//			mHandler.postDelayed(() -> setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8021), 200); //最高200度
		//			mHandler.postDelayed(() -> setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000), 1000);
		//			mHandler.postDelayed(() -> mUvcCameraHandler.whenShutRefresh(), 1800);
		//		} else if (DYTApplication.getRobotSingle() == DYTRobotSingle.TinYC_256_192) {
		////			loadingDialog.show();
		//				new Thread(new Runnable() {
		//					@Override
		//					public void run () {
		//						mUvcCameraHandler.setMachineSetting(UVCCamera.CTRL_ZOOM_ABS, 1, 1);
		//					}
		//				}).start();
		//
		//				mHandler.postDelayed(new Runnable() {
		//					@Override
		//					public void run () {
		//						setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
		//						runOnUiThread(new Runnable() {
		//							@Override
		//							public void run () {
		////								loadingDialog.dismiss();
		//							}
		//						});
		//					}
		//				}, 2000);
		//		}
	}

	private int setValue (final int flag, final int value) {//设置机芯参数,调用JNI层
		return mUvcCameraHandler != null ? mUvcCameraHandler.setValue(flag, value) : 0;
	}

	//	private static final int REQUEST_CODE_UNKNOWN_APP = 10085;


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

	public static long startTime = 0;

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

	/**
	 * 切换语言
	 *
	 * @param type 语言下标
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

	@Override
	protected int bindingLayout () {
		return R.layout.activity_preview;
	}

	@Override
	protected void initView () {
//		Log.e(TAG, "initView: ==========================手机型号为：===" + Build.MODEL);
		sp = mContext.get().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		configuration = getResources().getConfiguration();
		metrics = getResources().getDisplayMetrics();
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
						//
						////				// 旋转动画
						////				Animation rotate = new RotateAnimation(0,360,Animation.RELATIVE_TO_SELF,
						////						0.5f,Animation.RELATIVE_TO_SELF,0.5f);
						////				rotate.setDuration(1000);
						////				rotate.setRepeatMode(Animation.RESTART);
						////				rotate.setRepeatCount(Animation.INFINITE);
						//
						//				 平移动画
						//							Animation translate = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF,0f,
						//									Animation.ABSOLUTE, mDataBinding.ivPreviewLeftGallery.getX(),
						//									TranslateAnimation.RELATIVE_TO_SELF,0f,
						//									Animation.ABSOLUTE,(mDataBinding.ivPreviewLeftGallery.getY()
						//									+ mDataBinding.ivPreviewLeftGallery.getHeight()));
						//							translate.setDuration(500);
						//							translate.setStartOffset(0);
						//
						//				// 透明度动画
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
						//				setAnimation.addAnimation(rotate);
						//							setAnimation.addAnimation(translate);
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

		PermissionX.init(this).permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).onExplainRequestReason((scope, deniedList) -> scope.showRequestReasonDialog(deniedList, getResources().getString(R.string.toast_base_permission_explain), getResources().getString(R.string.confirm), getResources().getString(R.string.cancel))).onForwardToSettings((scope, deniedList) -> scope.showForwardToSettingsDialog(deniedList, getResources().getString(R.string.toast_base_permission_tosetting), getResources().getString(R.string.confirm), getResources().getString(R.string.cancel))).request((allGranted, grantedList, deniedList) -> {
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

				mDataBinding.dragTempContainerPreviewActivity.setmSeekBar(mDataBinding.customSeekbarPreviewFragment);
				mDataBinding.dragTempContainerPreviewActivity.setTempSuffix(sp.getInt(DYConstants.TEMP_UNIT_SETTING, 0));

				mUvcCameraHandler = UVCCameraHandler.createHandler((Activity) mContext.get(), mDataBinding.textureViewPreviewActivity, 1, 384, 292, 1, null, 0);

				//					fl = mDataBinding.flPreview;

				mUsbMonitor = new USBMonitor(mContext.get(), onDeviceConnectListener);
			} else {
				showToast(getResources().getString(R.string.toast_dont_have_permission));
			}
		});
	}

	private MyNumberPicker myNumberPicker;

	/**
	 * 初始化界面的监听器
	 */
	private void initListener () {
		//测试的 监听器
//		mDataBinding.btTest01.setVisibility(View.VISIBLE);
		mDataBinding.btTest01.setOnClickListener(v -> {
			//******************************testJNi***************************************
						mUvcCameraHandler.testJNi(Build.MODEL);
//			Log.e(TAG, "initListener: " + mDataBinding.textureViewPreviewActivity.getTemperatureCallback());


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

				myNumberPicker.setRotation(oldRotation != 0 && oldRotation != 270);
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
			if (oldRotation == 90 || oldRotation == 180) {
				int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
				PLRPopupWindows.getContentView().setRotation(180);
			}
			if (oldRotation == 0 || oldRotation == 270) {
				int offsetX = 0;
				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
				PLRPopupWindows.getContentView().setRotation(0);
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

			if (oldRotation == 90 || oldRotation == 180) {
				int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
				PLRPopupWindows.getContentView().setRotation(180);
			}
			if (oldRotation == 0 || oldRotation == 270) {
				int offsetX = 0;
				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
				PLRPopupWindows.getContentView().setRotation(0);
			}
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

			if (oldRotation == 90 || oldRotation == 180) {
				Log.e(TAG, "initListener: ===oldRotation == 90 || oldRotation == 180=====");
				int offsetX = -mDataBinding.llContainerPreviewSeekbar.getWidth();
				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
				PLRPopupWindows.getContentView().setRotation(180);
			}
			if (oldRotation == 0 || oldRotation == 270) {
				Log.e(TAG, "initListener: oldRotation == 0 || oldRotation == 270===");
				int offsetX = 0;
				PLRPopupWindows.showAsDropDown(mDataBinding.llContainerPreviewSeekbar, offsetX, -mDataBinding.llContainerPreviewSeekbar.getHeight(), Gravity.CENTER);
				PLRPopupWindows.getContentView().setRotation(0);
			}

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
			if (!mDataBinding.toggleAreaCheck.isSelected()) {
				mDataBinding.dragTempContainerPreviewActivity.openAreaCheck(mDataBinding.textureViewPreviewActivity.getWidth(), mDataBinding.textureViewPreviewActivity.getHeight());
				int[] areaData = mDataBinding.dragTempContainerPreviewActivity.getAreaIntArray();

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
				},200);
				mHandler.postDelayed(() -> {
					mUvcCameraHandler.whenShutRefresh();
				},500);
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
				EasyPhotos.createAlbum(PreviewActivity.this, false, false, GlideEngine.getInstance()).setFileProviderAuthority("com.dyt.wcc.dytpir.FileProvider").setCount(1000).setVideo(true).setGif(false).start(101);
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
			if (doubleClick())
				return;
			//是否连续打开
			if (mUvcCameraHandler.isOpened()) {
				if (mPid == 1 && mVid == 5396) {
					getCameraParams();//
				} else if (mPid == 22592 && mVid == 3034) {
					new Thread(new Runnable() {
						@Override
						public void run () {
							getTinyCCameraParams();
						}
					}).start();
				}
				mHandler.sendEmptyMessage(MSG_CAMERA_PARAMS);
			}
		});
		//公司信息弹窗   监听器使用的图表的监听器对象
		mDataBinding.ivPreviewLeftCompanyInfo.setOnClickListener(v -> {
			//请求权限
			View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_company_info, null);
			PopCompanyInfoBinding popCompanyInfoBinding = DataBindingUtil.bind(view);
			assert popCompanyInfoBinding != null;
			//				popCompanyInfoBinding.tvVersionName.setText(String.format("%s", LanguageUtils.getVersionName(mContext.get())));
			popCompanyInfoBinding.tvContactusEmail.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
			popCompanyInfoBinding.tvContactusEmail.setOnClickListener(v19 -> {
				Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
				emailIntent.setData(Uri.parse(getResources().getString(R.string.contactus_email_head) + getResources().getString(R.string.ContactEmail)));
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, "反馈标题");
				emailIntent.putExtra(Intent.EXTRA_TEXT, "反馈内容");
				//没有默认的发送邮件应用
				startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.contactus_choice_email)));
			});
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

	//色板切换
	View.OnClickListener                   paletteChoiceListener      = v -> {
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
	//用户添加 测温模式 切换
	View.OnClickListener                   tempModeCheckListener      = new View.OnClickListener() {
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
		companyPopWindows.setHeight(DensityUtil.dp2px(mContext.get(), 105));
		companyPopWindows.setWidth(mDataBinding.clPreviewActivity.getWidth() - DensityUtil.dp2px(mContext.get(), 20));
		//		Log.e(TAG, "================showPopWindows: " + companyPopWindows.getHeight());
		//		Log.e(TAG, "================showPopWindows: " + mDataBinding.clPreviewActivity.getHeight());

		companyPopWindows.setFocusable(false);
		companyPopWindows.setOutsideTouchable(true);
		companyPopWindows.setTouchable(true);

		if (oldRotation == 90 || oldRotation == 180) {
			//			if (isDebug)Log.e(TAG, "==================showPopWindows: 90 /180");
			int offsetX = -mDataBinding.clPreviewActivity.getWidth() + DensityUtil.dp2px(mContext.get(), 10);//
			companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -mDataBinding.clPreviewActivity.getHeight() + companyPopWindows.getHeight() + DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
			companyPopWindows.getContentView().setRotation(180);
		}
		if (oldRotation == 0 || oldRotation == 270) {
			//			if (isDebug)Log.e(TAG, "============showPopWindows: 0 /270");
			int offsetX = DensityUtil.dp2px(mContext.get(), 10);
			companyPopWindows.showAsDropDown(mDataBinding.clPreviewActivity, offsetX, -companyPopWindows.getHeight() - DensityUtil.dp2px(mContext.get(), 10), Gravity.CENTER);
			companyPopWindows.getContentView().setRotation(0);
		}
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
}