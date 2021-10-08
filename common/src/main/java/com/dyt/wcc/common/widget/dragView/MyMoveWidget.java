package com.dyt.wcc.common.widget.dragView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.dyt.wcc.common.R;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/27  9:56     </p>
 * <p>Description：三类可以拖动的View</p>
 * <p>PackagePath: com.dyt.wcc.common.widget     </p>
 */
public class MyMoveWidget extends ConstraintLayout {
	private static final boolean isDebug = true;
	private static final String TAG = "MyMoveWidget";

	private Bitmap maxTempBt, minTempBt;//最小温度，最大温度图片（单点只有最小温度的图片）
	private AddTempWidget mDefineView;//每次绘制的 数据源

	private int mMinHeight;//最小高度像素点个数   //矩阵类型独有
	private int mMinWidth;//最小宽度像素点个数    //矩阵和线独有

	private boolean isShowBg = false;//是否显示背景

	private TextPaint pointTextPaint, maxTempTextPaint, minTempTextPaint , centerTempTextPaint;//文字的
	private Paint pointPaint ,linePaint;//画点//画线条
	private Paint bgRoundPaint;//绘制背景

	private Context mContext;


	private int padLeft, padRight, padTop ,padBottom;//内容布局的四周margin
	private int perToolsMargin ;//每个工具栏的margin

	private int moveMaxWidth;//能移动的最大宽度和高度（即父控件的长宽）
	private int moveMaxHeight;

	private RectF pointBgRectF;

	//四种状态：左上  左下  右上 右下
	public static final int WIDGET_TOOLS_STATE_LEFT_TOP = 0x0000;
	public static final int WIDGET_TOOLS_STATE_LEFT_BOTTOM = 0x0001;
	public static final int WIDGET_TOOLS_STATE_RIGHT_TOP = 0x0010;
	public static final int WIDGET_TOOLS_STATE_RIGHT_BOTTOM = 0x0011;
	//最高最低文字的方位
	public static final int WIDGET_TEXT_STATE_LEFT_TOP = 0x0000;
	public static final int WIDGET_TEXT_STATE_LEFT_BOTTOM = 0x0001;
	public static final int WIDGET_TEXT_STATE_RIGHT_TOP = 0x0010;
	public static final int WIDGET_TEXT_STATE_RIGHT_BOTTOM = 0x0011;



	//内容的矩形、内容背景矩形、 工具图片绘制的矩形、 工具图片的背景 矩形、 文字矩形
	private RectF rectContent , rectContentBg , rectTool , rectToolsBg , textRectBg;

	private List<Integer> resBitMapTools;//工具栏的图片资源id
	//工具栏的数量 及其 图片资源的id
	//点击工具栏之后的控制 响应的事件。删除的事件。
	private int perToolsWidthHeightSet;//每个工具栏的宽高

	private int tempLocationState = WIDGET_TEXT_STATE_LEFT_TOP, toolsLocationState = WIDGET_TOOLS_STATE_LEFT_TOP, widgetLocationState;//文字 和 工具 绘制所处于的状态

	private int contentLeft ,contentRight, contentTop, contentBottom;
	private int contentBgLeft ,contentBgRight, contentBgTop, contentBgBottom;

	private int textNeedWidth ,textNeedHeight  ,  toolsNeedWidth , toolsNeedHeight;

	private int toolsLeft,toolsRight, toolsTop, toolsBottom;
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
		mDefineView = view;
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


//		perToolsWidthHeightSet = 40;//需动态计算 单个工具栏的 宽 高
		perToolsMargin = 5;//每个工具栏的margin  后续改写为动态配置

		resBitMapTools = new ArrayList<>();

		if (mDefineView.getType()==1){
			minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
			maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorgreen);
		}else {
			minTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorblue);
			maxTempBt = BitmapFactory.decodeResource(getResources(),R.mipmap.cursorred);
		}

