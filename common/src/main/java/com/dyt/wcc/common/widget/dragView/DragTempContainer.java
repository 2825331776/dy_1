package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.dyt.wcc.common.R;
import com.dyt.wcc.common.utils.DensityUtil;
import com.dyt.wcc.common.utils.KeyboardsUtils;
import com.dyt.wcc.common.widget.MyCustomRangeSeekBar;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
	public static int perToolsWidthHeightSet = 33;//每个工具栏的宽高
	public static int perToolsMargin = 1;//每个工具栏的margin

	private static final int UPDATE_TEMP_DATA = 1;

	private static final boolean isDebug = true;
	private static final String TAG = "MyDragContainer";
	private boolean isControlItem = false;//是否是操作子View

//	private CopyOnWriteArrayList<MyMoveWidget>                 highLowTempLists;//高温 最低温，中心点温度 集合
	private MyMoveWidget selectChild;
	private TempWidgetObj selectChildData;
	private CopyOnWriteArrayList<TempWidgetObj> userAddData;
//	private Bitmap                             pointBt, maxTempBt, minTempBt;//三张图片 ：单点温度、线和矩阵的 最高、最低温的图片

	private float WRatio, HRatio;//相机数据大小  与 显示屏幕大小的比值。


	private int           drawTempMode = -1;
	private WeakReference<Context> mContext;
	private float mDataNearByUnit = 0;// 距离数据源 的 单位，有多少个单位，以 矩形 为的感应区为一个单位

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

	private int screenWidth , screenHeight;//屏幕的宽高
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


