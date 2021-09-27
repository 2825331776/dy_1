package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

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
public class MyDragContainer extends RelativeLayout {
	private static final boolean isDebug = true;
	private static final String TAG = "MyDragContainer";
	private boolean isControlItem = false;//是否是操作子View

	private List<MyMoveWidget> viewLists;
	private Bitmap pointBt, maxTempBt, minTempBt;//三张图片 ：单点温度、线和矩阵的 最高、最低温的图片

	private int screenWidth , screenHeight;

	private int drawTempMode = -1;

	private MyDefineView addDefineView ;

	//设置点温度 最高 最低温度

	protected void setBitMap(Bitmap point,Bitmap max , Bitmap min){
		pointBt = point;maxTempBt = max;minTempBt = min;
		invalidate();
	}

	public MyDragContainer (Context context) {
		this(context,null);
	}
	public MyDragContainer (Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}
	public MyDragContainer (Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr,0);
	}
	public MyDragContainer (Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		initAttrs();
		initView();
	}
	private void initAttrs(){

	}
	private void initView(){
		viewLists = new ArrayList<>();
		addDefineView = new MyDefineView();
		addDefineView.setDrawType(drawTempMode);
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
		if (isDebug) Log.e(TAG, "onInterceptTouchEvent: ");
		if(isControlItem){//判断ev在不在子item 边界内
			return super.onInterceptTouchEvent(ev);
		}
		return true;


	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {//消费的具体
		//down 0  up 1 move 2
		if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: ==>" + event.getAction());
		switch (event.getAction()){
			case MotionEvent.ACTION_DOWN:
				if (drawTempMode != -1) {
					if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: " + "Down ==  set start point " );
					//todo 绘制模式
					addDefineView.setStartPointX(((int) event.getX()));
					addDefineView.setStartPointY(((int) event.getY()));
					addDefineView.setDrawType(drawTempMode);

				}
				break;
			case MotionEvent.ACTION_MOVE:
				 if (drawTempMode == 2 || drawTempMode ==3) {
					 addDefineView.setEndPointX(((int) event.getX()));
					 addDefineView.setEndPointY(((int) event.getY()));
				 }else if (drawTempMode ==1){
					 if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: Move = " + drawTempMode);
				 }
				break;
			case MotionEvent.ACTION_UP:
				if (drawTempMode == 2 || drawTempMode ==3){
					if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: up = " + drawTempMode);

					addDefineView.setEndPointX(((int) event.getX()));
					addDefineView.setEndPointY(((int) event.getY()));
					drawTempMode = -1;
					return true;
				}else if (drawTempMode ==1) {
					//todo 添加点视图
					if (isDebug)Log.e(TAG, "MyDragContainer.onTouchEvent: up = " + drawTempMode);
				}
				break;
		}
		return true;
	}

	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			return false;
		}
	}) ;
}
