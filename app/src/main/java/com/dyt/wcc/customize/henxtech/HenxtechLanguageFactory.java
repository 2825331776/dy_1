package com.dyt.wcc.customize.henxtech;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.dyt.wcc.BuildConfig;
import com.dyt.wcc.constans.DYConstants;
import com.dyt.wcc.customize.LanguageFactory;

import java.util.HashMap;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/7/18  15:59     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.customize.dyt     </p>
 */
public class HenxtechLanguageFactory extends LanguageFactory {
	public HenxtechLanguageFactory (Context mContext) {
		super(mContext);
	}


	/**
	 * 生成 AlertDialog
	 * @return
	 */
	@Override
	public AlertDialog createAlertDialog () {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		return builder.setSingleChoiceItems(languageMap.values().toArray(new String[]{}),
				mContext.getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE).getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0)
				, createDialogListener()).create();
	}

	@Override
	public HashMap<String, String> createLanguageHashMap (String configFlavor) {
		if (BuildConfig.FLAVOR == DYConstants.COMPANY_HENXTECH && languageMap.isEmpty()) {
			languageMap.put("zh-rCN", "中文");
			languageMap.put("en-rUS", "English");
		}
		if (listKeys.isEmpty() && listValues.isEmpty()) {
			listKeys.addAll(languageMap.keySet());
			listValues.addAll(languageMap.values());
		}
		Log.e("--COMPANY_HENXTECH-", "createLanguageHashMap: -------------keys---" + listKeys.toString() + " values-->" + listValues.toString());
		return languageMap;
	}

	/**
	 * 生成各自不同的监听器
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

}
