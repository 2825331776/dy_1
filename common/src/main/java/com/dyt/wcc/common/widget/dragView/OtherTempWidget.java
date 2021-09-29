package com.dyt.wcc.common.widget.dragView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/29  16:49     </p>
 * <p>Description：绘制线、矩阵 对象         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public class OtherTempWidget {
	private int lineColor;//绘制的线和 矩阵边框颜色

	private int startPointX;
	private int startPointY;
	private int endPointX;
	private int endPointY;

	private float minTemp;
	private float minTempX;
	private float minTempY;

	private float maxTemp;
	private float maxTempX;
	private float maxTempY;

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

	public float getMaxTemp () {
		return maxTemp;
	}

	public void setMaxTemp (float maxTemp) {
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
