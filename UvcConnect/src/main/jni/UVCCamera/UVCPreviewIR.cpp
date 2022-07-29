#include <stdlib.h>
#include <linux/time.h>
#include <unistd.h>
#include <math.h>


#include <iostream>
#include <fstream>

using namespace std;

#if 1    // set 1 if you don't need debug log
#ifndef LOG_NDEBUG
#define    LOG_NDEBUG        // w/o LOGV/LOGD/MARK
#endif
#undef USE_LOGALL
#else
#define USE_LOGALL
#undef LOG_NDEBUG
//	#undef NDEBUG
#endif

#include "utilbase.h"
#include "UVCPreviewIR.h"
#include "libuvc_internal.h"

#include <vector>

#define    LOCAL_DEBUG 1    // set 1 if you don't need debug log
#define MAX_FRAME 4
#define PREVIEW_PIXEL_BYTES 4    // RGBA/RGBX
#define FRAME_POOL_SZ MAX_FRAME
//切换数据时需要修改这个
#define OUTPUTMODE 4
//#define OUTPUTMODE 5


UVCPreviewIR::UVCPreviewIR(uvc_device_handle_t *devh, FrameImage *frameImage) {
    LOGE("构造函数步骤5");
    mPreviewWindow = NULL;
//             mCaptureWindow(NULL),//可以没有
    mDeviceHandle = devh;
    requestWidth = DEFAULT_PREVIEW_WIDTH;
    requestHeight = DEFAULT_PREVIEW_HEIGHT;
    requestMinFps = DEFAULT_PREVIEW_FPS_MIN;
    requestMaxFps = DEFAULT_PREVIEW_FPS_MAX;
    requestMode = DEFAULT_PREVIEW_MODE;
    requestBandwidth = DEFAULT_BANDWIDTH;
    frameWidth = DEFAULT_PREVIEW_WIDTH;
    frameHeight = DEFAULT_PREVIEW_HEIGHT;
    frameBytes = DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * 2;    // YUYV
    frameMode = 0;
    previewBytes = DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * PREVIEW_PIXEL_BYTES;
    previewFormat = WINDOW_FORMAT_RGBX_8888;
    mIsRunning = false;
    mIsTemperaturing = false;
    mIsCapturing = false;

    mFrameImage = frameImage;

    mIsComputed = true;
    mIsCopyPicture = false;
    OutPixelFormat = 3;
    mTypeOfPalette = 1;
    is_first_run = true;


    pthread_cond_init(&preview_sync, NULL);
    pthread_mutex_init(&preview_mutex, NULL);
//
    pthread_cond_init(&screenShot_sync, NULL);
    pthread_mutex_init(&screenShot_mutex, NULL);

    pthread_cond_init(&temperature_sync, NULL);
    pthread_mutex_init(&temperature_mutex, NULL);//初始化温度线程对象

    pthread_mutex_init(&data_callback_mutex, NULL);

//    pthread_cond_init(&tinyC_send_order_sync, NULL);
    pthread_mutex_init(&tinyC_send_order_mutex, NULL);

    EXIT();

}

UVCPreviewIR::~UVCPreviewIR() {
    ENTER();
    if (mDeviceHandle) {
        uvc_stop_streaming(mDeviceHandle);
//        SAFE_DELETE(mDeviceHandle)
    }
    is_first_run = true;
    LOGE("====1=====");
    mDeviceHandle = NULL;
    if (mPreviewWindow)
        ANativeWindow_release(mPreviewWindow);
    mPreviewWindow = NULL;
    LOGE("====2=====");
    SAFE_DELETE(mFrameImage);
    LOGE("====3=====");
//    mFrameImage = NULL;
    pthread_mutex_destroy(&preview_mutex);
    pthread_cond_destroy(&preview_sync);
    pthread_mutex_destroy(&screenShot_mutex);
    pthread_cond_destroy(&screenShot_sync);
    pthread_mutex_destroy(&temperature_mutex);//析构函数内释放内存
    pthread_cond_destroy(&temperature_sync);

    pthread_mutex_destroy(&data_callback_mutex);

//    pthread_cond_destroy(&tinyC_send_order_sync);
    pthread_mutex_destroy(&tinyC_send_order_mutex);
    LOGE("====4=====");
    EXIT();
}


/************************************绘制  预览***************************************************/
/***************************************************************************************/
inline const bool UVCPreviewIR::isRunning() const { return mIsRunning; }

inline const bool UVCPreviewIR::isComputed() const { return mIsComputed; }

inline const bool UVCPreviewIR::isCopyPicturing() const { return mIsCopyPicture; }

inline const bool UVCPreviewIR::isVerifySN() const { return mIsVerifySn; }

inline const bool UVCPreviewIR::isSnRight() const { return snIsRight; }

//此处会调用两次，第一次：初始化camera时：宽高是默认值；第二次（startPreview设置调用到）是传入了新的宽高，此时camera是已经初始化open。
int UVCPreviewIR::setPreviewSize(int width, int height, int min_fps, int max_fps, int mode,
                                 float bandwidth, int currentAndroidVersion) {
    ENTER();
//    LOGE("=========>requestWidth  == %d,width  == %d,requestHeight  == %d,height  == %d,requestMode  == %d, mode == %d",
//         requestWidth, width, requestHeight, height, requestMode, mode);
    //LOGE("setPreviewSize");
//    LOGE("=========== %d==============bandwidth========%f==========", width, bandwidth);
    LOGE("setPreviewSize步骤6");
    int result = 0;
    if ((requestWidth != width) || (requestHeight != height) || (requestMode != mode)) {
//        mFrameImage添加一个函数初始化绘制需要的值
//        LOGE("requestWidth  == %d,width  == %d,requestHeight  == %d,height  == %d,requestMode  == %d, mode == %d",
//             requestWidth, width, requestHeight, height, requestMode, mode);
        mFrameImage->setPreviewSize(width, height, mode);
        requestWidth = width;
        requestHeight = height;
        requestMinFps = min_fps;
        requestMaxFps = max_fps;
        requestMode = 0;
        requestBandwidth = bandwidth;
        uvc_stream_ctrl_t ctrl;

//        LOGE("uvc_get_stream_ctrl_format_size_fps ==  ===2222222222");
        result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, &ctrl,
                                                     !requestMode ? UVC_FRAME_FORMAT_YUYV
                                                                  : UVC_FRAME_FORMAT_MJPEG,
                                                     requestWidth, requestHeight, requestMinFps,
                                                     requestMaxFps);
    }
//    LOGE("setPreviewSize==========================over");
    mCurrentAndroidVersion = currentAndroidVersion;
    RETURN(result, int);
}

int UVCPreviewIR::setPreviewDisplay(ANativeWindow *preview_window) {
    ENTER();
    //LOGE("setPreviewDisplay");
    LOGE("setPreviewDisplay步骤7");
    pthread_mutex_lock(&preview_mutex);
    {
        if (mPreviewWindow != preview_window) {
            if (mPreviewWindow)
                ANativeWindow_release(mPreviewWindow);
            mPreviewWindow = preview_window;
            if (LIKELY(mPreviewWindow)) {
                /**
                 * 更改窗口缓冲区的格式和大小。宽度和高度控制缓冲区中像素的数量，
                 * 而不是屏幕上窗口的尺寸。 如果这些与窗口的物理大小不同，
                 * 则在将其合成到屏幕时将缩放其缓冲区以匹配该大小。 宽度和高度必须均为零或均为非零，
                 * 对于所有这些参数，如果提供0，则窗口的基值将恢复生效
                 */
                //S0机芯
                if (mPid == 1 && mVid == 5396) {
                    ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                     requestWidth, requestHeight - 4,
                                                     previewFormat);
                } else if (mPid == 22592 && mVid == 3034) {
                    int status = ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                                  requestWidth, requestHeight,
                                                                  previewFormat);
                    if (status) {
                        LOGE("=============设置失败====================");
                    } else {
                        LOGE("=============设置成功====================");
                    }
                }
            }
        }
    }
    pthread_mutex_unlock(&preview_mutex);
    RETURN(0, int);
}

int UVCPreviewIR::startPreview() {
    ENTER();
//LOGE("startPreview");
    int result = EXIT_FAILURE;
    if (!isRunning()) {
        mIsRunning = true;
        //pthread_mutex_lock(&preview_mutex);
        result = pthread_create(&preview_thread, NULL, preview_thread_func, (void *) this);
//        pthread_create(&tinyC_send_order_thread, NULL, tinyC_sendOrder_thread_func, (void *) this);
        ////LOGE("STARTPREVIEW RESULT1:%d",result);
        //}
        //	pthread_mutex_unlock(&preview_mutex);
        if (UNLIKELY(result != EXIT_SUCCESS)) {
            LOGE("UVCCamera::window does not exist/already running/could not create thread etc.");
            mIsRunning = false;
            pthread_mutex_lock(&preview_mutex);
            {
                pthread_cond_signal(&preview_sync);
            }
            pthread_mutex_unlock(&preview_mutex);
        }
    }
    ////LOGE("STARTPREVIEW RESULT2:%d",result);
    RETURN(result, int);
}

void UVCPreviewIR::setVidPid(int vid, int pid) {
    mVid = vid;
    mPid = pid;

}

int UVCPreviewIR::setIsVerifySn() {
    int result = EXIT_FAILURE;
    mIsVerifySn = true;
    result = EXIT_SUCCESS;
    RETURN(result, int);
}

//int UVCPreviewIR::tinyCControl() {
//    int result = UVC_ERROR_IO;
//    for (; LIKELY(isRunning());) {
//        pthread_mutex_lock(&tinyC_send_order_mutex);
//        pthread_cond_wait(&tinyC_send_order_sync, &tinyC_send_order_mutex);
//
//        result = doTinyCOrder();
//
//        pthread_mutex_unlock(&tinyC_send_order_mutex);
//    }
//    RETURN(result, int);
//}

