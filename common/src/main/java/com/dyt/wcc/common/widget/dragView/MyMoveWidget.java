package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.dyt.wcc.common.R;

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

	private Bitmap maxTempBt, minTempBt;//最小温度，最大温度图片（单点只有最小温度的图片）
	private AddTempWidget addTempWidget;//每次绘制的 数据源

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
	private int wMeasureSpecSize, hMeasureSpecSize;
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

	private int contentLeft ,contentRight, contentTop, contentBottom;
	private int contentBgLeft ,contentBgRight, contentBgTop, contentBgBottom;

	private int textNeedWidth ,textNeedHeight  ,  toolsNeedWidth , toolsNeedHeight;

	private int strLeft,strRight, strTop, strBottom;
	private int toolsBgLeft,toolsBgRight, toolsBgTop, toolsBgBottom;

	private String minTempStr , maxTempStr;//记录最小最高温度
	private int xOffset, yOffset;

	public MyMoveWidget (Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}
	public MyMoveWidget (Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr);
		mContext = context;
		initView();
		initPaint();
	}
	public MyMoveWidget(Context context, AddTempWidget view ,int maxWidth, int maxHeight){
		super(context);
		mContext = context;
		addTempWidget = view;
		moveMaxWidth = maxWidth;
		moveMaxHeight = maxHeight;
		setClickable(true);

		initView();
		initPaint();

		initData();
	}

	private void initView(){
		padTop = padBottom= padLeft = padRight = 14;//设置背景间距,动态计算。不同dpi有明显差异  3DP
		mMinHeight = mMinWidth = 15*padLeft;//矩形的最小宽高等于五倍pad
		recZoomBoxPaintStroke = 8 ;

		if (addTempWidget.getType()==1){
			minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
			maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
		}else {
			addTempWidget.setCanMove(true);
			minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorblue);
			maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorred);
		}

	}

	private void initPaint(){
//		if (isDebug)Log.e(TAG, "initPaint: ");
		pointPaint = new Paint();
		pointPaint.setColor(getResources().getColor(R.color.bg_preview_toggle_select));
		linePaint = new Paint();
		linePaint.setColor(getResources().getColor(R.color.teal_200));

		pointTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		pointTextPaint.setTextSize(addTempWidget.getTempTextSize());
		pointTextPaint.setColor(getResources().getColor(R.color.bg_preview_toggle_select));
		pointTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

		maxTempTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		maxTempTextPaint.setTextSize(addTempWidget.getTempTextSize());
		maxTempTextPaint.setColor(getResources().getColor(R.color.max_temp_text_color_red));

		minTempTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		minTempTextPaint.setTextSize(addTempWidget.getTempTextSize());
		minTempTextPaint.setColor(getResources().getColor(R.color.min_temp_text_color_blue));

		bgRoundPaint = new Paint();
		bgRoundPaint.setStyle(Paint.Style.FILL);
		bgRoundPaint.setColor(getResources().getColor(R.color.bg_move_layout_round_bg));
		bgRoundPaint.setAlpha(100);//透明度 0透明-255不透明
		bgRoundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);//抗锯齿

		recZoomBox = new Paint();
		recZoomBox.setColor(getResources().getColor(R.color.white));
		recZoomBox.setStrokeWidth(recZoomBoxPaintStroke);
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
		//工具栏是否为空，为空时默认添加一个删除按钮。
		if (addTempWidget.getToolsPicRes() ==null){
			addTempWidget.setToolsPicRes(new int[]{R.mipmap.define_view_tools_delete});
		}
		if (!addTempWidget.isCanMove()){//不管什么类型，不能移动绝对不能选中
			addTempWidget.setSelect(false);
		}
		hasBackGroundAndTools = addTempWidget.isCanMove()&& addTempWidget.isSelect();//初始化是否有工具栏及其背景
		//√计算工具栏的所需宽高
		toolsNeedWidth = DragTempContainer.perToolsWidthHeightSet + DragTempContainer.perToolsMargin*2;
		toolsNeedHeight = addTempWidget.getToolsNumber() * toolsNeedWidth;//包含了 margin
		//有无工具栏情况下，以左上角为坐标原点的 内容及其背景的坐标。确定工具栏的方位
		getContentAndBgCoordinate(addTempWidget,minTempBt);

		//得到文字的所需宽高,并计算文字方位
		switch (addTempWidget.getType()){
			case 1://点
				//点温度String
				minTempStr = addTempWidget.getPointTemp().getTemp() + addTempWidget.getTextSuffix();
				// 通过方位及其 内容坐标或 文字的坐标设置 ，温度文字基准绘制点
				textNeedWidth = (int) getStrWidth(pointTextPaint,minTempStr);
				textNeedHeight = (int) getStrHeight(pointTextPaint,minTempStr);
				//得到文字绘制方位
				int strDirection = getStrDirection(WIDGET_DIRECTION_STATE_RIGHT_BOTTOM,textNeedWidth,textNeedHeight,2);
				getStrCoordinate(textNeedWidth,textNeedHeight,strDirection,addTempWidget.getType());
				//文字方向时 工具方位，计算出内容的坐标值
				addTempWidget.getPointTemp().setTempDirection(strDirection);//保存当前文字绘制的方位
				//求完内容 背景内容 工具栏文字方位，及其坐标

				doMeasure();
				updateContentCoordinate();//更新所有的坐标
				break;
			case 2://线
			case 3://矩形
				minTempStr = addTempWidget.getOtherTemp().getMinTemp() + addTempWidget.getTextSuffix();
				maxTempStr = addTempWidget.getOtherTemp().getMaxTemp() + addTempWidget.getTextSuffix();

				doMeasure();
				break;
			default://-1 或其他

				break;
		}
		rectContentBg = new RectF(contentBgLeft, contentBgTop, contentBgRight, contentBgBottom);
		rectToolsBg = new RectF(toolsBgLeft,toolsBgTop,toolsBgRight,toolsBgBottom);
		rectContent = new RectF(contentLeft,contentTop, contentRight,contentBottom);
		//得到了所有的宽高 则可以计算所需总宽高了
	}


	/**
	 * 计算中心偏移量
	 */
	protected int getXOffset(){
		return xOffset;
	}
	/**
	 * 计算中心偏移量
	 */
	protected int getYOffset(){
		return yOffset;
	}
	private float getStrWidth(@NonNull TextPaint textPaint,@NonNull String tempMsg){
		int w;
		Rect minTempStrRect = new Rect();
		textPaint.getTextBounds(tempMsg,0 , tempMsg.length(),minTempStrRect);
		w = (int) Math.max(minTempStrRect.width(),textPaint.measureText(tempMsg));
		return w;
	}
	private float getStrHeight(@NonNull TextPaint textPaint,@NonNull String tempMsg){
		int h;
		Rect minTempStrRect = new Rect();
		textPaint.getTextBounds(tempMsg,0 , tempMsg.length(),minTempStrRect);
		h = minTempStrRect.height();
		return h;
	}

	/**
	 * 通过使用内容的周边 计算出文字绘制的方位，参照物 带背景的内容
	 * 文字绘制规则：高度距离顶部或底部2个文字高度。
	 * @param defaultValue 默认值 （默认绘制方位）
	 * @param useWidth 需求的宽度
	 * @param useHeight 需求的高度
	 * @param rate 缩放高度的倍率 (与顶部间隔多少个Height)
	 * @return 方位值：左上左下 右上右下
	 */
	private int getStrDirection(int defaultValue, int useWidth ,int useHeight, int rate){
		int direction = defaultValue;//文字默认在右底绘制
		if ((contentBgRight + useWidth <= moveMaxWidth)//direction_right_bottom
				&& (contentBgBottom + useHeight <= moveMaxHeight)){
			direction = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
		}else if ((contentBgRight + useWidth <= moveMaxWidth)//direction_right_top
				&& (contentBgTop - useHeight >= 0)){
			direction = WIDGET_DIRECTION_STATE_RIGHT_TOP;
		}else if ((contentBgLeft - useWidth >= 0)//direction_left_bottom
				&& (contentBgBottom + useHeight <= moveMaxHeight)){
			direction = WIDGET_DIRECTION_STATE_LEFT_BOTTOM;
		}else if ((contentBgLeft - useWidth >= 0)//direction_left_top
				&& (contentBgTop - useHeight >= 0)){
			direction = WIDGET_DIRECTION_STATE_LEFT_TOP;
		}
		return direction;
	}
	private void getStrCoordinate(int width ,int height ,int direction, int type){
		if (type==1){
			if (direction == WIDGET_DIRECTION_STATE_LEFT_TOP){
				strLeft = contentLeft - width;
				strRight = contentLeft;
				strTop = (contentBgTop+ contentBgBottom)/2 - height;
				strBottom = (contentBgTop+ contentBgBottom)/2;
			}else if (direction == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
				strLeft = contentLeft - width;
				strRight = contentLeft;
				strTop = (contentBgTop + contentBgBottom)/2;
				strBottom = (contentBgTop+ contentBgBottom)/2 + height;
			}else if (direction ==WIDGET_DIRECTION_STATE_RIGHT_TOP){
				strLeft = contentRight ;
				strRight = contentRight+ width;
				strTop = (contentBgTop+ contentBgBottom)/2 - height;
				strBottom = (contentBgTop+ contentBgBottom)/2;
			}else if (direction ==WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
				strLeft = contentRight;
				strRight = contentRight+ width;
				strTop = (contentBgTop+ contentBgBottom)/2;
				strBottom = (contentBgTop+ contentBgBottom)/2 + height;
			}
		}
	}

	/**
	 * 根据本地内容背景的四个边，及工具栏的宽高 确定工具栏绘制的位置
	 * 确定规则：以中心横线为基准去计算方位
	 * 一个Y轴的确定点
	 * @param endPointX 参照物左边
	 * @param endPointY 参照物右边
	 * @param width   参照点X轴
	 * @param height    参照点Y轴
	 * @param toolsWidth 工具栏宽度
	 * @param toolsHeight   工具栏高度
	 * @param type 额外添加的距离，针对于绘制线 和矩形时 温度图片的边界在边缘
	 * @return 左上 左对齐 右上 右对齐
	 */
	private void getToolsDirection(int endPointX , int endPointY , int width , int height , int toolsWidth , int toolsHeight , int type){
		//tools Direction
//		if (type ==1 || type ==2){
			if ((contentBgRight + toolsWidth <moveMaxWidth) && ((contentBgTop) - toolsHeight >= 0)){               //tools right top
				toolsLocationState = WIDGET_DIRECTION_STATE_RIGHT_TOP;
			}else if ((contentBgRight +toolsWidth <moveMaxWidth)&&(contentBgBottom + toolsHeight) <= moveMaxHeight){//tools right bottom
				toolsLocationState = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
			}else if ((contentBgLeft - toolsWidth > 0)&&(contentBgTop - toolsHeight >= 0)){                   //tools left top
				toolsLocationState = WIDGET_DIRECTION_STATE_LEFT_TOP;
			}else if ((contentBgLeft - toolsWidth > 0)&&(contentBgBottom + toolsHeight) <= moveMaxHeight){       //tools left bottom
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
		if (isDebug){
			Log.e(TAG, " toolsBgLeft " + toolsBgLeft + " toolsBgRight " + toolsBgRight +
					" toolsBgTop " + toolsBgTop+ " toolsBgBottom " + toolsBgBottom);
		}
	}
	//更新内容中心的偏移量
	private void updateContentCoordinate(){
		Log.e(TAG, "步骤四updateContentCoordinate: 更新内容 工具 背景的坐标系 ");
		contentLeft -= xOffset;
		contentRight -= xOffset;
		contentTop -= yOffset;
		contentBottom -= yOffset;

		contentBgLeft -= xOffset;
		contentBgRight -= xOffset;
		contentBgTop -= yOffset;
		contentBgBottom -= yOffset;

		toolsBgLeft -= xOffset;
		toolsBgRight -= xOffset;
		toolsBgTop -= yOffset;
		toolsBgBottom -= yOffset;

		strLeft -= xOffset;
		strRight -= xOffset;
		strTop -= yOffset;
		strBottom -= yOffset;


		Log.e(TAG, "updateContentCoordinate:xOffset==> " + xOffset + " yOffset==== >   "+ yOffset);
		Log.e(TAG, "contentLeft " + contentLeft + " contentRight " + contentRight + " contentTop " + contentTop + " contentBottom " +contentBottom);
		Log.e(TAG, "contentBgLeft " + contentBgLeft + " contentBgRight " + contentBgRight + " contentBgTop " + contentBgTop + " contentBgBottom " +contentBgBottom);
		Log.e(TAG, "toolsBgLeft " + toolsBgLeft + " toolsBgRight " + toolsBgRight + " toolsBgTop " + toolsBgTop + " toolsBgBottom " +toolsBgBottom);

//		xOffset += (contentBgRight + contentBgLeft)/2;
//		yOffset += (contentBottom + contentBgTop)/2;

//		if (select){//选中状态
//			//工具栏方位修改内容坐标
//			boolean toolsIsMax = toolsNeedHeight >= contentBgBottom;
//			if (toolsDirection == WIDGET_DIRECTION_STATE_RIGHT_TOP){
//				contentBgLeft = 0;
//				contentBgRight = minTempBt.getWidth() + padLeft + padRight;
//				if (toolsIsMax){
//					contentBgTop = toolsNeedHeight - padTop - minTempBt.getHeight()/2;
//					contentBgBottom = toolsNeedHeight + minTempBt.getHeight()/2 + padBottom;
//				}else {
//					contentBgTop = 0;
//					contentBgBottom =  minTempBt.getHeight() + padTop + padBottom;
//				}
//			}else if(toolsDirection == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
//				contentBgLeft = 0;
//				contentBgRight = minTempBt.getWidth() + padLeft + padRight;
//				contentBgTop = 0;
//				contentBgBottom =  minTempBt.getHeight() + padTop + padBottom;
//			}else if (toolsDirection == WIDGET_DIRECTION_STATE_LEFT_TOP){
//				contentBgLeft = toolsNeedWidth;
//				contentBgRight = toolsNeedWidth+ minTempBt.getWidth() + padLeft + padRight;
//				if (toolsIsMax){
//					contentBgTop = toolsNeedHeight - padTop - minTempBt.getHeight()/2;
//					contentBgBottom = toolsNeedHeight + minTempBt.getHeight()/2 + padBottom;
//				}else {
//					contentBgTop = 0;
//					contentBgBottom =  minTempBt.getHeight() + padTop + padBottom;
//				}
//			}else if (toolsDirection == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
//				contentBgLeft = toolsNeedWidth;
//				contentBgRight = toolsNeedWidth+ minTempBt.getWidth() + padLeft + padRight;
//				contentBgTop = 0;
//				contentBgBottom =  minTempBt.getHeight() + padTop + padBottom;
//			}
//			//含工具栏 以及 文字
//			boolean textIsMax = textNeedWidth > toolsNeedWidth;
//			if (strDirection == WIDGET_DIRECTION_STATE_RIGHT_TOP){
//
//			}else if (strDirection == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
//
//			}else if (strDirection == WIDGET_DIRECTION_STATE_LEFT_TOP){
//				if (textIsMax){
//					contentBgLeft = textNeedWidth;
//					contentBgRight = textNeedWidth+ minTempBt.getWidth() + padLeft + padRight;
//				}
//			}else if (strDirection == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
//				if (textIsMax){
//					contentBgLeft = textNeedWidth;
//					contentBgRight = textNeedWidth+ minTempBt.getWidth() + padLeft + padRight;
//				}
//			}
//			contentLeft = contentBgLeft + padLeft;
//			contentRight = contentBgRight + padRight;
//			contentTop = contentBgTop + padTop;
//			contentBottom = contentBgBottom + padBottom;
//
//		}else {//只含文字 修整内容的坐标
//			if (strDirection == WIDGET_DIRECTION_STATE_RIGHT_TOP){
//				contentBgLeft = contentLeft = 0;
//				contentBgTop = contentTop = 0;
//				contentBgRight = contentRight = minTempBt.getWidth();
//				contentBgBottom = contentBottom = minTempBt.getHeight();
//			}else if (strDirection == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
//				contentBgLeft = contentLeft = 0;
//				contentBgTop = contentTop = 0;
//				contentBgRight = contentRight = minTempBt.getWidth();
//				contentBgBottom = contentBottom = minTempBt.getHeight();
//			}else if (strDirection == WIDGET_DIRECTION_STATE_LEFT_TOP){
//				contentBgLeft = contentLeft = textNeedWidth;
//				contentBgTop = contentTop = 0;
//				contentBgRight = contentRight = textNeedWidth + minTempBt.getWidth();
//				contentBgBottom = contentBottom = minTempBt.getHeight();
//			}else if (strDirection == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
//				contentBgLeft = contentLeft = 0;
//				contentBgTop = contentTop = 0;
//				contentBgRight = contentRight = textNeedWidth + minTempBt.getWidth();
//				contentBgBottom = contentBottom = minTempBt.getHeight();
//			}
//		}
	}

	//得到所需的总宽高
	private void doMeasure(){
		int toolTextMaxW = Math.max(textNeedWidth, toolsNeedWidth);
		int halfContentToolHeight = Math.max(contentBgBottom, toolsNeedHeight);


		int minLeft, minTop , maxBottom, maxRight;
		//用于计算坐标原点偏移量
//		boolean toolsIsHeight = toolsNeedHeight > contentBgBottom/2;//是否工具栏高度比内容背景的一半高
//		boolean textIsWidth = textNeedWidth > toolsNeedWidth;//是否文字比工具栏更宽
		if (hasBackGroundAndTools){//有工具栏
			if (addTempWidget.getType()==1){
				int min,max ;
				//点模式，计算最左的坐标
				min = Math.min(contentBgLeft,toolsBgLeft);
				min = Math.min(min,strLeft);
				minLeft = min;
				min = Math.min(contentBgTop,toolsBgTop);
				min = Math.min(min,strTop);
				minTop = min;

				max = Math.max(contentBgRight,toolsBgRight);
				max = Math.max(max,strRight);
				maxRight = max;
				max = Math.max(contentBgBottom,toolsBgBottom);
				max = Math.max(max,strBottom);
				maxBottom = max;

				xOffset = minLeft;
				yOffset = minTop;

				hMeasureSpecSize = maxBottom - minTop;
				wMeasureSpecSize = maxRight - minLeft;

				Log.e(TAG, "doMeasure: minLeft " +minLeft+ " maxRight " + maxRight+ " minTop " +minTop +" maxBottom " +maxBottom );
				Log.e(TAG, "步骤三: 计算 要绘制的最左边 最右边  最顶部 最底部 ");
				if (isDebug){
					Log.e(TAG, " doMeasure: w " + wMeasureSpecSize + " hMeasureSpecSize" + hMeasureSpecSize );
				}

			}else if (addTempWidget.getType()==2){
				wMeasureSpecSize = contentRight + Math.max((minTempBt.getWidth()+textNeedWidth),toolsNeedWidth);
				hMeasureSpecSize = contentBgBottom*2 + toolsNeedHeight;
			}else if (addTempWidget.getType() ==3){
				wMeasureSpecSize = contentRight + Math.max((minTempBt.getWidth()+textNeedWidth),toolsNeedWidth);
				hMeasureSpecSize = contentBgBottom/2 + toolsNeedHeight;
			}
		}else {//没有工具栏
			if (addTempWidget.getType()==1){
				int min,max ;
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
			}else if (addTempWidget.getType()==2){
				wMeasureSpecSize = (contentBgRight)+(minTempBt.getWidth()+textNeedWidth);
				hMeasureSpecSize = contentBgBottom;
			}else if (addTempWidget.getType() ==3){
				wMeasureSpecSize = (contentBgRight)+(minTempBt.getWidth()+textNeedWidth);
				hMeasureSpecSize = contentBgBottom;
			}
		}
	}
	/**
	 * 通过传入的起始点坐标得到内容的 坐标 及内容背景坐标
	 * @param tempWidget 控件的实例化对象
	 * @param bp       点模式的照片
	 * 以图片的左上角为坐标原点计算
	 */
	private void getContentAndBgCoordinate(@NonNull AddTempWidget tempWidget ,Bitmap bp){
		if (hasBackGroundAndTools){//有工具栏及内容背景
			if (tempWidget.getType() ==1){
				if (isDebug)
					Log.e(TAG, "步骤一: 得到内容及其背景坐标 ");
				contentLeft = addTempWidget.getPointTemp().getStartPointX() - bp.getWidth()/2;
				contentRight = addTempWidget.getPointTemp().getStartPointX() + bp.getWidth()/2;
				contentTop = addTempWidget.getPointTemp().getStartPointY() - bp.getHeight()/2;
				contentBottom = addTempWidget.getPointTemp().getStartPointY() + bp.getHeight()/2;

				contentBgLeft = contentLeft - padLeft;
				contentBgRight = contentRight + padRight;
				contentBgTop = contentTop - padTop;
				contentBgBottom = contentBottom + padBottom;
				if (isDebug){
					Log.e(TAG, "contentLeft " + contentLeft + " contentRight " + contentRight +
							" contentTop " + contentTop+ " contentBottom " + contentBottom);
					Log.e(TAG, "contentBgLeft " + contentBgLeft + " contentBgRight " + contentBgRight + " contentBgTop " + contentBgTop
							+ " contentBgBottom " + contentBgBottom);
				}

				if (isDebug)
					Log.e(TAG, "步骤二: 得到内容及其工具栏方位及其坐标 ");
				//工具栏的放置方位计算
				getToolsDirection(0,0, 0,0,toolsNeedWidth,toolsNeedHeight,1);
			}else if (tempWidget.getType() ==2 ||tempWidget.getType() ==3){
				contentLeft = addTempWidget.getOtherTemp().getStartPointX();
				contentRight = addTempWidget.getOtherTemp().getEndPointX();
				contentTop = addTempWidget.getOtherTemp().getStartPointY();
				contentBottom = addTempWidget.getOtherTemp().getEndPointY();

				contentBgLeft = contentLeft - padLeft;
				contentBgRight = contentRight + padRight;
				contentBgTop = contentTop - padTop;
				contentBgBottom = contentBottom + padBottom;
				//工具栏的放置方位计算
				getToolsDirection(0,0, 0,0,toolsNeedWidth,toolsNeedHeight,1);
			}
			//初始化工具栏坐标
			getToolsCoordinate();
		}else {//无工具栏及内容背景
			if (tempWidget.getType() ==1) {
				contentLeft = contentBgLeft = addTempWidget.getPointTemp().getStartPointX() - bp.getWidth()/2;
				contentRight = contentBgRight = addTempWidget.getPointTemp().getStartPointX() + bp.getWidth()/2;
				contentTop = contentBgTop = addTempWidget.getPointTemp().getStartPointY() - bp.getHeight()/2;
				contentBottom = contentBgBottom = addTempWidget.getPointTemp().getStartPointY() + bp.getHeight()/2;
			} else if (tempWidget.getType() ==2 ||tempWidget.getType() ==3) {
				contentLeft = contentBgLeft = addTempWidget.getOtherTemp().getStartPointX();
				contentRight = contentBgRight = addTempWidget.getOtherTemp().getEndPointX();
				contentTop = contentBgTop = addTempWidget.getOtherTemp().getStartPointY();
				contentBottom = contentBgBottom = addTempWidget.getOtherTemp().getEndPointY();
			}
		}
	}
	/**
	 * @param pointX 图片的左边
	 * @param pointBp 绘制的图片本体
	 * @param pointDirection 绘制的方位
	 * @param needWidth 文字需要的长度
	 * @return 返回计算的X坐标
	 */
	private int getPointTempTextCoordinateX(int pointX, Bitmap pointBp , int pointDirection, int needWidth){
		//计算温度文字的矩形坐标  从图片边界计算
		Log.e(TAG, "getPointTempTextCoordinateX: pointDirection == >  " + pointDirection);
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
	private int getPointTempTextCoordinateY(int pointY,Bitmap pointBp , int pointDirection, int needHeight){
		//计算温度文字的矩形坐标  从图片边界计算
		int calculateY = 0;
		if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_TOP){
			calculateY = pointY -needHeight;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
			calculateY = pointY + needHeight;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_TOP){
			calculateY = pointY-needHeight;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
			calculateY = pointY + needHeight;
		}
		return calculateY;
	}


	public boolean isSelectedState () {
		return addTempWidget.isSelect();
	}
	public void setSelectedState (boolean selectedState) {
		if (selectedState){
			addTempWidget.setSelect(true);
		}else {
			addTempWidget.setSelect(false);
		}
	}

	public AddTempWidget getView () {
		return addTempWidget;
	}

	/**
	 * @param view 传入的视图对象
	 * @param canvas 需要绘制的画布
	 *             以工具栏背景的坐标为基准
	 */
	private void drawTool(@NonNull AddTempWidget view,@NonNull Canvas canvas){
			canvas.drawRoundRect(rectToolsBg,5,5,bgRoundPaint);
			int [] resPic = view.getToolsPicRes();
			RectF perToolsPic ;
			float left , right , top , bottom;
			if (resPic!= null && view.getToolsNumber() != 0){
				for (int i = 0 ; i < view.getToolsNumber(); i++){
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
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		Log.e(TAG, "onDraw: ");
		//是否绘制背景
		if (hasBackGroundAndTools){//可选 | 不可选
			if(addTempWidget.getType()==3){
				canvas.drawRoundRect(rectContent,0,0,bgRoundPaint);
				float length = mMinHeight/5.0f;
				int zoomBoxLeft,zoomBoxRight , zoomBoxTop , zoomBoxBottom , centerBeginX,leftRightBeginY;
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
			drawTool(addTempWidget,canvas); //绘制工具栏 背景及其颜色
		}
		//绘制内容
		if (addTempWidget.getType() ==1){
			//绘制标识圆
			if (isDebug)Log.e(TAG, "onDraw: 11111111");
			canvas.drawCircle((contentBgLeft+contentBgRight)/2.0f,(contentBgTop+contentBgBottom)/2.0f,5,maxTempTextPaint);

			canvas.drawBitmap(minTempBt, null,rectContent,pointTextPaint);
			//绘制点测温文字
			Log.e(TAG, "onDraw: " + contentTop + "  b  ===> " +  contentBottom + " h " + textNeedHeight);

			int textX= getPointTempTextCoordinateX(contentLeft,minTempBt,
					addTempWidget.getPointTemp().getTempDirection(),textNeedWidth);
			int textY = getPointTempTextCoordinateY((contentTop + contentBottom)/2,minTempBt,
					addTempWidget.getPointTemp().getTempDirection(),textNeedHeight);
			canvas.drawText(minTempStr,textX,textY,pointTextPaint);
		}else if (addTempWidget.getType() == 2){
			canvas.drawLine(addTempWidget.getOtherTemp().getStartPointX(),addTempWidget.getOtherTemp().getStartPointY()
					,addTempWidget.getOtherTemp().getEndPointX(),addTempWidget.getOtherTemp().getEndPointY(),linePaint);
			//todo 绘制两个点温度 (计算两个点放置的方位)
			int startX  = addTempWidget.getOtherTemp().getMinTempX(), startY = addTempWidget.getOtherTemp().getMinTempY();
			int endX = addTempWidget.getOtherTemp().getMaxTempX() , endY = addTempWidget.getOtherTemp().getMaxTempY();

			drawMinMaxTemp(canvas,startX,startY,minTempStr,minTempBt,minTempTextPaint);
			drawMinMaxTemp(canvas,endX,endY,maxTempStr,maxTempBt,maxTempTextPaint);
		}else if (addTempWidget.getType() == 3){
			int lsx  = addTempWidget.getOtherTemp().getStartPointX(), lsy = addTempWidget.getOtherTemp().getStartPointY();
			int lex = addTempWidget.getOtherTemp().getEndPointX() , ley = addTempWidget.getOtherTemp().getEndPointY();
			//todo 绘制四条线 矩形
			canvas.drawLine(lsx,lsy,lex,lsy,linePaint);
			canvas.drawLine(lex,lsy,lex,ley,linePaint);
			canvas.drawLine(lex,ley,lsx,ley,linePaint);
			canvas.drawLine(lsx,ley,lsx,lsy,linePaint);
			//todo 绘制两个点温度 (计算两个点放置的方位)
			int startX  = addTempWidget.getOtherTemp().getMinTempX(), startY = addTempWidget.getOtherTemp().getMinTempY();
			int endX = addTempWidget.getOtherTemp().getMaxTempX() , endY = addTempWidget.getOtherTemp().getMaxTempY();

			drawMinMaxTemp(canvas,startX,startY,minTempStr,minTempBt,minTempTextPaint);
			drawMinMaxTemp(canvas,endX,endY,maxTempStr,maxTempBt,maxTempTextPaint);
		}
	}

	//绘制最大温度：传入画布 坐标
	private void drawMinMaxTemp(Canvas canvas , int drawTempX, int drawTempY ,String tempStr,Bitmap bp, TextPaint tempPaint){
		int tempNeedW,tempNeedH , direction;
		Rect rect = new Rect();
		tempNeedW = (int) tempPaint.measureText(tempStr);
		tempPaint.getTextBounds(tempStr,0,tempStr.length(),rect);
		tempNeedH = rect.height();

		int picLeft= drawTempX - bp.getWidth()/3;
		int picRight = drawTempX + bp.getWidth()/3;
		int	picTop = drawTempY - bp.getHeight()/3;
		int picBottom = drawTempY + bp.getHeight()/3;
		Rect bpRect = new Rect(picLeft,picTop,picRight,picBottom);

		if (picRight + tempNeedW <= moveMaxWidth && picBottom + tempNeedH <= moveMaxHeight){//右 上下
			direction = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
			canvas.drawText(tempStr,picRight,picBottom - tempNeedH,tempPaint);
		}else if (picRight + tempNeedW <= moveMaxWidth && picTop - tempNeedH >=0){
			direction = WIDGET_DIRECTION_STATE_RIGHT_TOP;
			canvas.drawText(tempStr,picRight,picTop + tempNeedH,tempPaint);
		}else if (picLeft - tempNeedW >= 0 && picTop - tempNeedH >=0){//左 上下
			direction = WIDGET_DIRECTION_STATE_LEFT_TOP;
			canvas.drawText(tempStr,picLeft - tempNeedW,picTop + tempNeedH,tempPaint);
		}else if (picLeft - tempNeedW >= 0 && picBottom+ tempNeedH <= moveMaxHeight){
			direction = WIDGET_DIRECTION_STATE_LEFT_BOTTOM;
			canvas.drawText(tempStr,picLeft - tempNeedW,picBottom - tempNeedH,tempPaint);
		}
		canvas.drawBitmap(bp,null,bpRect ,tempPaint);
//		canvas.drawBitmap(bp,picLeft,picTop,tempPaint);
	}
	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		Log.e(TAG, "onLayout: ");
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		Log.e(TAG, "onMeasure: " +MeasureSpec.getSize(widthMeasureSpec)+ "   height  "+MeasureSpec.getSize(heightMeasureSpec) );
//		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),MeasureSpec.getSize(heightMeasureSpec));
		setMeasuredDimension(wMeasureSpecSize,hMeasureSpecSize);
//		invalidate();
	}


	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}
	//


	@Override
	public boolean onTouchEvent (MotionEvent event) {
		switch (event.getAction()){
			case MotionEvent.ACTION_DOWN:
				Log.e(TAG, "onTouchEvent:child action down");
				return true;
			case MotionEvent.ACTION_MOVE:
				Log.e(TAG, "onTouchEvent:child action move");
				return true;
		}
		if (isDebug)Log.e(TAG, "onTouchEvent: child default" );
		return true;
	}
}
