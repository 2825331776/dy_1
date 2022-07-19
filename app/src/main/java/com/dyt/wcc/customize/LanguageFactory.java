package com.dyt.wcc.customize;

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/7/18  10:03     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.customize     </p>
 */
public abstract class LanguageFactory {
	protected static final String[] language_dyt_array    = new String[]{"中文", "English"};
	protected static final String[] language_victor_array = new String[]{"中文", "English"};
	protected static final String[] language_qianli_array = new String[]{"中文", "English"};
	protected static final String[] language_teslong_array = new String[]{"中文", "English"};
	protected static final String[] language_jms_array    = new String[]{"中文", "English", "Deutsch", "意大利语", "韩语", "日语",};
	protected              Context  mContext;

	protected switchListener listener;

	public void setListener (switchListener listener) {
		this.listener = listener;
	}

	/**
	 * 切换的回调监听器函数
	 */
	public interface switchListener {
		/**
		 * 点击 嘎嘎
		 * @param index
		 */
		void onItemClickListener (DialogInterface dialog, int index);
	}

	public LanguageFactory (Context mContext) {
		this.mContext = mContext;
	}

	/**
	 * 获取语言String 通过传入下标
	 *
	 * @param index 下标（如果超过当前 数据长度，则返回一个默认值：英文）
	 * @return 返回 显示的语言string
	 */
	public abstract CharSequence getLanguageByIndex (@NonNull int index);

	/**
	 * 生成 AlertDialog
	 *
	 * @return
	 */
	public abstract AlertDialog createAlertDialog ();

	/**
	 * 生成各自不同的监听器
	 *
	 * @return 返回监听器
	 */
	public abstract android.content.DialogInterface.OnClickListener createDialogListener ();

	/**
	 * 无参获取 语言数组
	 * @return
	 */
	public abstract String [] getLanguageArray();
}
