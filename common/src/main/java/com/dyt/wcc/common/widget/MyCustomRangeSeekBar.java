package com.dyt.wcc.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dyt.wcc.common.R;

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
	private static final String TAG = "MyCustomRangeSeekBar";
	//条子的最高最低温度。
	private float mMaxTemp = 100;
	private float mMinTemp = 0;

	private DecimalFormat df = new DecimalFormat("0.0");//构造方法的字符格式这里如果小数不足2位,会以0补足.

	//滑块的最高最低温度 及其 滑块的图片
	private float mThumbMaxTemp;
	private float mThumbMinTemp;
	//滑块bitmap
	private Bitmap mThumbMaxImage;//滑块图像 对应表最大值
	private Bitmap mThumbMinImage;//滑块图像 对应表最小值
	private float mThumbHeight;//滑动条高度
	private float mThumbWidth;//滑动条宽度

	// 实时最高最低温
	// 绘制在屏幕上的的最高温度 ，和 最低温度。模式0时，绘制的温度等于实时的 屏幕最高最低温。模式1时，需要实时计算这个的值。
	private float rangeMaxTemp;
	private float rangeMinTemp;
	private Bitmap btRangeMaxTemp,btRangeMinTemp;
	private int widgetMode = 0;//控件的绘制模式。0代表简单的绘制最高最低温。非零代表固定温度条模式。

	//progress bar 选中背景
	private List<Bitmap> mProgressBarSelectBg;//整体的背景颜色
	private Bitmap mProgressBarBg;

	//最小值（绝对），取值0-100
	public static float mAbsoluteMinValue;
	//最大值（绝对）
	public static float mAbsoluteMaxValue;

	//已选标准（占滑动条百分比）最小值 。相对于整体的高度。
	private double mPercentSelectedMinValue = 0d;
	//已选标准（占滑动条百分比）最大值。相对于整体的高度。
	private double mPercentSelectedMaxValue = 1d;

	private RectF mProgressBarRectBg;//条子背景的矩形
	private RectF mProgressBarSelRect;//色板覆盖的矩形

	//是否可以滑动
	private boolean mEnable = true;

	//当前事件处理的thumb滑块 ，区分按下的操作是最小值 还是最大值的滑块
	private MyCustomRangeSeekBar.Thumb mPressedThumb = null;
	//滑块事件
	private ThumbListener            mThumbListener;
	public static final int UPDATE_VALUE = 1005;

	private int stripWidth;//那个滑动的 条条的 宽度

	private final Paint mPaint = new Paint();

	//控件最小高度
	private final int MIN_HEIGHT = 400;

	private float mTextSize;//文字的大小。
	private int topBottomPadding;
	private float seekbarWidth;//条子的宽度


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
				case UPDATE_VALUE:
					//更新时分秒
					float[] temp = (float[]) msg.obj;
					rangeMaxTemp = temp[0];//实时最高温
					rangeMinTemp = temp[1];//实时最低温
					if (widgetMode ==0) {//普通模式
						mMaxTemp = rangeMaxTemp;//条子的最高温
						mMinTemp = rangeMinTemp;//控件的最低温
					}else {//固定温度条
						calculateMaxMin(rangeMaxTemp,rangeMinTemp);
					}
					//todo 计算滑块所处于百分比 对应的温度数值，相对于条子的高低温。
					//todo 根据模式二 计算实时高低温的处于的位置。

					invalidate();
					break;
			}
		}
	};
	public void Update(float maxT, float minT){
		Message message = Message.obtain();
		message.what = UPDATE_VALUE;
		message.obj = new float[]{maxT, minT};
		mHandler.sendMessage(message);
	}

	public int getWidgetMode () { return widgetMode; }
	public void setWidgetMode (int widgetMode) { this.widgetMode = widgetMode; }

	/**
	 *  mMaxTemp 条子最高温， mMinTemp 条子最低温
	 * @param maxTemp 实时最大值
	 * @param minTemp 实时最小值
	 *
	 */
	private void calculateMaxMin(float maxTemp ,float minTemp){
		if (Math.abs(mMaxTemp - maxTemp) < 10 || Math.abs(minTemp - mMinTemp)  < 10 //如果实时高温+15大于了 条子高温， 或者 实时低温减去10 还小于条子低温
		||Math.abs(mMaxTemp - maxTemp) >= 20 || Math.abs(minTemp - mMinTemp) >= 20) {
			mMaxTemp = ((int)maxTemp/10)*10 + 20;
			mMinTemp = ((int)minTemp/10)*10 - 10;

			mThumbMaxTemp = rangeMaxTemp + 3;
			mThumbMinTemp = rangeMaxTemp - 2;

			mPercentSelectedMaxValue = (mThumbMaxTemp - mMinTemp)/(mMaxTemp - mMinTemp);
			mPercentSelectedMinValue = (mThumbMinTemp - mMinTemp)/(mMaxTemp - mMinTemp);

			//todo 计算变化之后的 滑块温度范围 对应的 百分比值。

		}else {

		}
	}

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
	//具体的温度转化成相对于 条子最高温距离的 百分比
	private double TempToPercent(float temp){
		return ( mMaxTemp- temp)/(mMaxTemp - mMinTemp);
	}
	//百分比
	private float Temp2Height(float temp){
		return (float) ((getHeight() - 2* topBottomPadding) * TempToPercent(temp));
	}



	public MyCustomRangeSeekBar (Context context) {
		super(context);


	}

	public MyCustomRangeSeekBar (Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MyCustomRangeSeekBar, 0, 0);

		mProgressBarBg = BitmapFactory.decodeResource(getResources(),R.mipmap.seekbar_bg);//整体的背景颜色

		mThumbMaxImage = BitmapFactory.decodeResource(getResources(), R.mipmap.span_max);
		mThumbMinImage = BitmapFactory.decodeResource(getResources(),R.mipmap.span_min);
		btRangeMaxTemp = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_max);
		btRangeMinTemp = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_min);

		mTextSize = a.getDimension(R.styleable.MyCustomRangeSeekBar_SeekBarAllTextSize, dp2px(context, 12));
		mPaint.setTextSize(mTextSize);

		Paint.FontMetrics metrics = mPaint.getFontMetrics();
		seekbarWidth = metrics.descent - metrics.ascent;

		topBottomPadding = (int) (mThumbMaxImage.getHeight() + seekbarWidth);//顶部底部的padding 为 滑动块高度 加上画笔绘制文字 内容部分的高度
