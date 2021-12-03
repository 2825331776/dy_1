package com.dyt.wcc.dytpir.ui.gallery;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.constans.DYConstants;
import com.dyt.wcc.dytpir.databinding.FragmentGalleryMainBinding;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:15     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.gallry     </p>
 */
public class GalleryFragment extends BaseFragment <FragmentGalleryMainBinding> implements View.OnClickListener {
	private GridLayoutManager gridLayoutManager ;
	private GalleryAdapter galleryAdapter ;

	private CopyOnWriteArrayList<GalleryBean > imagePathList;
	private CopyOnWriteArrayList<GalleryBean >            showList;

	private int selectCondition = 0;

	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what){
				case DYConstants.IMAGE_READY:
					//在此之前 对数据进行排序
					toSort();
					initRecycleView();
					break;
			}
			return false;
		}
	});

	private void toSort(){
		if (imagePathList!=null){
			//冒泡排序
			GalleryBean temp= null;

			for(int i = 0;i < imagePathList.size()-1;i++){
				for(int j = 0;j <imagePathList.size()-1-i;j++){
					if(imagePathList.get(j).getCreateDate() < imagePathList.get(j+1).getCreateDate()){
						temp = imagePathList.get(j);
						imagePathList.set(j,imagePathList.get(j+1));
						imagePathList.set(j+1, temp);
					}
				}
			}
			temp = null;

		}
	}

	private void  initRecycleView(){
		if (selectCondition == 0){
			showList.clear();
			showList.addAll(imagePathList);
			galleryAdapter.notifyDataSetChanged();
		}
		if (selectCondition == 1){
			for (GalleryBean bean : imagePathList){
				if (bean.getAbsoluteAddress().endsWith("jpg") || bean.getAbsoluteAddress().endsWith("png")){
					showList.add(bean);
				}
				galleryAdapter.notifyDataSetChanged();
			}
		}
		if (selectCondition == 2){
			for (GalleryBean bean : imagePathList){
				if (bean.getAbsoluteAddress().endsWith("mp4")){
					showList.add(bean);
				}
				galleryAdapter.notifyDataSetChanged();
			}
		}
		if (isDebug)Log.e(TAG, "handleMessage: "+ imagePathList.size() );
		if (isDebug)Log.e(TAG, "handleMessage: "+ imagePathList);
	}

	/**
	 * 得到相册的文件列表
	 */
	private void getImageList(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				imagePathList.clear();

				ContentResolver contentResolver = mContext.get().getContentResolver();

				Uri uriAll =  MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				//查询的数据
				String [] projection = new String[]{
						MediaStore.Images.Thumbnails._ID ,MediaStore.Images.Thumbnails.DATA,MediaStore.Images.Media.DATE_MODIFIED};
				//条件
				String selection = MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?";
				//条件参数
				String [] selectionArgs = new String[] {"image/png", "image/jpeg"};
				//查询排序方式
				String sort = MediaStore.Images.Media.DATE_MODIFIED + " ASC ";

				Cursor cursor = contentResolver.query(uriAll,projection,
						selection, selectionArgs, sort);

				// 获取id字段是第几列，该方法最好在循环之前做好
				int idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
				// 获取data字段是第几列，该方法最好在循环之前做好
				int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
				int imageDateModifiedColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);

				while (cursor.moveToNext()) {
					long id = cursor.getLong(idIndex);
					// 获取到每张图片的uri
					Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,id);
					//uri转 绝对路径， 如果路径是DYTCamera 结尾，则添加到list
					// 获取到每张图片的绝对路径
					String path = cursor.getString(dataIndex);
					long imageDataModified = cursor.getLong(imageDateModifiedColumnIndex);
					if (path.contains(DYConstants.PIC_PATH)){
						GalleryBean imageBean = new GalleryBean();
						imageBean.setType(0);
						imageBean.setAbsoluteAddress(path);
						imageBean.setUriAddress(imageUri);
						imageBean.setCreateDate(imageDataModified);

						imagePathList.add(imageBean);
					}
				}
				cursor.close();

				Uri uriVideo =  MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				//查询的数据
				String [] videoProjection = new String[]{MediaStore.Video.Media._ID ,MediaStore.Video.Media.DATA
						,MediaStore.Video.Media.DURATION,MediaStore.Images.Media.DATE_MODIFIED};

				//条件
				String videoSelection = MediaStore.Video.Media.MIME_TYPE + "=?";
				//条件参数
				String [] videoSelectionArgs = new String[] {"video/mp4"};
				//查询排序方式
				String videoSort = MediaStore.Video.Media.DATE_MODIFIED + " ASC ";

				Cursor videoCursor = contentResolver.query(uriVideo,videoProjection,
						videoSelection, videoSelectionArgs, videoSort);

				// 获取id字段是第几列，该方法最好在循环之前做好
				int videoIdIndex = videoCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
				// 获取data字段是第几列，该方法最好在循环之前做好
				int videoDataIndex = videoCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

				int duration = videoCursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION);
				int videoDateModifiedColumnIndex = videoCursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATE_MODIFIED);

				while (videoCursor.moveToNext()) {
					long id = videoCursor.getLong(videoIdIndex);
					// 获取到每张图片的uri
					Uri imageUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,id);
					//uri转 绝对路径， 如果路径是DYTCamera 结尾，则添加到list
					// 获取到每张图片的绝对路径
					String path = videoCursor.getString(videoDataIndex);
					int durations = videoCursor.getInt(duration);
					long dataModified = videoCursor.getLong(videoDateModifiedColumnIndex);

					if (path.contains(DYConstants.PIC_PATH)){

						GalleryBean videoBean = new GalleryBean();
						videoBean.setType(1);
						videoBean.setAbsoluteAddress(path);
						videoBean.setUriAddress(imageUri);
						videoBean.setVideoDuration(durations);
						videoBean.setCreateDate(dataModified);

						imagePathList.add(videoBean);
					}
				}
				videoCursor.close();
				mHandler.sendEmptyMessage(DYConstants.IMAGE_READY);
				}
		}).start();
	}

	@Override
	public void onResume () {
		super.onResume();
		Log.e(TAG, "onResume: ");
	}

	@Override
	public void onStop () {
		super.onStop();
		Log.e(TAG, "onStop: ");
	}

	@Override
	public void onDestroyView () {
		super.onDestroyView();
		Log.e(TAG, "onDestroyView: ");
	}

	@Override
	public void onDestroy () {
		super.onDestroy();
		Log.e(TAG, "onDestroy: ");
	}

	@Override
	public void onDetach () {
		super.onDetach();
		Log.e(TAG, "onDetach: ");
	}

	@Override
	public void onCreate (@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "onCreate: ");
	}

	/**
	 * 检查扩展名，得到图片格式的文件
	 * @param fName  文件名
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	private boolean checkIsImageFile(String fName) {
		boolean isImageFile = false;
		// 获取扩展名
		String FileEnd = fName.substring(fName.lastIndexOf(".") + 1,
				fName.length()).toLowerCase();
		if (FileEnd.equals("jpg") || FileEnd.equals("png")
				|| FileEnd.equals("jpeg")|| FileEnd.equals("mp4") ) {
			isImageFile = true;
		} else {
			isImageFile = false;
		}
		return isImageFile;
	}

	@Override
	protected boolean isInterceptBackPress () {
		return false;
	}

	@Override
	protected int bindingLayout () {
		return R.layout.fragment_gallery_main;
	}

	@Override
	public void onClick (View v) {
		switch (v.getId()){
			case R.id.bt_all_gallery_rightRl:
//				mDataBinding.btAllGalleryRightRl.setSelected(!mDataBinding.btAllGalleryRightRl.isSelected());
				selectCondition = 0;
				getImageList();
				break;
			case R.id.bt_pic_gallery_rightRl:
//				mDataBinding.btPicGalleryRightRl.setSelected(!mDataBinding.btPicGalleryRightRl.isSelected());
				showList.clear();
				selectCondition = 1;
				getImageList();
				break;
			case R.id.bt_video_gallery_rightRl:
//				mDataBinding.btVideoGalleryRightRl.setSelected(!mDataBinding.btVideoGalleryRightRl.isSelected());
				showList.clear();
				selectCondition = 2;
				getImageList();
				break;
			case R.id.bt_delete_gallery_rightRl:
				for (GalleryBean child : showList){
					if (child.isSelect()){
						if (child.getAbsoluteAddress().endsWith("mp4")){
								int res = mContext.get().getContentResolver().delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
										MediaStore.Video.Media.DATA + "=?",
										new String[]{child.getAbsoluteAddress()});
								if (res > -1){
									Log.e(TAG, "删除文件成功");
									showList.remove(child);
									galleryAdapter.notifyDataSetChanged();

									Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
									intent.setData(child.getUriAddress());
									mContext.get().sendBroadcast(intent);
								}else{
									Log.e(TAG, "删除文件失败");
								}
							}else {
								int res = mContext.get().getContentResolver().delete(child.getUriAddress(),
										MediaStore.Images.Media.DATA + "=?",
										new String[]{child.getAbsoluteAddress()});
								if (res > -1){
									showList.remove(child);
									galleryAdapter.notifyDataSetChanged();

									Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
									intent.setData(child.getUriAddress());
									mContext.get().sendBroadcast(intent);

									Log.e(TAG, "删除文件成功" + res);
								}else{
									Log.e(TAG, "删除文件失败");
								}
						}
					}
				}
				break;
		}
	}

	//删除图库照片
	private boolean deleteImage(String imgPath) {
		ContentResolver resolver = mContext.get().getContentResolver();
		Cursor cursor = MediaStore.Images.Media.query(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=?",
				new String[]{imgPath}, null);
		boolean result = false;
		if (null != cursor && cursor.moveToFirst()) {
			long id = cursor.getLong(0);
			Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
			Uri uri = ContentUris.withAppendedId(contentUri, id);
			int count = mContext.get().getContentResolver().delete(uri, null, null);
			result = count == 1;
		} else {
			File file = new File(imgPath);
			result = file.delete();
		}
		Log.e(TAG,"--deleteImage--imgPath:" + imgPath + "--result:" + result);
		return result;
	}

	@Override
	protected void initView () {

		Log.e(TAG, "initView: ");
		imagePathList = new CopyOnWriteArrayList<>();
		showList = new CopyOnWriteArrayList<>();



		mDataBinding.btAllGalleryRightRl.setOnClickListener(this);
		mDataBinding.btPicGalleryRightRl.setOnClickListener(this);
		mDataBinding.btVideoGalleryRightRl.setOnClickListener(this);
		mDataBinding.btDeleteGalleryRightRl.setOnClickListener(this);

//		mDataBinding.btAllGalleryRightRl.setSelected(!mDataBinding.btAllGalleryRightRl.isSelected());


		mDataBinding.setFgGallery(this);
		mDataBinding.ivBackGallery.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				Navigation.findNavController(mDataBinding.getRoot()).navigateUp();
			}
		});

		gridLayoutManager = new GridLayoutManager(mContext.get(),5);
		galleryAdapter = new GalleryAdapter(mContext.get(),showList);

		mDataBinding.recyclerViewGallery.setLayoutManager(gridLayoutManager);
		mDataBinding.recyclerViewGallery.setAdapter(galleryAdapter);
		galleryAdapter.setOnItemClickListener(new GalleryAdapter.MyOnItemClickListener() {
			@Override
			public void itemClickListener (int position) {
				Bundle args = new Bundle();
				args.putString("pathList",showList.get(position).getUriAddress().toString());

				if (showList.get(position).getAbsoluteAddress().endsWith("mp4")){//查看MP4文件
					Intent intent = new Intent();
					intent.setAction(android.content.Intent.ACTION_VIEW);
					Uri contentUri = showList.get(position).getUriAddress();
					intent.setDataAndType(contentUri, "video/mp4");
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
						intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}else {
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					}
					mContext.get().startActivity(intent);
				}else {//查看图片
					Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_galleryFg_to_lookFileFg,args);
				}
			}
		});

//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
			getImageList();
//		}else {
//
//		}
	}
}