int UVCPreviewIR::doTinyCOrder() {
//    uvc_diy_communicate(mDeviceHandle,0x41,0x45,0x0078,0x1d00,tinyC_data, sizeof(tinyC_data),1000);
    int ret = UVC_ERROR_IO;
    if (tinyC_mark == 20) {//读取用户区SN
        LOGE("==============user mark ==20 ===读取用户区SN====================");
        // 读取用户区域
        int dataLen = 15; //获取或读取数据大小
        unsigned char *readData = (unsigned char *) tinyC_params;
        int dwStartAddr = 0x7FF000;// 用户区域首地址
        unsigned char data[8] = {0};
        data[0] = 0x01;
        data[1] = 0x82;
        data[2] = ((dwStartAddr & 0xff000000) >> 24);
        data[3] = ((dwStartAddr & 0x00ff0000) >> 16);
        data[4] = ((dwStartAddr & 0x0000ff00) >> 8);
        data[5] = (dwStartAddr & 0x000000ff);
        data[6] = (dataLen >> 8);
        data[7] = (dataLen & 0xff);
        ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00, data, sizeof(data),
                                  1000);
        unsigned char status;
        for (int index = 0; index < 1000; index++) {
            uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x0200, &status, 1, 1000);
            if ((status & 0x01) == 0x00) {
                if ((status & 0x02) == 0x00) {
                    break;
                } else if ((status & 0xFC) != 0x00) {
                    RETURN(-1, int);
                }
            }
        }
        ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d08, readData, 15, 1000);
        LOGE("==========read Data ================= %s", readData);
        readData = NULL;
    } else if (tinyC_mark == 21) {//读取 机器SN
        LOGE("==============tinyC_mark ==21 ===读取机器SN====================");
        unsigned char *flashId = (unsigned char *) tinyC_params;
        unsigned char data2[8] = {0};
        data2[0] = 0x05;
        data2[1] = 0x84;
        data2[2] = 0x07;
        data2[3] = 0x00;
        data2[4] = 0x00;
        data2[5] = 0x10;
        data2[6] = 0x00;
        data2[7] = 0x10;
        ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00, data2, sizeof(data2),
                                  1000);
        unsigned char status1;
        for (int index = 0; index < 1000; index++) {
            uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x0200, &status1, 1, 1000);
            if ((status1 & 0x01) == 0x00) {
                if ((status1 & 0x02) == 0x00) {
                    break;
                } else if ((status1 & 0xFC) != 0x00) {
                    LOGE("=====读取Sn ====RETURN================= ");
                    RETURN(-1, int);
                }
            }
        }
        ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d08, flashId, 15, 1000);
        LOGE("==========flashId================= %s", flashId);
        flashId = NULL;
    }
    RETURN(ret, int);
}

/**
 *
 * @param params
 * @param func_tinyc
 * @param mark
 * @return
 */
int UVCPreviewIR::sendTinyCAllOrder(void *params, diy func_tinyc, int mark) {
    int ret = UVC_ERROR_IO;
//    LOGE("=======sendTinyCAllOrder === lock==Thread id = %d===== mark = %d==" , gettid() , tinyC_mark);
//    LOGE("=======mark === >%d=========" , mark);
    if (mark == 10) {//获去tinyc机芯参数列表
        pthread_mutex_lock(&tinyC_send_order_mutex);
        ret = getTinyCParams(params, func_tinyc);
        pthread_mutex_unlock(&tinyC_send_order_mutex);
    } else if (mark == 100) {//纯指令， 打挡  返回AD流
        LOGE("=========mark == 100======= === %d=======", *((int *) params));
        pthread_mutex_lock(&tinyC_send_order_mutex);
        ret = sendTinyCOrder((uint32_t *) params, func_tinyc);
        pthread_mutex_unlock(&tinyC_send_order_mutex);
    } else if (mark > 0 && mark < 10) {//设置机芯参数
        pthread_mutex_lock(&tinyC_send_order_mutex);
        ret = sendTinyCParamsModification((float *) (params), func_tinyc, mark);
        pthread_mutex_unlock(&tinyC_send_order_mutex);
    } else if (mark == 20 || mark == 21) {
        pthread_mutex_lock(&tinyC_send_order_mutex);
        LOGE("=========mark == 20 || mark == 21=======");
        ret = getTinyCUserData(params, func_tinyc, mark);
        pthread_mutex_unlock(&tinyC_send_order_mutex);
    }
    LOGE("=======sendTinyCAllOrder === ret =====**** = %d====", ret);
//    LOGE("=======sendTinyCAllOrder === unlock=Thread id ===%d=== mark = %d====" , gettid(), mark);
    RETURN(ret, int);
}

/**
 * 检查sn号是否是对的，对的代表出图了
 * @return
 */
bool UVCPreviewIR::snRightIsPreviewing() {
    return isSnRight();
}

/**
 * 2022年5月17日16:46:17 设置TinyC增益模式：低增益对应最高200℃ 高增益最高599.9℃
 * @param value 1低增益 0高增益
 * @param mark  标记位
 * @return  是否设置成功
 */
bool UVCPreviewIR::setMachineSetting(int value, int mark) {
    LOGE("============setMachineSetting============value ====>%d", value);
    bool result = false;
    unsigned char data_set_gain[8] = {0x14, 0xc5, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00};
    unsigned char data_get_gain[8] = {0x14, 0x85, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00};


    unsigned char flashId_get[2];
    flashId_get[1] = 0x5;
    flashId_get[0] = 0x5;
    int machine_gain_get = flashId_get[1];//从机芯读取的 高低温增益模式。0代表低增益，高温度。1代表高增益，低温度。
    unsigned char data_gain[8] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};


    while (sendCount < 50 && (value != machine_gain_get) && !result && LIKELY(mDeviceHandle)) {
        if (mark == -1) {
            uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data_get_gain,
                                sizeof(data_get_gain), 1000);
            uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data_gain,
                                sizeof(data_gain),
                                1000);
            //获取机芯的 高低温增益模式
            uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d10, flashId_get,
                                sizeof(flashId_get),
                                1000);
            machine_gain_get = flashId_get[1];
            LOGE("============获取TinyC 机芯的 高低温增益模式。=machine_gain_get=%d==flashId_get[1]=====",
                 machine_gain_get);
            //如果设置的值 与 增益不匹配，切换
            if (machine_gain_get != value) {
                data_set_gain[6] = (value >> 8);
                data_set_gain[7] = (value & 0xff);
                uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data_set_gain,
                                    sizeof(data_set_gain),
                                    1000);
                uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data_gain,
                                    sizeof(data_gain),
                                    1000);
            } else {
                LOGE("============设置机芯高低温增益成功==============");
                return true;
            }
            sendCount++;
        } else {
            if (getTinyCDevicesStatus()) {
                uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data_get_gain,
                                    sizeof(data_get_gain), 1000);
                uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data_gain,
                                    sizeof(data_gain),
                                    1000);
                //获取机芯的 高低温增益模式

                uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d10, flashId_get,
                                    sizeof(flashId_get),
                                    1000);
                machine_gain_get = flashId_get[1];
                LOGE("============获取TinyC 机芯的 高低温增益模式。=machine_gain_get=%d==flashId_get[1]=====",
                     machine_gain_get);

                //如果设置的值 与 增益不匹配，切换
                if (machine_gain_get != value) {
                    data_set_gain[6] = (value >> 8);
                    data_set_gain[7] = (value & 0xff);
                    uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data_set_gain,
                                        sizeof(data_set_gain),
                                        1000);
                    uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data_gain,
                                        sizeof(data_gain),
                                        1000);
                } else {
                    LOGE("============设置机芯高低温增益成功==============");
                    return true;
                }
                sendCount++;
            }
        }
    }
    sendCount = 0;
    return result;
}

//2022年5月17日16:46:17 设置机芯参数
float UVCPreviewIR::getMachineSetting(int flag, int value,
                                      int mark) {
    int ret = -1;
    float result = 0;
    //
    unsigned char flashId[2] = {0};
    unsigned char data[8] = {0x14, 0x85, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00};
    unsigned char data2[8] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data, sizeof(data), 1000);
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data2, sizeof(data2),
                              1000);
    if (getTinyCDevicesStatus()) {
        if (LIKELY(mDeviceHandle)) {
            ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d10, flashId,
                                      sizeof(flashId),
                                      1000);
            unsigned char flashId2[2] = {0};
            LOGE("getMachineSetting =====flashId== %d", *flashId);
            flashId2[0] = flashId[1];
            flashId2[1] = flashId[0];
            unsigned short *dd = (unsigned short *) flashId2;
            LOGE("getMachineSetting =====dd== %d", *dd);
            dd = NULL;
            LOGE("getMachineSetting 0 ==== %d", flashId[0]);
            LOGE("getMachineSetting 1 ==== %d", flashId[1]);
        }
    }
    return result;
}

void UVCPreviewIR::setRotateMatrix_180(bool isRotate) {
    isRotateMatrix_180 = isRotate;
}

int UVCPreviewIR::sendTinyCOrder(uint32_t *value, diy func_diy) {
    int ret = UVC_ERROR_IO;

    if (*value == 32772) {//画面设置指令 0X8004 , 32773 == 0X8005
        //输出温度数据(AD值)
        unsigned char data[8] = {0x0a, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        if (LIKELY(mDeviceHandle)) {
            ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078,
                                      0x1d00, data, sizeof(data), 1000);
        }
    }
    if (*value == 32768) {//打挡指令 0X8000
        LOGE("=======32768==========");
        unsigned char data[8] = {0x0d, 0xc1, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        {
            ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078,
                                      0x1d00, data, sizeof(data), 1000);
        }
    }
    RETURN(ret, int);
}

int UVCPreviewIR::sendTinyCParamsModification(float *value, diy func_diy, uint32_t mark) {
    int ret = UVC_ERROR_IO;

    unsigned char data[8] = {0x14, 0xc5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    unsigned char data2[8] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
    data[0] = 0x14;
    data[1] = 0xc5;
    data[2] = 0x00;
    data[3] = 0x01; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段；0x03 发射率
    data[4] = 0x00;
    data[5] = 0x00;
//    setIsVerifySn();
    if (mark == 1) {         //反射温度  ， 温度转 K 华氏度
        LOGE("=================== 反射温度===============%f======", (*value));
        int val = (int) (*value + 273.15f);
        data[3] = 0x01; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段
        data[6] = (val >> 8);
        data[7] = (val & 0xff);
    }
    if (mark == 2) {         //大气温度
        LOGE("=================== 大气温度===============%f======", (*value));
        int val = (int) (*value + 273.15f);
        data[3] = 0x02; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段
        data[6] = (val >> 8);
        data[7] = (val & 0xff);
    }
    if (mark == 3) {         //发射率
        LOGE("=================== 发射率===============%f======", (*value));
        int val = (int) round(*value * 128);
        data[3] = 0x03; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段
        data[6] = (val >> 8);
        data[7] = (val & 0xff);

    }
    if (mark == 4) {         //大气透过率
        LOGE("=================== 大气透过率===============%f======", (*value));
        int val = (int) round(*value * 128);
        data[3] = 0x04; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段
        data[6] = (val >> 8);
        data[7] = (val & 0xff);
    }

    if (data[3] != 0x00) {//保证修改的 值有数据
        LOGE("========== 修改设置前 ========= ret === %d ==================", ret);
        ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data, sizeof(data),
                                  1000);
        ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data2, sizeof(data2),
                                  1000);
        LOGE("========== 修改设置后 ========= ret === %d ==================", ret);

    }
    RETURN(ret, int);
}

