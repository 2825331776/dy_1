package com.dyt.wcc.common.widget.dragView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/29  16:49     </p>
 * <p>Description：绘制线、矩阵 对象         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public class OtherTempWidget {
	private float startPointX;
	private float startPointY;
	private float endPointX;
	private float endPointY;

	//可用点对象概括 默认type 为2
	private String minTemp;
	private float minTempX;
	private float minTempY;
	private int minTempDirection;//绘制前通过计算坐标初始化 方位

	private String maxTemp;
	private float maxTempX;
	private float maxTempY;
	private int maxTempDirection;//绘制前通过计算坐标初始化  方位

	@Override
	public String toString () {
		return "OtherTempWidget{" + "startPointX=" + startPointX + ", startPointY=" + startPointY + ", endPointX=" + endPointX +
				", endPointY=" + endPointY + ", minTemp='" + minTemp + '\'' +
				", minTempX=" + minTempX + ", minTempY=" + minTempY + ", minTempDirection=" + minTempDirection + ", maxTemp='" + maxTemp + '\'' +
				", maxTempX=" + maxTempX + ", maxTempY=" + maxTempY + ", maxTempDirection=" + maxTempDirection + '}';
	}

	public int getMinTempDirection () { return minTempDirection; }
	public void setMinTempDirection (int minTempDirection) { this.minTempDirection = minTempDirection; }
	public int getMaxTempDirection () {return maxTempDirection; }
	public void setMaxTempDirection (int maxTempDirection) { this.maxTempDirection = maxTempDirection; }

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

	public float getEndPointX () {
		return endPointX;
	}

	public void setEndPointX (float endPointX) {
		this.endPointX = endPointX;
	}

	public float getEndPointY () {
		return endPointY;
	}

	public void setEndPointY (float endPointY) {
		this.endPointY = endPointY;
	}

	public String getMinTemp () {
		return minTemp;
	}

	public void setMinTemp (String minTemp) {
		this.minTemp = minTemp;
	}

	public float getMinTempX () {
		return minTempX;
	}

	public void setMinTempX (float minTempX) {
		this.minTempX = minTempX;
	}

	public float getMinTempY () {
		return minTempY;
	}

	public void setMinTempY (float minTempY) {
		this.minTempY = minTempY;
	}

	public String getMaxTemp () {
		return maxTemp;
	}

	public void setMaxTemp (String maxTemp) {
		this.maxTemp = maxTemp;
	}

	public float getMaxTempX () {
		return maxTempX;
	}

	public void setMaxTempX (float maxTempX) {
		this.maxTempX = maxTempX;
	}

	public float getMaxTempY () {
		return maxTempY;
	}

	public void setMaxTempY (float maxTempY) {
		this.maxTempY = maxTempY;
	}
}
