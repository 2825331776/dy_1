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

package com.serenegiant.usb;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

public final class USBMonitor {

	private static final boolean DEBUG = true;	// TODO set false on production
	private static final String TAG = "USBMonitor";

	private static final String ACTION_USB_PERMISSION_BASE = "com.dyt.wcc.dytpir.";
	private final String ACTION_USB_PERMISSION = ACTION_USB_PERMISSION_BASE + hashCode();

//	public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";

	/**
	 * 打开 UsbControlBlock
	 */
	private final ConcurrentHashMap<UsbDevice, UsbControlBlock> mCtrlBlocks = new ConcurrentHashMap<UsbDevice, UsbControlBlock>();
	private final SparseArray<WeakReference<UsbDevice>> mHasPermissions = new SparseArray<WeakReference<UsbDevice>>();
	private ConcurrentHashMap<Integer ,UsbDevice> targetUsbDevice = new ConcurrentHashMap<Integer,UsbDevice>();

	private final WeakReference<Context> mWeakContext;
	private final UsbManager mUsbManager;
	private final OnDeviceConnectListener mOnDeviceConnectListener;
	private PendingIntent mPermissionIntent = null;
	private List<DeviceFilter> mDeviceFilters = new ArrayList<DeviceFilter>();

	/**
	 * 用于在工作线程上调用回调的处理程序
	 */
	private final Handler mAsyncHandler;
	private volatile boolean destroyed;
	private volatile boolean isConnect = false;//标识有无设备连接
	/**
	 * 更改 USB 设备状态时的回调监听器
	 */
	public interface OnDeviceConnectListener {
		/**
		 * called when device attached
		 * @param device
		 */
		public void onAttach(UsbDevice device);
		/**
		 * called when device dettach(after onDisconnect)
		 * @param device
		 */
		public void onDetach(UsbDevice device);
		/**
		 * called after device opend
		 * @param device
		 * @param ctrlBlock
		 * @param createNew
		 */
		public void onConnect(UsbDevice device, UsbControlBlock ctrlBlock, boolean createNew);
		/**
		 * called when USB device removed or its power off (this callback is called after device closing)
		 * @param device
		 * @param ctrlBlock
		 */
		public void onDisconnect(UsbDevice device, UsbControlBlock ctrlBlock);
		/**
		 * called when canceled or could not get permission from user
		 * @param device
		 */
		public void onCancel(UsbDevice device);
	}

	public USBMonitor(final Context context, final OnDeviceConnectListener listener) {
		if (DEBUG) Log.v(TAG, "USBMonitor:Constructor");
		if (listener == null)
			throw new IllegalArgumentException("OnDeviceConnectListener should not null.");
		mWeakContext = new WeakReference<Context>(context);
		mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		mOnDeviceConnectListener = listener;
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		destroyed = false;
		if (DEBUG) Log.v(TAG, "USBMonitor:mUsbManager=" + mUsbManager);
	}

	/**
	 * Release all related resources,
	 * never reuse again
	 */
	public void destroy() {
		isConnect = false;
		if (DEBUG) Log.i(TAG, "destroy:");
		//unregister();
		if (!destroyed) {
			destroyed = true;
			// 关闭所有受监控的 USB 设备;;
			final Set<UsbDevice> keys = mCtrlBlocks.keySet();
			if (keys != null) {
				UsbControlBlock ctrlBlock;
				try {
					for (final UsbDevice key: keys) {
						ctrlBlock = mCtrlBlocks.remove(key);
						if (ctrlBlock != null) {

							ctrlBlock.close();
						}
					}
				} catch (final Exception e) {
					Log.e(TAG, "destroy:", e);
				}
			}
			mCtrlBlocks.clear();
			try {
				mAsyncHandler.getLooper().quit();
			} catch (final Exception e) {
				Log.e(TAG, "destroy:", e);
			}
		}
	}

	/**
	 * register BroadcastReceiver to monitor USB events
	 * @throws IllegalStateException
	 */
	public synchronized void register() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		Log.e(TAG, "register: " + mDeviceFilters.size());
		if (mPermissionIntent == null) {
			if (DEBUG) Log.e(TAG, "register:");
			final Context context = mWeakContext.get();
//			if (DEBUG) Log.e(TAG,"=====register============before==========PendingIntent.getBroadcast=======");
			if (context != null) {
				Intent intent = new Intent(ACTION_USB_PERMISSION);
				mPermissionIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_CANCEL_CURRENT);
				final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
				// ACTION_USB_DEVICE_ATTACHED never comes on some devices so it should not be added here
				filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
				context.registerReceiver(mUsbReceiver, filter);
			}
			// start connection check
			if (DEBUG) Log.e(TAG,"=====register============before==========mDeviceCheckRunnable=======");
//			mDeviceCounts = 0;
			mAsyncHandler.postDelayed(mDeviceCheckRunnable, 1000);
			if (DEBUG) Log.e(TAG,"=====register============behind==========mDeviceCheckRunnable=======");
		}
	}

	/**
	 * unregister BroadcastReceiver
	 * @throws IllegalStateException
	 */
	public synchronized void unregister() throws IllegalStateException {
		// 接続チェック用Runnableを削除
//		mDeviceCounts = 0;
		if (!destroyed) {
			mAsyncHandler.removeCallbacks(mDeviceCheckRunnable);
		}
		if (mPermissionIntent != null) {
//			if (DEBUG) Log.i(TAG, "unregister:");
			final Context context = mWeakContext.get();
			try {
				if (context != null) {
					context.unregisterReceiver(mUsbReceiver);
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mPermissionIntent = null;
		}
	}

	public synchronized boolean isRegistered() {
		return !destroyed && (mPermissionIntent != null);
	}

	/**
	 * set device filter
	 * @param filter
	 * @throws IllegalStateException
	 */
	public void setDeviceFilter(final DeviceFilter filter) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.clear();
		mDeviceFilters.add(filter);
	}

	/**
	 * デバイスフィルターを追加
	 * @param filter
	 * @throws IllegalStateException
	 */
	public void addDeviceFilter(final DeviceFilter filter) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.add(filter);
	}

	/**
	 * デバイスフィルターを削除
	 * @param filter
	 * @throws IllegalStateException
	 */
	public void removeDeviceFilter(final DeviceFilter filter) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.remove(filter);
	}

	/**
	 * set device filters
	 * @param filters
	 * @throws IllegalStateException
	 */
	public void setDeviceFilter(final List<DeviceFilter> filters) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.clear();
		mDeviceFilters.addAll(filters);
	}

	/**
	 * add device filters
	 * @param filters
	 * @throws IllegalStateException
	 */
	public void addDeviceFilter(final List<DeviceFilter> filters) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.addAll(filters);
	}

	/**
	 * remove device filters
	 * @param filters
	 */
	public void removeDeviceFilter(final List<DeviceFilter> filters) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.removeAll(filters);
	}

	/**
	 * return the number of connected USB devices that matched device filter
	 * @return
	 * @throws IllegalStateException
	 */
	public int getDeviceCount() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		return getDeviceList().size();
	}

	/**
	 * return device list, return empty list if no device matched
	 * @return
	 * @throws IllegalStateException
	 */
	public List<UsbDevice> getDeviceList() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		return getDeviceList(mDeviceFilters);
	}

	/**
	 * return device list, return empty list if no device matched
	 * @param filters 过滤器
	 * @return 设备列表，通过过滤器
	 * @throws IllegalStateException
	 */
	public List<UsbDevice> getDeviceList(final List<DeviceFilter> filters) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