//20 读取 用户区SN ， 21 读取机器Sn
int UVCPreviewIR::getTinyCUserData(void *returnData, diy func_diy, int userMark) {
    int ret = UVC_ERROR_IO;
    //读取用户区数据
    unsigned char data[8] = {0x0d, 0xc1, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    if (userMark == 20) {//读取用户区SN
        int dataLen = 15; //读取数据长度
        unsigned char *readData = (unsigned char *) returnData;
        int dwStartAddr = 0x7FF000;// 用户区域首地址

        data[0] = 0x01;
        data[1] = 0x82;
        data[2] = ((dwStartAddr & 0xff000000) >> 24);
        data[3] = ((dwStartAddr & 0x00ff0000) >> 16);
        data[4] = ((dwStartAddr & 0x0000ff00) >> 8);
        data[5] = (dwStartAddr & 0x000000ff);
        data[6] = (dataLen >> 8);
        data[7] = (dataLen & 0xff);
        ret = func_diy(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00, data, sizeof(data), 1000);
        unsigned char status;
        for (int index = 0; index < 1000; index++) {
            func_diy(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x0200, &status, 1, 1000);
            if ((status & 0x01) == 0x00) {
                if ((status & 0x02) == 0x00) {
                    break;
                } else if ((status & 0xFC) != 0x00) {
                    RETURN(-1, int);
//                    snIsRight = false;
                }
            }
        }
        ret = func_diy(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d08, readData, 15, 1000);
        LOGE("==========read Data ================= %s", readData);
        readData = NULL;
    } else if (userMark == 21) {//读取 机器SN
        unsigned char *flashId = (unsigned char *) returnData;
        returnData = flashId;
        data[0] = 0x05;
        data[1] = 0x84;
        data[2] = 0x07;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x10;
        data[6] = 0x00;
        data[7] = 0x10;
        ret = func_diy(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00, data, sizeof(data), 1000);
        unsigned char status1;
        for (int index = 0; index < 1000; index++) {
            func_diy(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x0200, &status1, 1, 1000);
            if ((status1 & 0x01) == 0x00) {
                if ((status1 & 0x02) == 0x00) {
                    break;
                } else if ((status1 & 0xFC) != 0x00) {
                    RETURN(-1, int);
                    LOGE("=====读取Sn ====RETURN================= ");
                }
            }
        }
        ret = func_diy(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d08, flashId, 15, 1000);
        flashId = NULL;
    }

    RETURN(ret, int);
}

int UVCPreviewIR::getTinyCDevicesStatus() {
    int ret = 0;
    unsigned char status = 0;
    for (int index = 0; index < 1000; index++) {
        if (LIKELY(mDeviceHandle)) {
            uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x0200, &status, 1, 1000);
            if ((status & 0x01) == 0x00) {
                if ((status & 0x02) == 0x00) {
                    ret = 1;
                    break;
                } else if ((status & 0xFC) != 0x00) {
//                RETURN(ret, int)
                }
            }
        }
    }
    LOGE("===========ret ======%d", ret);
    RETURN(ret, int)
}

int UVCPreviewIR::getTinyCParams(void *rdata, diy func_diy) {
    LOGE("========================================");
    int ret = 0;
    unsigned char *backData = (unsigned char *) rdata;
    // 发射率
    unsigned char flashId[2] = {0};
    unsigned char reviewData[2] = {0};//复查View
    unsigned char data[8] = {0x14, 0x85, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00};
    unsigned char data2[8] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data, sizeof(data), 1000);
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data2, sizeof(data2),
                              1000);
    if (getTinyCDevicesStatus()) {
        ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d10, flashId,
                                  sizeof(flashId),
                                  1000);
        *backData = flashId[0];
        backData++;
        *backData = flashId[1];
        unsigned char flashId2[2] = {0};
        flashId2[0] = flashId[1];
        flashId2[1] = flashId[0];
        unsigned short *dd = (unsigned short *) flashId2;
        LOGE("发射率 ======= %d", *dd);
        dd = NULL;
        LOGE("发射率 0 ==== %d", flashId[0]);
        LOGE("发射率 1 ==== %d", flashId[1]);
    }

    // 获取 反射温度（已成功）
    data[3] = 0x01;
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data, sizeof(data), 1000);
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data2, sizeof(data2),
                              1000);
    if (getTinyCDevicesStatus()) {
        ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d10, flashId,
                                  sizeof(flashId),
                                  1000);
        backData++;
        *backData = flashId[0];
        backData++;
        *backData = flashId[1];
        LOGE("反射温度 0 ==== %d", flashId[0]);
        LOGE("反射温度 1 ==== %d", flashId[1]);
    }

    // 获取 大气温度（已成功）
    data[3] = 0x02;
//    status = 0;
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data, sizeof(data), 1000);
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data2, sizeof(data2),
                              1000);
    if (getTinyCDevicesStatus()) {
        ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d10, flashId,
                                  sizeof(flashId),
                                  1000);
        backData++;
        *backData = flashId[0];
        backData++;
        *backData = flashId[1];
        LOGE("大气温度 0 ==== %d", flashId[0]);
        LOGE("大气温度 1 ==== %d", flashId[1]);
    }
    backData = NULL;
    RETURN(ret, int);
}

int UVCPreviewIR::getTinyCParams_impl(void *reData, diy func_diy, unsigned char *data,
                                      unsigned char *data2) {
    unsigned char *backData = (unsigned char *) reData;
    int request_count = 0;//请求计数器
    int right_count = 0;//正确次数计数器
    unsigned char currentData[2] = {0};
    unsigned char oldData[2] = {0};
    bool isRight = false;
    unsigned char finalData[2] = {0};

    int ret = 0;
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x9d00, data, sizeof(data), 1000);
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d08, data2, sizeof(data2),
                              1000);
    if (request_count < 2) {
        if (getTinyCDevicesStatus()) {
            if (request_count == 0) {
                ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d10, currentData,
                                          sizeof(currentData),
                                          1000);
            }
            if (request_count == 1) {
                ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d10, oldData,
                                          sizeof(oldData),
                                          1000);
            }
        }
        request_count++;
    }

    if (currentData[0] == oldData[0] && currentData[1] == oldData[1]) {
        isRight = true;
        finalData[0] = currentData[0];
        finalData[1] = currentData[1];
    }
    if (isRight) {
        *backData = finalData[0];
        backData++;
        *backData = finalData[1];
        backData++;
    } else {
        *backData = 0;
        backData++;
        *backData = 0;
        backData++;
    }
    backData = NULL;
    RETURN(ret, int);
}

int UVCPreviewIR::stopPreview() {
    ENTER();
    bool b = isRunning();
    if (LIKELY(b)) {
        mIsCapturing = false;
        mIsRunning = false;
//        clearDisplay();
//        pthread_cond_signal(&capture_sync);

//        pthread_cond_signal(&tinyC_send_order_sync);
////        int *result_preview_join = NULL;
//        if (pthread_join(tinyC_send_order_thread, NULL) != EXIT_SUCCESS) {
//            LOGE("UVCPreviewIR::stopPreview tinyC_send_order_thread: EXIT_failed");
//        } else {
////            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_SUCCESS result =  %d" ,*result_preview_join);
//            LOGE("UVCPreviewIR::stopPreview tinyC_send_order_thread: EXIT_SUCCESS");
//        }
        pthread_cond_signal(&preview_sync);
//        int *result_preview_join = NULL;
        if (pthread_join(preview_thread, NULL) != EXIT_SUCCESS) {
//            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_failed  result =  %d" ,*result_preview_join);
            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_failed");
        } else {
//            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_SUCCESS result =  %d" ,*result_preview_join);
            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_SUCCESS");
        }
        if (mIsTemperaturing) {
            mIsTemperaturing = false;
            if (pthread_join(temperature_thread, NULL) != EXIT_SUCCESS) {
                LOGE("UVCPreviewIR::stopPreview temperature_thread: pthread_join failed");
            } else {
                LOGE("UVCPreviewIR::stopPreview temperature_thread: pthread_join success");
            }
        }
//        result_preview_join = NULL;
    }

    pthread_mutex_lock(&preview_mutex);
    if (mPreviewWindow) {
        ANativeWindow_release(mPreviewWindow);
        mPreviewWindow = NULL;
    }
    pthread_mutex_unlock(&preview_mutex);

    tinyC_params = NULL;

    LOGE("UVCPreviewIR::stopPreview ============== delete begin ");
    if (OutBuffer != NULL) {
        delete[] OutBuffer;
    }
    if (HoldBuffer != NULL) {
        delete[] HoldBuffer;
    }
    if (RgbaOutBuffer != NULL) {
        delete[] RgbaOutBuffer;
    }
    if (RgbaHoldBuffer != NULL) {
        delete[] RgbaHoldBuffer;
    }
    if (picOutBuffer != NULL) {
        delete[] picOutBuffer;
    }
    if (backUpBuffer != NULL) {
        delete[] backUpBuffer;
    }
//    LOGE("UVCPreviewIR::stopPreview ============== delete over ");
    RETURN(0, int);
}

//线程 preview_thread
void *UVCPreviewIR::preview_thread_func(void *vptr_args) {
    int result;
    ENTER();
    UVCPreviewIR *preview = reinterpret_cast<UVCPreviewIR *>(vptr_args);
    if (LIKELY(preview)) {
        uvc_stream_ctrl_t ctrl;
        result = preview->prepare_preview(&ctrl);
        if (LIKELY(!result)) {
            preview->do_preview(&ctrl);
        }
    }
    PRE_EXIT();
    pthread_exit(NULL);
}

//void *UVCPreviewIR::tinyC_sendOrder_thread_func(void *vptr_args) {
//    int result;
//    ENTER();
//    UVCPreviewIR *preview = reinterpret_cast<UVCPreviewIR *>(vptr_args);
//    if (LIKELY(preview)) {
//        result = preview->tinyCControl();
//    }
//    PRE_EXIT();
//    pthread_exit(NULL);
//}

/**
 *
 * @param ctrl
 * @return
 */
int UVCPreviewIR::prepare_preview(uvc_stream_ctrl_t *ctrl) {
//LOGE("prepare_preview");
    uvc_error_t result;
    ENTER();
    //此处初始化要mode判断 生成
//    CacheBuffer = new unsigned char[requestWidth*(requestHeight)*2];
    OutBuffer = new unsigned char[requestWidth * (requestHeight) *
                                  2];//分配内存：要*2 在draw_preview_one中会赋值给 unsigned short 类型
    HoldBuffer = new unsigned char[requestWidth * (requestHeight) * 2];
    if (mPid == 1 && mVid == 5396) {
        RgbaOutBuffer = new unsigned char[requestWidth * (requestHeight - 4) * 4];
        RgbaHoldBuffer = new unsigned char[requestWidth * (requestHeight - 4) * 4];
    } else if (mPid == 22592 && mVid == 3034) {
        LOGE("================init=2222====requestWidth==%d=====requestHeight==%d========",
             requestWidth, requestHeight);
        RgbaOutBuffer = new unsigned char[requestWidth * (requestHeight) * 4];
        RgbaHoldBuffer = new unsigned char[requestWidth * (requestHeight) * 4];
    }
    backUpBuffer = new unsigned char[requestWidth * (requestHeight) * 4];
    picOutBuffer = new unsigned char[requestWidth * (requestHeight) * 2];
//    picRgbaOutBuffer=new unsigned char[requestWidth*(requestHeight-4)*4];

    mCurrentAndroidVersion = 0;

//    LOGE("uvc_get_stream_ctrl_format_size_fps1111111111111");
    result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, ctrl,
                                                 !requestMode ? UVC_FRAME_FORMAT_YUYV
                                                              : UVC_FRAME_FORMAT_MJPEG,
                                                 requestWidth, requestHeight, requestMinFps,
                                                 requestMaxFps);
