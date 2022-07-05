package com.dyt.wcc.common.widget.dragView;

import androidx.annotation.NonNull;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/6/28  16:27     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.widget.dragView     </p>
 */
public class PointEntity {
	public static final int TEMP_ENTITY_POINT      = 1;
	public static final int TEMP_ENTITY_LINE       = 2;
	public static final int TEMP_ENTITY_REC        = 3;
	//point type
	public static final int TEMP_POINT_TYPE_HIGH   = 0;
	public static final int TEMP_POINT_TYPE_lOW    = 1;
	public static final int TEMP_POINT_TYPE_CENTER = 2;
	public static final int TEMP_POINT_TYPE_NORMAL = 3;

	//text direction
	public static final int TEMP_POINT_TEXT_DIRECTION_TOP          = 0;
	public static final int TEMP_POINT_TEXT_DIRECTION_RIGHT_TOP    = 1;
	public static final int TEMP_POINT_TEXT_DIRECTION_RIGHT        = 2;
	public static final int TEMP_POINT_TEXT_DIRECTION_RIGHT_BOTTOM = 3;
	public static final int TEMP_POINT_TEXT_DIRECTION_BOTTOM       = 4;
	public static final int TEMP_POINT_TEXT_DIRECTION_LEFT_BOTTOM  = 5;
	public static final int TEMP_POINT_TEXT_DIRECTION_LEFT         = 6;
	public static final int TEMP_POINT_TEXT_DIRECTION_LEFT_TOP     = 7;

	private float x;
	private float y;
	private float temp;//单纯的数值，如果需要带上单位，在最外层父控件去更改。
	private int   tempStrDirection = TEMP_POINT_TEXT_DIRECTION_RIGHT_BOTTOM;//文字的方位，后续计算才能得到，有个默认值
	private int   pointType        = TEMP_POINT_TYPE_NORMAL;//高温、低温、中心点温度、常规测温

	private PointEntity (@NonNull Builder builder) {
		this.x = builder.x;
		this.y = builder.y;
		this.temp = builder.temp;
		this.pointType = builder.pointType;
	}

	public float getX () {
		return x;
	}

	public void setX (float x) {
		this.x = x;
	}

	public float getY () {
		return y;
	}

	public void setY (float y) {
		this.y = y;
	}

	public float getTemp () {
		return temp;
	}

	public void setTemp (float temp) {
		this.temp = temp;
	}

	public int getPointType () {
		return pointType;
	}

	public void setPointType (int pointType) {
		this.pointType = pointType;
	}

	public int getTempStrDirection () {
		return tempStrDirection;
	}

	public void setTempStrDirection (int tempStrDirection) {
		this.tempStrDirection = tempStrDirection;
	}

	public static final class Builder {
		private float x;
		private float y;
		private float temp;//单纯的数值，如果需要带上单位，在最外层父控件去更改。
		private int   pointType;//高温、低温、中心点温度、常规测温

		public Builder setX (float x) {
			this.x = x;
			return this;
		}

		public Builder setY (float y) {
			this.y = y;
			return this;
		}

		public Builder setTemp (float temp) {
			this.temp = temp;
			return this;
		}

		public Builder setPointType (int pointType) {
			this.pointType = pointType;
			return this;
		}

		public PointEntity build () {
			return new PointEntity(this);
		}
	}
}
