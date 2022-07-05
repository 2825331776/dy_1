package com.dyt.wcc.common.widget.dragView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/6/28  17:08     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public class LineRectEntity {
	private PointEntity minPoint;
	private PointEntity maxPoint;

	public PointEntity getMinPoint () {
		return minPoint;
	}

	public void setMinPoint (PointEntity minPoint) {
		this.minPoint = minPoint;
	}

	public PointEntity getMaxPoint () {
		return maxPoint;
	}

	public void setMaxPoint (PointEntity maxPoint) {
		this.maxPoint = maxPoint;
	}
}
