package com.dyt.wcc.baselib.ui.doodle.base;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleSelectableItem;
import com.dyt.wcc.baselib.ui.doodle.impl.DoodlePaintAttrs;
import com.dyt.wcc.baselib.ui.doodle.util.DrawUtil;


/**
 * <p>Copyright (C),2022/11/30 10:39-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/30 10:39     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.base     </p>
 * <p>Description： 可设置选中装填的 item 。     </p>
 */
public abstract class DoodleSelectableItemBase extends DoodleItemBase implements IDoodleSelectableItem {

	//item 能够旋转的 边界？
	public final static int     ITEM_CAN_ROTATION_BOUND = 35;
	public final static int     ITEM_PADDING            = 3;
	//涂鸦边界
	private             Rect    mRect                   = new Rect();
	private             Rect    mRectTemp               = new Rect();
	//绘制 item 选中的边界框
	private             Paint   mPaint                  = new Paint();
	//选中的涂鸦的 中心点
	private             PointF  mTemp                   = new PointF();
	private             boolean mIsSelected             = false;


	public DoodleSelectableItemBase (IDoodle doodle, int itemRotation, float x, float y) {
		this(doodle, null, itemRotation, x, y);
	}

	public DoodleSelectableItemBase (IDoodle doodle, DoodlePaintAttrs attrs, int itemRotation, float x, float y) {
		super(doodle, attrs);
		setLocation(x, y);

		setItemRotate(itemRotation);

		resetBoundScaled(mRect);
	}


	protected void resetBoundScaled (Rect rect) {
		resetBounds(rect);
		float px = getPivotX() - getLocation().x;
		float py = getPivotY() - getLocation().y;

		DrawUtil.scaleRect(rect, getScale(), px, py);
	}


	@Override
	public void setScale (float scale) {
		super.setScale(scale);
		resetBoundScaled(mRect);
		refresh();
	}

	@Override
	public Rect getBounds () {
		return mRect;
	}

	@Override
	public void setSize (float penSize) {
		//必须调用，否则刷新失败
		super.setSize(penSize);
		resetBounds(getBounds());
		setLocation(getPivotX() - getBounds().width() / 2.0f, getPivotY() - getBounds().height() / 2.0f, false);

		resetBoundScaled(getBounds());
	}

	@Override
	public boolean contains (float x, float y) {
		resetBoundScaled(mRect);
		PointF location = getLocation();
		//把触摸点转换成在文字坐标系（ 即以文字起始点作为坐标原点）内的点
		x = x - location.x;
		y = y - location.y;

		//把变换后相对于矩形的触摸点，还原回未变换前的点，然后判断是否 在矩形中
		mTemp = DrawUtil.rotatePoint(mTemp, (int) -getItemRotate(), x, y, getPivotX() - getLocation().x, getPivotY() - getLocation().y);
		mRectTemp.set(mRect);
		float unit = getDoodle().getUnitSize();
		mRectTemp.left -= ITEM_PADDING * unit;
		mRectTemp.top -= ITEM_PADDING * unit;
		mRectTemp.right -= ITEM_PADDING * unit;
		mRectTemp.bottom -= ITEM_PADDING * unit;

		return mRectTemp.contains((int) mTemp.x, (int) mTemp.y);
	}

	@Override
	protected void drawBefore (Canvas canvas) {
	}

	@Override
	protected void drawAfter (Canvas canvas) {
	}

	@Override
	public void drawAtTheTop (Canvas canvas) {
		int saveCount = canvas.save();

		PointF location = getLocation();
		canvas.translate(location.x, location.y);
		canvas.rotate(getItemRotate(), getPivotX() - getLocation().x,
				getPivotY() - getLocation().y);

		doDrawAtTheTop(canvas);

		canvas.restoreToCount(saveCount);

	}

	/**
	 * 子类
	 * @param canvas
	 */
	public void doDrawAtTheTop (Canvas canvas) {
		if (isSelected()) {//选中时的效果，在最上面，避免被其他内容遮住
			//反向缩放画布，使视觉上选中边框不随图片 缩放而变化
			canvas.save();
			canvas.scale(1 / getDoodle().getDoodleScale(), 1 / getDoodle().getDoodleScale(), getPivotX() - getLocation().x, getPivotY() - getLocation().y);

			mRectTemp.set(getBounds());
			DrawUtil.scaleRect(mRectTemp, getDoodle().getDoodleScale(), getPivotX() - getLocation().x, getPivotY() - getLocation().y);

			float unit = getDoodle().getUnitSize();
			mRectTemp.left -= ITEM_PADDING * unit;
			mRectTemp.top -= ITEM_PADDING * unit;
			mRectTemp.right -= ITEM_PADDING * unit;
			mRectTemp.bottom -= ITEM_PADDING * unit;
			mPaint.setShader(null);
			mPaint.setColor(0x00888888);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setStrokeWidth(1);
			canvas.drawRect(mRectTemp, mPaint);

			//border
			mPaint.setColor(0x88ffffff);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(2 * unit);
			canvas.drawRect(mRectTemp, mPaint);

			//border line
			mPaint.setColor(0x44888888);
			mPaint.setStrokeWidth(0.8f * unit);
			canvas.drawRect(mRectTemp, mPaint);

			canvas.restore();
		}
	}

	protected void resetBoundsScaled(Rect rect) {
		resetBounds(rect);
		float px = getPivotX() - getLocation().x;
		float py = getPivotY() - getLocation().y;
		DrawUtil.scaleRect(rect, getScale(), px, py);
	}
	/**
	 * @param rect bounds for the item, start with (0,0)
	 */
	protected abstract void resetBounds(Rect rect);

	@Override
	public boolean isSelected () {
		return mIsSelected;
	}

	@Override
	public boolean isDoodleEditable () {
		return mIsEditAble;
	}
	public void setEditAble(boolean editAble){
		this.mIsEditAble = editAble;
	}
	private boolean mIsEditAble = false;

	@Override
	public void setSelect (boolean isSelect) {
		this.mIsSelected = isSelect;
		//当选中时，设置 界外 不需要剪裁
		setNeedClipOutside(!isSelect);
		refresh();
	}

}
