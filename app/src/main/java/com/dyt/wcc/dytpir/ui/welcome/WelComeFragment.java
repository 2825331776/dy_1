package com.dyt.wcc.dytpir.ui.welcome;

import android.os.Handler;

import androidx.navigation.Navigation;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.FragmentWelcomeMainBinding;


/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:16     </p>
 * <p>Description：splash页面        </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.ui.welcome     </p>
 */
public class WelComeFragment extends BaseFragment<FragmentWelcomeMainBinding> {

	@Override
	protected int bindingLayout () {
		return R.layout.fragment_welcome_main;
	}

	@Override
	protected void initView () {
		mDataBinding.setWelcomeImg(R.mipmap.welcome);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run () {
				Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_welcomeFg_to_previewFg);
			}
		},500);
	}
}
