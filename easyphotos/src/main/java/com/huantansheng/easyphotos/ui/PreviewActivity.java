package com.huantansheng.easyphotos.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleSelectableItem;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleTouchDetector;
import com.dyt.wcc.baselib.ui.doodle.impl.DoodleOnTouchGestureListener;
import com.dyt.wcc.baselib.ui.doodle.impl.DoodleTouchDetector;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleColor;
import com.dyt.wcc.baselib.ui.doodle.params.DoodlePen;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleShape;
import com.dyt.wcc.baselib.ui.widget.CircleDisplayView;
import com.huantansheng.easyphotos.R;
import com.huantansheng.easyphotos.constant.Code;
import com.huantansheng.easyphotos.constant.Key;
import com.huantansheng.easyphotos.databinding.ActivityPreviewEasyPhotosBinding;
import com.huantansheng.easyphotos.models.album.AlbumModel;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.huantansheng.easyphotos.result.Result;
import com.huantansheng.easyphotos.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * 预览页
 */
public class PreviewActivity extends AppCompatActivity /*implements PreviewPhotosAdapter
.OnClickListener, View.OnClickListener, PreviewFragment.OnPreviewFragmentClickListener*/ {

	/**
	 * @param act            起始Activity
	 * @param albumItemIndex 总共有多少个相册的索引 ，index
	 * @param currIndex      当前相册的索引index
	 * @param typeShowing    0 获取所有， 1 仅获取照片 2 仅视频
	 */
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
	private static final int              UI_ANIMATION_DELAY = 300;
	private              boolean          mVisible;
	private              int              index;
	private              ArrayList<Photo> photos             = new ArrayList<>();
	private              int              resultCode         = RESULT_CANCELED;
	// 最后一次角标位置，用于判断是否转换了item
	private              boolean          isSingle           = Setting.count == 1;
	private              boolean          unable             = Result.count() == Setting.count;

	private Photo photoClick;

	private ImageView ivBack;

	private static String currentPicPath = null;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_preview_easy_photos);
		if (null == AlbumModel.instance) {
			finish();
			return;
		}
		initData();
		initView();
	}


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

		//		Log.e("TAG", "initData: ------------index " + index +" photos 的大小为："+ photos.size
		//		());
		if (photos.size() > 10) {
			for (int i = 0; i < 10; i++) {
				Log.e("TAG", "initData: " + i + "------------" + photos.get(i).path);
			}
		}
		photoClick = photos.get(index);

		Log.e("TAG", "initData: -------------=====》" + photoClick.path);

		mVisible = true;
	}


	/**
	 * 刷新 右侧 和 顶部 的工具栏。
	 */
	private void refreshLayout () {
		if (photoFormatIsRight) {//是否为图片，并 是可编辑的图片
			mDataBinding.clPhotoDetailRightTools.setVisibility(View.VISIBLE);

			mDataBinding.llActionbarNormal.setVisibility(View.VISIBLE);
			mDataBinding.clActionbarGesture.setVisibility(View.GONE);

		} else {
			mDataBinding.clPhotoDetailRightTools.setVisibility(View.INVISIBLE);
			mDataBinding.llActionbarNormal.setVisibility(View.VISIBLE);
			mDataBinding.clActionbarGesture.setVisibility(View.GONE);
		}
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
		ivBack.setOnClickListener(v -> finish());
		if (photoClick.path.endsWith("jpg")){
			mDataBinding.ivVideo.setVisibility(View.GONE);
			mDataBinding.doodleView.setVisibility(View.VISIBLE);

			initDoodle();
		}else if (photoClick.path.endsWith("mp4")){
			mDataBinding.ivVideo.setVisibility(View.VISIBLE);
			mDataBinding.doodleView.setVisibility(View.GONE);

			initVideo();

		}else {
			mDataBinding.ivVideo.setVisibility(View.GONE);
			mDataBinding.doodleView.setVisibility(View.GONE);
			//显示错误布局

		}

		//		initRecyclerView();

		//		initTools();

		//获取系统保存的颜色，否则初始化 涂鸦颜色 字号，  文字颜色 及其字号
		mColorDoodle = new DoodleColor(Color.RED);

		mColorText = new DoodleColor(Color.BLUE);
	}

	private void initVideo(){
		Setting.imageEngine.loadPhoto(this, photoClick.uri,
				mDataBinding.ivVideo);

		mDataBinding.clPhotoDetailRightTools.setVisibility(View.GONE);

	}

	private DoodleOnTouchGestureListener touchGestureListener;

	private IDoodleTouchDetector doodleTouchDetector;

	private void initDoodle(){

		//设置右侧 工具栏可见，左侧 颜色 字体大小  不可见但占位
		mDataBinding.clPhotoDetailRightTools.setVisibility(View.VISIBLE);
		mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.INVISIBLE);

		mDataBinding.doodleView.setBitmap(BitmapFactory.decodeFile(photoClick.path));

		touchGestureListener = new DoodleOnTouchGestureListener(mDataBinding.doodleView);
		touchGestureListener.setSelectionListener(new DoodleOnTouchGestureListener.ISelectionListener() {
			@Override
			public void onSelectedItem (IDoodle doodle, IDoodleSelectableItem selectableItem,
			                            boolean selected) {

			}

			@Override
			public void onCreateSelectableItem (IDoodle doodle, float x, float y) {

			}
		});

		doodleTouchDetector = new DoodleTouchDetector(this, touchGestureListener);


		mDataBinding.doodleView.setGestureRecognitionAble(true);

		mColorDoodle = new DoodleColor(Color.RED);

		mDataBinding.doodleView.setColor(mColorDoodle);
		mDataBinding.doodleView.setShape(DoodleShape.HAND_WRITE);
		mDataBinding.doodleView.setPen(DoodlePen.BRUSH);
		mDataBinding.doodleView.setSize(10);

		mDataBinding.doodleView.setDefaultTouchDetector(doodleTouchDetector);
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

	//my tools listener
