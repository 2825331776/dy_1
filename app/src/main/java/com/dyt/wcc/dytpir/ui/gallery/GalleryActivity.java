package com.dyt.wcc.dytpir.ui.gallery;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.dyt.wcc.dytpir.R;
import com.huantansheng.easyphotos.EasyPhotos;

public class GalleryActivity extends AppCompatActivity {

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gallery);

		EasyPhotos.createAlbum(this, false, false, GlideEngine.getInstance())
//				.setFileProviderAuthority("com.huantansheng.easyphotos.demo.fileprovider")
				.setFileProviderAuthority("com.dyt.wcc.dytpir.FileProvider")
				.setCount(9)
				.setVideo(true)
				.setGif(false)
				.start(101);
	}
}