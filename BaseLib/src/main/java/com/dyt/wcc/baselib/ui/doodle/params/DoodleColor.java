package com.dyt.wcc.baselib.ui.doodle.params;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.Log;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleColor;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleItem;
import com.dyt.wcc.baselib.ui.doodle.base.DoodleItemBase;


/**
 * <p>Copyright (C),2022/11/30 15:25-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/30 15:25     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.params     </p>
 * <p>Description：       </p>
 */
public class DoodleColor implements IDoodleColor {
	public enum ColorType {
		COLOR, BITMAP
	}

	private int       mColor;
	private Bitmap    mBitmap;
	private ColorType mColorType;
	private Matrix    mMatrix;

	private int mLevel = 1;

	//about bitmap
	private Shader.TileMode mTileX = Shader.TileMode.MIRROR;
	private Shader.TileMode mTileY = Shader.TileMode.MIRROR;

	public DoodleColor (int color) {
		mColorType = ColorType.COLOR;
		mColor = color;
	}

	public DoodleColor (Bitmap bitmap) {
		this(bitmap, null);
	}

	public DoodleColor (Bitmap bitmap, Matrix matrix) {
		this(bitmap, matrix, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);
	}

	public DoodleColor (Bitmap bitmap, Matrix matrix, Shader.TileMode tileX, Shader.TileMode tileY) {
		mColorType = ColorType.BITMAP;
		mBitmap = bitmap;
		mMatrix = matrix;
		mTileX = tileX;
		mTileY = tileY;
	}

	//-----------------getter  setter-----------------------
	public void setColor (int color) {
		this.mColorType = ColorType.COLOR;
		this.mColor = color;
	}

	public void setColor (Bitmap bitmap) {
		mColorType = ColorType.BITMAP;
		mBitmap = bitmap;
	}

	public void setColor (Bitmap bitmap, Matrix matrix) {
		mColorType = ColorType.BITMAP;
		mMatrix = matrix;
		mBitmap = bitmap;
	}

	public void setColor (Bitmap bitmap, Matrix matrix, Shader.TileMode tileX, Shader.TileMode tileY) {
		mColorType = ColorType.BITMAP;
		mMatrix = matrix;
		mBitmap = bitmap;
		mTileX = tileX;
		mTileY = tileY;
	}

	public void setMatrix (Matrix matrix) {
		mMatrix = matrix;
	}

	public Matrix getMatrix () {
		return mMatrix;
	}

	public int getColor () {
		return mColor;
	}

	public Bitmap getBitmap () {
		return mBitmap;
	}

	public ColorType getColorType () {
		return mColorType;
	}

	public void setLevel (int level) {
		mLevel = level;
	}

	public int getLevel () {
		return mLevel;
	}

	//---------------override ---------------------
	@Override
	public void config (IDoodleItem doodleItem, Paint paint) {
		DoodleItemBase itemBase = (DoodleItemBase) doodleItem;
		if (mColorType == ColorType.COLOR) {
			paint.setColor(mColor);
			paint.setShader(null);
//			Log.e("TAG", "---------------------------config: ------------color-----------");
		} else if (mColorType == ColorType.BITMAP) {
			BitmapShader shader = new BitmapShader(mBitmap, mTileX, mTileY);
			shader.setLocalMatrix(mMatrix);
			paint.setShader(shader);
		}
	}

	@Override
	public IDoodleColor copy () {
		DoodleColor penColor = null;
		if (mColorType == ColorType.COLOR){
			penColor = new DoodleColor(mColor);
		}else {
			penColor = new DoodleColor(mBitmap);
		}
		penColor.mTileX = mTileX;
		penColor.mTileY = mTileY;
		penColor.mMatrix = mMatrix;
		penColor.mLevel = mLevel;
		return penColor;
	}
}
