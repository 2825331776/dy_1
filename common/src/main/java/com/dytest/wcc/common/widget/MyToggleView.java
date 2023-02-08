package com.dytest.wcc.common.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.dytest.wcc.common.R;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/16  15:07     </p>
 * <p>Description：@开关底部加文字  切换效果自定义         </p>
 * 支持：设置描边颜色， 切换颜色，圆按钮颜色，字体颜色
 * <p>PackagePath: com.dyt.wcc.widget     </p>
 * <p>
 * 可以做动画滑动效果，但未做。
 * 字体自适应效果未做
 */
public class MyToggleView extends View {
	private static final String TAG                            = "MyToggleView";
	//外围描边颜色
	private final        int    TOGGLE_STOKE_PAINT             = getResources().getColor(R.color.bg_preview_left_cutoff_rule);//开关背景
	//圆形矩阵 颜色
	private final        int    TOGGLE_FILL_PAINT_SELECT       = getResources().getColor(R.color.bg_preview_toggle_select);
	private final        int    TOGGLE_FILL_PAINT_UNSELECT     = getResources().getColor(R.color.bg_preview_toggle_unselect);
	//滑动圆圈 颜色
	private final        int    TOGGLE_CIR_FILL_PAINT_SELECT   = Color.WHITE;
	private final        int    TOGGLE_CIR_FILL_PAINT_UNSELECT = Color.WHITE;
	private final int               OPEN_ANIMATE  = 0;//滑动的开关动画
	private final int               CLOSE_ANIMATE = 0;
	private int mColorCirSelect;
	private int mColorCirUnselect;
	private int mColorFillSelect;
	private int mColorFillUnselect;
	private int mColorStroke;
	private int mColorTextSelect;
	private int mColorTextUnselect = Color.BLUE;
	//是否显示文字
	private       boolean           mIsShowText   = true;
	private       float             mTextSize     = 16f;
	private       String            textContent   = "开关";
	private       boolean           isSelected    = false;//是否选中
	//画笔
	private       Paint             toggleStrokePaint;//描边
	private       Paint             toggleFillPaint;//填充
	private       Paint             toggleCirPaint;//画圆
	private       TextPaint         textPaint;//文字
	private       Paint.FontMetrics fontMetrics;
	//画笔配置参数
	private       int               mStrokeWith   = 1;//描边宽度
	//控件的宽高
	private       int               mViewWidth;
	private       int               mViewHeight;

	private boolean mEnable = true;//是否可以点击
	private OnClickChangedState clickChangedState;

	public MyToggleView (Context context) {
		this(context, null);
	}

	public MyToggleView (Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MyToggleView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initAttrs(context, attrs);
		initPaint();
		//		Log.e(TAG, "init: ");
	}

	//通过attrs 拿到资源文件之中的配置
	private void initAttrs (Context context, AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MyToggleView);
		mColorCirSelect = typedArray.getColor(R.styleable.MyToggleView_ColorCirSelect, TOGGLE_CIR_FILL_PAINT_SELECT);
		mColorCirUnselect = typedArray.getColor(R.styleable.MyToggleView_ColorCirUnselect, TOGGLE_CIR_FILL_PAINT_UNSELECT);
		mColorFillSelect = typedArray.getColor(R.styleable.MyToggleView_ColorFillSelect, TOGGLE_FILL_PAINT_SELECT);
		mColorFillUnselect = typedArray.getColor(R.styleable.MyToggleView_ColorFillUnselect, TOGGLE_FILL_PAINT_UNSELECT);
		mColorStroke = typedArray.getColor(R.styleable.MyToggleView_ColorStroke, TOGGLE_STOKE_PAINT);
		mColorTextSelect = typedArray.getColor(R.styleable.MyToggleView_ColorTextSelect, Color.BLUE);
		mColorTextUnselect = typedArray.getColor(R.styleable.MyToggleView_ColorTextUnselect, Color.BLUE);

		String content = typedArray.getString(R.styleable.MyToggleView_showTextContent);
		if (!TextUtils.isEmpty(content)) {
			textContent = content;
		}
		mIsShowText = typedArray.getBoolean(R.styleable.MyToggleView_isShowText, true);
		mTextSize = typedArray.getDimension(R.styleable.MyToggleView_showTextSize, 16);
		mStrokeWith = typedArray.getInt(R.styleable.MyToggleView_mStrokeWith, 1);

