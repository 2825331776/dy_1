package com.dyt.wcc.common.utils;

import com.dyt.wcc.common.base.BaseConstants;

import java.text.DecimalFormat;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/12/27  15:42     </p>
 * <p>Description：温度转换工具类  ，摄氏度  华氏度  开氏度 之间的转换        </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.utils     </p>
 */
public class TempConvertUtils {

	/**
	 * @param temp 温度数值，（摄氏度，华氏度，开氏度）数值
	 * @param mode //代表输入数值为什么类型，0：摄氏度 ，1：华氏度，2：开氏度
	 * @return 摄氏度
	 */
	public static float temp2Celsius (float temp, int mode) {
		float result = 0.0f;//centigrade
		if (Float.isNaN(temp) || temp > BaseConstants.BASE_MAX_TEMP || temp < BaseConstants.BASE_MIN_TEMP) {
			return Float.NaN;
		}
		switch (mode) {
			case 0:
				result = temp;
				break;
			case 1:
				result = (temp - 32) / 1.8f;
				break;
			case 2:
				result = (temp - 273.15f);
				break;
		}
		return result;
	}

	/**
	 * 摄氏度 转换成 对应的温度
	 *
	 * @param temp 温度数值,摄氏度数值
	 * @param mode //代表转换到什么类型，0：摄氏度 ，1：华氏度，2：开氏度
	 * @return 0：摄氏度 ，1：华氏度，2：开氏度  摄氏度转换后的数值
	 */
	public static float Celsius2Temp (float temp, int mode) {
		float result = 0.0f;//centigrade
		switch (mode) {
			case 0:
				result = temp;
				break;
			case 1:
				result = (temp * 1.8f + 32);
				break;
			case 2:
				result = (temp + 273.15f);
				break;
		}
		if (result > BaseConstants.BASE_MAX_TEMP || result < BaseConstants.BASE_MIN_TEMP) {
			return Float.NaN;
		} else {
			return result;
		}

	}

	/**
	 * 摄氏度，转换成 软件设定的 显示温度String
	 *
	 * @param centigradeTemp 摄氏度
	 * @param mode           软件设置的 温度模式
	 * @return String 带后缀的 温度值
	 */
	public static String centigrade2TempString (float centigradeTemp, int mode) {
		String result = "";
		float relativeTemp = Celsius2Temp(centigradeTemp, mode);
		switch (mode) {
			case BaseConstants.CelsiusIndex:
				result = relativeTemp + BaseConstants.CelsiusSuffix;
				break;
			case BaseConstants.FahrenheitIndex:
				result = relativeTemp + BaseConstants.FahrenheitSuffix;
				break;
			case BaseConstants.KelvinIndex:
				result = relativeTemp + BaseConstants.KelvinSuffix;
				break;
		}
		return result;
	}


	/**
	 * 格式化 float 温度数值
	 *
	 * @param value 格式化的数值
	 * @return 返回格式化后的 float 数值
	 */
	private static float getFormatFloat (float value) {
		DecimalFormat df = new DecimalFormat("0.0");
		if (value < Float.MAX_VALUE || value > Float.MIN_VALUE) {
			return Float.parseFloat(df.format(value));
		} else {
			return Float.NaN;
		}
	}


}
