package com.dytest.wcc.common.widget.mySpinner;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/8/24  11:48     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.wcc.dytfourbie.main.widget     </p>
 */
enum PopUpTextAlignment {
	START(0), END(1), CENTER(2);
	private final int id;

	PopUpTextAlignment (int id) {
		this.id = id;
	}

	/**
	 * 类似查询id  id如果在枚举当中返回当前值；否则返回center的值2
	 *
	 * @param id 查询的值
	 * @return 在枚举包含中返回当前值。否者返回CENTER 2
	 */
	static PopUpTextAlignment fromId (int id) {
		for (PopUpTextAlignment value : values()) {
			if (value.id == id)
				return value;
		}
		return CENTER;
	}
}
