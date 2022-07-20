package com.dyt.wcc.customize.neutral;

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.dyt.wcc.constans.DYConstants;
import com.dyt.wcc.customize.LanguageFactory;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/7/18  15:59     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.customize.dyt     </p>
 */
public class NeutralLanguageFactory extends LanguageFactory {
	public NeutralLanguageFactory (Context mContext) {
		super(mContext);
	}

	/**
	 * 获取语言String 通过传入下标
	 *
	 * @param index 下标（如果超过当前 数据长度，则返回一个默认值：英文）
	 * @return 返回 显示的语言string
	 */
	@Override
	public CharSequence getLanguageByIndex (@NonNull int index) {
		if (index < language_neutral_array.length) {
			return language_neutral_array[index];
		} else {
			return language_neutral_array[0];
		}
	}

	/**
	 * 生成 AlertDialog
	 *
	 * @return
	 */
	@Override
	public AlertDialog createAlertDialog () {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		return builder.setSingleChoiceItems(LanguageFactory.language_neutral_array,
				mContext.getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE).getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0),
				createDialogListener()).create();
	}

	/**
	 * 生成各自不同的监听器
	 *
	 * @return 返回监听器
	 */
	@Override
	public DialogInterface.OnClickListener createDialogListener () {
		return (dialog, which) -> {
			if (listener != null) {
				listener.onItemClickListener(dialog, which);
			}
		};
	}

	/**
	 * 无参获取 语言数组
	 *
	 * @return
	 */
	@Override
	public String[] getLanguageArray () {
		return language_neutral_array;
	}
}
