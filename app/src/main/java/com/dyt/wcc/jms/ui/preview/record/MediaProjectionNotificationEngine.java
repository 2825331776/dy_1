package com.dyt.wcc.jms.ui.preview.record;

import android.app.Notification;

/**
 * 媒体投影通知引擎
 * Created by lishilin on 2020/03/19
 */
public interface MediaProjectionNotificationEngine {

	/**
	 * 获取 Notification
	 *
	 * @return Notification
	 */
	Notification getNotification ();

}
