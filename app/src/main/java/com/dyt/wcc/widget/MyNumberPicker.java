package com.dyt.wcc.widget;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.dyt.wcc.common.utils.TempConvertUtils;
import com.dyt.wcc.common.widget.NumberPickerView;
import com.dyt.wcc.common.widget.dragView.MeasureTempContainerView;
import com.dyt.wcc.R;

import java.lang.reflect.Field;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/11/29  13:51     </p>
 * <p>Description：超温报警弹窗，进来需要携带两个数据：一个是以前的值的数据，便于初始化。      </p>
 * <p>另外一个数据是  温度的单位。</p>
 * <p>PackagePath: com.dyt.wcc.ui.preview     </p>
 */
public class MyNumberPicker extends DialogFragment implements NumberPickerView.OnValueChangeListener {
	private static final String              TAG   = "MyNumberPicker";
	private              NumberPickerView    numberPickerView_hundreds;//百位
	private              NumberPickerView    numberPickerView_decade;//十位
	private              NumberPickerView    numberPickerView_unit;//个位
	private              NumberPickerView    numberPickerView_decimal;//小数位
	private              float               mValue;//数值
	private              Button              bt_confirm;//确认按钮
	private              Button              bt_cancel;
	private              int                 mType = 0; // 温度的类型 ： 0 摄氏度， 1 华氏度， 2 开氏度
	private              SetCompleteListener mListener;
	private              TextView            tv_unit;

	private String[] ranges = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

	public MyNumberPicker (@NonNull Context context, float oldValue, int type) {
		if (type < MeasureTempContainerView.tempSuffixList.length) {
			this.mType = type;
		} else {
			mType = 0;
		}
		mValue = TempConvertUtils.Celsius2Temp(oldValue, mType);
		if (mValue < 0) {
			mValue = 0.0f;
		}
		Log.e(TAG, "OverTempDialog: " + oldValue);
	}

	public SetCompleteListener getListener () {
		return mListener;
	}

	public void setListener (SetCompleteListener listener) {
		this.mListener = listener;
	}

	/**
	 * 连接机芯之后，拔出机芯，点击超温警告，出现闪退。
	 *
	 * @param manager
	 * @param tag
	 */
	@Override
	public void show (@NonNull FragmentManager manager, @Nullable String tag) {
		if (!isAdded()) {
			boolean canProcess = true;
			try {
				Class c = Class.forName("androidx.fragment.app.DialogFragment");
				Object obj = this;
				Field dismissed = c.getDeclaredField("mDismissed");
				dismissed.setAccessible(true);
				dismissed.set(obj, false);
				Field shownByMe = c.getDeclaredField("mShownByMe");
				shownByMe.setAccessible(true);
				shownByMe.set(obj, true);
			} catch (Exception e) {
				e.printStackTrace();
				canProcess = false;
			} finally {
				if (canProcess) {
					FragmentTransaction ft = manager.beginTransaction();
					ft.add(this, tag);
					ft.commitAllowingStateLoss();
					Log.e("BaseDialogFragment", "show commitAllowingStateLoss");
				} else {
					super.show(manager, tag);
					Log.e("BaseDialogFragment", "show commit");
				}
			}
		}
	}
	//	private int mRotation;
	//
	//	public int getRotation () {
	//		return mRotation;
	//	}
	//	public void setRotation (int rotation) {
	//		this.mRotation = rotation;
	////		getWindow().getDecorView().setRotation(mRotation);
	//	}

	//	@Override
	//	protected void onCreate (Bundle savedInstanceState) {
	//		super.onCreate(savedInstanceState);
	//		setContentView(R.layout.pop_overtemp_alarm);
	//
	//		initView();
	//		initData();
	//	}
	//	private boolean rotation;

	@Nullable
	@Override
	public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.pop_overtemp_alarm, container, false);
		//		if (rotation) {
		//			view.setRotation(180);
		//		} else {
		//			view.setRotation(0);
		//		}
		initView(view);
		initData();
		return view;
	}

	private void initView (@NonNull View view) {
		numberPickerView_hundreds = view.findViewById(R.id.numberPicker_hundreds_main_preview_overTemp_pop);
		numberPickerView_decade = view.findViewById(R.id.numberPicker_decade_main_preview_overTemp_pop);
		numberPickerView_unit = view.findViewById(R.id.numberPicker_unit_main_preview_overTemp_pop);
		numberPickerView_decimal = view.findViewById(R.id.numberPicker_decimal_main_preview_overTemp_pop);
		bt_confirm = view.findViewById(R.id.bt_submit);
		bt_cancel = view.findViewById(R.id.bt_cancel);
		tv_unit = view.findViewById(R.id.tv_main_preview_ovetemp_pop_unit);
	}

	private void initData () {
		numberPickerView_hundreds.refreshByNewDisplayedValues(ranges);
		numberPickerView_decade.refreshByNewDisplayedValues(ranges);
		numberPickerView_unit.refreshByNewDisplayedValues(ranges);
		numberPickerView_decimal.refreshByNewDisplayedValues(ranges);

		numberPickerView_hundreds.setOnValueChangedListener(this);
		numberPickerView_decade.setOnValueChangedListener(this);
		numberPickerView_unit.setOnValueChangedListener(this);
		numberPickerView_decimal.setOnValueChangedListener(this);

		numberPickerView_hundreds.setValue((int) mValue / 100);
		numberPickerView_decade.setValue((int) mValue % 100 / 10);
		numberPickerView_unit.setValue((int) mValue % 10);
		numberPickerView_decimal.setValue((int) (mValue * 10) % 10);
		tv_unit.setText(MeasureTempContainerView.tempSuffixList[mType]);

		bt_confirm.setOnClickListener(v -> {
			Log.e(TAG, "onClick:   value  = > " + mValue + "  mode =  > " + mType);
			if (mListener != null)
				mListener.onSetComplete(TempConvertUtils.temp2Celsius(mValue, mType));
			dismiss();
		});
		bt_cancel.setOnClickListener(v -> {
			if (mListener != null)
				mListener.onCancelListener();
			dismiss();
		});
	}

	@Override
	public void onValueChange (NumberPickerView picker, int oldVal, int newVal) {
		//		Toast.makeText(getContext(),picker.getDisplayedValues()[newVal - picker.getMinValue()],Toast.LENGTH_SHORT).show();
		String value = picker.getDisplayedValues()[newVal - picker.getMinValue()];
		switch (picker.getId()) {
			case R.id.numberPicker_hundreds_main_preview_overTemp_pop://百分更改
				mValue = mValue % 100 + Float.parseFloat(value) * 100;
				break;
			case R.id.numberPicker_decade_main_preview_overTemp_pop://十位更改， 第一部分：个位加小数。第二部分 为百位 ， 第三部分 十位更改后的值
				mValue = mValue % 10 + ((int) mValue / 100) * 100 + Float.parseFloat(value) * 10;
				break;
			case R.id.numberPicker_unit_main_preview_overTemp_pop://个位更改
				mValue = (int) mValue / 10 * 10 + Float.parseFloat(value) + (mValue - (int) mValue);
				break;
			case R.id.numberPicker_decimal_main_preview_overTemp_pop://小数位更改
				mValue = (int) mValue + Float.parseFloat(value) / 10;
				break;
		}
	}

	public interface SetCompleteListener {
		/**
		 * 设置超温警告的温度， 数值的单位为： 摄氏度。
		 *
		 * @param setValue float 类型的 摄氏度
		 */
		void onSetComplete (float setValue);

		void onCancelListener ();
	}
}