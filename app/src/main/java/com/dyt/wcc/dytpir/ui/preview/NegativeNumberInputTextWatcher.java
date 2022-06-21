package com.dyt.wcc.dytpir.ui.preview;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/6/21  10:22     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.preview     </p>
 */
public class NegativeNumberInputTextWatcher implements TextWatcher {
	private static final String Period         = ".";
	private static final String Zero           = "0";
	private static final String NegativeNumber = "-";
	private static final String TAG               = "NegativeNumberInputTextWatcher";

	/**
	 * 需要设置该 DecimalInputTextWatcher 的 EditText
	 */
	private EditText editText = null;

	/**
	 * 默认  小数的位数 2 位
	 */
	private static final int DEFAULT_DECIMAL_DIGITS = 2;

	private int decimalDigits = DEFAULT_DECIMAL_DIGITS;// 小数的位数
	private boolean canNegative = false;//是否可为负数
	private boolean canDecimal = false; //是否有小数
	private float show_max ;//能显示的最大值
	private float show_min ;//能显示的最小值
	private int totalDigits;//最大长度

	/**
	 * @param editText      editText
	 * @param show_min   显示的最小值
	 * @param show_max  显示的最大值
	 * @param canDecimalDigits 是否能输入小数点
	 * @param totalDigits 长度
	 */
	public NegativeNumberInputTextWatcher(EditText editText, float show_min, float show_max ,boolean canDecimalDigits, int totalDigits) {
		if (editText == null) {
			throw new RuntimeException("editText can not be null");
		}
		this.editText = editText;
		this.show_max = show_max;
		this.show_min = show_min;
		if (this.show_min < 0){
			this.canNegative = true;
		}
		this.canDecimal = canDecimalDigits;
		if (totalDigits <= 0)
			throw new RuntimeException("totalDigits must > 0");
		this.totalDigits = totalDigits;
//		if (decimalDigits <= 0)
//			throw new RuntimeException("decimalDigits must > 0");

//		this.decimalDigits = decimalDigits;
	}

	@Override
	public void beforeTextChanged (CharSequence s, int start, int count, int after) {
		Log.e(TAG, "beforeTextChanged: ");
	}

	@Override
	public void onTextChanged (CharSequence s, int start, int before, int count) {
		Log.e(TAG, "onTextChanged: ");
	}

	@Override
	public void afterTextChanged (Editable editable) {
		Log.e(TAG, "afterTextChanged: editable==>" +editable.toString());
		try {
			String s = editable.toString();
			editText.removeTextChangedListener(this);
			//限制最大长度
			editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(totalDigits)});

			if (s.contains(Period)) {
				//超过小数位限定位数,只保留限定小数位数
				if (s.length() - 1 - s.indexOf(Period) > decimalDigits) {
					s = s.substring(0,
							s.indexOf(Period) + decimalDigits + 1);
					editable.replace(0, editable.length(), s.trim());
				}
			}
			if (canDecimal) {//能否显示小数。
				if (s.trim().equals(Period)) {//如果首位输入"."自动补0
					s = Zero + s;
					editable.replace(0, editable.length(), s.trim());
				}
			}
			//首位输入0时,不再继续输入
			if (s.startsWith(Zero)
					&& s.trim().length() > 1) {
				if (!s.substring(1, 2).equals(Period)) {
					editable.replace(0, editable.length(), Zero);
				}
			}
//			//把输入的值保存在 最大值和最小值的范围内。
//			if ("-".equals(editable.toString())){}
			Log.e(TAG, "afterTextChanged: editable==>" +editable.toString());
			float fValue = Float.parseFloat(editable.toString());
			if (fValue> show_max){
				editable.replace(0,totalDigits,String.valueOf(show_max));
			}
			if (fValue < show_min){
				editable.replace(0,totalDigits,String.valueOf(show_min));
			}
			editText.addTextChangedListener(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
