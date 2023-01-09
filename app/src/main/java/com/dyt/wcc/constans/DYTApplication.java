package com.dyt.wcc.constans;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dyt.wcc.common.base.BaseApplication;

/**
 * <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
 * <p>Author：stefan cheng    </p>
 * <p>Create Date：2021/9/26  16:18 </p>
 * <p>Description：@todo describe         </p>
 * <p>PackagePath: com.dyt.wcc.ui     </p>
 */
public class DYTApplication extends BaseApplication {

	private static final String         TAG         = "DYTApplication";
	//	private AppViewModel appViewModel;//全局唯一ViewModel
	@NonNull
	public static        DYTApplication instance;
	private static       DYTRobotSingle robotSingle = DYTRobotSingle.NO_DEVICE;

	public static DYTRobotSingle getRobotSingle () {
		return robotSingle;
	}

	public static void setRobotSingle (DYTRobotSingle robotSingle) {
		DYTApplication.robotSingle = robotSingle;
	}

	@NonNull
	public static DYTApplication getInstance () {
		return instance;
	}

	@Override
	public void onCreate () {
		super.onCreate();
		instance = this;
		//		CrashReport.initCrashReport(getApplicationContext(), "253f40d84b", false);
//		CrashReport.initCrashReport(getApplicationContext());
		//监听activity生命周期
		registerActivityLifecycleCallbacks();
	}

//	@Override
//	protected void attachBaseContext (Context base) {
//		Log.e(TAG, "=====app override===========attachBaseContext: ========================");
//		//		if (instance != null) {
//		//		}else{
//		//			SharedPreferences sp = instance.getSharedPreferences(DYConstants.SP_NAME,Context.MODE_PRIVATE);
//		super.attachBaseContext(LanguageUtils.wrap(base));
//		//		}
//	}

	private void registerActivityLifecycleCallbacks () {
		instance.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			@Override
			public void onActivityCreated (@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
				// 对Application和Activity更新上下文的语言环境
//				LanguageUtils.applyAppLanguage(activity);
			}

			@Override
			public void onActivityStarted (@NonNull Activity activity) {

			}

			@Override
			public void onActivityResumed (@NonNull Activity activity) {

			}

			@Override
			public void onActivityPaused (@NonNull Activity activity) {

			}

			@Override
			public void onActivityStopped (@NonNull Activity activity) {

			}

			@Override
			public void onActivitySaveInstanceState (@NonNull Activity activity, @NonNull Bundle outState) {

			}

			@Override
			public void onActivityDestroyed (@NonNull Activity activity) {

			}
		});
	}
}
