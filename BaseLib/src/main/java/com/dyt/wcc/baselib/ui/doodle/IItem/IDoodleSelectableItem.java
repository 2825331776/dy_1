package com.dyt.wcc.baselib.ui.doodle.IItem;

import android.graphics.Rect;

/**
 * <p>Copyright (C),2022/11/30 10:40-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/30 10:40     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.item     </p>
 * <p>Description：可选中 涂鸦item 实现的接口       </p>
 */
public interface IDoodleSelectableItem extends IDoodleItem{
	/**
	 * 设置是否选中
	 *
	 * @param isSelect
	 */
	void setSelect (boolean isSelect);

	/**
	 * 是否选中
	 *
	 * @return
	 */
	boolean isSelected ();

	/**
	 * item的矩形(缩放scale之后)范围
	 *
	 * @return
	 */
	Rect getBounds ();

	/**
	 * 判断点（x,y）是否在item内，用于判断是否点中item
	 */
	boolean contains (float x, float y);

}
