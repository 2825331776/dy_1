package com.dyt.wcc.customize.jms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.dyt.wcc.R;
import com.dyt.wcc.constans.DYConstants;
import com.dyt.wcc.customize.CustomizeCompany;
import com.dyt.wcc.databinding.PopCompanyJmsBinding;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/7/7  14:23     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.customize.jms     </p>
 */
public class JMSCompanyView extends CustomizeCompany {
	/**
	 * 设置显示的公司布局
	 *
	 * @param context 填充的context
	 * @return View 返回id
	 */
	@Override
	public View getCompanyView (Context context) {
		this.mContext = context;
		return LayoutInflater.from(context).inflate(R.layout.pop_company_jms,null);
	}

	@Override
	public void initListener (View view) {
		PopCompanyJmsBinding binding = DataBindingUtil.bind(view);
		SharedPreferences sp = mContext.getSharedPreferences(DYConstants.SP_NAME,Context.MODE_PRIVATE);
//		binding.tvAboutMainBusinessContent.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		binding.tvAboutMainBusinessContent.setOnClickListener(v19 -> {
			if (sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, -1) == 0) {//中文
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				Uri content_url = Uri.parse("https://jingmingshu.tmall.com/");
				intent.setData(content_url);
				mContext.startActivity(intent);
			} else if (sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, -1) == 1) {
				Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
				emailIntent.setData(Uri.parse(mContext.getResources().getString(R.string.contactus_email_head) + mContext.getResources().getString(R.string.about_contactEmail_jms)));
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, "反馈标题");
				emailIntent.putExtra(Intent.EXTRA_TEXT, "反馈内容");
				//没有默认的发送邮件应用
				mContext.startActivity(Intent.createChooser(emailIntent, mContext.getResources().getString(R.string.contactus_choice_email)));
			}
		});
	}

}
