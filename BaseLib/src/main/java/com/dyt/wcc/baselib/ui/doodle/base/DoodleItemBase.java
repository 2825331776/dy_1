package com.dyt.wcc.baselib.ui.doodle.base;

import android.graphics.Canvas;
import android.graphics.PointF;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleColor;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleItem;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleItemListener;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodlePen;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleShape;
import com.dyt.wcc.baselib.ui.doodle.impl.DoodlePaintAttrs;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Copyright (C),2022/11/29 16:18-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/29 16:18     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.base     </p>
 * <p>Description：涂鸦的基类，实现了{@link IDoodleItem }（具体实现交给子类）实现了 涂鸦item 属性变更
 * 的回调接口{@link IDoodleItemListener}。       </p>
 */
public abstract class DoodleItemBase implements IDoodleItem, IDoodleItemListener {
	//设置一个 涂鸦的 最小最大 缩放倍数。
	public static final float MIN_SCALE = 0.01f;
	public static final float MAX_SCALE = 20f;
	// 涂鸦旋转的角度
	private             float mItemRotate;

	//父容器 的引用：
	private IDoodle      mDoodle;
	//涂鸦 左边界 左上角点
	private PointF       mLeftTopPoint = new PointF();
	private IDoodlePen   mPenType;
	private IDoodleShape mShape;
	private float        mPenSize;
	private IDoodleColor mPenColor;

	private boolean mIsNeedClipOutSide = false;
	private boolean mIsDrawOptimize    = true;
	private float   mPivotX, mPivotY;

	private float mMinScale = MIN_SCALE;
	private float mMaxScale = MAX_SCALE;
	// 涂鸦 item  具体缩放
	private float mScale    = 1f;

	private boolean mHasAdded = false;

	//单个item 需要这么多 listener ？
	private List<IDoodleItemListener> mItemListenerList = new ArrayList<>();

	public DoodleItemBase (IDoodle doodle) {
		this(doodle, null);
	}

	public DoodleItemBase (IDoodle doodle, DoodlePaintAttrs attrs) {
		setDoodle(doodle);

		if (attrs != null) {
			mPenType = attrs.getPenType();
			mShape = attrs.getPenShape();
			mPenColor = attrs.getPenColor();
			mPenSize = attrs.getSize();
		}
	}

	//---------------------IDoodleItem method----------------------------
	@Override
	public void setDoodle (IDoodle doodle) {
		//涂鸦全局设置
		if (doodle != null && mDoodle != null) {
			throw new RuntimeException("item's doodle object is not null");
		}
		mDoodle = doodle;
		if (doodle == null) {
		}
	}

	@Override
	public IDoodle getDoodle () {
		return mDoodle;
	}

	//设置 涂鸦  左上角点坐标
	@Override
	public void setPivotX (float pivotX) {
		mPivotX = pivotX;
		onPropertyChanged(PROPERTY_PIVOT_X);
	}

	@Override
	public float getPivotX () {
		return mPivotX;
	}

	@Override
	public void setPivotY (float pivotY) {
		mPivotY = pivotY;
		onPropertyChanged(PROPERTY_PIVOT_Y);
	}

	@Override
	public float getPivotY () {
		return mPivotY;
	}

	//设置 涂鸦缩放
	@Override
	public void setItemRotate (float rotate) {
		mItemRotate = rotate;
		onPropertyChanged(PROPERTY_ROTATE);
		refresh();
	}


	@Override
	public float getScale () {
		return mScale;
	}

	@Override
	public float getItemRotate () {
		return mItemRotate;
	}

	@Override
	public void setLocation (float x, float y) {
		setLocation(x, y, true);
	}

	/**
	 * @param x
	 * @param y
	 * @param changePivot 是否 随着移动 相应改变中心点的位置
	 */
	public void setLocation (float x, float y, boolean changePivot) {
		float diffX = x - mLeftTopPoint.x, diffY = y - mLeftTopPoint.y;

		mLeftTopPoint.x = x;
		mLeftTopPoint.y = y;
		if (changePivot) {
			mPivotX = mPivotX + diffX;
			mPivotY = mPivotY + diffY;
			onPropertyChanged(PROPERTY_PIVOT_X);
			onPropertyChanged(PROPERTY_PIVOT_Y);
		}

		refresh();
	}

	@Override
	public PointF getLocation () {
		return mLeftTopPoint;
	}
	//pen stroke size


	@Override
	public float getSize () {
		return mPenSize;
	}

	@Override
	public void setSize (float penSize) {
		mPenSize = penSize;
		onPropertyChanged(PROPERTY_SIZE);
		refresh();
	}

