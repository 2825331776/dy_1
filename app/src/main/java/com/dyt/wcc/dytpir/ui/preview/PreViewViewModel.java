package com.dyt.wcc.dytpir.ui.preview;

import android.app.Application;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.lang.ref.WeakReference;

/**
* <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
* <p>Author：stefan cheng    </p>
* <p>Create Date：2021/9/22  9:30 </p>
* <p>Description：@todo describe         </p>
* <p>PackagePath: com.dyt.wcc.dytpir.ui.preview     </p>
*/
public class PreViewViewModel extends AndroidViewModel {

	private WeakReference<Context> mContext;
	private MutableLiveData<Integer> connectState  ;//连接状态 0 未连接（销毁/释放）， 1 已连接， 2 暂停连接

	public PreViewViewModel (@NonNull Application application) {
		super(application);
		mContext = new WeakReference<>(application);
	}


	public LiveData<Integer> getConnectState () {
		if (connectState == null){
			connectState = new MutableLiveData<>();
			connectState.setValue(0);
		}
		return connectState;
	}


	public void showPaletteWindow(View view){
		Toast.makeText(mContext.get(),"1111",Toast.LENGTH_SHORT).show();
	}

}
