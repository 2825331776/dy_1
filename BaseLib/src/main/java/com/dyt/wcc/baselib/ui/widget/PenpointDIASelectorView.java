package com.dyt.wcc.baselib.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.dyt.wcc.baselib.R;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/21  11:31     </p>
 * <p>Description：笔尖 直径 选择
 * /n 顺序 多次onMeasure -> onSizeChanged -> layout -> onDraw</p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.view     </p>
 */
public class PenpointDIASelectorView extends View /*implements ScrollView */ {

	public PenpointDIASelectorView (Context context) {
		this(context, null);
	}

	public PenpointDIASelectorView (Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PenpointDIASelectorView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public PenpointDIASelectorView (Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initAttrs(context, attrs);

		initPaint(context);
	}


	//默认值
	private final int DEFAULT_ITEM_COUNT               = 5;//默认子item数量
	@ColorInt
	private final int DEFAULT_ITEM_BORDER_COLOR        = Color.WHITE;//默认item 边框颜色
	@ColorInt
	private final int DEFAULT_ITEM_BORDER_COLOR_SELECT = Color.BLUE;//默认item 边框颜色
	@ColorInt
	private final int DEFAULT_ITEM_FILL_COLOR          = Color.WHITE;//默认item 填充颜色

	private final int    DEFAULT_PAINT_FONT_SIZE = 16;//默认 item 字体大小
	private final String TAG                     = "-PaintSelector-";
	private final int    DEFAULT_SELECT_INDEX    = 0;

	private int backgroundColor = Color.GREEN;

	//自有属性

	private int itemCount             = DEFAULT_ITEM_COUNT;
	@ColorInt
	private int itemBorderColor       = DEFAULT_ITEM_BORDER_COLOR;//
	@ColorInt
	private int itemBorderSelectColor = DEFAULT_ITEM_BORDER_COLOR_SELECT;//选中边框颜色
	private int itemFillColor         = DEFAULT_ITEM_FILL_COLOR;
	private int paintFontSize         = DEFAULT_PAINT_FONT_SIZE/2;//画笔粗细
	private int borderFontSize        = DEFAULT_PAINT_FONT_SIZE / 2;//边框粗细

	private Paint contentPaint;//内容专用
	private Paint borderPaint;//背景，边框 画笔
	private int   selectIndex = DEFAULT_SELECT_INDEX;//选中的index

	private float percentWidgetWidth = 0.0f;//item 宽高 占 控件 宽度的百分比。
	private int   itemWidthHeight    = 0;//item 宽高,必须正方
	private int   perMarginTB        = 0;//item上下间隔
	private int   perMarginLR        = 0;

	private int marginTopBottom = 20;//整体 top bottom margin
	private int measureWidth, measureHeight;
	/*
	marginTop Bottom:整体item 的margin 。 perMargin :每个item 的margin
	宽高：高度 == itemWidthHeight * itemCount + 2* marginTop
	*/

	/*每个item 的半径 */
	private int itemRadius = 0;

	//第一个item 的坐标
	private int firstItemX;
	private int firstItemY;

	//动画
	private ValueAnimator animator;

	//回调接口
	private OnPaintSizeSelectorListener selectorListener;

	public OnPaintSizeSelectorListener getSelectorListener () {
		return selectorListener;
	}

	public void setSelectorListener (OnPaintSizeSelectorListener selectorListener) {
		this.selectorListener = selectorListener;
	}

	private void initAttrs (Context context, AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PenpointDIASelectorView);
		if (typedArray != null) {
			itemCount = typedArray.getInt(R.styleable.PenpointDIASelectorView_itemCount, DEFAULT_ITEM_COUNT);
			percentWidgetWidth = typedArray.getFloat(R.styleable.PenpointDIASelectorView_widthPercentWidget, 0.5f);



			typedArray.recycle();
		}
	}

	private void initPaint (Context context) {

		contentPaint = new Paint();
		contentPaint.setAntiAlias(true);
		contentPaint.setColor(DEFAULT_ITEM_FILL_COLOR);
		contentPaint.setStyle(Paint.Style.FILL_AND_STROKE);

		borderPaint = new Paint();
		borderPaint.setAntiAlias(true);
		borderPaint.setColor(DEFAULT_ITEM_BORDER_COLOR);
		borderPaint.setStyle(Paint.Style.STROKE);

		animator = new ValueAnimator();

	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		measureWidth = getMeasuredWidth();
		measureHeight = getMeasuredHeight();
		int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
		Log.e(TAG, "onMeasure: ---width=" + measureWidth + "--height=" + measureHeight);

		Log.e(TAG, "onMeasure mode : ---widthMode=" + widthMeasureMode + "--heightMode=" + heightMeasureMode);
	}

	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
//		Log.e(TAG, "onSizeChanged: ");
		itemWidthHeight = (int) (measureWidth * percentWidgetWidth);
	}

	@Override
	public void layout (int l, int t, int r, int b) {
		super.layout(l, t, r, b);
//		Log.e(TAG, "layout: ");
	}

	//顺序 多次onMeasure -> onSizeChanged -> layout -> onDraw
	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