//		ConstraintLayout constraintLayout_tool = (ConstraintLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_move_widget,this,true);
//		ConstraintLayout.LayoutParams  params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//		constraintLayout_tool.setLayoutParams(params);
//		toolView = constraintLayout_tool.findViewById(R.id.conLayout_tools);
//		if (!mDefineView.isSelect()){
//			toolView.setVisibility(GONE);
//		}
//		addView(constraintLayout_tool);
	}

	private void initPaint(){
		Log.e(TAG, "initPaint: ");
		pointPaint = new Paint();
		pointPaint.setColor(getResources().getColor(R.color.bg_preview_toggle_select));
		linePaint = new Paint();
		linePaint.setColor(getResources().getColor(R.color.teal_200));

		pointTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		pointTextPaint.setTextSize(mDefineView.getTempTextSize());
		pointTextPaint.setColor(getResources().getColor(R.color.bg_preview_toggle_select));

		maxTempTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		pointTextPaint.setTextSize(mDefineView.getTempTextSize());
		pointTextPaint.setColor(getResources().getColor(R.color.max_temp_text_color_red));
		minTempTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		pointTextPaint.setTextSize(mDefineView.getTempTextSize());
		pointTextPaint.setColor(getResources().getColor(R.color.min_temp_text_color_blue));

		bgRoundPaint = new Paint();
		bgRoundPaint.setStyle(Paint.Style.FILL);
		bgRoundPaint.setColor(getResources().getColor(R.color.bg_move_layout_round_bg));
		bgRoundPaint.setAlpha(100);//透明度 0透明-255不透明
		bgRoundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);//抗锯齿
	}

	//初始化数据
	private void initData(){
		resBitMapTools.add(R.drawable.ic_move_tools);

		Rect minTempStrRect = new Rect();
		Rect maxTempStrRect = new Rect();

		//得到 内容坐标， 内容背景坐标  工具栏 大小  判断工具栏 方位 ，文字方位
		if (mDefineView.getType() ==1 ) {//得到周边的点的坐标
			getContentAndBg(mDefineView.getPointTemp().getStartPointX(),mDefineView.getPointTemp().getStartPointY(),
					0,0,minTempBt,mDefineView.getType());

			//点温度 String
			minTempStr = mDefineView.getPointTemp().getTemp() + mDefineView.getTextSuffix();
			minTempTextPaint.getTextBounds(minTempStr,0 , minTempStr.length(),minTempStrRect);
			//点温度文字 所需要的长宽
			textNeedWidth = (int) Math.max(minTempStrRect.width(),minTempTextPaint.measureText(minTempStr));
			textNeedHeight = minTempStrRect.height();

			if (mDefineView.isCanMove()){
				//计算工具栏 工具栏
				perToolsWidthHeightSet = (contentBgRight - contentBgLeft)/2;
				toolsNeedWidth = perToolsWidthHeightSet + perToolsMargin*2;
				toolsNeedHeight = mDefineView.getToolsNumber() * toolsNeedWidth;//包含了 margin
			}else {
				mDefineView.setSelect(false);
			}

			if (isDebug)Log.e(TAG, "toolsNeed: " + toolsNeedWidth + " height == " + toolsNeedHeight );
			getToolsState(contentBgLeft,contentBgRight,contentTop,contentBottom,toolsNeedWidth,toolsNeedHeight,0);
			getTempTextState(false,minTempStr,minTempTextPaint);
			widgetLocationState = tempLocationState|toolsLocationState;
					;//通过得到的 大小计算 该如何摆放
			getToolsLocation();

		}else if (mDefineView.getType()==2 || mDefineView.getType() ==3){
			getContentAndBg(mDefineView.getOtherTemp().getStartPointX(),mDefineView.getOtherTemp().getStartPointY(),
					mDefineView.getOtherTemp().getEndPointX(),mDefineView.getOtherTemp().getEndPointY(),null,2);

			//计算文字需要的长宽
			minTempStr = mDefineView.getOtherTemp().getMinTemp() + mDefineView.getTextSuffix();
			maxTempStr = mDefineView.getOtherTemp().getMaxTemp() + mDefineView.getTextSuffix();

			textNeedWidth = (int) Math.max( pointTextPaint.measureText(minTempStr), pointTextPaint.measureText(maxTempStr));//拿到字体的最长的
			textNeedHeight = minTempStrRect.height();
			//通过文字的宽高 和计算传入的四个边界坐标，及其总体宽高计算位置


			minTempTextPaint.getTextBounds(minTempStr,0 , minTempStr.length(),minTempStrRect);
			maxTempTextPaint.getTextBounds(maxTempStr,0 , maxTempStr.length(),maxTempStrRect);

			getToolsState(contentBgLeft,contentBgRight,contentTop,contentBottom,toolsNeedWidth,toolsNeedHeight,0);
			getTempTextState(false,minTempStr,minTempTextPaint);//线和矩阵有两个文字温度
			getTempTextState(true,maxTempStr,maxTempTextPaint);
			widgetLocationState = tempLocationState|toolsLocationState;
		}

		rectContent = new RectF(contentLeft,contentTop, contentRight,contentBottom);
		rectContentBg = new RectF(contentBgLeft, contentBgTop, contentBgRight, contentBgBottom);

		rectToolsBg = new RectF(toolsBgLeft,toolsBgTop,toolsBgRight,toolsBgBottom);
	}
	/**
	 * 通过传入的起始点坐标得到内容的 坐标 及内容背景坐标
	 * @param left 左边的起始点X
	 * @param top   左边的起始点Y
	 * @param right 右边的起始点X
	 * @param bottom    右边的起始点Y
	 * @param bp       点模式的照片
	 * @param type  绘制的类型， 点线矩形
	 */
	private void getContentAndBg(int left ,int top ,int right, int bottom ,Bitmap bp ,int type){
		if (type ==1){
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
		contentBgLeft = contentLeft - padLeft;
		contentBgRight = contentRight + padRight;
		contentBgTop = contentTop - padTop;
		contentBgBottom = contentBottom + padBottom;
	}

	/**
	 * 获取文字 绘制的方位保存在成员变量textLocationState中
	 * @param tempX
	 * @param tempY
	 * @param temp
	 * @param tempTextSuffix
	 * @param tempBp
	 * @return
	 */
	private int getTempTextState(int tempX, int tempY, float temp, String tempTextSuffix , Bitmap tempBp){
		int tempState = WIDGET_TEXT_STATE_LEFT_TOP;




		return tempLocationState;
	}

	//返回 文字 或者工具栏绘制的位置：返回 左上 左下  右上 右下四个地方

	/**
	 * 根据参照物的四个边，及工具栏的宽高 确定工具栏绘制的位置
	 * @param left 参照物左边
	 * @param right 参照物右边
	 * @param top   参照物顶边
	 * @param bottom    参照物底边
	 * @param toolsWidth 工具栏宽度
	 * @param toolsHeight   工具栏高度
	 * @param extraDistance 额外添加的距离，针对于绘制线 和矩形时 温度图片的边界在边缘
	 * @return 左上 左下 右上 右下
	 */
	private void getToolsState(int left , int right , int top , int bottom , int toolsWidth , int toolsHeight , int extraDistance){
		//tools_state
		if ((right + toolsWidth >= moveMaxWidth)
				&& ((top + bottom)/2.0f - toolsHeight >= 0)){//tools_left_top
			//从顶部向底部绘制
			toolsLocationState = WIDGET_TOOLS_STATE_LEFT_TOP;
		}else if ((right + toolsWidth >= moveMaxWidth) && ((top+bottom)/2.0f + toolsHeight <= moveMaxHeight)){//tools_left_bottom
			toolsLocationState = WIDGET_TOOLS_STATE_LEFT_BOTTOM;//从底部 向 顶部绘制
		}else if ((right + toolsWidth < moveMaxWidth) && ((top+bottom)/2.0f - toolsHeight >= 0)){//tools_right_top
			toolsLocationState = WIDGET_TOOLS_STATE_RIGHT_TOP;
		}else if ((right + toolsWidth < moveMaxWidth) && ((top+bottom)/2.0f + toolsHeight <= moveMaxHeight)){//tools_right_bottom
			toolsLocationState = WIDGET_TOOLS_STATE_RIGHT_BOTTOM;
		}
	}
	//得到文字的方位
	private void getTempTextState(boolean isLeft ,String maxOrMinTempStr , TextPaint maxOrMinTempTextPaint){
		//text state  //图片背景周边开始计算
		int saveState = tempLocationState;

		if ((contentBgRight + textNeedWidth >= moveMaxWidth) && (contentBgTop - 2.0f*textNeedHeight >= 0)){//text_left_top
			tempLocationState = WIDGET_TEXT_STATE_LEFT_TOP;
		}else if ((contentBgRight + textNeedWidth >= moveMaxWidth) && ((contentTop+contentBottom)/2.0f + textNeedHeight <= moveMaxHeight)){//text_left_bottom
			tempLocationState = WIDGET_TEXT_STATE_LEFT_BOTTOM;//从底部 向 顶部绘制
		}else if ((contentBgRight + textNeedWidth < moveMaxWidth) && (contentBgTop - 2.0f*textNeedHeight >= 0)){//text_right_top
			tempLocationState = WIDGET_TEXT_STATE_RIGHT_TOP;
		}else if ((contentBgRight + textNeedWidth < moveMaxWidth) && ((contentTop + contentBottom)/2.0f+textNeedHeight <= moveMaxHeight)){//text_right_bottom
			tempLocationState = WIDGET_TEXT_STATE_RIGHT_BOTTOM;
		}

		if (isLeft){
			saveState = saveState<<8;
			tempLocationState = saveState|tempLocationState;
		}
	}

	//通过得到的位置状态 去计算工具栏的起始点
	//工具栏是后八位， 文字是前八位     //绘制点
	private void getToolsLocation(){
			//计算工具栏的背景图四个坐标 加上间隔的距离
		if (toolsLocationState == WIDGET_TOOLS_STATE_LEFT_TOP){
			toolsBgLeft = contentBgLeft - toolsNeedWidth - perToolsMargin;
			toolsBgRight = contentBgLeft - perToolsMargin;
			toolsBgTop = (contentBgTop + contentBgBottom) / 2 - toolsNeedHeight;
			toolsBgBottom = (contentBgTop + contentBgBottom) / 2;

		}else if (toolsLocationState == WIDGET_TOOLS_STATE_LEFT_BOTTOM){
			toolsBgLeft = contentBgLeft - toolsNeedWidth - perToolsMargin;
			toolsBgRight = contentBgLeft - perToolsMargin;
			toolsBgTop = (contentBgTop + contentBgBottom) / 2;
			toolsBgBottom = (contentBgTop + contentBgBottom) / 2 + toolsNeedHeight;

		}else if (toolsLocationState == WIDGET_TOOLS_STATE_RIGHT_TOP){
			toolsBgLeft = contentBgRight + perToolsMargin ;
			toolsBgRight = contentBgRight + toolsNeedWidth + perToolsMargin;
			toolsBgTop = (contentBgTop+contentBgBottom) / 2 - toolsNeedHeight;
			toolsBgBottom = (contentBgTop+contentBgBottom) / 2;

		}else if (toolsLocationState == WIDGET_TOOLS_STATE_RIGHT_BOTTOM){
			toolsBgLeft = contentBgRight +perToolsMargin;
			toolsBgRight = contentBgRight + toolsNeedWidth + perToolsMargin;
			toolsBgTop = (contentBgTop+contentBgBottom) / 2;
			toolsBgBottom = (contentBgTop+contentBgBottom) / 2 + toolsNeedHeight;
		}
	}
	private void getTempTextLocation(){
		//计算温度文字的矩形坐标  从图片边界计算
		if (tempLocationState == WIDGET_TEXT_STATE_LEFT_TOP){
			pointTempTextX = contentLeft - textNeedWidth ;
			pointTempTextY = (contentBottom + contentTop - textNeedHeight) / 2;
		}else if (toolsLocationState == WIDGET_TEXT_STATE_LEFT_BOTTOM){
			pointTempTextX = contentLeft -
					textNeedWidth ;
			pointTempTextY = contentBottom  - textNeedHeight/2;
		}else if (toolsLocationState == WIDGET_TEXT_STATE_RIGHT_TOP){
			pointTempTextX = contentRight;
			pointTempTextY = (contentBottom + contentTop- textNeedHeight ) / 2;
		}else if (toolsLocationState == WIDGET_TEXT_STATE_RIGHT_BOTTOM){
			pointTempTextX = contentRight;
			pointTempTextY = contentBottom  - textNeedHeight / 2;
		}

	}


	public boolean isSelectedState () {
		return mDefineView.isSelect();
	}
	public void setSelectedState (boolean selectedState) {
		if (selectedState){
			mDefineView.setSelect(true);
		}else {
			mDefineView.setSelect(false);
		}
	}

	public AddTempWidget getView () {
		return mDefineView;
	}

	/**
	 * @param view 传入的视图对象
	 * @param canvas 需要绘制的画布
	 *             以工具栏背景的坐标为基准
	 */
	private void drawTool(@NonNull AddTempWidget view,@NonNull Canvas canvas){
		if (view.getType()==1){//点
			canvas.drawRoundRect(rectToolsBg,5,5,bgRoundPaint);
			int [] resPic = view.getToolsPicRes();
			RectF perToolsPic ;
			float left , right , top , bottom;
			if (resPic!= null && view.getToolsNumber() != 0){
				for (int i = 0 ; i < view.getToolsNumber(); i++){
					left = toolsBgLeft + perToolsMargin;
					right =toolsBgRight - perToolsMargin;
					top = toolsBgTop + perToolsMargin + (perToolsWidthHeightSet + 2.0f* perToolsMargin)* i ;
					bottom = toolsBgTop +(perToolsMargin + perToolsWidthHeightSet) + (perToolsWidthHeightSet+ 2.0f* perToolsMargin)* i;

					perToolsPic = new RectF(left,top,right,bottom);

					canvas.drawBitmap(BitmapFactory.decodeResource(getResources(),resPic[i]),null,perToolsPic,pointPaint);
				}
			}

		}else {//线及其矩阵


		}

	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		Log.e(TAG, "onDraw: ");
		if (mDefineView.getType() ==1){
			//绘制标识圆
			if (isDebug)Log.e(TAG, "onDraw: 11111111");
			canvas.drawCircle(mDefineView.getPointTemp().getStartPointX(),mDefineView.getPointTemp().getStartPointY(),2,maxTempTextPaint);
			canvas.drawBitmap(minTempBt,
					contentLeft ,
					contentTop,pointTextPaint);
			canvas.drawText(minTempStr,pointTempTextX,pointTempTextY,pointTextPaint);

			if (mDefineView.isSelect()){//选中状态
				//绘制温度图片的背景
				canvas.drawRoundRect(rectContentBg,minTempBt.getWidth()/4.0f,minTempBt.getWidth()/4.0f,bgRoundPaint);
				drawTool(mDefineView,canvas);//绘制工具栏 背景及其颜色
			}
		}else if (mDefineView.getType() == 2){
			canvas.drawRoundRect(rectContentBg,minTempBt.getWidth()/4.0f,minTempBt.getWidth()/4.0f,bgRoundPaint);
			canvas.drawLine(mDefineView.getOtherTemp().getStartPointX(),mDefineView.getOtherTemp().getStartPointY()
					,mDefineView.getOtherTemp().getEndPointX(),mDefineView.getOtherTemp().getEndPointY(),linePaint);
			//todo 绘制两个点温度
			drawMinMaxTemp(canvas,mDefineView.getOtherTemp().getMinTempX(),mDefineView.getOtherTemp().getMinTempY(),minTempBt,minTempTextPaint);
			drawMinMaxTemp(canvas,mDefineView.getOtherTemp().getMaxTempX(),mDefineView.getOtherTemp().getMaxTempY(),maxTempBt,maxTempTextPaint);


		}
	}

	//绘制最大温度：传入画布 坐标
	private void drawMinMaxTemp(Canvas canvas , int drawMaxTempX, int drawMaxTempY ,Bitmap maxResPic, TextPaint tempPaint){
//		canvas
	}
	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		Log.e(TAG, "onLayout: ");
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		Log.e(TAG, "onMeasure: ");

		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),MeasureSpec.getSize(heightMeasureSpec));
//		invalidate();
	}


	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}
	//

	@Override
	public boolean onInterceptTouchEvent (MotionEvent ev) {//ViewGroup true拦截 super不拦截  false //固定这个返回super，否则无法响应工具栏的事件
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (mDefineView.getType() == 3){

		}else {
			if (isDebug)Log.e(TAG, "onTouchEvent: getDrawType != 3 不为矩形" );
		}
		if (isDebug)Log.e(TAG, "onTouchEvent: getDrawType != 3 不为矩形====================" );

		return true;
	}
}