//    LOGE("result：%d",result);
    //LOGE("re:%d,frameSize=(%d,%d)@%d,%d",result, requestWidth, requestHeight, requestMinFps,requestMaxFps);
    if (LIKELY(!result)) {
#if LOCAL_DEBUG
        uvc_print_stream_ctrl(ctrl, stderr);
#endif
        uvc_frame_desc_t *frame_desc;
        result = uvc_get_frame_desc(mDeviceHandle, ctrl, &frame_desc);
        if (LIKELY(!result)) {
            frameWidth = frame_desc->wWidth;
            frameHeight = frame_desc->wHeight;
            LOGE("==========frameSize=(%d,%d)@%s========", frameWidth, frameHeight,
                 (!requestMode ? "YUYV" : "MJPEG"));//frameSize=(256,196)@YUYV
//            pthread_mutex_lock(&preview_mutex);
//            if (LIKELY(mPreviewWindow)) {
            if (mPid == 1 && mVid == 5396) {
                ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                 frameWidth, frameHeight - 4,
                                                 previewFormat);//ir软件256*196中，实质256*192图像数据，4行其他数据
            } else if (mPid == 22592 && mVid == 3034) {
                ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                 frameWidth, frameHeight, previewFormat);
//                    ANativeWindow_setBuffersGeometry(mPreviewWindow,
//                                                     frameWidth, frameHeight,
//                                                     previewFormat);//ir软件256*192 160*120中，实质256*192 160*120图像数据
            }
//                LOGE("=======UVCPreviewIR::prepare_preview======getWidth===%d,======getHeight===%d====",ANativeWindow_getWidth(mPreviewWindow),ANativeWindow_getHeight(mPreviewWindow));
//                //LOGE("ANativeWindow_setBuffersGeometry:(%d,%d)", frameWidth, frameHeight);
//            }
//            pthread_mutex_unlock(&preview_mutex);
        } else {
            frameWidth = requestWidth;
            frameHeight = requestHeight;
        }
        frameMode = requestMode;
        frameBytes = frameWidth * frameHeight * 2;
        LOGE(" frameBytes ==== %d  , requestMode ========  %d", frameBytes, requestMode);
        previewBytes = frameWidth * frameHeight * PREVIEW_PIXEL_BYTES;
    } else {
        LOGE("could not negotiate with camera:err=%d", result);
    }
    RETURN(result, int);
}

/**
 * 替换函数
 * @param base
 * @param baseLength
 * @param src
 * @param dst
 * @return
 */
void *charReplace(void *base,
                  int baseLength, unsigned char src, unsigned char dst) {
    unsigned char *base_p = (unsigned char *) base;
    for (int i = 0; i < baseLength; i++) {
//        if (())
    }
    return base;
}


string replace(string &base, string src, string dst) {
//    LOGE(" DecryptSN =====步骤十一 ===== >");
    int pos = 0, srclen = src.size(), dstlen = dst.size();
    while ((pos = base.find(src, pos)) &&
           pos != string::npos) //不能写成 ((pos = base.find(src, pos))!= string::npos)
    {
        base.replace(pos, srclen, dst);
        pos += dstlen;
    }
    return base;
}

//字符串分割函数
std::vector<std::string> split(std::string str, std::string pattern) {
    std::string::size_type pos;
    std::vector<std::string> result;
    str += pattern;//扩展字符串以方便操作
    int size = str.size();
    for (int i = 0; i < size; i++) {
        pos = str.find(pattern, i);
        if (pos < size) {
            std::string s = str.substr(i, pos - i);
            result.push_back(s);
            i = pos + pattern.size() - 1;
        }
    }
    return result;
}

// 解密用户区 SN
/*
 * @param userSn  用户区SN数组 指针数组
 * @param robotSn  机器SN后六位 指针数组
 * @param returnData 返回的 指针数组
 * @return
 */
void *UVCPreviewIR::DecryptSN(void *userSn, void *robotSn, void *returnData) {
    unsigned char *sn = (unsigned char *) userSn;
    unsigned char *ir_sn = (unsigned char *) robotSn;
//    LOGE("==========用户区：==sn ====> %s", sn);
//    LOGE("==========机  器：===ir_sn ====> %s", ir_sn);
//    for (int i = 0; i < 15; i++) {
//        if (*ir_sn == '\0'){
//            *ir_sn = '0';
//        }
//        ir_sn++;
//    }
    unsigned char *ir_sn_h = (unsigned char *) robotSn + 2;
//    unsigned char* ir_sn_24 = new unsigned char[4];
//    mempcpy(ir_sn_24,ir_sn_h,4);
//    LOGE("=============ir_sn_h ====> %s", ir_sn_h);
//    LOGE("=============ir_sn_24 ====> %s",ir_sn_24);

//    int  sn_sum = atoi((char *)ir_sn_24)%127;
    int sn_sum = atoi((char *) ir_sn_h) % 127;
//    LOGE("======sn_sum========》%d", sn_sum);
    char strs[15];
//    for (int i = 0; i < strlen(sn); i++) {
//        LOGE(" >>>DecryptSN sn ======= > %d",(int)sn[i]);
//    }
    mempcpy(strs, sn, 15);
//    int strs_len = strlen(strs);
    int strs_len = 15;
//    LOGE(" DecryptSN =====步骤四222 ===== > %d", strs_len);
    //strcpy(strs,sn);
//    for (int i = 0; i < strs_len; i++) {
//        LOGE(" DecryptSN sn ======= > %d",(int)strs[i]);
//    }
//    LOGE(" DecryptSN =====步骤二 ===== >strs ===>  %s==%d==%d " ,strs,strlen(strs),strlen(sn));
//    LOGE(" DecryptSN =====步骤二 ===== >sn_sum ===>  %d " ,sn_sum);
    strs[0] = (char) (strs[0] ^ 18);
    strs[8] = (char) (strs[8] ^ 18);
    strs[11] = (char) (strs[11] ^ sn_sum);
    strs[3] = (char) (strs[3] ^ sn_sum);
    strs[7] = (char) (strs[7] ^ 18);
    strs[9] = (char) (strs[9] ^ sn_sum);
//    LOGE(" DecryptSN =====步骤三 ===== >%d==%d",strs[9],strs_len);
    for (int i = 0; i < strs_len; i++) {
        strs[i] = (char) (strs[i] ^ 29);
    }
    char a;
    // 前后0,2,4与7+0,7+2,7+4兑换
    for (int i = 0; i < 5; i += 2) {
        a = strs[i];
        strs[i] = strs[7 + i];
        strs[7 + i] = a;
    }
//    LOGE(" DecryptSN =====步骤四 ===== > %d", strs_len);
    for (int i = 0; i < 7; i += 3) {
        if (i == 0) {
            strs[i] = (char) (strs[i] ^ strs[strs_len - 3]);
            strs[i + 1] = (char) (strs[i + 1] ^ strs[strs_len - 2]);
            strs[i + 2] = (char) (strs[i + 2] ^ strs[strs_len - 1]);
        } else if (i == 3) {
            strs[i] = (char) (strs[i] ^ strs[strs_len - 1]);
            strs[i + 1] = (char) (strs[i + 1] ^ strs[strs_len - 2]);
            strs[i + 2] = (char) (strs[i + 2] ^ strs[strs_len - 3]);
        } else {
            strs[i] = (char) (strs[i] ^ strs[i + 3]);
            strs[i + 1] = (char) (strs[i + 1] ^ strs[i + 4]);
            strs[i + 2] = (char) (strs[i + 2] ^ strs[i + 5]);
        }
    }
//    for (int index = 0 ; index < 15; index ++) {
//        LOGE(" DecryptSN =====步骤五 ==strs=== >%s" ,strs);
//    }
    mempcpy(returnData, strs, 15);

    ir_sn_h = NULL;
//    delete [] ir_sn_24;
//    ir_sn_24 = NULL;

    sn = NULL;
    ir_sn = NULL;
    return returnData;
}


/**
 *
 * @param ctrl Control block, processed using {uvc_probe_stream_ctrl} or {uvc_get_stream_ctrl_format_size}
 */
