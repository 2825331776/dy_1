package com.dyt.wcc.baselib.ui.doodle.IItem;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * <p>Copyright (C),2022/11/29 11:27-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/29 11:27     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore     </p>
 * <p>Description：
 *     BRUSH, // 画笔
 *     COPY, // 仿制
 *     ERASER, // 橡皮擦
 *     TEXT, // 文本
 *     BITMAP, // 贴图
 *     MOSAIC; // 马赛克   </p>
 */
public interface IDoodlePen {

	void config (IDoodleItem doodleItem, Paint paint);

	IDoodlePen copy();

	void drawHelpers(Canvas canvas,IDoodle iDoodle);
}
