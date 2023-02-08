package com.dytest.wcc.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.dytest.wcc.common.R;


/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/11  11:11     </p>
 * <p>Description：自定义开关控件         </p>
 * <p>PackagePath: com.dyt.wcc.widget     </p>
 */
public class MyToggle extends LinearLayout {
	private static final String       TAG = "MyToggle.TAG";
	private              SwitchCompat switch_toggle_define;
	private              TextView     tv_toggle_define;
	private              LinearLayout ll_toggle_define;

	private boolean clickEnable = false;

	private String defaultString = "开关";
	private int    textSize      = 10;//默认字体大小

	private int textDefaultColor = Color.WHITE;
	private int textSelectColor  = Color.RED;

	private OnWidgetStateCheckedListener widgetStateCheckedListener;

	public MyToggle (Context context) {
		this(context, null);
		//		initView(context,null);
		//		initData();
		Log.e(TAG, "MyToggle: 000");
	}

	public MyToggle (Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initView(context, attrs);
		initData();
		Log.e(TAG, "MyToggle: 111");
	}

	private void initData () {
		tv_toggle_define.setTextSize(textSize);
		tv_toggle_define.setText(defaultString);
		tv_toggle_define.setTextColor(textDefaultColor);

		Log.e(TAG, "initView: " + clickEnable);
		switch_toggle_define.setChecked(clickEnable);

		if (clickEnable) {
			tv_toggle_define.setTextColor(textSelectColor);
		} else {
			tv_toggle_define.setTextColor(textDefaultColor);
		}
	}

	private void initView (Context context, AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MyToggle);

		textDefaultColor = typedArray.getColor(R.styleable.MyToggle_toggle_text_default_color, Color.WHITE);
		textSelectColor = typedArray.getColor(R.styleable.MyToggle_toggle_text_select_color, Color.RED);
		textSize = typedArray.getInt(R.styleable.MyToggle_toggle_text_size, textSize);
		defaultString = typedArray.getString(R.styleable.MyToggle_toggle_text_info);

		typedArray.recycle();

		View view = LayoutInflater.from(context).inflate(R.layout.layout_define_my_toggle, this, true);
		tv_toggle_define = view.findViewById(R.id.tv_toggle_ll_define);
		switch_toggle_define = view.findViewById(R.id.switch_toggle_ll_define);
		ll_toggle_define = view.findViewById(R.id.ll_toggle_define);

		ll_toggle_define.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (View v) {
				clickEnable = !clickEnable;
				if (clickEnable) {
					tv_toggle_define.setTextColor(textSelectColor);
				} else {
					tv_toggle_define.setTextColor(textDefaultColor);
				}
				switch_toggle_define.setChecked(clickEnable);
				//				switch_toggle_define.setSaveEnabled(true);

				if (widgetStateCheckedListener != null) {
					widgetStateCheckedListener.onStateChecked(clickEnable);
				}
			}
		});

		//		Log.e(TAG, "initView: ");

		//		ll_toggle_define.setClickable(clickEnable);


	}

	//	public MyToggle (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
	//		super(context, attrs, defStyleAttr);
	//		initView(context,attrs);
	//	}
	//	public MyToggle (Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
	//		super(context, attrs, defStyleAttr, defStyleRes);
	//
	//		initView(context,attrs);
	//	}

	public void setWidgetStateCheckedListener (OnWidgetStateCheckedListener checkedListener) {
		this.widgetStateCheckedListener = checkedListener;
	}

	//	public void setClickEnable (boolean clickEnable) {
	////		ll_toggle_define.setClickable(clickEnable);
	//		this.clickEnable = clickEnable;
	////		invalidate();
	//	}

	//	@Override
	//	protected void onRestoreInstanceState (Parcelable state) {
	//		Log.e(TAG, "onRestoreInstanceState: ");
	//
	//		Bundle bundle = (Bundle) state;
	//		Parcelable superParcelable = bundle.getParcelable("toggleData");
	//
	//		clickEnable = bundle.getBoolean("toggleSelect");
	//
	//		Log.e(TAG, "onRestoreInstanceState: " + clickEnable);
	//		switch_toggle_define.setChecked(clickEnable);
	//		if (clickEnable) {
	//			tv_toggle_define.setTextColor(textSelectColor);
	//		} else {
	//			tv_toggle_define.setTextColor(textDefaultColor);
	//			//			switch_toggle_define.setSaveEnabled(clickEnable);
	//		}
	//		super.onRestoreInstanceState(superParcelable);
	//	}
	//
	//	@Nullable
	//	@Override
	//	protected Parcelable onSaveInstanceState () {
	//		Log.e(TAG, "onSaveInstanceState: ");
	//
	//		Bundle bundle = new Bundle();
	//		Parcelable parcelable = super.onSaveInstanceState();
	//		bundle.putParcelable("toggleData",parcelable);
	//		bundle.putBoolean("toggleSelect",clickEnable);
	//
	//		switch_toggle_define.setSaveEnabled(true);
	//		return bundle;
	//	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		//		Log.e(TAG, "onMeasure: ");

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		//		int childCount = getChildCount();
		//		int heightTotal = 0;
		//		int widthTotal = 0;

		//		if (childCount>0){
		//			for (int i = 0 ; i < childCount ; i ++){
		//				View childView = getChildAt(i);
		//				heightTotal += childView.getHeight();
		//				widthTotal = widthTotal > childView.getWidth()? widthTotal : childView.getWidth();
		//			}
		//		}
		setMeasuredDimension(widthSize, heightSize);
		switch_toggle_define.setSwitchMinWidth(tv_toggle_define.getMeasuredWidth());
	}


	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
	}

	public interface OnWidgetStateCheckedListener {
		void onStateChecked (boolean widgetState);
	}

}
