package com.dyt.wcc.common.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;

import com.dyt.wcc.common.BuildConfig;

import java.lang.ref.WeakReference;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/8  14:59     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.common.base.ui     </p>
 */
public abstract class BaseFragment<T extends ViewDataBinding> extends Fragment {
	protected WeakReference <Context> mContext;
	protected final String  TAG = this.getClass().getSimpleName();
	protected T mDataBinding;
	protected boolean isDebug = BuildConfig.DEBUG;

	@Nullable
	@Override
	public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mDataBinding = DataBindingUtil.inflate(inflater,bindingLayout(),container,false);
		View view = mDataBinding.getRoot();
		mContext = new WeakReference<>(getActivity());
		initView();
		return view;
	}

	////设置绑定布局
	protected abstract int bindingLayout();
	//初始化控件
	protected abstract void initView();
}