//		Log.e(TAG, "onDraw: ");
//		canvas.drawRoundRect(0, 0, measureWidth, measureHeight, measureWidth / 2.0f, measureWidth / 2.0f, borderPaint);

		//顶部margin 和 底部margin
		//		itemWidthHeight = measureWidth / 5 * 3;


		perMarginTB = ((measureHeight - marginTopBottom * 2) / itemCount - itemWidthHeight) / 2;
		perMarginLR = (measureWidth - itemWidthHeight) / 2;

		borderPaint.setColor(Color.RED);
		borderPaint.setStrokeWidth(borderFontSize);
		//绘制 整体 top bottom 分割线
		//		canvas.drawLine(0, perMarginTB, measureWidth, perMarginTB, borderPaint);
		//		canvas.drawLine(0, measureHeight - perMarginTB, measureWidth, measureHeight - perMarginTB, borderPaint);

		//item 的圆圈
		borderPaint.setColor(DEFAULT_ITEM_BORDER_COLOR);
		borderPaint.setStrokeWidth(borderFontSize / 2.0f);

		//此处是为了适配宽度
		//		float widthCompare = measureWidth - 4 * perMargin;//要求内容区域大于左右margin之和
		//		itemWidthHeight = (int) ((widthCompare > 0) ? (measureWidth - 2 * perMargin) : (measureWidth / 2.0f));

		firstItemX = (int) (measureWidth / 2);
		firstItemY = (int) (marginTopBottom + perMarginTB + itemWidthHeight / 2);
		itemRadius = itemWidthHeight / 2 + 4;
		int breakLineSize = DEFAULT_PAINT_FONT_SIZE / 8;//半径

		for (int index = 0; index < itemCount; index++) {
			//画圈
			if (selectIndex == index) {//已选中的
				//				canvas.save();
				//				canvas.scale(1.1f, 1.1f);
				//外圈
				borderPaint.setColor(getContext().getColor(R.color.select_mti448));
				canvas.drawCircle(firstItemX, firstItemY, itemRadius, borderPaint);
				//画圈
				contentPaint.setStrokeWidth(breakLineSize);
				canvas.drawCircle(firstItemX, firstItemY, breakLineSize, contentPaint);
				//				canvas.restore();
				//
				breakLineSize += 3;
				firstItemY += (perMarginTB + itemWidthHeight + perMarginTB);
			} else {
				borderPaint.setColor(DEFAULT_ITEM_BORDER_COLOR);
				canvas.drawCircle(firstItemX, firstItemY, itemRadius, borderPaint);
				//画圈
				contentPaint.setStrokeWidth(breakLineSize);
				canvas.drawCircle(firstItemX, firstItemY, breakLineSize, contentPaint);

				breakLineSize += 3;
				firstItemY += (perMarginTB + itemWidthHeight + perMarginTB);
			}
		}
	}

	public int getPressIndex () {
		return this.selectIndex;
	}
	public void setSelectIndex (int selectIndex) {
		this.selectIndex = selectIndex;
		invalidate();
	}

	private int pressIndex = -1;//按下操作的下标

	private int judgeIsItemClick (float x, float y) {
		int firstX = measureWidth / 2;
		int firstY = (int) (marginTopBottom + perMarginTB + itemWidthHeight / 2);

		//if ((x >= (firstX - itemWidthHeight / 2.0f) && x <= (firstX + itemWidthHeight / 2.0f)) &&
		//					(y >= (firstY - itemWidthHeight / 2.0f) && y <= (firstY + itemWidthHeight / 2.0f)))
		for (int i = 0; i < itemCount; i++) {
			if ((x >= 0 && x <= measureWidth) && (y >= (firstY - itemWidthHeight / 2.0f - perMarginTB) && y <= (firstY + itemWidthHeight / 2.0f + perMarginTB))) {
				pressIndex = i;
				return i;
			} else {
				firstY += (perMarginTB * 2 + itemWidthHeight);
			}
		}
		return -1;
	}

	public int getPaintFontSize () {
		return paintFontSize;
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				int down = judgeIsItemClick(event.getX(), event.getY());
				if (down >= 0) {
					selectIndex = down;
					paintFontSize = (DEFAULT_PAINT_FONT_SIZE / 2) + 4 * down;
					if (selectorListener != null) {
						selectorListener.onItemSelect(down, paintFontSize);
					}
					invalidate();
				}
				Log.e(TAG, "onTouchEvent: down==>" + down);
				break;
			case MotionEvent.ACTION_UP:
				int up = judgeIsItemClick(event.getX(), event.getY());
				Log.e(TAG, "onTouchEvent: up==>" + up);
				break;
			case MotionEvent.ACTION_MOVE:
				break;
		}
		return true;
	}


	/*画笔粗细选择器，回调接口*/
	public interface OnPaintSizeSelectorListener {
		/**
		 * @param position        选中item下标
		 * @param selectPaintSize 选中item的PaintSize
		 */
		void onItemSelect (int position, int selectPaintSize);
	}

}
