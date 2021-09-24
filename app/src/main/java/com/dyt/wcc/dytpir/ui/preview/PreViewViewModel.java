package com.dyt.wcc.dytpir.ui.preview;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dyt.wcc.libuvccamera.usb.USBMonitor;

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
	private MutableLiveData<USBMonitor> mUsbMonitor ;

	private final boolean isDebug = true;
	private final static String TAG = "PreViewViewModel";



	public PreViewViewModel (@NonNull Application application) {
		super(application);
		mContext = new WeakReference<>(application);
		mUsbMonitor = new MutableLiveData<>();

		deviceConnectListener = new MutableLiveData<>();
		deviceConnectListener.setValue(new USBMonitor.OnDeviceConnectListener() {
			@Override
			public void onAttach (UsbDevice device) {
				if (isDebug)Log.e(TAG, "onAttach: "+ device.toString());
				if (device.getProductId() == 1 && device.getVendorId() == 5396) {
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (isDebug)Log.e(TAG, "检测到设备========");
							mUsbMonitor.getValue().requestPermission(device);
						}
					}, 100);
				}
			}
			@Override
			public void onConnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
				//			if (isDebug)Log.e(TAG, "onConnect: ");
				if (isDebug)Toast.makeText(mContext.get(),"onConnect========",Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onDetach (UsbDevice device) {
				if (isDebug)Log.e(TAG, "onDetach: ");
			}
			@Override
			public void onDisconnect (UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
				if (isDebug)Log.e(TAG, "onDisconnect: ");
			}
			@Override
			public void onCancel (UsbDevice device) {
				if (isDebug)Log.e(TAG, "onCancel: ");
			}
		});
		mUsbMonitor.setValue(new USBMonitor(application.getApplicationContext(),deviceConnectListener.getValue()));
	}

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
	//点击切换色板弹窗按钮
	public void showPaletteWindow(View view){
		Toast.makeText(mContext.get(),"1111",Toast.LENGTH_SHORT).show();
	}

}
