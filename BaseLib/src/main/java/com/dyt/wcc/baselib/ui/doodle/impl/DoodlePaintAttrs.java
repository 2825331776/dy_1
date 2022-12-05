package com.dyt.wcc.baselib.ui.doodle.impl;

import androidx.annotation.NonNull;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleColor;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodlePen;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleShape;

import org.jetbrains.annotations.Contract;

/**
 * <p>Copyright (C),2022/11/29 17:11-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/29 17:11     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.item     </p>
 * <p>Description：画笔 简单的参数设置         </p>
 */
public class DoodlePaintAttrs {
	private IDoodlePen   mPenType;
	private IDoodleShape mPenShape;
	private IDoodleColor mPenColor;
	private float        mSize ;

	public IDoodlePen getPenType () {
		return mPenType;
	}

	public DoodlePaintAttrs setPenType (IDoodlePen mPenType) {
		this.mPenType = mPenType;
		return this;
	}

	public IDoodleShape getPenShape () {
		return mPenShape;
	}

	public DoodlePaintAttrs setPenShape (IDoodleShape mPenShape) {
		this.mPenShape = mPenShape;
		return this;
	}

	public IDoodleColor getPenColor () {
		return mPenColor;
	}

	public DoodlePaintAttrs setPenColor (IDoodleColor mPenColor) {
		this.mPenColor = mPenColor;
		return this;
	}

	public float getSize () {
		return mSize;
	}

	public DoodlePaintAttrs setSize (float mSize) {
		this.mSize = mSize;
		return this;
	}

	@NonNull
	@Contract(" -> new")
	public static DoodlePaintAttrs create(){
		return new DoodlePaintAttrs();
	}
}
