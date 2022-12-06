package com.huantansheng.easyphotos.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
import com.dyt.wcc.baselib.ui.widget.ColorSliderView;
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
		mDataBinding.ivPreviewBack.setOnClickListener(v -> finish());

		if (photoClick.path.endsWith("jpg")) {//图片
			//不显示视频控件，显示涂鸦 图片控件
			mDataBinding.ivVideo.setVisibility(View.GONE);
			mDataBinding.doodleView.setVisibility(View.VISIBLE);
			mDataBinding.ivPlay.setVisibility(View.GONE);

			initDoodle();
		} else if (photoClick.path.endsWith("mp4")) {
			//显示视频控件，不显示涂鸦 图片控件
			mDataBinding.ivVideo.setVisibility(View.VISIBLE);
			mDataBinding.doodleView.setVisibility(View.GONE);
			mDataBinding.ivPlay.setVisibility(View.VISIBLE);

			initVideo();

		} else {
			mDataBinding.ivVideo.setVisibility(View.GONE);
			mDataBinding.doodleView.setVisibility(View.GONE);
			mDataBinding.ivPlay.setVisibility(View.GONE);
			//显示错误布局

		}

		//获取系统保存的颜色，否则初始化 涂鸦颜色 字号，  文字颜色 及其字号
		mColorDoodle = new DoodleColor(Color.RED);

		mColorText = new DoodleColor(Color.BLUE);

		initTools();
	}

	private void initVideo () {
		Setting.imageEngine.loadPhoto(this, photoClick.uri, mDataBinding.ivVideo);

		mDataBinding.clPhotoDetailRightTools.setVisibility(View.GONE);

		mDataBinding.ivPlay.setOnClickListener(v -> {
			toPlayVideo(photoClick.uri,photoClick.type);
		});
	}

	private void toPlayVideo (Uri uri, String type) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		}
		intent.setDataAndType(uri, type);
		this.startActivity(intent);
	}

	private DoodleOnTouchGestureListener touchGestureListener;

	private IDoodleTouchDetector doodleTouchDetector;

	private void initDoodle () {
		mColorDoodle = new DoodleColor(Color.RED);
		mColorText = new DoodleColor(Color.BLUE);



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
	//当前 选中 的是涂鸦，还是文字，还是 初始值
	private int                              type_palette_character = -1;
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


	private boolean isDoodle = false;

	/**
	 * 重置所有按钮，并恢复 actionbar 的状态
	 */
	private void UnSelectAllTools () {
		//重置 颜色 ，线条粗细 文字大小。
		mDataBinding.circlePalette.setSelected(false);
		mDataBinding.circleCharacterSize.setSelected(false);

		//重置 涂鸦，文字
		mDataBinding.ivDetailToolsDoodle.setSelected(false);
		mDataBinding.ivDetailToolsCharacter.setSelected(false);
	}

	//my tools listener
	private void initTools () {
		mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.INVISIBLE);

		if (photoFormatIsRight) {
			//		涂鸦
			mDataBinding.ivDetailToolsDoodle.setOnClickListener(v -> {
				type_palette_character = 0;
				mDataBinding.ivDetailToolsDoodle.setSelected(true);
				mDataBinding.ivDetailToolsCharacter.setSelected(false);
				//加载储存的 色板, 字号 index 及其 字号大小
				isDoodle = true;
				mDataBinding.doodleView.setGestureRecognitionAble(isDoodle);
				mDataBinding.doodleView.setColor(mColorDoodle);
				mDataBinding.doodleView.setShape(DoodleShape.HAND_WRITE);
				mDataBinding.doodleView.setPen(DoodlePen.BRUSH);
				mDataBinding.doodleView.setSize(doodleSizeIndex);
				//使 颜色圆形为 选中状态， 滑动条为 涂鸦的颜色值。隐藏 文字大小选择圆形控件和 六类线条粗细选择器
				mDataBinding.circlePalette.setSelected(true);
				mDataBinding.circleCharacterSize.setSelected(false);
				mDataBinding.editColorPick.setSelectPercent(doodlePercent);
				mDataBinding.editColorPick.setVisibility(View.VISIBLE);
				mDataBinding.paintSizeSelect.setVisibility(View.GONE);
				mDataBinding.circlePalette.setColor(doodleColorData,
						CircleDisplayView.CirCleImageType.COLOR);
				paintCircleCharacterSize(doodleSizeIndex);
				//顶部状态栏 变化
				mDataBinding.llActionbarNormal.setVisibility(View.GONE);
				mDataBinding.clActionbarGesture.setVisibility(View.VISIBLE);


				//具体工具栏 显示
				mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.VISIBLE);


			});
			//文字 选择
			mDataBinding.ivDetailToolsCharacter.setOnClickListener(v -> {
				type_palette_character = 1;
				mDataBinding.ivDetailToolsDoodle.setSelected(false);
				mDataBinding.ivDetailToolsCharacter.setSelected(true);
				//加载储存的 色板, 字号 index 及其 字号大小
				isDoodle = true;
				mDataBinding.doodleView.setGestureRecognitionAble(isDoodle);
				mDataBinding.doodleView.setColor(mColorText);
				mDataBinding.doodleView.setPen(DoodlePen.TEXT);
				mDataBinding.doodleView.setSize(characterSizeIndex);
				//使 颜色圆形为 选中状态， 滑动条为 涂鸦的颜色值。隐藏 文字大小选择圆形控件和 六类线条粗细选择器
				mDataBinding.circlePalette.setSelected(true);
				mDataBinding.circleCharacterSize.setSelected(false);
				mDataBinding.editColorPick.setSelectPercent(characterPercent);
				mDataBinding.editColorPick.setVisibility(View.VISIBLE);
				mDataBinding.paintSizeSelect.setVisibility(View.GONE);
				mDataBinding.circlePalette.setColor(characterColorData,
						CircleDisplayView.CirCleImageType.COLOR);
				paintCircleCharacterSize(characterSizeIndex);


				//顶部状态栏 变化
				mDataBinding.llActionbarNormal.setVisibility(View.GONE);
				mDataBinding.clActionbarGesture.setVisibility(View.VISIBLE);
				//具体工具栏 显示
				mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.VISIBLE);

			});

			//			色板  粗细 选择。
			mDataBinding.circlePalette.setClickListener((currentData, cirCleImageType) -> {
				mDataBinding.circlePalette.setSelected(true);
				mDataBinding.circleCharacterSize.setSelected(false);
				mDataBinding.editColorPick.setVisibility(View.VISIBLE);
				mDataBinding.paintSizeSelect.setVisibility(View.GONE);

				//此处的 是给 下面具体控件的
				if (type_palette_character == 0 && cirCleImageType == CircleDisplayView.CirCleImageType.COLOR) {
					mColorDoodle.setColor(currentData);
					mDataBinding.circlePalette.setColor(doodleColorData,
							CircleDisplayView.CirCleImageType.COLOR);
				} else if (type_palette_character == 1) {
					mColorText.setColor(currentData);
					mDataBinding.circlePalette.setColor(characterColorData,
							CircleDisplayView.CirCleImageType.COLOR);
				}
			});
			//文字大小
			mDataBinding.circleCharacterSize.setClickListener((currentData1, cirCleImageType1) -> {

				Log.e("TAG", "initTools: -------------character size " + currentData1);
				mDataBinding.circlePalette.setSelected(false);
				mDataBinding.circleCharacterSize.setSelected(true);
				mDataBinding.editColorPick.setVisibility(View.GONE);
				mDataBinding.paintSizeSelect.setVisibility(View.VISIBLE);

				if (type_palette_character == 0) {
					paintCircleCharacterSize(doodleSizeIndex);
				} else if (type_palette_character == 1) {
					paintCircleCharacterSize(characterSizeIndex);
				}
			});

			//		粗细切换  监听。
			mDataBinding.paintSizeSelect.setSelectorListener((position, selectPaintSize) -> {
				if (type_palette_character == 0) {
					doodleSizeIndex = position;
				} else {
					characterSizeIndex = position;
				}
				paintCircleCharacterSize(position);
				mDataBinding.doodleView.setSize(3 + (4 * position));
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
						mColorDoodle.setColor(doodleColorData);
						mDataBinding.doodleView.setColor(mColorDoodle);
					} else if (type_palette_character == 1) {
						characterPercent = percent;
						characterColorData = color;
						mColorText.setColor(characterColorData);
						mDataBinding.doodleView.setColor(mColorText);
					}
				}

				@Override
				public void onStartTrackingTouch (ColorSliderView picker) {

				}

				@Override
				public void onStopTrackingTouch (ColorSliderView picker) {

				}
			});
			//------ 重做 和前进--------------------------
			mDataBinding.ivDo.setOnClickListener(v -> {
				mDataBinding.doodleView.redo(1);
			});
			mDataBinding.ivUndo.setOnClickListener(v -> {
				mDataBinding.doodleView.undo(1);
			});

			//设置 手势识别 顶部 actionbar 监听器
			mDataBinding.ivToolsExit.setOnClickListener(v -> {
				mDataBinding.doodleView.clear();

				isDoodle = false;
				mDataBinding.doodleView.setGestureRecognitionAble(isDoodle);
				//顶部状态栏变化
				mDataBinding.llActionbarNormal.setVisibility(View.VISIBLE);
				mDataBinding.clActionbarGesture.setVisibility(View.GONE);
				mDataBinding.circleCharacterSize.setSelected(false);
				mDataBinding.circlePalette.setSelected(false);
				mDataBinding.ivDetailToolsDoodle.setSelected(false);
				mDataBinding.ivDetailToolsCharacter.setSelected(false);

				mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.INVISIBLE);


			});

			mDataBinding.ivSavePhoto.setOnClickListener(v -> {
				isDoodle = false;
				mDataBinding.doodleView.setGestureRecognitionAble(isDoodle);
				//顶部状态栏变化
				mDataBinding.llActionbarNormal.setVisibility(View.VISIBLE);
				mDataBinding.clActionbarGesture.setVisibility(View.GONE);
				mDataBinding.circleCharacterSize.setSelected(false);
				mDataBinding.circlePalette.setSelected(false);
				mDataBinding.ivDetailToolsDoodle.setSelected(false);
				mDataBinding.ivDetailToolsCharacter.setSelected(false);

				mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.INVISIBLE);
			});


		}
	}


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
