package com.dyt.wcc.baselib.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.dyt.wcc.baselib.R;


/**
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：${DATE} ${TIME}     </p>
 * <p>PackagePath: com.dyt.wcc.baselib.ui.widget     </p>
 * <p>Description：一个 带圆形边框的 同心圆。
 * 中心实心圆，填充的是内容。
 * </p>
 */
public class CircleDisplayView extends androidx.appcompat.widget.AppCompatImageButton {
	public CircleDisplayView (Context context) {
		this(context, null);
	}

	public CircleDisplayView (Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CircleDisplayView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initAttrs(context, attrs);

		initPaint();
	}

	/**
	 * 默认 边框颜色
	 */
	private final int DEFAULT_BORDER_COLOR        = Color.WHITE;
	/**
	 * 默认 边框 选中颜色
	 */
	private final int DEFAULT_BORDER_COLOR_SELECT = Color.GREEN;

	private final int DEFAULT_BORDER_STROKE_WIDTH = 6;

	private final static String TAG = "CircleImageViewInfo";

	private RectF rectFContent;

	//-----------变量------------------------
	/**
	 * 边框 颜色
	 */
	@ColorInt
	private int borderColor       = DEFAULT_BORDER_COLOR;//边框颜色
	/**
	 * 边框 选中颜色
	 */
	private int borderColorSelect = DEFAULT_BORDER_COLOR_SELECT;

	private int mMeasureWidth, mMeasureHeight;
	/**
	 * 边框画笔
	 */
	private Paint paintBorder;
	/**
	 * 内容画笔
	 */
	private Paint paintContent;

	/**
	 * 中心内容
	 */
	private int mContentData = 0;
	/**
	 * 边框半径， 内容半径
	 */
	private int borderRadius, contentRadius;
	/**
	 * 中心填充物 类型。
	 */
	private CirCleImageType mContentType = CirCleImageType.COLOR;//内容填充
	/**
	 * 是否 已选中
	 */
	private boolean         selected     = false;

	/**
	 * 是否需要重绘 bitmap
	 */
	private boolean needReDrawBitmap = true;
	/**
	 * 中心内容 bitmap
	 */
	private Bitmap  bitmapContent    = null;

	/**
	 * 边框的宽度
	 */
	private int mBorderStrokeWidth;

	public enum CirCleImageType {
		COLOR, BITMAP,
	}


	private OnCircleImageClickListener clickListener;

	public void setClickListener (OnCircleImageClickListener clickListener) {
		this.clickListener = clickListener;
	}

	/**
	 * 点击监听器
	 */
	public interface OnCircleImageClickListener {
		void onClicked (int currentData, CirCleImageType cirCleImageType);
	}

	private void initAttrs (Context context, AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs,
				R.styleable.CircleDisplayView);
		int type = typedArray.getInt(R.styleable.CircleDisplayView_contentType, 0);

		mContentType = (type == 0) ? CirCleImageType.COLOR : CirCleImageType.BITMAP;
		//获取边框颜色 及 选中颜色
		borderColorSelect = typedArray.getColor(R.styleable.CircleDisplayView_select_borderColor,
				DEFAULT_BORDER_COLOR_SELECT);
		borderColor = typedArray.getColor(R.styleable.CircleDisplayView_default_borderColor,
				DEFAULT_BORDER_COLOR);
		//获取画笔 边框的宽度
		mBorderStrokeWidth =
				typedArray.getInt(R.styleable.CircleDisplayView_select_border_StrokeWidth,
						DEFAULT_BORDER_STROKE_WIDTH);

		selected = typedArray.getBoolean(R.styleable.CircleDisplayView_isSelect, false);

		if (mContentType != CirCleImageType.COLOR) {
			needReDrawBitmap = true;
			mContentData = typedArray.getResourceId(R.styleable.CircleDisplayView_default_Bitmap,
					0);
		} else {
			mContentData = Color.BLACK;
		}

