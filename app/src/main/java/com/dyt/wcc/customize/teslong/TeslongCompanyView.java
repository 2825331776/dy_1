package com.dyt.wcc.customize.teslong;

import android.content.Context;
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
public class TeslongCompanyView extends CustomizeCompany {
	/**
	 * 设置显示的公司布局
	 *
	 * @param context 填充的context
	 * @return View 返回id
	 */
	@Override
	public View getCompanyView (Context context) {
		this.mContext = context;
		return LayoutInflater.from(context).inflate(R.layout.pop_company_teslong, null);
	}

	@Override
	public void initListener (View view) {
		PopCompanyTeslongBinding teslongBinding = DataBindingUtil.bind(view);
		//根据是中文还是因为 区分是主营/邮箱反馈
//		teslongBinding.tvAboutContactUsEmail.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
//		teslongBinding.tvAboutContactUsEmail.setOnClickListener(v19 -> {
//			Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
//			emailIntent.setData(Uri.parse(mContext.getResources().getString(R.string.contactus_email_head) + mContext.getResources().getString(R.string.preview_about_contact_email_content)));
//			//没有默认的发送邮件应用
//			mContext.startActivity(Intent.createChooser(emailIntent, mContext.getResources().getString(R.string.contactus_choice_email)));
//		});
	}
}
