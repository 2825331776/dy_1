package com.huantansheng.easyphotos.picNative;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/8/10  16:11     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.huantansheng.easyphotos.picNative     </p>
 */
public class PhotoHandler extends Handler {
	private PhotoNativeHelper nativeHelper;

	/**
	 * Default constructor associates this handler with the {@link Looper} for the
	 * current thread.
	 * <p>
	 * If this thread does not have a looper, this handler won't be able to receive messages
	 * so an exception is thrown.
	 *
	 * @deprecated Implicitly choosing a Looper during Handler construction can lead to bugs
	 * where operations are silently lost (if the Handler is not expecting new tasks and quits),
	 * crashes (if a handler is sometimes created on a thread without a Looper active), or race
	 * conditions, where the thread a handler is associated with is not what the author
	 * anticipated. Instead, use an {@link Executor} or specify the Looper
	 * explicitly, using {@link Looper#getMainLooper}, {link android.view.View#getHandler}, or
	 * similar. If the implicit thread local behavior is required for compatibility, use
	 * {@code new Handler(Looper.myLooper())} to make it clear to readers.
	 */
	public PhotoHandler (PhotoNativeHelper nativeHelper) {
		if (this.nativeHelper == null) {
			this.nativeHelper = nativeHelper;
		}
	}

	public static final int PHOTO_HANDLER_ERROR_PATH_SUFFIX      = -1;//.jpg后缀
	public static final int PHOTO_HANDLER_ERROR_PATH_DATA_FORMAT = -2;//数据格式

	public int verifyFilePath (String filePath) {
		if (filePath == null || filePath.isEmpty() || (!filePath.endsWith(".jpg"))) {
			return PHOTO_HANDLER_ERROR_PATH_SUFFIX;
		}
		int result = nativeHelper.verifyFilePath(filePath);
		if (result != 0) {//定义返回 0 为校验的正常结果
			return PHOTO_HANDLER_ERROR_PATH_DATA_FORMAT;
		} else {
			return result;
		}
	}

	public byte[] getByteArrayByFilePath (String filePath) {
		return nativeHelper.getByteArrayByFilePath(filePath);
	}

}
