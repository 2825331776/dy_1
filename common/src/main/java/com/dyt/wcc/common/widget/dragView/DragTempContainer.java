package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

	private static final int UPDATE_TEMP_DATA = 1;

	private static final boolean isDebug = true;
	private static final String TAG = "MyDragContainer";
	private boolean isControlItem = false;//是否是操作子View

	private List<MyMoveWidget> viewLists;
	private Bitmap pointBt, maxTempBt, minTempBt;//三张图片 ：单点温度、线和矩阵的 最高、最低温的图片

	private float WRatio, HRatio;//相机数据大小  与 显示屏幕大小的比值。


	private int           drawTempMode = -1;
	private WeakReference<Context> mContext;

	private float [] tempSource;

	private int pointNum = 0;
	private int lineNum = 0;
	private int recNum = 0;

	private int screenWidth , screenHeight;
	private int startPressX, startPressY, endPressX, endPressY;
	private int addWidgetMarginX, addWidgetMarginY;
	private int minAddWidgetWidth, minAddWidgetHeight;//添加控件的最小宽高  约束添加的线和矩形

	private boolean enabled = true;//设置是否可用,为false时不能添加view 不能进行任何操作

	//设置点温度 最高 最低温度

	protected void setBitMap(Bitmap point,Bitmap max , Bitmap min){
		pointBt = point;maxTempBt = max;minTempBt = min;
		invalidate();
	}
	//实时刷新本地的温度数据
	public void upDateTemp (float[] date){
		Message message = Message.obtain();
		message.what = UPDATE_TEMP_DATA;
		message.obj = date;
		handler.sendMessage(message);
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
		minAddWidgetWidth = minAddWidgetHeight = 100;


		pointBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
		minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorblue);
		maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorred);
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


	}

	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		screenWidth = getWidth();
		screenHeight = getHeight();

		WRatio = 256 / (float)screenWidth;
		HRatio = 192 / (float) screenHeight;

		MyMoveWidget child ;
		for (int index = 0; index < getChildCount(); index++){
			child = (MyMoveWidget) getChildAt(index);
			child.layout(child.getXOffset(),child.getYOffset(),child.getXOffset()+child.getWMeasureSpecSize(),child.getYOffset()+child.getHMeasureSpecSize());
		}
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	}

	public int getDrawTempMode () {
		return drawTempMode;
	}
	public void setDrawTempMode (int drawTempMode) {
		this.drawTempMode = drawTempMode;
	}
