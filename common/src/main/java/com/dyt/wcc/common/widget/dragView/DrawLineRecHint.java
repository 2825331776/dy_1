package com.dyt.wcc.common.widget.dragView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/12/24  14:17     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public class DrawLineRecHint {
	private int drawTempMode;
	private boolean needDraw = false;

	public boolean isNeedDraw () {
		return needDraw;
	}

	public void setNeedDraw (boolean needDraw) {
		this.needDraw = needDraw;
	}

	private float startXCoordinate;
	private float startYCoordinate;

	private float endXCoordinate;
	private float endYCoordinate;

	public int getDrawTempMode () {
		return drawTempMode;
	}

	public void setDrawTempMode (int drawTempMode) {
		this.drawTempMode = drawTempMode;
	}

	public float getStartXCoordinate () {
		return startXCoordinate;
	}

	public void setStartXCoordinate (float startXCoordinate) {
		this.startXCoordinate = startXCoordinate;
	}

	public float getStartYCoordinate () {
		return startYCoordinate;
	}

	public void setStartYCoordinate (float startYCoordinate) {
		this.startYCoordinate = startYCoordinate;
	}

	public float getEndXCoordinate () {
		return endXCoordinate;
	}

	public void setEndXCoordinate (float endXCoordinate) {
		this.endXCoordinate = endXCoordinate;
	}

	public float getEndYCoordinate () {
		return endYCoordinate;
	}

	public void setEndYCoordinate (float endYCoordinate) {
		this.endYCoordinate = endYCoordinate;
	}
}
