package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.dyt.wcc.common.R;
import com.dyt.wcc.common.utils.DensityUtil;

import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/27  9:56     </p>
 * <p>Description：三类可以拖动的View</p>
 * <p>PackagePath: com.dyt.wcc.common.widget     </p>
 */
public class MyMoveWidget extends View {
	private static final boolean isDebug = true;
	private static final String TAG = "MyMoveWidget";

	private Bitmap maxTempBt, minTempBt,centerTempBt;//最小温度，最大温度图片（单点只有最小温度的图片）
	private TempWidgetObj tempWidgetData;//数据源

	private int mMinHeight;//最小高度像素点个数   //矩阵类型独有
	private int mMinWidth;//最小宽度像素点个数    //矩阵和线独有
	//矩形描边的长度为 最小宽度的一半 除以3，厚度为固定值8

	private boolean isShowBg = false;//是否显示背景

	private TextPaint pointTextPaint, maxTempTextPaint, minTempTextPaint , centerTempTextPaint;//画笔：点文字、最高最低文字、 中心点文字
	private Paint pointPaint ,linePaint;//画笔：画点图片 、绘制线矩形的线条画笔
	private Paint bgRoundPaint,recZoomBox;//绘制背景画笔，绘制矩形八个方位的画笔
	private int recZoomBoxPaintStroke;

	private Context mContext;

	private int padLeft, padRight, padTop ,padBottom;//内容布局的四周margin

	private int moveMaxWidth;//能移动的最大宽度和高度（即父控件的长宽）
	private int moveMaxHeight;
	private boolean hasBackGroundAndTools = false;

	private RectF pointBgRectF;
	private float wMeasureSpecSize, hMeasureSpecSize;
	//todo  状态应提取出去
	//八个方位
	public static final int WIDGET_DIRECTION_STATE_LEFT = 0x000;
	public static final int WIDGET_DIRECTION_STATE_RIGHT = 0x001;
	public static final int WIDGET_DIRECTION_STATE_TOP = 0x010;
	public static final int WIDGET_DIRECTION_STATE_BOTTOM = 0x011;
	public static final int WIDGET_DIRECTION_STATE_LEFT_TOP = 0x100;//256
	public static final int WIDGET_DIRECTION_STATE_LEFT_BOTTOM = 0x101;//257
	public static final int WIDGET_DIRECTION_STATE_RIGHT_TOP = 0x110;//272
	public static final int WIDGET_DIRECTION_STATE_RIGHT_BOTTOM = 0x111;//273

	//内容的矩形、内容背景矩形、 工具图片绘制的矩形、 工具图片的背景 矩形、 文字矩形
	private RectF rectContent , rectContentBg , rectTool , rectToolsBg , textRectBg;

	private int tempLocationState = WIDGET_DIRECTION_STATE_LEFT_TOP, toolsLocationState = WIDGET_DIRECTION_STATE_LEFT_TOP, widgetLocationState;//文字 和 工具 绘制所处于的状态

	private float contentLeft ,contentRight, contentTop, contentBottom;
	private float contentBgLeft ,contentBgRight, contentBgTop, contentBgBottom;

	private float textNeedWidth ,textNeedHeight  ,  toolsNeedWidth , toolsNeedHeight;

	private float strLeft,strRight, strTop, strBottom;
	private float toolsBgLeft,toolsBgRight, toolsBgTop, toolsBgBottom;

	private String minTempStr , maxTempStr;//记录最小最高温度
	private float xOffset, yOffset;

	private float pressDownX,pressDownY;

	private DragTempContainer.OnChildToolsClickListener mChildToolsClickListener;
	public void setChildToolsClickListener (DragTempContainer.OnChildToolsClickListener childToolsClickListener) {
		this.mChildToolsClickListener = childToolsClickListener;
	}

