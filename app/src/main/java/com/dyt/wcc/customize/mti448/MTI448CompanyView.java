package com.dyt.wcc.customize.mti448;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.dyt.wcc.R;
import com.dyt.wcc.customize.CustomizeCompany;
import com.dyt.wcc.databinding.PopCompanyMti448Binding;

/**
 * <p>Copyright (C),2022/12/7 13:51-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/12/7 13:51     </p>
 * <p>PackagePath: com.dyt.wcc.customize     </p>
 * <p>Description：       </p>
 */
public class MTI448CompanyView extends CustomizeCompany {

	@Override
	public View getCompanyView (Context context) {
		this.mContext = context;
		return LayoutInflater.from(context).inflate(R.layout.pop_company_mti448, null);
	}

	@Override
	public void initListener (View view) {
		PopCompanyMti448Binding mti448Binding = DataBindingUtil.bind(view);
		//根据是中文还是因为 区分是主营/邮箱反馈
//		mti448Binding.tvAboutContactUsEmailMailseey.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
//		mti448Binding.tvAboutContactUsEmailMailseey.setOnClickListener(v19 -> {

//			Configuration configuration = mContext.getResources().getConfiguration();
//			Locale currentLocal;
//			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
//				//				Log.i("TAG", "initListener: ===tv_contact us email phone ===SDK Version ===>" + Build.VERSION.SDK_INT);
//				//				Log.i("TAG", "initListener: ===current language ===> " + configuration.getLocales().get(0).getLanguage());
//				currentLocal = configuration.getLocales().get(0);
//			} else {
//				currentLocal = configuration.locale;
//			}

//			if (currentLocal.getLanguage().toLowerCase().equals("zh")) {
//				//				Log.i("TAG", "initListener: =====equals  ==zh ==");
//				Intent intent = new Intent(Intent.ACTION_DIAL);
//				intent.setData(Uri.parse("tel:0755–86329055"));
//				mContext.startActivity(intent);
//			} else {
//				//				Log.i("TAG", "initListener: =====equals  ==other ==");
//				Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
//				emailIntent.setData(Uri.parse(mContext.getResources().getString(R.string.contactus_email_head) + mContext.getResources().getString(R.string.about_contactEmail_content_mileseey)));
//				emailIntent.putExtra(Intent.EXTRA_SUBJECT, "反馈标题");
//				emailIntent.putExtra(Intent.EXTRA_TEXT, "反馈内容");
//				//							没有默认的发送邮件应用
//				mContext.startActivity(Intent.createChooser(emailIntent, mContext.getResources().getString(R.string.contactus_choice_email)));
//			}
//		});
	}
}
