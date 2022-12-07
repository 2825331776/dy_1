package com.dyt.wcc.baselib.ui.doodle.impl;

import android.animation.ValueAnimator;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleItem;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodlePen;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleSelectableItem;
import com.dyt.wcc.baselib.ui.doodle.params.DoodlePen;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleShape;
import com.dyt.wcc.baselib.ui.doodle.view.DoodleView;

import java.util.List;

import cn.forward.androids.ScaleGestureDetectorApi27;
import cn.forward.androids.TouchGestureDetector;

/**
 * <p>Copyright (C),2022/12/1 16:45-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/12/1 16:45     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.impl     </p>
 * <p>Description： 按照事件去  绘制的       </p>
 */
public class DoodleOnTouchGestureListener extends TouchGestureDetector.OnTouchGestureListener {
	private static final float  VALUE = 1f;
	private static final String TAG   = "DoodleOnTouchGesture";

	// 触摸的相关信息
	private float mTouchX, mTouchY;
	private float mLastTouchX, mLastTouchY;
	private float mTouchDownX, mTouchDownY;

	// 缩放相关
	private Float mLastFocusX;
	private Float mLastFocusY;
	private float mTouchCentreX, mTouchCentreY;


	private float mStartX, mStartY;
	private float mRotateDiff; // 开始旋转item时的差值（当前item的中心点与触摸点的角度）

	private Path mCurrPath; // 当前手写的路径

	private DoodleView mDoodle;

	// 动画相关
	private ValueAnimator mScaleAnimator;
	private float         mScaleAnimTransX, mScaleAnimTranY;
	private ValueAnimator mTranslateAnimator;
	private float         mTransAnimOldY, mTransAnimY;

	private IDoodleSelectableItem mSelectedItem; // 当前选中的item
	private ISelectionListener mSelectionListener;

	private boolean    mSupportScaleItem = true;
	private DoodlePath mCurrDoodlePath;

	public DoodleOnTouchGestureListener (DoodleView doodle) {
		mDoodle = doodle;
	}

	public interface ISelectionListener {
		/**
		 * called when the item(such as text, texture) is selected/unselected.
		 * item（如文字，贴图）被选中或取消选中时回调
		 *
		 * @param selected 是否选中，false表示从选中变成不选中
		 */
		void onSelectedItem (IDoodle doodle, IDoodleSelectableItem selectableItem,
		                     boolean selected);

		/**
		 * called when you click the view to create a item(such as text, texture).
		 * 点击View中的某个点创建可选择的item（如文字，贴图）时回调
		 *
		 * @param x
		 * @param y
		 */
		void onCreateSelectableItem (IDoodle doodle, float x, float y);
	}
	// --------------------------设置 获取 选中的item --------------------------------------------
	public void setSelectedItem(IDoodleSelectableItem selectedItem) {
		IDoodleSelectableItem old = mSelectedItem;
		mSelectedItem = selectedItem;

		if (old != null) { // 取消选定
			old.setSelect(false);
			if (mSelectionListener != null) {
				mSelectionListener.onSelectedItem(mDoodle, old, false);
			}
			mDoodle.notifyItemFinishedDrawing(old);
		}
		if (mSelectedItem != null) {
			mSelectedItem.setSelect(true);
			if (mSelectionListener != null) {
				mSelectionListener.onSelectedItem(mDoodle, mSelectedItem, true);
			}
			mDoodle.markItemToOptimizeDrawing(mSelectedItem);
		}
	}
	public IDoodleSelectableItem getSelectedItem() {
		return mSelectedItem;
	}

	//----------------设置 获取 选中item的 监听器--------------------------------
	public ISelectionListener getSelectionListener () {
		return mSelectionListener;
	}

	public void setSelectionListener (ISelectionListener mSelectionListener) {
		this.mSelectionListener = mSelectionListener;
	}


	// 判断当前画笔是否可编辑
	private boolean isPenEditable (IDoodlePen pen) {
		return (mDoodle.getPen() == DoodlePen.TEXT && pen == DoodlePen.TEXT);
	}


	public DoodleOnTouchGestureListener () {
		super();
	}


	@Override
	public boolean onDown (MotionEvent e) {
		mTouchX = mTouchDownX = e.getX();
		mTouchY = mTouchDownY = e.getY();
		return true;
	}

