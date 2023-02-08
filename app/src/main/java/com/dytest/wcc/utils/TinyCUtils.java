package com.dytest.wcc.utils;

import android.util.Log;

import com.dytest.wcc.constans.DYConstants;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/4/13  17:55     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.utils     </p>
 */
public class TinyCUtils {

	public static char[] toChar (byte[] b) {
		if (b == null) {
			return null;
		} else {
			char[] returnData = new char[b.length / 2];
			int i = 0;
			for (int var3 = 0; i < b.length; returnData[var3++] = (char) ((b[i++] & 255) + ((b[i++] & 255) << 8))) {
			}
			return returnData;
		}
	}

	/**
	 * byte 数组 转换成 short 数组
	 *
	 * @param input 输入的 byte数组
	 * @return 如果输入为null，则返回null；否则返回 转换之后的 short 数组（第二位为高位，此为小端方式 转换的）
	 */
	public static short[] byte2Short (byte[] input) {
		if (input == null) {
			return null;
		} else {
			short[] returnData = new short[input.length / 2];
			int i = 0;
			for (int j = 0; i < input.length; ) {
				returnData[j++] = (short) ((input[i++] & 0xff) + ((input[i++] & 0xff) << 8));
			}
			return returnData;
		}
	}

	/**
	 * 获取 TinyC 等效大气透过率 ,需要在非UI线程调用
	 *
	 * @param at       环境温度
	 * @param hmi      大气湿度
	 * @param distance 距离
	 * @return 大气透过率 的值
	 */
	public static short getLUT (float at, float hmi, float distance, short[] tau_data) {
		//		float reValue = 0.0f;
		Log.e("===LUT===", "===before===getLUT: hmi =" + hmi + " at =" + at + " distance =" + distance);
		//判断传入的 大气温度 是否符合要求
		int ATPosition = 0, HMIPosition = 0, DISTANCEPosition = 0;
		if (at < DYConstants.TINYC_AIR_TEMP_POINT[0]) {
			at = DYConstants.TINYC_AIR_TEMP_POINT[0];
			ATPosition = 0;
		} else if (at > DYConstants.TINYC_AIR_TEMP_POINT[DYConstants.TINYC_AIR_TEMP_POINT.length - 1]) {
			at = DYConstants.TINYC_AIR_TEMP_POINT[DYConstants.TINYC_AIR_TEMP_POINT.length - 1];
			ATPosition = DYConstants.TINYC_AIR_TEMP_POINT.length - 1;
		} else {
			for (int i = 0; i < (DYConstants.TINYC_AIR_TEMP_POINT.length); i++) {
				if (at <= DYConstants.TINYC_AIR_TEMP_POINT[i]) {
					ATPosition = i;
					at = DYConstants.TINYC_AIR_TEMP_POINT[i];
					break;
				}
			}
		}
		//判断传入的 大气湿度 是否符合要求
		if (hmi < DYConstants.TINYC_HUMIDITY_LEVEL[0]) {
			hmi = DYConstants.TINYC_HUMIDITY_LEVEL[0];
			HMIPosition = 0;
		} else if (hmi > DYConstants.TINYC_HUMIDITY_LEVEL[DYConstants.TINYC_HUMIDITY_LEVEL.length - 1]) {
			hmi = DYConstants.TINYC_HUMIDITY_LEVEL[DYConstants.TINYC_HUMIDITY_LEVEL.length - 1];
			HMIPosition = DYConstants.TINYC_HUMIDITY_LEVEL.length - 1;
		} else {
			for (int i = 0; i < (DYConstants.TINYC_HUMIDITY_LEVEL.length); i++) {
				if (hmi <= DYConstants.TINYC_HUMIDITY_LEVEL[i]) {
					HMIPosition = i;
					hmi = DYConstants.TINYC_HUMIDITY_LEVEL[i];
					break;
				}
			}
		}
		//判断传入的 距离 是否符合要求
		if (distance < DYConstants.TINYC_DISTANCE[0]) {
			distance = DYConstants.TINYC_DISTANCE[0];
			DISTANCEPosition = 0;
		} else if (distance > DYConstants.TINYC_DISTANCE[DYConstants.TINYC_DISTANCE.length - 1]) {
			distance = DYConstants.TINYC_DISTANCE[DYConstants.TINYC_DISTANCE.length - 1];
			DISTANCEPosition = DYConstants.TINYC_DISTANCE.length - 1;
		} else {
			for (int i = 0; i < (DYConstants.TINYC_DISTANCE.length); i++) {
				if (distance <= DYConstants.TINYC_DISTANCE[i]) {
					DISTANCEPosition = i;
					distance = DYConstants.TINYC_DISTANCE[i];
					break;
				}
			}
		}
		Log.e("===LUT===", "======getLUT: hmi =" + hmi + " at =" + at + " distance =" + distance);
		Log.e("===LUT===", "======getLUT: HMIPosition =" + HMIPosition + " ATPosition =" + ATPosition + " DISTANCEPosition =" + DISTANCEPosition);
		int index = HMIPosition * 64 * 14 + ATPosition * 64 + DISTANCEPosition;

		Log.e("===LUT===", "getLUT:========== " + index);
		if (index >= tau_data.length) {
			return -1;
		} else {
			return (tau_data[index]);
		}
		//				int status = Libirtemp.read_tau(tau_data, hmi, at, distance, returnData);
		//		Log.e("===LUT===", "getLUT: ===" + (int) returnData[0] + " == status == " + status);
		//		reValue = (float) returnData[0];

	}
}
