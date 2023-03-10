/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.dytest.wcc.cameracommon.usbcameracommon;

import android.app.Activity;

import com.dytest.wcc.cameracommon.widget.UVCCameraTextureView;
import com.dytest.wcc.common.widget.dragView.MeasureTempContainerView;
import com.serenegiant.usb.ITemperatureCallback;
import com.serenegiant.usb.UVCCamera;


public class UVCCameraHandler extends AbstractUVCCameraHandler {
	/**
	 * UVC连接的状态。
	 */
	public static final  int              UVC_STATUS_PREVIEWING = 0;
	public static final  int              UVC_STATUS_SN_ERROR   = 1;
	private static final String           TAG                   = "UVCCameraHandler";
	private static       UVCCameraHandler mUVCCameraHandler;
//	/**
//	 * create UVCCameraHandler, use MediaVideoEncoder, try MJPEG, default bandwidth
//	 * @param parent
//	 * @param cameraView
//	 * @param width
//	 * @param height
//	 * @return
//	 */
	/*
	public static final UVCCameraHandler createHandler(
			final Activity parent, final UVCCameraTextureView cameraView,
			final int width, final int height) {

		return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH,null,0);
	}
	 */

//	/**
//	 * create UVCCameraHandler, use MediaVideoEncoder, try MJPEG
//	 * @param parent
//	 * @param cameraView
//	 * @param width
//	 * @param height
//	 * @param bandwidthFactor
//	 * @return
//	 */
	/*
	public static final UVCCameraHandler createHandler(
			final Activity parent, final UVCCameraTextureView cameraView,
			final int width, final int height, final float bandwidthFactor) {

		return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG,null,0);
	}
	 */

//	/**
//	 * create UVCCameraHandler, try MJPEG, default bandwidth
//	 *
//	 * @param parent
//	 * @param cameraView
//	 * @param encoderType 0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
//	 * @param width
//	 * @param height
//	 * @return
//	 */
	/*
	public static final UVCCameraHandler createHandler(
            final Activity parent, final UVCCameraTextureView cameraView,
            final int encoderType, final int width, final int height, ITemperatureCallback temperatureCallback) {

		return createHandler(parent, cameraView, encoderType, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH,null,0);
	}

	 */
	protected UVCCameraHandler (final CameraThread thread) {
		//调用父类的构造函数。仔细了解 constructor 类
		super(thread);
	}

	/**
	 * create UVCCameraHandler, default bandwidth默认带宽
	 *
	 * @param parent
	 * @param cameraView
	 * @param encoderType         0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
	 * @param width
	 * @param height
	 * @param format              either UVCCamera.FRAME_FORMAT_YUYV(0) or UVCCamera.FRAME_FORMAT_MJPEG(1)
	 * @param temperatureCallback 温度回调函数
	 * @param androidVersion      用户初始化Camera
	 * @return
	 */
	public static final UVCCameraHandler createHandler (final Activity parent, final UVCCameraTextureView cameraView, final int encoderType, final int width, final int height, final int format, ITemperatureCallback temperatureCallback, final MeasureTempContainerView containerView, int androidVersion) {
		return createHandler(parent, cameraView, encoderType, width, height, format, UVCCamera.DEFAULT_BANDWIDTH, temperatureCallback, containerView, androidVersion);
	}

	/**
	 * create UVCCameraHandler
	 *
	 * @param parent              显示的窗体的对象
	 * @param cameraView          显示图像控件
	 * @param encoderType         0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
	 * @param width               宽度
	 * @param height              高度
	 * @param format              either UVCCamera.FRAME_FORMAT_YUYV(0) or UVCCamera.FRAME_FORMAT_MJPEG(1)
	 * @param bandwidthFactor     带宽
	 * @param temperatureCallback 温度回调函数
	 * @param androidVersion      最终用于初始化Camera
	 * @return
	 */
	public static final UVCCameraHandler createHandler (final Activity parent, final UVCCameraTextureView cameraView, final int encoderType, final int width, final int height, final int format, final float bandwidthFactor, ITemperatureCallback temperatureCallback, final MeasureTempContainerView containerView, int androidVersion) {
		//		Log.e(TAG, "createHandler:  123 === " + System.currentTimeMillis());
		final CameraThread thread = new CameraThread(UVCCameraHandler.class, parent, cameraView, encoderType, width, height, format, bandwidthFactor, temperatureCallback, containerView, androidVersion);


		thread.start();
		mUVCCameraHandler = (UVCCameraHandler) thread.getHandler();
		return mUVCCameraHandler;
		//return (UVCCameraHandler)thread.getHandler();
	}

	public static synchronized UVCCameraHandler getInstance () {
		return mUVCCameraHandler;
	}

	@Override
	public void startPreview (final Object surface) {
		super.startPreview(surface);
	}

	@Override
	public void setRecordData (String externalPath) {
		super.setRecordData(externalPath);
	}

	@Override
	public void captureStill () {
		super.captureStill();
	}

	@Override
	public boolean captureStill (final String path) {
		return super.captureStill(path);
	}
}
