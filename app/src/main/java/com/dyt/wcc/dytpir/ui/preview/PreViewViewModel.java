package com.dyt.wcc.dytpir.ui.preview;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dyt.wcc.cameracommon.usbcameracommon.UVCCameraHandler;
import com.serenegiant.usb.USBMonitor;

import java.lang.ref.WeakReference;

/**
* <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
* <p>Author：stefan cheng    </p>
* <p>Create Date：2021/9/22  9:30 </p>
* <p>Description：@todo describe         </p>
* <p>PackagePath: com.dyt.wcc.dytpir.ui.preview     </p>
*/
public class PreViewViewModel extends AndroidViewModel {

	private WeakReference<Context> mContext;
	private MutableLiveData<Integer> connectState  ;//连接状态 0 未连接（销毁/释放）， 1 已连接， 2 暂停连接

	private MutableLiveData<USBMonitor.OnDeviceConnectListener> deviceConnectListener ;
	private MutableLiveData<USBMonitor>                         mUsbMonitor ;
	private MutableLiveData<UVCCameraHandler>                   mUvcCameraHandler ;
//	private MutableLiveData<UVCCameraTextureView> mUvcCameraTextureView;

	private final boolean isDebug = true;
	private final static String TAG = "PreViewViewModel";



	public PreViewViewModel (@NonNull Application application) {
		super(application);
		mContext = new WeakReference<>(application);
		mUsbMonitor = new MutableLiveData<>();

	}

	public void setDeviceConnectListener (USBMonitor.OnDeviceConnectListener deviceconnectlistener) {
		if (this.deviceConnectListener == null){this.deviceConnectListener = new MutableLiveData<>();}
		this.deviceConnectListener.setValue(deviceconnectlistener);;
		mUsbMonitor.setValue(new USBMonitor(mContext.get(),this.deviceConnectListener.getValue()));
	}

//	public void setMUvcCameraTextureView (MutableLiveData<UVCCameraTextureView> mUvcCameraTextureView) {
//		this.mUvcCameraTextureView = mUvcCameraTextureView;
//	}

	public MutableLiveData<USBMonitor> getMUsbMonitor () {
		return mUsbMonitor;
	}

	public LiveData<Integer> getConnectState () {
		if (connectState == null){
			connectState = new MutableLiveData<>();
			connectState.setValue(0);
		}
		return connectState;
	}
//	//点击切换色板弹窗按钮
//	public void showPaletteWindow(View view){
//		Toast.makeText(mContext.get(),"1111",Toast.LENGTH_SHORT).show();
//	}

}
