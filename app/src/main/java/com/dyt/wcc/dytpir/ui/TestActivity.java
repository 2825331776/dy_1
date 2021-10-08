package com.dyt.wcc.dytpir.ui;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.common.widget.dragView.AddTempWidget;
import com.dyt.wcc.common.widget.dragView.MyMoveWidget;
import com.dyt.wcc.common.widget.dragView.PointTempWidget;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.ActivityTestBinding;

public class TestActivity extends BaseActivity<ActivityTestBinding> implements View.OnClickListener {
	private int drawMode = -1;
	MyMoveWidget moveWidget ;

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

		DisplayMetrics metrics = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
	}

	@Override
	public void onClick (View v) {
		switch (v.getId()){
			case R.id.bt_point:
				drawMode = 1;
				mDataBinding.myDragContainer.setDrawTempMode(drawMode);
				mDataBinding.myDragContainer.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
				break;
			case R.id.bt_line:
				drawMode = 2;
				mDataBinding.myDragContainer.setDrawTempMode(drawMode);
				break;
			case R.id.bt_rectangle:
				drawMode = 3;
				mDataBinding.myDragContainer.setDrawTempMode(drawMode);
				moveWidget.setSelectedState(!moveWidget.getView().isSelect());

				break;
			case R.id.bt_reset:
//				drawMode = -1;
//				mDataBinding.myDragContainer.setDrawTempMode(drawMode);
				Log.e(TAG, "onClick: bt_reset");

				RelativeLayout.LayoutParams  layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				AddTempWidget view = new AddTempWidget();
				PointTempWidget pointTempWidget = new PointTempWidget();

				pointTempWidget.setStartPointY(100);
				pointTempWidget.setStartPointX(200);
				pointTempWidget.setTemp(1234);
				view.setCanMove(true);
				view.setType(1);
				view.setSelect(true);
				view.setTempTextSize(20);
				view.setTextSuffix("℃");
				view.setPointTemp(pointTempWidget);
				view.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete, R.mipmap.define_view_tools_other});

				moveWidget = new MyMoveWidget(mContext.get(),view,mDataBinding.myDragContainer.getWidth(),mDataBinding.myDragContainer.getHeight());
				moveWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
				moveWidget.setLayoutParams(layoutParams);

				mDataBinding.myDragContainer.addView(moveWidget);

				break;
		}
	}




}