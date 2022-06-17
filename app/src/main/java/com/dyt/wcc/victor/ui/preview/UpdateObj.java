package com.dyt.wcc.victor.ui.preview;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/4/12  11:14     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.preview     </p>
 */
public class UpdateObj {
	private int    appVersionCode;
	private String appVersionName;
	private String describe;
	//不带有.apk 后缀的
	private String packageName;
	//全称路径
	private String toSavePath;

	public int getAppVersionCode () {
		return appVersionCode;
	}

	public void setAppVersionCode (int appVersionCode) {
		this.appVersionCode = appVersionCode;
	}

	public String getAppVersionName () {
		return appVersionName;
	}

	public void setAppVersionName (String appVersionName) {
		this.appVersionName = appVersionName;
	}

	public String getDescribe () {
		return describe;
	}

	public void setDescribe (String describe) {
		this.describe = describe;
	}

	public String getPackageName () {
		return packageName;
	}

	public void setPackageName (String packageName) {
		this.packageName = packageName;
	}

	public String getToSavePath () {
		return toSavePath;
	}

	public void setToSavePath (String toSavePath) {
		this.toSavePath = toSavePath;
	}
}