//		Log.e(TAG, "MyCustomRangeSeekBar: " + topBottomPadding);
//		Log.e(TAG, "MyCustomRangeSeekBar:  " + metrics.descent + "   " +  metrics.ascent);

		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setColor(ContextCompat.getColor(context,R.color.max_temp_text_color_red));
		mPaint.setAlpha(255);

		initView();
		initPaint();

		a.recycle();
	}

	private void initView(){

	}

	private void initPaint(){

	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int height = MIN_HEIGHT;

		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {//wrapContent
			height = MeasureSpec.getSize(heightMeasureSpec);
		}
		//宽度 = 滑动条宽度（包含了文字的宽度） + 条子宽度 + 右边文字的宽度 + 实时最高最低温图片的宽度
		int width = (int)(mThumbMaxImage.getWidth() + seekbarWidth + btRangeMaxTemp.getWidth()
				+ mPaint.measureText("1000") );
//		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
//			width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
//		}

		setMeasuredDimension(width,height);
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		mProgressBarRectBg = new RectF(mThumbMaxImage.getWidth(),topBottomPadding,mThumbMaxImage.getWidth()+seekbarWidth,getHeight()- topBottomPadding);

		onDrawBg(canvas);
		onDrawBp(canvas);
		onDrawText(canvas);
	}
	private void onDrawBg(Canvas canvas){
		//绘制全局背景
		canvas.drawColor(getResources().getColor(R.color.bg_preview_toggle_select));
		//绘制条子的整体背景
		canvas.drawBitmap(mProgressBarBg,null,
				mProgressBarRectBg,mPaint);
	}

	private void onDrawBp(Canvas canvas){
		//绘制滑动条
		canvas.drawBitmap(mThumbMaxImage,0,percent2Height(mPercentSelectedMaxValue) - mThumbMaxImage.getHeight(),mPaint);
		canvas.drawBitmap(mThumbMinImage,0,     percent2Height(mPercentSelectedMinValue),mPaint);

		if (widgetMode ==1 ){
			RectF recRange = new RectF(mThumbMaxImage.getWidth() + seekbarWidth ,
					0,mThumbMaxImage.getWidth() + seekbarWidth + btRangeMaxTemp.getWidth()/2.0f,0);
			//绘制实时最高最低温图片
			recRange.top = topBottomPadding+ Temp2Height(rangeMaxTemp) - btRangeMaxTemp.getHeight()/4.0f;
			recRange.bottom =topBottomPadding+ Temp2Height(rangeMaxTemp) + btRangeMaxTemp.getHeight()/4.0f;
			canvas.drawBitmap(btRangeMaxTemp,null,recRange,mPaint);

			recRange.top = topBottomPadding+Temp2Height(rangeMinTemp) - btRangeMaxTemp.getHeight()/4.0f;
			recRange.bottom = topBottomPadding+Temp2Height(rangeMinTemp) + btRangeMaxTemp.getHeight()/4.0f;
			canvas.drawBitmap(btRangeMinTemp,null,recRange,mPaint);
		}
	}
	private void onDrawText(Canvas canvas){
		//绘制条子最高温、条子最低温
		canvas.drawText(Float2Str(mMaxTemp), mThumbMaxImage.getWidth()+ seekbarWidth/2 - mPaint.measureText(Float2Str(mMaxTemp))/2,
				mThumbMaxImage.getHeight() - mPaint.ascent() - mPaint.descent(),mPaint);
		canvas.drawText(Float2Str(mMinTemp), mThumbMaxImage.getWidth()+ seekbarWidth/2 - mPaint.measureText(Float2Str(mMinTemp))/2,
				getHeight()- mThumbMaxImage.getHeight(),mPaint);
		//绘制滑块最高温、最低温
		canvas.drawText(Float2Str(percent2Temp(mPercentSelectedMaxValue)),0,
				percent2Height(mPercentSelectedMaxValue) - mThumbMaxImage.getHeight() - mPaint.ascent() + mPaint.descent(),mPaint);

		canvas.drawText(Float2Str(percent2Temp(mPercentSelectedMinValue)),0,percent2Height(mPercentSelectedMinValue)- mPaint.ascent() + mPaint.descent(),mPaint);

		//绘制实时最高最低温文字
		if (widgetMode ==1 ){
			canvas.drawText(Float2Str(rangeMaxTemp),mThumbMaxImage.getWidth() + seekbarWidth + btRangeMaxTemp.getWidth()/2.0f,
					topBottomPadding+ Temp2Height(rangeMaxTemp)+mPaint.descent(),mPaint);
			canvas.drawText(Float2Str(rangeMinTemp),mThumbMaxImage.getWidth() + seekbarWidth + btRangeMaxTemp.getWidth()/2.0f,
					topBottomPadding+ Temp2Height(rangeMinTemp)+mPaint.descent(),mPaint);
		}
//		//右侧实时高低温的左边界线
//		canvas.drawLine(mThumbMaxImage.getWidth() + seekbarWidth ,
//				0,mThumbMaxImage.getWidth() + seekbarWidth ,
//				topBottomPadding,mPaint);
		//起始刻度
		canvas.drawLine(mThumbMaxImage.getWidth(),topBottomPadding,mThumbMaxImage.getWidth()+seekbarWidth,topBottomPadding,mPaint);
		//四分之一刻度
		canvas.drawLine(mThumbMaxImage.getWidth(),(getHeight()-2.0f*topBottomPadding)/4+topBottomPadding,
				mThumbMaxImage.getWidth()+seekbarWidth,(getHeight()-2.0f*topBottomPadding)/4+topBottomPadding,mPaint);
		//四分之二刻度
		canvas.drawLine(mThumbMaxImage.getWidth(),getHeight()/2.0f,mThumbMaxImage.getWidth()+seekbarWidth,getHeight()/2.0f,mPaint);
		//四分之三刻度
		canvas.drawLine(mThumbMaxImage.getWidth(),(getHeight()-2.0f*topBottomPadding)/4*3+topBottomPadding,
				mThumbMaxImage.getWidth()+seekbarWidth,(getHeight()-2.0f*topBottomPadding)/4*3+topBottomPadding,mPaint);
	}

	/**
	 * float 数值转String，并 格式化float数值。
	 * @return
	 */
	private String Float2Str(float value){
		return df.format(value);
	}

	/**
	 * 滑动块的百分比 转换成绘制的高度
	 * @param percent 取值[0,1] double 类型。
	 * @return 高度。
	 */
	private float percent2Height(double percent){
		return (float) (topBottomPadding + (getHeight() - topBottomPadding*2.0f) * (1.00 - percent));
	}

	private float percent2Temp(double percent){
		return (float) (mMaxTemp - (mMaxTemp- mMinTemp)*(1-percent));
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		return super.onTouchEvent(event);
	}

	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}
}
