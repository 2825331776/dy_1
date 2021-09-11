package com.dyt.wcc.dytpir.ui;

import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.ActivityMainBinding;


public class MainActivity extends BaseActivity<ActivityMainBinding> {

	@Override
	protected int bindingLayout () {
		return R.layout.activity_main;
	}

	@Override
	protected void initView () {
	}


//	@Override
//	public void onBackPressed () {

//		if (Navigation.findNavController(mDataBinding.getRoot()).getCurrentDestination().getLabel().toString().equals("nav_preview_fragment")){
//			finish();
//		}else {
//			Navigation.findNavController((Activity) mContext.get(),R.id.navigation_main).navigateUp();
//		}l

//		Toast.makeText(mContext.get(),"Activity BackPress" ,Toast.LENGTH_SHORT).show();

//	}
}