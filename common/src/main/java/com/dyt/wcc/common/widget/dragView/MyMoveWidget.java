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
	private AddTempWidget mDefineView;

	private int mMinHeight;//最小高度像素点个数   //矩阵类型独有
	private int mMinWidth;//最小宽度像素点个数    //矩阵和线独有

	private boolean isShowBg = false;//是否显示背景

	private TextPaint pointTextPaint, maxTempTextPaint, minTempTextPaint , centerTempTextPaint;//文字的
	private Paint pointPaint ,linePaint;//画点//画线条
	private Paint bgRoundPaint;//绘制背景

	private Context mContext;


	private float padLeft, padRight, padTop ,padBottom;//内容布局的四周margin
	private int perToolsMargin ;//每个工具栏的margin

	private int moveMaxWidth;//能移动的最大宽度和高度（即父控件的长宽）
	private int moveMaxHeight;

	private RectF pointBgRectF;

	//四种状态：左上  左下  右上 右下
	public static final int WIDGET_TOOLS_STATE_LEFT_TOP = 0x0000;
	public static final int WIDGET_TOOLS_STATE_LEFT_BOTTOM = 0x0001;
	public static final int WIDGET_TOOLS_STATE_RIGHT_TOP = 0x0010;
	public static final int WIDGET_TOOLS_STATE_RIGHT_BOTTOM = 0x0011;
	//文字
	public static final int WIDGET_TEXT_STATE_LEFT_TOP = 0x0000;
	public static final int WIDGET_TEXT_STATE_LEFT_BOTTOM = 0x0100;
	public static final int WIDGET_TEXT_STATE_RIGHT_TOP = 0x1000;
	public static final int WIDGET_TEXT_STATE_RIGHT_BOTTOM = 0x1100;



	//内容的矩形、内容背景矩形、 工具图片绘制的矩形、 工具图片的背景 矩形、 文字矩形
	private RectF rectContent , rectContentBg , rectTool , rectToolsBg , textRectBg;

	private List<Integer> resBitMapTools;//工具栏的图片资源id
	//工具栏的数量 及其 图片资源的id
	//点击工具栏之后的控制 响应的事件。删除的事件。
	private int perToolsWidthHeightSet;//每个工具栏的宽高

	private int textLocationState , toolLocationState , widgetLocationState;//文字 和 工具 绘制所处于的状态

	private float contentLeft ,contentRight, contentTop, contentBottom;
	private float contentBgLeft ,contentBgRight, contentBgTop, contentBgBottom;

	private float textNeedWidth ,textNeedHeight  ,   toolsNeedWidth , toolsNeedHeight;

	private float toolsLeft,toolsRight, toolsTop, toolsBottom;
	private float toolsBgLeft,toolsBgRight, toolsBgTop, toolsBgBottom;

	private String minTempStr , maxTempStr;//记录最小最高温度
	private float pointTempTextX ,pointTempTextY;//点温度文字绘制 基准线 坐标


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
		padLeft = padRight = 20.0f;//设置背景间距,动态计算。不同dpi有明显差异  3DP
		padTop = padBottom = 18.0f;


		perToolsWidthHeightSet = 40;//需动态计算 单个工具栏的 宽 高
		perToolsMargin = 2;//每个工具栏的margin  后续改写为动态配置

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
			//获取 点 图片 周围四个点
			contentLeft = mDefineView.getPointTemp().getStartPointX() - minTempBt.getWidth()/2.0f;
			contentRight = mDefineView.getPointTemp().getStartPointX() + minTempBt.getWidth()/2.0f;
			contentTop = mDefineView.getPointTemp().getStartPointY() - minTempBt.getHeight()/2.0f;
			contentBottom = mDefineView.getPointTemp().getStartPointY() + minTempBt.getHeight()/2.0f;
			//获取 点 背景四个点周彪
			contentBgLeft = contentLeft - padLeft;
			contentBgRight = contentRight + padRight;
			contentBgTop = contentTop - padTop;
			contentBgBottom = contentBottom + padBottom;
			//点温度 String
			minTempStr = mDefineView.getPointTemp().getTemp() + mDefineView.getTextSuffix();
			minTempTextPaint.getTextBounds(minTempStr,0 , minTempStr.length(),minTempStrRect);
