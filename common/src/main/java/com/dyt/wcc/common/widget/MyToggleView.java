package com.dyt.wcc.common.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;

import androidx.annotation.Nullable;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/16  15:07     </p>
 * <p>Description：@开关         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.widget     </p>
 */
public class MyToggleView extends View {
	//是否显示文字
	private boolean isShowText = true;
	private int textSize ;
	private int textColor ;
	//按钮圆图  及其  背景颜色。
	private int toggleButtonPic;
	private int toggleBgPicUnselect;
	private int toggleBgPicSelect;
	//控件的宽高
	private int viewWidth;
	private int viewHeight;
	//进出动画
	private Animation enterAnimate;
	private Animation exitAnimate;


	public MyToggleView (Context context) {
		super(context);
	}

	public MyToggleView (Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public MyToggleView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	private void initView(){

	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	}

	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
	}
}
