package com.dyt.wcc.dytpir.constans;

import android.os.Environment;

import java.io.File;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/18  15:13     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.constans     </p>
 */
public class DYConstants {

	public static final int IMAGE_READY = 100;

	public static final String PIC_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
			+ File.separator+"DYTCamera";
}
