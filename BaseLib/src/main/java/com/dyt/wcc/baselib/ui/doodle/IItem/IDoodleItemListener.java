package com.dyt.wcc.baselib.ui.doodle.IItem;

/**
 * <p>Copyright (C),2022/11/29 16:16-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/29 16:16     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.item     </p>
 * <p>Description： 画笔属性更改 回调接口      </p>
 */
public interface IDoodleItemListener {

	public int PROPERTY_SCALE = 1;
	public int PROPERTY_ROTATE = 2;
	public int PROPERTY_PIVOT_X = 3;
	public int PROPERTY_PIVOT_Y = 4;
	public int PROPERTY_SIZE = 5;
	public int PROPERTY_COLOR = 6;
	public int PROPERTY_LOCATION = 7;

	/**
	 * 属性改变时回调
	 * @param property 属性
	 */
	void onPropertyChanged(int property);
}
