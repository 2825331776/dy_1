package com.dyt.wcc.dytpir.ui.lookfile;

import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.FragmentLookfileMainBinding;

import java.util.ArrayList;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  17:53     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.ui.lookfile     </p>
 */
public class LookFileFragment extends BaseFragment<FragmentLookfileMainBinding> {
	private ArrayList<String > imageList ;
	@Override
	protected int bindingLayout () {
		return R.layout.fragment_lookfile_main;
	}

	@Override
	protected void initView () {
		imageList = new ArrayList<>();
		Bundle receiverArgs = getArguments();
		assert receiverArgs != null;
		imageList.addAll(receiverArgs.getStringArrayList("pathList"));
		Glide.with(mContext.get()).load(imageList.get(receiverArgs.getInt("position"))).into(mDataBinding.ivCheckPhoto);
	}
}
