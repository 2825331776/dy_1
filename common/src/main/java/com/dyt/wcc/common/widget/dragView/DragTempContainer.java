package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
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

	private List<MyMoveWidget> highLowTempLists;
	private List<MyMoveWidget> userAdd;
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


	private Paint testPaint;

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

		testPaint = new Paint();
		testPaint.setColor(getResources().getColor(R.color.min_temp_text_color_blue));

	}
	private void initView(){
		drawTempMode = -1;
		userAdd = new ArrayList<>();
		highLowTempLists = new ArrayList<>();
		tempSource = new float[256*196+10];
		minAddWidgetWidth = minAddWidgetHeight = 100;


		pointBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
		minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorblue);
		maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorred);
	}

	//对象方法 去增加一个 点 线 矩阵视图 , int type
	protected void addDrawView(MyMoveWidget moveWidget){
		addView(moveWidget);
		highLowTempLists.add(moveWidget) ;
		invalidate();
	}

	protected void removeDrawView(int id){
		for (int i = 0 ; i < highLowTempLists.size(); i++){
			if (id == highLowTempLists.get(i).getView().getId()){
			removeView(highLowTempLists.get(i));
			highLowTempLists.remove(highLowTempLists.get(i));
			invalidate();
			}
		}
	}

	protected void removeAllItemView(){
		removeAllViews();
		highLowTempLists.clear();
		invalidate();
	}
	//查询 分发事件

//	@Override
//	protected void onDraw (Canvas canvas) {
//		super.onDraw(canvas);
//		canvas.drawCircle(10,20,3,testPaint);
//
//		canvas.drawCircle(50,10,3,testPaint);
//	}

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

//	@Override
//	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//	}
	private float updatePointTemp(int x , int y){
		//数据源上的坐标
		int dataX = (int) (WRatio*x);
		int dataY = (int) (HRatio*y);
		return tempSource[(10+(dataX+dataY*256))];
	}
	private void updateLRTemp(OtherTempWidget tempWidget){
		//更新线，矩形的温度 及其坐标
		int startX ,startY ,endX ,endY;//数据源上的边界点
		startX = (int) (tempWidget.getStartPointX()*WRatio);startY = (int) (HRatio*tempWidget.getStartPointY());
		endX = (int) (tempWidget.getEndPointX()*WRatio); endY = (int) (HRatio*tempWidget.getEndPointY());
		//最低最高温度。及其坐标
		float minTemp , maxTemp;//默认值指向第一个点的数据
		minTemp = tempSource[(10+(startX+startY*256))];
		maxTemp = tempSource[(10+(startX+startY*256))];
		//用什么去记录 最高点 最低点时候的xy值。切记要加上前置的10
		int LRMinTempX,LRMinTempY, LRMaxTempX,LRMaxTempY;
		LRMinTempX = startX;LRMinTempY = startY;LRMaxTempX = endX;LRMaxTempY = endY;//默认值
		for (int i = startY; i < endY ; i++){//高度遍历
			for (int j = startX; j < endX ; j++){//宽度遍历



			}
		}


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
	/**
	 * 校正 线、矩形模式的起始点坐标。
	 * 线模式 传入的点 Y轴设置为起始点的Y轴坐标。
	 * 矩形模式：将
	 * @param type
	 * @return 传入的坐标是否可用。 默认为可用。矩形长宽 低于最小值不可用
	 */
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
		high.setType(1);//最高温
		high.setTemp(tempSource[3]);
//		Log.e(TAG, "x: "+ getXByWRatio(tempSource[1]) + "   y === > " + getYByHRatio(tempSource[2]));

		highTemp.setCanMove(false);
		highTemp.setTempTextSize(14);
		highTemp.setType(1);
		highTemp.setId(1);
		highTemp.setTextSuffix("℃");
		highTemp.setPointTemp(high);

		TempWidgetObj lowTemp = new TempWidgetObj();

		PointTempWidget low= new PointTempWidget();
		low.setStartPointX((int) getXByWRatio(tempSource[4]));
		low.setStartPointY((int) getYByHRatio(tempSource[5]));
		low.setTemp(tempSource[6]);
		low.setType(2);//最低温

		lowTemp.setCanMove(false);
		lowTemp.setTempTextSize(14);
		lowTemp.setType(1);
		lowTemp.setId(2);
		lowTemp.setTextSuffix("℃");
		lowTemp.setPointTemp(low);


		MyMoveWidget highWidget = new MyMoveWidget(mContext.get(),highTemp,screenWidth,screenHeight);
//		highWidget.setAlpha(0.6f);
		highWidget.setBackgroundColor(Color.TRANSPARENT);
		MyMoveWidget lowWidget = new MyMoveWidget(mContext.get(),lowTemp,screenWidth,screenHeight);
//		lowWidget.setAlpha(0.6f);
		lowWidget.setBackgroundColor(Color.TRANSPARENT);

		addView(highWidget);
		addView(lowWidget);

		highLowTempLists.add(highWidget);
		highLowTempLists.add(lowWidget);
	}
	public void closeHighLowTemp(){
		for (MyMoveWidget widget: highLowTempLists){
			removeView(widget);
		}
		highLowTempLists.clear();
	}
	//删除所有的控件
	public void clearAll(){
		for (MyMoveWidget widget: userAdd){
			removeView(widget);
		}
		userAdd.clear();
		pointNum = 0;
	}

	private void createPointView(){
		TempWidgetObj widget = new TempWidgetObj();

		PointTempWidget pointTempWidget = new PointTempWidget();
		pointTempWidget.setStartPointX(startPressX);
		pointTempWidget.setStartPointY(startPressY);
		//通过点计算

		pointTempWidget.setTemp(100);

		widget.setId(pointNum);
		widget.setType(drawTempMode);
		widget.setCanMove(true);
		widget.setSelect(false);
		widget.setTempTextSize(20);
		widget.setTextSuffix("℃");
		widget.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete, R.mipmap.define_view_tools_other});
		widget.setPointTemp(pointTempWidget);

		MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),widget,screenWidth,screenHeight);
		ConstraintLayout.LayoutParams  layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(moveWidget.getXOffset(),moveWidget.getYOffset(),0,0);

		moveWidget.setLayoutParams(layoutParams);
		moveWidget.setBackgroundColor(Color.TRANSPARENT);
		addView(moveWidget);
		pointNum++;
		userAdd.add(moveWidget);

		drawTempMode = -1;
	}
	private void createLineOrRecView(){
		Log.e(TAG, "reviseCoordinate: " + reviseCoordinate(drawTempMode));
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

			MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),tempWidget,screenWidth,screenHeight);
			RelativeLayout.LayoutParams  layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParams.setMargins(moveWidget.getXOffset(),moveWidget.getYOffset(),0,0);
			moveWidget.setLayoutParams(layoutParams);
