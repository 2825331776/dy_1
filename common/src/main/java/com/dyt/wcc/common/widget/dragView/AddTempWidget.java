package com.dyt.wcc.common.widget.dragView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/29  16:48     </p>
 * <p>Description：具体数据 对象         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public class AddTempWidget {
	private int id;//必须
	private int type; // 必须  1为点  2为线  3矩形  4 高低温
	private boolean isSelect;//是否显示工具栏和背景色
	private boolean isCanMove;//是否响应用户控制    /是否有工具栏 ==== 必须

	private int tempTextSize ;//温度字体大小      非必须

	private String textSuffix;//温度后缀 ℃  ℉  K 必须 从本地读

	private int [] toolsPicRes;//工具栏图片集合
	private int toolsNumber;

	private PointTempWidget pointTemp;
	private OtherTempWidget otherTemp;

	public int getTempTextSize () {
		return tempTextSize;
	}

	public void setTempTextSize (int tempTextSize) {
		this.tempTextSize = tempTextSize;
	}

	public String getTextSuffix () {
		return textSuffix;
	}

	public void setTextSuffix (String textSuffix) {
		this.textSuffix = textSuffix;
	}

	public int[] getToolsPicRes () {
		return toolsPicRes;
	}

	public void setToolsPicRes (int[] toolsPicRes) {
		this.toolsPicRes = toolsPicRes;
		this.toolsNumber = toolsPicRes.length;
	}

	public int getToolsNumber () {
		return toolsNumber;
	}

	public int getId () {
		return id;
	}

	public void setId (int id) {
		this.id = id;
	}

	public int getType () {
		return type;
	}

	public void setType (int type) {
		this.type = type;
	}

	public boolean isSelect () {
		return isSelect;
	}

	public void setSelect (boolean select) {
		isSelect = select;
	}

	public boolean isCanMove () {
		return isCanMove;
	}

	public void setCanMove (boolean canMove) {
		isCanMove = canMove;
	}

	public PointTempWidget getPointTemp () {
		return pointTemp;
	}

	public void setPointTemp (PointTempWidget pointTemp) {
		this.pointTemp = pointTemp;
	}

	public OtherTempWidget getOtherTemp () {
		return otherTemp;
	}

	public void setOtherTemp (OtherTempWidget otherTemp) {
		this.otherTemp = otherTemp;
	}
}