	@Override
	public void onScrollBegin (MotionEvent e) {
		Log.e(TAG, "---------------onScrollBegin:----------- ");
		//		super.onScrollBegin(e);

		mLastTouchX = mTouchX = e.getX();
		mLastTouchY = mTouchY = e.getY();
		if (mSelectedItem != null) {
			PointF xy = mSelectedItem.getLocation();
			mStartX = xy.x;
			mStartY = xy.y;
			/*if (mSelectedItem instanceof DoodleRotatableItemBase
					&& (((DoodleRotatableItemBase) mSelectedItem).canRotate(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY)))) {
				((DoodleRotatableItemBase) mSelectedItem).setIsRotating(true);
				mRotateDiff = mSelectedItem.getItemRotate() -
						DrawUtil.computeAngle(mSelectedItem.getPivotX(), mSelectedItem.getPivotY(), mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
			}*/
		}
		/*else {
			if (mDoodle.isEditMode()) {
				mStartX = mDoodle.getDoodleTranslationX();
				mStartY = mDoodle.getDoodleTranslationY();
			}
		}*/

		//手绘 涂鸦
		if (mDoodle.getPen() == DoodlePen.BRUSH) {
			mCurrPath = new Path();
			mCurrPath.moveTo(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
			Log.e(TAG, "onScrollBegin: mDoodle.shape===>" + mDoodle.getShape());

			if (mDoodle.getShape() == DoodleShape.HAND_WRITE) { // 手写
				mCurrDoodlePath = DoodlePath.toPath(mDoodle, mCurrPath);
			}

			if (mDoodle.isOptimizeDrawing()) {
				mDoodle.markItemToOptimizeDrawing(mCurrDoodlePath);
			} else {
				mDoodle.addItem(mCurrDoodlePath);
			}
		}
	}

	@Override
	public boolean onScroll (MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//		Log.e(TAG, "---------------onScroll:----------- ");
		mLastTouchX = mTouchX;
		mLastTouchY = mTouchY;
		mTouchX = e2.getX();
		mTouchY = e2.getY();
		//		mDoodle.setScrollingDoodle(false);

		if (mDoodle.isEditMode() || isPenEditable(mDoodle.getPen())) { //画笔是否是可选择的
			if (mSelectedItem != null) {
				/*if ((mSelectedItem instanceof DoodleRotatableItemBase) && (((DoodleRotatableItemBase) mSelectedItem).isRotating())) { // 旋转item
					mSelectedItem.setItemRotate(mRotateDiff + DrawUtil.computeAngle(
							mSelectedItem.getPivotX(), mSelectedItem.getPivotY(), mDoodle.toX(mTouchX), mDoodle.toY(mTouchY)
					));
				} else*/
				{ // 移动item
					mSelectedItem.setLocation(
							mStartX + mDoodle.toX(mTouchX) - mDoodle.toX(mTouchDownX),
							mStartY + mDoodle.toY(mTouchY) - mDoodle.toY(mTouchDownY));
				}
			} else {
				if (mDoodle.isEditMode()) {
					mDoodle.setDoodleTranslation(mStartX + mTouchX - mTouchDownX,
							mStartY + mTouchY - mTouchDownY);
				}
			}
		}


		if (mDoodle.getShape() == DoodleShape.HAND_WRITE && mDoodle.getPen() == DoodlePen.BRUSH) { // 手写
			mCurrPath.quadTo(mDoodle.toX(mLastTouchX), mDoodle.toY(mLastTouchY),
					mDoodle.toX((mTouchX + mLastTouchX) / 2),
					mDoodle.toY((mTouchY + mLastTouchY) / 2));
			mCurrDoodlePath.updatePath(mCurrPath);
		}
		mDoodle.refresh();
		return true;
	}


	@Override
	public void onScrollEnd (MotionEvent e) {
//		Log.e(TAG, "---------------onScrollEnd:---------是否优化绘制-- " + mDoodle.isOptimizeDrawing());
		mLastTouchX = mTouchX;
		mLastTouchY = mTouchY;
		mTouchX = e.getX();
		mTouchY = e.getY();

		if (mDoodle.getShape() == DoodleShape.HAND_WRITE && mDoodle.getPen() == DoodlePen.BRUSH) { // 手写
			if (mCurrDoodlePath != null) {
				if (mDoodle.isOptimizeDrawing()) {
					mDoodle.notifyItemFinishedDrawing(mCurrDoodlePath);
				}
				mCurrDoodlePath = null;
			}
		}

		mDoodle.refresh();
	}

	@Override
	public void onUpOrCancel (MotionEvent e) {
		super.onUpOrCancel(e);
	}

	@Override
	public boolean onFling (MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return super.onFling(e1, e2, velocityX, velocityY);
	}

	@Override
	public void onLongPress (MotionEvent e) {
		super.onLongPress(e);
	}


	@Override
	public void onShowPress (MotionEvent e) {
		Log.e(TAG, "onShowPress: ");
		super.onShowPress(e);
	}

	@Override
	public boolean onSingleTapUp (MotionEvent e) {
		if (isPenEditable(mDoodle.getPen())) {
			boolean found = false;
			IDoodleSelectableItem item;
			List<IDoodleItem> items = mDoodle.getAllItem();
			for (int i = items.size() - 1; i >= 0; i--) {
				IDoodleItem elem = items.get(i);
				if (!elem.isDoodleEditable()) {
					continue;//isDoodleEditable 为 false 跳至下一次循环
				}
				Log.e(TAG, "onSingleTapUp: ---------------是可以编辑的------");
				if (!(elem instanceof IDoodleSelectableItem)) {
					continue;
				}
				Log.e(TAG, "onSingleTapUp: ----------是 IDoodleSelectableItem 的子类 ，或实现类---");
				item = (IDoodleSelectableItem) elem;

				if (item.contains(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY))) {
					found = true;
					setSelectedItem(item);
					PointF xy = item.getLocation();
					mStartX = xy.x;
					mStartY = xy.y;
					break;
				}
			}


			if (!found) { // not found
				if (mSelectedItem != null) { // 取消选定
					setSelectedItem(null);

					/*IDoodleSelectableItem old = mSelectedItem;
//					setSelectedItem(null);
					if (mSelectionListener != null) {
						mSelectionListener.onSelectedItem(mDoodle, old, false);
					}*/
				}else {// 不是在已选中的item 框内，而且没有选中的 item
					if (mSelectionListener != null) {
						mSelectionListener.onCreateSelectableItem(mDoodle, mDoodle.toX(mTouchX),
								mDoodle.toY(mTouchY));
					}
				}
			}
		}

		mDoodle.refresh();
		Log.e(TAG, "onSingleTapUp: ");
		//		return super.onSingleTapUp(e);
		return true;
	}

