package com.dyt.wcc.dytpir.ui;

import android.view.View;

import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.ActivityTestBinding;

public class TestActivity extends BaseActivity<ActivityTestBinding> implements View.OnClickListener {
	private int drawMode = -1;


	@Override
	protected int bindingLayout () {
		return R.layout.activity_test;
	}

	@Override
	protected void initView () {
		mDataBinding.btPoint.setOnClickListener(this::onClick);
		mDataBinding.btLine.setOnClickListener(this::onClick);
		mDataBinding.btRectangle.setOnClickListener(this);
		mDataBinding.btReset.setOnClickListener(this);
	}

	@Override
	public void onClick (View v) {
		switch (v.getId()){
			case R.id.bt_point:
				drawMode = 1;
				mDataBinding.myDragContainer.setDrawTempMode(drawMode);
				break;
			case R.id.bt_line:
				drawMode = 2;
				mDataBinding.myDragContainer.setDrawTempMode(drawMode);
				break;
			case R.id.bt_rectangle:
				drawMode = 3;
				mDataBinding.myDragContainer.setDrawTempMode(drawMode);
				break;
			case R.id.bt_reset:
				drawMode = -1;
				mDataBinding.myDragContainer.setDrawTempMode(drawMode);
				break;
		}
	}




}