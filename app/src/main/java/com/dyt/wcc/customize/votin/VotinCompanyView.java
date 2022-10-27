package com.dyt.wcc.customize.votin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.dyt.wcc.R;
import com.dyt.wcc.customize.CustomizeCompany;
import com.dyt.wcc.databinding.PopCompanyVictorBinding;
import com.dyt.wcc.databinding.PopCompanyVotinBinding;

import java.util.Objects;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/7/7  14:24     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.customize.victor     </p>
 */
public class VotinCompanyView extends CustomizeCompany {
	/**
	 * 设置显示的公司布局
	 *
	 * @param context 填充的context
	 * @return View 返回id
	 */
	@Override
	public View getCompanyView (Context context) {
		this.mContext = context;
		return LayoutInflater.from(context).inflate(R.layout.pop_company_votin,null);
	}

	@Override
	public void initListener (View view) {
		PopCompanyVotinBinding votinBinding = DataBindingUtil.bind(view);
		//根据是中文还是因为 区分是主营/邮箱反馈
		Objects.requireNonNull(votinBinding).tvAboutContactUsEmail.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		votinBinding.tvAboutContactUsEmail.setOnClickListener(v19 -> {

			Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
			emailIntent.setData(Uri.parse(mContext.getResources().getString(R.string.contactus_email_head) +
					mContext.getResources().getString(R.string.about_contactEmail_content_victor)));
			emailIntent.putExtra(Intent.EXTRA_SUBJECT, "反馈标题");
			emailIntent.putExtra(Intent.EXTRA_TEXT, "反馈内容");
			//没有默认的发送邮件应用
			mContext.startActivity(Intent.createChooser(emailIntent, mContext.getResources().getString(R.string.contactus_choice_email)));
			//				}
		});
	}
}
