package com.dyt.wcc.customize;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.dyt.wcc.BuildConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/7/18  10:03     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.customize     </p>
 */
public abstract class LanguageFactory {
	/*//点扬
	languageMap.put("zh-rCN", "中文");
			languageMap.put("en-rUS", "English");//英语
			languageMap.put("ru-rRU", "Русский");//俄文
			languageMap.put("de-rDE", "Deutsch");//德文
			languageMap.put("it-rIT", "Italiano");//意大利文
			languageMap.put("ko-rKR", "한국인");//韩语
			languageMap.put("ja-rJP", "日本");//日语
			languageMap.put("fr-rFR", "Français");//法语
			languageMap.put("es-rES", "Español");//西班牙语
			languageMap.put("fi-rFI", "Suomalainen");//芬兰语
			languageMap.put("pl-rPL", "Polski");//波兰语
			languageMap.put("pt-rPT", "Português");//葡萄牙语
			languageMap.put("sv-rSE", "Svenska");//瑞典语
	protected static final String[] language_dyt_array     = new String[]{"中文", "English"};
	//精明鼠
	protected static final String[] language_jms_array     = new String[]{"中文", "English","Русский", "Deutsch",
			"Italiano", "한국인", "日本",
			//法语fr-rFR、西班牙语es-rES、芬兰语fi-rFI、波兰语pl-rPL、葡萄牙语pt-rPT
			"Français","Español","Suomalainen","Polski","Português"};
	//迈测
	protected static final String[] language_mileseey_array     = new String[]{"中文", "English","Deutsch","Français","Italiano","Español"};

	//MTI 448
	protected static final String[] language_mti448_array     = new String[]{"中文", "English","Русский", "Deutsch",
			"Italiano", "한국인", "日本",
			//法语fr-rFR、西班牙语es-rES、芬兰语fi-rFI、波兰语pl-rPL、葡萄牙语pt-rPT, 瑞典语 sv-rSE
			"Français","Español","Suomalainen","Polski","Português","Svenska"};*/

	protected              Context  mContext;

	protected switchListener listener;

	public void setListener (switchListener listener) {
		this.listener = listener;
	}

	/**
	 * 默认语言，为null 的话，则设置为系统语言。
	 * eg: "CN" , "US"
	 */
	private String defaultLanguageStr = null;

	public String getDefaultLanguageStr () {
		return defaultLanguageStr;
	}
	public void setDefaultLanguageStr (String defaultLanguageStr) {
		this.defaultLanguageStr = defaultLanguageStr;
	}

	/**
	 * 切换的回调监听器函数
	 */
	public interface switchListener {
		/**
		 * 点击 item
		 *
		 * @param index
		 */
		void onItemClickListener (DialogInterface dialog, int index);
	}

	public LanguageFactory (Context mContext) {
		this.mContext = mContext;
		createLanguageHashMap(BuildConfig.FLAVOR);
	}

	 /**
	 * 获取语言String 通过传入下标
	 *
	 * @param index 下标（如果超过当前 数据长度，则返回一个默认值：英文）
	 * @return 返回 显示的语言string
	 *//*
	public abstract CharSequence getLanguageByIndex (@NonNull int index);*/

	/**
	 * 生成 AlertDialog
	 *
	 * @return
	 */
	public abstract AlertDialog createAlertDialog ();

	protected HashMap<String,String> languageMap = new HashMap<>();
	protected List<String> listKeys = new ArrayList<>();
	protected List<String> listValues = new ArrayList<>();

	public List<String> getListKeys () {
		return listKeys;
	}

	public List<String> getListValues () {
		return listValues;
	}

	public HashMap<String, String> getLanguageMap () {
		return languageMap;
	}
	/**
	 *
	 * @param configFlavor
	 * @return
	 */
	public abstract HashMap<String,String> createLanguageHashMap(String configFlavor);


	/**
	 * 生成各自不同的监听器
	 *
	 * @return 返回监听器
	 */
	public abstract android.content.DialogInterface.OnClickListener createDialogListener ();

	/**
	 * 无参获取 语言数组
	 *
	 * @return
	 */
	/*public abstract String[] getLanguageArray ();*/
}
