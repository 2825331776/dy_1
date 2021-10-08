package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.dyt.wcc.common.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/27  10:52     </p>
 * <p>Description：包含所有的图片        </p>
 * <p>PackagePath: com.dyt.wcc.common.widget     </p>
 *
 * 有焦点的时候 哪个子VIew获得了，  否则都没有焦点  带焦点的控件有工具栏
 */
public class DragTempContainer extends RelativeLayout {
	private static final boolean isDebug = true;
	private static final String TAG = "MyDragContainer";
	private boolean isControlItem = false;//是否是操作子View

	private List<MyMoveWidget> viewLists;
	private Bitmap pointBt, maxTempBt, minTempBt;//三张图片 ：单点温度、线和矩阵的 最高、最低温的图片

	private int screenWidth , screenHeight;

	private int           drawTempMode = -1;
	private WeakReference<Context> mContext;

	private AddTempWidget addDefineView ;

	private float [] tempSource;


	private boolean enabled = true;//设置是否可用,为false时不能添加view 不能进行任何操作

	//设置点温度 最高 最低温度

	protected void setBitMap(Bitmap point,Bitmap max , Bitmap min){
		pointBt = point;maxTempBt = max;minTempBt = min;
		invalidate();
	}
	//绘制控件拿到温度数据
	public void upDateTemp (float[] date){
		tempSource = date;
	}

