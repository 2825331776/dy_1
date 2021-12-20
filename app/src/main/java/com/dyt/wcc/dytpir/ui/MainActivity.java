package com.dyt.wcc.dytpir.ui;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.ActivityMainBinding;


/**
* <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
* <p>Author：stefan cheng    </p>
* <p>Create Date：2021/9/26  16:18 </p>
* <p>Description：@todo describe         </p>
* <p>PackagePath: com.dyt.wcc.dytpir.ui     </p>
*/
public class MainActivity extends BaseActivity<ActivityMainBinding> {

	@Override
	protected int bindingLayout () {
		return R.layout.activity_main;
	}

	@Override
	protected void initView () {
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.e(TAG, "======= MainActivity Result: ====" + resultCode + "  " + requestCode   );
//		MediaProjectionHelper.getInstance().createVirtualDisplay(requestCode,resultCode,data,true,true);
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