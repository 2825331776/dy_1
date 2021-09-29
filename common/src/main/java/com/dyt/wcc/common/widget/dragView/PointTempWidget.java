package com.dyt.wcc.common.widget.dragView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/29  16:49     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 *
 * <p>点分为  高低温                 温度值和坐标   都会修改</p>
 * <p>    中心温度/点测温模式     温度值修改，坐标不改     </p>
 * <p>    线/矩阵 测温模式       范围内 最高最低 温度值和坐标    都改（温度依靠查询温度表）</p>
 */
public class PointTempWidget {

	private int startPointX;
	private int startPointY;

	private float temp;//温度值


	public int getStartPointX () {
		return startPointX;
	}
	public void setStartPointX (int startPointX) {
		this.startPointX = startPointX;
	}
	public int getStartPointY () {
		return startPointY;
	}
	public void setStartPointY (int startPointY) {
		this.startPointY = startPointY;
	}
	public float getTemp () {
		return temp;
	}

	public void setTemp (float temp) {
		this.temp = temp;
	}

}