void UVCPreviewIR::do_preview(uvc_stream_ctrl_t *ctrl) {
    ENTER();
//    LOGI("======================do_preview======================");
    uvc_error_t result = uvc_start_streaming_bandwidth(mDeviceHandle, ctrl,
                                                       uvc_preview_frame_callback, (void *) this,
                                                       requestBandwidth, 0);
    if (LIKELY(!result)) {
#if LOCAL_DEBUG
        LOGI("Streaming...");
#endif
        // yuvyv mode
        for (; LIKELY(isRunning());) {
//            LOGE("do_preview0");
            pthread_mutex_lock(&preview_mutex);
            {
                //等待数据 初始化到位,之后运行下面的代码。
                pthread_cond_wait(&preview_sync, &preview_mutex);
//                 LOGE("do_preview0===pthread_cond_wait=========================");
                //判断是否需要翻转
                if (IsRotateMatrix_180()) {
                    if (mPid == 1 && mVid == 5396) {
                        rotateMatrix_180((short *) backUpBuffer, (short *) HoldBuffer, frameWidth,
                                         frameHeight - 4);
                    } else {
                        rotateMatrix_180((short *) backUpBuffer, (short *) HoldBuffer, frameWidth,
                                         frameHeight);
                    }
                }

                if (isCopyPicturing()) {//判断截屏
//                    LOGE("======mutex===========");
                    memset(picOutBuffer, 0, frameWidth * frameHeight * 2);
                    memcpy(picOutBuffer, HoldBuffer, frameWidth * frameHeight * 2);
                    mIsCopyPicture = false;
                    signal_save_picture_thread();
                }

                uint8_t *tmp_buf = NULL;
                mIsComputed = false;
                //S0机芯 获取机芯的参数，环境温度  反射率 等等 用于 温度对照表。
                if (mPid == 1 && mVid == 5396) {
                    if (mFrameImage) {
                        mFrameImage->getCameraPara(HoldBuffer);
                    }
                }
                // swap the buffers rgba
                tmp_buf = RgbaOutBuffer;
                RgbaOutBuffer = RgbaHoldBuffer;
                RgbaHoldBuffer = tmp_buf;
                //初始时outbuffer里面没有数据了，数据给holdbuffer，自己指向原来holdbuffer分配的内存首地址
                //char强转成short类型 长度由requestWidth*requestHeight*2 变成requestWidth*requestHeight
//                unsigned short* orgData=(unsigned short*)HoldBuffer;// char a数组[1,2] 经过强转short可能变成513(256*2+1)，或258(256*1+2)。
                if (isVerifySN()) {
                    unsigned char *dytSn = dytTinyCSn;
                    //解码用户区 写入的 用户SN号
                    unsigned char *tinyUserSn = TinyUserSN;
                    unsigned char *tinyRobotSn = TinyRobotSn;
                    unsigned char *tinyC_UserSn_sixLast = TinyRobotSn + 6;//指向机器SN的第六位之后
                    unsigned char *readData = TinyUserSN;
                    unsigned char *flashId = TinyRobotSn;

                    if (mPid == 22592 && mVid == 3034) {//tinyC机芯
                        int ret = UVC_ERROR_IO;
                        int dataLen = 15; //获取或读取数据大小
                        int dwStartAddr = 0x7FF000;// 用户区域首地址
                        unsigned char data[8] = {0};
                        data[0] = 0x01;
                        data[1] = 0x82;
                        data[2] = ((dwStartAddr & 0xff000000) >> 24);
                        data[3] = ((dwStartAddr & 0x00ff0000) >> 16);
                        data[4] = ((dwStartAddr & 0x0000ff00) >> 8);
                        data[5] = (dwStartAddr & 0x000000ff);
                        data[6] = (dataLen >> 8);
                        data[7] = (dataLen & 0xff);
                        if (LIKELY(mDeviceHandle)) {
                            ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00,
                                                      data,
                                                      sizeof(data), 1000);
                        }
                        unsigned char status;
                        for (int index = 0; index < 1000; index++) {
                            if (LIKELY(mDeviceHandle)) {
                                uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x0200,
                                                    &status,
                                                    1, 1000);
                            }
                            if ((status & 0x01) == 0x00) {
                                if ((status & 0x02) == 0x00) {
                                    break;
                                } else if ((status & 0xFC) != 0x00) {
//                                    RETURN(-1,int);
                                }
                            }
                        }
                        if (LIKELY(mDeviceHandle)) {
                            ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d08,
                                                      readData, 15, 1000);
                        }
                        *(tinyUserSn + 15) = '\0';
//                        LOGE("===================readData>>>>>>>>>%s<<<<<<<<<<<<<<<============",TinyUserSN);
                        unsigned char data2[8] = {0};
                        data2[0] = 0x05;
                        data2[1] = 0x84;
                        data2[2] = 0x07;
                        data2[3] = 0x00;
                        data2[4] = 0x00;
                        data2[5] = 0x10;
                        data2[6] = 0x00;
                        data2[7] = 0x10;
                        if (LIKELY(mDeviceHandle)) {
                            ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00,
                                                      data2,
                                                      sizeof(data2), 1000);
                        }
                        unsigned char status1;
                        for (int index = 0; index < 1000; index++) {
                            if (LIKELY(mDeviceHandle)) {
                                uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x0200,
                                                    &status1,
                                                    1, 1000);
                            }
                            if ((status1 & 0x01) == 0x00) {
                                if ((status1 & 0x02) == 0x00) {
                                    break;
                                } else if ((status1 & 0xFC) != 0x00) {
                                    LOGE("=====读取Sn ====RETURN================= ");
//                                    RETURN(-1,int);
                                }
                            }
                        }
                        if (LIKELY(mDeviceHandle)) {
                            ret = uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d08,
                                                      flashId, 15, 1000);
                        }
//                        LOGE("===================flashId>>>>>>>>>%s<<<<<<<<<<<<<<<============" , TinyRobotSn);
                        *(tinyRobotSn + 15) = '\0';
                        //输出AD值 数据
                        unsigned char dataa[8] = {0x0a, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                        if (LIKELY(mDeviceHandle)) {
                            ret = uvc_diy_communicate(mDeviceHandle, 0x41,
                                                      0x45,
                                                      0x0078, 0x1d00, dataa,
                                                      sizeof(dataa), 1000);
                        }

//                        pthread_mutex_lock(&tinyC_send_order_mutex);
                        //获取 机器的SN 和 用户区的SN
//                        LOGE("========tinyC_RobotSn_sixLast == %s =========",tinyC_UserSn_sixLast);
//                        *(tinyUserSn+15) = '\0';


                        DecryptSN(TinyUserSN, tinyC_UserSn_sixLast, dytSn);
                        *(dytSn + 15) = '\0';
                        LOGE("==========Tinyc====机芯用户区SN=============%s", dytSn);

                    } else if (mVid == 5396 && mPid == 1)//S0机芯
                    {
                        unsigned char *fourLinePara = NULL;

                        if (requestWidth == 384 && requestHeight == 292) {
                            //***********************S0机芯 384*288 分辨率 读取SN号**********************
                            LOGE("****************S0机芯 384*288 分辨率 读取SN号**********************");
                            fourLinePara = HoldBuffer + ((384 * (288)) << 1);
//                        BYTE * fourLinPara = pDataGet + ((WIDTH * HEIGHT) << 1);
                            int amountPixels = (384 << 1);
//                        if(384 > 256){
                            amountPixels = ((384 * 3) << 1);
//                        }
                            int userAread = amountPixels + (127 << 1);
                            memcpy((void *) &machine_sn, fourLinePara + amountPixels + (32 << 1),
                                   (sizeof(char) << 5)); // ir序列号
                            memcpy((void *) &user_sn, fourLinePara + userAread + 100,
                                   sizeof(char) * sn_length);//序列号
//                          is_getSN = false;
                            //*************************************************************************
                        } else {
                            //根据 标识  去设置是否渲染 画面 读取SN号
                            fourLinePara = HoldBuffer + ((256 * (192)) << 1);
                            int amountPixels = (256 << 1);
                            int userArea = amountPixels + (127 << 1);
                            memcpy((void *) &machine_sn, fourLinePara + amountPixels + (32 << 1),
                                   (sizeof(char) << 5)); // ir序列号
                            memcpy((void *) &user_sn, fourLinePara + userArea + 100,
                                   sizeof(char) * sn_length);//序列号
//                        LOGE(" ir_sn ======= > %s",machine_sn);
//                        LOGE(" sn_char ===== > %s",user_sn);
                        }
                        //解密之后的数据
                        (unsigned char *) DecryptSN(user_sn, machine_sn, dytSn);
                        *(dytSn + 15) = '\0';
                        LOGE("==============机芯用户区SN=============%s", dytSn);
                        //释放用到的指针资源
                        fourLinePara = NULL;

                    }

                    //读取配置文件的 加密SN
                    FILE *inFile = NULL;
                    inFile = fopen(
                            app_private_path,
                            "a+");
                    //存储文件流数据 的指针
                    char *fileStore;
                    //读取文件流 字符
                    if (inFile) {
                        //获取文件长度
                        fseek(inFile, 0, SEEK_END);
                        int length = ftell(inFile);
                        rewind(inFile);
                        //指针 分配内存
                        fileStore = (char *) malloc(length);
                        //    LOGE("sssss======File length =======> %d",length);
                        if (fread(fileStore, length, 1, inFile) != length) {
                        }
                        fclose(inFile);
                    }
//                    LOGE("==============%s==============", dytTinyCSn);
                    //切割结果
                    std::vector<std::string> split_result = split(fileStore, ";");
                    int splitSize = split_result.size();
//                    LOGE("==================configs.txt split size == %d=========",splitSize);

//                    FILE* outFile = NULL;
//                    outFile =fopen("/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/DYTLog.txt", "a+");
//                    if(outFile != NULL)
//                    {
//                        fprintf(outFile, "%s", "                UVCCamera::do_preview\n");
//                        fprintf(outFile, "                UVCCamera::do_preview==Sn=%s\n", dytTinyCSn);
//                        fclose(outFile);
//                    }
                    //遍历 结果 是否 符合筛选
                    for (int i = 0; i < splitSize; i++) {
//                       LOGE("=============split_result=== %s =====",split_result[i].c_str());
                        AES aes;
                        string decryptionSplitChild = aes.DecryptionAES(split_result[i]).substr(0,
                                                                                                8);
//                         LOGE("============decryptionSplitChild=%s=======",decryptionSplitChild.c_str());
////                       unsigned char* decryptionChild = decryptionSplitChild.c_str();
                        unsigned char *decryptionChild = new unsigned char[8];
//
                        strncpy((char *) decryptionChild, decryptionSplitChild.c_str(), 8);
                        unsigned char *decryptionChild_h = decryptionChild;
                        bool flag = true;
                        for (int j = 0; j < 8; j++) {
                            if (decryptionChild_h[j] != dytTinyCSn[j]) {
                                flag = flag & false;
                            }
                        }
                        if (flag) {
                            LOGE("=============sn解码成功========");
                            snIsRight = true;
                        } else {
                            LOGE("==============sn解码失败========");
                            snIsRight = snIsRight | 0;
                        }
                        delete[]decryptionChild;
                        decryptionChild = NULL;
                        decryptionChild_h = NULL;
                    }
                    readData = NULL;
                    flashId = NULL;
                    tinyC_UserSn_sixLast = NULL;
                    tinyRobotSn = NULL;
                    tinyUserSn = NULL;

                    inFile = NULL;
                    free(fileStore);
                    fileStore = NULL;
                    dytSn = NULL;
                    //分支结束 之前将标识 设置为 false
                    if (snIsRight) {
                        mIsVerifySn = false;
                    } else {
                        mIsVerifySn = true;
                    }
                }
                //判断完了之后去 渲染成图
                if (snIsRight) {//s0 的 sn 是否符合规定。SN 校验是正确的，  使用 HoldBuffer 去判定 S0是否需要打挡
                    draw_preview_one(HoldBuffer, &mPreviewWindow, NULL, 4);

                    all_frame_count++;
                    if (all_frame_count > general_block_strategy_frame_interval) {
                        all_frame_count = 0;
                        //打挡。
                    }

                    if (mVid == 5396 && mPid == 1) {
                        unsigned short *tmp_buf = (unsigned short *) HoldBuffer;
                        int amountPixels1 = requestWidth * (requestHeight - 4);
                        amountPixels1 += 1;
                        newADValue = tmp_buf[amountPixels1];
                        if (oldADValue == 0) {
                            oldADValue = tmp_buf[amountPixels1];
                        }
                        if (abs(newADValue - oldADValue) >= s0_value_difference) {
//                            LOGE("=====newADValue==%d====oldADValue===%d", newADValue, oldADValue);
                            //打挡指令
                            uvc_set_zoom_abs(mDeviceHandle, 0x8000);
                            oldADValue = newADValue;
                            all_frame_count = 0;
                        }
//                        LOGE("=====newADValue==%d====oldADValue===%d",newADValue,oldADValue);
                        tmp_buf = NULL;
                    }
                    //TinyC 打挡策略
                    if (mPid == 22592 && mVid == 3034) {
                        //自动调整tinyc机芯到 低温模式
                        if (is_first_run) {
//                        LOGE("===================is_first_run=======================begin==");
                            if (setMachineSetting(1, 1)) {
                                float value = 0.9184f;
                                sendTinyCParamsModification(&value, uvc_diy_communicate, 0x04);
                                is_first_run = false;
                            } else {
                                LOGE("===================is_first_run=======设置默认低增益模式失败===还会再次设置=======");
                                continue;
                            }
                        }

                        tinyC_frame_count++;
                        if (tinyC_frame_count > tinyC_block_order_interval) {
                            //获取指令。
                            unsigned char reData[2] = {0};
                            unsigned char data[8] = {0x0d, 0x8b, 0x00, 0x00, 0x00, 0x00, 0x00,
                                                     0x02};

                            if (LIKELY(mDeviceHandle)) {
                                uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00, data,
                                                    sizeof(data),
                                                    1000);
                                uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d08,
                                                    reData, sizeof(reData),
                                                    1000);
                            }
                            unsigned char reData2[2] = {0};
                            reData2[1] = reData[0];
                            reData2[0] = reData[1];
                            unsigned short *dd = (unsigned short *) reData2;
//                            LOGE("================================ javaSendJniOrder=======dd====== %d======",*dd);
                            newADValue = *dd;
                            tinyC_frame_count = 0;
                            dd = NULL;
                        }
                        if (abs(newADValue - oldADValue) >= tinyC_block_value_difference) {
//                            LOGE("=====newADValue==%d====oldADValue===%d", newADValue, oldADValue);
                            oldADValue = newADValue;
                            //打挡指令
                            unsigned char data[8] = {0x0d, 0xc1, 0x00, 0x00, 0x00, 0x00, 0x00,
                                                     0x00};
                            if (LIKELY(mDeviceHandle)) {
                                uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078,
                                                    0x1d00, data, sizeof(data), 1000);
                            }
                            all_frame_count = 0;
                        }
                    }
                }
                tmp_buf = NULL;
                mIsComputed = true;
            }
            pthread_mutex_unlock(&preview_mutex);
