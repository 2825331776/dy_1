package com.dyt.wcc.common.widget.mySpinner;

import android.content.Context;

import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/8/24  14:23     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.wcc.dytfourbie.main.widget     </p>
 */
public class MySpinnerAdapter<T> extends MySpinnerBaseAdapter {

	private final List<T> items;

	/**
	 *  constructor
	 * @param context
	 * @param items
	 * @param textColor
	 * @param backgroundSelector
	 * @param spinnerTextFormatter
	 * @param horizontalAlignment
	 */
	MySpinnerAdapter (
			Context context,
			List<T> items,
			int textColor,
			int backgroundSelector,
			SpinnerTextFormatter spinnerTextFormatter,
			PopUpTextAlignment horizontalAlignment
	) {
		super(context, textColor, backgroundSelector, spinnerTextFormatter, horizontalAlignment);
		this.items = items;
	}

	@Override
	public int getCount () {
		return items.size()-1;
	}

	@Override
	public Object getItem (int position) {
		if (position >= selectedIndex) {
			return items.get(position + 1);
		} else {
			return items.get(position);
		}
	}

	@Override
	public Object getItemInDataset (int position) {
		return items.get(position);
	}
}
