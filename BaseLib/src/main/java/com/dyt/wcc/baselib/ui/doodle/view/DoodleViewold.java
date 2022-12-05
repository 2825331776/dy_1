package com.dyt.wcc.baselib.ui.doodle.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dyt.wcc.baselib.R;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleColor;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleItem;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleListener;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodlePen;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleShape;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleTouchDetector;
import com.dyt.wcc.baselib.ui.doodle.impl.DoodlePath;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleColor;
import com.dyt.wcc.baselib.ui.doodle.params.DoodlePen;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleShape;
import com.dyt.wcc.baselib.ui.doodle.util.DrawUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.forward.androids.utils.ImageUtils;
import cn.forward.androids.utils.LogUtil;
import cn.forward.androids.utils.Util;

/**
 * <p>Copyright (C),2022/11/29 17:09-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/29 17:09     </p>
 * <p>PackagePath: com.dyt.wcc.wechatpicedit.me.icore.view     </p>
 * <p>Description：涂鸦 总View        </p>
 */
public class DoodleViewold extends FrameLayout implements IDoodle {


	public DoodleViewold (@NonNull Context context) {
		this(context, null);
	}

	public DoodleViewold (@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DoodleViewold (@NonNull Context context, @Nullable AttributeSet attrs,
	                      int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public DoodleViewold (@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr
			, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		initDefaultView(context);

	}


	public final static String TAG                       = "DoodleView";
	//图片最大缩放倍数
	public final static float  MIN_SCALE                 = 0.25f;
	public final static float  MAX_SCALE                 = 5f;
	//默认画笔 strokeWidth
	public final static int    DEFAULT_PAINT_STROKE_SIZE = 6;
	public final static int    DEFAULT_BITMAP            = R.drawable.background_bitmap;

	public final static  int ERROR_INIT                      = -1;
	public final static  int ERROR_SAVE                      = -2;
	//标识：重绘背景，背景待绘制，刷新背景
	private final static int FLAG_RESET_BACKGROUND           = 1 << 1;
	private final static int FLAG_DRAW_PENDING_TO_BACKGROUND = 1 << 2;
	private final static int FLAG_REFRESH_BACKGROUND         = 1 << 3;

	//????  待处理
	private IDoodleListener mDoodleListener;
	//当前涂鸦的原图
	private Bitmap          mBitmap;

	//图片适应屏幕时的 缩放倍数 ，宽高， xy偏移量
	private float mCenterScale;
	private int   mCenterHeight, mCenterWidth;
	private float mCenterTranX, mCenterTranY;
	//在旋转后 适应屏幕 时的缩放倍数,xy偏移量  (可不用，控件未做 单独的旋转)
	private float mRotateScale = 1.0f;
	private float mRotateTranX, mRotateTranY;

	// 在适应屏幕时的缩放基础上的缩放倍数 （ 图片真实的缩放倍数为 mCenterScale*mScale ）
	private float mScale = 1.0f;
	// 图片在适应屏幕且处于居中位置的基础上的偏移量（ 图片真实偏移量为mCentreTranX + mTransX，View窗口坐标系上的偏移）
	private float mTranX = 0, mTranY = 0;
	private float mMinScale = MIN_SCALE;
	private float mMaxScale = MAX_SCALE;

	//画笔属性设置
	private float        mSize;
	private IDoodleColor mColor;
	private IDoodlePen   mPen;
	private IDoodleShape mShape;

	//是否只绘制原图
	private boolean isJustDrawOriginal = false;

	//触摸时，图片区域外 是否绘制 涂鸦轨迹
	private boolean mIsDrawableOutside = false;
	//是否 onMeasure 完毕，onSizeChanged()在onMeasure之后调用
	private boolean mReady             = false;

	//保存涂鸦操作，用于撤销，前进
	private List<IDoodleItem> mItemStack     = new ArrayList<>();
	private List<IDoodleItem> mRedoItemStack = new ArrayList<>();

	//放大镜功能
	private float mTouchX, mTouchY;
/*	private boolean mEnableZoomer = false; // 放大镜功能
	private boolean mEnableOverview = true; // 全图预览功能，建立在放大镜功能开启的前提下
	private float   mLastZoomerY;
	private float   mZoomerRadius;
	private Path    mZoomerPath;
	private float   mZoomerScale = 0; // 放大镜的倍数
	private Paint   mZooomerPaint, mZoomerTouchPaint;
	private int mZoomerHorizonX; // 放大器的位置的x坐标，使其水平居中
	private boolean mIsScrollingDoodle = false; // 是否正在滑动，只要用于标志触摸时才显示放大镜*/

	//长度单位，不同大小的图片的 长度单位不一样。该单位的意义同dp的作用类似，独立于图片之外的单位长度
	private float mDoodleSizeUnit     = 1;
	//相当于初始图片旋转的角度
	private int   mDoodleRotateDegree = 0;

	private Paint mPaint;

	//手势相关
	private IDoodleTouchDetector                  mDefaultTouchDetector;
	private Map<IDoodlePen, IDoodleTouchDetector> mTouchDetectorMap = new HashMap<>();

	//前景图 -> 涂鸦
	private ForeGroundView mForegroundView;
	//保存当前 View 的 矩阵
	private RectF          mDoodleBound = new RectF();
	//临时 缓存点
	private PointF         mTempPoint   = new PointF();

	//是否是编辑模式，可移动缩放涂鸦
	private boolean mIsEditMode = false;
	private boolean mIsSaving   = false;

	/**
	 * Whether or not to optimize drawing, it is suggested to open, which can optimize the drawing
	 * speed and performance.
	 * Note: When item is selected for editing after opening, it will be drawn at the top level,
	 * and not at the corresponding level until editing is completed.
	 * 是否优化绘制，建议开启，可优化绘制速度和性能.
	 * 注意：开启后item被选中编辑时时会绘制在最上面一层，直到结束编辑后才绘制在相应层级
	 **/
	private boolean           mOptimizeDrawing          = true; // 涂鸦及时绘制在图片上，优化性能
	private List<IDoodleItem> mItemStackOnViewCanvas    = new ArrayList<>(); // 这些item绘制在View
	// 的画布上，而不是在图片Bitmap.比如正在创建或选中的item
	private List<IDoodleItem> mPendingItemsDrawToBitmap = new ArrayList<>();
	private Bitmap            mDoodleBitmap;
	private int               mFlags                    = 0;
	private Canvas            mDoodleBitmapCanvas;
	private BackgroundView    mBackgroundView;

	//是否 可编辑 ,
	private boolean isEditable = false;


	/**
	 * 提供给外部调用 ，初始化所有 涂鸦相关变量。
	 *
	 * @param listener
	 */
	public void setListener (IDoodleListener listener, IDoodleTouchDetector defaultTouchDetector) {
		mDoodleListener = listener;
		//init TouchDetector listener
		if (defaultTouchDetector != null) {
			mDefaultTouchDetector = defaultTouchDetector;
		}
		//		if (mDoodleListener == null) {
		//			throw new RuntimeException("IDoodleListener is null!");
		//		}
		//		if (mBitmap == null) {
		//			throw new RuntimeException("Bitmap is null!");
		//		}
	}

	@Override
	public void setBitmap (Bitmap bitmap) {
		mBitmap = bitmap;
	}

	/**
	 * 设置默认手势识别器
	 *
	 * @param touchGestureDetector
	 */
	public void setDefaultTouchDetector (IDoodleTouchDetector touchGestureDetector) {
		mDefaultTouchDetector = touchGestureDetector;
	}


	/**
	 * 默认初始化的 变量
	 *
	 * @param context
	 */
	private void initDefaultView (Context context) {
		setClipChildren(false);
		if (mBitmap == null) {
			mBitmap = BitmapFactory.decodeResource(getResources(), DEFAULT_BITMAP);
		}

		if (mBitmap.getConfig() != Bitmap.Config.RGB_565) {
			Log.e(TAG,
					"DoodleView original Bitmap maybe contain alpha ,which will cause don't " +
							"work well");
		}

		mScale = 1.0f;
		if (mColor == null) {
			mColor = new DoodleColor(Color.RED);
		}
		if (mPen == null) {
			mPen = DoodlePen.BRUSH;
		}
		if (mShape == null) {
			mShape = DoodleShape.HAND_WRITE;
		}

		mForegroundView = new ForeGroundView(context);
		mBackgroundView = new BackgroundView(context);
		addView(mBackgroundView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		addView(mForegroundView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));

		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(Color.RED);
		mPaint.setStrokeWidth(10);
	}

	private void init () {
		int w = mBitmap.getWidth();
		int h = mBitmap.getHeight();
		//bitmap 跟 控件的 比值 bpAndViewWidthPercent ,bpAndViewHeightPercent;
		float bpVW = w * 1f / getWidth();
		float bpVH = h * 1f / getHeight();

		// 比值最大的边缩放，较小的边，为控件的相应宽或高。  ----- 错误这会 铺满整个控件的宽高。
		if (bpVW > bpVH) {
			mCenterScale = 1 / bpVW;//// getWidth /w
			mCenterWidth = getWidth();
			mCenterHeight = (int) (h * mCenterScale);
		} else {
			mCenterScale = 1 / bpVH; // getHeight /h
			mCenterWidth = (int) (w * mCenterScale);
			mCenterHeight = getHeight();
		}
		//图片缩放后 初始化  居中
		mCenterTranX = (getWidth() - mCenterWidth) / 2f;
		mCenterTranY = (getHeight() - mCenterHeight) / 2f;

		// 每dp有多少个 像素/ 缩放比
		mDoodleSizeUnit = Util.dp2px(getContext(), 1) / mCenterScale;

		//只有初始化时 才需要设置画笔大小
		if (!mReady) {
			mSize = DEFAULT_PAINT_STROKE_SIZE * mDoodleSizeUnit;
		}

		//居中适应屏幕
		mTranX = mTranY = 0;
		mScale = 1;

		initDoodleBitmap();
		refreshWithBackground();
	}

	/**
	 * 初始化 mDoodleBitmap ，并 初始化 mDoodleBitmapCanvas 画布
	 */
	private void initDoodleBitmap () {
		if (!mOptimizeDrawing) {
			return;
		}
		if (mDoodleBitmap != null) {
			mDoodleBitmap.recycle();
		}
		mDoodleBitmap = mBitmap.copy(mBitmap.getConfig(), true);
		mDoodleBitmapCanvas = new Canvas(mDoodleBitmap);
	}

	private void refreshWithBackground () {
		addFlag(FLAG_REFRESH_BACKGROUND);
		refresh();
	}

	/**
	 * 获取当前图片 在View坐标系 中的 矩形区域
	 */
	public RectF getDoodleBound () {
		float width = mCenterWidth * mRotateScale * mScale;
		float height = mCenterHeight * mRotateScale * mScale;
		if (mDoodleRotateDegree % 90 == 0) {
			if (mDoodleRotateDegree == 0) {
				mTempPoint.x = toTouchX(0);
				mTempPoint.y = toTouchY(0);
			} else if (mDoodleRotateDegree == 90) {
				mTempPoint.x = toTouchX(0);
				mTempPoint.y = toTouchY(mBitmap.getHeight());
				float t = width;
				width = height;
				height = t;
			} else if (mDoodleRotateDegree == 180) {
				mTempPoint.x = toTouchX(mBitmap.getWidth());
				mTempPoint.y = toTouchY(mBitmap.getHeight());
			} else if (mDoodleRotateDegree == 270) {
				mTempPoint.x = toTouchX(mBitmap.getWidth());
				mTempPoint.y = toTouchY(0);
				float t = width;
				width = height;
				height = t;
			}
			DrawUtil.rotatePoint(mTempPoint, mDoodleRotateDegree, mTempPoint.x, mTempPoint.y,
					getWidth() / 2.0f, getHeight() / 2.0f);
			mDoodleBound.set(mTempPoint.x, mTempPoint.y, mTempPoint.x + width,
					mTempPoint.y + height);
		} else {
			//转换成屏幕坐标
			//左上
			float ltX = toTouchX(0);
			float ltY = toTouchY(0);
			//右下
			float rbX = toTouchX(mBitmap.getWidth());
			float rbY = toTouchY(mBitmap.getHeight());
			//左下
			float lbX = toTouchX(0);
			float lbY = toTouchY(mBitmap.getHeight());
			//右上
			float rtX = toTouchX(mBitmap.getWidth());
			float rtY = toTouchY(0);

			//转换到View坐标系
			DrawUtil.rotatePoint(mTempPoint, mDoodleRotateDegree, ltX, ltY, getWidth() / 2,
					getHeight() / 2);
			ltX = mTempPoint.x;
			ltY = mTempPoint.y;
			DrawUtil.rotatePoint(mTempPoint, mDoodleRotateDegree, rbX, rbY, getWidth() / 2,
					getHeight() / 2);
			rbX = mTempPoint.x;
			rbY = mTempPoint.y;
			DrawUtil.rotatePoint(mTempPoint, mDoodleRotateDegree, lbX, lbY, getWidth() / 2,
					getHeight() / 2);
			lbX = mTempPoint.x;
			lbY = mTempPoint.y;
			DrawUtil.rotatePoint(mTempPoint, mDoodleRotateDegree, rtX, rtY, getWidth() / 2,
					getHeight() / 2);
			rtX = mTempPoint.x;
			rtY = mTempPoint.y;

			mDoodleBound.left = Math.min(Math.min(ltX, rbX), Math.min(lbX, rtX));
			mDoodleBound.top = Math.min(Math.min(ltY, rbY), Math.min(lbY, rtY));
			mDoodleBound.right = Math.max(Math.max(ltX, rbX), Math.max(lbX, rtX));
			mDoodleBound.bottom = Math.max(Math.max(ltY, rbY), Math.max(lbY, rtY));

		}
		return mDoodleBound;
	}

	/**
	 * 将屏幕触摸坐标x转换成在图片中的坐标
	 */
	public final float toX (float touchX) {
		return (touchX - getAllTranX()) / getAllScale();
	}

	/**
	 * 将屏幕触摸坐标y转换成在图片中的坐标
	 */
	public final float toY (float touchY) {
		return (touchY - getAllTranY()) / getAllScale();
	}


	private final float toTouchX (float x) {
		return x * getAllScale() + getAllTranX();
	}

	private final float toTouchY (float y) {
		return y * getAllScale() + getAllTranY();
	}

	private final float toTransX (float touchX, float doodleX) {
		return (-doodleX * getAllScale()) + (touchX - mCenterTranX - mRotateTranX);
	}

	private final float toTransY (float touchY, float doodleY) {
		return (-doodleY * getAllScale()) + (touchY - mCenterTranY - mRotateTranY);
	}

	private void addFlag (int flag) {
		mFlags = mFlags | flag;
	}

	private boolean hasFlag (int flag) {
		return (mFlags & flag) != 0;
	}

	private void clearFlag (int flag) {
		mFlags = mFlags & flag;
	}


	/**
	 * 标志item绘制在View的画布上，而不是在图片Bitmap. 比如正创建或选中的item. 结束绘制时应调用
	 * {@link #notifyItemFinishedDrawing(IDoodleItem)}
	 * 仅在开启优化绘制（mOptimizeDrawing=true）时生效
	 *
	 * @param item
	 */
	public void markItemToOptimizeDrawing (IDoodleItem item) {
		if (!mOptimizeDrawing) {
			return;
		}

		if (mItemStackOnViewCanvas.contains(item)) {
			throw new RuntimeException("The item has been added");
		}
		Log.e(TAG, "markItemToOptimizeDrawing: ---------------------------");
		mItemStackOnViewCanvas.add(item);

		if (mItemStack.contains(item)) {
			addFlag(FLAG_RESET_BACKGROUND);
		}

		refresh();
	}

	/**
	 * 把item从View画布中移除并绘制在涂鸦图片上. 对应 {@link #notifyItemFinishedDrawing(IDoodleItem)}
	 *
	 * @param item
	 */
	public void notifyItemFinishedDrawing (IDoodleItem item) {
		if (!mOptimizeDrawing) {
			return;
		}

		if (mItemStackOnViewCanvas.remove(item)) {
			if (mItemStack.contains(item)) {
				addFlag(FLAG_RESET_BACKGROUND);
			} else {
				addItem(item);
			}
		}

		refresh();
	}

	private void refreshDoodleBitmap (boolean drawAll) {
		if (!mOptimizeDrawing) {
			return;
		}

		initDoodleBitmap();

		List<IDoodleItem> items = null;
		if (drawAll) {
			items = mItemStack;
		} else {
			items = new ArrayList<>(mItemStack);
			items.removeAll(mItemStackOnViewCanvas);
		}
		//绘制所有的
		for (IDoodleItem item : items) {
			item.draw(mDoodleBitmapCanvas);
		}
	}

	private void drawToDoodleBitmap (List<IDoodleItem> items) {
		if (!mOptimizeDrawing) {
			return;
		}
		for (IDoodleItem item : items) {
			item.draw(mDoodleBitmapCanvas);
		}

	}


	//------------------override IDoodle interface-------------------------
	@Override
	public void refresh () {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			super.invalidate();
			mForegroundView.invalidate();
		} else {
			super.postInvalidate();
			mForegroundView.postInvalidate();
		}
	}

	@Override
	public void setColor (IDoodleColor color) {
		mColor = color;
		refresh();
	}

	@Override
	public void addItem (IDoodleItem item) {
		addItemInner(item);
		mRedoItemStack.clear();
	}

	@Override
	public void removeItem (IDoodleItem doodleItem) {
		if (!mItemStack.remove(doodleItem)) {
			return;
		}

		mItemStackOnViewCanvas.remove(doodleItem);
		mPendingItemsDrawToBitmap.remove(doodleItem);
		doodleItem.onRemove();

		addFlag(FLAG_RESET_BACKGROUND);

		refresh();
	}

	private void addItemInner (IDoodleItem item) {
		if (item == null) {
			throw new RuntimeException("item is null");
		}

		if (this != item.getDoodle()) {
			throw new RuntimeException("the object Doodle is illegal");
		}
		if (mItemStack.contains(item)) {
			throw new RuntimeException("the item has been added");
		}

		mItemStack.add(item);
		item.onAdd();

		mPendingItemsDrawToBitmap.add(item);
		addFlag(FLAG_DRAW_PENDING_TO_BACKGROUND);

		refresh();
	}


	@Override
	public void setDoodleMinScale (float minScale) {
		mMinScale = minScale;
		setDoodleScale(mScale, 0, 0);
	}

	@Override
	public float getDoodleMinScale () {
		return mMinScale;
	}


	@Override
	public void setDoodleMaxScale (float maxScale) {
		mMaxScale = maxScale;
		setDoodleScale(mScale, 0, 0);
	}

	@Override
	public float getDoodleMaxScale () {
		return mMaxScale;
	}

	@Override
	public int getItemCount () {
		return mItemStack.size();
	}

	@Override
	public List<IDoodleItem> getAllItem () {
		return new ArrayList<>(mItemStack);
	}

	@Override
	public int getRedoItemCount () {
		return mRedoItemStack.size();
	}

	@Override
	public List<IDoodleItem> getAllRedoItem () {
		return new ArrayList<>(mRedoItemStack);
	}


	/**
	 * 只绘制原图
	 *
	 * @param justDrawOriginal
	 */
	@Override
	public void setShowOriginal (boolean justDrawOriginal) {
		isJustDrawOriginal = justDrawOriginal;
		refreshWithBackground();
	}

	@Override
	public boolean isShowOriginal () {
		return isJustDrawOriginal;
	}

	/**
	 * 保存, 回调DoodleListener.onSaved()的线程和调用save()的线程相同
	 */
	@SuppressLint("StaticFieldLeak")
	@Override
	public void save () {
		if (mIsSaving) {
			return;
		}

		mIsSaving = true;

		new AsyncTask<Void, Void, Bitmap>() {

			@SuppressLint("WrongThread")
			@Override
			protected Bitmap doInBackground (Void... voids) {
				Bitmap savedBitmap = null;

				if (mOptimizeDrawing) {
					refreshDoodleBitmap(true);
					savedBitmap = mDoodleBitmap;
				} else {
					savedBitmap = mBitmap.copy(mBitmap.getConfig(), true);
					Canvas canvas = new Canvas(savedBitmap);
					for (IDoodleItem item : mItemStack) {
						item.draw(canvas);
					}
				}

				savedBitmap = ImageUtils.rotate(savedBitmap, mDoodleRotateDegree, true);
				return savedBitmap;
			}

			@Override
			protected void onPostExecute (Bitmap bitmap) {
				mDoodleListener.onSaved(DoodleViewold.this, bitmap, new Runnable() {
					@Override
					public void run () {
						mIsSaving = false;
						if (mOptimizeDrawing) {
							refreshDoodleBitmap(false);
						}
						refresh();
					}
				});
			}
		}.execute();
	}

	/**
	 * 清屏
	 */
	@Override
	public void clear () {
		List<IDoodleItem> temp = new ArrayList<>(mItemStack);
		mItemStack.clear();
		mRedoItemStack.clear();
		mItemStackOnViewCanvas.clear();
		mPendingItemsDrawToBitmap.clear();

		for (int i = temp.size() - 1; i >= 0; i--) {
			IDoodleItem item = temp.get(i);
			item.onRemove();
		}

		addFlag(FLAG_RESET_BACKGROUND);

		refresh();
	}

	@Override
	public void topItem (IDoodleItem item) {
		if (item == null) {
			throw new RuntimeException("item is null");
		}

		mItemStack.remove(item);
		mItemStack.add(item);

		addFlag(FLAG_RESET_BACKGROUND);

		refresh();
	}

	@Override
	public void bottomItem (IDoodleItem item) {
		if (item == null) {
			throw new RuntimeException("item is null");
		}

		mItemStack.remove(item);
		mItemStack.add(0, item);

		addFlag(FLAG_RESET_BACKGROUND);

		refresh();
	}

	@Override
	public boolean undo (int step) {
		if (mItemStack.size() > 0) {
			step = Math.min(mItemStack.size(), step);
			List<IDoodleItem> list =
					new ArrayList<IDoodleItem>(mItemStack.subList(mItemStack.size() - step,
							mItemStack.size()));
			for (IDoodleItem item : list) {
				removeItem(item);
				mRedoItemStack.add(0, item);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean redo (int step) {
		if (mRedoItemStack.isEmpty()) {
			return false;
		}

		for (int i = 0; i < step && !mRedoItemStack.isEmpty(); i++) {
			addItemInner(mRedoItemStack.remove(0));
		}
		return true;
	}

	/**
	 * 撤销
	 */
	@Override
	public boolean undo () {
		return undo(1);
	}

	@Override
	public Bitmap getBitmap () {
		return mBitmap;
	}

	@Override
	public Bitmap getDoodleBitmap () {
		return mDoodleBitmap;
	}

	@Override
	public IDoodleColor getColor () {
		return mColor;
	}

	@Override
	public float getUnitSize () {
		return mDoodleSizeUnit;
	}

	@Override
	public void setSize (float paintSize) {
		mSize = paintSize;
		refresh();
	}

	@Override
	public float getSize () {
		return mSize;
	}

	@Override
	public void setIsDrawableOutside (boolean isDrawableOutside) {
		mIsDrawableOutside = isDrawableOutside;
	}

	@Override
	public boolean isDrawableOutside () {
		return mIsDrawableOutside;
	}

	@Override
	public void setDoodleRotation (int degree) {
		mDoodleRotateDegree = degree;
		mDoodleRotateDegree = mDoodleRotateDegree % 360;

		//排除负数, 此时
		if (mDoodleRotateDegree < 0) {
			mDoodleRotateDegree = 360 + mDoodleRotateDegree;
		}

		//居中
		RectF rectF = getDoodleBound();
		int w = (int) (rectF.width() / getAllScale());
		int h = (int) (rectF.height() / getAllScale());

		float nw = w * 1f / getWidth();
		float nh = h * 1f / getHeight();
		float scale;
		float tx, ty;
		if (nw > nh) {
			scale = 1 / nw;
		} else {
			scale = 1 / nh;
		}

		int pivotX = mBitmap.getWidth() / 2;
		int pivotY = mBitmap.getHeight() / 2;

		mTranX = mTranY = 0;
		mRotateTranX = mRotateTranY = 0;
		this.mScale = 1;
		mRotateTranX = 1;
		float touchX = toTouchX(pivotX);
		float touchY = toTouchY(pivotY);
		mRotateScale = scale / mCenterScale;

		//缩放后 ，偏移图片,以产生围绕某个点缩放的效果
		tx = toTransX(touchX, pivotX);
		ty = toTransY(touchY, pivotY);

		mRotateTranX = tx;
		mRotateTranY = ty;

		refreshWithBackground();
	}

	@Override
	public int getDoodleRotation () {
		return mDoodleRotateDegree;
	}

	@Override
	public void setDoodleScale (float scale, float pivotX, float pivotY) {
		if (scale < mMinScale) {
			scale = mMinScale;
		} else if (scale > mMaxScale) {
			scale = mMaxScale;
		}

		float touchX = toTouchX(pivotX);
		float touchY = toTouchY(pivotY);
		this.mScale = scale;

		//缩放后，偏移图片，以产生 围绕某个点缩放的效果
		mTranX = toTransX(touchX, pivotX);
		mTranY = toTransY(touchY, pivotY);

		addFlag(FLAG_RESET_BACKGROUND);
		refresh();
	}

	@Override
	public float getDoodleScale () {
		return mScale;
	}

	@Override
	public void setPen (IDoodlePen pen) {
		if (pen == null) {
			throw new RuntimeException("setPenType(),params penType is null!!");
		}
		IDoodlePen oldPenType = mPen;
		mPen = pen;
		refresh();
	}

	@Override
	public IDoodlePen getPen () {
		return mPen;
	}

	@Override
	public void setShape (IDoodleShape penShape) {
		if (penShape == null) {
			throw new RuntimeException("setShape(),params penShape is null!!");
		}
		mShape = penShape;
		refresh();
	}

	@Override
	public IDoodleShape getShape () {
		return mShape;
	}

	@Override
	public void setDoodleTranslation (float transX, float transY) {
		mTranX = transX;
		mTranY = transY;
		refreshWithBackground();
	}

	@Override
	public void setDoodleTranslationX (float transX) {
		mTranX = transX;
		refreshWithBackground();
	}

	@Override
	public float getDoodleTranslationX () {
		return mTranX;
	}

	@Override
	public void setDoodleTranslationY (float transY) {
		mTranY = transY;
		refreshWithBackground();
	}

	@Override
	public float getDoodleTranslationY () {
		return mTranY;
	}

	//---------------------override View or ViewGroup method---------------------------
	private Matrix          mTouchEventMatrix = new Matrix();
	private OnTouchListener mOnTouchListener;

	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		init();

		if (!mReady) {
			if (mDoodleListener != null) {
				mDoodleListener.onReady(this);
			}

			mReady = true;
		}
	}


	@Override
	public boolean dispatchTouchEvent (MotionEvent ev) {
		//不可编辑 则直接返回 true
		if (!isEditable) {
			Log.e(TAG, "DoodleView Editable -------false------");
			return true;
		} else {
			Log.e(TAG, "DoodleView Editable -------true-------");
		}
		if (mOnTouchListener != null) {
			if (mOnTouchListener.onTouch(this, ev)) {
				return true;
			}
		}

		mTouchX = ev.getX();
		mTouchY = ev.getY();

		// 把事件转发给 mForegroundView，避免在区域外不可点击
		MotionEvent transformedEvent = MotionEvent.obtain(ev);
		mTouchEventMatrix.reset();
		mTouchEventMatrix.setRotate(-mDoodleRotateDegree, getWidth() / 2.0f, getHeight() / 2.0f);
		transformedEvent.transform(mTouchEventMatrix);
		boolean handled = mForegroundView.onTouchEvent(transformedEvent);
		transformedEvent.recycle();

		return handled;
	}


	@Override
	protected void dispatchDraw (Canvas canvas) {
		//		super.dispatchDraw(canvas);
		if (mBitmap.isRecycled()) {
			return;
		}
		if (hasFlag(FLAG_RESET_BACKGROUND)) {
			clearFlag(FLAG_RESET_BACKGROUND);
			clearFlag(FLAG_DRAW_PENDING_TO_BACKGROUND);
			clearFlag(FLAG_RESET_BACKGROUND);

			refreshDoodleBitmap(false);
			mPendingItemsDrawToBitmap.clear();
			mBackgroundView.invalidate();
		} else if (hasFlag(FLAG_DRAW_PENDING_TO_BACKGROUND)) {
			clearFlag(FLAG_DRAW_PENDING_TO_BACKGROUND);
			clearFlag(FLAG_REFRESH_BACKGROUND);
			drawToDoodleBitmap(mPendingItemsDrawToBitmap);
			mPendingItemsDrawToBitmap.clear();

			mBackgroundView.invalidate();
		} else if (hasFlag(FLAG_REFRESH_BACKGROUND)) {
			clearFlag(FLAG_REFRESH_BACKGROUND);
			mBackgroundView.invalidate();
		}

		int count = canvas.save();
		super.dispatchDraw(canvas);
		canvas.restoreToCount(count);

		//.......放大镜代码省略
	}

	//----------------------getter - setter ---------------------------------


	public void setBitMap (Bitmap bitmap) {
		mBitmap = bitmap;
		if (mBitmap.getConfig() != Bitmap.Config.RGB_565) {
			// 如果位图包含透明度，则可能会导致橡皮擦无法对透明部分进行擦除
			LogUtil.w(TAG,
					"the bitmap may contain alpha, which will cause eraser don't work well" + ".");
		}

		init();
	}


	public float getScale () {
		return mScale;
	}

	public boolean isEditable () {
		return isEditable;
	}

	public void setEditable (boolean editable) {
		isEditable = editable;
	}

	/**
	 * 设置 适应屏幕时 的缩放比
	 *
	 * @param mScale
	 */
	public void setScale (float mScale) {
		this.mScale = mScale;
	}

	public boolean isOptimizeDrawing () {
		return mOptimizeDrawing;
	}

	public void setOptimizeDrawing (boolean mOptimizeDrawing) {
		this.mOptimizeDrawing = mOptimizeDrawing;
	}

	public float getAllTranX () {
		return mCenterTranX + mRotateTranX + mTranX;
	}

	public float getAllTranY () {
		return mCenterTranY + mRotateTranY + mTranY;
	}

	public float getAllScale () {
		return mCenterScale * mRotateScale * mScale;
	}

	//------------------add define view ---------------------------------------
	private class ForeGroundView extends View {
		public ForeGroundView (Context context) {
			super(context);
			setLayerType(LAYER_TYPE_SOFTWARE, null);
		}

		@Override
		public boolean onTouchEvent (MotionEvent event) {
			Log.e(TAG,
					"--------------- Foreground onTouchEvent------------- " + event.getAction());
			IDoodleTouchDetector detector = mTouchDetectorMap.get(mPen);
			if (detector != null) {
				return detector.onTouchEvent(event);
			}
			if (mDefaultTouchDetector != null) {
				return mDefaultTouchDetector.onTouchEvent(event);
			}

			return false;
		}

		@Override
		protected void onDraw (Canvas canvas) {
			Log.e(TAG, "------Foreground onDraw---------------------");

			int count = canvas.save();
			canvas.rotate(mDoodleRotateDegree, getWidth() / 2.0f, getHeight() / 2.0f);

			doDraw(canvas);

			canvas.restoreToCount(count);

			//			super.onDraw(canvas);
		}

		private void doDraw (Canvas canvas) {
			if (isJustDrawOriginal) {
				return;
			}

			float left = getAllTranX();
			float top = getAllTranY();

			canvas.translate(left, top);
			float scale = getAllScale();
			canvas.scale(scale, scale);

			Bitmap bitmap = mOptimizeDrawing ? mDoodleBitmap : mBitmap;
			//
			Log.e(TAG,
					"doDraw:  -------------bitmap---->" + bitmap.getWidth() + "  height==>" + bitmap.getHeight());

			int saveCount = canvas.save();
			List<IDoodleItem> items = mItemStack;
			if (mOptimizeDrawing) {
				items = mItemStackOnViewCanvas;
			}

			Log.e(TAG, "doDraw:  背景图 打印 绘制items 的大小:" + mItemStackOnViewCanvas.size());

			boolean canvasClipped = false;
			if (!mIsDrawableOutside) {
				canvasClipped = true;
				canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
			}
			//测试 绘制
			canvas.drawCircle(200, 200, 200, mPaint);

			//绘制 item
			for (IDoodleItem item : items) {
				Log.e(TAG,
						"doDraw: -------item:" + item.getColor() + " shape=>" + item.getShape() +
								" path：" + ((DoodlePath) item).getPath().toString());

				if (!item.isNeedClipOutside()) {
					if (canvasClipped) {
						canvas.restore();
					}
					item.draw(canvas);

					if (canvasClipped) {
						canvas.save();
						canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
					}

				} else {
					item.draw(canvas);
				}
			}

			//draw at the top
			for (IDoodleItem item : items) {
				if (!item.isNeedClipOutside()) {
					if (canvasClipped) {
						canvas.restore();
					}
					item.drawAtTheTop(canvas);

					if (canvasClipped) {
						canvas.save();
						canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
					}

				} else {
					item.drawAtTheTop(canvas);
				}
			}
			canvas.restoreToCount(saveCount);

			if (mPen != null) {
				mPen.drawHelpers(canvas, DoodleViewold.this);
			}
			if (mShape != null) {
				mShape.drawHelpers(canvas, DoodleViewold.this);
			}

		}
	}

	private class BackgroundView extends View {
		public BackgroundView (Context context) {
			super(context);

		}

		@Override
		protected void onDraw (Canvas canvas) {
			//			super.onDraw(canvas);
			Log.e(TAG, "-----Background onDraw----------------------");
			int count = canvas.save();

			canvas.rotate(mDoodleRotateDegree, getWidth() / 2.0f, getHeight() / 2.0f);
			doDraw(canvas);

			canvas.restoreToCount(count);
		}

		private void doDraw (Canvas canvas) {

			float left = getAllTranX();
			float top = getAllTranY();

			//画布和 图片 共用一个坐标系，只需要处理屏幕坐标系到图片 坐标系的映射关系

			canvas.translate(left, top);
			float scale = getAllScale();
			canvas.scale(scale, scale);

			if (isJustDrawOriginal) {
				canvas.drawBitmap(mBitmap, 0, 0, null);
				return;
			}
			//背景也可能用 mDoodleBitmap，但 foregroundView 只是用了 canvas 没绘制bitmap
			Bitmap bitmap = mOptimizeDrawing ? mDoodleBitmap : mBitmap;
			canvas.drawBitmap(bitmap, 0, 0, null);

		}
	}

	//	public static final

	//-----------------------------Native method--------------------------------------------
}
