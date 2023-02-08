package com.dytest.wcc.common.widget.mySpinner;

import android.text.Spannable;
import android.text.SpannableString;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/8/24  11:24     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.wcc.dytfourbie.main.widget     </p>
 */
public class SimpleSpinnerTextFormatter implements SpinnerTextFormatter {

	@Override
	public Spannable format (Object item) {
		return new SpannableString(item.toString());
	}
}
