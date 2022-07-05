package com.dyt.wcc.common.utils;

/**
 * <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
 * <p>Author：stefan cheng    </p>
 * <p>Create Date：2021/9/26  11:49 </p>
 * <p>Description：@todo describe         </p>
 * <p>PackagePath: com.dyt.wcc.common.utils     </p>
 */
public class FontUtils {

	public static int adjustFontSize (int screenWidth, int screenHeight) {
		if (screenWidth <= 240) {        // 240X320 屏幕
			return 10;
		} else if (screenWidth <= 320) {    // 320X480 屏幕
			return 14;
		} else if (screenWidth <= 480) {    // 480X800 或 480X854 屏幕
			return 16;
		} else if (screenWidth <= 540) {    // 540X960 屏幕
			return 20;
		} else if (screenWidth <= 800) {    // 800X1280 屏幕
			return 24;
		} else if (screenWidth <= 1024) {    // 大于 800X1280
			return 28;
		} else if (screenWidth <= 1280) { // 大于 1280的宽
			return 44;
		} else {
			return 50;
		}
	}
}
