package com.dyt.wcc.baselib.ui.doodle.impl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleColor;
import com.dyt.wcc.baselib.ui.doodle.base.DoodleRotatableItemBase;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleColor;

import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * <p>Copyright (C),2022/12/1 17:58-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/12/1 17:58     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.IItem     </p>
 * <p>Description：       </p>
 */
public class DoodlePath extends DoodleRotatableItemBase {
	private final Path mPath       = new Path(); // 画笔的路径
	private final Path mOriginPath = new Path();

	private Paint mPaint = new Paint();

	private final Matrix mTransform         = new Matrix();
	private       Rect   mRect              = new Rect();
	private       Matrix mBitmapColorMatrix = new Matrix();

	private PointF mSxy = new PointF(); // 映射后的起始坐标，（手指点击）
	private PointF mDxy = new PointF(); // 映射后的终止坐标，（手指抬起）

	private RectF mBound = new RectF();

	public DoodlePath (IDoodle doodle) {
		super(doodle, 0, 0, 0);// 这里默认item旋转角度为0
	}

	public DoodlePath (IDoodle doodle, int itemRotation, float x, float y) {
		super(doodle, itemRotation, x, y);
	}

	public DoodlePath (IDoodle doodle, DoodlePaintAttrs attrs, int itemRotation, float x,
	                   float y) {
		super(doodle, attrs, itemRotation, x, y);
	}

	public static DoodlePath toPath(IDoodle doodle, Path p) {
		DoodlePath path = new DoodlePath(doodle);
		path.setPen(doodle.getPen().copy());
		path.setShape(doodle.getShape().copy());
		path.setSize(doodle.getSize());
		path.setColor(doodle.getColor().copy());

		path.updatePath(p);
//		if (doodle instanceof DoodleView) {
//			path.mCopyLocation = DoodlePen.COPY.getCopyLocation().copy();
//		} else {
//			path.mCopyLocation = null;
//		}
		return path;
	}
	public void updatePath(Path path) {
		mOriginPath.reset();
		this.mOriginPath.addPath(path);
		adjustPath(true);
	}

	@Override
	protected void resetBounds (Rect rect) {
		resetLocationBounds(rect);
		rect.set(0, 0, rect.width(), rect.height());
	}


	@Override
	public void setItemRotate (float textRotate) {
		super.setItemRotate(textRotate);
		//		adjustMosaic();
	}

	@Override
	public boolean isDoodleEditable () {
		return false;
	}


	private static WeakHashMap<IDoodle, HashMap<Integer, Bitmap>> sMosaicBitmapMap =
			new WeakHashMap<>();

	public static DoodleColor getMosaicColor (IDoodle doodle, int level) {
		HashMap<Integer, Bitmap> map = sMosaicBitmapMap.get(doodle);
		if (map == null) {
			map = new HashMap<>();
			sMosaicBitmapMap.put(doodle, map);
		}
		Matrix matrix = new Matrix();
		matrix.setScale(1f / level, 1f / level);
		Bitmap mosaicBitmap = map.get(level);
		if (mosaicBitmap == null) {
			mosaicBitmap = Bitmap.createBitmap(doodle.getBitmap(), 0, 0,
					doodle.getBitmap().getWidth(), doodle.getBitmap().getHeight(), matrix, true);
			map.put(level, mosaicBitmap);
		}
		matrix.reset();
		matrix.setScale(level, level);
		DoodleColor doodleColor = new DoodleColor(mosaicBitmap, matrix, Shader.TileMode.REPEAT,
				Shader.TileMode.REPEAT);
		doodleColor.setLevel(level);
		return doodleColor;
	}

	@Override
	public void setLocation (float x, float y, boolean changePivot) {
		super.setLocation(x, y, changePivot);
		//		adjustMosaic();
	}

	@Override
	public void setColor (IDoodleColor color) {
		super.setColor(color);
		//		if (getPen() == DoodlePen.MOSAIC) {
		//			setLocation(getLocation().x, getLocation().y, false);
		//		}
		adjustPath(false);
	}