//			minTempTextPaint.measureText(minTempStr);
			//点温度文字 所需要的长宽
			textNeedWidth = pointTextPaint.measureText(minTempStr);
			textNeedHeight = minTempStrRect.height();

			if (isDebug)Log.e(TAG, "===Text NeedWith : " + textNeedWidth +  "textNeedHeight" + textNeedHeight + " measureWidth" + minTempTextPaint.measureText(minTempStr));

			if (mDefineView.isCanMove()){
				//计算工具栏 工具栏
				toolsNeedWidth = perToolsWidthHeightSet + perToolsMargin*2.0f;
				toolsNeedHeight = mDefineView.getToolsNumber()* toolsNeedWidth;
			}else {
				mDefineView.setSelect(false);
			}

			if (isDebug)
				Log.e(TAG, "toolsNeed: " + toolsNeedWidth + " height == " + toolsNeedHeight );

			widgetLocationState = getToolsAndTextState();//通过得到的 大小计算 该如何摆放
			getToolsAndTextLocation(widgetLocationState);

		}else if (mDefineView.getType()==2 || mDefineView.getType() ==3){
			//获取 点 图片 周围四个点
			contentLeft = mDefineView.getOtherTemp().getStartPointX();
			contentRight = mDefineView.getOtherTemp().getStartPointY();
			contentTop = mDefineView.getOtherTemp().getEndPointX();
			contentBottom = mDefineView.getOtherTemp().getEndPointY() ;
			//获取 点 背景四个点周彪
			contentBgLeft = contentLeft - padLeft;
			contentBgRight = contentRight + padRight;
			contentBgTop = contentTop - padTop;
			contentBgBottom = contentBottom + padBottom;


			//计算文字需要的长宽
			minTempStr = mDefineView.getOtherTemp().getMinTemp() + mDefineView.getTextSuffix();
			maxTempStr = mDefineView.getOtherTemp().getMaxTemp() + mDefineView.getTextSuffix();




			minTempTextPaint.getTextBounds(minTempStr,0 , minTempStr.length(),minTempStrRect);
			maxTempTextPaint.getTextBounds(maxTempStr,0 , maxTempStr.length(),maxTempStrRect);
		}

		rectContent = new RectF(contentLeft,contentTop, contentRight,contentBottom);
		rectContentBg = new RectF(contentBgLeft, contentBgTop, contentBgRight, contentBgBottom);

		rectToolsBg = new RectF(toolsBgLeft,toolsBgTop,toolsBgRight,toolsBgBottom);

	}

	//返回 文字 或者工具栏绘制的位置：返回 左上 左下  右上 右下四个地方
	private int getToolsAndTextState(){
		int stateTools = WIDGET_TOOLS_STATE_LEFT_TOP, stateText = WIDGET_TEXT_STATE_LEFT_TOP;
		//tools_state
		if ((contentBgRight + toolsNeedWidth >= moveMaxWidth)
				&& ((contentTop+contentBottom)/2.0f - toolsNeedHeight >= 0)){//tools_left_top
			stateTools = WIDGET_TOOLS_STATE_LEFT_TOP;//从顶部向底部绘制
		}else if ((contentBgRight + toolsNeedWidth >= moveMaxWidth) && ((contentTop+contentBottom)/2.0f + toolsNeedHeight <= moveMaxHeight)){//tools_left_bottom
			stateTools = WIDGET_TOOLS_STATE_LEFT_BOTTOM;//从底部 向 顶部绘制
		}else if ((contentBgRight + toolsNeedWidth < moveMaxWidth) && ((contentTop+contentBottom)/2.0f - toolsNeedHeight >= 0)){//tools_right_top
			stateTools = WIDGET_TOOLS_STATE_RIGHT_TOP;
		}else if ((contentBgRight + toolsNeedWidth < moveMaxWidth) && ((contentTop+contentBottom)/2.0f + toolsNeedHeight <= moveMaxHeight)){//tools_right_bottom
			stateTools = WIDGET_TOOLS_STATE_RIGHT_BOTTOM;
		}

		//text state
		if ((contentBgRight + textNeedWidth >= moveMaxWidth) && (contentTop - 2.0f*textNeedHeight >= 0)){//text_left_top
			stateText = WIDGET_TEXT_STATE_LEFT_TOP;
		}else if ((contentBgRight + textNeedWidth >= moveMaxWidth) && ((contentTop+contentBottom)/2.0f + textNeedHeight <= moveMaxHeight)){//text_left_bottom
			stateText = WIDGET_TEXT_STATE_LEFT_BOTTOM;//从底部 向 顶部绘制
		}else if ((contentBgRight + textNeedWidth < moveMaxWidth) && (contentTop- 2.0f*textNeedHeight >= 0)){//text_right_top
			stateText = WIDGET_TEXT_STATE_RIGHT_TOP;
		}else if ((contentBgRight + textNeedWidth < moveMaxWidth) && ((contentTop+contentBottom)/2.0f+textNeedHeight <= moveMaxHeight)){//text_right_bottom
			stateText = WIDGET_TEXT_STATE_RIGHT_BOTTOM;
		}

		Log.e(TAG, "getToolsAndTextState: " + stateText + " tools = " + stateTools);
		return stateTools|stateText;
	}

	//通过得到的位置状态 去计算工具栏的起始点，及 文字的起始点
	//工具栏是后八位， 文字是前八位     //绘制点
	private void getToolsAndTextLocation(int toolsAndTextState){
		int toolsDirectionState , textDirectionState;
		textDirectionState = toolsAndTextState & 0xff00;//取高八位
		toolsDirectionState = toolsAndTextState & 0x00ff;//取低八位

		Log.e(TAG, "getToolsAndTextLocation: = " + textDirectionState + " tools = " + toolsDirectionState);

			//计算工具栏的背景图四个坐标
		if (toolsDirectionState == WIDGET_TOOLS_STATE_LEFT_TOP){
			toolsBgLeft = contentBgLeft - toolsNeedWidth;
			toolsBgRight = contentBgLeft;
			toolsBgTop = (contentBgTop + contentBgBottom) / 2.0f - toolsNeedHeight;
			toolsBgBottom = (contentBgTop + contentBgBottom) / 2.0f;

		}else if (toolsDirectionState == WIDGET_TOOLS_STATE_LEFT_BOTTOM){
			toolsBgLeft = contentBgLeft - toolsNeedWidth;
			toolsBgRight = contentBgLeft;
			toolsBgTop = (contentBgTop + contentBgBottom) / 2.0f;
			toolsBgBottom = (contentBgTop + contentBgBottom) / 2.0f + toolsNeedHeight;

		}else if (toolsDirectionState == WIDGET_TOOLS_STATE_RIGHT_TOP){
			toolsBgLeft = contentBgRight ;
			toolsBgRight = contentBgRight + toolsNeedWidth;
			toolsBgTop = (contentBgTop+contentBgBottom) / 2.0f - toolsNeedHeight;
			toolsBgBottom = (contentBgTop+contentBgBottom) / 2.0f;

		}else if (toolsDirectionState == WIDGET_TOOLS_STATE_RIGHT_BOTTOM){
			toolsBgLeft = contentBgRight ;
			toolsBgRight = contentBgRight + toolsNeedWidth;
			toolsBgTop = (contentBgTop+contentBgBottom) / 2.0f;
			toolsBgBottom = (contentBgTop+contentBgBottom) / 2.0f + toolsNeedHeight;
		}
		//计算温度文字的矩形坐标
		if (textDirectionState == WIDGET_TEXT_STATE_LEFT_TOP){
			pointTempTextX = contentBgLeft-textNeedWidth ;
			pointTempTextY = (contentBgBottom + contentBgTop - textNeedHeight) / 2.0f ;
		}else if (textDirectionState == WIDGET_TEXT_STATE_LEFT_BOTTOM){
			pointTempTextX = contentBgLeft -
					textNeedWidth ;
			pointTempTextY = contentBgBottom  - textNeedHeight/2.0f;
		}else if (textDirectionState == WIDGET_TEXT_STATE_RIGHT_TOP){
			pointTempTextX = contentBgRight ;
			pointTempTextY = (contentBgBottom + contentBgTop- textNeedHeight ) / 2.0f ;
		}else if (textDirectionState == WIDGET_TEXT_STATE_RIGHT_BOTTOM){
			pointTempTextX = contentBgRight  ;
			pointTempTextY = contentBgBottom  - textNeedHeight/2.0f;
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
					top = toolsBgTop + perToolsMargin + (perToolsWidthHeightSet+ 2.0f* perToolsMargin)* i ;
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
			canvas.drawCircle(mDefineView.getPointTemp().getStartPointX(),mDefineView.getPointTemp().getStartPointY(),2,maxTempTextPaint);

			Log.e(TAG, "onDraw: 11111111");
			if (mDefineView.isSelect()){//选中状态
				canvas.drawRoundRect(rectContentBg,minTempBt.getWidth()/4.0f,minTempBt.getWidth()/4.0f,bgRoundPaint);
				drawTool(mDefineView,canvas);//绘制工具栏 背景及其颜色
			}
			canvas.drawBitmap(minTempBt,
					contentLeft ,
					contentTop,pointTextPaint);

			canvas.drawText(minTempStr,pointTempTextX,pointTempTextY,pointTextPaint);
		}
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
