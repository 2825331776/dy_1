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

	private RectF pointBgRectF;
	private int wMeasureSpec, hMeasureSpec;
	//todo  状态应提取出去
	//八个方位
	public static final int WIDGET_DIRECTION_STATE_LEFT = 0x000;
	public static final int WIDGET_DIRECTION_STATE_RIGHT = 0x001;
	public static final int WIDGET_DIRECTION_STATE_TOP = 0x010;
	public static final int WIDGET_DIRECTION_STATE_BOTTOM = 0x011;
	public static final int WIDGET_DIRECTION_STATE_LEFT_TOP = 0x100;
	public static final int WIDGET_DIRECTION_STATE_LEFT_BOTTOM = 0x101;
	public static final int WIDGET_DIRECTION_STATE_RIGHT_TOP = 0x110;
	public static final int WIDGET_DIRECTION_STATE_RIGHT_BOTTOM = 0x111;

	//内容的矩形、内容背景矩形、 工具图片绘制的矩形、 工具图片的背景 矩形、 文字矩形
	private RectF rectContent , rectContentBg , rectTool , rectToolsBg , textRectBg;

	private int tempLocationState = WIDGET_DIRECTION_STATE_LEFT_TOP, toolsLocationState = WIDGET_DIRECTION_STATE_LEFT_TOP, widgetLocationState;//文字 和 工具 绘制所处于的状态

	private int contentLeft ,contentRight, contentTop, contentBottom;
	private int contentBgLeft ,contentBgRight, contentBgTop, contentBgBottom;

	private int textNeedWidth ,textNeedHeight  ,  toolsNeedWidth , toolsNeedHeight;

