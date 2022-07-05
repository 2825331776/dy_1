package com.dyt.wcc.cameracommon.widget;

/**
 * <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
 * <p>Author：stefan cheng    </p>
 * <p>Create Date：2021/9/8  16:30 </p>
 * <p>Description：@todo describe         </p>
 * <p>PackgePath: com.dyt.wcc.cameracommon.widget     </p>
 */
public class TouchPoint {
	public float   x;
	public float   y;
	public int     indexOfPoint;
	public boolean isSelected = false;

	@Override
	public String toString () {
		return "TouchPoint{" + "x=" + x + ", y=" + y + ", indexOfPoint=" + indexOfPoint + ", isSelected=" + isSelected + '}';
	}
}

