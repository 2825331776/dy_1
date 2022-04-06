package com.dyt.wcc.dytpir.constans;

import android.os.Environment;

import java.io.File;

/**
* <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
* <p>Author：stefan cheng    </p>
* <p>Create Date：2021/9/26  16:04 </p>
* <p>Description：@todo describe         </p>
* <p>PackagePath: com.dyt.wcc.dytpir.constans     </p>
*/
public class DYConstants {

	public static final int IMAGE_READY = 100;

	public static final String PIC_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
			+ File.separator+"DYTCamera";

	public static final String [] languageArray = new String []{"中文", "English"};
	public static final String [] tempUnit = new String[]{"℃","℉","K"};
	//设置界面六个设置的值：保存在本地的sp  当中的都是 摄氏度
	//发射率
	public static final String setting_emittance = "setting_emittance";
	public static final int SETTING_EMITTANCE_INT = 4 * 4;
	public static final float SETTING_EMITTANCE_DEFAULT_VALUE = 0.98f;
	//距离
	public static final String setting_distance = "setting_distance";
	public static final int SETTING_DISTANCE_INT = 5 * 4;
	public static final float SETTING_DISTANCE_DEFAULT_VALUE = 0.0f;
	//湿度
	public static final String setting_humidity = "setting_humidity";
	public static final int SETTING_HUMIDITY_INT = 3 * 4;
	public static final float SETTING_HUMIDITY_DEFAULT_VALUE = 0.5f;
	//修正
	public static final String setting_correction = "setting_correction";
	public static final int SETTING_CORRECTION_INT = 0 * 4;
	public static final float SETTING_CORRECTION_DEFAULT_VALUE = 0.0f;
	//反射温度
	public static final String setting_reflect = "setting_reflect";
	public static final int SETTING_REFLECT_INT = 1 * 4;
	public static final int SETTING_REFLECT_DEFAULT_VALUE = 25;
	//环境温度
	public static final String setting_environment = "setting_environment";
	public static final int SETTING_ENVIRONMENT_INT = 2 * 4;
	public static final int SETTING_ENVIRONMENT_DEFAULT_VALUE = 25;

	public static final int CAMERA_DATA_MODE_8004 = 0x8004;
	public static final int CAMERA_DATA_MODE_8005 = 0x8005;

//	public static final String HighLowTemp_Toggle_info = "highLowTemp_toggle";//高低温追踪 开关
//	public static final String areaCheck_Toggle_info = "areaCheck_toggle";//框内细查 开关

	public static final String PALETTE_NUMBER = "palette_number";//画板设置
	public static final String LANGUAGE_SETTING = "language_setting";//语言设置
	public static final String TEMP_UNIT_SETTING = "temp_unit";//温度单位

	public static final String LOCALE_LANGUAGE = "locale_language";
	public static final String SPTAG = "Main";

	public static final String SP_NAME = "DYT_IR_SP";
	public static final String RECORD_AUDIO_SETTING = "record_audio_setting";
	public static final String FIRST_RUN = "first_run";//是否第一次打开应用， 第一打开时，int 值为 0.以后为1

	public static final String [] paletteArrays = new String[]{"1.dat","2.dat","3.dat","4.dat","5.dat","6.dat"};



}