//	private int toolsLeft,toolsRight, toolsTop, toolsBottom;
	private int toolsBgLeft,toolsBgRight, toolsBgTop, toolsBgBottom;

	private String minTempStr , maxTempStr;//记录最小最高温度
	private int pointTempTextX ,pointTempTextY;//点温度文字绘制 基准线 坐标

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
		padLeft = padRight = 14;//设置背景间距,动态计算。不同dpi有明显差异  3DP
		padTop = padBottom = 14;
		mMinHeight = mMinWidth = 15*padLeft;//矩形的最小宽高等于五倍pad
		recZoomBoxPaintStroke = 8 ;


		if (addTempWidget.getType()==1){
			minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
			maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
		}else {
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
	 * 计算文字的宽高
	 * 通过计算后的宽高，得到该绘制的方位
	 * 及其储存所能放置的方位 、 最小 |最大。
	 */
	private int getTempTextMeasure(@NonNull TextPaint textPaint,@NonNull String tempMsg){
		Rect minTempStrRect = new Rect();
//		int w , h ;
		textPaint.getTextBounds(tempMsg,0 , tempMsg.length(),minTempStrRect);
		//点温度文字 所需要的长宽
		textNeedWidth = (int) Math.max(minTempStrRect.width(),textPaint.measureText(tempMsg));
		textNeedHeight = minTempStrRect.height();

		return getDirection(WIDGET_DIRECTION_STATE_RIGHT_BOTTOM,textNeedWidth,textNeedHeight,2);
	}

	/**
	 * 通过使用的宽高得到方位 ，比较内容背景的四个边
	 * @param defaultValue 默认值 （默认绘制方位）
	 * @param useWidth 需求的宽度
	 * @param useHeight 需求的高度
	 * @param rate 缩放高度的倍率 (与顶部间隔多少个Height)
	 * @return 方位值：左上左下 右上右下
	 */
	private int getDirection(int defaultValue, int useWidth ,int useHeight, int rate){
		int direction = defaultValue;//文字默认在右底绘制
		if ((contentBgRight + useWidth <= moveMaxWidth)
				&& ((contentBgTop + contentBgBottom)/2.0f + useHeight <= moveMaxHeight)){//direction_right_bottom
			direction = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
		}else if ((contentBgRight + useWidth <= moveMaxWidth)
				&& (contentBgTop - rate*useHeight >= 0)){//direction_right_top
			direction = WIDGET_DIRECTION_STATE_RIGHT_TOP;
		}else if ((contentBgLeft - useWidth >= 0)
				&& (contentBgTop - rate*useHeight >= 0)){//direction_left_top
			direction = WIDGET_DIRECTION_STATE_LEFT_TOP;
		}else if ((contentBgLeft - useWidth >= 0)
				&& ((contentBgTop+contentBgBottom)/2.0f + useHeight <= moveMaxHeight)){//direction_left_bottom
			direction = WIDGET_DIRECTION_STATE_LEFT_BOTTOM;//从底部 向 顶部绘制
		}
		return direction;
	}

//	private int getTempTextMeasure(){
//
//		return 0;
//	}
//
//	/**
//	 * 提供给 线 矩形 计算内置最大最小温度值方向
//	 * @return
//	 */
//	private int getDirection(int x, int y , Bitmap bp){
//		int direction = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
//
//		return direction;
//	}

	/**
	 *初始化 工具栏背景四个边 坐标
	 */
	private void getToolsCoordinate(){
		//计算工具栏的背景图四个坐标 加上间隔的距离
		if (toolsLocationState == WIDGET_DIRECTION_STATE_LEFT_TOP){
			toolsBgLeft = contentBgLeft - toolsNeedWidth - DragTempContainer.perToolsMargin;
			toolsBgRight = contentBgLeft - DragTempContainer.perToolsMargin;
			toolsBgTop = contentBgTop - DragTempContainer.perToolsWidthHeightSet - DragTempContainer.perToolsMargin;
			toolsBgBottom = (toolsBgTop + toolsNeedHeight);

		}else if (toolsLocationState == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
			toolsBgLeft = contentBgLeft - toolsNeedWidth - DragTempContainer.perToolsMargin;
			toolsBgRight = contentBgLeft - DragTempContainer.perToolsMargin;
			toolsBgTop = contentBgTop ;
			toolsBgBottom = contentBgTop + toolsNeedHeight;

		}else if (toolsLocationState == WIDGET_DIRECTION_STATE_RIGHT_TOP){
			toolsBgLeft = contentBgRight + DragTempContainer.perToolsMargin ;
			toolsBgRight = contentBgRight + toolsNeedWidth + DragTempContainer.perToolsMargin;
			toolsBgTop = contentBgTop - DragTempContainer.perToolsWidthHeightSet - DragTempContainer.perToolsMargin;
			toolsBgBottom = (toolsBgTop + toolsNeedHeight);

		}else if (toolsLocationState == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
			toolsBgLeft = contentBgRight +DragTempContainer.perToolsMargin;
			toolsBgRight = contentBgRight + toolsNeedWidth + DragTempContainer.perToolsMargin;
			toolsBgTop = contentBgTop ;
			toolsBgBottom = contentBgTop + toolsNeedHeight;
		}
	}
	/**
	 * 根据参照物的四个边，及工具栏的宽高 确定工具栏绘制的位置
	 * @param left 参照物左边
	 * @param right 参照物右边
	 * @param top   参照物顶边
	 * @param bottom    参照物底边
	 * @param toolsWidth 工具栏宽度
	 * @param toolsHeight   工具栏高度
	 * @param extraDistance 额外添加的距离，针对于绘制线 和矩形时 温度图片的边界在边缘
	 * @return 左上 左对齐 右上 右对齐
	 */
	private void getToolsDirection(int left , int right , int top , int bottom , int toolsWidth , int toolsHeight , int extraDistance){
		//tools Direction
//		int length = toolsHeight - (perToolsWidthHeightSet - perToolsMargin);
		if ((right + toolsWidth >= moveMaxWidth)
				&& ((top - DragTempContainer.perToolsWidthHeightSet -DragTempContainer.perToolsMargin) >= 0)){//tools_left_top
			//从顶部向底部绘制
			toolsLocationState = WIDGET_DIRECTION_STATE_LEFT_TOP;
		}else if ((right + toolsWidth >= moveMaxWidth) && ((top - DragTempContainer.perToolsWidthHeightSet - DragTempContainer.perToolsMargin) < 0)){//tools_left_bottom
			toolsLocationState = WIDGET_DIRECTION_STATE_LEFT_BOTTOM;//从底部 向 顶部绘制
		}else if ((right + toolsWidth < moveMaxWidth) && ((top - DragTempContainer.perToolsWidthHeightSet - DragTempContainer.perToolsMargin) >= 0)){//tools_right_top
			toolsLocationState = WIDGET_DIRECTION_STATE_RIGHT_TOP;
		}else if ((right + toolsWidth < moveMaxWidth) && ((top - DragTempContainer.perToolsWidthHeightSet - DragTempContainer.perToolsMargin) < 0)){//tools_right_bottom
			toolsLocationState = WIDGET_DIRECTION_STATE_RIGHT_BOTTOM;
		}
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

		switch (addTempWidget.getType()){
			case 1://点
				getContentAndBg(addTempWidget.getPointTemp().getStartPointX(),addTempWidget.getPointTemp().getStartPointY(),
						0,0,minTempBt,1);
				//点温度 String
				minTempStr = addTempWidget.getPointTemp().getTemp() + addTempWidget.getTextSuffix();
				//获得文字的宽高 。 通过宽高得到文字绘制的方位 。
				// 通过方位及其 内容坐标或 文字的坐标设置 ，温度文字基准绘制点
				int pointDirection = getTempTextMeasure(pointTextPaint,minTempStr);
				addTempWidget.getPointTemp().setTempDirection(pointDirection);
//				getPointTempTextCoordinateX(addTempWidget.getPointTemp().getStartPointX(),minTempBt,pointDirection,textNeedWidth);
//				getPointTempTextCoordinateY(addTempWidget.getPointTemp().getStartPointY(),minTempBt,pointDirection,textNeedHeight);
				break;
			case 2://线

			case 3://矩形
				getContentAndBg(addTempWidget.getOtherTemp().getStartPointX(),addTempWidget.getOtherTemp().getStartPointY(),
						addTempWidget.getOtherTemp().getEndPointX(),addTempWidget.getOtherTemp().getEndPointY(),null,addTempWidget.getType());
				addTempWidget.setCanMove(true);

				minTempStr = addTempWidget.getOtherTemp().getMinTemp() + addTempWidget.getTextSuffix();
				maxTempStr = addTempWidget.getOtherTemp().getMaxTemp() + addTempWidget.getTextSuffix();
				break;
			default://-1 或其他

				break;
		}
		//是否计算工具栏
		if (addTempWidget.isCanMove()){
			//工具栏总宽高
			toolsNeedWidth = DragTempContainer.perToolsWidthHeightSet + DragTempContainer.perToolsMargin*2;
			toolsNeedHeight = addTempWidget.getToolsNumber() * toolsNeedWidth;//包含了 margin
			//工具栏的放置方位计算
			getToolsDirection(contentBgLeft,contentBgRight,contentBgTop,contentBgBottom,toolsNeedWidth,toolsNeedHeight,0);
			getToolsCoordinate();//得到工具栏起始坐标

			rectContentBg = new RectF(contentBgLeft, contentBgTop, contentBgRight, contentBgBottom);
			rectToolsBg = new RectF(toolsBgLeft,toolsBgTop,toolsBgRight,toolsBgBottom);
		}else {
			addTempWidget.setSelect(false);
		}

		doMeasure();

		rectContent = new RectF(contentLeft,contentTop, contentRight,contentBottom);
	}
	//得到测量的具体值
	private void doMeasure(){
		if (addTempWidget.getType()==1 && !addTempWidget.isCanMove()){
			wMeasureSpec = (contentRight-contentLeft ) + textNeedWidth;
			hMeasureSpec = (contentBottom-contentTop);
		}else if (addTempWidget.getType()==1 && addTempWidget.isCanMove()){
			wMeasureSpec = (contentBgRight - contentBgLeft ) + textNeedWidth;
			hMeasureSpec = (contentBgBottom-contentBgTop);
		}else if (addTempWidget.getType()==2){
			wMeasureSpec = (contentBgRight - contentBgLeft ) + toolsNeedWidth;
			hMeasureSpec = (contentBgBottom-contentBgTop) +toolsNeedHeight;
		}

	}
	/**
	 * 通过传入的起始点坐标得到内容的 坐标 及内容背景坐标
	 * @param left 左边的起始点X
	 * @param top   左边的起始点Y
	 * @param right 右边的起始点X
	 * @param bottom    右边的起始点Y
	 * @param bp       点模式的照片
	 */
	private void getContentAndBg(int left ,int top ,int right, int bottom ,Bitmap bp, int type){
		if (bottom==0 && right ==0){//单个点
			//获取 点 图片 周围四个点
			contentLeft = left - bp.getWidth()/2;
			contentRight = left + bp.getWidth()/2;
			contentTop = top - bp.getHeight()/2;
			contentBottom = top + bp.getHeight()/2;
		}else {
			contentLeft = left;
			contentRight = right;
			contentTop = top;
			contentBottom = bottom;
		}
		//获取 点 背景四个点周彪
		if (type == 3){//矩形自己绘制背景
			contentBgLeft = contentLeft;
			contentBgRight = contentRight + recZoomBoxPaintStroke ;
			contentBgTop = contentTop;
			contentBgBottom = contentBottom;
		}else {
			contentBgLeft = contentLeft - padLeft;
			contentBgRight = contentRight + padRight;
			contentBgTop = contentTop - padTop;
			contentBgBottom = contentBottom + padBottom;
		}
	}



	/**
	 * 通过温度值的 方位，及内容背景计算出点的开始X坐标
	 */
	private int getPointTempTextCoordinateX(int pointX, Bitmap pointBp , int pointDirection, int needWidth){
		//计算温度文字的矩形坐标  从图片边界计算
		int calculateX = 0;
		if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_TOP){
			//(pointX - pointBp.getWidth()/2) == contentLeft
			calculateX = (pointX - pointBp.getWidth()/2) - needWidth ;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
			calculateX = (pointX - pointBp.getWidth()/2) - needWidth ;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_TOP){
			calculateX = (pointX + pointBp.getWidth()/2);
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
			calculateX = (pointX + pointBp.getWidth()/2);
		}
		return calculateX;
	}
	/**
	 * 通过温度值 的方位，及内容背景计算出点的开始X坐标
	 */
	private int getPointTempTextCoordinateY(int pointY,Bitmap pointBp , int pointDirection, int needHeight){
		//计算温度文字的矩形坐标  从图片边界计算
		int calculateY = 0;
		if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_TOP){
			calculateY = pointY - needHeight;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_LEFT_BOTTOM){
			calculateY = pointY+pointBp.getHeight()/2  - needHeight;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_TOP){
			calculateY = pointY - needHeight ;
		}else if (pointDirection == WIDGET_DIRECTION_STATE_RIGHT_BOTTOM){
			calculateY = pointY+pointBp.getHeight()/2  - needHeight;
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
//		if (view.getType()==1){//点
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

//		}else {//线及其矩阵
//		}
	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		Log.e(TAG, "onDraw: ");
		//是否绘制背景
		if (addTempWidget.isCanMove() && addTempWidget.isSelect()){//可选 | 不可选
			//绘制工具栏 背景及其颜色
			if(addTempWidget.getType()==3){// 已选
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
//			canvas.drawCircle(addTempWidget.getPointTemp().getStartPointX(),addTempWidget.getPointTemp().getStartPointY(),2,maxTempTextPaint);

			canvas.drawBitmap(minTempBt,
					contentLeft ,
					contentTop,pointTextPaint);
			//绘制点测温文字

			int textX= getPointTempTextCoordinateX(addTempWidget.getPointTemp().getStartPointX(),minTempBt,
					addTempWidget.getPointTemp().getTempDirection(),textNeedWidth);
			int textY = getPointTempTextCoordinateY(addTempWidget.getPointTemp().getStartPointY(),minTempBt,
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
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		Log.e(TAG, "onMeasure: " +MeasureSpec.getSize(widthMeasureSpec)+ "   height  "+MeasureSpec.getSize(heightMeasureSpec) );
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),MeasureSpec.getSize(heightMeasureSpec));
//		invalidate();
	}


	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}
	//


	@Override
	public boolean onTouchEvent (MotionEvent event) {
//		if (addTempWidget.getType() == 3){
//
//		}else {
//			if (isDebug)Log.e(TAG, "onTouchEvent: getDrawType != 3 不为矩形" );
//		}
		if (isDebug)Log.e(TAG, "onTouchEvent: getDrawType != 3 不为矩形====================" );

		return true;
	}
}
