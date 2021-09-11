package com.dyt.wcc.dytpir.widget;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/11  11:11     </p>
 * <p>Description：自定义开关控件         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.widget     </p>
 */
public class MyToggle extends LinearLayout {
	private TextView toggleTv;
	private TextView characterTv;

	private String defaultString = "开关";
	private int textSize = 10;//默认字体大小



	public MyToggle (Context context) {
		super(context);
	}


}
