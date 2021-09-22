package com.dyt.wcc.common.base;

import android.app.Application;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/8  15:36     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.base     </p>
 */
public abstract class BaseApplication extends Application {
	public static String deviceName="";

	@Override
	public void onCreate () {
		super.onCreate();
	}
}