//            LOGE("=============mIsTemperaturing==唤醒温度回调线程=%d==========",mIsTemperaturing);
            if (mIsTemperaturing)//绘制的时候唤醒温度绘制的线程
            {
                if (LIKELY(temperature_thread)) {
                    LOGE("do_preview====温度线程还在=====================唤醒温度回调线程===============");
                    pthread_cond_signal(&temperature_sync);
                } else {
                    LOGE("do_preview====温度线程不存在=====================需要重新创建温度线程===============");
                }
            }
            //LOGE("do_preview4");
        }
#if LOCAL_DEBUG
        LOGE("preview_thread_func:wait for all callbacks complete");
#endif
        if (mDeviceHandle) {
            uvc_stop_streaming(mDeviceHandle);
        }
#if LOCAL_DEBUG
        LOGI("Streaming finished");
#endif
    } else {
        LOGE("failed start_streaming");
        uvc_perror(result, "failed start_streaming");
    }
    EXIT();
}


//数据来源
void
UVCPreviewIR::uvc_preview_frame_callback(uint8_t *frameData, void *vptr_args, size_t hold_bytes) {
    UVCPreviewIR *preview = reinterpret_cast<UVCPreviewIR *>(vptr_args);
    //判断hold_bytes 不小于preview.frameBytes则跳转到 后面正常运行     UNLIKELY期待值大几率为false时,等价于if(value)
//    LOGE("==now===uvc_preview_frame_callback hold_bytes ===> %d < preview->frameBytes  ==== > %d",
//         hold_bytes, preview->frameBytes);
    if (UNLIKELY(hold_bytes < preview->frameBytes)) {   //有手机两帧才返回一帧的数据。
        LOGE("uvc_preview_frame_callback hold_bytes ===> %d < preview->frameBytes  ==== > %d",
             hold_bytes, preview->frameBytes);
//        unsigned char *headData = preview->CacheBuffer;
        return;
    }
    //preview->isComputed()是否绘制完
    if (LIKELY(preview->isRunning() && preview->isComputed())) {
//        pthread_mutex_lock(&preview->data_callback_mutex);
//            unsigned  char * thedata = preview->OutBuffer;
        //void *memcpy(void *destin, void *source, unsigned n);从源source中拷贝n个字节到目标destin中
        memcpy(preview->OutBuffer, frameData,
               (preview->requestWidth) * (preview->requestHeight) * 2);
        memcpy(preview->backUpBuffer, frameData,
               (preview->requestWidth) * (preview->requestHeight) * 2);
        /* swap the buffers org */
//            LOGE("======callback===11111111111111111111111========");
//OutBuffer 用于接收 回调的数据流，接收完成后，和之前的HoldBuffer的地址互调。HoldBuffer用于渲染，绘制，查询。
        uint8_t *tmp_buf = NULL;
        tmp_buf = preview->OutBuffer;
        preview->OutBuffer = preview->HoldBuffer;
        preview->HoldBuffer = tmp_buf;
        tmp_buf = NULL;
        preview->signal_receive_frame_data();
//        pthread_mutex_unlock(&preview->data_callback_mutex);
    }
}

void UVCPreviewIR::signal_receive_frame_data() {
    //唤醒 preview_thread    线程
    if (LIKELY(mDeviceHandle)) {
        pthread_cond_signal(&preview_sync);
    }
}

void UVCPreviewIR::signal_save_picture_thread() {
    //唤醒 screenShot_thread    截屏线程
    pthread_cond_signal(&screenShot_sync);
}

//void UVCPreviewIR::signal_tiny_send_order() {
//    pthread_cond_signal(&tinyC_send_order_sync);
//}


//#pragma pack(2)//必须得写，否则sizeof得不到正确的结果
//typedef unsigned char  BYTE;
//typedef unsigned short WORD;
//typedef unsigned long  DWORD;
//typedef long    LONG;
//typedef struct {
//    WORD    bfType;
//    DWORD   bfSize;
//    WORD    bfReserved1;
//    WORD    bfReserved2;
//    DWORD   bfOffBits;
//} BITMAPFILEHEADER;
//
//typedef struct {
//    DWORD      biSize;
//    LONG       biWidth;
//    LONG       biHeight;
//    WORD       biPlanes;
//    WORD       biBitCount;
//    DWORD      biCompression;
//    DWORD      biSizeImage;
//    LONG       biXPelsPerMeter;
//    LONG       biYPelsPerMeter;
//    DWORD      biClrUsed;
//    DWORD      biClrImportant;
//} BITMAPINFOHEADER;
//
//void saveBitmap(int w, int h,unsigned char *pData,int nDatasize )
//{
//
//    // Define BMP Size
//    const int height = w;
//    const int width = h;
//    const int size = nDatasize;
//    double x, y;
//    int index;
//
//    // Part.1 Create Bitmap File Header
//    BITMAPFILEHEADER fileHeader;
//
//    fileHeader.bfType = 0x4D42;
//    fileHeader.bfReserved1 = 0;
//    fileHeader.bfReserved2 = 0;
//    fileHeader.bfSize = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER) + size;
//    fileHeader.bfOffBits = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER);
//
//    // Part.2 Create Bitmap Info Header
//    BITMAPINFOHEADER bitmapHeader = { 0 };
//
//    bitmapHeader.biSize = sizeof(BITMAPINFOHEADER);
//    bitmapHeader.biHeight = -height;
//    bitmapHeader.biWidth = width;
//    bitmapHeader.biPlanes = 1;
//    bitmapHeader.biBitCount = 32;
//    bitmapHeader.biSizeImage = size;
//    bitmapHeader.biCompression = 0; //BI_RGB
//
//
//    // Write to file
//    FILE *output = fopen("/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/output.bmp", "wb");
//
//    if (output == NULL)
//    {
//        LOGE("Cannot open file!\n");
//    }
//    else
//    {
//        fwrite(&fileHeader, sizeof(BITMAPFILEHEADER), 1, output);
//        fwrite(&bitmapHeader, sizeof(BITMAPINFOHEADER), 1, output);
//        fwrite(pData, size, 1, output);
//        fclose(output);
//    }
//}

//绘制每一帧的图像
void
UVCPreviewIR::draw_preview_one(uint8_t *frameData, ANativeWindow **window, convFunc_t convert_func,
                               int pixcelBytes) {
    if (LIKELY(*window)) {
        if (mCurrentAndroidVersion == 0) {
            if (mFrameImage) {
                RgbaHoldBuffer = mFrameImage->onePreviewData(frameData);
//                unsigned char * d1 = RgbaHoldBuffer;
//                for (int i = 0; i < 120; i++) {
//                    for (int j = 0; j < 160; j++) {
////                        if(i>5){
////                            *d1 = 0;
////                            d1++;
////                            *d1 = 255;
////                            d1++;
////                        }else{
//                            *d1 = 255;
//                            d1++;
//                            *d1 = 0;
//                            d1++;
////                        }
//
//
//                        *d1 = 0;
//                        d1++;
//                        *d1 = 0;
//                        d1++;
//                    }
//                }
//                d1 = NULL;
            }
        }
        copyToSurface(RgbaHoldBuffer, window);
    }
}

void UVCPreviewIR::setResourcePath(const char *path) {
    strcpy(app_private_path, path);
    const char *file_f = ".txt";
    strcat(app_private_path, file_f);
    file_f = NULL;
    if (mFrameImage) {
        std::vector<std::string> split_result = split(path, "config");
        mFrameImage->setResourcePath(split_result[0].c_str());
    }
    EXIT();
}

inline const bool UVCPreviewIR::IsRotateMatrix_180() const { return isRotateMatrix_180; }

//图像 旋转180度  S0 机芯后面 四行参数不变，TinyC 全部都要改变
void UVCPreviewIR::rotateMatrix_180(short src_frameData[], short dst_frameData[], int width,
                                    int height) {
//    LOGE("=============width ====%d========height=====%d=========", width, height);
    for (int h = 0; h < height; h++) {
        for (int w = 0; w < width; w++) {
            dst_frameData[h * width + w] = src_frameData[(height - h - 1) * width +
                                                         (width - w - 1)];
        }
    }
}

// transfer specific frame data to the Surface(ANativeWindow)
int UVCPreviewIR::copyToSurface(uint8_t *frameData, ANativeWindow **window) {
//LOGE("copyToSurface");
//     ENTER();
    //LOGE("window shows picture here wupei before");
    int result = 0;
    if (LIKELY(*window)) {
        ANativeWindow_Buffer buffer;
        if (LIKELY(ANativeWindow_lock(*window, &buffer, NULL) == 0)) {
//            LOGE("====ANativeWindow_Buffer==width=%d==,height==%d====",buffer.width,buffer.height);
            // source = frame data
//            LOGE("=======UVCPreviewIR::copyToSurface=========getWidth===%d,======getHeight===%d====",ANativeWindow_getWidth(*window),ANativeWindow_getHeight(*window));
            const uint8_t *src = frameData;
            const int src_w = frameWidth * PREVIEW_PIXEL_BYTES;//256*4  代表一行 颜色 的RBGA值
            const int src_step = src_w; //一次绘制一行？
            // destination = Surface(ANativeWindow)
            uint8_t *dest = (uint8_t *) buffer.bits;
            const int dest_w = buffer.width * PREVIEW_PIXEL_BYTES;
            const int dest_step = buffer.stride * PREVIEW_PIXEL_BYTES;
            // use lower transfer bytes
            const int w = src_w < dest_w ? src_w : dest_w;//取两者的最小值
            // use lower height
            const int h = frameHeight < buffer.height ? frameHeight : buffer.height;//取两者的最小值
            //LOGE("copyToSurface");
            // transfer from frame data to the Surface
//            LOGE("copyToSurface:w:%d,h,%d",w,h);
            mFrameImage->copyFrame(src, dest, w, h, src_step, dest_step);
//            copyFrame(src, dest, w, h, src_step, dest_step);
            src = NULL;
            dest = NULL;
            //LOGE("copyToSurface2");
            ANativeWindow_unlockAndPost(*window);
            //LOGE("copyToSurface3");

        } else {
            //	//LOGE("copyToSurface4");
            result = -1;
        }
    } else {
        //LOGE("copyToSurface5");
        result = -1;
    }
    //LOGE("copyToSurface6");
    return result;
//    RETURN(result, int);
}
/*************************************预览 结束****************************************************/

