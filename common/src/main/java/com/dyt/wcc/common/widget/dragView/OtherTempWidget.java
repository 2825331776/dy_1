package com.dyt.wcc.common.widget.dragView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/29  16:49     </p>
 * <p>Description：绘制线、矩阵 对象         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public class OtherTempWidget {
	private int startPointX;
	private int startPointY;
	private int endPointX;
	private int endPointY;

	//可用点对象概括 默认type 为2
	private float minTemp;
	private int minTempX;
	private int minTempY;
	private int minTempDirection;//绘制前通过计算坐标初始化 方位

	private float maxTemp;
	private int maxTempX;
	private int maxTempY;
	private int maxTempDirection;//绘制前通过计算坐标初始化  方位

	public int getMinTempDirection () { return minTempDirection; }
	public void setMinTempDirection (int minTempDirection) { this.minTempDirection = minTempDirection; }
	public int getMaxTempDirection () {return maxTempDirection; }
	public void setMaxTempDirection (int maxTempDirection) { this.maxTempDirection = maxTempDirection; }
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

	public int getEndPointX () {
		return endPointX;
	}

	public void setEndPointX (int endPointX) {
		this.endPointX = endPointX;
	}

	public int getEndPointY () {
		return endPointY;
	}

	public void setEndPointY (int endPointY) {
		this.endPointY = endPointY;
	}

	public float getMinTemp () {
		return minTemp;
	}

	public void setMinTemp (float minTemp) {
		this.minTemp = minTemp;
	}

	public int getMinTempX () {
		return minTempX;
	}

	public void setMinTempX (int minTempX) {
		this.minTempX = minTempX;
	}

	public int getMinTempY () {
		return minTempY;
	}

	public void setMinTempY (int minTempY) {
		this.minTempY = minTempY;
	}

	public float getMaxTemp () {
		return maxTemp;
	}

	public void setMaxTemp (float maxTemp) {
		this.maxTemp = maxTemp;
	}

	public int getMaxTempX () {
		return maxTempX;
	}

	public void setMaxTempX (int maxTempX) {
		this.maxTempX = maxTempX;
	}

	public int getMaxTempY () {
		return maxTempY;
	}

	public void setMaxTempY (int maxTempY) {
		this.maxTempY = maxTempY;
	}
}
