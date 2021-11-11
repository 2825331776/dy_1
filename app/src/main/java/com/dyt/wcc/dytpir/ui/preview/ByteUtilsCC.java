package com.dyt.wcc.dytpir.ui.preview;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * com.wcc.common.UtillsCC
 * 创 建 者：stefa
 * 创建时间：2021/7/9  14:21
 * 描   述：
 */
public class ByteUtilsCC {

	/**
	 * @param data byte数据源。1 byte = 8 bits   eg:0000 0000
	 * @return
	 */
	public static Map<String,String> byte2String(Map<String,Float> data){
		Map<String,String> result =  new HashMap<>();
		if (data != null ){
			result.put("strFix",String.valueOf(data.get("Fix")));//修正
			result.put("strReflectTemp",String.valueOf(data.get("Refltmp")));//反射温度
			result.put("strAirTemp",String.valueOf(data.get("Airtmp")));//环境温度
			result.put("strHumidity",String.valueOf(data.get("humi")));//湿度
			result.put("strEmissivity",String.valueOf(data.get("emiss")));//发射率
			result.put("strDistance",String.valueOf(data.get("distance")));//距离
		}
		return result;
	}

	/**
	 *
	 * @param data
	 * @return
	 */
	public static Map<String,Float> byte2Float(byte [] data){
		Map<String , Float> result = new HashMap<>();

		result.put("Fix",get4Byte2Float(data,0));
		result.put("Refltmp",get4Byte2Float(data,4));
		result.put("Airtmp",get4Byte2Float(data,8));
		result.put("humi",get4Byte2Float(data,12));
		result.put("emiss",get4Byte2Float(data,16));
		result.put("distance",get2Byte2Short(data,20));//getShort   共计 拿了byte的 前 0-21
		Log.e("===Map=======",result.toString());
		return result;
	}

	/**
	 * 通过byte数组取得float
	 * @param b 数据源
	 * @param index 开始的下标
	 * @return 返回index 后面3个byte组成的 float值。index3 index2 index1 index0 高位是index3 低位是index0
	 */
	public static float get4Byte2Float(byte[] b, int index) {
		int l;
		l = b[index + 0];
		l &= 0xff; //0xff = 255 = 15* 16+ 15   :此处的含义是取 int l（32位2进制） 取其后八位  d的八位
		l |= ((long) b[index + 1] << 8); //把这个index+1 填充到 C的八位
		l &= 0xffff;
		l |= ((long) b[index + 2] << 16);//   b 的八位
		l &= 0xffffff;
		l |= ((long) b[index + 3] << 24);// a的八位
		return Float.intBitsToFloat(l);
	}

	/**
	 * 通过byte数组取到short
	 * @param b
	 * @param index 第几位开始取2个byte
	 * @return
	 */
	public static float get2Byte2Short(byte[] b, int index) {
		return (float) (((b[index + 1] << 8) | b[index + 0] & 0xff));
	}


	/**
	 * float转换byte
	 * @param bb
	 * @param x
	 * @param index
	 */
	public static void putFloat(byte[] bb, float x, int index) {
		// byte[] b = new byte[4];
		int l = Float.floatToIntBits(x);//float转成int
		for (int i = 0; i < 4; i++) {//每次取出 int l 的后八位bits ，放进byte bb[]数组中
			bb[index + i] = new Integer(l).byteValue();
			l = l >> 8;//
		}
	}
	/**
	 * 转换int为byte数组
	 * @param bb
	 * @param x
	 * @param index
	 */
	public static void putInt(byte[] bb, int x, int index) {
		bb[index + 3] = (byte) (x >> 24);
		bb[index + 2] = (byte) (x >> 16);
		bb[index + 1] = (byte) (x >> 8);
		bb[index + 0] = (byte) (x >> 0);
	}
}