	/**
	 * BRUSH, // 画笔
	 * COPY, // 仿制
	 * ERASER, // 橡皮擦
	 * TEXT, // 文本
	 * BITMAP, // 贴图
	 * MOSAIC; // 马赛克
	 *
	 * @param penType
	 */
	@Override
	public void setPen (IDoodlePen penType) {
		mPenType = penType;
		refresh();
	}

	@Override
	public IDoodlePen getPen () {
		return mPenType;
	}

	/**
	 * 设置 画笔的颜色 ，具体颜色 ，还是 bitmap 颜色
	 *
	 * @param penColor
	 */
	@Override
	public void setColor (IDoodleColor penColor) {
		mPenColor = penColor;
		onPropertyChanged(PROPERTY_COLOR);
		refresh();
	}

	@Override
	public IDoodleColor getColor () {
		return mPenColor;
	}

	/**
	 * 设置画笔的 形状 ，
	 * HAND_WRITE, // 手绘
	 * ARROW, // 箭头
	 * LINE, // 直线
	 * OVAL, // 椭圆
	 * HOLLOW_RECT; // 空心矩形
	 *
	 * @param doodleShape
	 */
	@Override
	public void setShape (IDoodleShape doodleShape) {
		mShape = doodleShape;
		refresh();
	}

	@Override
	public IDoodleShape getShape () {
		return mShape;
	}

	/**
	 * 涂鸦 item 缩放倍数
	 *
	 * @param scale
	 */
	@Override
	public void setScale (float scale) {
		if (scale < mMinScale) {
			scale = mMinScale;
		} else if (scale > mMaxScale) {
			scale = mMaxScale;
		}
		mScale = scale;
		onPropertyChanged(PROPERTY_SCALE);
		refresh();
	}

	public float getMinScale () {
		return mMinScale;
	}

	public void setMinScale (float minScale) {
		if (mMinScale <= 0) {
			minScale = MIN_SCALE;
		} else if (minScale >= mMaxScale) {
			minScale = mMaxScale;
		}
		mMinScale = minScale;
		setScale(getScale());
	}

	/**
	 * 设置 最大缩放
	 *
	 * @return
	 */
	public float getMaxScale () {
		return mMaxScale;
	}

	public void setMaxScale (float maxScale) {
		if (mMinScale <= 0) {
			maxScale = MIN_SCALE;
		} else if (maxScale >= mMaxScale) {
			maxScale = mMaxScale;
		}
		mMaxScale = maxScale;
		setScale(getScale());
	}

	@Override
	public void addItemListener (IDoodleItemListener listener) {
		if (listener == null || mItemListenerList.contains(listener)) {
			return;
		}
		mItemListenerList.add(listener);

	}

	@Override
	public void removeItemListener (IDoodleItemListener listener) {
		mItemListenerList.remove(listener);
	}

	@Override
	public void onPropertyChanged (int property) {
		for (int i = 0; i < mItemListenerList.size(); i++) {
			mItemListenerList.get(i).onPropertyChanged(property);
		}
	}


	@Override
	public boolean isNeedClipOutside () {
		return mIsNeedClipOutSide;
	}

	@Override
	public void setNeedClipOutside (boolean clip) {
		mIsNeedClipOutSide = clip;
	}

	@Override
	public void onAdd () {
		mHasAdded = true;
	}

	@Override
	public void onRemove () {
		mHasAdded = false;
	}

	@Override
	public void refresh () {
		if (mHasAdded && mDoodle != null) {
			mDoodle.refresh();
		}
	}

	@Override
	public boolean isDoodleEditable () {
		return false;
	}

	@Override
	public void draw (Canvas canvas) {
		drawBefore(canvas);

		int count = canvas.save();
		mLeftTopPoint = getLocation(); // 获取旋转后的起始坐标
		canvas.translate(mLeftTopPoint.x, mLeftTopPoint.y); // 偏移，把坐标系平移到item矩形范围
		float px = mPivotX - mLeftTopPoint.x, py = mPivotY - mLeftTopPoint.y; // 需要减去偏移
		canvas.rotate(mItemRotate, px, py); // 旋转坐标系
		canvas.scale(mScale, mScale, px, py); // 缩放
		doDraw(canvas);
		canvas.restoreToCount(count);

		drawAfter(canvas);
	}

	@Override
	public void drawAtTheTop (Canvas canvas) {

	}

	/**
	 * 涂鸦之前调用（相当于背景图，但保存图片时  不包含该部分）。仅画在View上。
	 *
	 * @param canvas
	 */
	protected void drawBefore (Canvas canvas) {
	}

	/**
	 * 绘制item ，不限制Canvas？
	 *
	 * @param canvas
	 */
	protected abstract void doDraw (Canvas canvas);

	/**
	 * 仅画在View上，在绘制涂鸦之后调用
	 *
	 * @param canvas
	 */
	protected void drawAfter (Canvas canvas) {
	}
}
