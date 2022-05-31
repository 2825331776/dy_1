package com.dyt.wcc.dytpir.widget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;

/**
 * <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
 * <p>Author：stefan cheng    </p>
 * <p>Create Date：2021/9/26  16:18 </p>
 * <p>Description：@todo describe         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.widget     </p>
 */
public class MyPopWindows extends PopupWindow {
	private static final String  TAG = "MyPopWindow";
	private              Context mContext;


	public MyPopWindows (Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MyPopWindows (Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public MyPopWindows (View contentView, int width, int height) {
		super(contentView, width, height);
	}

	public MyPopWindows (View contentView, int width, int height, boolean focusable) {
		super(contentView, width, height, focusable);
	}


	//	@Override
	//	public void showAsDropDown (View anchor, int xoff, int yoff, int gravity) {
	//		if(Build.VERSION.SDK_INT == 24) {
	//			Rect rect = new Rect();
	//			anchor.getGlobalVisibleRect(rect);
	//			int h = anchor.getResources().getDisplayMetrics().heightPixels - rect.bottom;
	//			setHeight(h);
	////			yoff = -h;
	//		}
	//		super.showAsDropDown(anchor, xoff, yoff, gravity);
	//	}

	@Override
	public void showAsDropDown (View anchor, int xoff, int yoff) {
		if (Build.VERSION.SDK_INT >= 24) {
			Rect rect = new Rect();
			anchor.getGlobalVisibleRect(rect);
			int h = anchor.getResources().getDisplayMetrics().heightPixels - rect.bottom;
			int w = anchor.getResources().getDisplayMetrics().widthPixels - rect.left;
			Log.e(TAG, "showAsDropDown: h " + h + " w " + w);
			setHeight(h);
			setWidth(w);
			//			yoff = -h;
		}
		Log.e(TAG, "showAsDropDown: " + getHeight() + " width = " + getWidth());
		super.showAsDropDown(anchor, xoff, yoff);
	}

	@Override
	public void showAtLocation (View parent, int gravity, int x, int y) {
		//		if(Build.VERSION.SDK_INT == 24) {
		//			Rect rect = new Rect();
		//			parent.getGlobalVisibleRect(rect);
		//			int h = parent.getResources().getDisplayMetrics().heightPixels - rect.bottom;
		//			setHeight(h);
		//			//			yoff = -h;
		//		}
		super.showAtLocation(parent, gravity, x, y);
	}
}