//	public void setHWRation(float wRatio , float hRatio){
//		this.WRatio = wRatio;
//		this.HRatio = hRatio;
//	}

	private float getXByWRatio(float x){
		return x/WRatio;
	}
	private float getYByHRatio(float y){
		return y/HRatio;
	}

	//根据每帧数据源去更新数据，刷新子View
	private void updateChildViewData(){

	}
	//校正每次添加的控件的宽高是否符合规范,大于最小值，否则无效添加
	private boolean reviseCoordinate(int type){
		int min ,max ;
		min = Math.min(startPressX,endPressX);
		max = Math.max(startPressX,endPressX);
		startPressX = min;
		endPressX = max;
		if (endPressX - startPressX < minAddWidgetWidth){
			return endPressX - startPressX < minAddWidgetWidth;
		}

		if (type ==2){
			endPressY = startPressY;
		}else {//type =3;
			min = Math.min(startPressY,endPressY);
			max = Math.max(startPressY,endPressY);
			startPressY = min;
			endPressY = max;
			return endPressY - startPressY >= minAddWidgetHeight;
		}
		return true;
	}
	public void openHighLowTemp(){
		TempWidgetObj highTemp = new TempWidgetObj();

		PointTempWidget high= new PointTempWidget();
		high.setStartPointX((int) getXByWRatio(tempSource[1]));
		high.setStartPointY((int) getYByHRatio(tempSource[2]));
		high.setTemp(tempSource[3]);
		Log.e(TAG, "x: "+ getXByWRatio(tempSource[1]) + "   y === > " + getYByHRatio(tempSource[2]));

		highTemp.setCanMove(false);
		highTemp.setTempTextSize(20);
		highTemp.setType(1);
		highTemp.setId(1);
		highTemp.setTextSuffix("℃");
		highTemp.setPointTemp(high);

		TempWidgetObj lowTemp = new TempWidgetObj();

		PointTempWidget low= new PointTempWidget();
		low.setStartPointX((int) getXByWRatio(tempSource[4]));
		low.setStartPointY((int) getYByHRatio(tempSource[5]));
		low.setTemp(tempSource[6]);

		lowTemp.setCanMove(false);
		lowTemp.setTempTextSize(20);
		lowTemp.setType(1);
		lowTemp.setId(2);
		lowTemp.setTextSuffix("℃");
		lowTemp.setPointTemp(low);


		MyMoveWidget highWidget = new MyMoveWidget(mContext.get(),highTemp,screenWidth,screenHeight);
//		highWidget.setAlpha(0.6f);
//		highWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
		MyMoveWidget lowWidget = new MyMoveWidget(mContext.get(),lowTemp,screenWidth,screenHeight);
//		lowWidget.setAlpha(0.6f);
//		lowWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));

		addView(highWidget);
//		addView(lowWidget);

		viewLists.add(highWidget);
//		viewLists.add(lowWidget);
	}
	public void closeHighLowTemp(){
		removeView(viewLists.get(0));
//		removeView(viewLists.get(1));

		viewLists.remove(0);
//		viewLists.remove(1);
	}

	private void createPointView(){
		TempWidgetObj widget = new TempWidgetObj();

		PointTempWidget pointTempWidget = new PointTempWidget();
		pointTempWidget.setStartPointX(startPressX);
		pointTempWidget.setStartPointY(startPressY);
		pointTempWidget.setTemp(100);

		widget.setId(pointNum);
		widget.setType(drawTempMode);
		widget.setCanMove(true);
		widget.setSelect(false);
		widget.setTempTextSize(20);
		widget.setTextSuffix("℃");
		widget.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete, R.mipmap.define_view_tools_other});
		widget.setPointTemp(pointTempWidget);

		calculateWidthHeight(drawTempMode,true,true);

		MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),widget,screenWidth,screenHeight);
		ConstraintLayout.LayoutParams  layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(moveWidget.getXOffset(),moveWidget.getYOffset(),0,0);

		moveWidget.setLayoutParams(layoutParams);
		moveWidget.setAlpha(0.6f);
		moveWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
		addView(moveWidget);
		pointNum++;
		viewLists.add(moveWidget);
	}
	private void createLineOrRecView(){
		if (reviseCoordinate(drawTempMode)){
			TempWidgetObj tempWidget = new TempWidgetObj();
			//校正起始点坐标
			OtherTempWidget otherTempWidget = new OtherTempWidget();

			otherTempWidget.setStartPointX(startPressX);
			otherTempWidget.setStartPointY(startPressY);
			otherTempWidget.setEndPointX(endPressX);
			otherTempWidget.setEndPointY(endPressY);

			otherTempWidget.setMinTempX(otherTempWidget.getStartPointX()+20);
			otherTempWidget.setMinTempY(otherTempWidget.getStartPointY());
			otherTempWidget.setMinTemp(50);
			otherTempWidget.setMaxTempX(otherTempWidget.getEndPointX()-20);
			otherTempWidget.setMaxTempY(otherTempWidget.getEndPointY());
			otherTempWidget.setMaxTemp(131);

			tempWidget.setType(drawTempMode);
			tempWidget.setCanMove(true);
			tempWidget.setSelect(false);
			tempWidget.setTempTextSize(16);
			tempWidget.setTextSuffix("℃");
			tempWidget.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete, R.mipmap.define_view_tools_other});
			tempWidget.setOtherTemp(otherTempWidget);
			calculateWidthHeight(drawTempMode,true,true);

			MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),tempWidget,screenWidth,screenHeight);
			RelativeLayout.LayoutParams  layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParams.setMargins(moveWidget.getXOffset(),moveWidget.getYOffset(),0,0);
			moveWidget.setLayoutParams(layoutParams);
			moveWidget.setAlpha(0.6f);
			moveWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
			addView(moveWidget);
			viewLists.add(moveWidget);
		}
	}
	//计算子控件 工具栏及文字放置的方位
	private void calculateWidthHeight(int type , boolean canMove , boolean isSelect){
		switch (type){
			case 1://点
				addWidgetMarginX =  startPressX - minTempBt.getWidth()/2;
				addWidgetMarginY =  startPressY - minTempBt.getHeight()/2;
				break;
			case 2:
			case 3:
				addWidgetMarginX =  startPressX;
				addWidgetMarginY =  startPressY;
				break;
		}
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
		if (drawTempMode != -1){//绘制模式 ，自己消费事件
//			switch (ev.getAction()) {
//				case MotionEvent.ACTION_MOVE:
//					if (drawTempMode == 2 || drawTempMode == 3) {
//						return true;
//					} else {
//						return super.onInterceptTouchEvent(ev);
//					}
//					//todo 移动可以移动的控件， 或实时缩放
//			}
			return true;
		}else {
			//拿到点击的点
			return super.onInterceptTouchEvent(ev);
		}
	}

