package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/6/24  14:01     </p>
 * <p>Description：所有绘制view的父类，对内 抽取每个子view的共同部分。对外抽取 操作的业务逻辑部分。</p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public abstract class MeasureTemperatureView extends View {

	public static final int MEASURE_VIEW_POINT     = 0;
	public static final int MEASURE_VIEW_LINE      = 1;
	public static final int MEASURE_VIEW_RECTANGLE = 2;

	public MeasureTemperatureView (Context context) {
		super(context);
	}
	//与父布局间距
	protected int containerBorderLength = 0;
	//绘制工具栏的paint
	protected Paint toolsPaint ;

	public MeasureTemperatureView (Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public MeasureTemperatureView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public MeasureTemperatureView (Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	/**
	 * 子类 业务操作的接口
	 */
	public interface ChildViewOperate {

		void onAttachView2Parent ();
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}
}
