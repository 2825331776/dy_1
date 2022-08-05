package com.dyt.wcc.constans;

import android.os.Environment;

import com.dyt.wcc.BuildConfig;

import java.io.File;

/**
 * <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
 * <p>Author：stefan cheng    </p>
 * <p>Create Date：2021/9/26  16:04 </p>
 * <p>Description：@todo describe         </p>
 * <p>PackagePath: com.dyt.wcc.constans     </p>
 */
public class DYConstants {

	public static final int IMAGE_READY     = 100;
	//校正 最小值  最大值 （在摄氏度）
	public static final int REVISE_MIN      = -20;
	public static final int REVISE_MAX      = 20;
	//环境 反射温度 的最小值  最大值 （在摄氏度）
	public static final int REFLECT_MIN     = -20;
	public static final int REFLECT_MAX     = 120;
	public static final int ENVIRONMENT_MIN = -20;
	public static final int ENVIRONMENT_MAX = 50;

	public static final String COMPANY_DYT     = "dyt";//点扬
	public static final String COMPANY_NEUTRAL = "neutral";//中性版
	public static final String COMPANY_VICTOR  = "victor";//胜利
	public static final String COMPANY_JMS     = "jms";//精明鼠
	public static final String COMPANY_QIANLI  = "qianli";//潜力
	public static final String COMPANY_TESLONG = "teslong";//泰视朗
	public static final String COMPANY_MAILSEEY = "mileseey";//迈测

	public static final String PIC_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + "DYTCamera";

	//	public static final String[] languageArray                     = new String[]{"中文", "English"};
	public static final String[] tempUnit                          = new String[]{"℃", "℉", "K"};
	//设置界面六个设置的值：保存在本地的sp  当中的都是 摄氏度
	//发射率
	public static final String   setting_emittance                 = "setting_emittance";
	public static final int      SETTING_EMITTANCE_INT             = 4 * 4;
	public static final float    SETTING_EMITTANCE_DEFAULT_VALUE   = 1.00f;
	//距离
	public static final String   setting_distance                  = "setting_distance";
	public static final int      SETTING_DISTANCE_INT              = 5 * 4;
	public static final float    SETTING_DISTANCE_DEFAULT_VALUE    = 0.0f;
	//湿度
	public static final String   setting_humidity                  = "setting_humidity";
	public static final int      SETTING_HUMIDITY_INT              = 3 * 4;
	public static final float    SETTING_HUMIDITY_DEFAULT_VALUE    = 0.5f;
	//修正
	public static final String   setting_correction                = "setting_correction";
	public static final int      SETTING_CORRECTION_INT            = 0 * 4;
	public static final float    SETTING_CORRECTION_DEFAULT_VALUE  = 0.0f;
	//反射温度
	public static final String   setting_reflect                   = "setting_reflect";
	public static final int      SETTING_REFLECT_INT               = 1 * 4;
	public static final int      SETTING_REFLECT_DEFAULT_VALUE     = 27;
	//环境温度
	public static final String   setting_environment               = "setting_environment";
	public static final int      SETTING_ENVIRONMENT_INT           = 2 * 4;
	public static final int      SETTING_ENVIRONMENT_DEFAULT_VALUE = 27;

	public static final int CAMERA_DATA_MODE_8004 = 0x8004;//数据流切换 0x8004
	public static final int CAMERA_DATA_MODE_8005 = 0x8005;//数据流切换 0x8005

	public static final String PALETTE_NUMBER         = "palette_number";//画板设置
	public static final String LANGUAGE_SETTING       = "language_setting";//语言设置
	public static final String LANGUAGE_SETTING_INDEX = "language_setting_index";//语言设置的下标
	public static final String TEMP_UNIT_SETTING      = "temp_unit";//温度单位

	//	public static final String SP_NAME              = "DYT_IR_SP_DY";
	public static final String SP_NAME              = BuildConfig.SP_NAME;
	public static final String RECORD_AUDIO_SETTING = "record_audio_setting";
	public static final String GAIN_TOGGLE_SETTING  = "gain_toggle_setting";
	public static final String FIRST_RUN            = "first_run";//是否第一次打开应用， 第一打开时，int 值为 0.以后为1

	public static final String[] paletteArrays = new String[]{"1.dat", "2.dat", "3.dat", "4.dat", "5.dat", "6.dat"};

	//下载更新api
	public static final String UPDATE_CHECK_INFO    = "http://114.115.130.132:8080/dytfile/getVersion?objPath=files/Apks/NF/";
	public static final String UPDATE_DOWNLOAD_API  = "http://114.115.130.132:8080/dytfile/downloadGET?fileName=Apks/NF/";
	public static final String UPDATE_DOWNLOAD_BASE = "http://114.115.130.132:8080/dytfile/downloadGET?fileName=Apks/";

	// tinyc 机芯的 距离点64个， 大气温度 温度点14个 ，湿度 等级4个
	public static final float[] TINYC_DISTANCE       = {0.25f, 0.30f, 0.35f, 0.40f, 0.45f, 0.50f, 0.55f, 0.60f, 0.65f, 0.70f, 0.75f, 0.80f, 0.85f, 0.90f, 0.95f, 1.00f, 1.05f, 1.10f, 1.15f, 1.20f, 1.30f, 1.40f, 1.50f, 1.60f, 1.70f, 1.80f, 1.90f, 2.00f, 2.20f, 2.40f, 2.60f, 2.80f, 3.00f, 3.20f, 3.40f, 3.60f, 3.80f, 4.00f, 4.50f, 5.00f, 5.50f, 6.00f, 6.50f, 7.00f, 7.50f, 8.00f, 9.00f, 10.00f, 11.00f, 12.00f, 13.00f, 14.00f, 16.00f, 18.00f, 20.00f, 22.00f, 24.00f, 26.00f, 28.00f, 30.00f, 35.00f, 40.00f, 45.00f, 50.00f};
	public static final float[] TINYC_AIR_TEMP_POINT = {-5, 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 55};
	public static final float[] TINYC_HUMIDITY_LEVEL = {0, 1, 2, 3};
}
