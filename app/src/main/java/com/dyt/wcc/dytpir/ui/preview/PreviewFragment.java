package com.dyt.wcc.dytpir.ui.preview;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.FragmentPreviewMainBinding;
import com.dyt.wcc.dytpir.widget.MyToggle;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:17     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.ui.main     </p>
 */
public class PreviewFragment extends BaseFragment<FragmentPreviewMainBinding> {
	private PreViewViewModel mViewModel;

	@Override
	protected int bindingLayout () {
		return R.layout.fragment_preview_main;
	}

	@Override
	public void onResume () {
		super.onResume();
		Log.e(TAG, "onResume: ");
	}

	@Override
	public void onPause () {
		super.onPause();
		Log.e(TAG, "onPause: ");
	}

	@Override
	public void onDetach () {
		super.onDetach();
		Log.e(TAG, "onDetach: ");
	}

	@Override
	public void onDestroy () {
		super.onDestroy();
		Log.e(TAG, "onDestroy: ");
	}

	@Override
	public void onDestroyView () {
		super.onDestroyView();
		Log.e(TAG, "onDestroyView: ");
	}

	@Override
	protected void initView () {
		mDataBinding.setPf(this);
		mViewModel = new ViewModelProvider(this,new ViewModelProvider.NewInstanceFactory()).get(PreViewViewModel.class);

//		Navigation.findNavController(mDataBinding.getRoot()).setGraph(R.navigation.nav_dytmain,R.id.nav_previewFg);

		mDataBinding.toggleHighLowTemp.setWidgetStateCheckedListener(new MyToggle.OnWidgetStateCheckedListener() {
			@Override
			public void onStateChecked (boolean widgetState) {
//				Toast.makeText(mContext.get(),"toggle is click"+widgetState,Toast.LENGTH_SHORT).show();
			}
		});

		mDataBinding.toggleHighTempAlarm.setWidgetStateCheckedListener(new MyToggle.OnWidgetStateCheckedListener() {
			@Override
			public void onStateChecked (boolean widgetState) {

			}
		});
		mDataBinding.toggleShowHighLowTemp.setWidgetStateCheckedListener(new MyToggle.OnWidgetStateCheckedListener() {
			@Override
			public void onStateChecked (boolean widgetState) {

			}
		});

		mDataBinding.toggleTemp.setWidgetStateCheckedListener(new MyToggle.OnWidgetStateCheckedListener() {
			@Override
			public void onStateChecked (boolean widgetState) {

			}
		});

		mDataBinding.ivPreviewLeftPalette.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {

				View view = LayoutInflater.from(mContext.get()).inflate(R.layout.pop_palette_choice,null);
				PopupWindow popupWindow = new PopupWindow(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

				popupWindow.setFocusable(true);
				popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				popupWindow.setOutsideTouchable(true);
				popupWindow.setTouchable(true);
				Log.e(TAG, "onClick: frameLayout =  "+ mDataBinding.flPreview.getWidth()  + " height ==  " + mDataBinding.flPreview.getHeight());
				popupWindow.showAsDropDown(mDataBinding.rlLeftContainer,mDataBinding.rlLeftContainer.getWidth()  ,mDataBinding.flPreview.getHeight() - 250, Gravity.BOTTOM);
			}
		});
	}

	public void toGallery(View view){
		Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_previewFg_to_galleryFg);
	}

}
