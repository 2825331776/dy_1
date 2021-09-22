package com.dyt.wcc.common.widget.mySpinner;

import android.text.Spannable;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/8/24  11:23     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.wcc.dytfourbie.main.widget     </p>
 */
public interface SpinnerTextFormatter<T> {

	Spannable format(T item);
}
