package com.dyt.wcc.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dyt.wcc.common.R;
import com.dyt.wcc.common.utils.DensityUtil;
import com.dyt.wcc.common.widget.dragView.DragTempContainer;

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
	/**
	 * 实时最大最小 摄氏度 数值。用于滑动了滑动块之后去直接计算返回给C++层的数值
	 */
	private float realTimeMaxDCTemp = 999.9f;
	private float realTimeMinDCTemp = 0.0f;
	/**
	 * 条形滑动栏的 最高温 最低温 。绘制在顶部和底部
	 */
	private float mMaxTemp = 100;
	private float mMinTemp = 0;
//	private float mViewWidth;
//	private float mViewHeight;
	private int thumbMaxCount = 0;
	private int thumbMinCount = 0;

	private DecimalFormat df = new DecimalFormat("0.0");//构造方法的字符格式这里如果小数不足2位,会以0补足.
	private static final String MAX_LENGTH_TEMP = "999.9";
	private Rect maxTempTextRect;//最大值温度文字的矩形
	/**
	 * 最大值温度文字的宽度
	 */
	private float maxTempTextLength;
	private int tempUnitMode = 0;//温度的单位，绘制在顶部

	/**
	 * 滑动块 的最高最低温度
	 */
	private float mThumbMaxTemp;
	private float mThumbMinTemp;
	/**
	 * 滑动块的图片
	 */
	private Bitmap mThumbMaxImage;// 最大值 滑块图片
	private Bitmap mThumbMinImage;// 最小值 滑块图片
	private float mThumbHeight;//滑动条高度
	private float mThumbWidth;//滑动条宽度
	private RectF mThumbRect;//滑动条的矩形

	/**
	 * 	实时返回的最高最低温,带有温度单位变化的
 	 */
	private float rangeMaxTemp;
	private float rangeMinTemp;
	private RectF realTimeTempPicRect;//实时温度
	private Bitmap btRangeMaxTemp,btRangeMinTemp;
	private int widgetMode = 0;//控件的绘制模式。0代表简单的绘制最高最低温。非零代表固定温度条模式。

	//progress bar 选中背景
	private List<Bitmap> mProgressBarSelectBgList;//整体的背景颜色
	private Bitmap mProgressBarBg;
	private Bitmap mProgressBarSelectBg;

	//最小值（绝对），取值0-100
	public static float mAbsoluteMinValue = 0;
	//最大值（绝对）
	public static float mAbsoluteMaxValue = 100;

	//已选标准（占滑动条百分比）最小值 。相对于整体的高度。
	private double mPercentSelectedMinValue = 0d;
	//已选标准（占滑动条百分比）最大值。相对于整体的高度。
	private double mPercentSelectedMaxValue = 1d;
	//最大值和最小值之间要求的最小范围绝对值，要将 最大值百分比和最小值 百分比 放大100倍之后计算。
	private float mBetweenAbsoluteValue = 1;
	/**
	 * 条子背景的矩形
	 */
	private RectF mProgressBarRectBg;
	/**
	 * 色板覆盖的矩形
	 */
	private RectF mProgressBarSelRect;

	//是否可以滑动
	private boolean mEnable = true;

	//当前事件处理的thumb滑块 ，区分按下的操作是最小值 还是最大值的滑块
	private MyCustomRangeSeekBar.Thumb mPressedThumb = null;
	//滑块事件
	private ThumbListener            mThumbListener;
	public static final int UPDATE_VALUE = 1005;

	private final Paint mPaint = new Paint();

	//控件最小高度
	private final int MIN_HEIGHT = 400;

	private float mTextSize;//文字的大小。
	private int topBottomPadding;
	//条子的宽度
	private float seekbarWidth;
	//实时温度 图片的宽度
	private int realTimeHighLowPicWidth;
	private static float MAX_TEMP = 500.0f;
	private static float MIN_TEMP = -100.0f;

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
		/**
		 * 最大最小值是否改变
		 * @param maxPercent 最大值百分比
		 * @param minPercent    最小值百分比
		 * @param maxValue  最大值百分比对应的温度数值
		 * @param minValue  最小百分比对应的温度数值
		 */
		void thumbChanged(float maxPercent, float minPercent,float maxValue, float minValue);

		/**
		 * 最大值的滑块 抬起
		 * @param maxPercent 最大值百分比
		 * @param minPercent    最小值百分比
		 * @param maxValue  最大值百分比对应的温度数值
		 * @param minValue  最小百分比对应的温度数值
		 */
		void onUpMaxThumb(float maxPercent, float minPercent,float maxValue, float minValue);
		/**
		 * 最大值的滑块 抬起
		 * @param maxPercent 最大值百分比
		 * @param minPercent    最小值百分比
		 * @param maxValue  最大值百分比对应的温度数值
		 * @param minValue  最小百分比对应的温度数值
		 */
		void onUpMinThumb(float maxPercent, float minPercent,float maxValue, float minValue);
		/**
		 * 最小值滑块移动
		 * @param maxPercent 最大值百分比
		 * @param minPercent    最小值百分比
		 * @param maxValue  最大值百分比对应的温度数值
		 * @param minValue  最小百分比对应的温度数值
		 */
		void onMinMove(float maxPercent, float minPercent,float maxValue, float minValue);

		/**
		 * 最大值滑块移动
		 * @param maxPercent 最大值百分比
		 * @param minPercent 最小值百分比
		 * @param maxValue  最大值百分比对应的温度数值
		 * @param minValue  最小百分比对应的温度数值
		 */
		void onMaxMove(float maxPercent, float minPercent,float maxValue, float minValue);
	}

	public void setTempUnitText (int tempUnitIndex) {
		this.tempUnitMode = tempUnitIndex;
	}

	public Handler mHandler = new Handler(){
		@Override
		public void handleMessage (Message msg) {
			super.handleMessage(msg);
			switch (msg.what){
				case UPDATE_VALUE://更新数据，从控件绘制层拿到的是摄氏度。根据本地保存的温度单位转换为 对应的数值（摄氏度，华氏度，开氏度）
					//更新时分秒
					float[] temp = (float[]) msg.obj;
					realTimeMaxDCTemp = temp[0];
					realTimeMinDCTemp = temp[1];
//					if (realTimeMaxDCTemp > 100.0f || realTimeMinDCTemp < MIN_TEMP){
//						realTimeMaxDCTemp = Float.NaN;
//						realTimeMinDCTemp = Float.NaN;
//						rangeMaxTemp = Float.NaN;
//						rangeMinTemp = Float.NaN;
//						break;
//					}

					rangeMaxTemp = getTempFloatByMode(temp[0]);//实时最高温
					rangeMinTemp = getTempFloatByMode(temp[1]);//实时最低温
					if (widgetMode ==0) {//普通模式
						mMaxTemp = rangeMaxTemp;//条子的最高温
						mMinTemp = rangeMinTemp;//条子的最低温
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

	/**
	 *  mMaxTemp 条子最高温， mMinTemp 条子最低温
	 * @param maxTemp 实时最大值 带温度单位
	 * @param minTemp 实时最小值 带温度单位
	 *
	 */
	private void calculateMaxMin(float maxTemp ,float minTemp){
		if (Float.isNaN(mMaxTemp) && !Float.isNaN(realTimeMaxDCTemp)){//针对于又nan 变成了正常温度 。
			calculateSeekBarTopBottomTemp();
		}
		//实时最大值 大于 （底部 -5）  或者 实时最大值 小于（底部-20）
		if (realTimeMaxDCTemp > temp2DegreeCentigrade(mMaxTemp) - 5
				||realTimeMaxDCTemp < temp2DegreeCentigrade(mMaxTemp) - 20) {
			thumbMaxCount++;
		}else {//不满足变化的条件，重置计数器
			thumbMaxCount =0;
		}
		//实时最小值 小于 （底部 +5）  或者 实时最小值 大于（底部+20）
		if (realTimeMinDCTemp < temp2DegreeCentigrade(mMinTemp) + 5
				||realTimeMinDCTemp > temp2DegreeCentigrade(mMinTemp) + 20){
			thumbMinCount++;
		}else {//不满足变化的条件，重置计数器
			thumbMinCount = 0;
		}

		if (thumbMaxCount >= 30 || thumbMinCount >=30 ){
			calculateSeekBarTopBottomTemp();
		}
	}

	/**
	 * 计算滑动条 顶部及其底部的温度。并计算滑动条的温度，百分比 。重置计数器
	 */
	private void calculateSeekBarTopBottomTemp(){
		mMaxTemp = getTempFloatByMode( (((int)realTimeMaxDCTemp+5)/10)*10 + 10);
		mMinTemp = getTempFloatByMode( (((int)realTimeMinDCTemp+5)/10)*10 - 10);

		mThumbMaxTemp = getTempFloatByMode(realTimeMaxDCTemp + 2);
		mThumbMinTemp = getTempFloatByMode(realTimeMaxDCTemp - 2);

		mPercentSelectedMaxValue = (mThumbMaxTemp - mMinTemp)/(mMaxTemp - mMinTemp);
		mPercentSelectedMinValue = (mThumbMinTemp - mMinTemp)/(mMaxTemp - mMinTemp);
		//回调给View层更改了画面数据。修改C++层的最大最小AD值
		//todo 计算变化之后的 滑块温度范围 对应的 百分比值。 重新计算滑块百分比之后 更改JNI层的渲染范围
		mThumbListener.thumbChanged(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue()
				,percent2DegreeCentigrade(mPercentSelectedMaxValue),percent2DegreeCentigrade(mPercentSelectedMinValue));
		thumbMaxCount = 0;
		thumbMinCount = 0;
	}

	/**
	 * 切换温度模式流程：切换到华氏度，得重新计算数值。并更改后面的单位
	 * 华氏度 = 摄氏度*1.8+32 （℉）  ； 开氏度= 摄氏度 + 237.15 （K）
	 * @param tempC 温度摄氏度
	 * @return String 温度数值带单位
	 */
	private float getTempFloatByMode( float tempC){
		float result = 0.0f;
		switch (tempUnitMode){
			case 0:
				result = tempC;
				break;
			case 1:
				result = getFormatFloat((float) (tempC * 1.8 + 32));
				break;
			case 2:
				result = getFormatFloat((float) (tempC + 273.15));
				break;
		}
		return result;
	}
	/**
	 * 百分比转化为摄氏度
	 * @param percent 百分比
	 * @return 摄氏度。（因为机芯返回的是温度表是 摄氏度）
	 */
	private float percent2DegreeCentigrade(double percent){
		//得到是可能是华氏度 开氏度 摄氏度的 数值
		float temp =  (float) (mMinTemp + (mMaxTemp - mMinTemp)*percent);
		float result = temp2DegreeCentigrade(temp);
		return result;
	}

	/**
	 * 具体的温度数值 转化成 摄氏度
	 * @param temp
	 * @return
	 */
	private float temp2DegreeCentigrade(float temp){
		float result = 0.0f;
		switch (tempUnitMode){
			case 0:
				result = temp ;
				break;
			case 1:
				result = (temp - 32.0f) * 5 / 9;
				break;
			case 2:
				result = temp - 273.15f;
				break;
		}
		return result;
	}

	private float getFormatFloat(float value){
		return Float.parseFloat(df.format(value));
	}

	/**
	 * 刷新温度数据
	 * @param maxT 最高温 ： 摄氏度
	 * @param minT 最低温 ： 摄氏度
	 */
	public void Update(float maxT, float minT){
		Message message = Message.obtain();
		message.what = UPDATE_VALUE;
		message.obj = new float[]{maxT, minT};
		mHandler.sendMessage(message);
	}

	public int getWidgetMode () { return widgetMode; }
	public void setWidgetMode (int widgetMode) {
		if (this.widgetMode == 1 && widgetMode ==0){//由固定温度条模式转化为正常模式
			mPercentSelectedMinValue = 0d;
			mPercentSelectedMaxValue = 1.0d;
		}
		thumbMaxCount = 50;
		this.widgetMode = widgetMode;
	}
	public List<Bitmap> getmProgressBarSelectBgList () {
		return mProgressBarSelectBgList; }
	public void setmProgressBarSelectBgList (List<Bitmap> mProgressBarSelectBgList) {
		this.mProgressBarSelectBgList = mProgressBarSelectBgList; }

	public boolean setPalette(int index ){
		if (mProgressBarSelectBgList!=null && index < mProgressBarSelectBgList.size()){
			mProgressBarSelectBg = mProgressBarSelectBgList.get(index);
			return true;
		}else {
			return false;
		}
	}




//	/**
//	 * 将dip或dp值转换为px值，保证尺寸大小不变
//	 *
//	 * @param dipValue （DisplayMetrics类中属性density）
//	 * @return
//	 */
//	private int dp2px(Context context, float dipValue) {
//		final float scale = context.getResources().getDisplayMetrics().density;
//		return (int) (dipValue * scale + 0.5f);
//	}
	//具体的温度转化成相对于 条子最高温距离的 百分比
	private double TempToPercent(float temp){
		return Math.max(0,(mMaxTemp- temp)/(mMaxTemp - mMinTemp));
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

		mThumbMaxImage = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_temp_seekbar_thumb_max_arrow);
		mThumbMinImage = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_temp_seekbar_thumb_min_arrow);
		btRangeMaxTemp = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_temp_seekbar_realtime_max_arrow);
		btRangeMinTemp = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_temp_seekbar_realtime_min_arrow);

		mTextSize = a.getDimension(R.styleable.MyCustomRangeSeekBar_SeekBarAllTextSize, DensityUtil.dp2px(context, 14));
		mPaint.setColor(ContextCompat.getColor(context,R.color.white));
		initPaint();//必须放这里

		initView();

		seekbarWidth = DensityUtil.dp2px(context, 8);//条子的宽度
		realTimeHighLowPicWidth = DensityUtil.dp2px(context,20);
		mThumbHeight = Math.max(mThumbMaxImage.getHeight(),DensityUtil.dp2px(context, 17));//滑动块的高度
		//滑块的宽度为 尖角的宽度 + 内容设置的宽度
		mThumbWidth = DensityUtil.dp2px(context,5)+Math.max(Math.max(mThumbMaxImage.getWidth(),
				DensityUtil.dp2px(context, 40)),mPaint.measureText(MAX_LENGTH_TEMP));//滑块的宽度
		// MyCustomRangeSeekBar: 40 dp == 40 999.9 length = > 35.0
//		Log.e(TAG, "MyCustomRangeSeekBar: 40 dp == " + dp2px(context,40) + " 999.9 length = > " + mPaint.measureText(MAX_LENGTH_TEMP));

		maxTempTextRect = new Rect();
		mPaint.getTextBounds(MAX_LENGTH_TEMP,0,MAX_LENGTH_TEMP.length(),maxTempTextRect);
		maxTempTextLength = mPaint.measureText(MAX_LENGTH_TEMP);
		//必须在画笔初始化之后去计算文字的高度
		topBottomPadding = (int) ((mThumbHeight + maxTempTextRect.height())*2);//顶部底部的padding 为 滑动块高度加2倍文字的高度
		//整个滑动条的背景颜色  底部坐标 通过绘制时计算得到
		mProgressBarRectBg = new RectF(maxTempTextLength + realTimeHighLowPicWidth,topBottomPadding,
				maxTempTextLength + realTimeHighLowPicWidth + seekbarWidth,topBottomPadding*2);
		//滑块之间的矩形（已选的渲染区间矩形）
		mProgressBarSelRect = new RectF(maxTempTextLength + realTimeHighLowPicWidth,topBottomPadding,
				maxTempTextLength + realTimeHighLowPicWidth + seekbarWidth,0);
		//滑块的矩形
		mThumbRect = new RectF(maxTempTextLength + realTimeHighLowPicWidth +seekbarWidth,topBottomPadding,
				maxTempTextLength + realTimeHighLowPicWidth + seekbarWidth + mThumbWidth , topBottomPadding + mThumbHeight);
		//实时高低温矩形
		realTimeTempPicRect = new RectF(maxTempTextLength,topBottomPadding,maxTempTextLength+ realTimeHighLowPicWidth,topBottomPadding+maxTempTextRect.height());

		a.recycle();
	}

	private void initView(){

	}

	private void initPaint(){
		mPaint.setTextSize(mTextSize);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setAlpha(255);
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int height = MIN_HEIGHT;

		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {//wrapContent
			height = MeasureSpec.getSize(heightMeasureSpec);
		}
		//宽度 = 滑动条宽度（包含了文字的宽度） + 条子宽度 + 右边文字的宽度 + 实时最高最低温图片的宽度
		int width = (int)(mThumbWidth + seekbarWidth + realTimeHighLowPicWidth
				+ maxTempTextLength);
//		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
//			width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
//		}

		setMeasuredDimension(width,height);
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		mProgressBarRectBg.bottom = getHeight() - topBottomPadding;

		onDrawBg(canvas);
		onDrawBp(canvas);
		onDrawText(canvas);
	}
	private void onDrawBg(Canvas canvas){
		//绘制条子的整体背景
		canvas.drawBitmap(mProgressBarBg,null,
				mProgressBarRectBg,mPaint);
	}

	private void onDrawBp(Canvas canvas){
		//绘制两个 滑块
		mThumbRect.bottom = percent2Height(mPercentSelectedMaxValue);
		mThumbRect.top = percent2Height(mPercentSelectedMaxValue) - mThumbHeight;
		canvas.drawBitmap(mThumbMaxImage,null,mThumbRect,mPaint);
		mThumbRect.bottom = percent2Height(mPercentSelectedMinValue)+ mThumbHeight;
		mThumbRect.top = percent2Height(mPercentSelectedMinValue);
		canvas.drawBitmap(mThumbMinImage,null,mThumbRect,mPaint);
		//固定温度条模式
		if (widgetMode ==1 ){
//			RectF recRange = new RectF(maxTempTextLength ,
//					0,maxTempTextLength + realTimeHighLowPicWidth,0);
			//绘制实时最高最低温图片
			realTimeTempPicRect.top = topBottomPadding+ Temp2Height(rangeMaxTemp) - btRangeMaxTemp.getHeight()/2.0f;
			realTimeTempPicRect.bottom =topBottomPadding+ Temp2Height(rangeMaxTemp) + btRangeMaxTemp.getHeight()/2.0f;
			canvas.drawBitmap(btRangeMaxTemp,null,realTimeTempPicRect,mPaint);

			realTimeTempPicRect.top = topBottomPadding+Temp2Height(rangeMinTemp) - btRangeMaxTemp.getHeight()/2.0f;
			realTimeTempPicRect.bottom = topBottomPadding+Temp2Height(rangeMinTemp) + btRangeMaxTemp.getHeight()/2.0f;
			canvas.drawBitmap(btRangeMinTemp,null,realTimeTempPicRect,mPaint);
		}
		//绘制两个滑块之间的 渲染矩形范围
		if (mProgressBarSelectBg != null){
			mProgressBarSelRect.top = percent2Height(mPercentSelectedMaxValue);
			mProgressBarSelRect.bottom = percent2Height(mPercentSelectedMinValue);
			//绘制两滑块中间的颜色
			canvas.drawBitmap(mProgressBarSelectBg ,null ,mProgressBarSelRect,mPaint);
		}
	}
	//绘制所有的文字
	private void onDrawText(Canvas canvas){
		if (realTimeMaxDCTemp > MAX_TEMP || realTimeMinDCTemp < MIN_TEMP){
			realTimeMaxDCTemp = Float.NaN;
			realTimeMinDCTemp = Float.NaN;
//
			mMaxTemp = Float.NaN;
			mMinTemp = Float.NaN;

			rangeMaxTemp = Float.NaN;
			rangeMinTemp = Float.NaN;
		}


		//绘制温度单位。
		canvas.drawText(DragTempContainer.tempSuffixList[tempUnitMode],
				maxTempTextLength+ realTimeHighLowPicWidth + seekbarWidth/2.0f - mPaint.measureText(DragTempContainer.tempSuffixList[tempUnitMode])
				,maxTempTextRect.height()+mPaint.descent(),mPaint);
		//绘制顶部最高温、底部最低温
		canvas.drawText(Float2Str(mMaxTemp), maxTempTextLength+ realTimeHighLowPicWidth + (seekbarWidth - maxTempTextLength)/2,
				 maxTempTextRect.height()*2-mPaint.ascent(),mPaint);
		canvas.drawText(Float2Str(mMinTemp), maxTempTextLength+ realTimeHighLowPicWidth + (seekbarWidth - maxTempTextLength)/2,
				getHeight() - maxTempTextRect.height()*2- mPaint.descent(),mPaint);
		//绘制滑块最高温、最低温
		//计算滑块文字的X轴位置：(mthumbwidth- mpaint.measuretext("maxtempStr"))/2
//		Math.abs((mThumbWidth-20) - mPaint.measureText(Float2Str(mThumbMaxTemp)))/2
		canvas.drawText(Float2Str(percent2Temp(mPercentSelectedMaxValue)),getWidth()- mPaint.measureText(Float2Str(percent2Temp(mPercentSelectedMaxValue))),
				percent2Height(mPercentSelectedMaxValue) - mThumbHeight/2.0f + mPaint.descent(),mPaint);

		canvas.drawText(Float2Str(percent2Temp(mPercentSelectedMinValue)),getWidth()- mPaint.measureText(Float2Str(percent2Temp(mPercentSelectedMinValue))),
				percent2Height(mPercentSelectedMinValue) + mThumbHeight/2.0f + mPaint.descent(),mPaint);

		//绘制实时最高最低温文字
		if (widgetMode ==1 ){
			canvas.drawText(Float2Str(rangeMaxTemp),maxTempTextLength - mPaint.measureText(Float2Str(rangeMaxTemp)),
					topBottomPadding+ Temp2Height(rangeMaxTemp)+mPaint.descent(),mPaint);
			canvas.drawText(Float2Str(rangeMinTemp),maxTempTextLength - mPaint.measureText(Float2Str(rangeMinTemp)),
					topBottomPadding+ Temp2Height(rangeMinTemp)+mPaint.descent(),mPaint);
		}
//		//右侧实时高低温的左边界线
//		canvas.drawLine(mThumbMaxImage.getWidth() + seekbarWidth ,
//				0,mThumbMaxImage.getWidth() + seekbarWidth ,
//				topBottomPadding,mPaint);
//		//起始刻度
//		canvas.drawLine(mThumbMaxImage.getWidth(),topBottomPadding,mThumbMaxImage.getWidth()+seekbarWidth,topBottomPadding,mPaint);
//		//四分之一刻度
//		canvas.drawLine(mThumbMaxImage.getWidth(),(getHeight()-2.0f*topBottomPadding)/4+topBottomPadding,
//				mThumbMaxImage.getWidth()+seekbarWidth,(getHeight()-2.0f*topBottomPadding)/4+topBottomPadding,mPaint);
//		//四分之二刻度
//		canvas.drawLine(mThumbMaxImage.getWidth(),getHeight()/2.0f,mThumbMaxImage.getWidth()+seekbarWidth,getHeight()/2.0f,mPaint);
//		//四分之三刻度
//		canvas.drawLine(mThumbMaxImage.getWidth(),(getHeight()-2.0f*topBottomPadding)/4*3+topBottomPadding,
//				mThumbMaxImage.getWidth()+seekbarWidth,(getHeight()-2.0f*topBottomPadding)/4*3+topBottomPadding,mPaint);
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

	public ThumbListener getmThumbListener () { return mThumbListener; }
	public void setmThumbListener (ThumbListener mThumbListener) {
		this.mThumbListener = mThumbListener;
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (!mEnable)
			return true;
		//根据触发的时间去 计算
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mPressedThumb = evalPressedThumb(event.getY());
//				Log.e(TAG, "onTouchEvent: "+ mPressedThumb);
				invalidate();
				//Intercept parent TouchEvent
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;

			case MotionEvent.ACTION_MOVE:
//				Log.e(TAG, "onTouchEvent: ACTION_MOVE"  + "  " + mPressedThumb);
				if (mPressedThumb != null) {
//					Log.e(TAG, "onTouchEvent: ACTION_MOVE"  + "  " + mPressedThumb);
					float eventY = event.getY();
					float maxValue = percentToAbsoluteValue(mPercentSelectedMaxValue);//最高值[0,100]
					float minValue = percentToAbsoluteValue(mPercentSelectedMinValue);//最低值[0,100]
					float eventValue = percentToAbsoluteValue(screenToPercent(eventY));//得到点距离顶部的百分比 [0-100]
//					Log.e(TAG, "onTouchEvent: value  "+ eventValue);
					if (Thumb.MIN.equals(mPressedThumb)) {
						minValue = eventValue;
						if (mBetweenAbsoluteValue > 0 && maxValue - minValue <= mBetweenAbsoluteValue)
							minValue = new Float((maxValue - mBetweenAbsoluteValue));
						setPercentSelectedMinValue(absoluteValueToPercent(minValue));
						if (mThumbListener != null){
//							if (widgetMode==0){
								mThumbListener.onMinMove(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue()
										,percent2DegreeCentigrade(mPercentSelectedMaxValue),percent2DegreeCentigrade(mPercentSelectedMinValue));
//										,percent2Temp(mPercentSelectedMaxValue),percent2Temp(mPercentSelectedMinValue));
//							}else {//取交集
//
//							}
						}
					} else if (Thumb.MAX.equals(mPressedThumb)) {
						maxValue = eventValue;
						if (mBetweenAbsoluteValue > 0 && maxValue - minValue <= mBetweenAbsoluteValue)
							maxValue = new Float(minValue + mBetweenAbsoluteValue);

						setPercentSelectedMaxValue(absoluteValueToPercent(maxValue));
						if (mThumbListener != null){
//							if (widgetMode==0){
								mThumbListener.onMaxMove(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue()
										,percent2DegreeCentigrade(mPercentSelectedMaxValue),percent2DegreeCentigrade(mPercentSelectedMinValue));
//										,percent2Temp(mPercentSelectedMaxValue),percent2Temp(mPercentSelectedMinValue));
//							}else {
//
//							}
						}

					}
				}
				//Intercept parent TouchEvent
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;

			case MotionEvent.ACTION_UP:
//				Log.e(TAG, "onTouchEvent: ACTION_UP"  + "  " + mPressedThumb);
				//抬起时,判断弹起的是 最高值 还是最低值，并回调对应的 抬起函数
				if (Thumb.MIN.equals(mPressedThumb)) {
					if (mThumbListener != null)
						mThumbListener.onUpMinThumb(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue()
								,percent2DegreeCentigrade(mPercentSelectedMaxValue),percent2DegreeCentigrade(mPercentSelectedMinValue));
//								,percent2Temp(mPercentSelectedMaxValue),percent2Temp(mPercentSelectedMinValue));
				}
				if (Thumb.MAX.equals(mPressedThumb)) {
					if (mThumbListener != null)
						mThumbListener.onUpMaxThumb(getSelectedAbsoluteMaxValue(), getSelectedAbsoluteMinValue()
								,percent2DegreeCentigrade(mPercentSelectedMaxValue),percent2DegreeCentigrade(mPercentSelectedMinValue));
//								,percent2Temp(mPercentSelectedMaxValue),percent2Temp(mPercentSelectedMinValue));
				}
				//Intercept parent TouchEvent
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;
			case MotionEvent.ACTION_CANCEL:
//				Log.e(TAG, "onTouchEvent: ACTION_CANCEL"  + "  " + mPressedThumb);
				mPressedThumb = null;
				//Intercept parent TouchEvent
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;
		}

		return true;
	}
	/**
	 * 根据touchY, 判断是哪一个thumb(Min or Max)
	 * @param touchY 触摸的Y在屏幕中坐标（相对于容器）
	 */
	private MyCustomRangeSeekBar.Thumb evalPressedThumb(float touchY) {
		MyCustomRangeSeekBar.Thumb result = null;
		boolean minThumbPressed = isInThumbRange(touchY, mPercentSelectedMinValue);
		boolean maxThumbPressed = isInThumbRange(touchY, mPercentSelectedMaxValue);
		if (minThumbPressed && maxThumbPressed) {
			// if both thumbs are pressed (they lie on top of each other), choose the one with more room to drag. this avoids "stalling" the thumbs in a corner, not being able to drag them apart anymore.
			result = touchY>= percentToScreen(mPercentSelectedMinValue)? MyCustomRangeSeekBar.Thumb.MIN : MyCustomRangeSeekBar.Thumb.MAX;
		} else if (minThumbPressed) {
			result = MyCustomRangeSeekBar.Thumb.MIN;
		} else if (maxThumbPressed) {
			result = MyCustomRangeSeekBar.Thumb.MAX;
		}
		return result;
	}

	/**
	 * <p>  判断 touchY 是否在滑块点击范围内    </p>
	 * @param touchY            需要被检测的 屏幕中的Y坐标（相对于容器）
	 * @param percentThumbValue 需要检测的滑块 Y 坐标百分比值（滑块Y 坐标）
	 */
	private boolean isInThumbRange(float touchY, double percentThumbValue) {
		return Math.abs(touchY - percentToScreen(percentThumbValue)) <= 2*mThumbHeight;
	}
	/**
	 * <p>  进度值，从百分比值转换到屏幕中坐标值 left&top的绘制模式    </p>
	 * <p>  (getHeight() - 2 * topBottomPadding)  代表 条子的高度  </p>
	 * <p>  (1 - percentValue) 代表 距离顶部的百分比  </p>
	 * <p>  topBottomPadding+ 顶部的距离 就是百分比所在的中心位置    </p>
	 */
	private float percentToScreen(double percentValue) {
		return (float) (topBottomPadding + (1 - percentValue) * (getHeight() - 2 * topBottomPadding));
	}
	/**
	 * 返回最大值滑块 的温度数值
	 * @return 返回最大值滑块 的温度数值float类型
	 */
	public float getSelectedAbsoluteMaxTemp() {
		return percentToAbsoluteTemp(mPercentSelectedMaxValue); }

	/**
	 * 滑块最大值百分比 转化成 0-100之间的float数值。
	 * @return
	 */
	public float getSelectedAbsoluteMaxValue() {
		return percentToAbsoluteValue(mPercentSelectedMaxValue); }
	/**
	 * 滑块最小值百分比 转化成 0-100之间的float数值。
	 * @return
	 */
	public float getSelectedAbsoluteMinValue() {
		return percentToAbsoluteValue(mPercentSelectedMinValue);
	}
	/**
	 * 百分比乘以最大值减去最小值，然后加上最小值。为返回的温度
	 * @param percent   百分比
	 * @return      (float) (mMinTemp + percent * (mMaxTemp - mMinTemp));
	 */
	private float percentToAbsoluteTemp(double percent) {
		return (float) (mMinTemp + percent * (mMaxTemp - mMinTemp));
	}
	/**
	 * 进度值，从百分比到绝对值 取值为 0 - 100
	 * @return  (float) percent * 100;
	 */
	private float percentToAbsoluteValue(double percent) {
		return (float) percent * 100;
	}
	/**
	 * <p>  进度值，转换屏幕像素值到百分比值     </p>
	 * <p> 返回的是 [0-1] 之间的数值 </p>
	 *
	 *
	 *
	 */
	private double screenToPercent(float screenCoord) {
		int height = getHeight();
		if (height <= 2 * topBottomPadding) {//整体控件的高度一定要大于上与下的间隔之和
			// prevent division by zero, simply return 0.
			return 0d;
		} else {
			//点的Y值 对应在 条子的 位置 百分比。  再用1减去百分比，得到点到顶部的百分比。
			double result = 1 - (screenCoord - topBottomPadding) / (height - 2 * topBottomPadding);//？？？？为什么要用用一减去这个百分比？
			return Math.min(1d, Math.max(0d, result));//返回的是 0-1 之间的数值
		}
	}
	/**
	 * 进度值，从绝对值到百分比
	 * @param value 绝对值 [0,99]
	 */
	private double absoluteValueToPercent(float value) {
		if (0 == mAbsoluteMaxValue - mAbsoluteMinValue) {
			// prevent division by zero, simply return 0.
			return 0d;
		}
		return (value - mAbsoluteMinValue) / (mAbsoluteMaxValue - mAbsoluteMinValue);
	}
	/**
	 * 设置已选择最小值的百分比值
	 * [0 , value , 1]
	 */
	public void setPercentSelectedMinValue(double value) {
		mPercentSelectedMinValue = Math.max(0d, Math.min(1d, Math.min(value, mPercentSelectedMaxValue)));
		invalidate();
	}
	/**
	 * [0 , value , 1]
	 * 设置已选择最大值的百分比值
	 */
	public void setPercentSelectedMaxValue(double value) {
		mPercentSelectedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, mPercentSelectedMinValue)));
		invalidate();
	}



	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}
}
