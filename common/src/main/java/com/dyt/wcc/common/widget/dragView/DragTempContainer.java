package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.dyt.wcc.common.R;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
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

	//温度数值的模式。0摄氏度， 1华氏度， 2开氏度
	private int tempSuffixMode  =0;
	private String []tempSuffixList = new String[]{"℃","℉","K"};


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
			child.layout((int)child.getXOffset(),(int)child.getYOffset(),
					(int)(child.getXOffset()+child.getWMeasureSpecSize()),(int)(child.getYOffset()+child.getHMeasureSpecSize()));
		}

	}

//	@Override
//	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//	}

	/**
	 * 给温度设置后缀
	 */
	public void setTempSuffix(int SuffixType){

	}


	private float updatePointTemp(float x , float y){
		//数据源上的坐标
		float dataX =  (WRatio*x);
		float dataY =  (HRatio*y);
		return tempSource[(int) (10+(dataX+dataY*256))];
	}

	private void updateLRTemp(OtherTempWidget tempWidget , int type){
//		Log.e(TAG, "updateLRTemp: ");
		//更新线，矩形的温度 及其坐标
		float startX ,startY ,endX ,endY;//数据源上的边界点
		startX =  (tempWidget.getStartPointX()*WRatio);startY = (HRatio*tempWidget.getStartPointY());
		endX =  (tempWidget.getEndPointX()*WRatio); endY = (HRatio*tempWidget.getEndPointY());
//		Log.e(TAG, "updateLRTemp: startX = " + startX + " startY " + startY + " endx " + endX  + " endy" + endY);
		//最低最高温度。及其坐标
		float minTemp , maxTemp;//默认值指向第一个点的数据
		minTemp = tempSource[(int) (10+(startX+startY*256))];
		maxTemp = tempSource[(int) (10+(startX+startY*256))];
		//用什么去记录 最高点 最低点时候的xy值。切记要加上前置的10
		float LRMinTempX,LRMinTempY, LRMaxTempX,LRMaxTempY;
		LRMinTempX = startX;LRMinTempY = startY;LRMaxTempX = startX;LRMaxTempY = startY;//默认值
		for (int i = (int) startY; i < endY ; i++){//高度遍历
			for (int j = (int) startX; j < endX ; j++){//宽度遍历
//				Log.e(TAG, "updateLRTemp: 222" + i + " j " + j);
				if (tempSource[(int) (LRMinTempX+(LRMinTempY*256)+10)] >= tempSource[j+(i*256)+10]){
					LRMinTempX = j;
					LRMinTempY = i;
				}
				if (tempSource[(int) (LRMaxTempX+(LRMaxTempY*256)+10)] <= tempSource[j+(i*256)+10]){
					LRMaxTempX = j;
					LRMaxTempY = i;
				}
			}
		}
		if (type ==2) {
			for (int j = (int) startX; j < endX ; j++){//宽度遍历
//				Log.e(TAG, "updateLRTemp: 222" + i + " j " + j);
				if (tempSource[(int) (LRMinTempX+(LRMinTempY*256)+10)] >= tempSource[(int) (j+(LRMinTempY*256)+10)]){
					LRMinTempX = j;
//					LRMinTempY = i;
				}
				if (tempSource[(int) (LRMaxTempX+(LRMinTempY*256)+10)] <= tempSource[(int) (j+(LRMinTempY*256)+10)]){
					LRMaxTempX = j;
				}
			}
		}
//		Log.e(TAG, "updateLRTemp: LRMaxTempX = " + LRMaxTempX + " LRMaxTempY " + LRMaxTempY + " LRMinTempX " + LRMinTempX  + " LRMinTempY" + LRMinTempY);
		tempWidget.setMinTempX((int) (LRMinTempX/WRatio));
		tempWidget.setMinTempY((int) (LRMinTempY/HRatio));
		tempWidget.setMinTemp(getTempStrByMode(tempSource[(int) (LRMinTempX + (LRMinTempY*256) + 10)]));
		tempWidget.setMaxTempX((int) (LRMaxTempX/WRatio));
		tempWidget.setMaxTempY((int) (LRMaxTempY/HRatio));
		tempWidget.setMaxTemp(getTempStrByMode(tempSource[(int) (LRMaxTempX + (LRMaxTempY*256) + 10)]));

//		tempWidget.setMinTempX(tempWidget.getStartPointX() + 20);
//		tempWidget.setMinTempY(tempWidget.getStartPointY());
//		tempWidget.setMinTemp(getTempStrByMode(50));
//		tempWidget.setMaxTempX(tempWidget.getEndPointX() - 20);
//		tempWidget.setMaxTempY(tempWidget.getEndPointY());
//		tempWidget.setMaxTemp(getTempStrByMode(60));
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
			return false;
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

	/**
	 * 切换温度模式流程：切换到华氏度，得重新计算数值。并更改后面的单位
	 * 华氏度 = 摄氏度*1.8+32 （℉）  ； 开氏度= 摄氏度 + 237.15 （K）
	 * @param tempC 温度摄氏度
	 * @return 温度数值带单位
	 */
	private String getTempStrByMode( float tempC){
		String result = "∞ ℃";
		//先对拿到的温度格式化
		switch (tempSuffixMode){
			case 0:
				result = getFormatFloat(tempC) + tempSuffixList[tempSuffixMode];
				break;
			case 1:
				result = getFormatFloat((float) (tempC * 1.8 + 32)) + tempSuffixList[tempSuffixMode];
				break;
			case 2:
				result = getFormatFloat((float) (tempC + 273.15)) + tempSuffixList[tempSuffixMode];
				break;
		}
		return result;
	}

	private float getFormatFloat(float value){
		DecimalFormat df = new DecimalFormat("#.0");
		return Float.parseFloat(df.format(value));
	}

	private void addHighLowTempWidget(float x , float y , float temp , int type , int id ){
		TempWidgetObj highTemp = new TempWidgetObj();

		PointTempWidget high= new PointTempWidget();
		high.setStartPointX((int) getXByWRatio(x));
		high.setStartPointY((int) getYByHRatio(y));
		high.setType(type);//1最高温 ,2 最低温
		high.setTemp(getTempStrByMode(temp));

		highTemp.setCanMove(false);
		highTemp.setTempTextSize(20);
		highTemp.setType(1);
		highTemp.setId(id);
		highTemp.setPointTemp(high);

		MyMoveWidget highWidget = new MyMoveWidget(mContext.get(),highTemp,screenWidth,screenHeight);
//		highWidget.setBackgroundColor(getResources().getColor(R.color.bg_preview_toggle_unselect));

		addView(highWidget);
		highLowTempLists.add(highWidget);
	}


	private void getMaxMinXYValue(OtherTempWidget other,int mode){
		int startX,startY,endX ,endY;



	}


	public void openHighLowTemp(){
		addHighLowTempWidget(tempSource[1],tempSource[2],tempSource[3],1,1);
		addHighLowTempWidget(tempSource[4],tempSource[5],tempSource[6],2,2);
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
		pointTempWidget.setTemp(getTempStrByMode(updatePointTemp(startPressX,startPressY)));

		widget.setId(pointNum);
		widget.setType(drawTempMode);
		widget.setCanMove(true);
		widget.setSelect(false);
		widget.setTempTextSize(20);
		widget.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete, R.mipmap.define_view_tools_other});
		widget.setPointTemp(pointTempWidget);

		MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),widget,screenWidth,screenHeight);

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

			updateLRTemp(otherTempWidget,drawTempMode);

			tempWidget.setType(drawTempMode);
			tempWidget.setCanMove(true);
			tempWidget.setSelect(false);
			tempWidget.setTempTextSize(16);
			tempWidget.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete, R.mipmap.define_view_tools_other});
			tempWidget.setOtherTemp(otherTempWidget);

			MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),tempWidget,screenWidth,screenHeight);
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
//			for (MyMoveWidget view : userAdd){
//
//			}

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
					highLowTempLists.get(0).getView().getPointTemp().setTemp(getTempStrByMode(tempSource[3]));
