package com.serenegiant.usb;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/6/27  10:11     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.cameracommon.usbcameracommon     </p>
 */
public interface IUVCStatusCallBack {
	/**
	 * 回调当前的UVC的状态。
	 * @param uvcStatus 当前连接状态。
	 */
	void onUVCCurrentStatus(int uvcStatus);
}
