package com.dyt.wcc.baselib.ui.doodle.IItem;

import android.graphics.Paint;

/**
 * <p>Copyright (C),2022/11/29 11:33-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/29 11:33     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore     </p>
 * <p>Description：       </p>
 */
public interface IDoodleColor {

	void config (IDoodleItem doodleItem, Paint paint);

	IDoodleColor copy();

}
