package com.dyt.wcc.jms.widget;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/4/27  17:56     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.preview     </p>
 */
public abstract class BaseDialogFragment extends DialogFragment {

	protected     Context        mContext;
	protected     View           view;
	private final DialogPosition dialogPosition;
	private final boolean        canceledOnTouchOutside;

	/**
	 * 初始化器
	 *
	 * @param dialogPosition         Dialog位置
	 * @param canceledOnTouchOutside 点击外部是否关闭Dialog
	 */
	public BaseDialogFragment (DialogPosition dialogPosition, boolean canceledOnTouchOutside) {
		this.dialogPosition = dialogPosition;
		this.canceledOnTouchOutside = canceledOnTouchOutside;
	}

	@Override
	public void onActivityCreated (@Nullable Bundle savedInstanceState) {
		Window window = Objects.requireNonNull(getDialog()).getWindow();
		window.getDecorView().setPadding(0, 0, 0, 0);
		WindowManager.LayoutParams attributes = window.getAttributes();
		switch (dialogPosition) {        // Dialog位置
			case CENTER:
				attributes.width = WindowManager.LayoutParams.WRAP_CONTENT;
				attributes.height = WindowManager.LayoutParams.WRAP_CONTENT;
				attributes.gravity = Gravity.CENTER;
				break;
			case BOTTOM:        // 底部Dialog的宽度会自动适应全屏
				attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
				attributes.height = WindowManager.LayoutParams.WRAP_CONTENT;
				attributes.gravity = Gravity.BOTTOM;
				break;
		}
		window.setAttributes(attributes);
		super.onActivityCreated(savedInstanceState);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog (@Nullable Bundle savedInstanceState) {
		Dialog dialog = new Dialog(getActivity(), getDialogStyleId());
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);    // 去掉标题栏
		dialog.setCanceledOnTouchOutside(canceledOnTouchOutside);    // 点击外部是否关闭Dialog
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(getLayoutId(), container, false);
		mContext = getActivity();
		this.initView();
		this.initData();
		this.initListener();
		return view;
	}

	/**
	 * 布局Id
	 */
	protected abstract int getLayoutId ();

	/**
	 * Dialog样式Id
	 */
	protected abstract int getDialogStyleId ();

	/**
	 * 初始化View
	 */
	protected abstract void initView ();

	/**
	 * 初始化数据
	 */
	protected abstract void initData ();

	/**
	 * 初始化监听器
	 */
	protected abstract void initListener ();

	/**
	 * Dialog位置
	 * CENTER：居中
	 * BOTTOM：底部
	 * TOP;顶部
	 */
	public enum DialogPosition {
		CENTER, BOTTOM, TOP
	}

	/**
	 * 弹出软键盘
	 * PS：若要使用此方法自动弹出软键盘，dialog样式必须配置<item name="android:windowSoftInputMode">stateVisible</item>
	 */
	public void openKeyboard (View view) {
		// 获取焦点
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.requestFocus();
		// 弹出软键盘
		InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
	}
}