		typedArray.recycle();
	}

	private void initPaint () {
		//初始化描边的画笔
		toggleStrokePaint = new Paint();
		toggleStrokePaint.setColor(mColorStroke);
		toggleStrokePaint.setStrokeWidth(mStrokeWith);
		toggleStrokePaint.setAntiAlias(true);
		toggleStrokePaint.setStyle(Paint.Style.STROKE);
		//圆角矩阵背景颜色
		toggleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		toggleFillPaint.setColor(mColorCirUnselect);
		toggleFillPaint.setAntiAlias(true);
		toggleFillPaint.setStyle(Paint.Style.FILL);

		//初始化画圆 画笔
		toggleCirPaint = new Paint();
		toggleCirPaint.setColor(mColorCirUnselect);
		toggleCirPaint.setStrokeWidth(1);
		toggleCirPaint.setAntiAlias(true);
		toggleCirPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		//初始化文字的画笔
		textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);


		textPaint.setTextSize(mTextSize);
		//		textPaint.setFakeBoldText(true);
		textPaint.setColor(mColorTextUnselect);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		//		Log.e(TAG, "onMeasure: ");
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
		mViewHeight = MeasureSpec.getSize(heightMeasureSpec);

		setMeasuredDimension(mViewWidth, mViewHeight);
	}

	@Override
	protected void onRestoreInstanceState (Parcelable state) {
		//		Log.e(TAG, "onRestoreInstanceState: ");
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;
			mColorCirSelect = bundle.getInt("mColorCirSelect");
			mColorCirUnselect = bundle.getInt("mColorCirUnselect");
			mColorFillSelect = bundle.getInt("mColorFillSelect");
			mColorFillUnselect = bundle.getInt("mColorFillUnselect");
			mColorStroke = bundle.getInt("mColorStroke");
			mColorTextSelect = bundle.getInt("mColorTextSelect");
			mColorTextUnselect = bundle.getInt("mColorTextUnselect");

			textContent = bundle.getString("textContent");
			mIsShowText = bundle.getBoolean("mIsShowText");
			mTextSize = bundle.getFloat("mTextSize");
			mStrokeWith = bundle.getInt("mStrokeWith");

			isSelected = bundle.getBoolean("isSelected");

			super.onRestoreInstanceState(bundle.getParcelable("myToggleView"));
		} else {
			super.onRestoreInstanceState(state);
		}
	}

	@Nullable
	@Override
	protected Parcelable onSaveInstanceState () {
		//		Log.e(TAG, "onSaveInstanceState: ");
		Bundle bundle = new Bundle();
		bundle.putParcelable("myToggleView", super.onSaveInstanceState());
		bundle.putInt("mColorCirSelect", mColorCirSelect);
		bundle.putInt("mColorCirUnselect", mColorCirUnselect);
		bundle.putInt("mColorFillSelect", mColorFillSelect);
		bundle.putInt("mColorFillUnselect", mColorFillUnselect);
		bundle.putInt("mColorStroke", mColorStroke);
		bundle.putInt("mColorTextSelect", mColorTextSelect);
		bundle.putInt("mColorTextUnselect", mColorTextUnselect);

		bundle.putString("textContent", textContent);
		bundle.putBoolean("mIsShowText", mIsShowText);
		bundle.putFloat("mTextSize", mTextSize);
		bundle.putInt("mStrokeWith", mStrokeWith);

		bundle.putBoolean("isSelected", isSelected);
		return bundle;
	}

	@Override
	public boolean isSelected () {
		return isSelected;
	}

	@Override
	public void setSelected (boolean selected) {
		isSelected = selected;
		invalidate();
	}

	public void setOnClickChangedState (OnClickChangedState clickChangedState) {
		this.clickChangedState = clickChangedState;
	}

	//设置是否可以点击 切换
	public boolean isMEnable () {
		return mEnable;
	}

	public void setMEnable (boolean mEnable) {
		this.mEnable = mEnable;
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (!mEnable) {
			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			isSelected = !isSelected;
			invalidate();
			if (clickChangedState != null) {
				clickChangedState.onClick(isSelected);
			}
		}
		return true;
	}

	/**
	 * 总体高度除以4 = 滑动的圆的半径
	 * 定义圆角矩形的半径为 滑动圆的半径减去 - 10
	 *
	 * @param canvas
	 */
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		//总体描边
		canvas.drawRoundRect(new RectF(0, 0, mViewWidth, mViewHeight * 0.5f), mViewHeight * 0.25f + 1, mViewHeight * 0.25f + 1, toggleStrokePaint);

		if (isSelected) {
			toggleFillPaint.setColor(mColorFillSelect);
			toggleCirPaint.setColor(mColorCirSelect);
			//绘制圆角背景
			canvas.drawRoundRect(new RectF(mStrokeWith, mStrokeWith, mViewWidth - mStrokeWith, mViewHeight * 0.5f - mStrokeWith), mViewHeight * 0.25f - mStrokeWith, mViewHeight * 0.25f - mStrokeWith, toggleFillPaint);
			//			//绘制圆形滑动按钮
			canvas.drawCircle(mViewWidth - mViewHeight * 0.25f, mViewHeight * 0.25f, mViewHeight * 0.25f - 2 * mStrokeWith, toggleCirPaint);
		} else {
			toggleFillPaint.setColor(mColorFillUnselect);
			toggleCirPaint.setColor(mColorCirUnselect);
			//绘制圆角背景
			canvas.drawRoundRect(new RectF(mStrokeWith, mStrokeWith, mViewWidth - mStrokeWith, mViewHeight * 0.5f - mStrokeWith), mViewHeight * 0.25f - mStrokeWith, mViewHeight * 0.25f - mStrokeWith, toggleFillPaint);
			//			//绘制圆形滑动按钮
			canvas.drawCircle(mViewHeight * 0.25f, mViewHeight * 0.25f, mViewHeight * 0.25f - 2 * mStrokeWith, toggleCirPaint);
		}
		float textWidth = textPaint.measureText(textContent);//文字的宽度
		textWidth = Math.min(textWidth, mViewWidth);
		float y = mViewHeight * 0.75f - ((textPaint.ascent() + textPaint.descent()) * 0.5f);//文字高度
		//绘制文字
		canvas.drawText(textContent, mViewWidth * 0.5f - textWidth * 0.5f, y, textPaint);
	}

	/**
	 * 监听状态改变回调接口
	 */
	public interface OnClickChangedState {
		void onClick (boolean checkState);
	}
}
