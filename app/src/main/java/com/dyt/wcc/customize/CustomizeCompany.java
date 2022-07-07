package com.dyt.wcc.customize;

import android.content.Context;
import android.view.View;
import android.widget.PopupWindow;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/7/7  14:16     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.customize     </p>
 */
public abstract class CustomizeCompany {
	protected Context mContext;
	protected PopupWindow popupWindow;
	/**
	 * 设置显示的公司布局
	 * @param context 填充的context
	 * @return View 返回id
	 */
	public abstract View getCompanyView (Context context);

	public abstract void initListener (View view);

}
