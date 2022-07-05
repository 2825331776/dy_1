package com.dyt.wcc.common.widget.mySpinner;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dyt.wcc.common.R;


/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/8/24  11:41     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.wcc.dytfourbie.main.widget     </p>
 */
public abstract class MySpinnerBaseAdapter<T> extends BaseAdapter {
	private final PopUpTextAlignment   horizontalAlignment;//枚举类型 START 0  END 1  CENTER 2
	private final SpinnerTextFormatter spinnerTextFormatter;
	int selectedIndex;
	private int textColor;
	private int backgroundSelector;
	private Context mContext;

	/**
	 * constructor
	 *
	 * @param context
	 * @param textColor
	 * @param backgroundSelector
	 * @param spinnerTextFormatter
	 * @param horizontalAlignment
	 */
	MySpinnerBaseAdapter (Context context, int textColor, int backgroundSelector, SpinnerTextFormatter spinnerTextFormatter, PopUpTextAlignment horizontalAlignment) {
		this.mContext = context;
		this.spinnerTextFormatter = spinnerTextFormatter;
		this.backgroundSelector = backgroundSelector;
		this.textColor = textColor;
		this.horizontalAlignment = horizontalAlignment;
	}

	@Override
	public View getView (int position, View convertView, ViewGroup parent) {

		TextView textView;
		if (convertView == null) {
			convertView = View.inflate(mContext, R.layout.spinner_item, null);
			textView = convertView.findViewById(R.id.tv_item_spinner);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				textView.setBackground(ContextCompat.getDrawable(mContext, backgroundSelector));
			}
			convertView.setTag(new ViewHolder(textView));
		} else {
			textView = ((ViewHolder) convertView.getTag()).textView;
		}

		textView.setText(spinnerTextFormatter.format(getItem(position)));
		textView.setTextColor(textColor);

		setTextHorizontalAlignment(textView);

		return convertView;
	}

	private void setTextHorizontalAlignment (TextView textView) {
		switch (horizontalAlignment) {
			case START:
				textView.setGravity(Gravity.START);
				break;
			case END:
				textView.setGravity(Gravity.END);
				break;
			case CENTER:
				textView.setGravity(Gravity.CENTER_HORIZONTAL);
				break;
		}
	}

	@Override
	public long getItemId (int position) {
		return position;
	}

	public abstract T getItemInDataset (int position);

	@Override
	public abstract T getItem (int position);

	/***************get set method *******************/
	public int getSelectedIndex () {
		return selectedIndex;
	}

	void setSelectedIndex (int index) {
		selectedIndex = index;
	}

	static class ViewHolder {
		TextView textView;

		ViewHolder (TextView textView) {
			this.textView = textView;
		}
	}
}
