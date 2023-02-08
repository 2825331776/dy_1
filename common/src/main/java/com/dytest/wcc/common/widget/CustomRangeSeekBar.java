package com.dytest.wcc.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.dytest.wcc.common.R;

import java.text.DecimalFormat;


/**
 * 自定义范围选取滑块工具栏
 *
 * @author zouyulong
 */
public class CustomRangeSeekBar extends View {
	private static final int    DEFAULT_MODE = 0;
	//最小值（绝对），
	public static float mAbsoluteMinValue;
	public static float mMinTemp = 0;//滑块的最低温度
	//最大值（绝对）
	public static float mAbsoluteMaxValue;
	public static float mMaxTemp = 100;//滑块的最高温度
	//progress bar 选中背景
	private static Bitmap mProgressBarSelBg;//整体的背景颜色
	private static Bitmap mProgressBarBgOne;
	private static Bitmap mProgressBarBgTwo;
	private static Bitmap mProgressBarBgThree;
	private static Bitmap mProgressBarBgFour;
	private static Bitmap mProgressBarBgFive;
	private static Bitmap mProgressBarBgSix;
	public final int UPDATE_VALUE = 1000;
	private final Paint  mPaint    = new Paint();
	//控件最小长度
	private final int     MIN_HEIGHT            = 400;
	public        int    backcolor = 1;
	//handler what
	DecimalFormat df = new DecimalFormat("0.0");//构造方法的字符格式这里如果小数不足2位,会以0补足.
	//滑块bitmap
	private       Bitmap mThumbImageMax;//滑块图像 对应表最大值
	private       Bitmap mThumbImageMin;//滑块图像 对应表最小值
	private              float  imgMax;//图层的最高温
	private              float  imgMin;//图层的最低温
	private              Bitmap mTempMaxImg;//图像 最高温
	private              Bitmap mTempMinImg;//图像 最低温
	private              int    widgetMode;//控件的绘制模式。0代表简单的绘制最高最低温。非零代表固定温度条模式。
	private              float  max;//控件最高温
	private              float  min;//控件最低温
	/**
	 * handler处理
	 */
	public Handler handler = new Handler() {
		@Override
		public void handleMessage (Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case UPDATE_VALUE:
					//更新时分秒
					float[] temp = (float[]) msg.obj;
					imgMax = temp[0];//滑块最高温 /图层最高温
					imgMin = temp[1];//滑块最低温/ 图层最低温
					if (widgetMode == 0) {//普通模式
						max = imgMax;//控件的最高温
						min = imgMin;//控件的最低温
					} else {//固定温度条
						calculateMaxMin(imgMax, imgMin);
					}
					invalidate();
					break;
			}
		}
	};
	//changed by wupei
	private float mThumbHeight;//滑动条高度
	private float mThumbWidth;//滑动条宽度
	//长度上下padding
	private float mHeightPadding;
	//已选标准（占滑动条百分比）最小值
	private double mPercentSelectedMinValue = 0d;
	//已选标准（占滑动条百分比）最大值
	private double mPercentSelectedMaxValue = 1d;
	//当前事件处理的thumb滑块
	private Thumb         mPressedThumb = null;
	//滑块事件
	private ThumbListener mThumbListener;
	private       RectF   mProgressBarRect;
	private       RectF   mProgressBarSelRect;
	//是否可以滑动
	private       boolean mIsEnable             = true;
	//最大值和最小值之间要求的最小范围绝对值
	private       float   mBetweenAbsoluteValue = 1;
	//文本宽度
	private int mWordWidth;
	//文本字体大小
	private float mWordSize;
	private float mStartMinPercent;
	private float mStartMaxPercent;

	public CustomRangeSeekBar (Context context) {
		super(context);
	}

	public CustomRangeSeekBar (Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CustomRangeSeekBar, 0, 0);
		mAbsoluteMinValue = a.getFloat(R.styleable.CustomRangeSeekBar_absoluteMin, (float) 0.0);
		mAbsoluteMaxValue = a.getFloat(R.styleable.CustomRangeSeekBar_absoluteMax, (float) 100.0);
		mStartMinPercent = a.getFloat(R.styleable.CustomRangeSeekBar_startMinPercent, 0);
		mStartMaxPercent = a.getFloat(R.styleable.CustomRangeSeekBar_startMaxPercent, 1);
		mThumbImageMin = BitmapFactory.decodeResource(getResources(), a.getResourceId(R.styleable.CustomRangeSeekBar_thumbImage, R.mipmap.span_min));
		mThumbImageMax = BitmapFactory.decodeResource(getResources(), a.getResourceId(R.styleable.CustomRangeSeekBar_thumbImage, R.mipmap.span_max));

		mTempMaxImg = BitmapFactory.decodeResource(getResources(), R.mipmap.max);
		mTempMinImg = BitmapFactory.decodeResource(getResources(), R.mipmap.min);
		//mBetweenAbsoluteValue = a.getFloat(R.styleable.CustomRangeSeekBar_betweenAbsoluteValue, 0);
		mWordSize = a.getDimension(R.styleable.CustomRangeSeekBar_progressTextSize, dp2px(context, 14));
		mPaint.setTextSize(mWordSize);

		mProgressBarSelBg = BitmapFactory.decodeResource(getResources(), a.getResourceId(R.styleable.CustomRangeSeekBar_progressBarBg, R.mipmap.seekbar_bg));


		//changed by wupei
		mThumbHeight = mThumbImageMax.getHeight();
		mThumbWidth = mThumbImageMax.getWidth();

		//TOOD 提供定义attr
		mHeightPadding = mThumbHeight;

		Paint.FontMetrics metrics = mPaint.getFontMetrics();
		//changed by wupei
		//        mWordWidth = (int) (metrics.descent - metrics.ascent);
		mWordWidth = (int) mPaint.measureText(String.valueOf(max));

		restorePercentSelectedMinValue();
		restorePercentSelectedMaxValue();
		a.recycle();

		widgetMode = DEFAULT_MODE;
	}

	/**
	 * 格式化毫秒->00:00
	 */
	private static String formatSecondTime (int millisecond) {
		if (millisecond == 0) {
			return "00:00";
		}
		int second = millisecond / 1000;
		int m = second / 60;
		int s = second % 60;
		if (m >= 60) {
			int hour = m / 60;
			int minute = m % 60;
			return hour + ":" + (minute > 9 ? minute : "0" + minute) + ":" + (s > 9 ? s : "0" + s);
		} else {
			return (m > 9 ? m : "0" + m) + ":" + (s > 9 ? s : "0" + s);
		}
	}

	/**
	 * 将dip或dp值转换为px值，保证尺寸大小不变
	 *
	 * @param dipValue （DisplayMetrics类中属性density）
	 * @return
	 */
	public static int dp2px (Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

	public static void setBackBitmap (Bitmap btm1, Bitmap btm2, Bitmap btm3, Bitmap btm4, Bitmap btm5, Bitmap btm6) {
		mProgressBarBgOne = btm1;
		mProgressBarBgTwo = btm2;
		mProgressBarBgThree = btm3;
		mProgressBarBgFour = btm4;
		mProgressBarBgFive = btm5;
		mProgressBarBgSix = btm6;
	}

	//绘制模式
	public int getWidgetMode () {
		return widgetMode;
	}

	public void setWidgetMode (int widgetMode) {
		this.widgetMode = widgetMode;
	}

	/**
	 * 还原min滑块到初始值
	 */
	public void restorePercentSelectedMinValue () {
		setPercentSelectedMinValue(mStartMinPercent);
	}

	/**
	 * 还原max滑块到初始值
	 */
	public void restorePercentSelectedMaxValue () {
		setPercentSelectedMaxValue(mStartMaxPercent);
	}

	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		mProgressBarRect = new RectF(mThumbWidth, mHeightPadding, mWordWidth + mThumbWidth, h - mHeightPadding);

		//mProgressBarSelRect在ondraw的时候还会变的
		mProgressBarSelRect = new RectF(mProgressBarRect);
	}

	/**
	 * 设置seekbar 是否接收事件
	 *
	 * @param enabled
	 */
	@Override
	public void setEnabled (boolean enabled) {
		super.setEnabled(enabled);
		this.mIsEnable = enabled;
	}

	public void setBackcolor (int backcolor) {
		this.backcolor = backcolor;
	}

	/**
	 * 返回被选择的最小值(绝对值)
	 *
	 * @return The currently selected min value.
	 */
	public float getSelectedAbsoluteMinTemp () {
		return percentToAbsoluteTemp(mPercentSelectedMinValue);
	}

	public float getSelectedAbsoluteMinValue () {
		return percentToAbsoluteValue(mPercentSelectedMinValue);
	}

	/**
	 * 返回被选择的最大值（绝对值）.
	 */
	public float getSelectedAbsoluteMaxTemp () {
		return percentToAbsoluteTemp(mPercentSelectedMaxValue);
	}

	public float getSelectedAbsoluteMaxValue () {
		return percentToAbsoluteValue(mPercentSelectedMaxValue);
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (!mIsEnable)
			return true;
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mPressedThumb = evalPressedThumb(event.getY());
				invalidate();
				//Intercept parent TouchEvent
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (mPressedThumb != null) {
					//changed by wupei
					float eventY = event.getY();
					float maxValue = percentToAbsoluteValue(mPercentSelectedMaxValue);
					float minValue = percentToAbsoluteValue(mPercentSelectedMinValue);
					float eventValue = percentToAbsoluteValue(screenToPercent(eventY));
					if (Thumb.MIN.equals(mPressedThumb)) {
						minValue = eventValue;
						if (mBetweenAbsoluteValue > 0 && maxValue - minValue <= mBetweenAbsoluteValue)
							minValue = new Float((maxValue - mBetweenAbsoluteValue));
						setPercentSelectedMinValue(absoluteValueToPercent(minValue));
						if (mThumbListener != null)
							mThumbListener.onMinMove(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue());
					} else if (Thumb.MAX.equals(mPressedThumb)) {
						maxValue = eventValue;
						if (mBetweenAbsoluteValue > 0 && maxValue - minValue <= mBetweenAbsoluteValue)
							maxValue = new Float(minValue + mBetweenAbsoluteValue);

						setPercentSelectedMaxValue(absoluteValueToPercent(maxValue));
						if (mThumbListener != null)
							mThumbListener.onMaxMove(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue());
					}
				}
				//Intercept parent TouchEvent
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;
			case MotionEvent.ACTION_UP:
				if (Thumb.MIN.equals(mPressedThumb)) {
					if (mThumbListener != null)
						mThumbListener.onUpMinThumb(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue());
				}
				if (Thumb.MAX.equals(mPressedThumb)) {
					if (mThumbListener != null)
						mThumbListener.onUpMaxThumb(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue());
				}
				//Intercept parent TouchEvent
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				mPressedThumb = null;
				//Intercept parent TouchEvent
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;
		}
		return true;
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		int height = MIN_HEIGHT;
		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
			height = MeasureSpec.getSize(heightMeasureSpec);
		}
		//int height = mThumbImage.getHeight() + mWordHeight;
		//changed by wupei
		//*2 + mTempMaxImg.getWidth()
		int width = mThumbImageMax.getWidth() + mWordWidth * 2 + mTempMaxImg.getWidth();
		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
			width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
		}
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		// draw seek bar background line
		mPaint.setStyle(Style.FILL);
		canvas.drawBitmap(mProgressBarSelBg, null, mProgressBarRect, mPaint);
		//changed by wupei
		mProgressBarSelRect.top = percentToScreen(mPercentSelectedMaxValue);
		mProgressBarSelRect.bottom = percentToScreen(mPercentSelectedMinValue);
		// draw seek bar active range line

		switch (backcolor) {
			case 1:
				canvas.drawBitmap(mProgressBarBgOne, null, mProgressBarSelRect, mPaint);
				break;
			case 2:
				canvas.drawBitmap(mProgressBarBgTwo, null, mProgressBarSelRect, mPaint);
				break;
			case 3:
				canvas.drawBitmap(mProgressBarBgThree, null, mProgressBarSelRect, mPaint);
				break;
			case 4:
				canvas.drawBitmap(mProgressBarBgFour, null, mProgressBarSelRect, mPaint);
				break;
			case 5:
				canvas.drawBitmap(mProgressBarBgFive, null, mProgressBarSelRect, mPaint);
				break;
			case 6:
				canvas.drawBitmap(mProgressBarBgSix, null, mProgressBarSelRect, mPaint);
				break;
		}

		//        if (widgetMode==1){
		//            canvas.drawBitmap(mTempMaxImg,0,);
		//        }


		// draw minimum thumb   //绘制滑块
		drawThumbMax(percentToScreen(mPercentSelectedMaxValue), canvas);
		// draw maximum thumb
		drawThumbMin(percentToScreen(mPercentSelectedMinValue), canvas);
		mPaint.setColor(Color.rgb(255, 165, 0));

		//changed by wp
		drawThumbMaxText(percentToScreen(mPercentSelectedMaxValue), canvas);
		drawThumbMinText(percentToScreen(mPercentSelectedMinValue), canvas);

	}

	@Override
	protected Parcelable onSaveInstanceState () {
		Bundle bundle = new Bundle();
		bundle.putParcelable("SUPER", super.onSaveInstanceState());
		bundle.putDouble("MIN", mPercentSelectedMinValue);
		bundle.putDouble("MAX", mPercentSelectedMaxValue);
		return bundle;
	}

	@Override
	protected void onRestoreInstanceState (Parcelable parcel) {
		Bundle bundle = (Bundle) parcel;
		super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
		mPercentSelectedMinValue = bundle.getDouble("MIN");
		mPercentSelectedMaxValue = bundle.getDouble("MAX");
	}

	/**
	 * Draws the "normal" resp. "pressed" thumb image on specified x-coordinate.
	 *
	 * @param screenCoord The x-coordinate in screen space where to draw the image.
	 * @param canvas      The canvas to draw upon.
	 */
	private void drawThumbMax (float screenCoord, Canvas canvas) {
		canvas.drawBitmap(mThumbImageMax, 0, screenCoord - mThumbHeight, mPaint);
	}

	private void drawThumbMin (float screenCoord, Canvas canvas) {
		canvas.drawBitmap(mThumbImageMin, 0, screenCoord, mPaint);
	}

	/**
	 * 画滑块值text
	 * 绘制最大值，和滑块的最大值的文字
	 *
	 * @param screenCoord
	 * @param canvas
	 */
	//changed by wp: float->double
	private void drawThumbMaxText (float screenCoord, Canvas canvas) {
		String progress = df.format(max);//转化最大值为String类型
		float progressHeight = mPaint.measureText(progress);//测量最大值String需要的长度
		canvas.drawText(progress, 0.7f * mThumbWidth, mThumbHeight, mPaint);//绘制最大值的String，起始点：跟测量的长度无关？

		//绘制最大值滑块，设置了最大值和最小值的差距为0.1
		double temp = getSelectedAbsoluteMaxTemp();
		if (getSelectedAbsoluteMaxTemp() - getSelectedAbsoluteMinTemp() < 0.1)
			temp = getSelectedAbsoluteMinTemp() + 0.1;
		String selectmax = df.format(temp);

		canvas.drawText(selectmax, 0, screenCoord - 0.4f * mWordSize, mPaint);
	}

	/**
	 * 绘制最小值，和滑块的最小值的 文字
	 *
	 * @param screenCoord
	 * @param canvas
	 */
	private void drawThumbMinText (float screenCoord, Canvas canvas) {
		String progress = df.format(min);
		float progressHeight = mPaint.measureText(progress);
		canvas.drawText(progress, 0.7f * mThumbWidth, getHeight() - 0.5f * mWordSize, mPaint);

		String selectmin = df.format(getSelectedAbsoluteMinTemp());
		canvas.drawText(selectmin, 0, screenCoord + mWordSize, mPaint);
	}

	/**
	 * 根据touchY, 判断是哪一个thumb(Min or Max)
	 *
	 * @param touchY 触摸的Y在屏幕中坐标（相对于容器）
	 */
	private Thumb evalPressedThumb (float touchY) {
		Thumb result = null;
		boolean minThumbPressed = isInThumbRange(touchY, mPercentSelectedMinValue);
		boolean maxThumbPressed = isInThumbRange(touchY, mPercentSelectedMaxValue);
		if (minThumbPressed && maxThumbPressed) {
			// if both thumbs are pressed (they lie on top of each other), choose the one with more room to drag. this avoids "stalling" the thumbs in a corner, not being able to drag them apart anymore.
			result = touchY >= percentToScreen(mPercentSelectedMinValue) ? Thumb.MIN : Thumb.MAX;
		} else if (minThumbPressed) {
			result = Thumb.MIN;
		} else if (maxThumbPressed) {
			result = Thumb.MAX;
		}
		return result;
	}

	/**
	 * 判断touchX是否在滑块点击范围内
	 *
	 * @param touchY            需要被检测的 屏幕中的x坐标（相对于容器）
	 * @param percentThumbValue 需要检测的滑块x坐标百分比值（滑块x坐标）
	 */
	private boolean isInThumbRange (float touchY, double percentThumbValue) {
		return Math.abs(touchY - percentToScreen(percentThumbValue)) <= 2 * mThumbHeight;
	}

	/**
	 * 设置已选择最小值的百分比值
	 * [0 , value , 1]
	 */
	public void setPercentSelectedMinValue (double value) {
		mPercentSelectedMinValue = Math.max(0d, Math.min(1d, Math.min(value, mPercentSelectedMaxValue)));
		invalidate();
	}

	/**
	 * [0 , value , 1]
	 * 设置已选择最大值的百分比值
	 */
	public void setPercentSelectedMaxValue (double value) {
		mPercentSelectedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, mPercentSelectedMinValue)));
		invalidate();
	}

	/**
	 * 进度值，从百分比到绝对值 取值为 0 - 100
	 *
	 * @return (float) percent * 100;
	 */
	@SuppressWarnings("unchecked")
    /*这么写不对
    private float percentToAbsoluteValue(double normalized) {
        return (float) normalized * 100;
    }
     */ private float percentToAbsoluteValue (double percent) {
		return (float) percent * 100;
	}

	/**
	 * 百分比转化为 整体最大值与最小值之间的百分比。
	 * 百分比乘以最大值减去最小值，然后加上最小值。为返回的温度
	 *
	 * @param percent 百分比
	 * @return (float) (mMinTemp + percent * (mMaxTemp - mMinTemp));
	 */
	private float percentToAbsoluteTemp (double percent) {
		return (float) (mMinTemp + percent * (mMaxTemp - mMinTemp));
	}

	/**
	 * 进度值，从绝对值到百分比
	 */
	private double absoluteValueToPercent (float value) {
		if (0 == mAbsoluteMaxValue - mAbsoluteMinValue) {
			// prevent division by zero, simply return 0.
			return 0d;
		}
		return (value - mAbsoluteMinValue) / (mAbsoluteMaxValue - mAbsoluteMinValue);
	}

	/**
	 * 进度值，从百分比值转换到屏幕中坐标值 left&top的绘制模式
	 */
	private float percentToScreen (double percentValue) {
		return (float) (mHeightPadding + (1 - percentValue) * (getHeight() - 2 * mHeightPadding));

	}

	/**
	 * 进度值，转换屏幕像素值到百分比值
	 */
	private double screenToPercent (float screenCoord) {
		int height = getHeight();
		if (height <= 2 * mHeightPadding) {
			// prevent division by zero, simply return 0.
			return 0d;
		} else {
			double result = 1 - (screenCoord - mHeightPadding) / (height - 2 * mHeightPadding);
			return Math.min(1d, Math.max(0d, result));
		}
	}

	public void setThumbListener (ThumbListener mThumbListener) {
		this.mThumbListener = mThumbListener;
	}

	public void Update (float maxtem, float mintem) {
		Message message = Message.obtain();
		message.what = UPDATE_VALUE;
		message.obj = new float[]{maxtem, mintem};
		handler.sendMessage(message);
	}

	/**
	 * max 控件的最高点温度， min 最低点温度
	 *
	 * @param maxTemp 实时最大值
	 * @param minTemp 实时最小值
	 */
	private void calculateMaxMin (float maxTemp, float minTemp) {
		if (Math.abs(max - maxTemp) < 15 || Math.abs(min - minTemp) < 10) {//相差不足10
			max = ((int) maxTemp / 10) * 10 + 20;
			min = ((int) minTemp / 10) * 10 - 10;
		} else {

		}
	}

	/**
	 * Thumb枚举， 最大或最小
	 */
	private enum Thumb {
		MIN, MAX
	}

	/**
	 * 滑块事件
	 *
	 * @author zouyulong
	 */
	public interface ThumbListener {
		//void onClickMinThumb(Number max, Number min);

		//void onClickMaxThumb();

		void onUpMinThumb (float max, float min);

		void onUpMaxThumb (float max, float min);

		void onMinMove (float max, float min);

		void onMaxMove (float max, float min);
	}

}
