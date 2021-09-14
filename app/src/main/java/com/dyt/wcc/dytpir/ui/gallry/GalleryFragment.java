package com.dyt.wcc.dytpir.ui.gallry;

import android.view.View;

import androidx.navigation.Navigation;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.FragmentGalleryMainBinding;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:15     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.ui.gallry     </p>
 */
public class GalleryFragment extends BaseFragment <FragmentGalleryMainBinding> {
	@Override
	protected int bindingLayout () {
		return R.layout.fragment_gallery_main;
	}

	@Override
	protected void initView () {
		mDataBinding.setFgGallery(this);
		mDataBinding.includeGallery.ivBackGallery.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				Navigation.findNavController(mDataBinding.getRoot()).navigateUp();
			}
		});
	}
}
