package com.dyt.wcc.cameracommon.widget;

/**
 * Created by Administrator on 2018/3/29 0029.
 */

import android.graphics.Bitmap;

import com.serenegiant.glutils.IDrawer2D;

public interface IDrawer2dES21 extends IDrawer2D {
	int glGetAttribLocation (String var1);

	int glGetUniformLocation (String var1);

	void glUseProgram ();

	void draw (int[] var1, float[] var2, int var3, Bitmap bitmap);
}
