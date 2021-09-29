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


	private float padLeft, padRight, padTop ,padBottom;

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
	private RectF rectContent , rectContentBg , rectTool , rectToolBg , textRectBg;

	private List<Integer> resBitMapTools;//工具栏的图片资源id
	//工具栏的数量 及其 图片资源的id
	//点击工具栏之后的控制 响应的事件。删除的事件。
	private int perToolsWidthHeightSet;//每个工具栏的宽高

	private int textLocationState , toolLocationState;//文字 和 工具 绘制所处于的状态

	private float contentLeft ,contentRight, contentTop, contentBottom;
	private float contentBgLeft ,contentBgRight, contentBgTop, contentBgBottom;

	private float textNeedWidth ,textNeedHeight  ,   toolsNeedWidth , toolsNeedHeight;

	private float toolsLeft,toolsRight, toolsTop, toolsBottom;
	private float toolsBgLeft,toolsBgRight, toolsBgTop, toolsBgBottom;

	private String minTempStr , maxTempStr;//记录最小最高温度


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

		perToolsWidthHeightSet = 30;//需动态计算 单个工具栏的 宽 高

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
		bgRoundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
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
			//点温度文字 所需要的长宽
			textNeedWidth = minTempStrRect.width();
			textNeedHeight = minTempStrRect.height();

			if (mDefineView.isCanMove()){
				//计算工具栏 工具栏
				toolsNeedWidth = perToolsWidthHeightSet ;
				toolsNeedHeight = mDefineView.getToolsNumber()* perToolsWidthHeightSet;
			}

			getToolsAndTextState();//通过得到的 大小计算 该如何摆放

		}else if (mDefineView.getType()==2 || mDefineView.getType() ==3){

			//计算文字需要的长宽
			minTempStr = mDefineView.getOtherTemp().getMinTemp() + mDefineView.getTextSuffix();
			maxTempStr = mDefineView.getOtherTemp().getMaxTemp() + mDefineView.getTextSuffix();

			minTempTextPaint.getTextBounds(minTempStr,0 , minTempStr.length(),minTempStrRect);
			maxTempTextPaint.getTextBounds(maxTempStr,0 , maxTempStr.length(),maxTempStrRect);
		}




		rectContent = new RectF(contentLeft,contentTop, contentRight,contentBottom);
		rectContentBg = new RectF(contentBgLeft, contentBgTop, contentBgRight, contentBgBottom);

		rectTool = new RectF(perToolsWidthHeightSet,perToolsWidthHeightSet,perToolsWidthHeightSet,perToolsWidthHeightSet);

	}

	//返回 文字 或者工具栏绘制的位置：返回 左上 左下  右上 右下四个地方
	private int getToolsAndTextState(){
		int stateTools = WIDGET_TOOLS_STATE_LEFT_TOP, stateText = WIDGET_TEXT_STATE_LEFT_TOP;
		//tools_state
		if ((contentBgRight + toolsNeedWidth >= moveMaxWidth) && (contentBgTop+toolsNeedHeight < moveMaxHeight)){//tools_left_top
			stateTools = WIDGET_TOOLS_STATE_LEFT_TOP;//从顶部向底部绘制
		}else if ((contentBgRight + toolsNeedWidth >= moveMaxWidth) && (contentBgTop+toolsNeedHeight >= moveMaxHeight)){//tools_left_bottom
			stateTools = WIDGET_TOOLS_STATE_LEFT_BOTTOM;//从底部 向 顶部绘制
		}else if ((contentBgRight + toolsNeedWidth < moveMaxWidth) && (contentBgTop+toolsNeedHeight < moveMaxHeight)){//tools_right_top
			stateTools = WIDGET_TOOLS_STATE_RIGHT_TOP;
		}else if ((contentBgRight + toolsNeedWidth < moveMaxWidth) && (contentBgTop+toolsNeedHeight >= moveMaxHeight)){//tools_right_bottom
			stateTools = WIDGET_TOOLS_STATE_RIGHT_BOTTOM;
		}
		//text state
		if ((contentBgRight + textNeedWidth >= moveMaxWidth) && (contentBgTop+textNeedHeight < moveMaxHeight)){//text_left_top
			stateText = WIDGET_TEXT_STATE_LEFT_TOP;
		}else if ((contentBgRight + textNeedWidth >= moveMaxWidth) && (contentBgTop+textNeedHeight >= moveMaxHeight)){//text_left_bottom
			stateText = WIDGET_TEXT_STATE_LEFT_BOTTOM;//从底部 向 顶部绘制
		}else if ((contentBgRight + textNeedWidth < moveMaxWidth) && (contentBgTop+textNeedHeight < moveMaxHeight)){//text_right_top
			stateText = WIDGET_TEXT_STATE_RIGHT_TOP;
		}else if ((contentBgRight + textNeedWidth < moveMaxWidth) && (contentBgTop+textNeedHeight >= moveMaxHeight)){//text_right_bottom
			stateText = WIDGET_TEXT_STATE_RIGHT_BOTTOM;
		}
		return stateTools|stateText;
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
	 *绘制工具栏 拿绘制的对象、 访问得到的屏幕宽高  ，计算控件（图片）需要的宽高 ，传入对应的pad
	 * @param view 传入的视图对象
	 * @param toolsNumber  工具栏目个数
	 * @param res   每个栏目的图片按钮资源（大小 按照第一个图片的大小绘制）
	 * @param canvas 需要绘制的画布
	 */
	private void drawTool(@NonNull AddTempWidget view,@NonNull int toolsNumber ,int []res,@NonNull Canvas canvas){
		float needWidth , needHeight;//需要的宽高
		needWidth = needHeight = 0;
		if (res.length==toolsNumber){
			Bitmap bitmap_Tools1 = BitmapFactory.decodeResource(getResources(),res[0]);
			Log.e(TAG, "drawTool: need width == " + bitmap_Tools1.getWidth() + " needh  = " + bitmap_Tools1.getHeight() );
			needWidth = bitmap_Tools1.getWidth();
			needHeight = bitmap_Tools1.getHeight()*toolsNumber;
			Log.e(TAG, "drawTool: need width == " + needWidth + " needh  = " + needHeight + "  res.length == toolsNumber");
		}
//		else {//所给的工具栏图片和数目不一致，则所有的工具栏都使用第一个的图片.数目为toolsNumber的值
//			Bitmap bitmap_Tools1;
//			if (res!=null){
//				bitmap_Tools1 = BitmapFactory.decodeResource(getResources(),res[0]);
//			}else {//default img
////				bitmap_Tools1 =
//			}
//
//			needWidth = bitmap_Tools1.getWidth();
//			needHeight = bitmap_Tools1.getHeight()*toolsNumber;
//		}

		//起始点
		float drawStartX, drawStartY ;
		if (view.getType()==1){//点
			//根据计算的状态  来计算开始的坐标
			drawStartX = view.getPointTemp().getStartPointX() + padRight  + minTempBt.getWidth()/2.0f + needWidth/2.0f + 3;
			drawStartY = view.getPointTemp().getStartPointY() - padTop - minTempBt.getWidth()/2.0f - needHeight/2.0f - 3;

			//绘制工具栏圆角背景
			float roundLeft ,roundRight,roundTop, roundBottom ;

			roundLeft = view.getPointTemp().getStartPointX() + padRight  + minTempBt.getWidth()/2.0f  + 3;
			roundRight = roundLeft + needWidth + 3;
			roundTop = view.getPointTemp().getStartPointY() - padTop - minTempBt.getWidth()/2.0f - needHeight/2.0f - 3;
			roundBottom = roundTop + needHeight + 9;

			canvas.drawRoundRect(roundLeft,roundTop,roundRight,roundBottom,6,6,bgRoundPaint);

			//绘制工具栏
			for (int i = 0 ;i < toolsNumber ; i++){
				canvas.drawBitmap(BitmapFactory.decodeResource(getResources(),res[0]),drawStartX,drawStartY+(needHeight/toolsNumber)*i,pointPaint);
			}


		}else {//线及其矩阵
			float rightX , rightY ;//值最大的点：矩阵右下的点  线右边
			rightX = Math.max(view.getOtherTemp().getStartPointX(), view.getOtherTemp().getEndPointX());
			rightY = Math.max(view.getOtherTemp().getStartPointY(), view.getOtherTemp().getEndPointY());


		}

	}

	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		Log.e(TAG, "onDraw: ");
		if (mDefineView.getType() ==1){
			Log.e(TAG, "onDraw: 11111111");
			if (mDefineView.isSelect()){
				//绘制背景
				float bgLeft ,bgRight ,bgTop ,bgBottom;
				bgLeft = mDefineView.getPointTemp().getStartPointX() - minTempBt.getWidth()/2.0f -padLeft;
				bgRight = mDefineView.getPointTemp().getStartPointX() + minTempBt.getWidth()/2.0f + padRight;
				bgTop = mDefineView.getPointTemp().getStartPointY() - minTempBt.getHeight()/2.0f - padTop;
				bgBottom = mDefineView.getPointTemp() .getStartPointY() + minTempBt.getHeight()/2.0f + padBottom;

				pointBgRectF = new RectF(bgLeft,bgTop,bgRight,bgBottom);

				canvas.drawRoundRect(pointBgRectF,minTempBt.getWidth()/4.0f,minTempBt.getWidth()/4.0f,bgRoundPaint);

				//绘制工具栏 也有四个边角  假设工具栏长宽为 toolW toolH
				//工具栏坐标 右上角：X  == startPointX + minTempBt.getWidth/2.0f + padRight    Y == startPintY - minTempBt.getHeight /2.0f - padTop - toolH/2.0f

				drawTool(mDefineView,2,new int[]{R.mipmap.cursorred,R.mipmap.cursorblue},canvas);


			}

//			canvas.drawBitmap(minTempBt,mDefineView.getStartPointX()- minTempBt.getWidth()/2.0f,mDefineView.getStartPointY()-minTempBt.getHeight()/2.0f,pointPaint);
//			String tempStr = mDefineView.getMinTemp() + mDefineView.getTextSuffix();
//			//计算需要的宽度 高度 及其 设置保存 控件的状态。
//			float needWidth , needHeight ,newith;
//			float tempWidth = pointTextPaint.measureText(tempStr);//文字需要的宽度
//			needWidth = tempWidth ;//图片宽度的一半 + 文字的宽度 + 间隔宽度
//
//			Rect rect = new Rect();
//			pointTextPaint.getTextBounds(tempStr,0 , tempStr.length(),rect);
//			needHeight = rect.height();//图片高度的一半+字体的一半+ 顶部间隔
//			newith = rect.width();
//			Log.e(TAG, "onDraw: mDefineView x = " + mDefineView.getStartPointX() + " y = " + mDefineView.getStartPointY());
//			Log.e(TAG, "onDraw: Need w = " + needWidth + " h = " + needHeight +" newith = " +newith);
//			//图片四个角落的基准点baseLine坐标
//			float rightTopX = mDefineView.getStartPointX() + minTempBt.getWidth()/2.0f +padRight;
//			float rightTopY = mDefineView.getStartPointY() - needHeight - padTop;
//			float leftBottomX = mDefineView.getStartPointX() - minTempBt.getWidth()/2.0f - padRight - needWidth;
//			float leftBottomY = mDefineView.getStartPointY() + needHeight + padBottom;
//
//
//			if (rightTopX + needWidth < moveMaxWidth && mDefineView.getStartPointY() - minTempBt.getHeight()/2.0 > 0){//右上角
//				canvas.drawText(tempStr, rightTopX, mDefineView.getStartPointY()-pointTextPaint.descent(),pointTextPaint);
//			}else if (rightTopX + needWidth < moveMaxWidth && mDefineView.getStartPointY() -minTempBt.getHeight()/2.0 <= 0){//右下角
//				canvas.drawText(tempStr, rightTopX, mDefineView.getStartPointY()-pointTextPaint.ascent(),pointTextPaint);
//			}if (rightTopX + needWidth >= moveMaxWidth && mDefineView.getStartPointY() - minTempBt.getHeight()/2.0 > 0){//左上角
//				canvas.drawText(tempStr,leftBottomX,  mDefineView.getStartPointY()-pointTextPaint.descent(),pointTextPaint);
//			}else if (rightTopX + needWidth >= moveMaxWidth && mDefineView.getStartPointY() - minTempBt.getHeight()/2.0 <= 0){//左下角
//				canvas.drawText(tempStr,leftBottomX,  mDefineView.getStartPointY()-pointTextPaint.ascent(),pointTextPaint);
//			}

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
