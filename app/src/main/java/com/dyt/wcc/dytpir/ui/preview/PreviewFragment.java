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

//		mDataBinding.getRoot().setOnKeyListener(new View.OnKeyListener() {
//			@Override
//			public boolean onKey (View v, int keyCode, KeyEvent event) {
//				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK){
//					Toast.makeText(mContext.get(),"back press",Toast.LENGTH_SHORT).show();
//					return true;
//				}
//				return false;
//			}
//		});

//		Navigation.findNavController(mDataBinding.getRoot()).setGraph(R.navigation.nav_dytmain,R.id.nav_previewFg);
	}

	public void toGallery(View view){
		Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_previewFg_to_galleryFg);
	}

}
