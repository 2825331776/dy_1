package com.dyt.wcc.dytpir.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.dyt.wcc.dytpir.R;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/11  11:11     </p>
 * <p>Description：自定义开关控件         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.widget     </p>
 */
public class MyToggle extends LinearLayout {
	private static final String TAG = "MyToggle.TAG";
	private Switch switch_toggle_define;
	private TextView     tv_toggle_define;
	private LinearLayout ll_toggle_define;

	private boolean clickEnable = false;

	private String defaultString = "开关";
	private int textSize = 10;//默认字体大小

	private int textDefaultColor = Color.WHITE;
	private int textSelectColor = Color.RED;

	private OnWidgetStateCheckedListener widgetStateCheckedListener;

	public MyToggle (Context context) {
		this(context,null);
	}
	public MyToggle (Context context, @Nullable AttributeSet attrs) {
		this(context, attrs,0);
	}
	public MyToggle (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr,0);
	}
	public MyToggle (Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		TypedArray typedArray  = context.obtainStyledAttributes(attrs, R.styleable.MyToggle);

		textDefaultColor = typedArray.getColor(R.styleable.MyToggle_toggle_text_default_color,Color.WHITE);
		textSelectColor = typedArray.getColor(R.styleable.MyToggle_toggle_text_select_color,Color.RED);
		textSize = typedArray.getInt(R.styleable.MyToggle_toggle_text_size,textSize);
		defaultString = typedArray.getString(R.styleable.MyToggle_toggle_text_info);

		typedArray.recycle();
		initView(context);
	}

	public void setWidgetStateCheckedListener (OnWidgetStateCheckedListener checkedListener) {
		this.widgetStateCheckedListener = checkedListener;
	}

	public void setClickEnable (boolean clickEnable) {
		ll_toggle_define.setClickable(clickEnable);
		this.clickEnable = clickEnable;
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		Log.e(TAG, "onMeasure: ");

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
		setMeasuredDimension(widthSize,heightSize);
		switch_toggle_define.setSwitchMinWidth(tv_toggle_define.getMeasuredWidth());
	}

	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

	}

	private void initView(Context context){
		LayoutInflater.from(context).inflate(R.layout.layout_define_my_toggle,this,true);
		tv_toggle_define = findViewById(R.id.tv_toggle_ll_define);
		switch_toggle_define = findViewById(R.id.switch_toggle_ll_define);
		ll_toggle_define = findViewById(R.id.ll_toggle_define);

		tv_toggle_define.setTextSize(textSize);
		tv_toggle_define.setText(defaultString);
		tv_toggle_define.setTextColor(textDefaultColor);

//		switch_toggle_define.setClickable(false);
		switch_toggle_define.setChecked(false);

//		ll_toggle_define.setClickable(clickEnable);
		ll_toggle_define.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (View v) {
				clickEnable = !clickEnable;
				if (clickEnable){
					tv_toggle_define.setTextColor(textSelectColor);
				}else {
					tv_toggle_define.setTextColor(textDefaultColor);
				}

				switch_toggle_define.setChecked(clickEnable);

				if (widgetStateCheckedListener!= null){
					widgetStateCheckedListener.onStateChecked(clickEnable);
				}
			}
		});

	}
	public interface OnWidgetStateCheckedListener{
		void onStateChecked(boolean widgetState);
	}

}
