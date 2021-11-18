package com.dyt.wcc.dytpir.constans;

import android.os.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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


	public static final List<String> languageArray = new CopyOnWriteArrayList<>(Arrays.asList("中文", "英文"));


	public static final int CAMERA_DATA_MODE_8004 = 0x8004;
	public static final int CAMERA_DATA_MODE_8005 = 0x8005;

	public static final String HighLowTemp_Toggle_info = "highLowTemp_toggle";//高低温追踪 开关
	public static final String areaCheck_Toggle_info = "areaCheck_toggle";//框内细查 开关

	public static final String PALETTE_NUMBER = "palette_number";//画板设置
	public static final String LANGUAGE_SETTING = "language_setting";//语言设置
	public static final String TEMP_UNIT_SETTING = "temp_unit";//温度单位

	public static final String LOCALE_LANGUAGE = "locale_language";
	public static final String SPTAG = "Main";

	public static final String SP_NAME = "DYT_IR_SP";


}
