package com.dyt.wcc.dytpir.utils;

import android.util.Log;

import com.dyt.wcc.dytpir.constans.DYConstants;
import com.infisense.iruvc.sdkisp.Libirtemp;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/4/13  17:55     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.utils     </p>
 */
public class TinyCUtils {
	/**
	 * 获取 TinyC 等效大气透过率 ,需要在非UI线程调用
	 * @param at 环境温度
	 * @param hmi  大气湿度
	 * @param distance  距离
	 * @return 大气透过率 的值
	 */
	public static float getLUT(float at, float hmi , float distance, char [] tau_data , char [] returnData){
		float reValue = 0.0f;
		Log.e("===LUT===", "===before===getLUT: hmi =" + hmi + " at =" + at + " distance =" + distance);
		//判断传入的 大气温度 是否符合要求

		if (at < DYConstants.TINYC_AIR_TEMP_POINT[0]){
			at = DYConstants.TINYC_AIR_TEMP_POINT[0];
		}else if (at > DYConstants.TINYC_AIR_TEMP_POINT[DYConstants.TINYC_AIR_TEMP_POINT.length -1]){
			at =  DYConstants.TINYC_AIR_TEMP_POINT[DYConstants.TINYC_AIR_TEMP_POINT.length -1];
		}else {
			for (int i = 0 ; i < (DYConstants.TINYC_AIR_TEMP_POINT.length); i++){
				if (at <= DYConstants.TINYC_AIR_TEMP_POINT[i]){
					at = DYConstants.TINYC_AIR_TEMP_POINT[i];
					break;
				}
			}
		}
			//判断传入的 大气湿度 是否符合要求
		if (hmi < DYConstants.TINYC_HUMIDITY_LEVEL[0]){
			hmi = DYConstants.TINYC_HUMIDITY_LEVEL[0];
		}else if (hmi > DYConstants.TINYC_HUMIDITY_LEVEL[DYConstants.TINYC_HUMIDITY_LEVEL.length -1]){
			hmi =  DYConstants.TINYC_HUMIDITY_LEVEL[DYConstants.TINYC_HUMIDITY_LEVEL.length -1];
		}else {
			for (int i = 0 ; i < (DYConstants.TINYC_HUMIDITY_LEVEL.length); i++){
				if (hmi <= DYConstants.TINYC_HUMIDITY_LEVEL[i]){
					hmi = DYConstants.TINYC_HUMIDITY_LEVEL[i];
					break;
				}
			}
		}
			//判断传入的 距离 是否符合要求
		if (distance < DYConstants.TINYC_DISTANCE[0]){
			distance = DYConstants.TINYC_DISTANCE[0];
		}else if (distance > DYConstants.TINYC_DISTANCE[DYConstants.TINYC_DISTANCE.length -1]){
			distance =  DYConstants.TINYC_DISTANCE[DYConstants.TINYC_DISTANCE.length -1];
		}else {
			for (int i = 0 ; i < (DYConstants.TINYC_DISTANCE.length); i++){
				if (distance <= DYConstants.TINYC_DISTANCE[i]){
					distance = DYConstants.TINYC_DISTANCE[i];
					break;
				}
			}
		}
		Log.e("===LUT===", "======getLUT: hmi =" + hmi + " at =" + at + " distance =" + distance);
		int status = Libirtemp.read_tau(tau_data,hmi,at,distance,returnData);
		Log.e("===LUT===", "getLUT: ===" + (int)returnData[0] + " == status == " + status);
		reValue = (float) returnData[0];
		return reValue;
	}
}