	public DragTempContainer (Context context) {
		this(context,null);
	}
	public DragTempContainer (Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}
	public DragTempContainer (Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr,0);
	}
	public DragTempContainer (Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		mContext = new WeakReference<>(context) ;
		initAttrs();
		initView();
	}
	private void initAttrs(){

	}
	private void initView(){
		viewLists = new ArrayList<>();
		addDefineView = new AddTempWidget();
		addDefineView.setType(drawTempMode);

		tempSource = new float[256*196+10];
	}

	//对象方法 去增加一个 点 线 矩阵视图
	protected void addDrawView(MyMoveWidget moveWidget , int type){
		addView(moveWidget);
		viewLists.add(moveWidget) ;
		invalidate();
	}

	protected void removeDrawView(int id){
		for (int i = 0 ; i < viewLists.size(); i++){
			if (id == viewLists.get(i).getView().getId()){
			removeView(viewLists.get(i));
			viewLists.remove(viewLists.get(i));
			invalidate();
			}
		}
	}

	protected void removeAllItemView(){
		removeAllViews();
		viewLists.clear();
		invalidate();
	}
	//查询 分发事件

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);

		screenWidth = getWidth();
		screenHeight = getHeight();

	}

	public int getDrawTempMode () {
		return drawTempMode;
	}
	public void setDrawTempMode (int drawTempMode) {
		this.drawTempMode = drawTempMode;
	}

	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		if (!enabled){
			return true;
		}
		//判断点是否在子View中
		if (drawTempMode == -1){//不分发事件
			//判断是否要  移动已有的子View（判断焦点在哪个View）  否则直接消费
			//或者是删除子View ||  或者移动  缩放VIew
			if (isDebug)Log.e(TAG, "dispatchTouchEvent: 拦截事件");
			return true;
		}else {//分发事件   绝对是要新增View
			if (isDebug)Log.e(TAG, "dispatchTouchEvent: 分发事件");
			return super.dispatchTouchEvent(ev);
		}
	}

	@Override
	public boolean onInterceptTouchEvent (MotionEvent ev) {
		if (isDebug) Log.e(TAG, "onInterceptTouchEvent: "+ ev.getAction());

		switch (ev.getAction()){
			case MotionEvent.ACTION_DOWN:
				if (drawTempMode == 1) {
					if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: " + "Down ==  set start point " );
					//					Toast.makeText(mContext.get(),"XY " + event.getX() + "  Y " + event.getY(),Toast.LENGTH_SHORT).show();
					//todo 绘制模式
					PointTempWidget pointTempWidget = new PointTempWidget();


					pointTempWidget.setStartPointX(((int) ev.getX()));
					pointTempWidget.setStartPointY(((int) ev.getY()));
					pointTempWidget.setTemp(111.0f);

					addDefineView.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete,R.mipmap.define_view_tools_other});
					addDefineView.setType(drawTempMode);
					addDefineView.setTextSuffix("℃");
					addDefineView.setTempTextSize(15);
					addDefineView.setCanMove(true);
					addDefineView.setSelect(true);


					addDefineView.setPointTemp(pointTempWidget);
				}else if (drawTempMode ==2){

					OtherTempWidget otherTempWidget = new OtherTempWidget();

					otherTempWidget.setStartPointX(((int) ev.getX()));
					otherTempWidget.setStartPointY((int) ev.getY());
					otherTempWidget.setEndPointY(((int) ev.getY()));
					otherTempWidget.setMaxTemp(100.0f);
					otherTempWidget.setMinTemp(15.0f);
					otherTempWidget.setMinTempX((int) (ev.getX()+10));
					otherTempWidget.setMinTempY((int) ev.getY());


					addDefineView.setOtherTemp(otherTempWidget);


					addDefineView.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete,R.mipmap.define_view_tools_other});
					addDefineView.setType(drawTempMode);
					addDefineView.setTextSuffix("℃");
					addDefineView.setTempTextSize(15);
					addDefineView.setCanMove(true);
					addDefineView.setSelect(true);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (drawTempMode == 2 || drawTempMode ==3) {
//					addDefineView.getOtherTemp().setEndPointX(((int) ev.getX()));
//					addDefineView.getOtherTemp().setEndPointY(((int) ev.getY()));
				}else if (drawTempMode ==1){
					if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: Move = " + drawTempMode);
				}
				break;
			case MotionEvent.ACTION_UP:
				if (drawTempMode == 2){
					if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: up = " + drawTempMode +" x = " + ev.getX());

					//todo 判定谁是起始点  未做
//					Math.min(addDefineView.getOtherTemp().getStartPointX(),ev.getX());
//					Math.max(addDefineView.getOtherTemp().getStartPointX(),ev.getX());
//					Math.min(addDefineView.getOtherTemp().getStartPointX(),ev.getY());
//					Math.max(addDefineView.getOtherTemp().getStartPointX(),ev.getX());
					addDefineView.getOtherTemp().setStartPointX(Math.min(addDefineView.getOtherTemp().getStartPointX(),(int)ev.getX()));
					addDefineView.getOtherTemp().setEndPointX(Math.max(addDefineView.getOtherTemp().getStartPointX(),(int)ev.getX()));

					Log.e(TAG, "drawTempMode == 2: "+ addDefineView.getOtherTemp().getStartPointX() + "  endX " + addDefineView.getOtherTemp().getEndPointX());
					addDefineView.getOtherTemp().setMaxTempX(addDefineView.getOtherTemp().getEndPointX()-10);
					addDefineView.getOtherTemp().setMaxTempY(addDefineView.getOtherTemp().getStartPointY());
//					drawTempMode = -1;

					MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),addDefineView,screenWidth,screenHeight);
					moveWidget.setClickable(true);
					LinearLayout.LayoutParams  layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					moveWidget.setLayoutParams(layoutParams);
					//					moveWidget.setClickable(true);

					moveWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
					addView(moveWidget);
					viewLists.add(moveWidget);
					invalidate();

				}else if (drawTempMode ==1) {
					//todo 添加点视图
					MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),addDefineView,screenWidth,screenHeight);
					moveWidget.setClickable(true);
					LinearLayout.LayoutParams  layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					moveWidget.setLayoutParams(layoutParams);
//					moveWidget.setClickable(true);

					moveWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
					addView(moveWidget);
					viewLists.add(moveWidget);
					invalidate();
					if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: up = " + drawTempMode);
				}else if (drawTempMode ==3){
					//todo 绘制矩形
				}
				break;
		}
		Log.e(TAG, "onInterceptTouchEvent:  continue =====");
//		if(isControlItem){//判断ev在不在子item 边界内
//			return false;
//		}else {
//
//		}
		return super.onInterceptTouchEvent(ev);


	}


	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			return false;
		}
	}) ;
}