	public MyMoveWidget (Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}
	public MyMoveWidget (Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr);
		mContext = context;
		initView();
		initPaint();
	}
	public MyMoveWidget(Context context, TempWidgetObj view , int maxWidth, int maxHeight){
		super(context);
		mContext = context;
		tempWidgetData = view;
		moveMaxWidth = maxWidth;
		moveMaxHeight = maxHeight;
		setClickable(true);

//		Log.e(TAG, "MyMoveWidget: w=====================> " + moveMaxWidth +" h == " +moveMaxHeight);

		initView();
		initPaint();

		initData();
	}

	private void initView(){
		padTop = padBottom= padLeft = padRight = 14;//设置背景间距,动态计算。不同dpi有明显差异  3DP
		mMinHeight = mMinWidth = 15*padLeft;//矩形的最小宽高等于五倍pad
		recZoomBoxPaintStroke = DensityUtil.dp2px(mContext,5) ;
	}
	private Timer mTimer = null;
	private TimerTask mTimeTask;
	private boolean changeSelectState = false;
	private static final int TO_CHANGE_SELECT = 11;
	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what){
				case TO_CHANGE_SELECT:
					if (tempWidgetData.isCanMove()){
						tempWidgetData.setSelect(true);
						requestLayout();
					}
					break;
			}
			return false;
		}
	});

	public TempWidgetObj gettempWidgetData () {
		return tempWidgetData;
	}

	public void settempWidgetData (TempWidgetObj tempWidgetData) {
		this.tempWidgetData = tempWidgetData;
	}

	private void initPaint(){
//		if (isDebug)Log.e(TAG, "initPaint: ");
		pointPaint = new Paint();
		pointPaint.setColor(getResources().getColor(R.color.bg_preview_toggle_select));
		linePaint = new Paint();
		linePaint.setStrokeWidth(DensityUtil.dp2px(mContext,3));
		linePaint.setColor(getResources().getColor(R.color.teal_200));

		pointTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		pointTextPaint.setStrokeWidth(2);
		pointTextPaint.setTextSize(DensityUtil.dp2px(mContext,tempWidgetData.getTempTextSize()));
		pointTextPaint.setColor(getResources().getColor(R.color.bg_preview_toggle_select,null));
		pointTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

		if (tempWidgetData.getType()==1){
			if (tempWidgetData.getPointTemp().getType()==1){
				minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorred);
				pointTextPaint.setColor(getResources().getColor(R.color.max_temp_text_color_red,null));
			}else if (tempWidgetData.getPointTemp().getType()==2){
				minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorblue);
				pointTextPaint.setColor(getResources().getColor(R.color.min_temp_text_color_blue,null));
			}else {
				minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
				maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
			}
		}else {
			tempWidgetData.setCanMove(true);
			minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorblue);
			maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorred);
		}

		maxTempTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		maxTempTextPaint.setTextSize(DensityUtil.dp2px(mContext,tempWidgetData.getTempTextSize()));
		maxTempTextPaint.setColor(getResources().getColor(R.color.max_temp_text_color_red));
		maxTempTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

		minTempTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		minTempTextPaint.setTextSize(DensityUtil.dp2px(mContext,tempWidgetData.getTempTextSize()));
		minTempTextPaint.setColor(getResources().getColor(R.color.min_temp_text_color_blue));
		minTempTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

		bgRoundPaint = new Paint();
		bgRoundPaint.setStyle(Paint.Style.FILL);
		bgRoundPaint.setColor(getResources().getColor(R.color.bg_move_layout_round_bg));
		bgRoundPaint.setAlpha(150);//透明度 0透明-255不透明
		bgRoundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);//抗锯齿

		recZoomBox = new Paint();
		recZoomBox.setColor(getResources().getColor(R.color.white));
		Log.e(TAG, "initPaint: recZoomBoxPaintStroke = " + recZoomBoxPaintStroke);
		recZoomBox.setStrokeWidth(recZoomBoxPaintStroke);
	}

	public void dataUpdate(TempWidgetObj obj){
		tempWidgetData = obj;
		initData();
		invalidate();
	}

	/**
	 * 初始化数据
	 * 坐标：Coordinate  方位： Direction
	 * todo 流程： 初始化之后 根据类型去计算：
	 * todo 1，计算内容的坐标 背景的坐标， 工具栏坐标  文字坐标
	 * todo 绘制的时候判定类型 ，及其是够可选  及 是否已选
	 * todo 触碰 判断
	 */
	private void initData(){
//		Log.e(TAG, "initData: =============");
		//工具栏是否为空，为空时默认添加一个删除按钮。
		if (tempWidgetData.getToolsPicRes() ==null){
			tempWidgetData.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete});
		}
		if (!tempWidgetData.isCanMove()){//不管什么类型，不能移动绝对不能选中
			tempWidgetData.setSelect(false);
		}
		hasBackGroundAndTools = tempWidgetData.isCanMove()&& tempWidgetData.isSelect();//初始化是否有工具栏及其背景
		//√计算工具栏的所需宽高
		toolsNeedWidth = DensityUtil.dp2px(mContext,(DragTempContainer.perToolsWidthHeightSet + DragTempContainer.perToolsMargin*2));
		toolsNeedHeight = tempWidgetData.getToolsNumber() * toolsNeedWidth;//包含了 margin
		//有无工具栏情况下，以左上角为坐标原点的 内容及其背景的坐标。确定工具栏的方位
		getContentAndBgCoordinate(tempWidgetData,minTempBt);

		//得到文字的所需宽高,并计算文字方位
		switch (tempWidgetData.getType()){
			case 1://点
				//点温度String
				minTempStr = tempWidgetData.getPointTemp().getTemp();
				// 通过方位及其 内容坐标或 文字的坐标设置 ，温度文字基准绘制点
				textNeedWidth = (int) getStrWidth(pointTextPaint,minTempStr);
				textNeedHeight = (int) getStrHeight(pointTextPaint,minTempStr);
//				Log.e(TAG, "initData: "+ textNeedHeight);
				//得到文字绘制方位
				int strDirection = getStrDirection(WIDGET_DIRECTION_STATE_RIGHT_BOTTOM,textNeedWidth,textNeedHeight);

				getStrCoordinate(textNeedWidth,textNeedHeight,strDirection,tempWidgetData.getType());
				//文字方向时 工具方位，计算出内容的坐标值
				tempWidgetData.getPointTemp().setTempDirection(strDirection);//保存当前文字绘制的方位
				//求完内容 背景内容 工具栏文字方位，及其坐标

				doMeasure();
				updateContentCoordinate();//更新所有的坐标
				break;
			case 2://线
			case 3://矩形
				minTempStr = tempWidgetData.getOtherTemp().getMinTemp();
				maxTempStr = tempWidgetData.getOtherTemp().getMaxTemp();

				//todo 校正 最高温和最低温 是否在 线或 矩形的边界临界 延伸点

				doMeasure();
				updateContentCoordinate();//更新所有的坐标
				break;
			default://-1 或其他

				break;
		}
		rectContentBg = new RectF(contentBgLeft, contentBgTop, contentBgRight, contentBgBottom);
		rectToolsBg = new RectF(toolsBgLeft,toolsBgTop,toolsBgRight,toolsBgBottom);
		rectContent = new RectF(contentLeft,contentTop, contentRight,contentBottom);
		//得到了所有的宽高 则可以计算所需总宽高了
	}

	public float getWMeasureSpecSize () {
		return wMeasureSpecSize;
	}

	public float getHMeasureSpecSize () {
		return hMeasureSpecSize;
	}

	/**
	 * 计算中心偏移量
	 */
	protected float getXOffset(){
		return xOffset;
	}
	/**
	 * 计算中心偏移量
	 */
	protected float getYOffset(){
		return yOffset;
	}
	private float getStrWidth(@NonNull TextPaint textPaint,@NonNull String tempMsg){
		Rect tempRect = new Rect();
		textPaint.getTextBounds(tempMsg,0 , tempMsg.length(),tempRect);
		return tempRect.width();
	}
	private float getStrHeight(@NonNull TextPaint textPaint,@NonNull String tempMsg){
		Rect tempRect = new Rect();
		textPaint.getTextBounds(tempMsg,0 , tempMsg.length(),tempRect);
		return tempRect.height();
	}

	/**
	 * 通过使用内容的周边 计算出文字绘制的方位，参照物 带背景的内容
	 * 文字绘制规则：高度距离顶部或底部2个文字高度。
	 * @param defaultValue 默认值 （默认绘制方位）
	 * @param useWidth 需求的宽度
	 * @param useHeight 需求的高度
	 * @return 方位值：左上左下 右上右下
	 */
	private int getStrDirection(int defaultValue, float useWidth ,float useHeight){
		int direction = defaultValue;//文字默认在右底绘制
		if ((contentBgRight + useWidth <= moveMaxWidth)//direction_right_bottom
				&& (contentBgBottom + useHeight <= moveMaxHeight)){
			direction = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
		}else if ((contentBgRight + useWidth <= moveMaxWidth)//direction_right_top
				&& (contentBgTop - useHeight > 0)){
			direction = WIDGET_DIRECTION_STATE_RIGHT_TOP;
		}else if ((contentBgLeft - useWidth >= 0)//direction_left_bottom
				&& (contentBgBottom + useHeight <= moveMaxHeight)){
			direction = WIDGET_DIRECTION_STATE_LEFT_BOTTOM;
		}else if ((contentBgLeft - useWidth >= 0)//direction_left_top
				&& (contentBgTop - useHeight > 0)){
			direction = WIDGET_DIRECTION_STATE_LEFT_TOP;
		}
		return direction;
	}
	private void getStrCoordinate(float width ,float height ,int direction, int type){
		if (type==1){
			if (direction == WIDGET_DIRECTION_STATE_LEFT_TOP){
				strLeft = contentLeft - width;
				strRight = contentLeft;
				strTop = (contentTop + contentBottom )/2 - height;
				strBottom = (contentTop+ contentBottom)/2;
			}else if (direction == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
				strLeft = contentLeft - width;
				strRight = contentLeft;
				strTop = (contentTop+ contentBottom)/2;
				strBottom = (contentTop+ contentBottom)/2 + height;
			}else if (direction ==WIDGET_DIRECTION_STATE_RIGHT_TOP){
				strLeft = contentRight ;
				strRight = contentRight + width;
				strTop = (contentTop+ contentBottom )/2- height;
				strBottom = (contentTop+ contentBottom)/2;
			}else if (direction ==WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
				strLeft = contentRight;
				strRight = contentRight+ width;
				strTop = (contentTop+ contentBottom)/2;
				strBottom = (contentTop+ contentBottom)/2 + height;
			}
		}
	}

	/**
	 * 根据本地内容背景的四个边，及工具栏的宽高 确定工具栏绘制的位置
	 * 确定规则：以中心横线为基准去计算方位
	 * 一个Y轴的确定点
	 * @param toolsWidth 工具栏宽度
	 * @param toolsHeight   工具栏高度
	 * @param type 额外添加的距离，针对于绘制线 和矩形时 温度图片的边界在边缘
	 * @return 左上 左对齐 右上 右对齐
	 */
	private void getToolsDirection(float toolsWidth , float toolsHeight , int type){
		//tools Direction
//		if (type ==1 || type ==2){
			if ((contentBgRight + toolsWidth <moveMaxWidth) && ((contentBgTop + contentBgBottom)/2.0f - toolsHeight > 0)){               //tools right top
				toolsLocationState = WIDGET_DIRECTION_STATE_RIGHT_TOP;
			}else if ((contentBgRight +toolsWidth <moveMaxWidth)&&(contentBgTop + contentBgBottom)/2.0f+toolsHeight < moveMaxHeight){//tools right bottom
				toolsLocationState = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
			}else if ((contentBgLeft - toolsWidth > 0)&&((contentBgTop + contentBgBottom)/2.0f - toolsHeight > 0)){                   //tools left top
				toolsLocationState = WIDGET_DIRECTION_STATE_LEFT_TOP;
			}else if ((contentBgLeft - toolsWidth > 0)&&((contentBgTop + contentBgBottom)/2.0f + toolsHeight) < moveMaxHeight){       //tools left bottom
				toolsLocationState = WIDGET_DIRECTION_STATE_LEFT_BOTTOM;
			}
	}
	/**
	 *通过工具栏的方位可以得到控件工具栏的相对（相对于本控件的坐标原点）坐标
	 */
	private void getToolsCoordinate(){
		if (toolsLocationState == WIDGET_DIRECTION_STATE_LEFT_TOP){
			toolsBgLeft = contentBgLeft - toolsNeedWidth;
			toolsBgRight = contentBgLeft;
			toolsBgTop = (contentBgTop + contentBgBottom)/2 - toolsNeedHeight;
			toolsBgBottom =  (contentBgTop + contentBgBottom)/2;
		}else if (toolsLocationState == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
			toolsBgLeft = contentBgLeft - toolsNeedWidth;
			toolsBgRight = contentBgLeft;
			toolsBgTop = (contentBgTop + contentBgBottom)/2 ;
			toolsBgBottom = (contentBgTop + contentBgBottom)/2 + toolsNeedHeight;
		}else if (toolsLocationState == WIDGET_DIRECTION_STATE_RIGHT_TOP){
			toolsBgLeft = contentBgRight ;
			toolsBgRight = contentBgRight + toolsNeedWidth;
			toolsBgTop = (contentBgTop + contentBgBottom)/2 - toolsNeedHeight;
			toolsBgBottom =  (contentBgTop + contentBgBottom)/2;
		}else if (toolsLocationState == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
			toolsBgLeft = contentBgRight ;
			toolsBgRight = contentBgRight + toolsNeedWidth;
			toolsBgTop = (contentBgTop + contentBgBottom)/2 ;
			toolsBgBottom = (contentBgTop + contentBgBottom)/2 + toolsNeedHeight;
		}
//		if (isDebug){
//			Log.e(TAG, " toolsBgLeft " + toolsBgLeft + " toolsBgRight " + toolsBgRight +
//					" toolsBgTop " + toolsBgTop+ " toolsBgBottom " + toolsBgBottom);
//		}
	}
	//更新内容中心的偏移量
	private void updateContentCoordinate(){
//		Log.e(TAG, "步骤四updateContentCoordinate: 更新内容 工具 背景的坐标系 ");
		contentLeft -= xOffset;
		contentRight -= xOffset;
		contentTop -= yOffset;
		contentBottom -= yOffset;

		contentBgLeft -= xOffset;
		contentBgRight -= xOffset;
		contentBgTop -= yOffset;
		contentBgBottom -= yOffset;
		if (toolsBgRight!=0){//有工具栏
			toolsBgLeft -= xOffset;
			toolsBgRight -= xOffset;
			toolsBgTop -= yOffset;
			toolsBgBottom -= yOffset;
		}
		strLeft -= xOffset;
		strRight -= xOffset;
		strTop -= yOffset;
		strBottom -= yOffset;


//		Log.e(TAG, "updateContentCoordinate:xOffset==> " + xOffset + " yOffset==== >   "+ yOffset);
//		Log.e(TAG, "contentLeft " + contentLeft + " contentRight " + contentRight + " contentTop " + contentTop + " contentBottom " +contentBottom);
//		Log.e(TAG, "contentBgLeft " + contentBgLeft + " contentBgRight " + contentBgRight + " contentBgTop " + contentBgTop + " contentBgBottom " +contentBgBottom);
//		Log.e(TAG, "toolsBgLeft " + toolsBgLeft + " toolsBgRight " + toolsBgRight + " toolsBgTop " + toolsBgTop + " toolsBgBottom " +toolsBgBottom);
//		Log.e(TAG, "strLeft " + strLeft + " strRight " + strRight + " strTop " + strTop + " strBottom " +strBottom);
	}

	//得到所需的总宽高
	private void doMeasure(){
		//用于计算坐标原点偏移量
//		Log.e(TAG, "doMeasure: ");
		float minLeft, minTop , maxBottom, maxRight;
		if (hasBackGroundAndTools){//有工具栏
			if (tempWidgetData.getType()==1){
				float min,max ;
				//点模式，计算最左 及 最顶部 坐标值为整体偏移量
				min = Math.min(contentBgLeft,toolsBgLeft);
				min = Math.min(min,strLeft);
				minLeft = min;
				min = Math.min(contentBgTop,toolsBgTop);
				min = Math.min(min,strTop);
				minTop = min;
				//计算最底部最右侧的值，与最左 最顶部之差 为大小
				max = Math.max(contentBgRight,toolsBgRight);
				max = Math.max(max,strRight);
				maxRight = max;
				max = Math.max(contentBgBottom,toolsBgBottom);
				max = Math.max(max,strBottom);
				maxBottom = max;
				xOffset = minLeft;
				yOffset = minTop;
				//整体大小
				hMeasureSpecSize = maxBottom - minTop;
				wMeasureSpecSize = maxRight - minLeft;

//				Log.e(TAG, "doMeasure: minLeft " +minLeft+ " maxRight " + maxRight+ " minTop " +minTop +" maxBottom " +maxBottom );
//				Log.e(TAG, "步骤三: 计算 要绘制的最左边 最右边  最顶部 最底部 ");
//				if (isDebug){
//					Log.e(TAG, " doMeasure: w " + wMeasureSpecSize + " hMeasureSpecSize" + hMeasureSpecSize );
//				}

			}else if (tempWidgetData.getType()==2 || tempWidgetData.getType() ==3){
				//有背景 线和矩形的大小看 最高最低点图片的绘制是否影响控件的尺寸
				float min,max ;
				//点模式，计算最左的坐标
//				min = Math.min(tempWidgetData.getOtherTemp().getMinTempX()- minTempBt.getWidth()/2.0f,tempWidgetData.getOtherTemp().getMaxTempX()- maxTempBt.getWidth()/2.0f);
//				min = Math.min(min,contentBgLeft);
				min = Math.min(contentBgLeft,toolsBgLeft);
				minLeft = min;
//				min = Math.min(tempWidgetData.getOtherTemp().getMinTempY()- minTempBt.getHeight()/2.0f,tempWidgetData.getOtherTemp().getMaxTempY()- maxTempBt.getHeight()/2.0f);
//				min = Math.min(min,contentBgTop);
				min = Math.min(contentBgTop,toolsBgTop);
//				min = Math.min(contentBgTop,toolsBgTop);
				minTop = min;

//				max = Math.max(tempWidgetData.getOtherTemp().getMinTempX() + minTempBt.getWidth()/2.0f,tempWidgetData.getOtherTemp().getMaxTempX()+ maxTempBt.getWidth()/2.0f);
//				max = Math.max(max,contentBgRight);
				max = Math.max(contentBgRight,toolsBgRight);
//				max = Math.max(contentBgRight,toolsBgRight);
				maxRight = max;

//				max = Math.max(tempWidgetData.getOtherTemp().getMinTempY() + minTempBt.getHeight()/2.0f,tempWidgetData.getOtherTemp().getMaxTempY()+ maxTempBt.getHeight()/2.0f);
//				max = Math.max(max,contentBgBottom);
				max = Math.max(contentBgBottom,toolsBgBottom);
//				max = Math.max(contentBgBottom,toolsBgBottom);
				maxBottom = max;
				//线及矩形的偏移量不是这样算
				xOffset = minLeft;
				yOffset = minTop;

				hMeasureSpecSize = maxBottom - minTop;
				wMeasureSpecSize = maxRight - minLeft;
			}
		}else {//没有工具栏
			if (tempWidgetData.getType()==1){
				float min,max ;
				//点模式，计算最左的坐标

				min = Math.min(contentBgLeft,strLeft);
				minLeft = min;
				min = Math.min(contentBgTop,strTop);
				minTop = min;

				max = Math.max(contentBgRight,strRight);
				maxRight = max;
				max = Math.max(contentBgBottom,strBottom);
				maxBottom = max;

				xOffset = minLeft;
				yOffset = minTop;

				hMeasureSpecSize = maxBottom - minTop;
				wMeasureSpecSize = maxRight - minLeft;
			}else if (tempWidgetData.getType()==2 ||tempWidgetData.getType() ==3){//无背景，无工具栏
				textNeedHeight = (int) getStrHeight(maxTempTextPaint,maxTempStr);
				float min,max ;
				//点模式，计算最左的坐标
//				min = Math.min(tempWidgetData.getOtherTemp().getMinTempX()- minTempBt.getWidth()/2.0f,tempWidgetData.getOtherTemp().getMaxTempX()- maxTempBt.getWidth()/2.0f);
				min = Math.min(contentBgLeft,contentLeft);;
				minLeft = min;
//				min = Math.min(tempWidgetData.getOtherTemp().getMinTempY()- minTempBt.getHeight()/2.0f,tempWidgetData.getOtherTemp().getMaxTempY()- maxTempBt.getHeight()/2.0f);
//				min = Math.min(min,tempWidgetData.getOtherTemp().getMinTempY()-textNeedHeight);
//				min = Math.min(min,tempWidgetData.getOtherTemp().getMaxTempY()-textNeedHeight);
				min = Math.min(contentBgTop,contentTop);;
				minTop = min;

//				max = Math.max(tempWidgetData.getOtherTemp().getMinTempX() + minTempBt.getWidth()/2.0f,tempWidgetData.getOtherTemp().getMaxTempX()+ maxTempBt.getWidth()/2.0f);
				max = Math.max(contentBgRight,contentRight);;
				maxRight = max;

//				max = Math.max(tempWidgetData.getOtherTemp().getMinTempY() + minTempBt.getHeight()/2.0f,tempWidgetData.getOtherTemp().getMaxTempY()+ maxTempBt.getHeight()/2.0f);
//				max = Math.max(max,tempWidgetData.getOtherTemp().getMinTempY()+textNeedHeight);
//				max = Math.max(max,tempWidgetData.getOtherTemp().getMaxTempY()+textNeedHeight);
				max = Math.max(contentBgBottom,contentBottom);
				maxBottom = max;
				//线及矩形的偏移量不是这样算
				xOffset = minLeft;
				yOffset = minTop;

				hMeasureSpecSize = maxBottom - minTop;
				wMeasureSpecSize = maxRight - minLeft;

//				Log.e(TAG, "doMeasure: 2*TH = > " + 2* textNeedHeight + " hM = " + hMeasureSpecSize + " WM "+ wMeasureSpecSize) ;
			}
		}
	}
	/**
	 * 通过传入的起始点坐标得到内容的 坐标 及内容背景坐标
	 * @param tempWidget 控件的实例化对象
	 * @param bp       点模式的照片
	 * 以图片的左上角为坐标原点计算
	 */
	private void getContentAndBgCoordinate(@NonNull TempWidgetObj tempWidget , Bitmap bp){
		if (hasBackGroundAndTools){//有工具栏及内容背景
			if (tempWidget.getType() ==1){
				if (isDebug)
//					Log.e(TAG, "步骤一: 得到内容及其背景坐标 ");
				contentLeft = tempWidgetData.getPointTemp().getStartPointX() - bp.getWidth()/2.0f;
				contentRight = tempWidgetData.getPointTemp().getStartPointX() + bp.getWidth()/2.0f;
				contentTop = tempWidgetData.getPointTemp().getStartPointY() - bp.getHeight()/2.0f;
				contentBottom = tempWidgetData.getPointTemp().getStartPointY() + bp.getHeight()/2.0f;

//				if (isDebug){
//					Log.e(TAG, "contentLeft " + contentLeft + " contentRight " + contentRight +
//							" contentTop " + contentTop+ " contentBottom " + contentBottom);
//					Log.e(TAG, "contentBgLeft " + contentBgLeft + " contentBgRight " + contentBgRight + " contentBgTop " + contentBgTop
//							+ " contentBgBottom " + contentBgBottom);
//				}
//				if (isDebug)
//					Log.e(TAG, "步骤二: 得到内容及其工具栏方位及其坐标 ");
			}else if (tempWidget.getType() ==2){
				contentLeft = tempWidgetData.getOtherTemp().getStartPointX() - bp.getWidth()/2.0f;
				contentRight = tempWidgetData.getOtherTemp().getEndPointX() + bp.getWidth()/2.0f;
				contentTop = tempWidgetData.getOtherTemp().getStartPointY() - bp.getHeight()/2.0f;
				contentBottom = tempWidgetData.getOtherTemp().getEndPointY() + bp.getHeight()/2.0f;
			}else if (tempWidget.getType() ==3){
				contentLeft = tempWidgetData.getOtherTemp().getStartPointX() - bp.getWidth()/2.0f;
				contentRight = tempWidgetData.getOtherTemp().getEndPointX() + bp.getWidth()/2.0f;
				contentTop = tempWidgetData.getOtherTemp().getStartPointY() - bp.getHeight()/2.0f;
				contentBottom = tempWidgetData.getOtherTemp().getEndPointY() + bp.getHeight()/2.0f;
			}
			contentBgLeft = contentLeft - padLeft;
			contentBgRight = contentRight + padRight;
			contentBgTop = contentTop - padTop;
			contentBgBottom = contentBottom + padBottom;
			getToolsDirection(toolsNeedWidth,toolsNeedHeight,1);
			//初始化工具栏坐标
			getToolsCoordinate();
		}else {//无工具栏及内容背景
			if (tempWidget.getType() ==1) {
				contentLeft = contentBgLeft = tempWidgetData.getPointTemp().getStartPointX() - bp.getWidth()/2.0f;
				contentRight = contentBgRight = tempWidgetData.getPointTemp().getStartPointX() + bp.getWidth()/2.0f;
				contentTop = contentBgTop = tempWidgetData.getPointTemp().getStartPointY() - bp.getHeight()/2.0f;
				contentBottom = contentBgBottom = tempWidgetData.getPointTemp().getStartPointY() + bp.getHeight()/2.0f;
			} else if (tempWidget.getType() ==2 ||tempWidget.getType() ==3) {
				contentLeft = contentBgLeft = tempWidgetData.getOtherTemp().getStartPointX() - bp.getWidth()/2.0f;
				contentRight = contentBgRight = tempWidgetData.getOtherTemp().getEndPointX() + bp.getWidth()/2.0f;
				contentTop = contentBgTop = tempWidgetData.getOtherTemp().getStartPointY() - bp.getHeight()/2.0f;
				contentBottom = contentBgBottom = tempWidgetData.getOtherTemp().getEndPointY() + bp.getHeight()/2.0f;
			}
			toolsBgLeft = 0;
			toolsBgRight = 0;
			toolsBgTop = 0;
			toolsBgBottom =  0;
		}
	}
	/**
	 * @param pointX 图片的左边
	 * @param pointBp 绘制的图片本体
	 * @param pointDirection 绘制的方位
	 * @param needWidth 文字需要的长度
	 * @return 返回计算的X坐标
	 */
	private int getPointTempTextCoordinateX(int pointX, Bitmap pointBp , int pointDirection, int needWidth,TextPaint tp){
		//计算温度文字的矩形坐标  从图片边界计算
//		Log.e(TAG, "getPointTempTextCoordinateX: pointDirection == >  " + pointDirection);

		int calculateX = 0;
		if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_TOP){
			//(pointX - pointBp.getWidth()/2) == contentLeft
			calculateX = pointX - needWidth ;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
			calculateX = pointX - needWidth ;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_TOP){
			calculateX = pointX + pointBp.getWidth();
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
			calculateX = pointX + pointBp.getWidth();
		}
		return calculateX;
	}

	/**
	 * @param pointY 图片的顶边
	 * @param pointBp 图片本体
	 * @param pointDirection  文字该绘制的方位
	 * @param needHeight 需要的高度
	 * @return 计算的Y坐标
	 */
	private int getPointTempTextCoordinateY(float pointY,Bitmap pointBp , int pointDirection, float needHeight,TextPaint tp){
		//计算温度文字的矩形坐标  从图片边界计算
		Paint.FontMetrics metrics = tp.getFontMetrics();
		int calculateY = 0;
		if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_TOP){
			calculateY = (int) (pointY - needHeight +(-tp.getFontMetrics().ascent));
		}else if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
			calculateY = (int) (pointY + needHeight - tp.getFontMetrics().descent);
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_TOP){
			calculateY = (int) (pointY - needHeight +(-tp.getFontMetrics().ascent));
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
			calculateY = (int) (pointY + needHeight- tp.getFontMetrics().descent);
		}
		return calculateY;
	}


	public boolean isSelectedState () {
		return tempWidgetData.isSelect();
	}
	public void setSelectedState (boolean selectedState) {
		if (selectedState){
			tempWidgetData.setSelect(true);
		}else {
			tempWidgetData.setSelect(false);
		}
	}

	public TempWidgetObj getView () {
		return tempWidgetData;
	}

	/**
	 * @param data 传入的视图对象
	 * @param canvas 需要绘制的画布
	 *             以工具栏背景的坐标为基准
	 */
	private void drawTool(@NonNull TempWidgetObj data, @NonNull Canvas canvas){
			canvas.drawRoundRect(rectToolsBg,5,5,bgRoundPaint);
			int [] resPic = data.getToolsPicRes();
			RectF perToolsPic ;
			float left , right , top , bottom;
			if (resPic!= null && data.getToolsNumber() != 0){
				for (int i = 0 ; i < data.getToolsNumber(); i++){
					left = toolsBgLeft + DragTempContainer.perToolsMargin;
					right =toolsBgRight - DragTempContainer.perToolsMargin;
					top = toolsBgTop + DragTempContainer.perToolsMargin + (DragTempContainer.perToolsWidthHeightSet + 2.0f* DragTempContainer.perToolsMargin)* i ;
					bottom = toolsBgTop +(DragTempContainer.perToolsMargin + DragTempContainer.perToolsWidthHeightSet) + (DragTempContainer.perToolsWidthHeightSet+ 2.0f* DragTempContainer.perToolsMargin)* i;

					perToolsPic = new RectF(left,top,right,bottom);

					canvas.drawBitmap(BitmapFactory.decodeResource(getResources(),resPic[i]),null,perToolsPic,pointPaint);
				}
			}
	}

	@Override
	public void draw (Canvas canvas) {
		super.draw(canvas);
	}

	@Override
	protected void onDraw (Canvas canvas) {
//		super.onDraw(canvas);
//		Log.e(TAG, "onDraw: xOffset== > " + xOffset + " yoffset >> " + yOffset);
		//是否绘制背景
		if (hasBackGroundAndTools){//可选 | 不可选
			if(tempWidgetData.getType()==3){
				canvas.drawRoundRect(rectContent,0,0,bgRoundPaint);
				float length = mMinHeight/5.0f;
				float zoomBoxLeft,zoomBoxRight , zoomBoxTop , zoomBoxBottom , centerBeginX,leftRightBeginY;
				zoomBoxLeft = contentLeft - recZoomBoxPaintStroke;
				zoomBoxRight = contentRight + recZoomBoxPaintStroke;
				zoomBoxTop = contentTop - recZoomBoxPaintStroke;
				zoomBoxBottom = contentBottom + recZoomBoxPaintStroke;
				centerBeginX = (int) ((contentLeft+contentRight - length)/2);
				leftRightBeginY = (int) ((contentTop+contentBottom - length)/2);

				//todo 矩形特定的背景
				//左上
				canvas.drawLine(zoomBoxLeft,zoomBoxTop,zoomBoxLeft + length,zoomBoxTop,recZoomBox);
				canvas.drawLine(zoomBoxLeft,zoomBoxTop,zoomBoxLeft,zoomBoxTop + length,recZoomBox);

				canvas.drawLine(centerBeginX,zoomBoxTop,
						centerBeginX+length,zoomBoxTop,recZoomBox);

				canvas.drawLine(zoomBoxRight,zoomBoxTop,
						zoomBoxRight - length,zoomBoxTop,recZoomBox);
				canvas.drawLine(zoomBoxRight,zoomBoxTop,
						zoomBoxRight,zoomBoxTop + length,recZoomBox);
				//绘制左右两条
				canvas.drawLine(zoomBoxLeft,leftRightBeginY,zoomBoxLeft,leftRightBeginY+length,recZoomBox);
				canvas.drawLine(zoomBoxRight,leftRightBeginY,zoomBoxRight,leftRightBeginY+length,recZoomBox);
				//底部三条

				canvas.drawLine(zoomBoxLeft,zoomBoxBottom,zoomBoxLeft + length,zoomBoxBottom,recZoomBox);
				canvas.drawLine(zoomBoxLeft,zoomBoxBottom,zoomBoxLeft,zoomBoxBottom - length,recZoomBox);

				canvas.drawLine(centerBeginX,zoomBoxBottom,
						centerBeginX+length,zoomBoxBottom,recZoomBox);

				canvas.drawLine(zoomBoxRight,zoomBoxBottom,
						zoomBoxRight - length,zoomBoxBottom,recZoomBox);
				canvas.drawLine(zoomBoxRight,zoomBoxBottom,
						zoomBoxRight,zoomBoxBottom - length,recZoomBox);

			}else {
				//绘制内容的背景
				canvas.drawRoundRect(rectContentBg,minTempBt.getWidth()/4.0f,minTempBt.getWidth()/4.0f,bgRoundPaint);
			}
			drawTool(tempWidgetData,canvas); //绘制工具栏 背景及其颜色
		}
		//绘制内容
		if (tempWidgetData.getType() ==1){
			//绘制标识圆
//			if (isDebug)Log.e(TAG, "onDraw: 11111111");
//			canvas.drawCircle((contentBgLeft+contentBgRight)/2.0f,(contentBgTop+contentBgBottom)/2.0f,5,maxTempTextPaint);

			canvas.drawBitmap(minTempBt, null,rectContent,pointTextPaint);
			//绘制点测温文字
//

			float textX= strLeft;
			int textY = getPointTempTextCoordinateY((contentTop + contentBottom)/2,minTempBt,tempWidgetData.getPointTemp().getTempDirection(),textNeedHeight,pointTextPaint);

			canvas.drawText(minTempStr,textX,textY,pointTextPaint);
		}else if (tempWidgetData.getType() == 2){
//			Log.e(TAG, "onDraw: type ===2============================");

			//todo 绘制两个点温度 (计算两个点放置的方位)
			float startX  = tempWidgetData.getOtherTemp().getMinTempX() - xOffset, startY = tempWidgetData.getOtherTemp().getMinTempY() - yOffset;
			float endX = tempWidgetData.getOtherTemp().getMaxTempX() - xOffset, endY = tempWidgetData.getOtherTemp().getMaxTempY() - yOffset;

//			Log.e(TAG, "onDraw: type ===2===minx " + startX + " miny = " + startY + "maxx " + endX + " maxY" + endY );

			canvas.drawLine(tempWidgetData.getOtherTemp().getStartPointX() - xOffset,tempWidgetData.getOtherTemp().getStartPointY()-yOffset
					,tempWidgetData.getOtherTemp().getEndPointX() - xOffset,tempWidgetData.getOtherTemp().getEndPointY()-yOffset,linePaint);

//			Log.e(TAG, "onDraw: 2===> " + tempWidgetData.getOtherTemp().getMaxTemp() + " min " + tempWidgetData.getOtherTemp().getMinTemp());

			drawMinMaxTemp(canvas,startX,startY,minTempStr,minTempBt,minTempTextPaint,contentLeft,contentRight,contentTop,contentBottom,tempWidgetData.getType());
			drawMinMaxTemp(canvas,endX,endY,maxTempStr,maxTempBt,maxTempTextPaint,contentLeft,contentRight,contentTop,contentBottom,tempWidgetData.getType());
		}else if (tempWidgetData.getType() == 3){
//			Log.e(TAG, "onDraw: type ===3============================");
			float lsx  = tempWidgetData.getOtherTemp().getStartPointX() - xOffset, lsy = tempWidgetData.getOtherTemp().getStartPointY() - yOffset;
			float lex = tempWidgetData.getOtherTemp().getEndPointX() - xOffset, ley = tempWidgetData.getOtherTemp().getEndPointY()- yOffset;
			//todo 绘制四条线 矩形
			canvas.drawLine(lsx,lsy,lex,lsy,linePaint);
			canvas.drawLine(lex,lsy,lex,ley,linePaint);
			canvas.drawLine(lex,ley,lsx,ley,linePaint);
			canvas.drawLine(lsx,ley,lsx,lsy,linePaint);
			//todo 绘制两个点温度 (计算两个点放置的方位)
			float startX  = tempWidgetData.getOtherTemp().getMinTempX() - xOffset, startY = tempWidgetData.getOtherTemp().getMinTempY() -yOffset;
			float endX = tempWidgetData.getOtherTemp().getMaxTempX() - xOffset, endY = tempWidgetData.getOtherTemp().getMaxTempY() - yOffset;

			drawMinMaxTemp(canvas,startX,startY,minTempStr,minTempBt,minTempTextPaint,contentLeft,contentRight,contentTop,contentBottom,tempWidgetData.getType());
			drawMinMaxTemp(canvas,endX,endY,maxTempStr,maxTempBt,maxTempTextPaint,contentLeft,contentRight,contentTop,contentBottom,tempWidgetData.getType());
		}
	}

	/**
	 * 在画布上绘制一个 相对自身原点的温度。
	 * @param canvas 画布
	 * @param drawTempX 温度的x坐标
	 * @param drawTempY 温度的y坐标
	 * @param tempStr 温度数值
	 * @param bp 温度图片
	 * @param tempPaint 绘制的画笔
	 * @param leftBorder  内容左边界
	 * @param rightBorder  右边界
	 * @param topBorder 上边界
	 * @param bottomBorder  下边界
	 */
	private void drawMinMaxTemp(Canvas canvas ,float drawTempX, float drawTempY ,String tempStr,Bitmap bp,TextPaint tempPaint,
	                            float leftBorder,float rightBorder,float topBorder,float bottomBorder, int drawType){
		//温度文字 所需要的宽高，通过画笔计算
		float tempNeedW,tempNeedH ;int direction = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
		Rect rect = new Rect();
		tempPaint.getTextBounds(tempStr,0,tempStr.length(),rect);
		tempNeedW = rect.width();
//		tempNeedH = rect.height();
		tempNeedH = -tempPaint.getFontMetrics().ascent;
		//定义图片四个边界
		float picLeft= drawTempX - bp.getWidth()/3.0f;
		float picRight = drawTempX + bp.getWidth()/3.0f;
		float picTop = drawTempY - bp.getHeight()/3.0f;
		float picBottom = drawTempY + bp.getHeight()/3.0f;
//		Log.e(TAG, "drawMinMaxTemp:  " + picLeft + " " + picRight + " " + picTop + " " + picBottom);

		RectF bpRect = new RectF(picLeft,picTop,picRight,picBottom);

		if (picRight + tempNeedW <= rightBorder && (topBorder + bottomBorder)/2 - tempNeedH < bottomBorder){//右下
			direction = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
		}else if (picRight + tempNeedW <= rightBorder && (topBorder + bottomBorder)/2 - tempNeedH >= topBorder){//右上
			direction = WIDGET_DIRECTION_STATE_RIGHT_TOP;
		}else if (picLeft - tempNeedW >= leftBorder && (topBorder + bottomBorder)/2 - tempNeedH > topBorder){//左上
			direction = WIDGET_DIRECTION_STATE_LEFT_TOP;
		}else if (picLeft - tempNeedW >= leftBorder && (topBorder + bottomBorder)/2 + tempNeedH < bottomBorder){//左下
			direction = WIDGET_DIRECTION_STATE_LEFT_BOTTOM;
		}
		if (drawType ==2){
			if (direction ==WIDGET_DIRECTION_STATE_RIGHT_BOTTOM ){direction = WIDGET_DIRECTION_STATE_RIGHT_TOP;}
			if (direction ==WIDGET_DIRECTION_STATE_LEFT_BOTTOM ){direction = WIDGET_DIRECTION_STATE_LEFT_TOP;}
			switch (direction){
				case WIDGET_DIRECTION_STATE_LEFT_TOP:
					canvas.drawText(tempStr,picLeft - tempNeedW, picBottom- tempPaint.getFontMetrics().descent,tempPaint);
					break;
				case WIDGET_DIRECTION_STATE_RIGHT_TOP:
					canvas.drawText(tempStr,picRight,picBottom- tempPaint.getFontMetrics().descent,tempPaint);
					break;
			}
		}else {//矩形 绘制温度文字方位
			switch (direction){
				case WIDGET_DIRECTION_STATE_LEFT_TOP:
					canvas.drawText(tempStr,picLeft - tempNeedW,(picTop + picBottom)/2.0f - tempPaint.getFontMetrics().descent,tempPaint);
					break;
				case WIDGET_DIRECTION_STATE_LEFT_BOTTOM:
					canvas.drawText(tempStr,picLeft - tempNeedW,(picTop + picBottom)/2.0f - tempPaint.getFontMetrics().top,tempPaint);
					break;
				case WIDGET_DIRECTION_STATE_RIGHT_TOP:
					canvas.drawText(tempStr,picRight,(picTop + picBottom)/2.0f - tempPaint.getFontMetrics().descent,tempPaint);
					break;
				case WIDGET_DIRECTION_STATE_RIGHT_BOTTOM:
					canvas.drawText(tempStr,picRight,(picTop + picBottom)/2.0f - tempPaint.getFontMetrics().top,tempPaint);
					break;
			}
		}

		canvas.drawBitmap(bp,null,bpRect ,tempPaint);
	}
	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b) {
//		layout(xOffset,yOffset, xOffset+wMeasureSpecSize, yOffset+hMeasureSpecSize);
//		Log.e(TAG, "onLayout: ");
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
//		Log.e(TAG, "============ Child ===onMeasure========== ");
		initData();
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		Log.e(TAG, "onMeasure: " +MeasureSpec.getSize(widthMeasureSpec)+ "   height  "+MeasureSpec.getSize(heightMeasureSpec) );
//		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),MeasureSpec.getSize(heightMeasureSpec));
		setMeasuredDimension((int)wMeasureSpecSize,(int)hMeasureSpecSize);
	}

	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}
	//


	@Override
	public boolean onTouchEvent (MotionEvent event) {
		Log.e(TAG, "Child onTouchEvent:  x " + event.getX() + " y " + event.getY());
		//判定无效区域and 工具栏
		if (hasBackGroundAndTools && event.getX()> toolsBgLeft && event.getX()< toolsBgRight && event.getY()> toolsBgTop && event.getY() < toolsBgBottom){
			//点击了工具栏
			if (event.getAction() == MotionEvent.ACTION_DOWN){
				int clickPosition = (int )((event.getY() - toolsBgTop)/toolsNeedWidth );
				Log.e(TAG, "onTouchEvent: child click tools position is === >  "+ clickPosition);
				mChildToolsClickListener.onChildToolsClick(tempWidgetData,clickPosition);
			}
			return true;
		}
		//事件在内容页内  && event.getX()> contentBgLeft && event.getX()< contentBgRight && event.getY()> contentBgTop && event.getY() < contentBgBottom
		if (tempWidgetData.isCanMove()){

			switch (event.getAction()){
				case MotionEvent.ACTION_DOWN:


					Log.e(TAG, "onTouchEvent:child action down " + event.getRawX() +"  Y = >" +event.getRawY());
					pressDownX = event.getRawX();
					pressDownY = event.getRawY();

					if (tempWidgetData.isCanMove()&& !tempWidgetData.isSelect()){
						if (mTimer == null)
							mTimer = new Timer();
						mTimeTask = new TimerTask() {
							@Override
							public void run() {
								Log.e(TAG, "==========TimerTask进来了");
								Message message = Message.obtain();
								message.what = TO_CHANGE_SELECT;
								mHandler.sendMessage(message);
							}
						};
						mTimer.schedule(mTimeTask, 500);
					}
					break;
				case MotionEvent.ACTION_MOVE://每帧都会刷新
					if (hasBackGroundAndTools){
						if (tempWidgetData.getType()==1){
							float x = Math.max(0,tempWidgetData.getPointTemp().getStartPointX()+ (event.getRawX()- pressDownX));
							float y = Math.max(0,tempWidgetData.getPointTemp().getStartPointY()+ (event.getRawY()- pressDownY));
							pressDownX = event.getRawX();
							pressDownY = event.getRawY();
							x = Math.min(x,moveMaxWidth);
							y = Math.min(y,moveMaxHeight);
//							Log.e(TAG, "onTouchEvent: x == " + (xOffset+ event.getX()) + " y " + (yOffset+ event.getY()));
							tempWidgetData.getPointTemp().setStartPointX(x);
							tempWidgetData.getPointTemp().setStartPointY(y);
							requestLayout();
						}
						if (tempWidgetData.getType()==2){
							Log.e(TAG, "onTouchEvent: type == 2 "  );
							//(event.getRawX()- pressDownX) 偏移量   (event.getRawY()- pressDownY)
							//X 起点的取值范围为[ 0 , moveMaxWidth-(endX-startX)]   终点 [ (endX-startX) , moveMaxWidth]
							//Y 起点取值范围[0,moveMaxHeight]   终点 [0,moveMaxHeight]
							float xoff = (event.getRawX()- pressDownX),yoff = (event.getRawY()- pressDownY);
							float sx = tempWidgetData.getOtherTemp().getStartPointX(), sy= tempWidgetData.getOtherTemp().getStartPointY() ;
							float ex= tempWidgetData.getOtherTemp().getEndPointX() ,ey = sy;
							float length = ex - sx;
							if ((sx + xoff) >= padLeft && (sx + xoff) <= (moveMaxWidth - length)
									&& (ex + xoff) >= length && (ex + xoff) <= moveMaxWidth - padRight){
								sx += xoff;
								ex += xoff;

								tempWidgetData.getOtherTemp().setStartPointX(sx);
								tempWidgetData.getOtherTemp().setEndPointX(ex);
								requestLayout();

							}
							if ((sy + yoff) >= padTop && (sy + xoff) <= (moveMaxHeight - padBottom)){
								sy += yoff;
								ey = sy;
								tempWidgetData.getOtherTemp().setStartPointY(sy);
								tempWidgetData.getOtherTemp().setEndPointY(ey);
								requestLayout();
							}

							pressDownX = event.getRawX();
							pressDownY = event.getRawY();

							Log.e(TAG, "onTouchEvent: offset x = " + xoff + " yoffset = "  + yoff);
						}
						if (tempWidgetData.getType()==3){
							if (tempWidgetData.getOtherTemp().getStartPointX()+ event.getX()>= 0
									&&tempWidgetData.getOtherTemp().getEndPointX() + event.getX() <= moveMaxWidth
									&&tempWidgetData.getOtherTemp().getStartPointY() + event.getY()>= 0
									&&tempWidgetData.getOtherTemp().getEndPointY() + event.getY() <= moveMaxHeight) {


							}
						}

					}
//					Log.e(TAG, "onTouchEvent:child action move " + event.getRawX() +"  Y = >"+ event.getRawY());
					//				Log.e(TAG, "onTouchEvent:child action move");
					break;
				case MotionEvent.ACTION_UP:
					Log.e(TAG, "onTouchEvent:child action up" + event.getX() +"  Y = >"+ event.getY());
					Log.e(TAG, "onTouchEvent:child action up" + event.getRawX() +"  Y = >"+ event.getRawY());
					//				Log.e(TAG, "onTouchEvent:child action up");
					if (mTimeTask != null){
						mTimeTask.cancel();
					}

					break;
			}

			return true;
		}else {
			return true;
		}
	}
}
