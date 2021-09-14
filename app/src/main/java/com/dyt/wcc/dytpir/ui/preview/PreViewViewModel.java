package com.dyt.wcc.dytpir.ui.preview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/14  15:36     </p>
 * <p>Description：@todo         </p>
 * <p>PackgePath: com.dyt.wcc.dytpir.ui.preview     </p>
 */
public class PreViewViewModel extends ViewModel {
	private MutableLiveData<Integer> connectState  ;//连接状态 0 未连接（销毁/释放）， 1 已连接， 2 暂停连接



	public LiveData<Integer> getConnectState () {
		if (connectState == null){
			connectState = new MutableLiveData<>();
			connectState.setValue(0);
		}
		return connectState;
	}
}
