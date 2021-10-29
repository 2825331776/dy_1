package com.dyt.wcc.dytpir.ui.gallry;

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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.constans.DYConstants;
import com.dyt.wcc.dytpir.databinding.FragmentGalleryMainBinding;

import java.util.ArrayList;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:15     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.gallry     </p>
 */
public class GalleryFragment extends BaseFragment <FragmentGalleryMainBinding> {
	private GridLayoutManager gridLayoutManager ;
	private GalleryAdapter galleryAdapter ;

	private ArrayList<GalleryBean > imagePathList;

	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what){
				case DYConstants.IMAGE_READY:

					Log.e(TAG, "handleMessage: ");
					gridLayoutManager = new GridLayoutManager(mContext.get(),4);
					galleryAdapter = new GalleryAdapter(mContext.get(),imagePathList);

					mDataBinding.recyclerViewGallery.setLayoutManager(gridLayoutManager);
					mDataBinding.recyclerViewGallery.setAdapter(galleryAdapter);
					galleryAdapter.setOnItemClickListener(new GalleryAdapter.MyOnItemClickListener() {
						@Override
						public void itemClickListener (int position) {
							Bundle args = new Bundle();
							args.putString("pathList",imagePathList.get(position).getUriAddress().toString());

							if (imagePathList.get(position).getAbsoluteAddress().endsWith("mp4")){//查看MP4文件
								Intent intent = new Intent();
								intent.setAction(android.content.Intent.ACTION_VIEW);
								intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								Uri contentUri = imagePathList.get(position).getUriAddress();
								intent.setDataAndType(contentUri, "video/mp4");

								mContext.get().startActivity(intent);
							}else {//查看图片
								Navigation.findNavController(mDataBinding.getRoot()).navigate(R.id.action_galleryFg_to_lookFileFg,args);
							}
						}
					});
//					mDataBinding.tvTittle.setText(getResources().getString(R.string.DYTGallery)+"("+imagePathList.size()+")");
					Log.e(TAG, "handleMessage: "+ imagePathList.size() );
					Log.e(TAG, "handleMessage: "+ imagePathList);
					break;
			}
			return false;
		}
	});

	/**
	 * 得到相册的文件列表
	 */
	private void getImageList(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				String filePath = DYConstants.PIC_PATH;

				ContentResolver contentResolver = mContext.get().getContentResolver();

				Uri uriAll =  MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				//查询的数据
				String [] projection = new String[]{
						MediaStore.Images.Media._ID ,MediaStore.Images.Media.DATA};
				//条件
				String selection = MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?";
				//条件参数
				String [] selectionArgs = new String[] {"image/png"};
				//查询排序方式
				String sort = MediaStore.Images.Media.DATE_MODIFIED + " desc ";

				Cursor cursor = contentResolver.query(uriAll,projection,
						selection, selectionArgs, sort);

//				final String column = "_data";
				// 获取id字段是第几列，该方法最好在循环之前做好
				int idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
				// 获取data字段是第几列，该方法最好在循环之前做好
				int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

				while (cursor.moveToNext()) {
					long id = cursor.getLong(idIndex);
					// 获取到每张图片的uri
					Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,id);
					//uri转 绝对路径， 如果路径是DYTCamera 结尾，则添加到list
					// 获取到每张图片的绝对路径
					String path = cursor.getString(dataIndex);
					if (path.contains(DYConstants.PIC_PATH)){
						GalleryBean imageBean = new GalleryBean();
						imageBean.setType(0);
						imageBean.setAbsoluteAddress(path);
						imageBean.setUriAddress(imageUri);

						imagePathList.add(imageBean);

						Log.e(TAG, "run: imageUri " + imageUri);
						Log.e(TAG, "run: path " + path);
					}
				}
				cursor.close();

				Uri uriVideo =  MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				//查询的数据
				String [] videoProjection = new String[]{MediaStore.Video.Media._ID ,MediaStore.Video.Media.DATA};
				//条件
				String videoSelection = MediaStore.Video.Media.MIME_TYPE + "=?";
				//条件参数
				String [] videoSelectionArgs = new String[] {"video/mp4"};
				//查询排序方式
				String videoSort = MediaStore.Video.Media.DATE_MODIFIED + " desc ";

				Cursor videoCursor = contentResolver.query(uriVideo,videoProjection,
						videoSelection, videoSelectionArgs, videoSort);

				// 获取id字段是第几列，该方法最好在循环之前做好
				int videoIdIndex = videoCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
				// 获取data字段是第几列，该方法最好在循环之前做好
				int videoDataIndex = videoCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

				while (videoCursor.moveToNext()) {
					long id = videoCursor.getLong(videoIdIndex);
					// 获取到每张图片的uri
					Uri imageUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,id);
					//uri转 绝对路径， 如果路径是DYTCamera 结尾，则添加到list
					// 获取到每张图片的绝对路径
					String path = videoCursor.getString(videoDataIndex);
					if (path.contains(DYConstants.PIC_PATH)){

						GalleryBean videoBean = new GalleryBean();
						videoBean.setType(1);
						videoBean.setAbsoluteAddress(path);
						videoBean.setUriAddress(imageUri);

						imagePathList.add(videoBean);
					}
				}
				videoCursor.close();

				mHandler.sendEmptyMessage(DYConstants.IMAGE_READY);

				}
		}).start();
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

	public void onSelectAll(View view){

	}
	public void onSelectImage(View view){

	}
	public void onSelectVideo(View view){

	}

	@Override
	protected void initView () {
		imagePathList = new ArrayList<>();

		mDataBinding.setFgGallery(this);
		mDataBinding.includeGallery.ivBackGallery.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				Navigation.findNavController(mDataBinding.getRoot()).navigateUp();
			}
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
			getImageList();
		}else {

		}
	}
}
