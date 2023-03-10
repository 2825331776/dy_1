package com.dytest.wcc.ui.base;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.dytest.wcc.common.utils.KeyboardsUtils;

import java.lang.ref.WeakReference;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/8  15:28     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.base.ui     </p>
 */
public abstract class BaseActivity<T extends ViewDataBinding> extends AppCompatActivity {
	protected final String TAG = this.getClass().getSimpleName();

	protected WeakReference<Context> mContext;
	protected T                      mDataBinding;//绑定的布局View
	protected boolean                isDebug = true;
	protected Toast                  mToast;
	protected int                    mRotation;

	@Override
	protected void onCreate (@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDataBinding = DataBindingUtil.setContentView(this, bindingLayout());//绑定布局
		mContext = new WeakReference<>(this);
		//		onCreateLanguageStr = getResources().getConfiguration().locale.getLanguage();
		//		Log.e(TAG, "onCreate: =====onCreateLanguageStr===" + onCreateLanguageStr);


		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Log.e(TAG, "============onCreate: ====================================");

		mToast = Toast.makeText(mContext.get(), "", Toast.LENGTH_SHORT);
		//		mToast.setGravity();
		initView();
	}

	//	//实现一个当前获取sp返回 local.xxx.getLanguage();
	//	protected String onCreateLanguageStr = "";
	protected abstract String getLanguageStr ();

	////设置绑定布局
	protected abstract int bindingLayout ();

	//初始化控件
	protected abstract void initView ();

	public int getMRotation () {
		return mRotation;
	}

	public void setMRotation (int mRotation) {
		this.mRotation = mRotation;
		mToast.getView().setRotation(mRotation);
	}

	protected void showToast (int resId) {
		//		mToast.cancel();
		mToast.setText(resId);
		mToast.show();
	}

	protected void showToast (String str) {
		//		mToast.cancel();
		mToast.setText(str);
		mToast.show();
	}

	protected void hideInput (IBinder token) {
		InputMethodManager im = (InputMethodManager) mContext.get().getSystemService(Context.INPUT_METHOD_SERVICE);
		im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
	}

	@Override
	public void onBackPressed () {
		super.onBackPressed();
	}


	/**
	 * 点击非编辑区域收起键盘
	 * 获取点击事件
	 * CSDN-深海呐
	 */
	@CallSuper
	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			View view = getCurrentFocus();
			if (KeyboardsUtils.isShouldHideKeyBord(view, ev)) {
				KeyboardsUtils.hintKeyBoards(view);
			}
		}
		return super.dispatchTouchEvent(ev);
	}

	//	public ContextWrapper wrap (Context context) {
	//		Resources res = context.getResources();
	//		Configuration configuration = res.getConfiguration();
	//		//获得你想切换的语言，可以用SharedPreferences保存读取
	//		Locale newLocale = new Locale(getLanguageStr());
	//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
	//			configuration.setLocale(newLocale);
	//			LocaleList localeList = new LocaleList(newLocale);
	//			LocaleList.setDefault(localeList);
	//			configuration.setLocales(localeList);
	//			context = context.createConfigurationContext(configuration);
	//		}
	//		else {
	//			configuration.locale = newLocale;
	//			res.updateConfiguration(configuration, res.getDisplayMetrics());
	//		}
	//		return new ContextWrapper(context);
	//	}

	//		@Override
	//		protected void attachBaseContext(Context newBase) {
	//			if (isSupportMultiLanguage()) {
	//				String language = LanguageSp.getLanguage(newBase);
	//				Context context = LanguageUtil.attachBaseContext(newBase, language);
	//				final Configuration configuration = context.getResources().getConfiguration();
	//				final ContextThemeWrapper wrappedContext = new ContextThemeWrapper(context,
	//						R.style.Theme_AppCompat_Empty) {
	//					@Override
	//					public void applyOverrideConfiguration(Configuration overrideConfiguration) {
	//						if (overrideConfiguration != null) {
	//							overrideConfiguration.setTo(configuration);
	//						}
	//						super.applyOverrideConfiguration(overrideConfiguration);
	//					}
	//				};
	//				super.attachBaseContext(wrappedContext);
	//			} else {
	//				super.attachBaseContext(newBase);
	//			}
	//		}

	@Override
	public void applyOverrideConfiguration (Configuration overrideConfiguration) {
		// 兼容androidX在部分手机切换语言失败问题
		if (overrideConfiguration != null) {
			int uiMode = overrideConfiguration.uiMode;
			overrideConfiguration.setTo(getBaseContext().getResources().getConfiguration());
			overrideConfiguration.uiMode = uiMode;
		}
		super.applyOverrideConfiguration(overrideConfiguration);
	}
}
