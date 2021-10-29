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
	private Uri uriAddress;
	private String AbsoluteAddress;
	private int type;
	private boolean isSelect ;

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
