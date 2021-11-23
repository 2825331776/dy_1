package com.dyt.wcc.dytpir.ui.gallry;

import android.net.Uri;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/10/29  14:06     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.gallry     </p>
 */
public class GalleryBean {
	//Uri地址
	private Uri uriAddress;
	//绝对地址
	private String AbsoluteAddress;
	//类型
	private int type;
	//是否被选中
	private boolean isSelect ;
	//视频时长
	private int videoDuration ;

	public int getVideoDuration () {
		return videoDuration;
	}
	public void setVideoDuration (int videoDuration) {
		this.videoDuration = videoDuration;
	}

	public boolean isSelect () {
		return isSelect;
	}

	public void setSelect (boolean select) {
		isSelect = select;
	}

	public Uri getUriAddress () {
		return uriAddress;
	}

	public void setUriAddress (Uri uriAddress) {
		this.uriAddress = uriAddress;
	}

	public String getAbsoluteAddress () {
		return AbsoluteAddress;
	}

	public void setAbsoluteAddress (String absoluteAddress) {
		AbsoluteAddress = absoluteAddress;
	}

	public int getType () {
		return type;
	}

	public void setType (int type) {
		this.type = type;
	}
}
