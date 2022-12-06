package com.huantansheng.easyphotos.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodle;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleListener;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleSelectableItem;
import com.dyt.wcc.baselib.ui.doodle.IItem.IDoodleTouchDetector;
import com.dyt.wcc.baselib.ui.doodle.impl.DoodleOnTouchGestureListener;
import com.dyt.wcc.baselib.ui.doodle.impl.DoodleText;
import com.dyt.wcc.baselib.ui.doodle.impl.DoodleTouchDetector;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleColor;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleParams;
import com.dyt.wcc.baselib.ui.doodle.params.DoodlePen;
import com.dyt.wcc.baselib.ui.doodle.params.DoodleShape;
import com.dyt.wcc.baselib.ui.doodle.util.DialogController;
import com.dyt.wcc.baselib.ui.doodle.view.DoodleView;
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.forward.androids.utils.ImageUtils;
import cn.forward.androids.utils.Util;

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
	 * sharePreference 读取与 保存
	 */
	private static String            doodle_sp                    = "doodle_sp";
	private        SharedPreferences sp;
	private static String            DOODLE_PALETTE_COLOR_VALUE   = "doodle_palette_color_value";
	private static String            DOODLE_PALETTE_COLOR_PERCENT = "doodle_palette_color_percent";
	private static String            DOODLE_PAINT_SIZE_INDEX      = "doodle_paint_size_index";
	private static String            DOODLE_PAINT_SIZE            = "doodle_paint_size";
	// 绘制 文字：画笔的颜色 数值，滑动条的百分比，文字的大小
	private static String            DRAW_TEXT_PALETTE_VALUE      = "text_palette_value";
	private static String            DRAW_TEXT_PALETTE_PERCENT    = "text_palette_percent";
	private static String            DRAW_TEXT_PAINT_SIZE_INDEX   = "text_paint_size_index";
	private static String            DRAW_TEXT_PAINT_SIZE         = "text_paint_size";

	//保存 与 恢复
	public static final String       KEY_PARAMS = "key_doodle_params";
	private             DoodleParams mDoodleParams;


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


	private static String currentPicPath = null;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_preview_easy_photos);
		if (null == AlbumModel.instance) {
			finish();
			return;
		}
		sp = getSharedPreferences(doodle_sp, MODE_PRIVATE);
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
		//		if (photos.size() > 10) {
		//			for (int i = 0; i < 10; i++) {
		//				Log.e("TAG", "initData: " + i + "------------" + photos.get(i).path);
		//			}
		//		}
		photoClick = photos.get(index);
		mVisible = true;
		//		Log.e("TAG", "initData: -------------=====》" + photoClick.path);
		//初始化 涂鸦  文字 的设置参数。
		doodleColorData = sp.getInt(DOODLE_PALETTE_COLOR_VALUE, Color.RED);
		doodlePercent = sp.getFloat(DOODLE_PALETTE_COLOR_PERCENT, 1f);
		doodleSizeIndex = sp.getInt(DOODLE_PAINT_SIZE_INDEX, 0);
		doodlePaintSize = sp.getInt(DOODLE_PAINT_SIZE, 10);

		characterColorData = sp.getInt(DRAW_TEXT_PALETTE_VALUE, Color.RED);
		characterPercent = sp.getFloat(DRAW_TEXT_PALETTE_PERCENT, 1f);
		characterSizeIndex = sp.getInt(DRAW_TEXT_PAINT_SIZE_INDEX, 0);
		characterPaintSize = sp.getInt(DRAW_TEXT_PAINT_SIZE, 40);
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
		/**
		 * 初始化 涂鸦 视图的 保存监听器
		 */
		mDataBinding.doodleView.setDoodleListener(new IDoodleListener() {
			@Override
			public void onSaved (IDoodle doodle, Bitmap doodleBitmap, Runnable callback) {
				File doodleFile = null;
				File file = null;
				String savePath = photoClick.path.replace(".jpg",
						"_doodle_" + ((int) (Math.random() * 1000)) + ".jpg");
				if (!TextUtils.isEmpty(savePath)) {
					file = new File(savePath);
					doodleFile = file.getParentFile();
					doodleFile.mkdirs();
				} else {
					return;
				}
				FileOutputStream outputStream = null;
				try {
					outputStream = new FileOutputStream(file);
					doodleBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
					ImageUtils.addImage(getContentResolver(), file.getAbsolutePath());
					Intent intent = new Intent();
					intent.putExtra(KEY_IMAGE_PATH, file.getAbsolutePath());
					setResult(Activity.RESULT_OK, intent);
					finish();
				} catch (Exception e) {
					e.printStackTrace();
					onError(DoodleView.ERROR_SAVE, e.getMessage());
				} finally {
					Util.closeQuietly(outputStream);
					callback.run();
				}
			}

			@Override
			public void onReady (IDoodle doodle) {

			}
		});

		//初始化 返回按钮监听器
		mDataBinding.ivPreviewBack.setOnClickListener(v -> {
		finish();

		});
		//判别 内容是 图片还是 视频
		//获取系统保存的颜色，否则初始化 涂鸦颜色 字号，  文字颜色 及其字号
		mColorDoodle = new DoodleColor(doodleColorData);
		mColorText = new DoodleColor(characterColorData);
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
		initTools();
	}

	public static final int    RESULT_ERROR   = -111; // 出现错误
	public static final String KEY_IMAGE_PATH = "key_image_path";

	private void onError (int i, String msg) {
		setResult(RESULT_ERROR);
		finish();
	}

	/**
	 * 初始化显示视频的布局，并设置点击监听器。跳转视频播放 api
	 */
	private void initVideo () {
		Setting.imageEngine.loadPhoto(this, photoClick.uri, mDataBinding.ivVideo);

		mDataBinding.clPhotoDetailRightTools.setVisibility(View.GONE);

		mDataBinding.ivPlay.setOnClickListener(v -> {
			toPlayVideo(photoClick.uri, photoClick.type);
		});
	}

	/**
	 * 选择手机内播放mp4的软件
	 *
	 * @param uri
	 * @param type
	 */
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

	/**
	 * 初始化 涂鸦 的图片设置，并给设置手势监听器。
	 */
	private void initDoodle () {

		//给涂鸦使徒设置图片。
		mDataBinding.doodleView.setBitmap(BitmapFactory.decodeFile(photoClick.path));

		//设置右侧 工具栏可见，左侧 颜色 字体大小  不可见但占位
		mDataBinding.clPhotoDetailRightTools.setVisibility(View.VISIBLE);
		mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.INVISIBLE);

		touchGestureListener = new DoodleOnTouchGestureListener(mDataBinding.doodleView);
		touchGestureListener.setSelectionListener(new DoodleOnTouchGestureListener.ISelectionListener() {
			@Override
			public void onSelectedItem (IDoodle doodle, IDoodleSelectableItem selectableItem,
			                            boolean selected) {

			}

			@Override
			public void onCreateSelectableItem (IDoodle doodle, float x, float y) {
				Log.e("TAG", "onCreateSelectableItem: -----------x" + x + " y=>" + y);
				if (mDataBinding.doodleView.getPen() == DoodlePen.TEXT) {
					createDoodleText(null, x, y);
				}
			}
		});

		doodleTouchDetector = new DoodleTouchDetector(this, touchGestureListener);
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
	private int                              doodleColorData;
	private float                            doodlePercent;
	private int                              doodleSizeIndex;
	private int                              doodlePaintSize;
	//文字 颜色值 ，百分比， 字体大小
	private DoodleColor                      mColorText;
	private int                              characterColorData;
	private float                            characterPercent;
	private int                              characterSizeIndex;
	private int                              characterPaintSize;


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

	/**
	 *
	 */
	private void initTools () {

		if (photoFormatIsRight) {
			//绘制 涂鸦 按钮。
			mDataBinding.ivDetailToolsDoodle.setOnClickListener(v -> {
				type_palette_character = 0;
				mDataBinding.ivDetailToolsDoodle.setSelected(true);
				mDataBinding.ivDetailToolsCharacter.setSelected(false);

				mDataBinding.doodleView.setGestureRecognitionAble(true);

				mDataBinding.doodleView.setColor(mColorDoodle);
				mDataBinding.doodleView.setShape(DoodleShape.HAND_WRITE);
				mDataBinding.doodleView.setPen(DoodlePen.BRUSH);
				mDataBinding.doodleView.setSize(doodlePaintSize);
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
			//绘制 文字， 按钮
			mDataBinding.ivDetailToolsCharacter.setOnClickListener(v -> {
				type_palette_character = 1;
				mDataBinding.ivDetailToolsDoodle.setSelected(false);
				mDataBinding.ivDetailToolsCharacter.setSelected(true);

				mDataBinding.doodleView.setGestureRecognitionAble(true);

				mDataBinding.doodleView.setColor(mColorText);
				mDataBinding.doodleView.setPen(DoodlePen.TEXT);
				mDataBinding.doodleView.setSize(characterPaintSize);
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

			//	色板选择按钮
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
			//画笔 直径选择按钮
			mDataBinding.circleCharacterSize.setClickListener((currentData1, cirCleImageType1) -> {

				//				Log.e("TAG", "initTools: -------------character size " +
				//				currentData1);
				mDataBinding.circlePalette.setSelected(false);
				mDataBinding.circleCharacterSize.setSelected(true);
				mDataBinding.editColorPick.setVisibility(View.GONE);
				mDataBinding.paintSizeSelect.setVisibility(View.VISIBLE);

				if (type_palette_character == 0) {
					paintCircleCharacterSize(doodleSizeIndex);
					mDataBinding.doodleView.setSize(doodlePaintSize);
				} else if (type_palette_character == 1) {
					paintCircleCharacterSize(characterSizeIndex);
					mDataBinding.doodleView.setSize(characterPaintSize);
				}
			});

			//	画笔 直径 选择器
			mDataBinding.paintSizeSelect.setSelectorListener((position, selectPaintSize) -> {
				Log.e("TAGZ",
						"initTools: --------------p-" + position + " paint Size" + selectPaintSize);
				if (type_palette_character == 0) {
					doodlePaintSize = selectPaintSize / 2;
					doodleSizeIndex = position;
					sp.edit().putInt(DOODLE_PAINT_SIZE_INDEX, position).apply();
					sp.edit().putInt(DOODLE_PAINT_SIZE, doodlePaintSize).apply();
					mDataBinding.doodleView.setSize(doodlePaintSize);
				} else if (type_palette_character == 1) {
					characterPaintSize = 5 * selectPaintSize;
					characterSizeIndex = position;
					sp.edit().putInt(DRAW_TEXT_PAINT_SIZE_INDEX, position).apply();
					sp.edit().putInt(DRAW_TEXT_PAINT_SIZE, characterPaintSize).apply();
					mDataBinding.doodleView.setSize(characterPaintSize);
				}
				paintCircleCharacterSize(position);

				//设置 画笔 或 文字的 值
			});

			//色板 seekbar 选择器
			mDataBinding.editColorPick.setOnColorPickerChangeListener(new ColorSliderView.OnColorPickerChangeListener() {
				@Override
				public void onColorChanged (ColorSliderView picker, int color, float percent) {
					mDataBinding.circlePalette.setColor(color,
							CircleDisplayView.CirCleImageType.COLOR);
					Log.e("TAG", "onColorChanged: ======色板选择器 百分比============" + percent);
					//设置 画笔 或 文字的 值
					if (type_palette_character == 0) {
						doodlePercent = percent;
						doodleColorData = color;
						mColorDoodle.setColor(doodleColorData);
						mDataBinding.doodleView.setColor(mColorDoodle);
						sp.edit().putInt(DOODLE_PALETTE_COLOR_VALUE, color).apply();
						sp.edit().putFloat(DOODLE_PALETTE_COLOR_PERCENT, doodlePercent).apply();
					} else if (type_palette_character == 1) {
						characterPercent = percent;
						characterColorData = color;
						mColorText.setColor(characterColorData);
						mDataBinding.doodleView.setColor(mColorText);
						sp.edit().putInt(DRAW_TEXT_PALETTE_VALUE, color).apply();
						sp.edit().putFloat(DRAW_TEXT_PALETTE_PERCENT, characterPercent).apply();
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

			//左上角  顶部 退出按钮
			mDataBinding.ivToolsExit.setOnClickListener(v -> {

				mDataBinding.doodleView.setGestureRecognitionAble(false);

				mDataBinding.doodleView.save();
				//顶部状态栏变化
				mDataBinding.llActionbarNormal.setVisibility(View.VISIBLE);
				mDataBinding.clActionbarGesture.setVisibility(View.GONE);
				mDataBinding.circleCharacterSize.setSelected(false);
				mDataBinding.circlePalette.setSelected(false);
				mDataBinding.ivDetailToolsDoodle.setSelected(false);
				mDataBinding.ivDetailToolsCharacter.setSelected(false);

				mDataBinding.llPaletteCharacterSizeContainer.setVisibility(View.INVISIBLE);


			});
			//保存按钮
			mDataBinding.ivSavePhoto.setOnClickListener(v -> {
				mDataBinding.doodleView.setGestureRecognitionAble(false);

				mDataBinding.doodleView.save();
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

	// 添加文字
	private void createDoodleText (final DoodleText doodleText, final float x, final float y) {
		if (isFinishing()) {
			return;
		}

		DialogController.showInputTextDialog(this, doodleText == null ? null :
				doodleText.getText(), new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				//todo confirm operate

				Log.e("TAG", "----创建 文字------------------onClick: ");
				String text = (v.getTag() + "").trim();
				Log.e("TAG", "----创建 文字--------getTag----------onClick: " + text);
				if (TextUtils.isEmpty(text)) {
					return;
				}
				if (doodleText == null) {
					Log.e("TAG",
							"onClick: -------------mDataBinding.doodleView.getSize()-" + mDataBinding.doodleView.getSize());
					IDoodleSelectableItem item = new DoodleText(mDataBinding.doodleView, text,
							mDataBinding.doodleView.getSize(),
							mDataBinding.doodleView.getColor().copy(), x, y);
					mDataBinding.doodleView.addItem(item);
					touchGestureListener.setSelectedItem(item);
				} else {
					doodleText.setText(text);
				}
				mDataBinding.doodleView.refresh();
			}
		}, v -> {
			//todo cancel operate
		});
		//		if (doodleText == null) {
		//			mSettingsPanel.removeCallbacks(mHideDelayRunnable);
		//		}
	}


	private void paintCircleCharacterSize (int clickPosition) {
		mDataBinding.paintSizeSelect.setSelectIndex(clickPosition);
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

	@Override
	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_PARAMS, mDoodleParams);
	}

	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState,
	                                    PersistableBundle persistentState) {
		super.onRestoreInstanceState(savedInstanceState, persistentState);
		mDoodleParams = savedInstanceState.getParcelable(KEY_PARAMS);
	}

}
