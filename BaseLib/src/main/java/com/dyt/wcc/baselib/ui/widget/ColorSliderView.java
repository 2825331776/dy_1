package com.dyt.wcc.baselib.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.dyt.wcc.baselib.R;


/**
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：${DATE} ${TIME}     </p>
 * <p>PackagePath: com.dyt.wcc.baselib.ui.widget     </p>
 * <p>Description：       </p>
 */
public class ColorSliderView extends View {
	public ColorSliderView (Context context) {
		this(context, null);
	}

	public ColorSliderView (Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ColorSliderView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public ColorSliderView (Context context, @Nullable AttributeSet attrs, int defStyleAttr,
	                        int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initAttrs(context, attrs);

		initData();
	}

	private void initAttrs (Context context, AttributeSet attrs) {
		final TypedArray array = context.obtainStyledAttributes(attrs,
				R.styleable.ColorSliderView);
		mIndicatorColor = array.getColor(R.styleable.ColorSliderView_slider_indicator_color,
				Color.WHITE);

		selectPercent = array.getFloat(R.styleable.ColorSliderView_indicator_percent, -1);

		int or = array.getInteger(R.styleable.ColorSliderView_slider_orientation, 0);
		orientation = ((or == 0) ? Orientation.HORIZONTAL : Orientation.VERTICAL);

		array.recycle();
	}

	private void initData () {

	}
	//------------------------指示器-----------------------
	/**
	 * 指示器颜色
	 */
	private       int    mIndicatorColor;
	/**
	 * 指示器 画笔
	 */
	private final Paint  paintIndicator;
	/**
	 * indicator bitmap 绘制的 圆角矩形/圆形
	 */
	private final Rect   rectIndicator = new Rect();
	//选中的百分比 ,便于 设置预设值。
	private       float  selectPercent = 1;
	//	/**
	//	 * 指示器是否可用
	//	 */
	//	private       boolean mIndicatorEnable;
	/**
	 * 指示器半径
	 */
	private       int    mRadius;
	/**
	 * indicator bitmap
	 */
	private       Bitmap bitmapForIndicator;

	//-----------------颜色 tab ,及其view------------------
	/**
	 * View 和 tab 的画笔
	 */
	private final Paint          paintTab;
	/**
	 * 线性 渐变
	 */
	private       LinearGradient linearGradient;
	/**
	 * padding 坐标
	 */
	private       int            mTop, mLeft, mRight, mBottom;
	/**
	 * 颜色tab 的 圆角矩形
	 */
	private final Rect        rectTab = new Rect();
	/**
	 * 控件方向
	 */
	private       Orientation orientation;
	/**
	 * tab bitmap
	 */
	private       Bitmap      bitmapForTab;

	//默认 长边 与 短边的 比值为 10：1
	private static final int defaultSizeLong  = 500;
	private static final int defaultSizeShort = 50;

	/**
	 * 是否需要重绘 tab 颜色条
	 */
	private boolean needReDrawTab = true;
	/**
	 * 是否需要重绘 指示器
	 */
	//	private boolean needReDrawIndicator = true;

	/**
	 * 手指 坐标
	 */
	private int currentX, currentY;

	/**
	 * 渐变颜色 范围
	 */
	private int[] colors = null;
	/**
	 * 先选中颜色
	 */
	private int   currentColor;


	/**
	 * 控件方向
	 */
	public enum Orientation {
		/**
		 * 水平
		 */
		HORIZONTAL, // 0
		/**
		 * 竖直
		 */
		VERTICAL // 1
	}

	private static final String TAG = "-ColorPickBar-";

	{
		Log.e(TAG, "instance initializer: ");
		bitmapForTab = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		bitmapForIndicator = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

		//
		this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		paintTab = new Paint();
		//无锯齿
		paintTab.setAntiAlias(true);
		paintTab.setColor(Color.WHITE);
		paintTab.setStrokeWidth(5);

		paintIndicator = new Paint();
		paintIndicator.setAntiAlias(true);

		currentX = currentY = Integer.MAX_VALUE;
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int width, height;

		if (widthMode == MeasureSpec.AT_MOST) { //AT_MOST->wrap_content
			Log.e(TAG, "onMeasure: AT_MOST");
		} else if (widthMode == MeasureSpec.EXACTLY) {// 60dp->2次，0dp->四次,match_parent->2次
			Log.e(TAG, "onMeasure: EXACTLY");
		} else if (widthMode == MeasureSpec.UNSPECIFIED) {
			Log.e(TAG, "onMeasure: UNSPECIFIED");
		}

		if (widthMode == MeasureSpec.EXACTLY) { //AT_MOST->wrap_content
			width = widthSize;
		} else {
			width = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
		}

		if (heightMode == MeasureSpec.EXACTLY) {
			height = heightSize;
		} else {
			height = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
		}

		width = Math.max(width, (orientation == Orientation.HORIZONTAL) ? defaultSizeLong :
				defaultSizeShort);
		height = Math.max(height, (orientation == Orientation.HORIZONTAL) ? defaultSizeShort :
				defaultSizeLong);

		Log.e(TAG, "onMeasure: width ==>" + width + " height ==> " + height);

		setMeasuredDimension(width, height);
		//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}


	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		mTop = getPaddingTop();
		mBottom = getMeasuredHeight() - getPaddingBottom();
		mLeft = getPaddingLeft();
		mRight = getMeasuredWidth() - getPaddingRight();


		//算出tab渐变色条 边界
		calculateBounds();
		if (colors == null) {
			setColors(createDefaultColorTable());
		} else {
			setColors(colors);
		}
		createBitmap();
		//优先 计算 tab渐变色条的边界，才能通过 边界及其 选中的百分比 计算出currentX 和Y
		if ((currentX == currentY || currentY == Integer.MAX_VALUE) && selectPercent == -1) {
			currentX = getWidth() / 2;
			currentY = getHeight() / 2;
		} else {
			if (orientation == Orientation.VERTICAL) {
				currentX = getWidth() / 2;
				currentY = (int) (mTop + mRadius + rectTab.height() * (1 - selectPercent));
			} else {
				currentX = (int) (mLeft + mRadius + rectTab.width() * (1 - selectPercent));
				currentY = getHeight() / 2;
			}
			//			Log.e(TAG, "onLayout: current select indicator init coordinate");
		}

	/*	Log.e(TAG, "此时函数中的 : top " + top + " left " + left + " right " + right + " bottom " +
	bottom);

		Log.e(TAG, "onLayout: getWidth = " + getWidth() + " getHeight == " + getHeight());

		Log.e(TAG, "onLayout: measureWidth ==" + getMeasuredWidth() + " measureHeight==> " +
		getMeasuredHeight());
		Log.e(TAG, "onLayout : top " + mTop + " left " + mLeft + " right " + mRight + " bottom " +
		 mBottom);

		Log.e(TAG, "padding: top " + getPaddingTop() + " left " + getPaddingLeft() + " right " +
		getPaddingRight() + " bottom " + getPaddingBottom());*/

	}

	/**
	 * 已知四个边界 top left bottom right ，width  height，计算 tab 颜色的边界。
	 * 放置方位： 水平  horizontal
	 * 出去 paddingTop， paddingBottom ，整体分为九份；
	 * 2/9:预留的空白区域
	 * 1/9: 指示器 色条 上半部分圆的高度
	 * 3/9: 指示器 在色条上的 高度
	 * 1/9:指示器 在色条 下半部分的 高度
	 * 2/9:预留的空白区域
	 */
	private void calculateBounds () {//设置的是 tab 色条 rect 的边界
		//分割分数
		final int average = 9;
		//每份长度 （宽度/高度,根据放置方位决定）
		int perLength = 0;
		//计算控件 宽高。 padding已经减去了
		int h = mBottom - mTop;
		int w = mRight - mLeft;

		int size = Math.min(w, h);
		//特殊情况，水平时：高度大于宽度。垂直时：宽度大于高度； size 取最小值的六分之一。
		if (orientation == Orientation.HORIZONTAL) {
			if (w <= h) {// 正常情况 w >h
				size = w / 6;
			}
		} else if (orientation == Orientation.VERTICAL) {
			if (w >= h) {
				size = h / 6;
			}
		}
		//特殊情况： 水平时：宽度被分成54份，垂直时，高度被分成 54份。
		perLength = size / average;
		//指示器，占 5/9宽度，色条占 3/9
		mRadius = perLength * 5 / 2;//半径

		int t, l, b, r;
		//颜色条的 半个个宽度 perLength *3
		// 矩形色条的宽度 为： perLength *3; 高度为 h - 2*mRadius
		final int tabHalfWidth = perLength * 3 / 2;

		if (orientation == Orientation.HORIZONTAL) {
			l = mLeft + mRadius;
			r = mRight - mRadius;

			t = (getHeight() / 2) - tabHalfWidth;
			b = (getHeight() / 2) + tabHalfWidth;
		} else {
			t = mTop + mRadius;
			b = mBottom - mRadius;

			l = getWidth() / 2 - tabHalfWidth;
			r = getWidth() / 2 + tabHalfWidth;
		}
		//rectTab 设置边界：
		rectTab.set(l, t, r, b);
	}

	/**
	 * 垂直方位：
	 * 最短边分界线:11份,宽度
	 * 3：空白
	 * 1: 上半区域
	 * 3:色条主体
	 * 1：下半区域
	 * 3：留白
	 * <p>
	 * 长度为 ：最长长度的 20/1
	 */
	private void calculateBounds_min () {//设置的是 tab 色条 rect 的边界
		//分割分数
		final int average_vertical = 11;
		//每份长度（宽度/高度,根据放置方位决定）
		int perWidth = 0;
		//计算控件 宽高。
		int h = mBottom - mTop;
		int w = mRight - mLeft;

		int size = Math.min(w, h);

		if (orientation == Orientation.HORIZONTAL) {
			if (w <= h) {// 正常w >h ,
				size = w / 6;
			}
		} else if (orientation == Orientation.VERTICAL) {
			if (w >= h) {//垂直 的宽度 大于 了高度，把高度分成六分。
				size = h / 6;
			}
		}
		//perLength 被分成了 6*9 = 54 份，
		//水平时：宽度被分成54份，垂直时，高度被分成 54份。也就是按理最长的部分。
		perWidth = size / average_vertical;
		mRadius = perWidth * 7 / 2;//半径

		int t, l, b, r;
		final int s = perWidth * 3 / 2;

		if (orientation == Orientation.HORIZONTAL) {
			l = mLeft + mRadius;
			r = mRight - mRadius;

			t = (getHeight() / 2) - s;
			b = (getHeight() / 2) + s;
		} else {
			t = mTop + mRadius;
			b = mBottom - mRadius;

			l = getWidth() / 2 - s;
			r = getWidth() / 2 + s;
		}

		rectTab.set(l, t, r, b);
	}

	private int[] createDefaultColorTable () {
		return new int[]{Color.rgb(255, 0, 0), Color.rgb(255, 255, 0), Color.rgb(0, 255, 0),
				Color.rgb(0, 255, 255), Color.rgb(0, 0, 255), Color.rgb(255, 0, 255),
				Color.rgb(255, 0, 0)};
	}

	/**
	 * 设置 linearGradient 的渐变色
	 *
	 * @param colors
	 */
	public void setColors (int... colors) {
		linearGradient = null;
		this.colors = colors;

		if (orientation == Orientation.HORIZONTAL) {
			linearGradient = new LinearGradient(rectTab.left, rectTab.top, rectTab.right,
					rectTab.top, colors, null, Shader.TileMode.CLAMP);
		} else {
			linearGradient = new LinearGradient(rectTab.left, rectTab.top, rectTab.left,
					rectTab.bottom, colors, null, Shader.TileMode.CLAMP);
		}
		//重画 tab
		needReDrawTab = true;
		invalidate();
	}

	/**
	 * 创建两个画布
	 */
	private void createBitmap () {
		//tab 宽 高
		int ht = rectTab.height();
		int wt = rectTab.width();
		// indicator 宽高
		int hi = mRadius * 2;
		int wi = hi;

		//回收
		if (bitmapForTab != null) {
			if (!bitmapForTab.isRecycled()) {
				bitmapForTab.recycle();
				bitmapForTab = null;
			}
		}
		if (bitmapForIndicator != null) {
			if (!bitmapForIndicator.isRecycled()) {
				bitmapForIndicator.recycle();
				bitmapForIndicator = null;
			}
		}
		//创建
		bitmapForTab = Bitmap.createBitmap(wt, ht, Bitmap.Config.ARGB_8888);
		bitmapForIndicator = Bitmap.createBitmap(wi, hi, Bitmap.Config.ARGB_8888);
	}


	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		//		Log.e(TAG, "-------颜色选择器的 onDraw:---------------------- ");
		// ------绘制 色条底色---再绘制 渐变色条--------------
		/*通过下面的 三个测试线，
		确定 整个 控件的宽高 为：onMeasure:getMeasureWidth,getMeasureHeight。
		onLayout:110 getWidth，700 getHeight
		最左侧为坐标原点。
		 */
		/*canvas.drawLine(0, 225, 110, 225, paintTab);
		canvas.drawLine(0, 700, 110, 700, paintTab);
		canvas.drawLine(100, 0, 100, 700, paintTab);*/

		if (needReDrawTab) {
			createColorTableBitmap();//运行之后重置 needReDrawTab为false。
		}
		// 绘制 bitmapForTab 到总 canvas 上。
		canvas.drawBitmap(bitmapForTab, null, rectTab, paintTab);

		//--------------绘制 指示器-----------

		//		if (needReDrawIndicator) {
		createIndicatorBitmap();
		//		}
		rectIndicator.set(currentX - mRadius, currentY - mRadius, currentX + mRadius,
				currentY + mRadius);
		canvas.drawBitmap(bitmapForIndicator, null, rectIndicator, paintIndicator);

		//测试 绘制
		//		canvas.drawLine(0, mBottom, mRight, mBottom, paintTab);
	}

