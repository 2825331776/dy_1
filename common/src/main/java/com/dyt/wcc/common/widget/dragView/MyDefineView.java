package com.dyt.wcc.common.widget.dragView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/27  10:55     </p>
 * <p>Description：包含一次操作的信息</p>
 *  <p>id 必须有</p>
 *  <p>startPoint 开始点坐标（必须）单个点 线 矩阵都有</p>
 *  <p>endPoint 单个点无此属性 </p>
 *  <p>minTemp 查询数据之后填充的最小温度（初始化无数据）</p>
 *  <p>maxTemp 查询数据之后填充的最大温度（初始化无数据）</p>
 *  <p>drawType 绘制的类型（必须，区分点1、线2、矩阵3）</p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */

public class MyDefineView {
	private int id;

	private int startPointX;
	private int startPointY;
	private int endPointX;
	private int endPointY;

	private float minTemp;
	private float maxTemp;
	private int minTextColor;//字体颜色
	private int maxTextColor;
	private int minTextSize ;//字体大小

	private String textSuffix;//温度后缀 ℃  ℉ K

	private int drawType;//点类型

	public int getId () {
		return id;
	}

	public void setId (int id) {
		this.id = id;
	}

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

	public float getMaxTemp () {
		return maxTemp;
	}

	public void setMaxTemp (float maxTemp) {
		this.maxTemp = maxTemp;
	}

	public int getMinTextColor () {
		return minTextColor;
	}

	public void setMinTextColor (int minTextColor) {
		this.minTextColor = minTextColor;
	}

	public int getMaxTextColor () {
		return maxTextColor;
	}

	public void setMaxTextColor (int maxTextColor) {
		this.maxTextColor = maxTextColor;
	}

	public int getMinTextSize () {
		return minTextSize;
	}

	public void setMinTextSize (int minTextSize) {
		this.minTextSize = minTextSize;
	}

	public String getTextSuffix () {
		return textSuffix;
	}

	public void setTextSuffix (String textSuffix) {
		this.textSuffix = textSuffix;
	}

	public int getDrawType () {
		return drawType;
	}

	public void setDrawType (int drawType) {
		this.drawType = drawType;
	}
}
