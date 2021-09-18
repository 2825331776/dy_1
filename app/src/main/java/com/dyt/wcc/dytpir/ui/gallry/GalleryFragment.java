package com.dyt.wcc.dytpir.ui.gallry;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.dyt.wcc.common.base.BaseFragment;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.constans.DYConstants;
import com.dyt.wcc.dytpir.databinding.FragmentGalleryMainBinding;

import java.io.File;
import java.util.ArrayList;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/9  16:15     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.ui.gallry     </p>
 */
public class GalleryFragment extends BaseFragment <FragmentGalleryMainBinding> {
	private GridLayoutManager gridLayoutManager ;
	private GalleryAdapter galleryAdapter ;

	private ArrayList<String > imagePathList;

	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage (@NonNull Message msg) {
			switch (msg.what){
				case DYConstants.IMAGE_READY:

					Log.e(TAG, "handleMessage: ");
					gridLayoutManager = new GridLayoutManager(mContext.get(),5);
					galleryAdapter = new GalleryAdapter(mContext.get(),imagePathList);

					mDataBinding.recyclerViewGallery.setLayoutManager(gridLayoutManager);
					mDataBinding.recyclerViewGallery.setAdapter(galleryAdapter);
					galleryAdapter.setOnItemClickListener(new GalleryAdapter.MyOnItemClickListener() {
						@Override
						public void itemClickListener (int position) {
							Bundle args = new Bundle();
							args.putStringArrayList("pathList",imagePathList);
							args.putInt("position",position);

							if (imagePathList.get(position).endsWith("mp4")){//查看MP4文件
								Intent intent = new Intent();
								intent.setAction(android.content.Intent.ACTION_VIEW);
								File file = new File(imagePathList.get(position));
								Uri uri;

								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
									intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
									Uri contentUri = FileProvider.getUriForFile(mContext.get(), mContext.get().getApplicationContext().getPackageName()
											+ ".FileProvider", file);
									intent.setDataAndType(contentUri, "video/*");
								} else {
									uri = Uri.fromFile(file);
									intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									intent.setDataAndType(uri, "video/*");
								}

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
				Log.e(TAG, "run: " + filePath);

				File fileAll = new File(filePath);
//				Log.e(TAG, "run: "+ fileAll.getAbsolutePath());
				File[] files = fileAll.listFiles();
				if (files!= null){
					for (File file : files) {
						if (checkIsImageFile(file.getPath())) {// 将所有的文件存入ArrayList中,并过滤所有图片格式的文件
							imagePathList.add(file.getPath());
						}
					}

					mHandler.sendEmptyMessage(DYConstants.IMAGE_READY);
				}else {
					Log.e(TAG, "run: files is null");
				}

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
	protected int bindingLayout () {
		return R.layout.fragment_gallery_main;
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
		getImageList();
	}
}