	/**
	 * 在 bitmapForTab的canvas 上绘制 背景，及其渐变色条。
	 */
	private void createColorTableBitmap () {
		Canvas canvas = new Canvas(bitmapForTab);
		RectF rectF = new RectF(0, 0, bitmapForTab.getWidth(), bitmapForTab.getHeight());
		//圆角大小
		int radius;
		if (orientation == Orientation.HORIZONTAL) {
			radius = bitmapForTab.getHeight() / 2;
		} else {
			radius = bitmapForTab.getWidth() / 2;
		}
		//先绘制 黑色背景，否则有alpha 时 绘制不正常
		paintTab.setColor(Color.BLACK);
		canvas.drawRoundRect(rectF, radius, radius, paintTab);

		paintTab.setShader(linearGradient);
		canvas.drawRoundRect(rectF, radius, radius, paintTab);
		paintTab.setShader(null);

		needReDrawTab = false;
	}

	private void createIndicatorBitmap () {
		Canvas canvas = new Canvas(bitmapForIndicator);
		//		RectF rectF = new RectF(0, 0, bitmapForIndicator.getWidth(), bitmapForIndicator
		//		.getHeight());

		int radius = 3;
		paintIndicator.setShadowLayer(radius, 0, 0, Color.GRAY);
		paintIndicator.setColor(mIndicatorColor);
		//		paintIndicator.setStyle(Paint.Style.STROKE);
		//		paintIndicator.setStrokeWidth(3);
		//		canvas.drawRoundRect(rectF, mRadius / 2.0f, mRadius / 2.0f, paintIndicator);
		canvas.drawCircle(mRadius, mRadius, mRadius - radius, paintIndicator);

		//		needReDrawIndicator = false;
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		int ex = (int) event.getX();
		int ey = (int) event.getY();

		if (!inBoundOfColorTable(ex, ey)) {
			return true;
		}

		if (orientation == Orientation.HORIZONTAL) {
			currentX = ex;
			currentY = getHeight() / 2;
			selectPercent = 1 - (currentX - mRadius - getPaddingLeft()) * 1.0f / rectTab.width();
		} else {//垂直的
			currentX = getWidth() / 2;
			currentY = ey;
			selectPercent = 1 - (currentY - mRadius - getPaddingTop()) * 1.0f / rectTab.height();
		}


		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			if (colorPickerChangeListener != null) {
				colorPickerChangeListener.onStartTrackingTouch(this);
				calcuColor();
				colorPickerChangeListener.onColorChanged(this, currentColor, selectPercent);
			}

		} else if (event.getActionMasked() == MotionEvent.ACTION_UP) { //手抬起
			if (colorPickerChangeListener != null) {
				colorPickerChangeListener.onStopTrackingTouch(this);
				calcuColor();
				colorPickerChangeListener.onColorChanged(this, currentColor, selectPercent);
			}

		} else { //按着+拖拽
			if (colorPickerChangeListener != null) {
				calcuColor();
				colorPickerChangeListener.onColorChanged(this, currentColor, selectPercent);
			}
		}
		//		Log.e(TAG, "onTouchEvent: ---------selectPercent==>" + selectPercent);
		invalidate();
		return true;
	}

	private boolean inBoundOfColorTable (int ex, int ey) {
		if (orientation == Orientation.HORIZONTAL) {
			return (ex > mLeft + mRadius && ex < mRight - mRadius);
		} else {
			return ey > mTop + mRadius && ey < mBottom - mRadius;
		}
	}


	private int calcuColor () {
		int x, y;
		if (orientation == Orientation.HORIZONTAL) { // 水平
			y = (rectTab.bottom - rectTab.top) / 2;
			if (currentX < rectTab.left) {
				x = 1;
			} else if (currentX > rectTab.right) {
				x = bitmapForTab.getWidth() - 1;
			} else {
				x = currentX - rectTab.left;
			}
		} else { // 竖直
			x = (rectTab.right - rectTab.left) / 2;
			if (currentY < rectTab.top) {
				y = 1;
			} else if (currentY > rectTab.bottom) {
				y = bitmapForTab.getHeight() - 1;
			} else {
				y = currentY - rectTab.top;
			}
		}
		int pixel = bitmapForTab.getPixel(x, y);
		currentColor = pixelToColor(pixel);
		return currentColor;
	}

	private int pixelToColor (int pixel) {
		int alpha = Color.alpha(pixel);
		int red = Color.red(pixel);
		int green = Color.green(pixel);
		int blue = Color.blue(pixel);

		return Color.argb(alpha, red, green, blue);
	}

	private OnColorPickerChangeListener colorPickerChangeListener;

	public void setOnColorPickerChangeListener (OnColorPickerChangeListener l) {
		this.colorPickerChangeListener = l;
	}

	//设置 滑块的百分比。
	public void setSelectPercent (float selectPercent) {
		this.selectPercent = selectPercent;
		calculateCurrentXY(selectPercent);
		invalidate();
	}

	public float getSelectPercent () {
		return selectPercent;
	}

	private void calculateCurrentXY (float percent) {
		if (orientation == Orientation.HORIZONTAL) {
			currentY = getHeight() / 2;
			if (percent == 0) {
				currentX = mLeft + mRadius;
			} else if (percent == 1) {
				currentX = mRight;
			} else {
				currentX = (int) (mLeft + mRadius + (rectTab.width() * (1-percent)));
			}
		} else {
			currentX = getWidth() / 2;
			if (percent == 0) {
				currentY = rectTab.height() + mRadius + getPaddingTop();
			} else if (percent == 1) {
				currentY = getPaddingTop() + mRadius;
			} else {
				currentY = (int) (getPaddingTop() + mRadius + (rectTab.height() * (1-percent)));
			}
		}
	}

	public interface OnColorPickerChangeListener {

		/**
		 * 选取的颜色值改变时回调
		 *
		 * @param sliderView ColorPickerView
		 * @param color      颜色
		 */
		void onColorChanged (ColorSliderView sliderView, int color, float percent);

		/**
		 * 开始颜色选取
		 *
		 * @param sliderView ColorPickerView
		 */
		void onStartTrackingTouch (ColorSliderView sliderView);

		/**
		 * 停止颜色选取
		 *
		 * @param sliderView ColorPickerView
		 */
		void onStopTrackingTouch (ColorSliderView sliderView);
	}

	private class SavedState extends BaseSavedState {
		int selX, selY;
		int[]  colors;//色表  ,后续还有一个 positions 的 数组也可以储存，表示按照什么百分比分布
		Bitmap color;//tab渐进色条 的bitmap
		Bitmap indicator = null; // 指示器的bitmap
		float  selectedPercent;


		SavedState (Parcelable source) {
			super(source);
		}

		@Override
		public void writeToParcel (Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(selX);
			out.writeInt(selY);
			out.writeParcelable(color, flags);
			out.writeIntArray(colors);
			out.writeFloat(selectedPercent);
			if (indicator != null) {
				out.writeParcelable(indicator, flags);
			}
		}
	}

	@Override
	protected Parcelable onSaveInstanceState () {
		Parcelable parcelable = super.onSaveInstanceState();
		SavedState ss = new SavedState(parcelable);
		ss.selX = currentX;
		ss.selY = currentY;
		ss.color = bitmapForTab;
		//		if (mIndicatorEnable) {
		ss.indicator = bitmapForIndicator;
		ss.selectedPercent = selectPercent;
		//		}
		return ss;
	}

	@Override
	protected void onRestoreInstanceState (Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		currentX = ss.selX;
		currentY = ss.selY;
		colors = ss.colors;

		bitmapForTab = ss.color;
		//		if (mIndicatorEnable) {
		bitmapForIndicator = ss.indicator;
		selectPercent = ss.selectedPercent;
		//		needReDrawIndicator = true;
		//		}
		//		needReDrawColorTable = true;
	}
}
