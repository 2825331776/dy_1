package com.dyt.wcc.dytpir.ui;

import com.dyt.wcc.common.base.BaseApplication;

/**
* <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
* <p>Author：stefan cheng    </p>
* <p>Create Date：2021/9/26  16:18 </p>
* <p>Description：@todo describe         </p>
* <p>PackagePath: com.dyt.wcc.dytpir.ui     </p>
*/
public class DYTApplication extends BaseApplication {

	//	private AppViewModel appViewModel;//全局唯一ViewModel
	public  static DYTApplication instance;
	public static DYTApplication getInstance(){
		return instance;
	}

	@Override
	public void onCreate () {
		super.onCreate();
		instance = this;
	}
}