/***************************************截屏相关  开始**************************************/

int UVCPreviewIR::savePicture(const char *path) {
//    LOGE("UVCPreviewIR savePicture  pathlength ==   %d" , strlen(path));
    mIsCopyPicture = true;
//    memset(savePicPath,'0',strlen(savePicPath));
    memcpy(&savePicPath, path, strlen(path));

    if (pthread_create(&screenShot_thread, NULL, screenShot_thread_func, (void *) this) == 0) {
        LOGE("UVCPreviewIR::savePicture screenShot_thread_func: pthread_create success");
    } else {
        LOGE("UVCPreviewIR::savePicture screenShot_thread_func: pthread_create failed");
    }
    return 0;
}

/**
 * 截屏线程定义
 * @param vptr_args pointer to UVCPreviewIR instance
 * @return
 */
void *UVCPreviewIR::screenShot_thread_func(void *vptr_args) {
    int result;
    if (LOCAL_DEBUG) { LOGE("capture_thread_func"); }
    ENTER();
    UVCPreviewIR *preview = reinterpret_cast<UVCPreviewIR *>(vptr_args);
    if (LIKELY(preview)) {
        //todo 从 holdbuffer拷贝出一个ad值备份，然后此处开始组装数据
        preview->do_savePicture();
//        //LOGE("capture_thread_func do_capture");
////        preview->do_capture(env);	// never return until finish previewing
//        LOGE("=============screenShot_thread_func====run ================= %d",getpid());
    }
    PRE_EXIT();
    pthread_exit(NULL);
}

void UVCPreviewIR::do_savePicture() {
    if (isRunning()) {
        pthread_mutex_lock(&screenShot_mutex);
        {
            LOGE("=======do_savePicture======pthread_cond_wait====");
            pthread_cond_wait(&screenShot_sync, &screenShot_mutex);
//            LOGE("=============print====run ====savePicPath============= %s", savePicPath);
            //此时拿到关键的帧数据后，线程被唤醒，执行，包装插入数据：自定义结构体数据、  插入ad值数据。
//            LOGE("=======do_savePicture======pthread_cond_wait================ ");
            savePicDefineData();
        }
        pthread_mutex_unlock(&screenShot_mutex);
    }
}

void UVCPreviewIR::fixedTempStripChange(bool state) {
//    pthread_mutex_lock(&fixed_mutex);
    {
        mFrameImage->fixedTempStripChange(state);
    }
//    pthread_mutex_unlock(&fixed_mutex);
}

/**
 * 保存TinyC 机芯参数 指令
 */
void UVCPreviewIR::setTinySaveCameraParams() {
//		保存设置
//    pthread_mutex_lock(&tinyC_send_order_mutex);
    int ret = UVC_ERROR_IO;
    unsigned char data[8] = {0x14, 0x85, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00};
    data[0] = 0x01;
    data[1] = 0x0B;
    data[2] = 0x0C;
    data[3] = 0x00;
    data[4] = 0x00;
    data[5] = 0x00;
    data[6] = 0x00;
    data[7] = 0x00;
    ret = uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00, data, sizeof(data), 1000);
    LOGE("======== 保存设置 ============ ret === %d ==================", ret);
//    pthread_mutex_unlock(&tinyC_send_order_mutex);
}