//		Log.e(TAG, "getDeviceList:  ======size==>" + deviceList.size());
		final List<UsbDevice> result = new ArrayList<UsbDevice>();
		if (deviceList.size() != 0) {
			//判断有无过滤器
			if ((filters == null) || filters.isEmpty()) {
				result.addAll(deviceList.values());
			} else {
				for (final UsbDevice device: deviceList.values() ) {
					for (final DeviceFilter filter: filters) {
						if ((filter != null) && filter.matches(device)) {
							// when filter matches
							if (!filter.isExclude) {
								result.add(device);
							}
							break;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * return device list, return empty list if no device matched
	 * @param filter
	 * @return
	 * @throws IllegalStateException
	 */
	public List<UsbDevice> getDeviceList(final DeviceFilter filter) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		final List<UsbDevice> result = new ArrayList<UsbDevice>();
		if (deviceList != null) {
			for (final UsbDevice device: deviceList.values() ) {
				if ((filter == null) || (filter.matches(device) && !filter.isExclude)) {
					result.add(device);
				}
			}
		}
		return result;
	}

	/**
	 * get USB device list, without filter
	 * @return
	 * @throws IllegalStateException
	 */
	public Iterator<UsbDevice> getDevices() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		Iterator<UsbDevice> iterator = null;
		final HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
		if (list != null)
			iterator = list.values().iterator();
		return iterator;
	}

	/**
	 * output device list to LogCat
	 */
	public final void dumpDevices() {
		final HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
		if (list != null) {
			final Set<String> keys = list.keySet();
			if (keys != null && keys.size() > 0) {
				final StringBuilder sb = new StringBuilder();
				for (final String key: keys) {
					final UsbDevice device = list.get(key);
					final int num_interface = device != null ? device.getInterfaceCount() : 0;
					sb.setLength(0);
					for (int i = 0; i < num_interface; i++) {
						sb.append(String.format(Locale.US, "interface%d:%s", i, device.getInterface(i).toString()));
					}
					Log.i(TAG, "key=" + key + ":" + device + ":" + sb.toString());
				}
			} else {
				Log.i(TAG, "no device");
			}
		} else {
			Log.i(TAG, "no device");
		}
	}

	/**
	 * 判断设备是否有权限，有则添加到 mHasPermissions 中。否则返回无权限
	 * return whether the specific Usb device has permission
	 * @param device
	 * @return true: 指定したUsbDeviceにパーミッションがある
	 * @throws IllegalStateException
	 */
	public final boolean hasPermission(final UsbDevice device) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		if (mUsbManager.hasPermission(device)){
			updatePermission(device,true);
		}
		return mUsbManager.hasPermission(device);
	}

	/**
	 * 更新内部持有的权限状态。
	 * 有权限则更新mHasPermissions中列表
	 * 有权限：则把设备的信息String的hashcode存储在一个spareArray mHasPermissions中
	 * @param device 设备
	 * @param hasPermission 是否含有权限
	 * @return hasPermission 是否有权限
	 */
	private boolean updatePermission(final UsbDevice device, final boolean hasPermission) {
//		mUsbManager.requestPermission(device,mPermissionIntent);
//		if (device.getProductId() == 1 && device.getVendorId() == 5396){
//			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){//API 大于等于2 3 /9时，所有的USB设备访问之前都给权限
//				if (DEBUG)
//					Log.e(TAG, "run: Build.VERSION.SDK_INT >= Build.VERSION_CODES.p");
//					if(device.getProductId() == 1 && device.getVendorId() == 5396) {
//						//							if (DEBUG)
//						Log.e(TAG, "run: contains 5396 1 ");
//						if (!mUsbManager.hasPermission(device)){
//							mUsbManager.requestPermission(device,mPermissionIntent);
//						}
//					}
//			}

			final int deviceKey = getDeviceKey(device, true);
			synchronized (mHasPermissions) {
				if (hasPermission) {
					if (mHasPermissions.get(deviceKey) == null) {
						mHasPermissions.put(deviceKey, new WeakReference<UsbDevice>(device));
					}
				} else {
					mHasPermissions.remove(deviceKey);
				}
			}
			return hasPermission;
	}

	/**
	 * request permission to access to USB device
	 * @param device
	 * @return true if fail to request permission
	 */
	public synchronized boolean requestPermission(final UsbDevice device) {
		if (DEBUG) Log.e(TAG, "requestPermission:device=" + device);
		boolean result = false;
		if (isRegistered()) {
			if (device != null) {
				if (mUsbManager.hasPermission(device)) {
					//还得更新mHasPermission 列表 读取设备的信息。
					// call onConnect if app already has permission
//					updatePermission(device,true);
					processConnect(device);
				} else {
					try {
						// パーミッションがなければ要求する
						mUsbManager.requestPermission(device, mPermissionIntent);
					} catch (final Exception e) {
						// Android5.1.xのGALAXY系でandroid.permission.sec.MDM_APP_MGMTという意味不明の例外生成するみたい
						Log.w(TAG, e);
						processCancel(device);
						result = true;
					}
				}
			} else {
				processCancel(device);
				result = true;
			}
		} else {
			processCancel(device);
			result = true;
		}
		return result;
	}

	/**
	 * 指定したUsbDeviceをopenする
	 * @param device
	 * @return
	 * @throws SecurityException パーミッションがなければSecurityExceptionを投げる
	 */
	public UsbControlBlock openDevice(final UsbDevice device) throws SecurityException {
		if (hasPermission(device)) {
			UsbControlBlock result = mCtrlBlocks.get(device);
			if (result == null) {
				result = new UsbControlBlock(USBMonitor.this, device);    // この中でopenDeviceする
				mCtrlBlocks.put(device, result);
			}
			return result;
		} else {
			throw new SecurityException("has no permission");
		}
	}

	/**
	 * BroadcastReceiver for USB permission
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (destroyed) return;
			final String action = intent.getAction();
			if (DEBUG)Log.e(TAG, "onReceive: Define"+action);
			if (ACTION_USB_PERMISSION.equals(action)) {
				// when received the result of requesting USB permission
				synchronized (USBMonitor.this) {
					final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//					assert device != null;
//					Log.e(TAG, "onReceive: " + (device.toString()));
					Log.e(TAG, "onReceive:  hasPermission " + mUsbManager.hasPermission(device));
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {
//							isConnect = true;
//							processConnect(device);
							// get permission, call onConnect
							updatePermission(device,true);
							if (DEBUG) Log.e(TAG,"UsbMonitor.register.broadcastReceiver.mUsbReceiver.OnReceiver=device !=null!!!====todo connect");
							processConnect(device);
						}
					} else {
						// failed to get permission
						if (DEBUG) Log.e(TAG,"UsbMonitor.register.broadcastReceiver.mUsbReceiver.OnReceiver=device ==null!!!====todo processCancel");
						processCancel(device);
					}
				}
			} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				if (DEBUG)Log.e(TAG, "onReceive: ACTION_USB_DEVICE_ATTACHED");
				final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				hasPermission(device);
//				if (mHasPermissions.size() > 0){
////					updatePermission(device, true);
//				}else if (mHasPermissions.size() == 0){
//					processAttach(device);
//				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				// when device removed
				if (DEBUG)Log.e(TAG, "onReceive: ACTION_USB_DEVICE_DETACHED");
				final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//				if (mHasPermissions.)

				if (device != null) {
					UsbControlBlock ctrlBlock = mCtrlBlocks.remove(device);
					if (ctrlBlock != null) {
						// cleanup
						isConnect = false;
						ctrlBlock.close();
					}
//					mDeviceCounts = 0;
					processDettach(device);

				}
			}
		}
	};


	/**
	 * open specific USB device
	 * @param device
	 */
	private final void processConnect(final UsbDevice device) {
		if (mUsbManager.hasPermission(device)) {
			if (destroyed)
				return;
			updatePermission(device, true);
			if (DEBUG)
				Log.e(TAG, "processConnect:device=" + device);
			if (DEBUG)
				Log.e(TAG, "=================processConnect===========================");
			UsbControlBlock ctrlBlock;
			final boolean createNew;
			ctrlBlock = mCtrlBlocks.get(device);
			if (ctrlBlock == null) {
				ctrlBlock = new UsbControlBlock(USBMonitor.this, device);
				mCtrlBlocks.put(device, ctrlBlock);
				createNew = true;
			} else {
				createNew = false;
			}
			if (mOnDeviceConnectListener != null) {
				isConnect = true;
				mOnDeviceConnectListener.onConnect(device, ctrlBlock, createNew);
			}
		}

		/*
		mAsyncHandler.post(new Runnable() {
			@Override
			public void run() {
				if (DEBUG) Log.v(TAG, "processConnect:device=" + device);
				UsbControlBlock ctrlBlock;
				final boolean createNew;
				ctrlBlock = mCtrlBlocks.get(device);
				if (ctrlBlock == null) {
					ctrlBlock = new UsbControlBlock(USBMonitor.this, device);
					mCtrlBlocks.put(device, ctrlBlock);
					createNew = true;
				} else {
					createNew = false;
				}
				if (mOnDeviceConnectListener != null) {
					mOnDeviceConnectListener.onConnect(device, ctrlBlock, createNew);
				}
			}
		});
		*/
	}
	/** number of connected & detected devices */
//	private volatile int mDeviceCounts = 0;
	/**
	 * periodically check connected devices and if it changed, call onAttach
	 */
	private final Runnable mDeviceCheckRunnable = new Runnable() {
		@Override
		public void run() {
			if (destroyed) return;
			//每两秒刷新的设备列表

			final List<UsbDevice> devices = getDeviceList();

//			final int hasPermissionCounts;//两秒前 已有权限的数量
//			final int m;//现拥有设备的 权限数量
			//什么时候要去连接设备。
			targetUsbDevice.clear();
			for(UsbDevice device : devices){
				if(device.getProductId() == 1 && device.getVendorId() == 5396){
					targetUsbDevice.put(targetUsbDevice.size(),device);
				}
			}
			final int n = targetUsbDevice.size();//当前 连接符合条件的设备数量

			synchronized (mHasPermissions) {
//				hasPermissionCounts = mHasPermissions.size();
				mHasPermissions.clear();
				//遍历
				for (final UsbDevice device: targetUsbDevice.values()) {
						hasPermission(device);
				}
				//如果没有设备有权限则会调用请求权限。收到一个广播
//				if (targetUsbDevice.size() > 0){
//
//					(targetUsbDevice.get(0));
//				}
//				m = mHasPermissions.size();
			}
			//当权限列表 == 设备列表 则去连接每个有权限的设备
			if (!isConnect && targetUsbDevice.size() > 0) {
//				mDeviceCounts = n;
//				if (mOnDeviceConnectListener != null) {
					//					for (int index : mHasPermissions.keyAt(0)) {
				UsbDevice device = targetUsbDevice.get(0);
//					if (DEBUG) Log.e(TAG,"===============> "+mUsbManager.hasPermission(device) + " " + device.toString());
//					if(device.getProductId() == 1 && device.getVendorId() == 5396 && mUsbManager.hasPermission(device) ) {
						mAsyncHandler.post(new Runnable() {
							@Override
							public void run() {
								processAttach(device);
//								processConnect(device);
//								isConnect = true;
							}
						});
//					}
					//					}
//				}
			}
			mAsyncHandler.postDelayed(this, 2000);	// confirm every 2 seconds
		}
	};

	private final void processCancel(final UsbDevice device) {
		if (mUsbManager.hasPermission(device)){
			if (destroyed) return;
			if (DEBUG) Log.v(TAG, "processCancel:");
			updatePermission(device, false);
			if (mOnDeviceConnectListener != null) {
				mAsyncHandler.post(new Runnable() {
					@Override
					public void run() {
						mOnDeviceConnectListener.onCancel(device);
//						isConnect = false;
					}
				});
			}
		}
	}

	/**
	 * 调用了 成员方法 RequestPermission 方法。
	 * @param device
	 */
	private final void processAttach(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.e(TAG, "processAttach:");
		if (mOnDeviceConnectListener != null) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnDeviceConnectListener.onAttach(device);
				}
			});
		}
	}

	private final void processDettach(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.e(TAG, "processDettach:");
		if (mOnDeviceConnectListener != null) {
			mAsyncHandler.post(new Runnable() {
				@Override
				public void run() {
					mOnDeviceConnectListener.onDetach(device);

				}
			});
		}
	}

	/**
	 * 生成用于保存每个 USB 设备设置的设备密钥名称。
	 * 从供应商 ID、产品 ID、设备类、设备子类、设备协议生成
	 * 请注意，相同类型的产品将使用相同的密钥名称。
	 * @param device 如果为 null 则返回空字符串
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device) {
		return getDeviceKeyName(device, null, false);
	}

	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。
	 * useNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device
	 * @param useNewAPI
	 * @return
	 */
	public static final String getDeviceKeyName(final UsbDevice device, final boolean useNewAPI) {
		return getDeviceKeyName(device, null, useNewAPI);
	}

	public static final String getMyDeviceKeyName(final UsbDevice device, final String serial) {
		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId());			sb.append("#");	// API >= 12
		sb.append(device.getProductId());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceClass());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceSubclass());		sb.append("#");	// API >= 12
		sb.append(device.getDeviceProtocol());						// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#");	sb.append(serial);
		}
		if (DEBUG) Log.e(TAG, "getMyDeviceKeyName:" + sb.toString());
		return sb.toString();
	}

	/**
	 * USB機器毎の設定保存用にデバイスキー名を生成する。この機器名をHashMapのキーにする
	 * UsbDeviceがopenしている時のみ有効
	 * ベンダーID, プロダクトID, デバイスクラス, デバイスサブクラス, デバイスプロトコルから生成
	 * serialがnullや空文字でなければserialを含めたデバイスキー名を生成する
	 * useNewAPI=trueでAPIレベルを満たしていればマニュファクチャ名, バージョン, コンフィギュレーションカウントも使う
	 * @param device nullなら空文字列を返す
	 * @param serial	传递UsbDeviceConnection获取的序列号#getSerial，如果useNewAPI = true and API> = 21 with null，内部获取
	 * @param useNewAPI 仅在 API> = 21 或 API> = 23 中可用的方法(但是，根据设备的不同，会返回null，因此是否有效取决于设备。)
	 * @return
	 */
	@SuppressLint("NewApi")
	public static final String getDeviceKeyName(final UsbDevice device, final String serial, final boolean useNewAPI) {
		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId());			sb.append("#");	// API >= 12
		sb.append(device.getProductId());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceClass());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceSubclass());		sb.append("#");	// API >= 12
		sb.append(device.getDeviceProtocol());						// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#");	sb.append(serial);
		}
		//长城注释 有问题
		if (useNewAPI && BuildCheck.isAndroid5()) {

			sb.append("#");
			if (TextUtils.isEmpty(serial)) {
				sb.append(device.getSerialNumber());	sb.append("#");	// API >= 21 //target Sdk >= 29时要权限
			}
			sb.append(device.getManufacturerName());	sb.append("#");	// API >= 21
			sb.append(device.getConfigurationCount());	sb.append("#");	// API >= 21
			if (BuildCheck.isMarshmallow()) {           // API >= 23
				sb.append(device.getVersion());			sb.append("#");
			}
		}