	@Override
	public boolean onDoubleTap (MotionEvent e) {
		Log.e(TAG, "onDoubleTap: ");
		return super.onDoubleTap(e);
	}

	@Override
	public boolean onDoubleTapEvent (MotionEvent e) {
		Log.e(TAG, "onDoubleTapEvent: ");
		return super.onDoubleTapEvent(e);
	}

	@Override
	public boolean onSingleTapConfirmed (MotionEvent e) {
		Log.e(TAG, "onSingleTapConfirmed: ");
		return super.onSingleTapConfirmed(e);
	}

	@Override
	public boolean onScale (ScaleGestureDetectorApi27 detector) {
		Log.e(TAG, "onScale: ");
		//		return super.onScale(detector);
		// 屏幕上的焦点
		mTouchCentreX = detector.getFocusX();
		mTouchCentreY = detector.getFocusY();

		if (mLastFocusX != null && mLastFocusY != null) { // 焦点改变
			final float dx = mTouchCentreX - mLastFocusX;
			final float dy = mTouchCentreY - mLastFocusY;
			// 移动图片
			if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
				if (mSelectedItem == null || !mSupportScaleItem) {
					mDoodle.setDoodleTranslationX(mDoodle.getDoodleTranslationX() + dx + pendingX);
					mDoodle.setDoodleTranslationY(mDoodle.getDoodleTranslationY() + dy + pendingY);
				} else {
					// nothing
				}
				pendingX = pendingY = 0;
			} else {
				pendingX += dx;
				pendingY += dy;
			}
		}

		if (Math.abs(1 - detector.getScaleFactor()) > 0.005f) {
			//			if (mSelectedItem == null || !mSupportScaleItem) {
			// 缩放图片
			float scale = mDoodle.getDoodleScale() * detector.getScaleFactor() * pendingScale;
			mDoodle.setDoodleScale(scale, mDoodle.toX(mTouchCentreX), mDoodle.toY(mTouchCentreY));
			//			} else {
			//				mSelectedItem.setScale(mSelectedItem.getScale() * detector
			//				.getScaleFactor() * pendingScale);
			//			}
			pendingScale = 1;
		} else {
			pendingScale *= detector.getScaleFactor();
		}

		mLastFocusX = mTouchCentreX;
		mLastFocusY = mTouchCentreY;


