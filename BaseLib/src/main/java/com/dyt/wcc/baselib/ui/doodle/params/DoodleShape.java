package com.dyt.wcc.baselib.ui.doodle.params;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleItem;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleShape;


/**
 * <p>Copyright (C),2022/11/30 15:19-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/30 15:19     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.base     </p>
 * <p>Description：       </p>
 */
public enum DoodleShape implements IDoodleShape {
	HAND_WRITE, // 手绘
	ARROW, // 箭头

	POINT, // 点
	LINE, // 直线
	RECT, // 矩形
	OVAL// 椭圆
	;

	@Override
	public void config (IDoodleItem doodleItem, Paint paint) {
		if (doodleItem.getShape() == DoodleShape.POINT){
			paint.setStyle(Paint.Style.FILL);
		}else {
			paint.setStyle(Paint.Style.STROKE);
		}
	}

	@Override
	public IDoodleShape copy () {
		return this;
	}

	@Override
	public void drawHelpers (Canvas canvas, IDoodle iDoodle) {

	}
}