	@Override
	public void setSize (float size) {
		super.setSize(size);


		if (mTransform == null) {
			return;
		}

		//		if (DoodleShape.ARROW.equals(getShape())) {
		//			mOriginPath.reset();
		//			updateArrowPath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
		//		}

		adjustPath(false);
	}

	@Override
	public void setScale (float scale) {
		super.setScale(scale);
		//		adjustMosaic();
	}

	//	private void adjustMosaic() {
	//		if (getPen() == DoodlePen.MOSAIC
	//				&& getColor() instanceof DoodleColor) {
	//			DoodleColor doodleColor = ((DoodleColor) getColor());
	//			Matrix matrix = doodleColor.getMatrix();
	//			matrix.reset();
	//			matrix.preScale(1 / getScale(), 1 / getScale(), getPivotX(), getPivotY()); //
	//			restore scale
	//			matrix.preTranslate(-getLocation().x * getScale(), -getLocation().y * getScale());
	//			matrix.preRotate(-getItemRotate(), getPivotX(), getPivotY());
	//			matrix.preScale(doodleColor.getLevel(), doodleColor.getLevel());
	//			doodleColor.setMatrix(matrix);
	//			refresh();
	//		}
	//	}

	@Override
	protected void doDraw (Canvas canvas) {
		mPaint.reset();
		mPaint.setStrokeWidth(getSize());
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setDither(true);
		mPaint.setAntiAlias(true);

		getPen().config(this, mPaint);
		getColor().config(this, mPaint);
		getShape().config(this, mPaint);

		canvas.drawPath(getPath(), mPaint);
	}

	public Path getPath () {
		return mPath;
	}

	private PointF getDxy () {
		return mDxy;
	}

	private PointF getSxy () {
		return mSxy;
	}

	private void resetLocationBounds (Rect rect) {
		if (mOriginPath == null) {
			return;
		}

		int diff = (int) (getSize() / 2 + 0.5f);
		mOriginPath.computeBounds(mBound, false);
		/*if (getShape() == DoodleShape.ARROW || getShape() == DoodleShape.FILL_CIRCLE || getShape
		() == DoodleShape.FILL_RECT) {
			diff = (int) getDoodle().getUnitSize();
		}*/
		rect.set((int) (mBound.left - diff), (int) (mBound.top - diff),
				(int) (mBound.right + diff), (int) (mBound.bottom + diff));
	}

	private void adjustPath (boolean changePivot) {
		resetLocationBounds(mRect);
		mPath.reset();
		this.mPath.addPath(mOriginPath);
		mTransform.reset();
		mTransform.setTranslate(-mRect.left, -mRect.top);
		mPath.transform(mTransform);
		if (changePivot) {
			setPivotX(mRect.left + mRect.width() / 2);
			setPivotY(mRect.top + mRect.height() / 2);
			setLocation(mRect.left, mRect.top, false);
		}

		/*if ((getColor() instanceof DoodleColor)) {
			DoodleColor color = (DoodleColor) getColor();
			if (color.getType() == DoodleColor.Type.BITMAP && color.getBitmap() != null) {
				mBitmapColorMatrix.reset();

				if (getPen() == DoodlePen.MOSAIC) {
					adjustMosaic();
				} else {
					if (getPen() == DoodlePen.COPY) {
						// 根据旋转值获取正确的旋转底图
						float transXSpan = 0, transYSpan = 0;
						CopyLocation copyLocation = getCopyLocation();
						// 仿制时需要偏移图片
						if (copyLocation != null) {
							transXSpan = copyLocation.getTouchStartX() - copyLocation
							.getCopyStartX();
							transYSpan = copyLocation.getTouchStartY() - copyLocation
							.getCopyStartY();
						}
						resetLocationBounds(mRect);
						mBitmapColorMatrix.setTranslate(transXSpan - mRect.left, transYSpan -
						mRect.top);
					} else {
						mBitmapColorMatrix.setTranslate(-mRect.left, -mRect.top);
					}

					int level = color.getLevel();
					mBitmapColorMatrix.preScale(level, level);
					color.setMatrix(mBitmapColorMatrix);
					refresh();
				}
			}
		}*/

		refresh();
	}
}
