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

package com.serenegiant.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.serenegiant.dialog.MessageDialogFragment;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.PermissionCheck;
import com.serenegiant.uvccamera.R;

/**
 * Created by saki on 2016/11/18.
 *
 */
public class BaseActivity extends AppCompatActivity
	implements MessageDialogFragment.MessageDialogListener {

	private static boolean DEBUG = false;	// FIXME 实际工作时设置为false
	private static final String TAG = BaseActivity.class.getSimpleName();

	/** 负责操作处理UI的 handler */
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	private final Thread mUiThread = mUIHandler.getLooper().getThread();
	/** 用于在工作（子）线程上进行的 handler */
	private Handler mWorkerHandler;
	private long mWorkerThreadID = -1;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 创建工作线程
		if (mWorkerHandler == null) {
			mWorkerHandler = HandlerThreadHandler.createHandler(TAG);
			mWorkerThreadID = mWorkerHandler.getLooper().getThread().getId();
		}
	}

	@Override
	protected void onPause() {
		clearToast();
		super.onPause();
	}

	@Override
	protected synchronized void onDestroy() {
		// 销毁 工作线程
		if (mWorkerHandler != null) {
			try {
				mWorkerHandler.getLooper().quit();
			} catch (final Exception e) {
				//
			}
			mWorkerHandler = null;
		}
		super.onDestroy();
	}