//		if (DEBUG) Log.e(TAG, "getDeviceKeyName:" + sb.toString());
		return sb.toString();
	}

	/**
	 * 以整数形式获取设备密钥
	 * getDeviceKeyName 获取中获取的字符串的hasCode
	 * 从供应商 ID、产品 ID、设备类、设备子类、设备协议生成
	 * 请注意，相同类型的产品将使用相同的设备密钥。
	 * @param device 如果为空则返回 0
	 * @return
	 */
	public static final int getDeviceKey(final UsbDevice device) {
		return device != null ? getDeviceKeyName(device, null, false).hashCode() : 0;
	}

	/**
	 * 以整数形式获取设备密钥
	 * getDeviceKeyName 获取中获取的字符串的hasCode
	 * useNewAPI=false 请注意，相同类型的产品将使用相同的设备密钥。
	 * @param device 判断设备为不为空
	 * @param useNewAPI
	 * @return 设备信息拼接的字符串的hashcode 或 0
	 */
	public static final int getDeviceKey(final UsbDevice device, final boolean useNewAPI) {
		return device != null ? getDeviceKeyName(device, null, useNewAPI).hashCode() : 0;
	}

	/**
	 * デバイスキーを整数として取得
	 * getDeviceKeyNameで得られる文字列のhasCodeを取得
	 * serialがnullでuseNewAPI=falseで同種の製品だと同じデバイスキーになるので注意
	 * @param device nullなら0を返す
	 * @param serial UsbDeviceConnection#getSerialで取得したシリアル番号を渡す, nullでuseNewAPI=trueでAPI>=21なら内部で取得
	 * @param useNewAPI API>=21またはAPI>=23のみで使用可能なメソッドも使用する(ただし機器によってはnullが返ってくるので有効かどうかは機器による)
	 * @return
	 */
	public static final int getDeviceKey(final UsbDevice device, final String serial, final boolean useNewAPI) {
		return device != null ? getDeviceKeyName(device, serial, useNewAPI).hashCode() : 0;
	}

	public static class UsbDeviceInfo {
		public String usb_version;
		public String manufacturer;
		public String product;
		public String version;
		public String serial;

		private void clear() {
			usb_version = manufacturer = product = version = serial = null;
		}

		@Override
		public String toString() {
			return String.format("UsbDevice:usb_version=%s,manufacturer=%s,product=%s,version=%s,serial=%s",
				usb_version != null ? usb_version : "",
				manufacturer != null ? manufacturer : "",
				product != null ? product : "",
				version != null ? version : "",
				serial != null ? serial : "");
		}
	}

	private static final int USB_DIR_OUT = 0;
	private static final int USB_DIR_IN = 0x80;
	private static final int USB_TYPE_MASK = (0x03 << 5);
	private static final int USB_TYPE_STANDARD = (0x00 << 5);
	private static final int USB_TYPE_CLASS = (0x01 << 5);
	private static final int USB_TYPE_VENDOR = (0x02 << 5);
	private static final int USB_TYPE_RESERVED = (0x03 << 5);
	private static final int USB_RECIP_MASK = 0x1f;
	private static final int USB_RECIP_DEVICE = 0x00;
	private static final int USB_RECIP_INTERFACE = 0x01;
	private static final int USB_RECIP_ENDPOINT = 0x02;
	private static final int USB_RECIP_OTHER = 0x03;
	private static final int USB_RECIP_PORT = 0x04;
	private static final int USB_RECIP_RPIPE = 0x05;
	private static final int USB_REQ_GET_STATUS = 0x00;
	private static final int USB_REQ_CLEAR_FEATURE = 0x01;
	private static final int USB_REQ_SET_FEATURE = 0x03;
	private static final int USB_REQ_SET_ADDRESS = 0x05;
	private static final int USB_REQ_GET_DESCRIPTOR = 0x06;
	private static final int USB_REQ_SET_DESCRIPTOR = 0x07;
	private static final int USB_REQ_GET_CONFIGURATION = 0x08;
	private static final int USB_REQ_SET_CONFIGURATION = 0x09;
	private static final int USB_REQ_GET_INTERFACE = 0x0A;
	private static final int USB_REQ_SET_INTERFACE = 0x0B;
	private static final int USB_REQ_SYNCH_FRAME = 0x0C;
	private static final int USB_REQ_SET_SEL = 0x30;
	private static final int USB_REQ_SET_ISOCH_DELAY = 0x31;
	private static final int USB_REQ_SET_ENCRYPTION = 0x0D;
	private static final int USB_REQ_GET_ENCRYPTION = 0x0E;
	private static final int USB_REQ_RPIPE_ABORT = 0x0E;
	private static final int USB_REQ_SET_HANDSHAKE = 0x0F;
	private static final int USB_REQ_RPIPE_RESET = 0x0F;
	private static final int USB_REQ_GET_HANDSHAKE = 0x10;
	private static final int USB_REQ_SET_CONNECTION = 0x11;
	private static final int USB_REQ_SET_SECURITY_DATA = 0x12;
	private static final int USB_REQ_GET_SECURITY_DATA = 0x13;
	private static final int USB_REQ_SET_WUSB_DATA = 0x14;
	private static final int USB_REQ_LOOPBACK_DATA_WRITE = 0x15;
	private static final int USB_REQ_LOOPBACK_DATA_READ = 0x16;
	private static final int USB_REQ_SET_INTERFACE_DS = 0x17;

	private static final int USB_REQ_STANDARD_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_DEVICE);		// 0x10
	private static final int USB_REQ_STANDARD_DEVICE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE);			// 0x90
	private static final int USB_REQ_STANDARD_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);	// 0x11
	private static final int USB_REQ_STANDARD_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);	// 0x91
	private static final int USB_REQ_STANDARD_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);	// 0x12
	private static final int USB_REQ_STANDARD_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);		// 0x92

	private static final int USB_REQ_CS_DEVICE_SET  = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0x20
	private static final int USB_REQ_CS_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);					// 0xa0
	private static final int USB_REQ_CS_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);			// 0x21
	private static final int USB_REQ_CS_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);			// 0xa1
	private static final int USB_REQ_CS_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);				// 0x22
	private static final int USB_REQ_CS_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);				// 0xa2

	private static final int USB_REQ_VENDER_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0x40
	private static final int USB_REQ_VENDER_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0xc0
	private static final int USB_REQ_VENDER_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);		// 0x41
	private static final int USB_REQ_VENDER_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);		// 0xc1
	private static final int USB_REQ_VENDER_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);			// 0x42
	private static final int USB_REQ_VENDER_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);			// 0xc2

	private static final int USB_DT_DEVICE = 0x01;
	private static final int USB_DT_CONFIG = 0x02;
	private static final int USB_DT_STRING = 0x03;
	private static final int USB_DT_INTERFACE = 0x04;
	private static final int USB_DT_ENDPOINT = 0x05;
	private static final int USB_DT_DEVICE_QUALIFIER = 0x06;
	private static final int USB_DT_OTHER_SPEED_CONFIG = 0x07;
	private static final int USB_DT_INTERFACE_POWER = 0x08;
	private static final int USB_DT_OTG = 0x09;
	private static final int USB_DT_DEBUG = 0x0a;
	private static final int USB_DT_INTERFACE_ASSOCIATION = 0x0b;
	private static final int USB_DT_SECURITY = 0x0c;
	private static final int USB_DT_KEY = 0x0d;
	private static final int USB_DT_ENCRYPTION_TYPE = 0x0e;
	private static final int USB_DT_BOS = 0x0f;
	private static final int USB_DT_DEVICE_CAPABILITY = 0x10;
	private static final int USB_DT_WIRELESS_ENDPOINT_COMP = 0x11;
	private static final int USB_DT_WIRE_ADAPTER = 0x21;
	private static final int USB_DT_RPIPE = 0x22;
	private static final int USB_DT_CS_RADIO_CONTROL = 0x23;
	private static final int USB_DT_PIPE_USAGE = 0x24;
	private static final int USB_DT_SS_ENDPOINT_COMP = 0x30;
	private static final int USB_DT_CS_DEVICE = (USB_TYPE_CLASS | USB_DT_DEVICE);
	private static final int USB_DT_CS_CONFIG = (USB_TYPE_CLASS | USB_DT_CONFIG);
	private static final int USB_DT_CS_STRING = (USB_TYPE_CLASS | USB_DT_STRING);
	private static final int USB_DT_CS_INTERFACE = (USB_TYPE_CLASS | USB_DT_INTERFACE);
	private static final int USB_DT_CS_ENDPOINT = (USB_TYPE_CLASS | USB_DT_ENDPOINT);
	private static final int USB_DT_DEVICE_SIZE = 18;

	/**
	 * 指定したIDのStringディスクリプタから文字列を取得する。取得できなければnull
	 * @param connection
	 * @param id
	 * @param languageCount
	 * @param languages
	 * @return
	 */
	private static String getString(final UsbDeviceConnection connection, final int id, final int languageCount, final byte[] languages) {
		final byte[] work = new byte[256];
		String result = null;
		for (int i = 1; i <= languageCount; i++) {
			int ret = connection.controlTransfer(
				USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
				USB_REQ_GET_DESCRIPTOR,
				(USB_DT_STRING << 8) | id, languages[i], work, 256, 0);
			if ((ret > 2) && (work[0] == ret) && (work[1] == USB_DT_STRING)) {
				// skip first two bytes(bLength & bDescriptorType), and copy the rest to the string
				try {
					result = new String(work, 2, ret - 2, "UTF-16LE");
					if (!"Љ".equals(result)) {	// 変なゴミが返ってくる時がある
						break;
					} else {
						result = null;
					}
				} catch (final UnsupportedEncodingException e) {
					// ignore
				}
			}
		}
		return result;
	}

	/**
	 * ベンダー名・製品名・バージョン・シリアルを取得する
	 * @param device
	 * @return
	 */
	public UsbDeviceInfo getDeviceInfo(final UsbDevice device) {
		return updateDeviceInfo(mUsbManager, device, null);
	}

	/**
	 * 获取供应商名称/产品名称/版本/序列号
	 * #updateDeviceInfo(final UsbManager, final UsbDevice, final UsbDeviceInfo)のヘルパーメソッド
	 * @param context
	 * @param device
	 * @return
	 */
	public static UsbDeviceInfo getDeviceInfo(final Context context, final UsbDevice device) {
		return updateDeviceInfo((UsbManager)context.getSystemService(Context.USB_SERVICE), device, new UsbDeviceInfo());
	}

	/**
	 * ベンダー名・製品名・バージョン・シリアルを取得する
	 * @param manager
	 * @param device
	 * @param _info
	 * @return
	 */
	public static UsbDeviceInfo updateDeviceInfo(final UsbManager manager, final UsbDevice device, final UsbDeviceInfo _info) {
		final UsbDeviceInfo info = _info != null ? _info : new UsbDeviceInfo();
		info.clear();

		if (device != null) {
			if (BuildCheck.isLollipop()) {
				info.manufacturer = device.getManufacturerName();
				info.product = device.getProductName();
				info.serial = device.getSerialNumber();
			}
			if (BuildCheck.isMarshmallow()) {
				info.usb_version = device.getVersion();
			}
			if ((manager != null) && manager.hasPermission(device)) {
				final UsbDeviceConnection connection = manager.openDevice(device);
				final byte[] desc = connection.getRawDescriptors();

				if (TextUtils.isEmpty(info.usb_version)) {
					info.usb_version = String.format("%x.%02x", ((int)desc[3] & 0xff), ((int)desc[2] & 0xff));
				}
				if (TextUtils.isEmpty(info.version)) {
					info.version = String.format("%x.%02x", ((int)desc[13] & 0xff), ((int)desc[12] & 0xff));
				}
				if (TextUtils.isEmpty(info.serial)) {
					info.serial = connection.getSerial();
				}

				final byte[] languages = new byte[256];
				int languageCount = 0;
				// controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)
				try {
					int result = connection.controlTransfer(
						USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
	    				USB_REQ_GET_DESCRIPTOR,
	    				(USB_DT_STRING << 8) | 0, 0, languages, 256, 0);
					if (result > 0) {
	        			languageCount = (result - 2) / 2;
					}
					if (languageCount > 0) {
						if (TextUtils.isEmpty(info.manufacturer)) {
							info.manufacturer = getString(connection, desc[14], languageCount, languages);
						}
						if (TextUtils.isEmpty(info.product)) {
							info.product = getString(connection, desc[15], languageCount, languages);
						}
						if (TextUtils.isEmpty(info.serial)) {
							info.serial = getString(connection, desc[16], languageCount, languages);
						}
					}
				} finally {
					connection.close();
				}
			}
			if (TextUtils.isEmpty(info.manufacturer)) {
				info.manufacturer = USBVendorId.vendorName(device.getVendorId());
			}
			if (TextUtils.isEmpty(info.manufacturer)) {
				info.manufacturer = String.format("%04x", device.getVendorId());
			}
			if (TextUtils.isEmpty(info.product)) {
				info.product = String.format("%04x", device.getProductId());
			}
		}
		return info;
	}

	/**
	 * control class
	 * never reuse the instance when it closed
	 */
	public static final class UsbControlBlock implements Cloneable {
		private final WeakReference<USBMonitor> mWeakMonitor;
		private final WeakReference<UsbDevice> mWeakDevice;
		protected UsbDeviceConnection mConnection;
		protected final UsbDeviceInfo mInfo;
		private final int mBusNum;
		private final int mDevNum;
		private final SparseArray<SparseArray<UsbInterface>> mInterfaces = new SparseArray<SparseArray<UsbInterface>>();

		/**
		 * this class needs permission to access USB device before constructing
		 * @param monitor
		 * @param device
		 */
		private UsbControlBlock(final USBMonitor monitor, final UsbDevice device) {
			if (DEBUG) Log.e(TAG, "UsbControlBlock:constructor");
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
			mConnection = monitor.mUsbManager.openDevice(device);
			mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
			final String name = device.getDeviceName();
			final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
			int busnum = 0;
			int devnum = 0;
			if (v != null) {
				busnum = Integer.parseInt(v[v.length-2]);
				devnum = Integer.parseInt(v[v.length-1]);
			}
			mBusNum = busnum;
			mDevNum = devnum;
//			if (DEBUG) {
				if (mConnection != null) {
					final int desc = mConnection.getFileDescriptor();
					final byte[] rawDesc = mConnection.getRawDescriptors();
					Log.e(TAG, String.format(Locale.US, "name=%s,desc=%d,busnum=%d,devnum=%d,rawDesc=", name, desc, busnum, devnum) + rawDesc);
				} else {
					Log.e(TAG, "could not connect to device " + name);
				}
//			}
		}

		/**
		 * copy constructor
		 * @param src
		 * @throws IllegalStateException
		 */
		private UsbControlBlock(final UsbControlBlock src) throws IllegalStateException {
			final USBMonitor monitor = src.getUSBMonitor();
			final UsbDevice device = src.getDevice();
			if (device == null) {
				throw new IllegalStateException("device may already be removed");
			}
			mConnection = monitor.mUsbManager.openDevice(device);
			if (mConnection == null) {
				throw new IllegalStateException("device may already be removed or have no permission");
			}
			mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
			mBusNum = src.mBusNum;
			mDevNum = src.mDevNum;
			// FIXME USBMonitor.mCtrlBlocksに追加する(今はHashMapなので追加すると置き換わってしまうのでだめ, ListかHashMapにListをぶら下げる?)
		}

		/**
		 * duplicate by clone
		 * need permission
		 * USBMonitor never handle cloned UsbControlBlock, you should release it after using it.
		 * @return
		 * @throws CloneNotSupportedException
		 */
		@Override
		public UsbControlBlock clone() throws CloneNotSupportedException {
			final UsbControlBlock ctrlblock;
			try {
				ctrlblock = new UsbControlBlock(this);
			} catch (final IllegalStateException e) {
				throw new CloneNotSupportedException(e.getMessage());
			}
			return ctrlblock;
		}

		public USBMonitor getUSBMonitor() {
			return mWeakMonitor.get();
		}

		public final UsbDevice getDevice() {
			return mWeakDevice.get();
		}

		/**
		 * get device name
		 * @return
		 */
		public String getDeviceName() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getDeviceName() : "";
		}

		/**
		 * get device id
		 * @return
		 */
		public int getDeviceId() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getDeviceId() : 0;
		}

		/**
		 * get device key string
		 * @return same value if the devices has same vendor id, product id, device class, device subclass and device protocol
		 */
		public String getDeviceKeyName() {
			return USBMonitor.getDeviceKeyName(mWeakDevice.get());
		}

		/**
		 * get device key string
		 * @param useNewAPI if true, try to use serial number
		 * @return
		 * @throws IllegalStateException
		 */
		public String getDeviceKeyName(final boolean useNewAPI) throws IllegalStateException {
			if (useNewAPI) checkConnection();
			return USBMonitor.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, useNewAPI);
		}

		/**
		 * get device key
		 * @return
		 * @throws IllegalStateException
		 */
		public int getDeviceKey() throws IllegalStateException {
			checkConnection();
			return USBMonitor.getDeviceKey(mWeakDevice.get());
		}

		/**
		 * get device key
		 * @param useNewAPI if true, try to use serial number
		 * @return
		 * @throws IllegalStateException
		 */
		public int getDeviceKey(final boolean useNewAPI) throws IllegalStateException {
			if (useNewAPI) checkConnection();
			return USBMonitor.getDeviceKey(mWeakDevice.get(), mInfo.serial, useNewAPI);
		}

		/**
		 * get device key string
		 * if device has serial number, use it
		 * @return
		 */
		public String getDeviceKeyNameWithSerial() {
			return USBMonitor.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, false);
		}

		/**
		 * get device key
		 * if device has serial number, use it
		 * @return
		 */
		public int getDeviceKeyWithSerial() {
			return getDeviceKeyNameWithSerial().hashCode();
		}

		/**
		 * get UsbDeviceConnection
		 * @return
		 */
		public synchronized UsbDeviceConnection getConnection() {
			return mConnection;
		}

		/**
		 * get file descriptor to access USB device
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized int getFileDescriptor() throws IllegalStateException {
			checkConnection();
			return mConnection.getFileDescriptor();
		}

		/**
		 * get raw descriptor for the USB device
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized byte[] getRawDescriptors() throws IllegalStateException {
			checkConnection();
			return mConnection.getRawDescriptors();
		}

		/**
		 * get vendor id
		 * @return
		 */
		public int getVenderId() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getVendorId() : 0;
		}

		/**
		 * get product id
		 * @return
		 */
		public int getProductId() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getProductId() : 0;
		}

		/**
		 * get version string of USB
		 * @return
		 */
		public String getUsbVersion() {
			return mInfo.usb_version;
		}

		/**
		 * get manufacture
		 * @return
		 */
		public String getManufacture() {
			return mInfo.manufacturer;
		}

		/**
		 * get product name
		 * @return
		 */
		public String getProductName() {
			return mInfo.product;
		}

		/**
		 * get version
		 * @return
		 */
		public String getVersion() {
			return mInfo.version;
		}

		/**
		 * get serial number
		 * @return
		 */
		public String getSerial() {
			return mInfo.serial;
		}

		public int getBusNum() {
			return mBusNum;
		}

		public int getDevNum() {
			return mDevNum;
		}

		/**
		 * get interface
		 * @param interface_id
		 * @throws IllegalStateException
		 */
		public synchronized UsbInterface getInterface(final int interface_id) throws IllegalStateException {
			return getInterface(interface_id, 0);
		}

		/**
		 * get interface
		 * @param interface_id
		 * @param altsetting
		 * @return
		 * @throws IllegalStateException
		 */
		public synchronized UsbInterface getInterface(final int interface_id, final int altsetting) throws IllegalStateException {
			checkConnection();
			SparseArray<UsbInterface> intfs = mInterfaces.get(interface_id);
			if (intfs == null) {
				intfs = new SparseArray<UsbInterface>();
				mInterfaces.put(interface_id, intfs);
			}
			UsbInterface intf = intfs.get(altsetting);
			if (intf == null) {
				final UsbDevice device = mWeakDevice.get();
				final int n = device.getInterfaceCount();
				for (int i = 0; i < n; i++) {
					final UsbInterface temp = device.getInterface(i);
					if ((temp.getId() == interface_id) && (temp.getAlternateSetting() == altsetting)) {
						intf = temp;
						break;
					}
				}
				if (intf != null) {
					intfs.append(altsetting, intf);
				}
			}
			return intf;
		}

		/**
		 * open specific interface
		 * @param intf
		 */
		public synchronized void claimInterface(final UsbInterface intf) {
			claimInterface(intf, true);
		}

		public synchronized void claimInterface(final UsbInterface intf, final boolean force) {
			checkConnection();
			mConnection.claimInterface(intf, force);
		}

		/**
		 * close interface
		 * @param intf
		 * @throws IllegalStateException
		 */
		public synchronized void releaseInterface(final UsbInterface intf) throws IllegalStateException {
			checkConnection();
			final SparseArray<UsbInterface> intfs = mInterfaces.get(intf.getId());
			if (intfs != null) {
				final int index = intfs.indexOfValue(intf);
				intfs.removeAt(index);
				if (intfs.size() == 0) {
					mInterfaces.remove(intf.getId());
				}
			}
			mConnection.releaseInterface(intf);
		}

		/**
		 * Close device
		 * This also close interfaces if they are opened in Java side
		 */
		public synchronized void close() {
			if (DEBUG) Log.i(TAG, "UsbControlBlock#close:");

			if (mConnection != null) {
				final int n = mInterfaces.size();
				for (int i = 0; i < n; i++) {
					final SparseArray<UsbInterface> intfs = mInterfaces.valueAt(i);
					if (intfs != null) {
						final int m = intfs.size();
						for (int j = 0; j < m; j++) {
							final UsbInterface intf = intfs.valueAt(j);
							mConnection.releaseInterface(intf);
						}
						intfs.clear();
					}
				}
				mInterfaces.clear();
				mConnection.close();
				mConnection = null;
				final USBMonitor monitor = mWeakMonitor.get();
				if (monitor != null) {
					if (monitor.mOnDeviceConnectListener != null) {
						monitor.mOnDeviceConnectListener.onDisconnect(mWeakDevice.get(), UsbControlBlock.this);
					}
					monitor.mCtrlBlocks.remove(getDevice());
				}
			}
		}

		@Override
		public boolean equals(final Object o) {
			if (o == null) return false;
			if (o instanceof UsbControlBlock) {
				final UsbDevice device = ((UsbControlBlock) o).getDevice();
				return device == null ? mWeakDevice.get() == null
						: device.equals(mWeakDevice.get());
			} else if (o instanceof UsbDevice) {
				return o.equals(mWeakDevice.get());
			}
			return super.equals(o);
		}

//		@Override
//		protected void finalize() throws Throwable {
///			close();
//			super.finalize();
//		}

		private synchronized void checkConnection() throws IllegalStateException {
			if (mConnection == null) {
				throw new IllegalStateException("already closed");
			}
		}
	}

}
