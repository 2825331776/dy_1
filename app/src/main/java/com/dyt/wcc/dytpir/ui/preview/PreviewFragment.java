package com.dyt.wcc.dytpir.ui.preview;

import android.view.View;

import androidx.navigation.Navigation;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.FragmentPreviewMainBinding;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:17     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.ui.main     </p>
 */
public class PreviewFragment extends BaseFragment<FragmentPreviewMainBinding> {
	@Override
	protected int bindingLayout () {
		return R.layout.fragment_preview_main;
	}


	@Override
	protected void initView () {
		mDataBinding.setPf(this);


	}

	public void toGallery(View view){
		Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_previewFg_to_galleryFg);
	}


}
