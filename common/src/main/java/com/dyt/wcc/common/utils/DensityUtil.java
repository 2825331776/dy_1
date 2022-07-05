package com.dyt.wcc.common.utils;

import android.content.Context;
import android.util.TypedValue;

public class DensityUtil {
	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	//我们主要用到这个函数，将像素单位转换为dp单位，这样我们的界面在不同机型上的表现是差不多的
	public static int dp2px (Context context, float dpValue) {
		//        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public static int px2dp (Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	/**
	 * @param context
	 * @param spVal
	 * @return
	 */
	public static int sp2px (Context context, float spVal) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spVal, context.getResources().getDisplayMetrics());
	}

}
