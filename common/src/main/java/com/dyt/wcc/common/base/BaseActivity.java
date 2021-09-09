package com.dyt.wcc.common.base;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/8  15:28     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.common.base.ui     </p>
 */
public abstract class BaseActivity<T extends ViewDataBinding> extends AppCompatActivity {
	protected final String  TAG = this.getClass().getSimpleName();

	protected       Context mContext;
	protected     T       mDataBinding;//绑定的布局View
	@Override
	protected void onCreate (@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDataBinding = DataBindingUtil.setContentView(this,bindingLayout());//绑定布局

		mContext = this;
		initView();
	}
	////设置绑定布局
	protected abstract int bindingLayout();
	//初始化控件
	protected abstract void initView();

}
