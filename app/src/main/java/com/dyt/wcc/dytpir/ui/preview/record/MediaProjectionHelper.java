package com.dyt.wcc.dytpir.ui.preview.record;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
/**
* <p>Copyright (C), 2021.04.01-? , DY Technology    </p>
* <p>Author：stefan cheng    </p>
* <p>Create Date：2021/11/1  14:01 </p>
* <p>Description：@todo describe         </p>
* <p>PackagePath: com.dyt.wcc.dytpir.ui.preview.record     </p>
*/
public class MediaProjectionHelper {

    public static final int REQUEST_CODE = 10086;
    private static final String TAG         = "ProjectionHelper" ;

    private static class InstanceHolder {
        private static final MediaProjectionHelper instance = new MediaProjectionHelper();
    }

    public static MediaProjectionHelper getInstance() {
        return InstanceHolder.instance;
    }

    private MediaProjectionHelper () {
        super();
    }

    private MediaProjectionNotificationEngine notificationEngine;

    private MediaProjectionManager mediaProjectionManager;
    private DisplayMetrics displayMetrics;

    private ServiceConnection      serviceConnection;
    private MediaProjectionService mediaProjectionService;

    private MediaRecorderCallback mediaRecorderCallback;

    private int Record_State = 0;

    public int getRecord_State () {
        return Record_State;
    }

    /**
     * 设置 通知引擎
     *
     * @param notificationEngine notificationEngine
     */
    public void setNotificationEngine(MediaProjectionNotificationEngine notificationEngine) {
        this.notificationEngine = notificationEngine;
    }

    /**
     * 启动媒体投影服务
     *
     * @param context activity
     */
    public void startService(Activity context) {
        if (mediaProjectionManager != null) {
            return;
        }
        // 此处宽高需要获取屏幕完整宽高，否则截屏图片会有白/黑边


        // 启动媒体投影服务
        mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null) {
            (context).startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }

        displayMetrics = new DisplayMetrics();
        (context).getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        // 绑定服务
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (service instanceof MediaProjectionService.MediaProjectionBinder) {
                    Log.e(TAG, "--------onServiceConnected: ----------------");
                    mediaProjectionService = ((MediaProjectionService.MediaProjectionBinder) service).getService();
                    mediaProjectionService.setNotificationEngine(notificationEngine);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mediaProjectionService = null;
            }
        };
        MediaProjectionService.bindService(context, serviceConnection);

        Record_State = 100;
        Log.e(TAG, "startService: ");
    }

    public void setMediaRecorderCallback (MediaRecorderCallback mediaRecorderCallback) {
        this.mediaRecorderCallback = mediaRecorderCallback;
    }

    /**
     * 停止媒体投影服务
     *
     * @param context context
     */
    public void stopService(Context context) {
        mediaProjectionService = null;

        if (serviceConnection != null) {
            MediaProjectionService.unbindService(context, serviceConnection);
            serviceConnection = null;
        }

        displayMetrics = null;

        mediaProjectionManager = null;

        Record_State = 0;
        Log.e(TAG, "stopService: ");
    }

    /**
     * 创建VirtualDisplay(onActivityResult中调用)
     *
     * @param requestCode           requestCode
     * @param resultCode            resultCode
     * @param data                  data
     * @param isScreenCaptureEnable 是否可以屏幕截图
     * @param isMediaRecorderEnable 是否可以媒体录制
     */
    public void createVirtualDisplay(int requestCode, int resultCode, Intent data, boolean isScreenCaptureEnable, boolean isMediaRecorderEnable) {
        if (mediaProjectionService == null) {
            return;
        }
        if (requestCode != REQUEST_CODE) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        mediaProjectionService.createVirtualDisplay(resultCode, data, displayMetrics, isScreenCaptureEnable, isMediaRecorderEnable);
        startMediaRecorder(mediaRecorderCallback);
        Log.e(TAG, "startService end ==================: ");
    }

    /**
     * 屏幕截图
     *
     * @param callback callback
     */
    public void capture(ScreenCaptureCallback callback) {
        if (mediaProjectionService == null) {
            callback.onFail();
            return;
        }
        mediaProjectionService.capture(callback);
    }

    /**
     * 开始 屏幕录制
     *
     * @param callback callback
     */
    public void startMediaRecorder(MediaRecorderCallback callback) {
        if (mediaProjectionService == null) {
            Log.e(TAG, "onError:  ===66=== ");
            callback.onFail();
            return;
        }
        mediaProjectionService.startRecording(callback);
    }

    /**
     * 停止 屏幕录制
     */
    public void stopMediaRecorder() {
        if (mediaProjectionService == null) {
            return;
        }
        mediaProjectionService.stopRecording();
    }

}