//================================================================================
	/**
	 * 在 UI 线程中运行 Runnable 的辅助方法
	 * @param task
	 * @param duration
	 */
	public final void runOnUiThread(final Runnable task, final long duration) {
		if (task == null) return;
		mUIHandler.removeCallbacks(task);
		if ((duration > 0) || Thread.currentThread() != mUiThread) {
			mUIHandler.postDelayed(task, duration);
		} else {
			try {
				task.run();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * 如果 UI 线程上指定的 Runnable 正在等待执行，则取消执行等待。
	 * @param task
	 */
	public final void removeFromUiThread(final Runnable task) {
		if (task == null) return;
		mUIHandler.removeCallbacks(task);
	}

	/**
	 * 在工作线程上执行指定的 Runnable
	 * 如果有相同的Runnable没有被执行，就会被取消（只有后面指定的那个会被执行）
	 * @param task
	 * @param delayMillis
	 */
	protected final synchronized void queueEvent(final Runnable task, final long delayMillis) {
		if ((task == null) || (mWorkerHandler == null)) return;
		try {
			mWorkerHandler.removeCallbacks(task);
			if (delayMillis > 0) {
				mWorkerHandler.postDelayed(task, delayMillis);
			} else if (mWorkerThreadID == Thread.currentThread().getId()) {
				task.run();
			} else {
				mWorkerHandler.post(task);
			}
		} catch (final Exception e) {
			// ignore
		}
	}

	/**
	 * 如果安排在工作线程上执行，则取消指定的Runnable
	 * @param task
	 */
	protected final synchronized void removeEvent(final Runnable task) {
		if (task == null) return;
		try {
			mWorkerHandler.removeCallbacks(task);
		} catch (final Exception e) {
			// ignore
		}
	}

//================================================================================
	private Toast mToast;
	/**
	 * 在 Toast 中查看消息
	 * @param msg
	 */
	protected void showToast(@StringRes final int msg, final Object... args) {
		removeFromUiThread(mShowToastTask);
		mShowToastTask = new ShowToastTask(msg, args);
		runOnUiThread(mShowToastTask, 0);
	}

	/**
	 * 如果显示 Toast 则取消
	 */
	protected void clearToast() {
		removeFromUiThread(mShowToastTask);
		mShowToastTask = null;
		try {
			if (mToast != null) {
				mToast.cancel();
				mToast = null;
			}
		} catch (final Exception e) {
			// ignore
		}
	}

	private ShowToastTask mShowToastTask;
	private final class ShowToastTask implements Runnable {
		final int msg;
		final Object args;
		private ShowToastTask(@StringRes final int msg, final Object... args) {
			this.msg = msg;
			this.args = args;
		}

		@Override
		public void run() {
			try {
				if (mToast != null) {
					mToast.cancel();
					mToast = null;
				}
				if (args != null) {
					final String _msg = getString(msg, args);
					mToast = Toast.makeText(BaseActivity.this, _msg, Toast.LENGTH_SHORT);
				} else {
					mToast = Toast.makeText(BaseActivity.this, msg, Toast.LENGTH_SHORT);
				}
				mToast.show();
			} catch (final Exception e) {
				// ignore
			}
		}
	}

//================================================================================
	/**
	 * MessageDialogFragment 来自消息对话框的回调监听器
	 * @param dialog
	 * @param requestCode
	 * @param permissions
	 * @param result
	 */
	@SuppressLint("NewApi")
	@Override
	public void onMessageDialogResult(final MessageDialogFragment dialog, final int requestCode, final String[] permissions, final boolean result) {
        Log.e(TAG, "onMessageDialogResult" );
		if (result) {
			// 在消息对话框中按下 OK 时请求权限
			if (BuildCheck.isMarshmallow()) {
				requestPermissions(permissions, requestCode);
				return;
			}
		}
		//在消息对话框中取消且不是Android 6时，请自行检查并调用#checkPermissionResult
		for (final String permission: permissions) {
			checkPermissionResult(requestCode, permission, PermissionCheck.hasPermission(this, permission));
		}
	}

	/**
	 * 接收权限请求结果的方法
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);	// 我什么都没做
		final int n = Math.min(permissions.length, grantResults.length);
		for (int i = 0; i < n; i++) {
			checkPermissionResult(requestCode, permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
		}
	}

	/**
	 * 检查权限请求的结果
	 * 在这里，只是在无法获得权限的情况下，用 Toast 显示一条消息
	 * @param requestCode
	 * @param permission
	 * @param result
	 */
	protected void checkPermissionResult(final int requestCode, final String permission, final boolean result) {
		// 当您没有权限时显示消息
		if (!result && (permission != null)) {
			if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
				showToast(R.string.permission_audio);
			}
			if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
				showToast(R.string.permission_ext_storage);
			}
			if (Manifest.permission.INTERNET.equals(permission)) {
				showToast(R.string.permission_network);
			}
		}
	}

	// 动态权限请求的请求代码
	protected static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x12345;
	protected static final int REQUEST_PERMISSION_AUDIO_RECORDING = 0x234567;
	protected static final int REQUEST_PERMISSION_NETWORK = 0x345678;
	protected static final int REQUEST_PERMISSION_CAMERA = 0x537642;


	/**
	 * 检查您是否有外部存储的写权限
	 * 如果不是，则显示说明对话框。
	 * @return true 您对外部存储有写权限
	 */
	protected boolean checkPermissionWriteExternalStorage() {
        Log.e(TAG, "checkPermissionWriteExternalStorage:%X"+this );
		if (!PermissionCheck.hasWriteExternalStorage(this)) {
            Log.e(TAG, "checkPermissionWriteExternalStorage: ready showDialog" );
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                    R.string.per_title, R.string.per_storage,
				new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
            Log.e(TAG, "checkPermissionWriteExternalStorage: showDialog" );
			return false;
		}
		return true;

            //this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
            //return true;

	}

	/**
	 * 録音のパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 * @return true 録音のパーミッションが有る
	 * * 检查您是否有录音权限
	 * * 如果不是，则显示说明对话框。
	 * * @return true 你有录音权限
	 */
	protected boolean checkPermissionAudio() {
        Log.e(TAG, "checkPermissionAudio" );
		if (!PermissionCheck.hasAudio(this)) {
			Log.e(TAG, "checkPermissionAudio: ready showDialog" );
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_AUDIO_RECORDING,
				R.string.per_title, R.string.per_audio,
				new String[]{Manifest.permission.RECORD_AUDIO});
			Log.e(TAG, "checkPermissionAudio: showDialog" );
			return false;
		}
		return true;
	}



	/**
	 * ネットワークアクセスのパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 * @return true ネットワークアクセスのパーミッションが有る
	 */
	protected boolean checkPermissionNetwork() {
		if (!PermissionCheck.hasNetwork(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_NETWORK,
				R.string.per_title, R.string.permission_network_request,
				new String[]{Manifest.permission.INTERNET});
			return false;
		}
		return true;
	}

	/**
	 * カメラアクセスのパーミッションがあるかどうかをチェック
	 * なければ説明ダイアログを表示する
	 * @return true カメラアクセスのパーミッションが有る
	 */
	protected boolean checkPermissionCamera() {
		if (!PermissionCheck.hasCamera(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_CAMERA,
				R.string.per_title, R.string.per_camera,
				new String[]{Manifest.permission.CAMERA});
			return false;
		}
		return true;
	}

}
