package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dyt.wcc.common.R;
import com.dyt.wcc.common.utils.DensityUtil;
import com.dyt.wcc.common.widget.MyCustomRangeSeekBar;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
	private MyCustomRangeSeekBar mSeekBar;

	//点击工具栏之后的控制 响应的事件。删除的事件。
	public static int perToolsWidthHeightSet = 35;//每个工具栏的宽高
	public static int perToolsMargin = 5;//每个工具栏的margin

	private static final int UPDATE_TEMP_DATA = 1;

	private static final boolean isDebug = true;
	private static final String TAG = "MyDragContainer";
	private boolean isControlItem = false;//是否是操作子View

	private CopyOnWriteArrayList<MyMoveWidget>                 highLowTempLists;
	private CopyOnWriteArrayList<MyMoveWidget> userAdd;
	private Bitmap                             pointBt, maxTempBt, minTempBt;//三张图片 ：单点温度、线和矩阵的 最高、最低温的图片

	private float WRatio, HRatio;//相机数据大小  与 显示屏幕大小的比值。


	private int           drawTempMode = -1;
	private WeakReference<Context> mContext;

	/**
	 * temperatureData[0]=centerTmp;
	 * 	temperatureData[1]=(float)maxx1;
	 * 	temperatureData[2]=(float)maxy1;
	 * 	temperatureData[3]=maxTmp;
	 * 	temperatureData[4]=(float)minx1;
	 * 	temperatureData[5]=(float)miny1;
	 * 	temperatureData[6]=minTmp;
	 * 	temperatureData[7]=point1Tmp;
	 * 	temperatureData[8]=point2Tmp;
	 * 	temperatureData[9]=point3Tmp;
	 */
	private float [] tempSource;

	private int pointNum = 0;
	private int lineNum = 0;
	private int recNum = 0;
	private static final int POINT_MAX_NUMBER = 3;
	private static final int LINE_MAX_NUMBER = 3;
	private static final int RECTANGLE_MAX_NUMBER = 3;

	private int screenWidth , screenHeight;
	private int startPressX, startPressY, endPressX, endPressY;
	private int addWidgetMarginX, addWidgetMarginY;
	private int minAddWidgetWidth, minAddWidgetHeight;//添加控件的最小宽高  约束添加的线和矩形

	private boolean enabled = true;//设置是否可用,为false时不能添加view 不能进行任何操作

	//温度数值的模式。0摄氏度， 1华氏度， 2开氏度
	private int tempSuffixMode  =0;
	/**
	 * 温度单位 String
	 * tempSuffixList
	 * "℃","℉","K"
	 */
	public static String []tempSuffixList = new String[]{"℃","℉","K"};


	private Paint testPaint;
	private RectF rectFHighTempAlarm;
	private float valueHighTempAlarm = 0.0f;//设置最高温数值
	private boolean isAboveHighTemp = false;//是否超温
	private boolean highTempAlarmToggle = false;
	private int alarmCountDown = 0;
	private MyMoveWidget operateChild = null;
	private int frameCount = 0;
	private MediaPlayer audioPlayer;
	private long lastAboveTime = 0;


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

	public MyCustomRangeSeekBar getmSeekBar () {
		return mSeekBar;
	}
	public void setmSeekBar (MyCustomRangeSeekBar mSeekBar) {
		this.mSeekBar = mSeekBar;
	}

	/**
	 * <p>通过添加的 数据源列表，得到其中矩形的坐标的 int [] 数组。</p>
	 * <p>排布顺序，开始X，> 结束X， 开始Y，> 结束Y。</p>
	 * @return int [] 数组
	 */
	public int [] getAreaIntArray(){
		int areaNumber = 0;
		//得到矩形数据源列表中，矩形的数量
		for (MyMoveWidget child : userAdd){
			if (child.getView().getType() ==3){
				areaNumber ++;
			}
		}
		//创建一个int数据保存 数据源列表中的矩阵坐标。一个矩形 四个坐标。
		int [] areaData = new int[4*areaNumber];

		int areaIndex =0 ;
		for (MyMoveWidget child : userAdd){
			if (child.getView().getType() ==3){
				areaData[4 * areaIndex] = (int) (child.getView().getOtherTemp().getStartPointX() / screenWidth * 256);
				areaData[4*areaIndex+1] = (int) (child.getView().getOtherTemp().getEndPointX() / screenWidth * 256);
				areaData[4*areaIndex+2] = (int) (child.getView().getOtherTemp().getStartPointY() / screenHeight * 192);
				areaData[4*areaIndex+3] = (int) (child.getView().getOtherTemp().getEndPointY() / screenHeight * 192);
				areaIndex++;
			}
		}
		return areaData;
	}

	public boolean isHighTempAlarmToggle () {
		return highTempAlarmToggle;
	}

	public void setHighTempAlarmToggle (boolean highTempAlarmToggle) {
		this.highTempAlarmToggle = highTempAlarmToggle;
	}

	/**
	 * 超温报警 数据源是摄氏度.传入的带温度单位的数值。
	 * <p>超温报警逻辑：</p>
	 * <p>温度超过，并且开启高温警告。如果超温 则开始倒计时 绘制。 每间隔0.5s 绘制警告框。0.5S时间不绘制</p>
	 * <p>如果关闭，则停止绘制，倒计时也停止。 如果未超温则 不绘制。</p>
	 *
	 * @param thresholdTemp 带了模式 的温度。 需要转换成摄氏度
	 */
	public void openHighTempAlarm(float thresholdTemp){
		highTempAlarmToggle = true;

		if (tempSuffixMode == 0){//0摄氏度， 1华氏度， 2开氏度
			valueHighTempAlarm = getFormatFloat(thresholdTemp);
		}
		if (tempSuffixMode == 1){
			valueHighTempAlarm = getFormatFloat((thresholdTemp - 32) / 1.8f);
		}
		if (tempSuffixMode == 2){
			valueHighTempAlarm = getFormatFloat((thresholdTemp - 273.15f));
		}
		if (audioPlayer == null){
			audioPlayer = MediaPlayer.create(mContext.get(),R.raw.a2_ding);
			audioPlayer.setLooping(true);
		}
	}
	public void closeHighTempAlarm(){
		highTempAlarmToggle = false;
		invalidate();

		if (audioPlayer != null){
			audioPlayer.stop();
			audioPlayer.release();
			audioPlayer = null;
		}

	}

	public interface OnChildToolsClickListener{
		void onChildToolsClick(TempWidgetObj childObj, int position);

		void onRectChangedListener();
	}
	private OnChildToolsClickListener mChildToolsClickListener;
	public void setChildToolsClickListener (OnChildToolsClickListener childToolsClickListener) {
		this.mChildToolsClickListener = childToolsClickListener;
	}

	private void initAttrs(){

		testPaint = new Paint();
		testPaint.setStyle(Paint.Style.STROKE);
		testPaint.setStrokeWidth(DensityUtil.dp2px(mContext.get(),3));
		testPaint.setColor(getResources().getColor(R.color.max_temp_text_color_red));

	}
	private void initView(){
		drawTempMode = -1;
		userAdd = new CopyOnWriteArrayList<>();
		highLowTempLists = new CopyOnWriteArrayList<>();//初始化高低中心点 温度 追踪集合
		tempSource = new float[256*196+10];
		minAddWidgetWidth = minAddWidgetHeight = DensityUtil.dp2px(mContext.get(),70);//最小为50个dp

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

	/**
	 * 删除一个子控件，通过回调得到的 数据源对象
	 * @param obj
	 */
	public void removeChildByDataObj(TempWidgetObj obj){
		for (MyMoveWidget child  : userAdd){
			if (obj.equals(child.getView())){
				removeView(child);
				userAdd.remove(child);
			}
		}
	}

	protected void removeAllItemView(){
		removeAllViews();
		highLowTempLists.clear();
		invalidate();
	}
	//查询 分发事件

	/**
	 * 查询在 绘制页面有没有添加 高温 低温 中心 的点坐标
	 * @param type
	 * @return
	 */
	public boolean hasOpenToggleByType(int type){
		if (highLowTempLists.size()==0)return false;
		for (MyMoveWidget pointChild : highLowTempLists){
			if (pointChild.getView().getPointTemp().getType()== type){
				return true;
			}
		}
		return false;
	}


	@Nullable
	@Override
	protected Parcelable onSaveInstanceState () {
		return super.onSaveInstanceState();
	}

	@Override
	protected void onRestoreInstanceState (Parcelable state) {
		super.onRestoreInstanceState(state);
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
//		Log.e(TAG, "onDraw: " + highTempAlarmToggle +" "+ isAboveHighTemp);
		if (highTempAlarmToggle && isAboveHighTemp){//每隔 0.5S 绘制一次
			if (frameCount < 10){//绘制超温
				rectFHighTempAlarm = new RectF(6,6,screenWidth-6,screenHeight- 6);
				canvas.drawRect(rectFHighTempAlarm,testPaint);
				frameCount++;
			}else if (frameCount < 25){//间隔帧数
				frameCount++;
			}else {//释放
				frameCount = 0;
			}
		}
	}


	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		screenWidth = getWidth();
		screenHeight = getHeight();

		WRatio = 256*1.0f / screenWidth;
		HRatio = 192*1.0f /  screenHeight;

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
	 * 创建区域检查
	 * 打开逻辑：userAdd中，无区域检查的矩形，则添加，否则，拿到矩形的列表 穿给控件。
	 * 取消逻辑：控件层取消
	 */
	public List<MyMoveWidget> openAreaCheck(int width,int height){
		List<MyMoveWidget> recList = new ArrayList<>();
		for (MyMoveWidget child : userAdd){
			if (child.getView().getType()==3){
				recList.add(child);
			}
		}
		if (recList.size() ==0){
			TempWidgetObj tempWidget = new TempWidgetObj();
			//校正起始点坐标
			OtherTempWidget otherTempWidget = new OtherTempWidget();

			otherTempWidget.setStartPointX(width/3.0f);
			otherTempWidget.setStartPointY(height/3.0f);
			otherTempWidget.setEndPointX(width/3.0f*2);
			otherTempWidget.setEndPointY(height/3.0f*2);

			updateLRTemp(otherTempWidget,3);

			tempWidget.setType(3);
			tempWidget.setCanMove(true);
			tempWidget.setSelect(false);
			tempWidget.setTempTextSize(20);
			tempWidget.setToolsPicRes(new int[]{R.mipmap.ic_areacheck_tools_delete});
			tempWidget.setOtherTemp(otherTempWidget);

			MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),tempWidget,screenWidth,screenHeight);
			moveWidget.setChildToolsClickListener(mChildToolsClickListener);
			//todo 重置userAddView的集合
			addView(moveWidget);
			userAdd.add(moveWidget);
		}
		return recList;
	}

	/**
	 * <p> 给数值设置后缀。</p>
	 * <p> 给接收数据的滑动条 控件也设置 温度单位。</p>
	 * <p> throws NullPointerException 初始化没设置滑动条</p>
	 */
	public void setTempSuffix(int suffixType) throws NullPointerException{
		tempSuffixMode = suffixType;
		//给滑动条也设置温度单位
		if (mSeekBar!=null) mSeekBar.setTempUnitText(tempSuffixMode);
	}

	//更新点的温度
	private float updatePointTemp(float x , float y){
		//数据源上的坐标
		int index =  (10 + (int)(y*HRatio) * 256 + (int)(x*WRatio));
		return tempSource[index];
	}
	//更新线L，矩形Rec 上的最高最低温度
	private void updateLRTemp(OtherTempWidget tempWidget , int type){
//		Log.e(TAG, "updateLRTemp: ");
		//更新线，矩形的温度 及其坐标
		int startX ,startY ,endX ,endY, minIndex, maxIndex;//数据源上的边界点
		startX =  (int)(tempWidget.getStartPointX()*WRatio);startY = (int)(HRatio*tempWidget.getStartPointY());
		endX =  (int)(tempWidget.getEndPointX()*WRatio); endY = (int)(HRatio*tempWidget.getEndPointY());
//		Log.e(TAG, "updateLRTemp: startX = " + startX + " startY " + startY + " endx " + endX  + " endy" + endY);
		//最低最高温度。及其坐标
//		float minTemp , maxTemp;//默认值指向第一个点的数据
//		minTemp = tempSource[(10+(startX+startY*256))];
//		maxTemp = tempSource[(10+(startX+startY*256))];
		//用什么去记录 最高点 最低点时候的xy值。切记要加上前置的10
		int LRMinTempX,LRMinTempY, LRMaxTempX,LRMaxTempY;
		LRMinTempX = startX;LRMinTempY = startY;LRMaxTempX = startX;LRMaxTempY = startY;//默认值
		for (int i = startY; i < endY ; i++){//高度遍历
			for (int j = startX; j < endX ; j++){//宽度遍历
				if (tempSource[(LRMinTempX+(LRMinTempY*256)+10)] > tempSource[j+(i*256)+10]){
					LRMinTempX = j;
					LRMinTempY = i;
				}
				if (tempSource[(int) (LRMaxTempX+(LRMaxTempY*256)+10)] < tempSource[j+(i*256)+10]){
					LRMaxTempX = j;
					LRMaxTempY = i;
				}
			}
		}
		if (type ==2) {
			for (int j = startX; j < endX ; j++){//宽度遍历
				if (tempSource[(int) (LRMinTempX+(LRMinTempY*256)+10)] >= tempSource[(int) (j+(LRMinTempY*256)+10)]){
					LRMinTempX = j;
				}
				if (tempSource[(int) (LRMaxTempX+(LRMinTempY*256)+10)] <= tempSource[(int) (j+(LRMinTempY*256)+10)]){
					LRMaxTempX = j;
				}
			}
		}
		tempWidget.setMinTempX((LRMinTempX/WRatio));
		tempWidget.setMinTempY((LRMinTempY/HRatio));
		tempWidget.setMinTemp(getTempStrByMode(tempSource[(LRMinTempX + (LRMinTempY*256) + 10)]));
		tempWidget.setMaxTempX((LRMaxTempX/WRatio));
		tempWidget.setMaxTempY((LRMaxTempY/HRatio));
		tempWidget.setMaxTemp(getTempStrByMode(tempSource[(LRMaxTempX + (LRMaxTempY*256) + 10)]));

	}
	//返回绘制的模式drawTempMode
	public int getDrawTempMode () {
		return drawTempMode;
	}
	public void setDrawTempMode (int drawTempMode) {
		this.drawTempMode = drawTempMode;
	}

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
		min = Math.min(startPressY,endPressY);
		max = Math.max(startPressY,endPressY);
		startPressY = min;
		endPressY = max;
		//type 2/3 校正X坐标
		if (endPressX - startPressX < minAddWidgetWidth){
			if(startPressX > screenWidth - minAddWidgetWidth){
				endPressX = screenWidth ;
				startPressX  = screenWidth - minAddWidgetWidth;
			} else {
				endPressX = startPressX + minAddWidgetWidth;
			}
		}
		if (type ==2){
			endPressY = startPressY;
		}else {//type =3; 校正Y坐标
			if (endPressY - startPressY < minAddWidgetHeight){
				if(startPressY > screenHeight - minAddWidgetHeight){
					//
					endPressY = screenHeight ;
					startPressY  = screenHeight - minAddWidgetHeight;
				} else {
					endPressY = startPressY + minAddWidgetHeight;
				}
			}
		}
		return true;
	}

	/**
	 * 单纯的获取最高 最低温度值，根据设置的温度 单位 K F C。
	 * @param tempC 温度摄氏度
	 * @return float 温度数值
	 */
	private float getTempByMode( float tempC){
		float result = 0.0f;
		//先对拿到的温度格式化
		switch (tempSuffixMode){
			case 0:
				result = getFormatFloat(tempC) ;
				break;
			case 1:
				result = getFormatFloat((float) (tempC * 1.8 + 32)) ;
				break;
			case 2:
				result = getFormatFloat((float) (tempC + 273.15));
				break;
		}
		return result;
	}

	/**
	 * 切换温度模式流程：切换到华氏度，得重新计算数值。并更改后面的单位
	 * 华氏度 = 摄氏度*1.8+32 （℉）  ； 开氏度= 摄氏度 + 237.15 （K）
	 * @param tempC 温度摄氏度
	 * @return String 温度数值带单位
	 */
	private String getTempStrByMode( float tempC){
		String result = "0.0℃";
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
		DecimalFormat df = new DecimalFormat("0.0");
		return Float.parseFloat(df.format(value));
	}


	/**
	 * @param x 数据源上的X坐标
	 * @param y 数据源上的Y坐标
	 * @param temp 温度数值
	 * @param type 区分代表的是高温还是低温 1 高温；2低温
	 * @param id 控件的id
	 */
	private void addHighLowTempWidget(float x , float y , float temp , int type , int id ){
		TempWidgetObj highTemp = new TempWidgetObj();

		PointTempWidget high= new PointTempWidget();
		if (type==3){//中心点直接是屏幕的坐标 所以不需要计算数据源的坐标
			high.setStartPointX(x);
			high.setStartPointY(y);
		}else {
			high.setStartPointX((int) getXByWRatio(x));
			high.setStartPointY((int) getYByHRatio(y));
		}
		high.setType(type);//1最高温 ,2 最低温, 3中心点温度
		high.setTemp(getTempStrByMode(temp));

		highTemp.setCanMove(false);
		highTemp.setTempTextSize(20);
		highTemp.setType(1);
		highTemp.setId(id);
		highTemp.setPointTemp(high);

		MyMoveWidget highWidget = new MyMoveWidget(mContext.get(),highTemp,screenWidth,screenHeight);
		highWidget.setFocusable(false);

		addView(highWidget);
		highLowTempLists.add(highWidget);
		highWidget.requestLayout();
	}

	/**
	 * 打开高低温追踪，暴露给外部调用
	 */
	public void openHighLowTemp(int type){
		switch (type){
			case 1:
				addHighLowTempWidget(tempSource[1],tempSource[2],tempSource[3],1,1);
				break;
			case 2:
				addHighLowTempWidget(tempSource[4],tempSource[5],tempSource[6],2,2);
				break;
			case 3:
				addHighLowTempWidget(getMeasuredWidth()/2.0f,getMeasuredHeight()/2.0f,tempSource[0],3,3);
//				if (isDebug)Log.e(TAG, "openHighLowTemp: center point == > " + getMeasuredWidth()/2.0f + "  getMeasuredHeight " + getMeasuredHeight() /2.0f);
//				if (isDebug)Log.e(TAG, "openHighLowTemp:  size  " + highLowTempLists.size() );
				break;
		}
	}

	/**
	 * 关闭高低温追踪，暴露给外部调用
	 */
	public void closeHighLowTemp(int type){
		Log.e(TAG, "openHighLowTemp: center point == > "+  type);
		for (MyMoveWidget widget: highLowTempLists){
			if (widget.getView().getPointTemp().getType() == type){
				removeView(widget);
				highLowTempLists.remove(widget);
				invalidate();
			}
		}
		Log.e(TAG, "closeHighLowTemp: highLowLists close before size " + highLowTempLists.size() );
	}

	/**
	 * 删除所有添加的测温控件。
	 */
	public void clearAll(){
		for (MyMoveWidget widget: userAdd){
			removeView(widget);
		}
		userAdd.clear();
		pointNum = 0;
	}
	//添加点测温模式
	private void createPointView(){
		TempWidgetObj widget = new TempWidgetObj();

		PointTempWidget pointTempWidget = new PointTempWidget();
		pointTempWidget.setStartPointX(startPressX);
		pointTempWidget.setStartPointY(startPressY);
		//通过点计算
		pointTempWidget.setTemp(getTempStrByMode(updatePointTemp(pointTempWidget.getStartPointX(),pointTempWidget.getStartPointY())));

		widget.setId(pointNum);
		widget.setType(drawTempMode);
		widget.setCanMove(true);
		widget.setSelect(false);
		widget.setTempTextSize(20);
		widget.setToolsPicRes(new int[]{R.mipmap.ic_areacheck_tools_delete});
		widget.setPointTemp(pointTempWidget);

		resetUserAddView(widget);
		drawTempMode = -1;
	}
	//添加 线或者矩形 测温模式
	private void createLineOrRecView(){
//		Log.e(TAG, "reviseCoordinate: " + reviseCoordinate(drawTempMode));
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
			tempWidget.setTempTextSize(20);
			tempWidget.setToolsPicRes(new int[]{R.mipmap.ic_areacheck_tools_delete});
			tempWidget.setOtherTemp(otherTempWidget);

			//todo 重置userAddView的集合
			resetUserAddView(tempWidget);
		}
		drawTempMode = -1;
	}

	/**
	 * 新添加子View的业务逻辑，小于类型所规定的值，直接添加。
	 * 大于类型的值。第一个添加的直接移除。再添加一个。
	 * @param childData 数据源
	 */
	private void resetUserAddView(TempWidgetObj childData){
		int type = childData.getType();
		MyMoveWidget firstTypeChild  = null;
		int typeCount = 0;
		//记录第一个同类型的View,并累计有都少个同类型
		for (MyMoveWidget child : userAdd){
			if (type == child.getView().getType() && firstTypeChild ==null){
				firstTypeChild = child;
			}
			if (type == child.getView().getType()){
				typeCount++;
			}
		}
		switch (type){
			case 1:
				if (typeCount == POINT_MAX_NUMBER){
					removeView(firstTypeChild);
					userAdd.remove(firstTypeChild);
				}
				addChild(childData);
				break;
			case 2:
				if (typeCount == LINE_MAX_NUMBER){
					removeView(firstTypeChild);
					userAdd.remove(firstTypeChild);
				}
				addChild(childData);
				break;
			case 3:
				if (typeCount == RECTANGLE_MAX_NUMBER){
					removeView(firstTypeChild);
					userAdd.remove(firstTypeChild);
				}
				addChild(childData);
				break;
		}
	}

	private void addChild(TempWidgetObj childData){
		MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),childData,screenWidth,screenHeight);
		moveWidget.setChildToolsClickListener(mChildToolsClickListener);
		//todo 重置userAddView的集合
		addView(moveWidget);
		userAdd.add(moveWidget);
	}

	/**
	 * 通过点击事件，得到位置上是否有子View。
	 * @param event 点击事件
	 * @return 是否有子View
	 */
	private boolean getEventInChild(MotionEvent event){
		boolean isOperate = false;
		for (MyMoveWidget child : userAdd){
			isOperate = isOperate | (event.getX() >= child.getLeft() && event.getX() <= child.getRight()
					&& event.getY() >= child.getTop() && event.getY() <= child.getBottom());
		}
		return isOperate;
	}

	/**
	 * 判断是否有选中状态的子View
	 * @return 是否有选中的子View
	 */
	private boolean hasSelectChild(MotionEvent event){
		boolean hasSelectChild = false;
		for (MyMoveWidget child : userAdd){
			hasSelectChild = hasSelectChild|child.isSelectedState();
		}
		return hasSelectChild;
	}
	//按下的操作是否在已选中的控件中。
	private boolean eventInSelectChild(MotionEvent event){
		boolean flag = false;
//		Log.e(TAG, "eventInSelectChild: userAdd size = " + userAdd.size() );
		for (MyMoveWidget child : userAdd){
//			Log.e(TAG, "getSelectChildByMotionEvent: chile left " + child.getLeft() + " right " + child.getRight() + " top " + child.getTop()+ " bottom " + child.getBottom());
			flag = flag |(child.isSelectedState() && (event.getX() >= child.getLeft() && event.getX() <= child.getRight()
					&& event.getY() >= child.getTop() && event.getY() <= child.getBottom()));
		}
		return flag;
	}
	private MyMoveWidget getSelectChildByEvent(MotionEvent event){
		MyMoveWidget children = null;
		for (MyMoveWidget child : userAdd){
			if (child.isSelectedState() && (event.getX() >= child.getLeft() && event.getX() <= child.getRight()
					&& event.getY() >= child.getTop() && event.getY() <= child.getBottom())){
				children = child;
			}
		}
		return children;
	}

	/**
	 * 通过点击的坐标。得到选中的子View（返回的是最后一个此范围的控件。效果不对）
	 * @param event
	 * @return
	 */
	private MyMoveWidget getSelectChildByMotionEvent(MotionEvent event){
		MyMoveWidget select = null;
		for (MyMoveWidget child : userAdd){

			if (event.getX() >= child.getLeft() && event.getX() <= child.getRight()
					&& event.getY() >= child.getTop() && event.getY() <= child.getBottom()){
				select = child;
			}
		}
		return select;
	}

	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		if (!enabled){
			return true;
		}
		return super.dispatchTouchEvent(ev);
	}
	@Override
	public boolean onInterceptTouchEvent (MotionEvent ev) {
		if (isDebug)Log.e(TAG, "Parent onIntercept: x " + ev.getX() + " Y = " + ev.getY() + " action= " + ev.getAction() );

		if (drawTempMode == -1){
			if (ev.getAction() == MotionEvent.ACTION_DOWN){//看点的位置是否有选中的View，有则一直调用他的分发。否则设置为所有view并发响应
				Log.e(TAG, "onInterceptTouchEvent: " + eventInSelectChild(ev));
				if (!eventInSelectChild(ev)){
					setAllChildUnSelect();
//					operateChild = null;//重置
				}
//				else {//点击事件在选中的子View
//					operateChild = getSelectChildByEvent(ev);
//				}
//				return super.onInterceptTouchEvent(ev);
			}
//			if (ev.getAction() == MotionEvent.ACTION_UP){
//				if (operateChild != null && operateChild.getView().getType()!=3){//移动子View
//
//				}
////				return super.onInterceptTouchEvent(ev);
//			}
//			if (ev.getAction() == MotionEvent.ACTION_MOVE){
//				return super.onInterceptTouchEvent(ev);
//			}
			return super.onInterceptTouchEvent(ev);
		}else {
			return true;
		}


//		if (getEventInChild(ev) && drawTempMode == -1){//按下的位置在子view 并且不是绘制模式。分发事件
//			//判断是否有选中状态；有控件为选中状态，则是修改的选中状态的控件；无选中状态，则是要触发第一个子View的分发事件。其余的不响应
//			if (ev.getAction() == MotionEvent.ACTION_DOWN){
//				setAllChildUnSelect();
//			}
//			if (isDebug)Log.e(TAG, "Parent onInterceptTouchEvent:操作子View ，不拦截事件");
//			return super.onInterceptTouchEvent(ev);
//		}else if (drawTempMode !=-1){//绘制模式
//			if (isDebug)Log.e(TAG, "Parent onInterceptTouchEvent:绘制控件，拦截事件  drawTempMode!= -1 ");
//			return true;
//		}else {//点击了空白区域
//			if (ev.getAction() == MotionEvent.ACTION_DOWN){
//				setAllChildUnSelect();
//			}
//			if (isDebug)Log.e(TAG, "Parent onInterceptTouchEvent: 错误进入 " + ev.getAction());
//			return true;
//		}
		//逻辑一：触碰的点在子View中，并且有子View是选中状态。则选中的子View自己处理这个触碰时间，其余的未选中的不做处理
//		if (getEventInChild(ev)){
//			if (!hasSelectChild(ev)){
//				return super.onInterceptTouchEvent(ev);
//			}
//		}else {//触碰的点不在子View
//			//按下的时候将所有的子View遍历为未选中状态
//			if (ev.getAction() == MotionEvent.ACTION_DOWN){
//				setAllChildUnSelect();
//			}
//			return true;
//		}

//		switch (ev.getAction()){
//			case MotionEvent.ACTION_DOWN:
//
//				break;
//			case MotionEvent.ACTION_MOVE:
//
//				break;
//			case MotionEvent.ACTION_UP:
//
//				break;
//		}




	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		Log.e(TAG, "onTouchEvent: mode ==> " + drawTempMode + " action ==> "+ event.getAction());
		if (drawTempMode != -1){//添加控件
			switch (event.getAction()){
				case MotionEvent.ACTION_DOWN:
//					if (drawTempMode != -1){
						startPressX = (int) event.getX();
						startPressY = (int) event.getY();
//					}
					break;
				case MotionEvent.ACTION_MOVE:
//					if (drawTempMode != -1) {
						endPressX = (int) event.getX();
						endPressY = (int) event.getY();
//					}
					break;
				case MotionEvent.ACTION_UP:
//					if (drawTempMode != -1) {
						endPressX = (int) event.getX();
						endPressY = (int) event.getY();
//					}

					if (drawTempMode == 2){
						createLineOrRecView();
					}else if (drawTempMode ==3){
						createLineOrRecView();
						if (mChildToolsClickListener!=null){
							mChildToolsClickListener.onRectChangedListener();
						}
					}else if (drawTempMode ==1) {
							createPointView();
					}
					break;
			}
		}else {//移动控件

		}
		return true;
	}


	 //设置所有的子控件都未选中
	public void setAllChildUnSelect(){
		Log.e(TAG, "Parent setAllChildUnSelect: ");
		if (userAdd.size() > 0){
			for (MyMoveWidget child : userAdd){
				child.setSelectedState(false);
			}
		}
	}


	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what){
				case UPDATE_TEMP_DATA:
				tempSource = (float[]) msg.obj;
				if (tempSource[3] >= valueHighTempAlarm){//判定超温报警是否超过了额定温度，每帧刷新
					isAboveHighTemp= true;
				}else {
					isAboveHighTemp = false;
				}
				if (isAboveHighTemp && highTempAlarmToggle){//打开了开关，并且超温了。
					lastAboveTime = System.currentTimeMillis();
					if (audioPlayer != null && !audioPlayer.isPlaying()){
						audioPlayer.start();
					}
					Log.e(TAG, "handleMessage: " + audioPlayer.isPlaying());
				}else {//一段时间（X）后停止音乐的播放，如果音乐是在播放，则停止。
					long seconds = ((System.currentTimeMillis() - lastAboveTime)/1000);

					if (seconds > 3 && audioPlayer != null && audioPlayer.isPlaying()){//时间可以更改
						audioPlayer.pause();
					}
				}
				invalidate();//重新绘制

				if (mSeekBar!=null){//更新滑动温度条
					mSeekBar.Update(tempSource[3],tempSource[6]);
				}
				//刷新高低温追踪的数据
				if (highLowTempLists.size()!=0){
					for (MyMoveWidget myMoveWidget : highLowTempLists){
						if (myMoveWidget.getView().getPointTemp().getType() == 1){
							myMoveWidget.getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[1]));
							myMoveWidget.getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[2]));
							myMoveWidget.getView().getPointTemp().setTemp(getTempStrByMode(tempSource[3]));
						}
						if (myMoveWidget.getView().getPointTemp().getType()==2){
							myMoveWidget.getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[4]));
							myMoveWidget.getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[5]));
							myMoveWidget.getView().getPointTemp().setTemp(getTempStrByMode(tempSource[6]));
						}
						if (myMoveWidget.getView().getPointTemp().getType() == 3){
							myMoveWidget.getView().getPointTemp().setTemp(getTempStrByMode(tempSource[0]));
						}
						myMoveWidget.requestLayout();
						myMoveWidget.invalidate();
					}
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
