package com.dyt.wcc.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dyt.wcc.constans.DYConstants;
import com.dyt.wcc.constans.DYTApplication;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * 多语言工具类
 * Created by Fitem on 2020/03/20.
 */

public class LanguageUtils {
	public static final String SYSTEM_LANGUAGE_TGA = "systemLanguageTag";

	public static synchronized String getVersionName (Context context) {//得到软件版本名，eg:1.0
		try {
			PackageManager packageManager = context.getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ContextWrapper wrap (Context context) {
		Resources res = context.getResources();
		Configuration configuration = res.getConfiguration();
		//获得你想切换的语言，可以用SharedPreferences保存读取
		Locale newLocale = Locale.getDefault();
//		Log.e("LanguageUtils", "wrap: ======================="+ configuration.locale.getLanguage());
//		Log.e("LanguageUtils", "wrap: =================newLocale======"+ newLocale.getLanguage());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			configuration.setLocale(newLocale);
			LocaleList localeList = new LocaleList(newLocale);
			LocaleList.setDefault(localeList);
			configuration.setLocales(localeList);
			context = context.createConfigurationContext(configuration);
		}
		//		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
		//					configuration.setLocale(newLocale);
		//					context = context.createConfigurationContext(configuration);
		//				}
		else {
			configuration.locale = newLocale;
			res.updateConfiguration(configuration, res.getDisplayMetrics());
		}
		return new ContextWrapper(context);
	}

	/**
	 * 更新该context的config语言配置，对于application进行反射更新
	 *
	 * @param context
	 * @param locale
	 */
	public static void updateLanguage (final Context context, Locale locale) {
		Resources resources = context.getResources();
		Configuration config = resources.getConfiguration();
		Locale contextLocale = config.locale;
//		Log.e("===updateLanguage====", "updateLanguage: =======现有语言contextLocale.getLanguage()==============="+ contextLocale.getLanguage());
//		Log.e("===updateLanguage====", "updateLanguage: =======目标切换getLanguage()==============="+ contextLocale.getLanguage());
		if (isSameLocale(contextLocale, locale)) {
			return;
		}
		DisplayMetrics dm = resources.getDisplayMetrics();
		config.setLocale(locale);
		if (context instanceof Application) {
			Context newContext = context.createConfigurationContext(config);
			try {
				//noinspection JavaReflectionMemberAccess
				Field mBaseField = ContextWrapper.class.getDeclaredField("mBase");
				mBaseField.setAccessible(true);
				mBaseField.set(context, newContext);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resources.updateConfiguration(config, dm);
	}

	/**
	 * 对Application上下文进行替换
	 *
	 * @param activity activity
	 */
	public static void applyAppLanguage (@NonNull Activity activity) {
		Locale appLocale = getCurrentAppLocale();
		updateLanguage(DYTApplication.getInstance(), appLocale);
		updateLanguage(activity, appLocale);
	}

	/**
	 * 获取系统Local
	 *
	 * @return
	 */
	public static Locale getSystemLocale () {
		return Resources.getSystem().getConfiguration().locale;
	}

	/**
	 * 获取app缓存语言
	 *
	 * @return
	 */
	private static String getPrefAppLocaleLanguage () {
		SharedPreferences sp = DYTApplication.getInstance().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		if (sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, -1) == 0) {
			return new Locale("zh").getLanguage();
		} else {
			return new Locale("en").getLanguage();
		}
		//		return sp.getString(DYConstants.LANGUAGE_SETTING, "");
	}

	/**
	 * 获取app缓存Locale
	 *
	 * @return null则无
	 */
	public static Locale getPrefAppLocale () {
		String appLocaleLanguage = getPrefAppLocaleLanguage();
		//		if (!TextUtils.isEmpty(appLocaleLanguage)) {
		//			if (SYSTEM_LANGUAGE_TGA.equals(appLocaleLanguage)) { //系统语言则返回null
		//				return null;
		//			} else {
		//				return Locale.forLanguageTag(appLocaleLanguage);
		//			}
		//		}
		//		return Locale.SIMPLIFIED_CHINESE; // 为空，默认是简体中文
		if (new Locale("zh").getLanguage().equals(appLocaleLanguage)) {
			return Locale.SIMPLIFIED_CHINESE;
		} else {
			return Locale.UK;
		}
	}

	/**
	 * 获取当前需要使用的locale，用于activity上下文的生成
	 *
	 * @return
	 */
	public static Locale getCurrentAppLocale () {
		Locale prefAppLocale = getPrefAppLocale();
		return prefAppLocale == null ? getSystemLocale() : prefAppLocale;
	}

	/**
	 * 缓存app当前语言
	 *
	 * @param language
	 */
	public static void saveAppLocaleLanguage (String language) {
		SharedPreferences sp = DYTApplication.getInstance().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = sp.edit();
		edit.putString(DYConstants.LANGUAGE_SETTING, language);
		edit.apply();
	}

	/**
	 * 判断是否是APP语言
	 *
	 * @param context
	 * @param locale
	 * @return
	 */
	public static boolean isSimpleLanguage (Context context, Locale locale) {
		Locale appLocale = context.getResources().getConfiguration().locale;
		Log.e("Language Util =", "isSimpleLanguage: context lang = " + appLocale.getLanguage() + " locale =" + locale.getLanguage() );
		return appLocale.equals(locale);
	}

	/**
	 * 获取App当前语言
	 *
	 * @return
	 */
	public static String getAppLanguage () {
		Locale locale = DYTApplication.getInstance().getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		String country = locale.getCountry();
		StringBuilder stringBuilder = new StringBuilder();
		if (!TextUtils.isEmpty(language)) { //语言
			stringBuilder.append(language);
		}
		if (!TextUtils.isEmpty(country)) { //国家
			stringBuilder.append("-").append(country);
		}

		return stringBuilder.toString();
	}

	/**
	 * 是否是相同的locale
	 *
	 * @param l0
	 * @param l1
	 * @return
	 */
	private static boolean isSameLocale (Locale l0, Locale l1) {
		return equals(l1.getLanguage(), l0.getLanguage()) && equals(l1.getCountry(), l0.getCountry());
	}

	/**
	 * Return whether string1 is equals to string2.
	 *
	 * @param s1 The first string.
	 * @param s2 The second string.
	 * @return {@code true}: yes<br>{@code false}: no
	 */
	public static boolean equals (final CharSequence s1, final CharSequence s2) {
		if (s1 == s2)
			return true;
		int length;
		if (s1 != null && s2 != null && (length = s1.length()) == s2.length()) {
			if (s1 instanceof String && s2 instanceof String) {
				return s1.equals(s2);
			} else {
				for (int i = 0; i < length; i++) {
					if (s1.charAt(i) != s2.charAt(i))
						return false;
				}
				return true;
			}
		}
		return false;
	}

}
