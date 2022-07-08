package com.dyt.wcc.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/6/30  19:44     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.victor.utils     </p>
 */
public class LanguageUtil {
	public static void applyLanguage(Context context, String newLanguage) {
		Resources resources = context.getResources();
		Configuration configuration = resources.getConfiguration();
		Locale locale = SupportLanguageUtil.getSupportLanguage(newLanguage);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			// apply locale
			configuration.setLocale(locale);

		} else {
			// updateConfiguration
			configuration.locale = locale;
			DisplayMetrics dm = resources.getDisplayMetrics();
			resources.updateConfiguration(configuration, dm);
		}
	}

	public static Context attachBaseContext(Context context, String language) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return createConfigurationResources(context, language);
		} else {
			applyLanguage(context, language);
			return context;
		}
	}

	@TargetApi(Build.VERSION_CODES.N)
	private static Context createConfigurationResources(Context context, String language) {
		Resources resources = context.getResources();
		Configuration configuration = resources.getConfiguration();
		Locale locale;
		if (TextUtils.isEmpty(language)) {//如果没有指定语言使用系统首选语言
			locale = SupportLanguageUtil.getSystemPreferredLanguage();
		} else {//指定了语言使用指定语言，没有则使用首选语言
			locale = SupportLanguageUtil.getSupportLanguage(language);
		}
		configuration.setLocale(locale);
		return context.createConfigurationContext(configuration);
	}
}
