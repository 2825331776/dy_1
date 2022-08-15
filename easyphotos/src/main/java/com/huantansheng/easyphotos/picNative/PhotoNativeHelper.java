package com.huantansheng.easyphotos.picNative;

import android.os.Handler;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/8/9  13:49     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.huantansheng.easyphotos.picNative     </p>
 */
public class PhotoNativeHelper extends Handler {
	static {
		System.loadLibrary("PhotoNativeHelper");
	}

	public int verifyFilePath (String filePath) {
		return nativeVerifyFilePath(filePath);
	}

	public byte[] getByteArrayByFilePath (String filePath) {
		return nativeGetByteArrayByFilePath(filePath);
	}


	// 返回一个校验值，正确 则继续下一步操作
	private static final native int nativeVerifyFilePath (String filePath);

	//
	private static final native byte[] nativeGetByteArrayByFilePath (String filePath);

}
