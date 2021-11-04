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

	public static final String palette_number_info = "palette_number";//画板设置
	public static final String language_setting = "language_setting";//语言设置
	public static final String tempUnit_setting = "tempUnit";//温度单位


}
