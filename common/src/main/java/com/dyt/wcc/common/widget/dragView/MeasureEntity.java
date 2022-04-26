package com.dyt.wcc.common.widget.dragView;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/29  16:48     </p>
 * <p>Description：具体数据 对象         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public class MeasureEntity {
	private int id;//必须
	private int type; // 必须  1为点  2为线  3矩形  4 高低温
	private boolean isSelect;//是否显示工具栏和背景色
//	private boolean isCanMove;//是否响应用户控制    /是否有工具栏 ==== 必须
	private boolean isMoving = false;//是否在移动中

	private int tempTextSize ;//温度字体大小      非必须

	private List<Bitmap> toolsPicsBp;//工具栏图片集合
	private int          toolsNumber;
	//主屏是否旋转了。旋转后 偏移计算
	private boolean isRotate = false;

	//高温 低温 开关
	private boolean highTempToggle = false;
	private boolean lowTempToggle  = false;

	private MeasurePointEntity    pointTemp;
	private MeasureLineRectEntity otherTemp;
	public MeasureEntity (){
		if (toolsPicsBp == null) {
			toolsPicsBp = new ArrayList<>();
		}
	}
	public boolean isRotate () {
		return isRotate;
	}
	public void setRotate (boolean rotate) {
		isRotate = rotate;
	}
	public boolean isHighTempToggle () {
		return highTempToggle;
	}

	public void setHighTempToggle (boolean highTempToggle) {
		this.highTempToggle = highTempToggle;
	}

	public boolean isLowTempToggle () {
		return lowTempToggle;
	}

	public void setLowTempToggle (boolean lowTempToggle) {
		this.lowTempToggle = lowTempToggle;
	}

	public int getTempTextSize () {
		return tempTextSize;
	}

	public void setTempTextSize (int tempTextSize) {
		this.tempTextSize = tempTextSize;
	}

	public boolean isMoving () { return isMoving;
	}
	public void setMoving (boolean moving) { isMoving = moving; }

	@Override
	public String toString () {
		//", isCanMove=" + isCanMove +
		return "TempWidgetObj{" + "id=" + id + ", type=" + type + ", isSelect=" + isSelect +
				 ", tempTextSize=" + tempTextSize + ", toolsPicRes size =" + toolsPicsBp.size() +
				", toolsNumber=" + toolsNumber + ", pointTemp=" + pointTemp.toString() + ", otherTemp=" + otherTemp.toString() + '}';
	}

	public List<Bitmap> getToolsPicRes () {
		return toolsPicsBp;
	}

	public void addToolsBp (Bitmap bp) {
		if (toolsPicsBp ==null){
			toolsPicsBp = new ArrayList<>();
		}
		this.toolsPicsBp.add(bp);
		this.toolsNumber = toolsPicsBp.size();
	}

	public int getToolsNumber () {
		return toolsPicsBp.size();
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

	public MeasurePointEntity getPointTemp () {
		return pointTemp;
	}

	public void setPointTemp (MeasurePointEntity pointTemp) {
		this.pointTemp = pointTemp;
	}

	public MeasureLineRectEntity getOtherTemp () {
		return otherTemp;
	}

	public void setOtherTemp (MeasureLineRectEntity otherTemp) {
		this.otherTemp = otherTemp;
	}
}