//					highLowTempLists.get(0).dataUpdate(highLowTempLists.get(0).getView());
					highLowTempLists.get(0).requestLayout();
					highLowTempLists.get(0).invalidate();

					highLowTempLists.get(1).getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[4]));
					highLowTempLists.get(1).getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[5]));
					highLowTempLists.get(1).getView().getPointTemp().setTemp(getTempStrByMode(tempSource[6]));
					highLowTempLists.get(1).requestLayout();
				}
				if (userAdd.size()!= 0){//点线测温有数据
					//todo 更新温度
					for (int i = 0 ; i < userAdd.size(); i++){
						//todo 更新每一个的温度信息。
						if (userAdd.get(i).getView().getType()==1){//点
							float temp = updatePointTemp(userAdd.get(i).getView().getPointTemp().getStartPointX(),userAdd.get(i).getView().getPointTemp().getStartPointY());
							userAdd.get(i).getView().getPointTemp().setTemp(getTempStrByMode(temp));
						}else {//线及其矩形
							updateLRTemp(userAdd.get(i).gettempWidgetData().getOtherTemp(),userAdd.get(i).gettempWidgetData().getType());
//							userAdd.get(i).invalidate();
						}
						userAdd.get(i).requestLayout();
					}
				}
				break;
			}
			return false;
		}
	}) ;
}
