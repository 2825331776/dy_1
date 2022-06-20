package com.dyt.wcc.dytpir.utils;

import android.util.Log;

import com.dyt.wcc.dytpir.constans.DYConstants;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * com.wcc.common.UtillsCC
 * 创 建 者：stefa
 * 创建时间：2021/7/9  14:21
 * 描   述：
 */
public class ByteUtilsCC {

	private static final String TAG = "ByteUtilsCC";

	public static final String FIX              = "Fix";//修正
	public static final String STR_FIX          = "strFix";
	public static final String REFLECT_TEMP     = "Refltmp";//反射温度
	public static final String STR_REFLECT_TEMP = "strReflectTemp";
	public static final String AIR_TEMP         = "Airtmp";//环境温度
	public static final String STR_AIR_TEMP     = "strAirTemp";
	public static final String HUMIDITY         = "humi";//湿度
	public static final String STR_HUMIDITY     = "strHumidity";
	public static final String EMISSIVITY       = "emiss";//发射率
	public static final String STR_EMISSIVITY   = "strEmissivity";
	public static final String DISTANCE         = "distance";//距离
	public static final String STR_DISTANCE     = "strDistance";

	/**
	 * @param data byte数据源。1 byte = 8 bits   eg:0000 0000
	 * @return
	 */
	public static Map<String, String> byte2String (Map<String, Float> data) {
		Map<String, String> result = new HashMap<>();
		if (data != null) {
			result.put(STR_FIX, String.valueOf(data.get(FIX)));//修正
			result.put(STR_REFLECT_TEMP, String.valueOf(data.get(REFLECT_TEMP)));//反射温度
			result.put(STR_AIR_TEMP, String.valueOf(data.get(AIR_TEMP)));//环境温度
			result.put(STR_HUMIDITY, String.valueOf(data.get(HUMIDITY)));//湿度
			result.put(STR_EMISSIVITY, String.valueOf(data.get(EMISSIVITY)));//发射率
			result.put(STR_DISTANCE, String.valueOf(data.get(DISTANCE)));//距离
		}
		return result;
	}

	/**
	 * S0机芯 解析温度数据。
	 *
	 * @param data
	 * @return
	 */
	public static Map<String, Float> byte2Float (byte[] data) {
		Map<String, Float> result = new HashMap<>();

		result.put(DYConstants.setting_correction, get4Byte2Float(data, 0));
		result.put(DYConstants.setting_reflect, get4Byte2Float(data, 4));
		result.put(DYConstants.setting_environment, get4Byte2Float(data, 8));
		result.put(DYConstants.setting_humidity, get4Byte2Float(data, 12));
		result.put(DYConstants.setting_emittance, get4Byte2Float(data, 16));
		result.put(DYConstants.setting_distance, get2Byte2Short(data, 20));//getShort   共计 拿了byte的 前 0-21
		//		Log.e("===Map=======",result.toString());
		return result;
	}

	/**
	 * tinyC JNI获取到Byte数组转成HashMap
	 *
	 * @param data 发射率 0-1      emittance  取值 ： （0 -1）
	 *             反射温度 2-3     reflect  取值 ： （-20 - 120）
	 *             环境温度 4-5     environment  取值 ： （-20 - 50）
	 *             湿度 6-7         humidity  取值 ： （0 - 100）
	 *             多余 8-9
	 * @return
	 */
	public static Map<String, Float> tinyCByte2HashMap (byte[] data) {
		Map<String, Float> result = new HashMap<>();
		short numberData = 0;
		float params = 0.0f;
		result.put(DYConstants.setting_correction, 0.0f);

		numberData = (short) (data[3] & 0xff);
		numberData |= (data[2] << 8 & 0xff00);
		params = numberData - 273.15f;
		DecimalFormat df = new DecimalFormat("##0.00");
		//		params = Math.round(params* 100f) / 100f ;
		params = Math.round(params);
		params = Float.parseFloat(df.format(params));
		//		Log.e(TAG, "反射温度: numberData " +numberData);
		//		Log.e(TAG, "反射温度: params " + params);
		result.put(DYConstants.setting_reflect, params);//反射温度 =》 反射温度

		numberData = (short) (data[5] & 0xff);
		numberData |= (data[4] << 8 & 0xff00);
		params = numberData - 273.15f;
		//		params = Math.round(params* 100f) / 100f ;
		params = Math.round(params);
		params = Float.parseFloat(df.format(params));
		//		Log.e(TAG, "环境温度: numberData " +numberData);
		//		Log.e(TAG, "环境温度: params " + params);
		result.put(DYConstants.setting_environment, params);//环境温度 =》 大气温度

		numberData = (short) (data[7] & 0xff);
		params = numberData / 128.0f;
		params = Math.round(params * 100f) / 100f;
		params = Float.parseFloat(df.format(params));
		//		Log.e(TAG, "湿度: numberData " +numberData);
		//		Log.e(TAG, "湿度: params " + params);
		result.put(DYConstants.setting_humidity, params);//湿度 =》 大气透过率

		numberData = (short) (data[1] & 0xff);
		params = numberData / 128.0f;
		params = Math.round(params * 100f) / 100f;
		params = Float.parseFloat(df.format(params));
		//		Log.e(TAG, "发射率: numberData " +numberData);
		//		Log.e(TAG, "发射率: params " + params);
		result.put(DYConstants.setting_emittance, params);//发射率 = 》 发射率

		result.put(DYConstants.setting_distance, 0.25f);//getShort   共计 拿了byte的 前 0-21
		Log.e("===Map=======", result.toString());
		return result;
	}

	/**
	 * 通过byte数组取得float
	 *
	 * @param b     数据源
	 * @param index 开始的下标
	 * @return 返回index 后面3个byte组成的 float值。index3 index2 index1 index0 高位是index3 低位是index0
	 */
	public static float get4Byte2Float (byte[] b, int index) {
		int l;
		l = b[index + 0];
		l &= 0xff; //0xff = 255 = 15* 16+ 15   :此处的含义是取 int l（32位2进制） 取其后八位  b的八位
		l |= ((long) b[index + 1] << 8); //把这个index+1 填充到 C的八位
		l &= 0xffff;
		l |= ((long) b[index + 2] << 16);//   b 的八位
		l &= 0xffffff;
		l |= ((long) b[index + 3] << 24);// a的八位
		return Float.intBitsToFloat(l);
	}

	/**
	 * 通过byte数组取到short
	 *
	 * @param b
	 * @param index 第几位开始取2个byte
	 * @return
	 */
	public static float get2Byte2Short (byte[] b, int index) {
		return (float) (((b[index + 1] << 8) | b[index + 0] & 0xff));
	}


	/**
	 * float转换byte
	 *
	 * @param bb
	 * @param x
	 * @param index
	 */
	public static void putFloat (byte[] bb, float x, int index) {
		// byte[] b = new byte[4];
		int l = Float.floatToIntBits(x);//float转成int
		for (int i = 0; i < 4; i++) {//每次取出 int l 的后八位bits ，放进byte bb[]数组中
			bb[index + i] = new Integer(l).byteValue();
			l = l >> 8;//
		}
	}

	/**
	 * 转换int为byte数组
	 *
	 * @param bb
	 * @param x
	 * @param index
	 */
	public static void putInt (byte[] bb, int x, int index) {
		bb[index + 3] = (byte) (x >> 24);
		bb[index + 2] = (byte) (x >> 16);
		bb[index + 1] = (byte) (x >> 8);
		bb[index + 0] = (byte) (x >> 0);
	}
}