//	@Override
//	public void draw (Canvas canvas) {
//		super.draw(canvas);
//
//
//	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (drawTempMode != -1){
			switch (event.getAction()){
				case MotionEvent.ACTION_DOWN:
					if (drawTempMode != -1){
						startPressX = (int) event.getX();
						startPressY = (int) event.getY();
					}
					break;
				case MotionEvent.ACTION_MOVE:
					endPressX = (int) event.getX();
					endPressY = (int) event.getY();
					break;
				case MotionEvent.ACTION_UP:
					endPressX = (int) event.getX();
					endPressY = (int) event.getY();

					if (drawTempMode == 2 || drawTempMode ==3){
						createLineOrRecView();
					}else if (drawTempMode ==1) {
						createPointView();
					}
					break;
			}
			return true;
		}else {//不是绘制
			return false;
		}
	}

	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what){
				case UPDATE_TEMP_DATA:
				tempSource = (float[]) msg.obj;
//					Log.e(TAG, "UPDATE: " + " x = " + tempSource[1] + " y = "+ tempSource[2] +" value "+ tempSource[3]);
//					Log.e(TAG, "UPDATE ==>>: "+ " x = " + tempSource[4] + " y = "+ tempSource[5] +" value ==> "+ tempSource[6]);
//					Log.e(TAG, "handleMessage: " + viewLists.size());
				if (viewLists.size()==1){
					viewLists.get(0).getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[1]));
					viewLists.get(0).getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[2]));

					Log.e(TAG, "HRatio: "+ HRatio + "   WRatio === > " + WRatio);
					Log.e(TAG, "x: "+ tempSource[1] + "   y === > " + tempSource[2]);
					Log.e(TAG, "x: "+ (int)getXByWRatio(tempSource[1]) + "   y === > " + (int)getYByHRatio(tempSource[2]));
					Log.e(TAG, "x: "+ viewLists.get(0).getView().getPointTemp().getStartPointX() + "   y === > " + viewLists.get(0).getView().getPointTemp().getStartPointY());
					viewLists.get(0).getView().getPointTemp().setTemp(tempSource[3]);
					viewLists.get(0).dataUpdate(viewLists.get(0).getView());
					viewLists.get(0).invalidate();

//					viewLists.get(1).getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[4]));
//					viewLists.get(1).getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[5]));
//					viewLists.get(1).getView().getPointTemp().setTemp(tempSource[6]);
//					viewLists.get(1).invalidate();
				}
//				invalidate();
				break;
			}
			return false;
		}
	}) ;
}