		return true;
	}

	private float pendingX, pendingY, pendingScale = 1;

	@Override
	public boolean onScaleBegin (ScaleGestureDetectorApi27 detector) {
		Log.e(TAG, "onScaleBegin: ");
		mLastFocusX = null;
		mLastFocusY = null;
		//		return super.onScaleBegin(detector);
		return true;
	}

	@Override
	public void onScaleEnd (ScaleGestureDetectorApi27 detector) {
		Log.e(TAG, "onScaleEnd: ");
		//		if (mDoodle.isEditMode()) {
		//			limitBound(true);
		//			return;
		//		}

		center();
		//		super.onScaleEnd(detector);
	}

	public void center () {
		if (mDoodle.getDoodleScale() < 1) { //
			if (mScaleAnimator == null) {
				mScaleAnimator = new ValueAnimator();
				mScaleAnimator.setDuration(100);
				mScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate (ValueAnimator animation) {
						float value = (float) animation.getAnimatedValue();
						float fraction = animation.getAnimatedFraction();
						mDoodle.setDoodleScale(value, mDoodle.toX(mTouchCentreX),
								mDoodle.toY(mTouchCentreY));
						mDoodle.setDoodleTranslation(mScaleAnimTransX * (1 - fraction),
								mScaleAnimTranY * (1 - fraction));
					}
				});
			}
			mScaleAnimator.cancel();
			mScaleAnimTransX = mDoodle.getDoodleTranslationX();
			mScaleAnimTranY = mDoodle.getDoodleTranslationY();
			mScaleAnimator.setFloatValues(mDoodle.getDoodleScale(), 1);
			mScaleAnimator.start();
		} else { //
			limitBound(true);
		}
	}

	/**
	 * 限定边界
	 *
	 * @param anim 动画效果
	 */
	public void limitBound (boolean anim) {
		if (mDoodle.getDoodleRotation() % 90 != 0) { // 只处理0,90,180,270
			return;
		}

		final float oldX = mDoodle.getDoodleTranslationX(), oldY = mDoodle.getDoodleTranslationY();
		RectF bound = mDoodle.getDoodleBound();
		float x = mDoodle.getDoodleTranslationX(), y = mDoodle.getDoodleTranslationY();
		float width = mDoodle.getCenterWidth() * mDoodle.getRotateScale(), height =
				mDoodle.getCenterHeight() * mDoodle.getRotateScale();

		// 上下都在屏幕内
		if (bound.height() <= mDoodle.getHeight()) {
			if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
				y = (height - height * mDoodle.getDoodleScale()) / 2;
			} else {
				x = (width - width * mDoodle.getDoodleScale()) / 2;
			}
		} else {
			float heightDiffTop = bound.top;
			// 只有上在屏幕内
			if (bound.top > 0 && bound.bottom >= mDoodle.getHeight()) {
				if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
					if (mDoodle.getDoodleRotation() == 0) {
						y = y - heightDiffTop;
					} else {
						y = y + heightDiffTop;
					}
				} else {
					if (mDoodle.getDoodleRotation() == 90) {
						x = x - heightDiffTop;
					} else {
						x = x + heightDiffTop;
					}
				}
			} else if (bound.bottom < mDoodle.getHeight() && bound.top <= 0) { // 只有下在屏幕内
				float heightDiffBottom = mDoodle.getHeight() - bound.bottom;
				if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
					if (mDoodle.getDoodleRotation() == 0) {
						y = y + heightDiffBottom;
					} else {
						y = y - heightDiffBottom;
					}
				} else {
					if (mDoodle.getDoodleRotation() == 90) {
						x = x + heightDiffBottom;
					} else {
						x = x - heightDiffBottom;
					}
				}
			}
		}

		// 左右都在屏幕内
		if (bound.width() <= mDoodle.getWidth()) {
			if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
				x = (width - width * mDoodle.getDoodleScale()) / 2;
			} else {
				y = (height - height * mDoodle.getDoodleScale()) / 2;
			}
		} else {
			float widthDiffLeft = bound.left;
			// 只有左在屏幕内
			if (bound.left > 0 && bound.right >= mDoodle.getWidth()) {
				if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
					if (mDoodle.getDoodleRotation() == 0) {
						x = x - widthDiffLeft;
					} else {
						x = x + widthDiffLeft;
					}
				} else {
					if (mDoodle.getDoodleRotation() == 90) {
						y = y + widthDiffLeft;
					} else {
						y = y - widthDiffLeft;
					}
				}
			} else if (bound.right < mDoodle.getWidth() && bound.left <= 0) { // 只有右在屏幕内
				float widthDiffRight = mDoodle.getWidth() - bound.right;
				if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
					if (mDoodle.getDoodleRotation() == 0) {
						x = x + widthDiffRight;
					} else {
						x = x - widthDiffRight;
					}
				} else {
					if (mDoodle.getDoodleRotation() == 90) {
						y = y - widthDiffRight;
					} else {
						y = y + widthDiffRight;
					}
				}
			}
		}
		if (anim) {
			if (mTranslateAnimator == null) {
				mTranslateAnimator = new ValueAnimator();
				mTranslateAnimator.setDuration(100);
				mTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate (ValueAnimator animation) {
						float value = (float) animation.getAnimatedValue();
						float fraction = animation.getAnimatedFraction();
						mDoodle.setDoodleTranslation(value,
								mTransAnimOldY + (mTransAnimY - mTransAnimOldY) * fraction);
					}
				});
			}
			mTranslateAnimator.setFloatValues(oldX, x);
			mTransAnimOldY = oldY;
			mTransAnimY = y;
			mTranslateAnimator.start();
		} else {
			mDoodle.setDoodleTranslation(x, y);
		}
	}
}
