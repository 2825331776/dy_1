package com.dyt.wcc.baselib.ui.doodle.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.dyt.wcc.baselib.R;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.forward.androids.utils.StatusBarUtil;

/**
 * Created by huangziwei on 2017/4/21.
 */

public class DialogController {
	public static Dialog showEnterCancelDialog (Activity activity, String title, String msg, final View.OnClickListener enterClick,
	                                            final View.OnClickListener cancelClick) {
		return showMsgDialog(activity, title, msg, activity.getString(R.string.doodle_cancel), activity.getString(R.string.doodle_enter),
				enterClick, cancelClick);
	}

	public static Dialog showEnterDialog (Activity activity, String title, String msg, final View.OnClickListener enterClick) {
		return showMsgDialog(activity, title, msg, null, activity.getString(R.string.doodle_enter), enterClick, null);
	}

	public static Dialog showMsgDialog (Activity activity, String title, String msg, String btn01, String btn02,
	                                    final View.OnClickListener enterClick, final View.OnClickListener cancelClick) {

		final Dialog dialog = getDialog(activity);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		StatusBarUtil.setStatusBarTranslucent(dialog.getWindow(), true, false);
		dialog.show();
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);

		final Dialog finalDialog = dialog;

		View view = View.inflate(activity, R.layout.doodle_dialog, null);
		dialog.setContentView(view);

		if (TextUtils.isEmpty(title)) {
			dialog.findViewById(R.id.dialog_title).setVisibility(View.GONE);
			dialog.findViewById(R.id.dialog_list_title_divider).setVisibility(View.GONE);
		} else {
			TextView titleView = (TextView) dialog.findViewById(R.id.dialog_title);
			titleView.setText(title);
		}

		if (TextUtils.isEmpty(msg)) {
			dialog.findViewById(R.id.dialog_enter_msg).setVisibility(View.GONE);
		} else {
			TextView titleView = (TextView) dialog.findViewById(R.id.dialog_enter_msg);
			titleView.setText(Html.fromHtml(msg));
		}

		if (TextUtils.isEmpty(btn01)) {
			dialog.findViewById(R.id.dialog_enter_btn_01).setVisibility(View.GONE);
		} else {
			TextView textView = (TextView) dialog.findViewById(R.id.dialog_enter_btn_01);
			textView.setText(btn01);
		}

		if (TextUtils.isEmpty(btn02)) {
			dialog.findViewById(R.id.dialog_enter_btn_02).setVisibility(View.GONE);
		} else {
			TextView textView = (TextView) dialog.findViewById(R.id.dialog_enter_btn_02);
			textView.setText(btn02);
		}

		View.OnClickListener onClickListener = new View.OnClickListener() {
			public void onClick (View v) {
				if (v.getId() == R.id.dialog_bg) {
					finalDialog.dismiss();
				} else if (v.getId() == R.id.dialog_enter_btn_02) {
					finalDialog.dismiss();
					if (enterClick != null) {
						enterClick.onClick(v);
					}
				} else if (v.getId() == R.id.dialog_enter_btn_01) {
					finalDialog.dismiss();
					if (cancelClick != null) {
						cancelClick.onClick(v);
					}
				}
			}
		};
		view.findViewById(R.id.dialog_bg).setOnClickListener(onClickListener);
		view.findViewById(R.id.dialog_enter_btn_01).setOnClickListener(onClickListener);
		view.findViewById(R.id.dialog_enter_btn_02).setOnClickListener(onClickListener);

		return dialog;
	}

	public static Dialog showInputTextDialog (Activity activity, final String text, final View.OnClickListener enterClick,
	                                          final View.OnClickListener cancelClick) {
		boolean fullScreen = (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
		final Dialog dialog = getDialog(activity);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		dialog.show();

		ViewGroup container = (ViewGroup) View.inflate(activity, R.layout.doodle_create_text, null);

		container.setOnClickListener(v -> dialog.dismiss());
		dialog.setContentView(container);
		if (fullScreen) {
			DrawUtil.assistActivity(dialog.getWindow());
		}

		final EditText textView = (EditText) container.findViewById(R.id.doodle_selectable_edit);
		final View cancelBtn = container.findViewById(R.id.doodle_text_cancel_btn);
		final TextView enterBtn = (TextView) container.findViewById(R.id.doodle_text_enter_btn);

		textView.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm.isActive()) {
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
				dialog.dismiss();
				if (enterClick != null) {
					enterBtn.setTag((textView.getText() + "").trim());
					enterClick.onClick(enterBtn);
				}
			}
			return false;
		});

		textView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged (CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged (CharSequence s, int start, int before, int count) {
				String text = (textView.getText() + "").trim();
				if (TextUtils.isEmpty(text)) {
					enterBtn.setEnabled(false);
					enterBtn.setTextColor(0xffb3b3b3);
				} else {
					enterBtn.setEnabled(true);
					enterBtn.setTextColor(0xff232323);
				}
			}

			@Override
			public void afterTextChanged (Editable s) {

			}
		});
		textView.setText(text == null ? "" : text);
		//设置默认弹出 输入法
		textView.setFocusable(true);
		textView.requestFocus();

		Timer timer = new Timer(); //设置定时器
		timer.schedule(new TimerTask() {
			@Override
			public void run () { //弹出软键盘的代码
				InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(textView, InputMethodManager.RESULT_SHOWN);
			}
		}, 300);

		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				dialog.dismiss();
				if (cancelClick != null) {
					cancelClick.onClick(cancelBtn);
				}
			}
		});

		enterBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				dialog.dismiss();
				if (enterClick != null) {
					enterBtn.setTag((textView.getText() + "").trim());
					enterClick.onClick(enterBtn);
				}
			}
		});
		return dialog;
	}

	public static Dialog showSelectImageDialog (Activity activity, final ImageSelectorView.ImageSelectorListener listener) {
		final Dialog dialog = getDialog(activity);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		dialog.show();
		ViewGroup container = (ViewGroup) View.inflate(activity, R.layout.doodle_create_bitmap, null);
		container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				dialog.dismiss();
			}
		});
		dialog.setContentView(container);

		ViewGroup selectorContainer = (ViewGroup) dialog.findViewById(R.id.doodle_image_selector_container);
		ImageSelectorView selectorView = new ImageSelectorView(activity, false, 1, null, new ImageSelectorView.ImageSelectorListener() {
			@Override
			public void onCancel () {
				dialog.dismiss();
				if (listener != null) {
					listener.onCancel();
				}
			}

			@Override
			public void onEnter (List<String> pathList) {
				dialog.dismiss();
				if (listener != null) {
					listener.onEnter(pathList);
				}

			}
		});
		selectorView.setColumnCount(4);
		selectorContainer.addView(selectorView);
		return dialog;
	}

	private static Dialog getDialog (Activity activity) {
		boolean fullScreen = (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
		Dialog dialog = null;
		if (fullScreen) {
			dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
		} else {
			dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
		}
		return dialog;
	}

}