void UVCPreviewIR::savePicDefineData() {
    int len = 0;
/**************************自定义结构体数据********************************/
    // 文件头
    D_FILE_HEAD fileHead;
    fileHead.version = 10001;
    fileHead.dytName[0] = 'd';
    fileHead.dytName[1] = 'y';
    fileHead.dytName[2] = 't';
    fileHead.dytName[3] = 's';
    fileHead.offset = 500;
    memset(fileHead.reserved, 0, 16);
    len += sizeof(fileHead);
//    LOGE("===D_FILE_HEAD=========%d",sizeof(fileHead));

    // 热数据说明
    unsigned short *adPicbuffer = (unsigned short *) picOutBuffer;

    D_IR_DATA_PAR irDataPar;
    irDataPar.ir_w = 256;
    irDataPar.ir_h = 192;
    int fourLineIndex = 256 * 192;

    irDataPar.raw_max = adPicbuffer[fourLineIndex + 4];
    irDataPar.raw_min = adPicbuffer[fourLineIndex + 7];
//    irDataPar.raw_max = 8000;
//    irDataPar.raw_min = 5000;

    irDataPar.pointSize = 2;
    irDataPar.typeAndSize = (1 << 4) | 4;;
    memset(irDataPar.reserved, 0, 14);
    len += sizeof(irDataPar);

    adPicbuffer = NULL;
//    LOGE("===D_IR_DATA_PAR=========%d",sizeof(irDataPar));
    LOGE("=======raw_max =====  %d", irDataPar.raw_max);
    LOGE("=======raw_min ======  %d", irDataPar.raw_min);


    // 相机硬件部分
    D_CAMERA_INFO cameraInfo;
    string devName = "CA10";
    string devSN = "DYTCA10DJK010010";
    string devf = "CA10D1234567";
    memcpy(cameraInfo.devName, devName.c_str(), 4);
    memcpy(cameraInfo.devSN, devSN.c_str(), 18);
    memcpy(cameraInfo.devFirmware, devf.c_str(), 12);
    memset(cameraInfo.reserved, 0, 52);
    len += sizeof(cameraInfo);
//    LOGE("===D_CAMERA_INFO=========%d",sizeof(cameraInfo));

    // 传感器信息
    D_SENSOR_INFO sensorInfo;
    string senName = "sensor1234567890";
    memcpy(sensorInfo.sensorName, senName.c_str(), 16);
    memset(sensorInfo.sensorType, 0, 16);
    memset(sensorInfo.sensorP1, 0, 16);
    memset(sensorInfo.sensorP2, 0, 12);
    memset(sensorInfo.sensorP3, 0, 12);
    memset(sensorInfo.sensorP4, 0, 12);
    memset(sensorInfo.reserved, 0, 20);
    len += sizeof(sensorInfo);
//    LOGE("===D_SENSOR_INFO=========%d",sizeof(sensorInfo));

    // 镜头信息
    D_LENS_INFO lensInfo;
    string lensName = "lensName=CA10D12";
    string lensPn = "lensPn===CA10D12";
    string lensSn = "lensSn===CA10D12";
    memcpy(lensInfo.lensName, lensName.c_str(), 16);
    memcpy(lensInfo.lensPn, lensPn.c_str(), 16);
    memcpy(lensInfo.lensSn, lensSn.c_str(), 16);
    lensInfo.HFoV = 55.5f;
    lensInfo.VFov = 33.3f;
    lensInfo.Transm = 88.8;
    lensInfo.FocalLength = 5;
    lensInfo.length = 8;
    lensInfo.Aperture = 77.7;
    memset(lensInfo.reserved, 0, 20);
    len += sizeof(lensInfo);
//    LOGE("===D_LENS_INFO=========%d",sizeof(lensInfo));

    // 相机设置部分
    D_CAMERA_PAR cameraPar;
    cameraPar.emissivity = 95;
    cameraPar.distance[0] = 3;
    cameraPar.distance[1] = 5;
    cameraPar.ambientTemp = 25.8;
    cameraPar.reflexTemp = 27.5;
    cameraPar.humidity = 45;
    cameraPar.fix = 1.5;
    cameraPar.startFPA = 18;
    cameraPar.currentFPA = 25;
    cameraPar.shutterFPA = 22;
    cameraPar.shellFPA = 30;
    memset(cameraPar.reserved, 0, 44);
    int siezPar = sizeof(cameraPar);
    len += sizeof(cameraPar);
//    LOGE("===D_CAMERA_PAR=========%d",sizeof(cameraPar));

    // 图像信息
    D_IMAGE_INFO imageInfo;
    imageInfo.tempUnit = 1;
    imageInfo.lutCode = 10;
    memset(imageInfo.reserved, 0, 26);
    len += sizeof(imageInfo);
//    LOGE("===D_IMAGE_INFO=========%d",sizeof(imageInfo));

    // 融合信息
    D_FUSE_INFO fuseInfo;
    fuseInfo.zoomFactor = 0;
    fuseInfo.xpanVal = 0;
    fuseInfo.vpanVal = 0;
    fuseInfo.firstFusionX = 0;
    fuseInfo.lastFusionX = 0;
    fuseInfo.firstFusionY = 0;
    fuseInfo.lastFusionY = 0;
    fuseInfo.fusionMode = 0;
    fuseInfo.fusionLevel = 0;
    memset(fuseInfo.reserved, 0, 10);
    int lens = sizeof(fuseInfo);
    len += sizeof(fuseInfo);
//    LOGE("===D_FUSE_INFO=========%d",sizeof(fuseInfo));

    // GPS
    D_GPS_INFO gpsInfo;
    gpsInfo.directionIndicator = (1 << 4) | 0;
    gpsInfo.longitude = 100.85;
    gpsInfo.latitude = 200.85;
    gpsInfo.altitude = 8448.62;
    gpsInfo.time = 884862;
    memset(gpsInfo.reserved, 0, 15);
    int gpslen = sizeof(gpsInfo);
    len += sizeof(gpsInfo);
    int longlen = sizeof(unsigned long);
//    LOGE("===D_GPS_INFO=========%d",sizeof(gpsInfo));
//    LOGE("===D_GPS_INFO==此时len=======%d",len);


    char tableData[1024];
    for (int i = 0; i < 1024; i++) {
        tableData[i] = 9;
    }
    len += 1024;

    // 图形说明
    D_SHAPE_INFO dShapeInfo;
    dShapeInfo.pointSize = 2;
    dShapeInfo.lineSize = 3;
    dShapeInfo.recSize = 2;
    dShapeInfo.ellipseSize = 0;
    dShapeInfo.polygonSize = 2;

    len += sizeof(dShapeInfo);
    LOGE("the len ==============   %d ", len);

    fileHead.offset = len;//结构体的长度。即偏移量。  用于快速的到ad值位置的偏移量

    /******************开始合并自定义的数据集合**********************/
    int dataLen = irDataPar.ir_w * irDataPar.ir_h * 2;//ad值的长度
    // 开辟等长内存
    unsigned char *dataParBuf = (unsigned char *) malloc(
            len + dataLen);//dataParBuf   的长度  为包装自定义信息加上 ad值的长度
    unsigned char *old_dataParBuf = dataParBuf;
    int cpyLen = 0;//以拷贝的长度

    //从自定义结构体长度开始拷贝
    cpyLen = sizeof(fileHead);//得到fileHead对象结构体 的长度
    //memset(void *s ,int ch , size_t n );//将s中当前位置后面的n个字节，用 ch 替换 并返回s;
    memcpy(dataParBuf, &fileHead, cpyLen);//从fileHead 从拷贝 cpylen个字节到 dataParBuf 中
    dataParBuf += cpyLen;//指向第fileHead的位置-1。

    cpyLen = sizeof(irDataPar);
    memcpy(dataParBuf, &irDataPar, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = sizeof(cameraInfo);
    memcpy(dataParBuf, &cameraInfo, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = sizeof(sensorInfo);
    memcpy(dataParBuf, &sensorInfo, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = sizeof(lensInfo);
    memcpy(dataParBuf, &lensInfo, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = sizeof(cameraPar);
    memcpy(dataParBuf, &cameraPar, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = sizeof(imageInfo);
    memcpy(dataParBuf, &imageInfo, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = sizeof(fuseInfo);
    memcpy(dataParBuf, &fuseInfo, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = sizeof(gpsInfo);
    memcpy(dataParBuf, &gpsInfo, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = 1024;//预留的1024长度
    memcpy(dataParBuf, &tableData, cpyLen);
    dataParBuf += cpyLen;

    cpyLen = sizeof(dShapeInfo);
    memcpy(dataParBuf, &dShapeInfo, cpyLen);
    dataParBuf += cpyLen;

    for (int i = 0; i < dataLen; i++) {
        dataParBuf[i] = picOutBuffer[i];
    }
    LOGE("====================dataLen===============%d", dataLen);
    LOGE("====================len===============%d", len);

    int fileLen = len + dataLen;
    LOGE("====================fileLen===============%d", fileLen);
    int result = D_saveData(savePicPath, old_dataParBuf, fileLen);
    if (result) {
        //todo java 截屏成功方法
    } else {
        //todo java  截屏失败
    }
    dataParBuf = NULL;
    free(old_dataParBuf);
    old_dataParBuf = NULL;
}
/***************************************录制相关 结束**************************************/

/***********************************温度相关 开始******************************************************/

//add by 吴长城 获取UVC连接状态回调
int UVCPreviewIR::setUVCStatusCallBack(JNIEnv *env, jobject uvc_connect_status_callback) {
    pthread_mutex_lock(&temperature_mutex);
    {
        if (!env->IsSameObject(mUvcStatusCallbackObj, uvc_connect_status_callback)) {
            iUvcStatusCallback.onUVCCurrentStatus = NULL;
            if (mUvcStatusCallbackObj) {
                env->DeleteLocalRef(mUvcStatusCallbackObj);
            }
            mUvcStatusCallbackObj = uvc_connect_status_callback;
            if (mUvcStatusCallbackObj) {
                // get method IDs of Java object for callback
                jclass clazz = env->GetObjectClass(mUvcStatusCallbackObj);
                if (LIKELY(clazz)) {
                    iUvcStatusCallback.onUVCCurrentStatus = env->GetMethodID(clazz,
                                                                             "onUVCCurrentStatus",
                                                                             "(I)V");
                } else {
                    LOGE("UVCPreviewIR::setUVCStatusCallBack failed to get object class");
                }
                env->ExceptionClear();
                if (!iUvcStatusCallback.onUVCCurrentStatus) {
                    LOGE("Can't find iUvcStatusCallback#onUVCCurrentStatus");
                    env->DeleteGlobalRef(uvc_connect_status_callback);
                    mUvcStatusCallbackObj = NULL;
                    uvc_connect_status_callback = NULL;
                }
            }
        }
    }
    pthread_mutex_unlock(&temperature_mutex);
    RETURN(0, int);
}

//传递一个温度回调的对象，还传递一个回调的方法，实现通用
int UVCPreviewIR::setTemperatureCallback(JNIEnv *env, jobject temperature_callback_obj) {
    ENTER();
    //LOGE("setTemperatureCallback01");
    LOGE("setTemperatureCallback步骤9");
    pthread_mutex_lock(&temperature_mutex);
    {
//        FILE* outFile = NULL;
//        outFile =fopen("/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/DYTLog.txt", "a+");
//        if(outFile != NULL)
//        {
//            fprintf(outFile, "               UVCPreviewIR::setTemperatureCallback\n");
//            fclose(outFile);
//        }
        mFrameImage->setTemperatureCallback(env, temperature_callback_obj);
        LOGE("setTemperatureCallback步骤10");
    }
    pthread_mutex_unlock(&temperature_mutex);
    RETURN(0, int);
}

int UVCPreviewIR::startTemp() {
    ENTER();
    pthread_mutex_lock(&temperature_mutex);
    {
        if (isRunning() && (!mIsTemperaturing)) {
//            LOGE("startTemp=======isRunning&!mIsTemperaturing==========");
            mIsTemperaturing = true;
        }
    }
    pthread_mutex_unlock(&temperature_mutex);
    //在这里开始传递测温数据
    if (pthread_create(&temperature_thread, NULL, temperature_thread_func, (void *) this) == 0) {
        LOGE("========UVCPreviewIR::startTemp temperature_thread: pthread_create success=====");
    } else {
        LOGE("========UVCPreviewIR::startTemp temperature_thread: pthread_create failed======");
    }

    RETURN(0, int);
}

int UVCPreviewIR::stopTemp() {
    ENTER();
    pthread_mutex_lock(&temperature_mutex);
    {
        if (isRunning() && mIsTemperaturing) {
//            LOGE("stopTemp");
            mIsTemperaturing = false;
            pthread_cond_signal(&temperature_sync);
//            pthread_cond_wait(&temperature_sync,
//                              &temperature_mutex);    // wait finishing Temperatur
        }
    }
    pthread_mutex_unlock(&temperature_mutex);
    if (pthread_join(temperature_thread, NULL) != EXIT_SUCCESS) {
        LOGE("====UVCPreviewIR::stopTemp temperature_thread: pthread_join failed======");
    } else {
        LOGE("====UVCPreviewIR::stopTemp temperature_thread: pthread_join success=====");
    }
    RETURN(0, int);
}

void *UVCPreviewIR::temperature_thread_func(void *vptr_args) {
    int result;
    LOGE("temperature_thread_func步骤0");
    ENTER();
    UVCPreviewIR *preview = reinterpret_cast<UVCPreviewIR *>(vptr_args);
    if (LIKELY(preview)) {
        JavaVM *vm = getVM();
        JNIEnv *env;
        //attach to JavaVM
        vm->AttachCurrentThread(&env, NULL);
//        LOGE("temperature_thread_func do_temperature");
        preview->do_temperature(env);    // never return until finish previewing
        //detach from JavaVM
        vm->DetachCurrentThread();
        MARK("DetachCurrentThread");
    }
    PRE_EXIT();
    pthread_exit(NULL);
}

void UVCPreviewIR::do_temperature(JNIEnv *env) {
    ENTER();
//    LOGE("do_temperature温度步骤3");
    ////LOGE("do_temperature mIsTemperaturing:%d",mIsTemperaturing);
//    LOGE("=====do_temperature=====isRunning===%d,IsTemperaturing===%d", isRunning(),
//         mIsTemperaturing);
//    FILE* outFile = NULL;
//    outFile =fopen("/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/DYTLog.txt", "a+");
//    if(outFile != NULL)
//    {
//        fprintf(outFile, "               UVCPreviewIR::do_temperature\n");
//        fclose(outFile);
//    }
    for (; isRunning() && mIsTemperaturing;) {
//        LOGE("=====do_temperature=====for==both=== true============");
        pthread_mutex_lock(&temperature_mutex);
        {
            LOGE("do_temperature========wait=======callback==========");
            pthread_cond_wait(&temperature_sync, &temperature_mutex);
            LOGE("do_temperature02============================");
            if (mIsTemperaturing) {
//                if (LIKELY(mUvcStatusCallbackObj) &&
//                    LIKELY(iUvcStatusCallback.onUVCCurrentStatus)) {
//                    //有导致空引用
//                    env->CallVoidMethod(mUvcStatusCallbackObj,
//                                        iUvcStatusCallback.onUVCCurrentStatus, UVC_STATUS);
////                    env->ExceptionClear();
//                }
                mFrameImage->do_temperature_callback(env, HoldBuffer);
            }
//            LOGE("do_temperature03");
        }
        pthread_mutex_unlock(&temperature_mutex);
    }
    pthread_cond_broadcast(&temperature_sync);
    EXIT();
}

//读取原始数据holdbuffer .将原始YUV数据查表之后的温度数据回调。10+ 256*192
void UVCPreviewIR::do_temperature_callback(JNIEnv *env, uint8_t *frameData) {
    LOGE("=========do_temperature_callback ======");
    if (mPid == 1 && mVid == 5396) {
        mFrameImage->do_temperature_callback(env, frameData);
    } else if (mPid == 22592 && mVid == 3034) {
        mFrameImage->do_temperature_callback(env, frameData);
    }

}

/***************************************温度相关 结束**************************************/
void UVCPreviewIR::clearDisplay() {//
    ENTER();
//LOGE("clearDisplay");
    ANativeWindow_Buffer buffer;
    pthread_mutex_lock(&preview_mutex);
    {
        if (LIKELY(mPreviewWindow)) {
            if (LIKELY(ANativeWindow_lock(mPreviewWindow, &buffer, NULL) == 0)) {
                uint8_t *dest = (uint8_t *) buffer.bits;
                const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
                const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
                for (int i = 0; i < buffer.height; i++) {
                    memset(dest, 0, bytes);
                    dest += stride;
                }
                dest = NULL;
                ANativeWindow_unlockAndPost(mPreviewWindow);
            }
        }
    }
    pthread_mutex_unlock(&preview_mutex);
    EXIT();
}

int UVCPreviewIR::getByteArrayTemperaturePara(uint8_t *para) {
    if (mPid == 1 && mVid == 5396) {
        mFrameImage->getByteArrayTemperaturePara(para, HoldBuffer);
    }
    return true;
}

void UVCPreviewIR::shutRefresh() {
//    pthread_mutex_lock(&temperature_mutex);
    if (mFrameImage) {
        mFrameImage->shutRefresh();
    }
//    pthread_mutex_unlock(&temperature_mutex);
}

void UVCPreviewIR::testJNI(const char *phoneStr) {
    LOGE("==========================testJNI=======================%s==", phoneStr);
//    auto pixels = new unsigned char[frameWidth*frameHeight*3];
//    unsigned char optput = '1';
//    TooJpeg::writeJpeg((**optput),pixels,frameWidth,frameHeight,true,95,false,NULL);
//    temperature_thread
//    if (temperature_thread = NULL){
//
//    }
//    delete [] pixels;
}