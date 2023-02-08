package com.dytest.wcc.ImageLoad;

import android.text.TextUtils;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;

/**
 * <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
 * <p>Author：stefan cheng    </p>
 * <p>Create Date：2021/9/26  16:17 </p>
 * <p>Description：@todo describe         </p>
 * <p>PackagePath: com.dyt.wcc.ImageLoad     </p>
 */
public class ImageViewBindAdapter {
	/**
	 * 欢迎页面加载图片
	 *
	 * @param imageView
	 * @param imageUrl
	 * @param resId
	 */
	@BindingAdapter(value = {"netUrl", "localRes"}, requireAll = false)
	public static void WelComeLoadImage (ImageView imageView, String imageUrl, int resId) {
		if (imageUrl != null && (!TextUtils.isEmpty(imageUrl))) {
			Glide.with(imageView).load(imageUrl).into(imageView);
		} else {
			Glide.with(imageView).load(resId).into(imageView);
		}

	}

}
