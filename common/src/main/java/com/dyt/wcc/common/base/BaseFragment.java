package com.dyt.wcc.common.base;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;

import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/8  14:59     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.common.base.ui     </p>
 */
public abstract class BaseFragment<T extends ViewDataBinding> extends Fragment {
	protected WeakReference <Activity> mContext;
	protected final String             TAG = this.getClass().getSimpleName();
	protected T                        mDataBinding;
	protected boolean                  isDebug = true;
	protected Toast mToast;

	@Nullable
	@Override
	public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mDataBinding = DataBindingUtil.inflate(inflater,bindingLayout(),container,false);
		View view = mDataBinding.getRoot();
		mContext = new WeakReference<>(getActivity());
		initView();
		mToast = Toast.makeText(mContext.get(),"",Toast.LENGTH_SHORT);

		return view;
	}

	protected void showToast(int resId){
//		mToast.cancel();
		mToast.setText(resId);
		mToast.show();
	}
	protected void showToast(String str){
//		mToast.cancel();
		mToast.setText(str);
		mToast.show();
	}
	protected void cancelToast(){
		if (mToast!= null)mToast.cancel();
	}

	////设置绑定布局
	protected abstract int bindingLayout();
	//初始化控件
	protected abstract void initView();

	//是否拦截系统的返回事件
	protected abstract boolean isInterceptBackPress();

	//检查权限列表中是否存在未授权的 权限。
	protected boolean checkPermission(String ... permissions){
		boolean result = true;
		for (String permission : permissions){
			result = result	&& (ActivityCompat.checkSelfPermission(mContext.get(),permission) == PackageManager.PERMISSION_GRANTED);
		}
		return result;
	}

	protected void getPermissions(String ... permissions){
		PermissionX.init(this).permissions(permissions).request(new RequestCallback() {
			@Override
			public void onResult (boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {

			}
		});
	}


}
