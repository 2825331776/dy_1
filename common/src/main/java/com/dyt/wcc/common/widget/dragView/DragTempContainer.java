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
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

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
	public static int padTop = 14;
	//点击工具栏之后的控制 响应的事件。删除的事件。
	public static int perToolsWidthHeightSet = 3 * padTop;//每个工具栏的宽高
	public static int perToolsMargin = 5;//每个工具栏的margin
	//工具栏


	private static final boolean isDebug = true;
	private static final String TAG = "MyDragContainer";
	private boolean isControlItem = false;//是否是操作子View

	private List<MyMoveWidget> viewLists;
	private Bitmap pointBt, maxTempBt, minTempBt;//三张图片 ：单点温度、线和矩阵的 最高、最低温的图片

	private int screenWidth , screenHeight;

	private int           drawTempMode = -1;
	private WeakReference<Context> mContext;

//	private AddTempWidget addDefineView ;

	private float [] tempSource;

	private int pointNum = 0;
	private int lineNum = 0;
	private int recNum = 0;

	private int startPressX, startPressY, endPressX, endPressY;

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
		tempSource = new float[256*196+10];
	}

	//对象方法 去增加一个 点 线 矩阵视图 , int type
	protected void addDrawView(MyMoveWidget moveWidget){
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
		if (drawTempMode == 0){//不分发事件
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
//				addDefineView = new AddTempWidget();
				if (drawTempMode != -1){
					startPressX = (int) ev.getX();
					startPressY = (int) ev.getY();
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (drawTempMode == 2 || drawTempMode ==3) {
					endPressX = (int) ev.getX();
					endPressY = (int) ev.getY();
				}
				//todo 移动可以移动的控件， 或实时缩放
				break;
			case MotionEvent.ACTION_UP:
				endPressX = (int) ev.getX();
				endPressY = (int) ev.getY();

				if (drawTempMode == 2 || drawTempMode ==3){
					AddTempWidget tempWidget = new AddTempWidget();

					OtherTempWidget otherTempWidget = new OtherTempWidget();
					otherTempWidget.setStartPointX(Math.min(startPressX,endPressX));
					otherTempWidget.setStartPointY(Math.min(startPressY,endPressY));
					otherTempWidget.setEndPointX(Math.max(startPressX,endPressX));
					if (drawTempMode ==2){
						otherTempWidget.setEndPointY(Math.min(startPressY,endPressY));
					}else {
						otherTempWidget.setEndPointY(Math.max(startPressY,endPressY));
					}
					otherTempWidget.setMinTempX(otherTempWidget.getStartPointX()+20);
					otherTempWidget.setMinTempY(otherTempWidget.getStartPointY());
					otherTempWidget.setMinTemp(50);
					otherTempWidget.setMaxTempX(otherTempWidget.getEndPointX()-20);
					otherTempWidget.setMaxTempY(otherTempWidget.getEndPointY());
					otherTempWidget.setMaxTemp(131);

					tempWidget.setType(drawTempMode);
					tempWidget.setCanMove(true);
					tempWidget.setSelect(true);
					tempWidget.setTempTextSize(16);
					tempWidget.setTextSuffix("℃");
					tempWidget.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete, R.mipmap.define_view_tools_other});
					tempWidget.setOtherTemp(otherTempWidget);


					MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),tempWidget,screenWidth,screenHeight);
					ConstraintLayout.LayoutParams  layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					layoutParams.setMargins(tempWidget.getOtherTemp().getStartPointX(),tempWidget.getOtherTemp().getStartPointY(),0,0);
					moveWidget.setLayoutParams(layoutParams);
					moveWidget.setAlpha(0.4f);
					moveWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
					addView(moveWidget);
					viewLists.add(moveWidget);
				}else if (drawTempMode ==1) {
					AddTempWidget widget = new AddTempWidget();

					PointTempWidget pointTempWidget = new PointTempWidget();
					pointTempWidget.setStartPointX(startPressX);
					pointTempWidget.setStartPointY(startPressY);
					pointTempWidget.setTemp(100);

					widget.setType(drawTempMode);
					widget.setCanMove(true);
					widget.setSelect(true);
					widget.setTempTextSize(20);
					widget.setTextSuffix("℃");
					widget.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete, R.mipmap.define_view_tools_other});
					widget.setPointTemp(pointTempWidget);

					MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),widget,screenWidth,screenHeight);
					ConstraintLayout.LayoutParams  layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					layoutParams.setMargins(widget.getPointTemp().getStartPointX(),widget.getPointTemp().getStartPointY(),0,0);
					moveWidget.setLayoutParams(layoutParams);
					moveWidget.setAlpha(0.4f);
					moveWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_right));
					addView(moveWidget);
					viewLists.add(moveWidget);
				}
				break;
		}
		return super.onInterceptTouchEvent(ev);
	}


	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			return false;
		}
	}) ;
}
