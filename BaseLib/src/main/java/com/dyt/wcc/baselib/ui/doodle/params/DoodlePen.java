package com.dyt.wcc.baselib.ui.doodle.params;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleItem;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodlePen;


/**
 * <p>Copyright (C),2022/11/30 15:26-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/30 15:26     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.params     </p>
 * <p>Description：       </p>
 */
public enum DoodlePen implements IDoodlePen {
	BRUSH,
	TEXT;

//	private copylocation mCopy


	@Override
	public void config (IDoodleItem doodleItem, Paint paint) {


	}

	@Override
	public IDoodlePen copy () {
		return this;
	}

	@Override
	public void drawHelpers (Canvas canvas, IDoodle iDoodle) {

	}
}