		typedArray.recycle();
	}

	private void initPaint () {
		paintBorder = new Paint();
		paintBorder.setAntiAlias(true);

		paintBorder.setStyle(Paint.Style.STROKE);
		paintBorder.setStrokeWidth(mBorderStrokeWidth);


		paintContent = new Paint();
		paintContent.setAntiAlias(true);

		paintContent.setStyle(Paint.Style.FILL_AND_STROKE);

		bitmapContent = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		rectFContent = new RectF();
	}


	//------------------业务逻辑-------------------------
	//绘制 边框， 绘制 内容， 点击 回调 监听接口。
	public void setColor (int data, CirCleImageType type) {
		mContentData = data;
		if (type == CirCleImageType.COLOR) {
			paintContent.setColor(data);
		} else {
			bitmapContent = BitmapFactory.decodeResource(getResources(), data);
			needReDrawBitmap = true;
		}
		invalidate();
	}


	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		//设置 边框颜色
		paintContent.setColor(Color.BLACK);
		if (selected) {
			paintBorder.setColor(borderColorSelect);
		} else {
			paintBorder.setColor(borderColor);
		}

		mMeasureWidth = getMeasuredWidth();
		mMeasureHeight = getMeasuredHeight();
		rectFContent.set(0, 0, mMeasureWidth, mMeasureHeight);

		borderRadius = mMeasureWidth / 2 - getPaddingLeft() - getPaddingRight();
		contentRadius = borderRadius - 10;

		if (mContentType == CirCleImageType.BITMAP) {
			createBitmap();
		}
	}

	//仅仅创建了 画布 ，里面什么都没画
	private void createBitmap () {
		if (bitmapContent != null) {
			if (!bitmapContent.isRecycled()) {
				bitmapContent.recycle();
				bitmapContent = null;
			}
		}
		bitmapContent = Bitmap.createBitmap(mMeasureWidth, mMeasureHeight,
				Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmapContent);

		if (mContentData != 0) {
			Bitmap bp = BitmapFactory.decodeResource(getResources(), mContentData);
			canvas.drawBitmap(bp, null, rectFContent, paintContent);
		}
	}


	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);

		//draw border
		if (selected) {
			paintBorder.setColor(borderColorSelect);
		} else {
			paintBorder.setColor(borderColor);
		}
		canvas.drawCircle(mMeasureWidth / 2.0f, mMeasureHeight / 2.0f, borderRadius, paintBorder);

		//draw content
		if (mContentType == CirCleImageType.COLOR) {
			paintContent.setColor(mContentData);
			paintContent.setStyle(Paint.Style.FILL);
			canvas.drawCircle(mMeasureWidth / 2.0f, mMeasureHeight / 2.0f, contentRadius,
					paintContent);
		} else {
			paintContent.setStyle(Paint.Style.STROKE);
			paintContent.setStrokeWidth(DEFAULT_BORDER_STROKE_WIDTH);
			//中心线条多宽？
			if (needReDrawBitmap) {
				canvas.drawBitmap(bitmapContent, null, rectFContent, paintContent);
			}
		}

	}

	public void setSelected (boolean state) {
		selected = state;
		invalidate();
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (!selected) {
				selected = true;
				if (clickListener != null) {
					clickListener.onClicked(mContentData, mContentType);
				}
				invalidate();
			}
		}
		return true;
	}


	private class SaveInstance extends BaseSavedState {
		private int    type;//类型
		private int    data;// color ： 颜色；Bitmap:对应图片资源 id。
		private Bitmap bitmap;
		private int    isSelected;


		public SaveInstance (Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel (Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(type);
			out.writeInt(this.data);
			out.writeParcelable(bitmap, flags);
			out.writeInt(isSelected);
		}
	}

	//----------------保存 与 恢复----------------------------------
	@Nullable
	@Override
	protected Parcelable onSaveInstanceState () {
		Parcelable parcelable = super.onSaveInstanceState();
		SaveInstance ss = new SaveInstance(parcelable);
		ss.data = mContentData;
		ss.type = (mContentType == CirCleImageType.COLOR ? 0 : 1);
		ss.bitmap = bitmapContent;
		ss.isSelected = selected ? 1 : 0;
		return ss;
	}

	@Override
	protected void onRestoreInstanceState (Parcelable state) {
		if (!(state instanceof SaveInstance)) {
			super.onRestoreInstanceState(state);
			return;
		}
		SaveInstance ss = (SaveInstance) state;
		super.onRestoreInstanceState(ss.getSuperState());

		mContentType = (ss.type == 0) ? CirCleImageType.COLOR : CirCleImageType.BITMAP;
		mContentData = ss.data;
		bitmapContent = ss.bitmap;
		selected = (ss.isSelected == 1);
	}

}