//			moveWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));
			addView(moveWidget);
			userAdd.add(moveWidget);
		}
		drawTempMode = -1;
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

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		Log.e(TAG, "onTouchEvent: mode ==> " + drawTempMode + " action ==> "+ event.getAction());
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
						if (pointNum < 5){
							createPointView();
						}
					}
					break;
			}
			return true;
		}else {//不是绘制
			return false;
		}
	}


	 //设置所有的子控件都未选中
	public void setAllChildUnSelect(){
		if (userAdd.size() > 0){
			for (MyMoveWidget child : userAdd){
				child.setSelected(false);
			}
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
//					Log.e(TAG, "handleMessage: " + highLowTempLists.size());
					//刷新高低温追踪的数据
				if (highLowTempLists.size()!=0){
					highLowTempLists.get(0).getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[1]));
					highLowTempLists.get(0).getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[2]));

//					Log.e(TAG, "HRatio: "+ HRatio + "   WRatio === > " + WRatio);
//					Log.e(TAG, "x: "+ tempSource[1] + "   y === > " + tempSource[2]);
//					Log.e(TAG, "x: "+ (int)getXByWRatio(tempSource[1]) + "   y === > " + (int)getYByHRatio(tempSource[2]));
//					Log.e(TAG, "x: "+ highLowTempLists.get(0).getView().getPointTemp().getStartPointX() + "   y === > " + highLowTempLists.get(0).getView().getPointTemp().getStartPointY());
					highLowTempLists.get(0).getView().getPointTemp().setTemp(tempSource[3]);
//					highLowTempLists.get(0).dataUpdate(highLowTempLists.get(0).getView());
					highLowTempLists.get(0).requestLayout();
					highLowTempLists.get(0).invalidate();

					highLowTempLists.get(1).getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[4]));
					highLowTempLists.get(1).getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[5]));
					highLowTempLists.get(1).getView().getPointTemp().setTemp(tempSource[6]);
					highLowTempLists.get(1).requestLayout();
				}
				if (userAdd.size()!= 0){//点线测温有数据
					//todo 更新温度
					for (int i = 0 ; i < userAdd.size(); i++){
						//todo 更新每一个的温度信息。
						if (userAdd.get(i).getView().getType()==1){//点
							float temp = updatePointTemp(userAdd.get(i).getView().getPointTemp().getStartPointX(),userAdd.get(i).getView().getPointTemp().getStartPointY());
							userAdd.get(i).getView().getPointTemp().setTemp(temp);
							userAdd.get(i).requestLayout();
						}else {//线及其矩形

						}

					}
				}
				break;
			}
			return false;
		}
	}) ;
}
