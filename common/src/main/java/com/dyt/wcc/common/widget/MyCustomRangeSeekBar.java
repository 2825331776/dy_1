package com.dyt.wcc.common.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/11/8  16:33     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget     </p>
 */
public class MyCustomRangeSeekBar extends View {
	//整个屏幕的最高最低温度。
	private float mMaxTemp;
	private float mMinTemp;

	private DecimalFormat df = new DecimalFormat("0.0");//构造方法的字符格式这里如果小数不足2位,会以0补足.

	//滑块的最高最低温度 及其 滑块的图片
	private float mThumbMaxTemp;
	private float mThumbMinTemp;
	//滑块bitmap
	private Bitmap mThumbMaxImage;//滑块图像 对应表最大值
	private Bitmap mThumbMinImage;//滑块图像 对应表最小值
	private float mThumbHeight;//滑动条高度
	private float mThumbWidth;//滑动条宽度

	//绘制在屏幕上的的最高温度 ，和 最低温度。模式0时，绘制的温度等于实时的 屏幕最高最低温。模式1时，需要实时计算这个的值。
	private float rangeMaxTemp;
	private float rangeMinTemp;
	private int widgetMode;//控件的绘制模式。0代表简单的绘制最高最低温。非零代表固定温度条模式。

	//progress bar 选中背景
	private List<Bitmap> mProgressBarSelBg;//整体的背景颜色

	//最小值（绝对），取值0-100
	public static float mAbsoluteMinValue;
	//最大值（绝对）
	public static float mAbsoluteMaxValue;

	//已选标准（占滑动条百分比）最小值 。相对于整体的高度。
	private double mPercentSelectedMinValue = 0d;
	//已选标准（占滑动条百分比）最大值。相对于整体的高度。
	private double mPercentSelectedMaxValue = 1d;

	private RectF mProgressBarRect;//整体的背景
	private RectF mProgressBarSelRect;//色板覆盖的矩形

	//是否可以滑动
	private boolean mEnable = true;

	//当前事件处理的thumb滑块 ，区分按下的操作是最小值 还是最大值的滑块
	private MyCustomRangeSeekBar.Thumb mPressedThumb = null;
	//滑块事件
	private ThumbListener            mThumbListener;
	public static final int UPDATE_VALUE = 1005;

	private int stripWidth;//那个滑动的 条条的 宽度

	//控件最小高度
	private final int MIN_HEIGHT = 400;

	/**
	 * Thumb枚举， 最大或最小
	 */
	private enum Thumb {
		MIN, MAX
	}
	/**
	 * 滑块事件
	 */
	public interface ThumbListener {
		//void onClickMinThumb(Number max, Number min);

		//void onClickMaxThumb();
		//最小值滑块移动
		void onUpMinThumb(float max, float min);
		//最大值的滑块移动
		void onUpMaxThumb(float max, float min);

		void onMinMove(float max, float min);

		void onMaxMove(float max, float min);
	}


	public Handler mHandler = new Handler(){
		@Override
		public void handleMessage (Message msg) {
			super.handleMessage(msg);
			switch (msg.what){

			}
		}
	};

	/**
	 * 将dip或dp值转换为px值，保证尺寸大小不变
	 *
	 * @param dipValue （DisplayMetrics类中属性density）
	 * @return
	 */
	private int dp2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

	public MyCustomRangeSeekBar (Context context) {
		super(context);


	}

	public MyCustomRangeSeekBar (Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);


	}

	private void initView(){

	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int height = MIN_HEIGHT;

		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {//wrapContent
			height = MeasureSpec.getSize(heightMeasureSpec);
		}
		int width = 0;


		setMeasuredDimension(width,height);

	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
	}

	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}
}
