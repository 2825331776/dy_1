package com.dytest.wcc.common.widget.dragView;

import androidx.annotation.NonNull;

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
public class MeasurePointEntity {
	private int type;//1最高温 2最低温， 3中心温度  用于区分画笔 颜色 和图片，默认是点测温的

	private float startPointX;
	private float startPointY;

	private String temp;//温度值
	private int    tempDirection;////绘制前通过计算坐标初始化 方位

	public int getType () {
		return type;
	}

	public void setType (int type) {
		this.type = type;
	}

	public int getTempDirection () {
		return tempDirection;
	}

	public void setTempDirection (int tempDirection) {
		this.tempDirection = tempDirection;
	}

	public float getStartPointX () {
		return startPointX;
	}

	public void setStartPointX (float startPointX) {
		this.startPointX = startPointX;
	}

	public float getStartPointY () {
		return startPointY;
	}

	public void setStartPointY (float startPointY) {
		this.startPointY = startPointY;
	}

	public String getTemp () {
		return temp;
	}

	public void setTemp (String temp) {
		this.temp = temp;
	}

	@NonNull
	@Override
	public String toString () {
		return "PointTempWidget{" + "type=" + type + ", startPointX=" + startPointX + ", startPointY=" + startPointY + ", temp='" + temp + '\'' + ", tempDirection=" + tempDirection + '}';
	}
}
