package com.huantansheng.easyphotos.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.dyt.wcc.baselib.ui.doodle.params.DoodleColor;
import com.dyt.wcc.baselib.ui.doodle.params.DoodlePen;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleShape;
import com.dyt.wcc.baselib.ui.doodle.view.DoodleView;
import com.dyt.wcc.baselib.ui.widget.CircleDisplayView;
import com.dyt.wcc.baselib.ui.widget.ColorSliderView;
import com.huantansheng.easyphotos.R;
import com.huantansheng.easyphotos.constant.Code;
import com.huantansheng.easyphotos.constant.Key;
import com.huantansheng.easyphotos.databinding.ActivityPreviewEasyPhotosBinding;
import com.huantansheng.easyphotos.models.album.AlbumModel;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.huantansheng.easyphotos.picNative.PhotoHandler;
import com.huantansheng.easyphotos.picNative.PhotoNativeHelper;
import com.huantansheng.easyphotos.result.Result;
import com.huantansheng.easyphotos.setting.Setting;
import com.huantansheng.easyphotos.ui.adapter.PreviewPhotosAdapter;
import com.huantansheng.easyphotos.utils.system.SystemUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 预览页
 */
public class PreviewActivity extends AppCompatActivity implements PreviewPhotosAdapter.OnClickListener, View.OnClickListener, PreviewFragment.OnPreviewFragmentClickListener {

	public static void start (Activity act, int albumItemIndex, int currIndex, int typeShowing) {
		Intent intent = new Intent(act, PreviewActivity.class);
		intent.putExtra(Key.PREVIEW_ALBUM_ITEM_INDEX, albumItemIndex);
		intent.putExtra(Key.PREVIEW_PHOTO_INDEX, currIndex);
		intent.putExtra(Key.PREVIEW_TYPE_SHOWING, typeShowing);
		act.startActivityForResult(intent, Code.REQUEST_PREVIEW_ACTIVITY);
	}


	/**
	 * 一些旧设备在UI小部件更新之间需要一个小延迟
	 * and a change of the status and navigation bar.
	 */
	private static final int      UI_ANIMATION_DELAY = 300;
	private final        Handler  mHideHandler       = new Handler();
	private final        Runnable mHidePart2Runnable = new Runnable() {
		@Override
		public void run () {
			SystemUtils.getInstance().systemUiHide(PreviewActivity.this, decorView);
		}
	};
	//    private RelativeLayout mBottomBar;
	//    private FrameLayout mToolBar;
	private final        Runnable mShowPart2Runnable = new Runnable() {
		@Override
		public void run () {
			// 延迟显示UI元素
			//            mBottomBar.setVisibility(View.VISIBLE);
			//            mToolBar.setVisibility(View.VISIBLE);
		}
	};
	private              boolean  mVisible;
	View decorView;
	//    private TextView tvNumber;//tvOriginal
	//    private PressedTextView tvDone;
	//    private ImageView ivSelector;
	private RecyclerView         rvPhotos;
	private PreviewPhotosAdapter adapter;
	private PagerSnapHelper      snapHelper;
	private LinearLayoutManager  lm;
	private int                  index;
	private ArrayList<Photo>     photos       = new ArrayList<>();
	private int                  resultCode   = RESULT_CANCELED;
	private int                  lastPosition = 0;//记录recyclerView最后一次角标位置，用于判断是否转换了item
	private boolean              isSingle     = Setting.count == 1;
	private boolean              unable       = Result.count() == Setting.count;

	//    private FrameLayout flFragment;
	//    private PreviewFragment previewFragment;
	private int statusColor;

	private ImageView ivBack;

	private        PhotoHandler photoHandler;
	private static String       currentPicPath = null;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		decorView = getWindow().getDecorView();
		//        requestWindowFeature(Window.FEATURE_NO_TITLE);
		SystemUtils.getInstance().systemUiInit(this, decorView);

		mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_preview_easy_photos);
		//		setContentView(R.layout.activity_preview_easy_photos);
		photoHandler = new PhotoHandler(new PhotoNativeHelper());
		//		currentPicPath = "/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/aa
		//		.jpg";


		//		hideActionBar();
		//		adaptationStatusBar();
		if (null == AlbumModel.instance) {
			finish();
			return;
		}
		initData();
		initView();
	}

	//	private void adaptationStatusBar () {
	//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
	//			statusColor = ContextCompat.getColor(this, R.color.easy_photos_status_bar);
	//			if (ColorUtils.isWhiteColor(statusColor)) {
	//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
	//			}
	//		}
	//	}

	//	private void hideActionBar () {
	//		ActionBar actionBar = getSupportActionBar();
	//		if (actionBar != null) {
	//			actionBar.hide();
	//		}
	//	}


	private void initData () {
		Intent intent = getIntent();
		int albumItemIndex = intent.getIntExtra(Key.PREVIEW_ALBUM_ITEM_INDEX, 0);
		photos.clear();
		//是否选择照片 0 所有， 1 照片 2 视频
		int type_showing = intent.getIntExtra(Key.PREVIEW_TYPE_SHOWING, 0);

		if (albumItemIndex == -1) {
			photos.addAll(Result.photos);
		} else {
			List<Photo> list = new ArrayList<>();
			for (Photo photo : AlbumModel.instance.getCurrAlbumItemPhotos(albumItemIndex)) {
				switch (type_showing) {
					case 0:
						list.add(photo);
						break;
					case 1:
						if (photo.path.contains("jpg")) {
							list.add(photo);
						}
						break;
					case 2:
						if (photo.path.contains("mp4")) {
							list.add(photo);
						}
						break;
				}
			}
			photos.addAll(list);
			Setting.showVideo = true;
		}
		index = intent.getIntExtra(Key.PREVIEW_PHOTO_INDEX, 0);
		Log.e("TAG", "initData: ------------index " + index);
		currentGestureIndex = index;

		lastPosition = index;
		mVisible = true;
	}

	private void toggle () {
		if (mVisible) {
			hide();
		} else {
			show();
		}
	}

	private void hide () {
		// Hide UI first
		AlphaAnimation hideAnimation = new AlphaAnimation(1.0f, 0.0f);
		hideAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart (Animation animation) {

			}

			@Override
			public void onAnimationEnd (Animation animation) {
				//                mBottomBar.setVisibility(View.GONE);
				//                mToolBar.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat (Animation animation) {

			}
		});
		hideAnimation.setDuration(UI_ANIMATION_DELAY);
		//        mBottomBar.startAnimation(hideAnimation);
		//        mToolBar.startAnimation(hideAnimation);
		mVisible = false;

		// Schedule a runnable to remove the status and navigation bar after a delay
		mHideHandler.removeCallbacks(mShowPart2Runnable);

		mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);

	}


	private void show () {
		// Show the system bar
		if (Build.VERSION.SDK_INT >= 16) {
			SystemUtils.getInstance().systemUiShow(this, decorView);
		}

		mVisible = true;

		// Schedule a runnable to display UI elements after a delay
		mHideHandler.removeCallbacks(mHidePart2Runnable);
		mHideHandler.post(mShowPart2Runnable);
	}

	/**
	 * 刷新 右侧 和 顶部 的工具栏。
	 */
	private void refreshLayout () {
		if (photoFormatIsRight) {//是否为图片，并 是可编辑的图片
			mDataBinding.clPhotoDetailRightTools.setVisibility(View.VISIBLE);

			mDataBinding.llNormal.setVisibility(View.VISIBLE);
			mDataBinding.clPhotoEdit.setVisibility(View.GONE);

		} else {
			mDataBinding.clPhotoDetailRightTools.setVisibility(View.INVISIBLE);
			mDataBinding.llNormal.setVisibility(View.VISIBLE);
			mDataBinding.clPhotoEdit.setVisibility(View.GONE);
		}
	}

	@Override
	public void onPhotoClick () {
		toggle();
	}

	@Override
	public void onShowItemChanged (int lastPosition, boolean isPic) {
		//		Log.e("TAG", "onShowItemChanged: lastPosition==="+ lastPosition + "===photoSize="+
		//		photos.size());
		if (isPic) {
			mDataBinding.clPhotoDetailRightTools.setVisibility(View.VISIBLE);

			if ((lastPosition == 0 || (lastPosition == photos.size() - 1))) {
				currentPicPath = photos.get(lastPosition).path;
				Log.e("TAG", "onShowItemChanged: ====111==currentPicPath=" + currentPicPath);
			}
			if (lastPosition > 0 && lastPosition < photos.size() - 2) {
				currentPicPath = photos.get(lastPosition - 1).path;
				Log.e("TAG", "onShowItemChanged: ====222==currentPicPath=" + currentPicPath);
			}
		} else {
			mDataBinding.clPhotoDetailRightTools.setVisibility(View.INVISIBLE);
		}
	}


	@Override
	public void onPhotoScaleChanged () {
		if (mVisible)
			hide();
	}

	@Override
	public void onBackPressed () {
		doBack();
	}

	private void doBack () {
		Intent intent = new Intent();
		intent.putExtra(Key.PREVIEW_CLICK_DONE, false);
		setResult(resultCode, intent);
		finish();
	}

	private void initView () {
		ivBack = findViewById(R.id.iv_preview_back);
		ivBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				// Toast.makeText(PreviewActivity.this, "ivBack click", Toast.LENGTH_SHORT).show();
				finish();
			}
		});

		initRecyclerView();

		initTools();

		//获取系统保存的颜色，否则初始化 涂鸦颜色 字号，  文字颜色 及其字号
		mColorDoodle = new DoodleColor(Color.RED);

		mColorText = new DoodleColor(Color.BLUE);


	}

	//control field
	//是否是我们的图片，item 中 每个都有一份。 控制 按钮 状态
	private boolean                          photoFormatIsRight     = true;
	private ActivityPreviewEasyPhotosBinding mDataBinding;
	private int                              type_palette_character = 0;
	//涂鸦 颜色值 ，百分比， 字体大小
	private DoodleColor                      mColorDoodle;
	private int                              doodleColorData        = 0;
	private float                            doodlePercent          = 0.5f;
	private int                              doodleSizeIndex        = 0;
	//文字 颜色值 ，百分比， 字体大小
	private DoodleColor                      mColorText;
	private int                              characterColorData     = 0;
	private float                            characterPercent       = 0.5f;
	private int                              characterSizeIndex     = 0;


	private boolean    isDoodle = false;
	private DoodleView mDoodleView;

	//my tools listener
	private void initTools () {

		mDataBinding.rvPhotos.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch (View v, MotionEvent event) {
				if (isDoodle){
					mDataBinding.rvPhotos.getChildAt(currentGestureIndex).findViewById(R.id.doodle_item_view).dispatchTouchEvent(event);
				}

				return isDoodle;
			}
		});


		mDataBinding.circlePalette.setSelected(true);
		mDataBinding.circleCharacterSize.setSelected(false);
		mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.INVISIBLE);
		if (photoFormatIsRight) {
			//		涂鸦  。
			mDataBinding.cbDetailToolsDoodle.setOnClickListener(v -> {
				//加载储存的 色板, 字号 inde 及其 字号大小
				//todo ...
				isDoodle = true;
				mDoodleView =  mDataBinding.rvPhotos.getChildAt(currentGestureIndex).findViewById(R.id.doodle_item_view);
				mDoodleView.setColor(mColorDoodle);
				mDoodleView.setShape(DoodleShape.HAND_WRITE);
				mDoodleView.setPen(DoodlePen.BRUSH);
				mDoodleView.setSize(10);

				Toast.makeText(this, "当前ID：" + photos.get(index).name, Toast.LENGTH_LONG).show();

				Log.e("TAG", "==========initTools: ========涂鸦=============");
				mDataBinding.cbDetailToolsDoodle.setSelected(true);
				mDataBinding.cbDetailToolsCharacter.setSelected(false);
				type_palette_character = 0;
				mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.VISIBLE);

				//				mDataBinding.circlePalette.performClick();
				mDataBinding.editColorPick.setSelectPercent(doodlePercent);
				mDataBinding.pssDetailTools.setSelectIndex(doodleSizeIndex);

				paintCircleCharacterSize(doodleSizeIndex);
				mDataBinding.circlePalette.setColor(doodleColorData,
						CircleDisplayView.CirCleImageType.COLOR);

				//
				photos.get(currentGestureIndex).gestureDetectorAble = true;
				adapter.notifyDataSetChanged();
				mDataBinding.rvPhotos.invalidate();

				//隐藏 正常 actionbar，显示手势action bar
				mDataBinding.llNormal.setVisibility(View.GONE);
				mDataBinding.clPhotoEdit.setVisibility(View.VISIBLE);

			});
			//文字 选择
			mDataBinding.cbDetailToolsCharacter.setOnClickListener(v -> {
				//加载储存的 色板, 字号 inde 及其 字号大小
				//todo ...
				isDoodle = true;
				mDoodleView =  mDataBinding.rvPhotos.getChildAt(currentGestureIndex).findViewById(R.id.doodle_item_view);
				mDoodleView.setColor(mColorText);
				mDoodleView.setPen(DoodlePen.TEXT);
				mDoodleView.setSize(10);

				Log.e("TAG", "==========initTools: ============文字=========");
				mDataBinding.cbDetailToolsDoodle.setSelected(false);
				mDataBinding.cbDetailToolsCharacter.setSelected(true);
				type_palette_character = 1;
				mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.VISIBLE);

				mDataBinding.editColorPick.setSelectPercent(characterPercent);
				mDataBinding.pssDetailTools.setSelectIndex(characterSizeIndex);

				paintCircleCharacterSize(characterSizeIndex);
				mDataBinding.circlePalette.setColor(characterColorData,
						CircleDisplayView.CirCleImageType.COLOR);


				//
				photos.get(currentGestureIndex).gestureDetectorAble = true;
				adapter.notifyDataSetChanged();
				mDataBinding.rvPhotos.invalidate();

				//隐藏 正常 actionbar，显示手势action bar
				mDataBinding.llNormal.setVisibility(View.GONE);
				mDataBinding.clPhotoEdit.setVisibility(View.VISIBLE);

			});
			//			色板  粗细 选择。
			mDataBinding.circlePalette.setClickListener((currentData, cirCleImageType) -> {
				Log.e("TAG", "initTools:circlePalette -------------");
				if (type_palette_character == 0 && cirCleImageType == CircleDisplayView.CirCleImageType.COLOR) {
					mColorDoodle.setColor(currentData);
					//刷新 画笔
					mDoodleView.setColor(mColorDoodle);
				} else {
					mColorText.setColor(currentData);
					//刷新 画笔
					mDoodleView.setColor(mColorText);
				}


				mDataBinding.circleCharacterSize.setSelected(false);

				mDataBinding.editColorPick.setVisibility(View.VISIBLE);
				mDataBinding.pssDetailTools.setVisibility(View.GONE);
			});
			mDataBinding.circleCharacterSize.setClickListener((currentData1, cirCleImageType1) -> {
				Log.e("TAG", "initTools:circlePalette -------------");
				mDataBinding.circlePalette.setSelected(false);

				mDataBinding.editColorPick.setVisibility(View.GONE);
				mDataBinding.pssDetailTools.setVisibility(View.VISIBLE);
			});
			//			色板  粗细切换  监听。
			mDataBinding.pssDetailTools.setSelectorListener((position, selectPaintSize) -> {
				if (type_palette_character == 0) {
					doodleSizeIndex = position;
				} else {
					characterSizeIndex = position;
				}
				paintCircleCharacterSize(position);

				Log.e("TAG",
						"initTools: --------position---" + position + " selectPaintSize ----" + selectPaintSize);
				//设置 画笔 或 文字的 值
			});
			//设置监听器
			mDataBinding.editColorPick.setOnColorPickerChangeListener(new ColorSliderView.OnColorPickerChangeListener() {
				@Override
				public void onColorChanged (ColorSliderView picker, int color, float percent) {
					mDataBinding.circlePalette.setColor(color,
							CircleDisplayView.CirCleImageType.COLOR);
					//设置 画笔 或 文字的 值

					if (type_palette_character == 0) {
						doodlePercent = percent;
						doodleColorData = color;
					} else {
						characterPercent = percent;
						characterColorData = color;
					}
				}

				@Override
				public void onStartTrackingTouch (ColorSliderView picker) {

				}

				@Override
				public void onStopTrackingTouch (ColorSliderView picker) {

				}
			});
			//设置 手势识别 顶部 actionbar 监听器
			mDataBinding.ivToolsExit.setOnClickListener(v -> {
				isDoodle = false;
				photos.get(currentGestureIndex).gestureDetectorAble = false;
				adapter.notifyDataSetChanged();
				mDataBinding.rvPhotos.invalidate();

				//隐藏 正常 actionbar，显示手势action bar
				mDataBinding.llNormal.setVisibility(View.VISIBLE);
				mDataBinding.clPhotoEdit.setVisibility(View.GONE);
			});

			mDataBinding.ivSavePhoto.setOnClickListener(v -> {
				isDoodle = false;
				Toast.makeText(this, "save", Toast.LENGTH_SHORT).show();
				photos.get(currentGestureIndex).gestureDetectorAble = false;
				adapter.notifyDataSetChanged();
				mDataBinding.rvPhotos.invalidate();

				//隐藏 正常 actionbar，显示手势action bar
				mDataBinding.llNormal.setVisibility(View.VISIBLE);
				mDataBinding.clPhotoEdit.setVisibility(View.GONE);
			});


		} else {

		}
	}

	private int currentGestureIndex = 0;

	private void paintCircleCharacterSize (int clickPosition) {
		switch (clickPosition) {
			case 0:
				mDataBinding.circleCharacterSize.setColor(R.mipmap.photo_detail_tools_size_1_select, CircleDisplayView.CirCleImageType.BITMAP);
				break;
			case 1:
				mDataBinding.circleCharacterSize.setColor(R.mipmap.photo_detail_tools_size_2_select, CircleDisplayView.CirCleImageType.BITMAP);
				break;
			case 2:
				mDataBinding.circleCharacterSize.setColor(R.mipmap.photo_detail_tools_size_3_select, CircleDisplayView.CirCleImageType.BITMAP);
				break;
			case 3:
				mDataBinding.circleCharacterSize.setColor(R.mipmap.photo_detail_tools_size_4_select, CircleDisplayView.CirCleImageType.BITMAP);
				break;
			case 4:
				mDataBinding.circleCharacterSize.setColor(R.mipmap.photo_detail_tools_size_5_select, CircleDisplayView.CirCleImageType.BITMAP);
				break;
			case 5:
				mDataBinding.circleCharacterSize.setColor(R.mipmap.photo_detail_tools_size_6_select, CircleDisplayView.CirCleImageType.BITMAP);
				break;
		}
		//设置 doodle view
	}

	private void initRecyclerView () {
		rvPhotos = (RecyclerView) findViewById(R.id.rv_photos);
		adapter = new PreviewPhotosAdapter(this, photos, this);
		lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
		rvPhotos.setLayoutManager(lm);
		rvPhotos.setAdapter(adapter);
		//		rvPhotos.getChildAdapterPosition()
		rvPhotos.scrollToPosition(index);
		toggleSelector();
		snapHelper = new PagerSnapHelper();
		snapHelper.attachToRecyclerView(rvPhotos);
		rvPhotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged (@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);

				View view = snapHelper.findSnapView(lm);
				if (view == null) {
					return;
				}
				int position = lm.getPosition(view);
				Log.e("TAG", "onScrollStateChanged:  -------position==> " + position);
				currentGestureIndex = position;
				if (lastPosition == position) {
					return;
				}
				lastPosition = position;
				//                previewFragment.setSelectedPosition(-1);
				//                tvNumber.setText(getString(R.string
				//                .preview_current_number_easy_photos,
				//                        lastPosition + 1, photos.size()));
				toggleSelector();
			}
		});
		//        tvNumber.setText(getString(R.string.preview_current_number_easy_photos, index
		//        + 1,
		//                photos.size()));
	}

	private boolean clickDone = false;

	@Override
	public void onClick (View v) {
		//		int id = v.getId();
		//        if (R.id.iv_back == id) {
		//            doBack();
		//        }
		//        else if (R.id.tv_selector == id) {
		//            updateSelector();
		//        }
		//        else if (R.id.iv_selector == id) {
		//            updateSelector();
		//        }
		//        else
		//        if (R.id.tv_original == id) {
		//            if (!Setting.originalMenuUsable) {
		//                Toast.makeText(getApplicationContext(), Setting
		//                .originalMenuUnusableHint, Toast.LENGTH_SHORT).show();
		//                return;
		//            }
		//            Setting.selectedOriginal = !Setting.selectedOriginal;
		//            processOriginalMenu();
		//        }
		//        else if (R.id.tv_done == id) {
		//            if (clickDone) return;
		//            clickDone = true;
		//            Intent intent = new Intent();
		//            intent.putExtra(Key.PREVIEW_CLICK_DONE, true);
		//            setResult(RESULT_OK, intent);
		//            finish();
		//        }
		//        else if (R.id.m_bottom_bar == id) {
		//
		//        } else if (R.id.tv_edit == id) {
		//
		//        }
	}


	private void toggleSelector () {
		if (photos.get(lastPosition).selected) {
			//            ivSelector.setImageResource(R.drawable.ic_selector_true_easy_photos);
			if (!Result.isEmpty()) {
				int count = Result.count();
				for (int i = 0; i < count; i++) {
					if (photos.get(lastPosition).path.equals(Result.getPhotoPath(i))) {
						//                        previewFragment.setSelectedPosition(i);
						break;
					}
				}
			}
		} else {
			//            ivSelector.setImageResource(R.drawable.ic_selector_easy_photos);
		}
		//        previewFragment.notifyDataSetChanged();
		//        shouldShowMenuDone();
	}

	@SuppressLint("StringFormatInvalid")
	private void updateSelector () {
		resultCode = RESULT_OK;
		Photo item = photos.get(lastPosition);
		if (isSingle) {
			singleSelector(item);
			return;
		}
		if (unable) {
			if (item.selected) {
				Result.removePhoto(item);
				if (unable) {
					unable = false;
				}
				toggleSelector();
				return;
			}
			if (Setting.isOnlyVideo()) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.selector_reach_max_video_hint_easy_photos,
								Setting.count), Toast.LENGTH_SHORT).show();

			} else if (Setting.showVideo) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.selector_reach_max_hint_easy_photos, Setting.count),
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.selector_reach_max_image_hint_easy_photos,
								Setting.count), Toast.LENGTH_SHORT).show();
			}
			return;
		}
		item.selected = !item.selected;
		if (item.selected) {
			int res = Result.addPhoto(item);
			if (res != 0) {
				item.selected = false;
				switch (res) {
					case Result.PICTURE_OUT:
						Toast.makeText(getApplicationContext(),
								getString(R.string.selector_reach_max_image_hint_easy_photos,
										Setting.complexPictureCount), Toast.LENGTH_SHORT).show();
						break;
					case Result.VIDEO_OUT:
						Toast.makeText(getApplicationContext(),
								getString(R.string.selector_reach_max_video_hint_easy_photos,
										Setting.complexVideoCount), Toast.LENGTH_SHORT).show();
						break;
					case Result.SINGLE_TYPE:
						Toast.makeText(getApplicationContext(),
								getString(R.string.selector_single_type_hint_easy_photos),
								Toast.LENGTH_SHORT).show();
						break;
				}
				return;
			}
			if (Result.count() == Setting.count) {
				unable = true;
			}
		} else {
			Result.removePhoto(item);
			//            previewFragment.setSelectedPosition(-1);
			if (unable) {
				unable = false;
			}
		}
		toggleSelector();
	}

	private void singleSelector (Photo photo) {
		if (!Result.isEmpty()) {
			if (Result.getPhotoPath(0).equals(photo.path)) {
				Result.removePhoto(photo);
			} else {
				Result.removePhoto(0);
				Result.addPhoto(photo);
			}
		} else {
			Result.addPhoto(photo);
		}
		toggleSelector();
	}

	//    private void shouldShowMenuDone() {
	//        if (Result.isEmpty()) {
	//            if (View.VISIBLE == tvDone.getVisibility()) {
	//                ScaleAnimation scaleHide = new ScaleAnimation(1f, 0f, 1f, 0f);
	//                scaleHide.setDuration(200);
	//                tvDone.startAnimation(scaleHide);
	//            }
	//            tvDone.setVisibility(View.GONE);
	////            flFragment.setVisibility(View.GONE);
	//        }
	//        else {
	//            if (View.GONE == tvDone.getVisibility()) {
	//                ScaleAnimation scaleShow = new ScaleAnimation(0f, 1f, 0f, 1f);
	//                scaleShow.setDuration(200);
	//                tvDone.startAnimation(scaleShow);
	//            }
	////            flFragment.setVisibility(View.VISIBLE);
	//            tvDone.setVisibility(View.VISIBLE);
	//
	//            if (Result.isEmpty()) {
	//                return;
	//            }
	//
	//            if (Setting.complexSelector) {
	//                if (Setting.complexSingleType) {
	//                    if (Result.getPhotoType(0).contains(Type.VIDEO)) {
	//                        tvDone.setText(getString(R.string.selector_action_done_easy_photos,
	//                        Result.count(),
	//                                Setting.complexVideoCount));
	//                        return;
	//                    }
	//                    tvDone.setText(getString(R.string.selector_action_done_easy_photos,
	//                    Result.count(),
	//                            Setting.complexPictureCount));
	//                    return;
	//                }
	//            }
	//            tvDone.setText(getString(R.string.selector_action_done_easy_photos, Result
	//            .count(),
	//                    Setting.count));
	//        }
	//    }

	@Override
	public void onPreviewPhotoClick (int position) {
		String path = Result.getPhotoPath(position);
		int size = photos.size();
		for (int i = 0; i < size; i++) {
			if (TextUtils.equals(path, photos.get(i).path)) {
				rvPhotos.scrollToPosition(i);
				lastPosition = i;
				//                tvNumber.setText(getString(R.string
				//                .preview_current_number_easy_photos,
				//                        lastPosition + 1, photos.size()));
				//                previewFragment.setSelectedPosition(position);
				toggleSelector();
				return;
			}
		}
	}

	private void setClick (@IdRes int... ids) {
		for (int id : ids) {
			findViewById(id).setOnClickListener(this);
		}
	}

	private void setClick (View... views) {
		for (View v : views) {
			v.setOnClickListener(this);
		}
	}
}
