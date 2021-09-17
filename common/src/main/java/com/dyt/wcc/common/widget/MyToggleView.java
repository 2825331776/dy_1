package com.dyt.wcc.common.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
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
	private static final String TAG = "MyToggleView";
	//默认值
	private final int TEXT_COLOR = Color.BLACK;
	private final int TEXT_SIZE = 14;
	private final boolean IS_SHOW_TEXT = true;
	private final int TOGGLE_STOKE_PAINT = Color.GREEN;//开关背景

	private final int TOGGLE_FILL_PAINT_SELECT = Color.WHITE;
	private final int TOGGLE_FILL_PAINT_UNSELECT = Color.RED;

	private final int TOGGLE_CIR_FILL_PAINT_SELECT = Color.TRANSPARENT;
	private final int TOGGLE_CIR_FILL_PAINT_UNSELECT = Color.BLUE;

	private final int TEXT_SELECT = Color.BLUE;

	private final int OPEN_ANIMATE = 0;//滑动的开关动画
	private final int CLOSE_ANIMATE = 0;
	private final int TOGGLE_CIR_BG_SELECT = Color.RED;

	//是否显示文字
	private boolean isShowText   = true;
	private float textSize = 16 ;
	private int textColor ;
	private String textContent   = "开关";

	//画笔
	private Paint toggleStrokePaint;//描边
	private Paint toggleFillPaint;//填充
	private Paint toggleCirPaint;//画圆
	private TextPaint textPaint;//文字
	private Paint.FontMetrics fontMetrics;
	//画笔配置参数
	private int toggleStrokeWith = 2;//描边宽度
	
	//按钮圆图  及其  背景颜色。
	private int toggleButtonPic;
	private int toggleBgPicUnselect;
	private int toggleBgPicSelect;
	//控件的宽高
	private int mViewWidth;
	private int mViewHeight;
	//进出动画
	private Animation enterAnimate;
	private Animation exitAnimate;
	public MyToggleView (Context context) {
		this(context,null);
	}
	public MyToggleView (Context context, @Nullable AttributeSet attrs) {
		this(context, attrs,0);
	}
	public MyToggleView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initAttrs(context,attrs);
		initPaint();
		Log.e(TAG, "MyToggleView: ");
	}   

	private void initAttrs(Context context, AttributeSet attrs){
		//初始化描边的画笔
		toggleStrokePaint = new Paint();
		toggleStrokePaint.setColor(TOGGLE_STOKE_PAINT);
		toggleStrokePaint.setStrokeWidth(toggleStrokeWith);
		toggleStrokePaint.setAntiAlias(true);
		toggleStrokePaint.setStyle(Paint.Style.STROKE);
		//初始化画圆 画笔
		toggleCirPaint = new Paint();
		toggleCirPaint.setColor(TOGGLE_CIR_FILL_PAINT_UNSELECT);
		toggleCirPaint.setStrokeWidth(1);
		toggleCirPaint.setAntiAlias(true);
		toggleCirPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		//初始化文字的画笔
		textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setTextSize(50f);
		textPaint.setFakeBoldText(true);
		textPaint.setColor(Color.BLACK);
	}
	private void initPaint(){
		
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		mViewWidth = MeasureSpec.getSize(widthMeasureSpec) ;
		mViewHeight = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(mViewWidth,mViewHeight);
	}

	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);


	}


	/**
	 * 总体高度除以4 = 滑动的圆的半径
	 * 定义圆角矩形的半径为 滑动圆的半径减去 - 10
	 * @param canvas
	 */
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawRoundRect(new RectF(0  ,0 ,mViewWidth ,
				mViewHeight*0.5f ),mViewHeight*0.25f,mViewHeight*0.25f,toggleStrokePaint);

		canvas.drawCircle(mViewHeight * 0.25f ,mViewHeight * 0.25f ,mViewHeight*0.25f-2*toggleStrokeWith,toggleCirPaint);
		float textWidth = textPaint.measureText(textContent);


		float y = mViewHeight * 0.75f-((textPaint.ascent()+textPaint.descent())*0.5f);
		canvas.drawText(textContent,mViewWidth* 0.5f - textWidth*0.5f ,y,textPaint);
//		canvas.drawColor(TOGGLE_SELECT_BG);

		canvas.drawLine(0,mViewHeight,mViewWidth,mViewHeight,textPaint);

	}
}
