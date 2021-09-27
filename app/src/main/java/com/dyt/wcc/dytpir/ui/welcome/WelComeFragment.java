package com.dyt.wcc.dytpir.ui.welcome;

import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.FragmentWelcomeMainBinding;


/**
* <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
* <p>Author：stefan cheng    </p>
* <p>Create Date：2021/9/26  16:18 </p>
* <p>Description：@todo describe         </p>
* <p>PackagePath: com.dyt.wcc.dytpir.ui.welcome     </p>
*/
public class WelComeFragment extends BaseFragment<FragmentWelcomeMainBinding> {

	@Override
	protected int bindingLayout () {
		return R.layout.fragment_welcome_main;
	}

	@Override
	protected boolean isInterceptBackPress () {
		return false;
	}

	@Override
	protected void initView () {
		mDataBinding.setWelcomeImg(R.mipmap.ic_welcome);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run () {
				NavHostFragment.findNavController(WelComeFragment.this).navigate(R.id.action_welcomeFg_to_previewFg);
//				NavOptions options = new NavOptions.Builder().build();
//				Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_welcomeFg_to_previewFg);
//				Navigation.findNavController(mDataBinding.getRoot()).setGraph(R.navigation.nav_dytmain);
//				Navigation.findNavController(mDataBinding.getRoot()).navigateUp();
			}
		},500);
	}

	@Override
	public void onCreate (@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}
}
