package com.dyt.wcc.baselib.ui.doodle.IItem;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * <p>Copyright (C),2022/11/29 11:27-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/29 11:27     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore     </p>
 * <p>Description：      HAND_WRITE, // 手绘
 *     ARROW, // 箭头
 *     LINE, // 直线
 *     FILL_CIRCLE, // 实心圆
 *     HOLLOW_CIRCLE, // 空心圆
 *     FILL_RECT, // 实心矩形
 *     HOLLOW_RECT; // 空心矩形     </p>
 */
public interface IDoodleShape {

	void config (IDoodleItem doodleItem, Paint paint);

	IDoodleShape copy();

	void drawHelpers(Canvas canvas,IDoodle iDoodle);
}