//	protected void setBitMap(Bitmap point,Bitmap max , Bitmap min){
//		pointBt = point;maxTempBt = max;minTempBt = min;
//		invalidate();
//	}
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
		if (isDebug)Log.e(TAG, "DragTempContainer: ");
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

	public CopyOnWriteArrayList<TempWidgetObj> getUserAddData () {
		return userAddData;
	}


	/**
	 * <p>通过添加的 数据源列表，得到其中矩形的坐标的 int [] 数组。</p>
	 * <p>排布顺序，开始X，> 结束X， 开始Y，> 结束Y。</p>
	 * @return int [] 数组
	 */
	public int [] getAreaIntArray(){
		int areaNumber = 0;
		//得到矩形数据源列表中，矩形的数量
		for (TempWidgetObj child : userAddData){
			if (child.getType() ==3){
				areaNumber ++;
			}
		}
		//创建一个int数据保存 数据源列表中的矩阵坐标。一个矩形 四个坐标。
		int [] areaData = null;
		if (areaNumber > 0){
			areaData = new int[4*areaNumber];
		}else {
			return null;
		}
		int areaIndex =0 ;
		for (TempWidgetObj child : userAddData){
			if (child.getType() ==3){
				areaData[4 * areaIndex] = (int) (child.getOtherTemp().getStartPointX() / screenWidth * 256);
				areaData[4*areaIndex+1] = (int) (child.getOtherTemp().getEndPointX() / screenWidth * 256);
				areaData[4*areaIndex+2] = (int) (child.getOtherTemp().getStartPointY() / screenHeight * 192);
				areaData[4*areaIndex+3] = (int) (child.getOtherTemp().getEndPointY() / screenHeight * 192);
				areaIndex++;
			}
		}
		return areaData;
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
//		Log.e(TAG, "openHighTempAlarm:  {$valueHighTempAlarm  }  " + valueHighTempAlarm );
//		if (tempSuffixMode == 0){//0摄氏度， 1华氏度， 2开氏度
			valueHighTempAlarm = getFormatFloat(thresholdTemp);
//		}
//		if (tempSuffixMode == 1){
//			valueHighTempAlarm = getFormatFloat((thresholdTemp - 32) / 1.8f);
//		}
//		if (tempSuffixMode == 2){
//			valueHighTempAlarm = getFormatFloat((thresholdTemp - 273.15f));
//		}
		if (audioPlayer == null){
			audioPlayer = MediaPlayer.create(mContext.get(),R.raw.a2_ding);
		}
		if (audioPlayer != null){
			audioPlayer.setLooping(true);
		}
//
//		Log.e(TAG, "openHighTempAlarm:  {$valueHighTempAlarm  }  " + valueHighTempAlarm );
	}
	public void closeHighTempAlarm(){
		highTempAlarmToggle = false;
		invalidate();

		if (audioPlayer != null){
//			audioPlayer.set
			audioPlayer.stop();
			audioPlayer.release();
			audioPlayer = null;
		}

	}

	private OnUserCRUDListener onUserCRUDListener;

	public OnUserCRUDListener getUserCRUDListener () {
		return onUserCRUDListener;
	}

	public void setUserCRUDListener (OnUserCRUDListener userCRUDListener) {
		this.onUserCRUDListener = userCRUDListener;
	}

	/**
	 * 用户添加的 子View 添加，删除，查找，移动，监听器
	 */
	public interface OnUserCRUDListener{
		/**
		 * 添加 子测温数据
		 * @return
		 */
		boolean onAddChildView();

		/**
		 * 删除 子测温数据
		 * @return
		 */
		boolean onDeleteChildView();
		/**
		 * 获取所有的 子测温数据
		 * @return
		 */
		void onGetAllChildView();
		/**
		 * 子测温数据 更改
		 * @return
		 */
		void onChildViewChanged();
	}

	public interface OnChildToolsClickListener{
		/**
		 * 子View 工具栏被点击回调函数
		 * @param childObj 被点击的子View 数据源
		 * @param position 被点击工具栏的 坐标（从0开始）
		 */
		void onChildToolsClick(TempWidgetObj childObj, int position);

		/**
		 * 数据源中  矩形的数量发生变化回调
		 */
		void onRectChangedListener();

		/**
		 * 清除所有数据源 回调
		 */
		void onClearAllListener();
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
//		userAdd = new CopyOnWriteArrayList<>();
		userAddData = new CopyOnWriteArrayList<>();
//		highLowTempLists = new CopyOnWriteArrayList<>();//初始化高低中心点 温度 追踪集合
		tempSource = new float[256*196+10];
		minAddWidgetWidth = minAddWidgetHeight = DensityUtil.dp2px(mContext.get(),70);//最小为50个dp

//		pointBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
//		minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorblue);
//		maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorred);
		//init MediaPlayer
//		if (audioPlayer == null){
//			audioPlayer = MediaPlayer.create(mContext.get(),R.raw.a2_ding);
//			audioPlayer.setLooping(true);
//		}
		mDataNearByUnit = DensityUtil.dp2px(mContext.get(),10);
	}



	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
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

	/**
	 * 创建区域检查
	 * 打开逻辑：userAdd中，无区域检查的矩形，则添加，否则，拿到矩形的列表 穿给控件。
	 * 取消逻辑：控件层取消
	 */
	public List<TempWidgetObj> openAreaCheck(int width,int height){
		List<TempWidgetObj> recList = new ArrayList<>();
		for (TempWidgetObj child : userAddData){
			if (child.getType()==3){
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
			tempWidget.addToolsBp(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_areacheck_tools_delete));
			tempWidget.setOtherTemp(otherTempWidget);

//			MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),tempWidget,screenWidth,screenHeight);
//			moveWidget.setChildToolsClickListener(mChildToolsClickListener);
//			//todo 重置userAddView的集合
//			addView(moveWidget);
			userAddData.add(tempWidget);
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

	public int getTempSuffixMode () {
		return tempSuffixMode;
	}

	//更新点的温度
	private float updatePointTemp(float x , float y){
		//数据源上的坐标
		int index =  (10 + (int)(y*HRatio) * 256 + (int)(x*WRatio));
		return tempSource[index];
	}
	/**
	 * 更新线L，矩形Rec 上的最高最低温度
	 * @param tempWidget 绘制的内容 对象
	 * @param type 类型 2 线， 3 矩形
	 */
	private void updateLRTemp(OtherTempWidget tempWidget , int type){
//		Log.e(TAG, "updateLRTemp: ");
		//更新线，矩形的温度 及其坐标
		int startX ,startY ,endX ,endY, minIndex, maxIndex;//数据源上的边界点
		//将屏幕上的点 转换成数据源上的点
		startX =  (int)(tempWidget.getStartPointX()*WRatio);startY = (int)(HRatio*tempWidget.getStartPointY());
		endX =  (int)(tempWidget.getEndPointX()*WRatio); endY = (int)(HRatio*tempWidget.getEndPointY());
//		Log.e(TAG, "updateLRTemp: startX = " + startX + " startY " + startY + " endX " + endX  + " endY" + endY);
		//最低最高温度。及其坐标
		//用什么去记录 最高点 最低点时候的xy值。切记要加上前置的10
		int LRMinTempX,LRMinTempY, LRMaxTempX,LRMaxTempY;
		LRMinTempX = startX;LRMinTempY = startY;LRMaxTempX = startX;LRMaxTempY = startY;//默认值
		if (type == 3){
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
		}
		if (type == 2) {
			//斜率
			float k = ((float) endY - startY)/(endX - startX);
			float b = startY - k*startX;
//			Log.e(TAG, "updateLRTemp:  b===== > " + b + " startX  == > " + startX + " startY ==>  " + startY);
//			Log.e(TAG, "updateLRTemp: kkk =====>  " + k  + " === " + Math.round(k) );
			int pointy = startY;//临时存储 最高温 最低温的Y值
			for (int j = startX; j <= endX ; j++){//宽度遍历
				pointy = Math.round(k * j + b);
				if (tempSource[ (j+(pointy*256)+10)] <= tempSource[ (LRMinTempX+(LRMinTempY*256)+10)]){
					LRMinTempX = j;
					LRMinTempY = pointy;
				}
				if (tempSource[(j+(pointy*256)+10)] >= tempSource[ (LRMaxTempX+(LRMaxTempY*256)+10)]){
					LRMaxTempX = j;
					LRMaxTempY = pointy;
				}
			}
		}
		tempWidget.setMinTempX((LRMinTempX/WRatio));
		tempWidget.setMinTempY((LRMinTempY/HRatio));
		//线  和 矩形的  最高最低温  都是带单位及 单位符号的 String 类型
		tempWidget.setMinTemp(getTempStrByMode(tempSource[(LRMinTempX + (LRMinTempY*256) + 10)]));
		tempWidget.setMaxTempX((LRMaxTempX/WRatio));
		tempWidget.setMaxTempY((LRMaxTempY/HRatio));
		//线  和 矩形的  最高最低温  都是带单位及 单位符号的 String 类型
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
	 * 校正线模式、矩形模式 绘制的点。
	 * 线模式。左右分割，左边的点为起始点，右边点为结束点。结束点旁边绘制工具栏
	 * 矩形模式：左上为起始点，右下为结束点。工具栏的方位根据整体的 边距去 绘制
	 * @param type
	 * @return 传入的坐标是否可用。 默认为可用。矩形长宽 低于最小值不可用
	 */
	private boolean reviseCoordinate(int type){
		int min ,max ;
		//调整 起始点的坐标
		if (type ==2){
			if (startPressX > endPressX){//起点在右边，则交换两者的 位置
				min = endPressX;
				max = endPressY;
				endPressX = startPressX;
				endPressY = startPressY;
				startPressX = min;
				startPressY = max;
			}
			if (Math.sqrt((endPressY - startPressY)*(endPressY - startPressY) +
					(endPressX - startPressX)*(endPressX - startPressX)) < minAddWidgetWidth){
				return false;
			}

		}else if (type ==3){
			min = Math.min(screenWidth,Math.min(startPressX,endPressX));
			max = Math.min(screenWidth,Math.max(startPressX,endPressX));
			startPressX = min;
			endPressX = max;
			min = Math.min(screenHeight,Math.min(startPressY,endPressY));
			max = Math.min(screenHeight,Math.max(startPressY,endPressY));
			startPressY = min;
			endPressY = max;

			//调整长度，使之符合标准
			//type 2/3 校正X坐标
			if (Math.abs(endPressX - startPressX) < minAddWidgetWidth){//X 轴 长度不够
				if(startPressX > screenWidth - minAddWidgetWidth){//起点X坐标 距离右边界 不足 最小宽度
					endPressX = screenWidth ;
					startPressX  = screenWidth - minAddWidgetWidth;
				} else {//起点X坐标 距离右边界 比 最小宽度 多
					endPressX = startPressX + minAddWidgetWidth;
				}
			}
			//type 2/3 校正X坐标
			if (Math.abs(endPressY - startPressY) < minAddWidgetHeight){
				if(startPressY > screenHeight - minAddWidgetHeight){
					endPressY = screenHeight ;
					startPressY  = screenHeight - minAddWidgetHeight;
				} else {
					endPressY = startPressY + minAddWidgetHeight;
				}
			}
		}else {
			return false;
		}
		//明确 起始点的边界值
		startPressX = Math.max(0,Math.min(startPressX ,screenWidth));
		startPressY = Math.max(0,Math.min(startPressY ,screenHeight));
		endPressX = Math.max(0,Math.min(endPressX ,screenWidth));
		endPressY = Math.max(0,Math.min(endPressY ,screenHeight));


		Log.e(TAG, "reviseCoordinate:  Math" + " SX = > "+ startPressX + " SY => " + startPressY + " , EX = > " + endPressX + " EY = > " + endPressY);
		Log.e(TAG, "reviseCoordinate: minAddWidgetWidth " + minAddWidgetWidth  + " minAddWidgetHeight " + minAddWidgetHeight);

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
		if (value <Float.MAX_VALUE || value > Float.MIN_VALUE){
			return Float.parseFloat(df.format(value));
		}else {
			return Float.NaN;
		}
	}

	/**
	 * 删除所有添加的测温控件。
	 */
	public void clearAll(){
//		for (MyMoveWidget widget: userAddData){
//			removeView(widget);
//		}
		userAddData.clear();
		pointNum = 0;
		removeAllViews();
		selectChild = null;

		if (mChildToolsClickListener!=null){
			mChildToolsClickListener.onClearAllListener();
		}

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
		widget.addToolsBp(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_areacheck_tools_delete));
		widget.setPointTemp(pointTempWidget);

//
		resetUserAddView(widget);
		drawTempMode = -1;
	}

	//添加 线或者矩形 测温模式
	private void createLineOrRecView(){
//		Log.e(TAG, "reviseCoordinate: " + reviseCoordinate(drawTempMode));
//		if (drawTempMode ==3)reviseCoordinate(drawTempMode)
		if (reviseCoordinate(drawTempMode)){//线  和 矩形 都要通过 校正
			TempWidgetObj tempWidget = new TempWidgetObj();
			//校正起始点坐标
			OtherTempWidget otherTempWidget = new OtherTempWidget();
			Log.e(TAG, "createLineOrRecView: " + " SX = > "+ startPressX + " SY => " + startPressY + " , EX = > " + endPressX + " EY = > " + endPressY);
			otherTempWidget.setStartPointX(startPressX);
			otherTempWidget.setStartPointY(startPressY);
			otherTempWidget.setEndPointX(endPressX);
			otherTempWidget.setEndPointY(endPressY);

			updateLRTemp(otherTempWidget,drawTempMode);

			tempWidget.setType(drawTempMode);
			tempWidget.setCanMove(true);
			tempWidget.setSelect(false);
			tempWidget.setTempTextSize(20);
			tempWidget.addToolsBp(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_areacheck_tools_delete));
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
		TempWidgetObj firstTypeChild  = null;//添加数据类型的 第一个对象
		int typeCount = 0;
		//记录第一个同类型的View,并累计有多少个
		for (TempWidgetObj child : userAddData){
			if (type == child.getType() && firstTypeChild ==null){
				firstTypeChild = child;
			}
			if (type == child.getType()){
				typeCount++;
			}
		}
		switch (type){
			case 1:
				if (typeCount == POINT_MAX_NUMBER){
//					removeView(firstTypeChild);
					userAddData.remove(firstTypeChild);
				}
				addChild(childData);
				break;
			case 2:
				if (typeCount == LINE_MAX_NUMBER){
//					removeView(firstTypeChild);
					userAddData.remove(firstTypeChild);
				}
				addChild(childData);
				break;
			case 3:
				if (typeCount == RECTANGLE_MAX_NUMBER){
//					removeView(firstTypeChild);
					userAddData.remove(firstTypeChild);
				}
				addChild(childData);
				break;
		}
	}

	private void addChild(TempWidgetObj childData){
//		MyMoveWidget moveWidget = new MyMoveWidget(mContext.get(),childData,screenWidth,screenHeight);
////		moveWidget.setBackgroundColor(getResources().getColor(R.color.max_temp_text_color_red));
//		moveWidget.setChildToolsClickListener(mChildToolsClickListener);
		//todo 重置userAddView的集合
//		addView(moveWidget);
		userAddData.add(childData);
		if (onUserCRUDListener!=null){
			onUserCRUDListener.onAddChildView();
		}

	}

	/**
	 * 通过点击事件，得到位置上是否有子View。
	 * @param event 点击事件
	 * @return 是否有子View
	 */
//	private boolean getEventInChild(MotionEvent event){
//		boolean isOperate = false;
//		for (MyMoveWidget child : userAdd){
//			isOperate = isOperate | (event.getX() >= child.getLeft() && event.getX() <= child.getRight()
//					&& event.getY() >= child.getTop() && event.getY() <= child.getBottom());
//		}
//		return isOperate;
//	}

//	/**
//	 * 判断是否有选中状态的子View
//	 * @return 是否有选中的子View
//	 */
//	private boolean hasSelectChild(MotionEvent event){
//		boolean hasSelectChild = false;
//		for (MyMoveWidget child : userAdd){
//			hasSelectChild = hasSelectChild|child.isSelectedState();
//		}
//		return hasSelectChild;
//	}


//	private MyMoveWidget getSelectChildByEvent(MotionEvent event){
//		MyMoveWidget children = null;
//		for (MyMoveWidget child : userAdd){
//			if (child.isSelectedState() && (event.getX() >= child.getLeft() && event.getX() <= child.getRight()
//					&& event.getY() >= child.getTop() && event.getY() <= child.getBottom())){
//				children = child;
//			}
//		}
//		return children;
//	}

//	/**
//	 * 通过点击的坐标。得到选中的子View（返回的是最后一个此范围的控件。效果不对）
//	 * @param event
//	 * @return
//	 */
//	private MyMoveWidget getSelectChildByMotionEvent(MotionEvent event){
//		MyMoveWidget select = null;
//		for (MyMoveWidget child : userAdd){
//
//			if (event.getX() >= child.getLeft() && event.getX() <= child.getRight()
//					&& event.getY() >= child.getTop() && event.getY() <= child.getBottom()){
//				select = child;
//			}
//		}
//		return select;
//	}
	/**
	 * 按下的事件点 是否在选中的控件中
	 * 事件在 已选的框内，则越过所有事件 直接给子View消费事件。
	 */
	private boolean pressInSelectView(MotionEvent event){
		boolean flag = false;
//			Log.e(TAG, "getSelectChildByMotionEvent: chile left " + child.getLeft() + " right " + child.getRight() +
		//			" top " + child.getTop()+ " bottom " + child.getBottom());
		if (selectChild != null){
			flag = ((event.getX() >= selectChild.getLeft() && event.getX() <= selectChild.getRight()
					&& event.getY() >= selectChild.getTop() && event.getY() <= selectChild.getBottom()));
		}
		Log.e(TAG, "pressInSelectView:  =====> " + flag);
		return flag;
	}

	/**
	 * 按下的点，是否在 添加的 数据源感应区（左右多少个像素） 内
	 * @param event
	 * @return
	 */
	private TempWidgetObj pressInUserAddData(MotionEvent event){
		TempWidgetObj result = null;
		for (TempWidgetObj childData : userAddData){
			switch (childData.getType()){
				case 1://点
					if ((event.getX() < childData.getPointTemp().getStartPointX() + mDataNearByUnit*3.0f) && (event.getX() > childData.getPointTemp().getStartPointX() - mDataNearByUnit*3.0f)
							&& (event.getY() < childData.getPointTemp().getStartPointY() + mDataNearByUnit*3.0f) && (event.getY() > childData.getPointTemp().getStartPointY() - mDataNearByUnit*3.0f)){
						result = childData;
					}
					break;
				case 2://线
					float k = (childData.getOtherTemp().getEndPointY() - childData.getOtherTemp().getStartPointY())
							/(childData.getOtherTemp().getEndPointX() - childData.getOtherTemp().getStartPointX());
					float kb = childData.getOtherTemp().getStartPointY() - k * childData.getOtherTemp().getStartPointX() ;
					//点击的点 在绘制的线 周围 20个像素范围内
					if (Math.abs(kb - (event.getY() - k* event.getX())) < mDataNearByUnit*2.0f){
						result = childData;
					}
					break;
				case 3://矩形
					if ((event.getX() > childData.getOtherTemp().getStartPointX() - mDataNearByUnit)
							&& (event.getX() < childData.getOtherTemp().getEndPointX() + mDataNearByUnit)
							&& (event.getY() > childData.getOtherTemp().getStartPointY() - mDataNearByUnit)
							&& (event.getY() < childData.getOtherTemp().getEndPointY() + mDataNearByUnit)){
						result = childData;
					}
					break;
			}
		}
		return result;
	}

	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		if (!enabled){
			return true;
		}
		return super.dispatchTouchEvent(ev);
	}

	private Timer     mTimer = null;
	private TimerTask mSelect_TimeTask  ;
	private TimerTask mUnSelect_TimeTask ;
	private static final int TO_SELECT = 2;
	private static final int TO_UNSELECT = 3;
	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what){
				case TO_SELECT:
					selectChild = new MyMoveWidget(mContext.get(),selectChildData ,screenWidth,screenHeight);
					selectChild.setSelectedState(true);
					selectChild.setChildToolsClickListener(mChildToolsClickListener);
					addView(selectChild);
					pressSelectNotUp = true;
					break;
				case TO_UNSELECT:
					setAllChildUnSelect();
					mChildToolsClickListener.onRectChangedListener();
					break;
			}
			return true;
		}
	});

	@Override
	public boolean onInterceptTouchEvent (MotionEvent ev) {
//		if (isDebug)Log.e(TAG, "Parent onIntercept: x " + ev.getX() + " Y = " + ev.getY() + " action= " + ev.getAction() );
		//不添加测温模式的情况下。
		if (ev.getAction() == MotionEvent.ACTION_UP ){
			pressSelectNotUp = false;
		}
		if (drawTempMode == -1){
			//判定事件 是否在  已选中子View ，则分发给子View
			if (pressInSelectView(ev)){
				if (ev.getAction() == MotionEvent.ACTION_DOWN ){
//					Log.e(TAG, "onInterceptTouchEvent: ==========mUnSelect_Task.cancel");
					if (selectChild !=null && mUnSelect_TimeTask != null){
//						mHandler.removeMessages(TO_UNSELECT);
						mUnSelect_TimeTask.cancel();
//						mTimer.purge();
					}
				}

				if (ev.getAction() == MotionEvent.ACTION_UP){
					if (selectChild !=null) {
						mUnSelect_TimeTask = new TimerTask() {
							@Override
							public void run () {
//								Log.e(TAG, "====DragTempContainer==TO_UNSELECT==TimerTask进来了");
								Message message = Message.obtain();
								message.what = TO_UNSELECT;
								mHandler.sendMessage(message);
							}
						};
						mTimer.schedule(mUnSelect_TimeTask, 2000);
					}
				}
				return super.onInterceptTouchEvent(ev);
			}else {// 事件 不在 已选中的View 中，交给当前的 onTouchEvent处理
//				if (mTimer!=null) mTimer.cancel();
				setAllChildUnSelect();
				mChildToolsClickListener.onRectChangedListener();
				return true;
			}
		}else {//含有绘制模式 。交给当前 onTouchEvent 处理
			//true 拦截给自己的onTouchEvent处理 。
			return true;
		}
	}

	private boolean pressSelectNotUp = false ; // 按下选中，没有弹起

	/**
	 * 选择绘制模式后，去绘制提示线的 监听器。
	 */
	public interface onAddChildDataListener{
		/**
		 * 移动事件回调
		 */
		void onIsEventActionMove(DrawLineRecHint hint);

		/**
		 * 弹起时间回调。取消绘制提示线
		 */
		void onIsEventActionUp(DrawLineRecHint hint);
	}

	public onAddChildDataListener getAddChildDataListener () {
		return addChildDataListener;
	}
	public void setAddChildDataListener (onAddChildDataListener addChildDataListener) {
		this.addChildDataListener = addChildDataListener;
		drawHint = new DrawLineRecHint();
	}

	private onAddChildDataListener addChildDataListener;
	private DrawLineRecHint        drawHint ;



	@Override
	public boolean onTouchEvent (MotionEvent event) {
//		if (isDebug)Log.e(TAG, "DragTempContainer onTouchEvent: mode ==> " + drawTempMode + " action ==> "+ event.getAction());
		KeyboardsUtils.hintKeyBoards(this);
		if (drawTempMode != -1){//添加控件 ,含有测温模式
			switch (event.getAction()){
				case MotionEvent.ACTION_DOWN:

//					if (drawTempMode != -1){
						startPressX = (int) event.getX();
						startPressY = (int) event.getY();

//					}
					if (drawTempMode!= 1){
						drawHint.setStartXCoordinate(startPressX);
						drawHint.setStartYCoordinate(startPressY);
						drawHint.setDrawTempMode( drawTempMode);

					}
					break;
				case MotionEvent.ACTION_MOVE:
//					if (drawTempMode != -1) {
						endPressX = (int) event.getX();
						endPressY = (int) event.getY();
					if (drawTempMode != 1){
						drawHint.setEndXCoordinate(endPressX);
						drawHint.setEndYCoordinate(endPressY);
						drawHint.setNeedDraw(true);
						addChildDataListener.onIsEventActionMove(drawHint);
					}
//					}
					break;
				case MotionEvent.ACTION_UP:
//					if (drawTempMode != -1) {
					if (addChildDataListener !=null){
						drawHint.setNeedDraw(false);
						addChildDataListener.onIsEventActionUp(drawHint);
					}

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
		}else {//不含测温模式 添加。drawTempMode = -1， 触发的事件 不在已选中的子View
			// 事件 是否在 数据源 感应区中
			if (pressInUserAddData(event) == null){
				//删除选中View
				setAllChildUnSelect();

			}else {// 事件 在 数据源 的感应区中
				//倒计时：
				switch (event.getAction()){
					case MotionEvent.ACTION_DOWN:
						if (selectChild == null){
							selectChildData = pressInUserAddData(event);
							if (mTimer == null)
								mTimer = new Timer();
							//去设置选中的  任务
							mSelect_TimeTask = new TimerTask() {
								@Override
								public void run() {
//									Log.e(TAG, "====DragTempContainer==TO_SELECT==Task进来了");
									Message message = Message.obtain();
									message.what = TO_SELECT;
									mHandler.sendMessage(message);
								}
							};
							mTimer.schedule(mSelect_TimeTask, 800);
						}
						break;
					case MotionEvent.ACTION_UP:
						if (selectChild == null){
//							Log.e(TAG, "onInterceptTouchEvent: =ACTION_UP=========TO_SELECT Task.cancel");
//							mHandler.removeMessages(TO_SELECT);
							mSelect_TimeTask.cancel();
						}else {
							mUnSelect_TimeTask = new TimerTask() {
								@Override
								public void run() {
//									Log.e(TAG, "====DragTempContainer==TO_UNSELECT==TimerTask进来了");
									Message message = Message.obtain();
									message.what = TO_UNSELECT;
									mHandler.sendMessage(message);
								}
							};
							mTimer.schedule(mUnSelect_TimeTask, 2000);
						}
						break;
					case MotionEvent.ACTION_MOVE:
						if (pressSelectNotUp && selectChild != null){
//							selectChild.
						}
						break;
				}
			}
		}
		//消费事件
		return true;
	}


	/**
	 * 设置所有的子控件都未选中
	 */
	public void setAllChildUnSelect(){
//		if (isDebug)Log.e(TAG, "Parent setAllChildUnSelect: ");
//		if (userAddData.size() > 0){
//			for (TempWidgetObj child : userAddData){
////				child.setSelectedState(false);
//			}
//		}

		if (selectChild != null){
			selectChild.setSelectedState(false);
//			removeView(selectChild);
			removeAllViews();
			selectChild = null;
		}
	}

	public void deleteChildView(TempWidgetObj childData){
		userAddData.remove(childData);
		removeView(selectChild);
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
				//超温警告 打开了开关，并且超温了。没帧刷新数据
				if (isAboveHighTemp && highTempAlarmToggle){
					lastAboveTime = System.currentTimeMillis();//最后一次播放的时间
					if (audioPlayer != null && !audioPlayer.isPlaying()){
						audioPlayer.start();
					}
//					Log.e(TAG, "handleMessage: " + audioPlayer.isPlaying());
				}else {//一段时间（X）后停止音乐的播放，如果音乐是在播放，则停止。
					if (((System.currentTimeMillis() - lastAboveTime)/1000) > 3
							&& audioPlayer != null && audioPlayer.isPlaying()){//时间可以更改
						audioPlayer.pause();
					}
				}
//				invalidate();//重新绘制

				if (mSeekBar!=null){//更新滑动温度条
					mSeekBar.Update(tempSource[3],tempSource[6]);
				}
//				//刷新高低温追踪的数据
//				if (highLowTempLists.size()!=0){
//					for (MyMoveWidget myMoveWidget : highLowTempLists){
//						if (myMoveWidget.getView().getPointTemp().getType() == 1){
//							myMoveWidget.getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[1]));
//							myMoveWidget.getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[2]));
//							myMoveWidget.getView().getPointTemp().setTemp(getTempStrByMode(tempSource[3]));
//						}
//						if (myMoveWidget.getView().getPointTemp().getType()==2){
//							myMoveWidget.getView().getPointTemp().setStartPointX((int) getXByWRatio(tempSource[4]));
//							myMoveWidget.getView().getPointTemp().setStartPointY((int) getYByHRatio(tempSource[5]));
//							myMoveWidget.getView().getPointTemp().setTemp(getTempStrByMode(tempSource[6]));
//						}
//						if (myMoveWidget.getView().getPointTemp().getType() == 3){
//							myMoveWidget.getView().getPointTemp().setTemp(getTempStrByMode(tempSource[0]));
//						}
//						myMoveWidget.requestLayout();
//						myMoveWidget.invalidate();
//					}
//				}
					// 刷新 底层绘制 数据源
				if (userAddData.size()!= 0){
					for (int i = 0 ; i < userAddData.size(); i++){
						//todo 更新每一个的 温度信息。
						if (userAddData.get(i).getType()==1){//点
							float temp = updatePointTemp(userAddData.get(i).getPointTemp().getStartPointX(),userAddData.get(i).getPointTemp().getStartPointY());
							userAddData.get(i).getPointTemp().setTemp(getTempStrByMode(temp));
						}else {//线及其矩形
							updateLRTemp(userAddData.get(i).getOtherTemp(),userAddData.get(i).getType());
						}
//						userAdd.get(i).requestLayout();
					}
				}
				//刷新已选 子View的数据
					if (selectChild != null){
						if (selectChildData.getType() ==1){
							float temp = updatePointTemp(selectChildData.getPointTemp().getStartPointX(),selectChildData.getPointTemp().getStartPointY());
							selectChildData.getPointTemp().setTemp(getTempStrByMode(temp));
						}else {
							updateLRTemp(selectChildData.getOtherTemp(),selectChildData.getType());
						}
						selectChild.requestLayout();
					}



				break;
			}
			return false;
		}
	}) ;
}