/*	private void initTools () {

//		mDataBinding.rvPhotos.setOnTouchListener(new View.OnTouchListener() {
//			@Override
//			public boolean onTouch (View v, MotionEvent event) {
//				if (isDoodle){
//					mDataBinding.rvPhotos.getChildAt(currentGestureIndex).findViewById(R.id
.doodle_item_view).dispatchTouchEvent(event);
//				}
//
//				return isDoodle;
//			}
//		});


		mDataBinding.circlePalette.setSelected(true);
		mDataBinding.circleCharacterSize.setSelected(false);
		mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.INVISIBLE);
		if (photoFormatIsRight) {
			//		涂鸦  。
			mDataBinding.cbDetailToolsDoodle.setOnClickListener(v -> {
				//加载储存的 色板, 字号 inde 及其 字号大小
				//todo ...
				isDoodle = true;
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

				//隐藏 正常 actionbar，显示手势action bar
				mDataBinding.llActionbarNormal.setVisibility(View.GONE);
				mDataBinding.llActionbarNormal.setVisibility(View.VISIBLE);

			});
			//文字 选择
			mDataBinding.cbDetailToolsCharacter.setOnClickListener(v -> {
				//加载储存的 色板, 字号 inde 及其 字号大小
				//todo ...
				isDoodle = true;
				mDoodleView =  mDataBinding.rvPhotos.getChildAt(currentGestureIndex).findViewById
				(R.id.doodle_item_view);
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
				if (type_palette_character == 0 && cirCleImageType == CircleDisplayView
				.CirCleImageType.COLOR) {
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
						"initTools: --------position---" + position + " selectPaintSize ----" +
						selectPaintSize);
				//设置 画笔 或 文字的 值
			});
			//设置监听器
			mDataBinding.editColorPick.setOnColorPickerChangeListener(new ColorSliderView
			.OnColorPickerChangeListener() {
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
		}
	}*/


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
	}

}
