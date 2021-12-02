package com.dyt.wcc.common.base;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.dyt.wcc.common.utils.KeyboardsUtils;

import java.lang.ref.WeakReference;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/8  15:28     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.base.ui     </p>
 */
public abstract class BaseActivity<T extends ViewDataBinding> extends AppCompatActivity {
	protected final String  TAG = this.getClass().getSimpleName();

	protected WeakReference<Context> mContext;
	protected     T       mDataBinding;//绑定的布局View
	protected boolean isDebug = true;
	@Override
	protected void onCreate (@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDataBinding = DataBindingUtil.setContentView(this,bindingLayout());//绑定布局

		mContext = new WeakReference<>(this);
		initView();
	}
	////设置绑定布局
	protected abstract int bindingLayout();
	//初始化控件
	protected abstract void initView();

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
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (ev.getAction() ==  MotionEvent.ACTION_DOWN ) {
			View view = getCurrentFocus();
			if (KeyboardsUtils.isShouldHideKeyBord(view, ev)) {
				KeyboardsUtils.hintKeyBoards(view);
			}
		}
		return super.dispatchTouchEvent(ev);
	}
}
