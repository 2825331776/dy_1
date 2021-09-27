package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/27  9:56     </p>
 * <p>Description：三类可以拖动的View</p>
 * <p>PackagePath: com.dyt.wcc.common.widget     </p>
 */
public class MyMoveWidget extends RelativeLayout {
	private static final boolean isDebug = true;
	private static final String TAG = "MyMoveWidget";
	private Bitmap maxTempBt, minTempBt;//最小温度，最大温度图片（单点只有最小温度的图片）
	private MyDefineView view;

	private int mMinHeight;//最小高度像素点个数   //矩阵类型独有
	private int mMinWidth;//最小宽度像素点个数    //矩阵和线独有

	private boolean isSelected = false;//是否被选中
	private boolean isShowBg = false;//是否显示背景

	private View toolView;//工具栏



	public MyMoveWidget (Context context) {
		this(context,null);
	}
	public MyMoveWidget (Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}
	public MyMoveWidget (Context context, AttributeSet attrs, int defStyleAttr) { this(context, attrs, defStyleAttr,0); }
	public MyMoveWidget (Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initView();
		initPaint();

	}
	private void initView(){}
	private void initPaint(){}

	public MyDefineView getView () {
		return view;
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
	}

	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}


	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}
	//

	@Override
	public boolean onInterceptTouchEvent (MotionEvent ev) {//ViewGroup true拦截 super不拦截  false //固定这个返回super，否则无法响应工具栏的事件
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (view.getDrawType() == 3){

		}else {
			if (isDebug)Log.e(TAG, "onTouchEvent: getDrawType != 3 不为矩形" );
		}


		return super.onTouchEvent(event);
	}
}
