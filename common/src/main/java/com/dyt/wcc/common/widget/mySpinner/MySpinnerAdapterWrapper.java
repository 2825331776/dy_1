package com.dyt.wcc.common.widget.mySpinner;

import android.content.Context;
import android.widget.ListAdapter;

/**
 * <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
 * <p>Author：stefan cheng    </p>
 * <p>Create Date：2021/8/24  15:19 </p>
 * <p>Description：@todo describe         </p>
 * <p>PackagePath: com.wcc.dytfourbie.main.widget     </p>
 */
public class MySpinnerAdapterWrapper extends MySpinnerBaseAdapter {

	private final ListAdapter baseAdapter;

	MySpinnerAdapterWrapper (Context context, ListAdapter toWrap, int textColor, int backgroundSelector, SpinnerTextFormatter spinnerTextFormatter, PopUpTextAlignment horizontalAlignment) {
		super(context, textColor, backgroundSelector, spinnerTextFormatter, horizontalAlignment);
		baseAdapter = toWrap;
	}

	@Override
	public int getCount () {
		return baseAdapter.getCount() - 1;
	}

	@Override
	public Object getItem (int position) {
		return baseAdapter.getItem(position >= selectedIndex ? position + 1 : position);
	}

	@Override
	public Object getItemInDataset (int position) {
		return baseAdapter.getItem(position);
	}
}