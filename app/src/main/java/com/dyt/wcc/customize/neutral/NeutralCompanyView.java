package com.dyt.wcc.customize.neutral;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.dyt.wcc.R;
import com.dyt.wcc.customize.CustomizeCompany;
import com.dyt.wcc.databinding.PopCompanyTeslongBinding;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/7/7  14:20     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.customize.dyt     </p>
 */
public class NeutralCompanyView extends CustomizeCompany {
	/**
	 * 设置显示的公司布局
	 *
	 * @param context 填充的context
	 * @return View 返回id
	 */
	@Override
	public View getCompanyView (Context context) {
		this.mContext = context;
		return LayoutInflater.from(context).inflate(R.layout.pop_company_neutral, null);
	}

	@Override
	public void initListener (View view) {
	}
}
