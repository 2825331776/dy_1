#include <stdlib.h>
#include <linux/time.h>
#include <unistd.h>
#include <math.h>


#include <iostream>
#include <fstream>
using namespace std;

#if 1	// set 1 if you don't need debug log
#ifndef LOG_NDEBUG
#define	LOG_NDEBUG		// w/o LOGV/LOGD/MARK
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
#include "jpegext.h"

#include <vector>
#include "AES.h"
#include "Base64.h"

#define LOG_TAG "===UVCPREVIEW==="

#define	LOCAL_DEBUG 0
#define MAX_FRAME 4
#define PREVIEW_PIXEL_BYTES 4	// RGBA/RGBX
#define FRAME_POOL_SZ MAX_FRAME
//切换数据时需要修改这个
#define OUTPUTMODE 4
//#define OUTPUTMODE 5


UVCPreviewIR::UVCPreviewIR(uvc_device_handle_t *devh ,FrameImage * frameImage){
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
            frameBytes = DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * 2;	// YUYV
            frameMode = 0;
            previewBytes = DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * PREVIEW_PIXEL_BYTES;
            previewFormat = WINDOW_FORMAT_RGBX_8888;
            mIsRunning = false;
            mIsTemperaturing = false;
            mIsCapturing = false;

    mFrameImage = frameImage;

    mIsComputed=true;
    mIsCopyPicture = false;
    OutPixelFormat=3;
    mTypeOfPalette=1;



    pthread_cond_init(&preview_sync, NULL);
    pthread_mutex_init(&preview_mutex, NULL);
//
    pthread_cond_init(&screenShot_sync, NULL);
    pthread_mutex_init(&screenShot_mutex, NULL);

    pthread_cond_init(&temperature_sync,NULL);
    pthread_mutex_init(&temperature_mutex,NULL);//初始化温度线程对象

    pthread_mutex_init(&data_callback_mutex,NULL);

    pthread_mutex_init(&tinyc_send_order_mutex,NULL);

//    pthread_mutex_init(&fixed_mutex,NULL);
    EXIT();

}

UVCPreviewIR::~UVCPreviewIR() {

    ENTER();
    mFrameImage = NULL;
    if (mPreviewWindow)
        ANativeWindow_release(mPreviewWindow);
    mPreviewWindow = NULL;
    pthread_mutex_destroy(&preview_mutex);
    pthread_cond_destroy(&preview_sync);
    //LOGE("~UVCPreviewIR() 1");
//    if (mCaptureWindow)
//        ANativeWindow_release(mCaptureWindow);
//    mCaptureWindow = NULL;
    pthread_mutex_destroy(&screenShot_mutex);
    pthread_cond_destroy(&screenShot_sync);
    pthread_mutex_destroy(&temperature_mutex);//析构函数内释放内存
    pthread_cond_destroy(&temperature_sync);

    pthread_mutex_destroy(&data_callback_mutex);

    pthread_mutex_destroy(&tinyc_send_order_mutex);
//    pthread_mutex_destroy(&fixed_mutex);
    //LOGE("~UVCPreviewIR() 8");

//    if(OutBuffer!=NULL){
//        delete[] OutBuffer;
//    }
//    if(HoldBuffer!=NULL){
//        delete[] HoldBuffer;
//    }
//    if(RgbaOutBuffer!=NULL){
//        delete[] RgbaOutBuffer;
//    }
//    if(RgbaHoldBuffer!=NULL){
//        delete[] RgbaHoldBuffer;
//    }

        delete[] picOutBuffer;
//        delete[] picRgbaOutBuffer;
    EXIT();
}


/************************************绘制  预览***************************************************/
/***************************************************************************************/
inline const bool UVCPreviewIR::isRunning() const {return mIsRunning; }
inline const bool UVCPreviewIR::isComputed() const {return mIsComputed; }
inline const bool UVCPreviewIR::isCopyPicturing() const {return mIsCopyPicture;}
inline const bool UVCPreviewIR::isVerifySN() const {return mIsVerifySn;}

//此处会调用两次，第一次：初始化camera时：宽高是默认值；第二次（startPreview设置调用到）是传入了新的宽高，此时camera是已经初始化open。
int UVCPreviewIR::setPreviewSize(int width, int height, int min_fps, int max_fps, int mode, float bandwidth,int currentAndroidVersion) {
    ENTER();
    LOGE("=========>requestWidth  == %d,width  == %d,requestHeight  == %d,height  == %d,requestMode  == %d, mode == %d",requestWidth,width,requestHeight,height,requestMode, mode);
    //LOGE("setPreviewSize");
    LOGE("=========== %d======================%d==========",width,height);
    LOGE("setPreviewSize步骤6");
    int result = 0;
    if ((requestWidth != width) || (requestHeight != height) || (requestMode != mode)) {
//        mFrameImage添加一个函数初始化绘制需要的值
        LOGE("requestWidth  == %d,width  == %d,requestHeight  == %d,height  == %d,requestMode  == %d, mode == %d",requestWidth,width,requestHeight,height,requestMode, mode);
        mFrameImage->setPreviewSize(width,height,mode);
        requestWidth = width;
        requestHeight = height;
        requestMinFps = min_fps;
        requestMaxFps = max_fps;
        requestMode = 0;
        requestBandwidth = bandwidth;
        uvc_stream_ctrl_t ctrl;

        LOGE("uvc_get_stream_ctrl_format_size_fps ==  ===2222222222");
        result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, &ctrl,
                                                     !requestMode ? UVC_FRAME_FORMAT_YUYV : UVC_FRAME_FORMAT_MJPEG,
                                                     requestWidth, requestHeight, requestMinFps, requestMaxFps);
    }

    LOGE("setPreviewSize==========================over");
    mCurrentAndroidVersion=currentAndroidVersion;
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
                //requestHeight-4  for camera
                /**
                 * 更改窗口缓冲区的格式和大小。宽度和高度控制缓冲区中像素的数量，
                 * 而不是屏幕上窗口的尺寸。 如果这些与窗口的物理大小不同，
                 * 则在将其合成到屏幕时将缩放其缓冲区以匹配该大小。 宽度和高度必须均为零或均为非零，
                 * 对于所有这些参数，如果提供0，则窗口的基值将恢复生效
                 */
                //S0机芯
                if (mPid == 1 && mVid == 5396){
                    ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                     requestWidth, requestHeight -4, previewFormat);
                }else if (mPid == 22592 && mVid == 3034){
                    ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                 requestWidth, requestHeight , previewFormat);
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
    if (!isRunning())
    {
        mIsRunning = true;
        //pthread_mutex_lock(&preview_mutex);
        //{
        result = pthread_create(&preview_thread, NULL, preview_thread_func, (void *)this);
        ////LOGE("STARTPREVIEW RESULT1:%d",result);
        //}
        //	pthread_mutex_unlock(&preview_mutex);
        if (UNLIKELY(result != EXIT_SUCCESS))
        {
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
void UVCPreviewIR::setVidPid(int vid ,int pid){
    mVid = vid;
    mPid = pid;
}
void UVCPreviewIR::setIsVerifySn(){
    mIsVerifySn = !mIsVerifySn;
}
int UVCPreviewIR::sendTinyCAllOrder(void * params , diy func_tinyc, int mark){
    int ret = UVC_ERROR_IO;
    LOGE("=======sendTinyCAllOrder === lock==Thread id = %d===== mark = %d==" , gettid() , mark);
//    LOGE("=======mark === >%d=========" , mark);
    pthread_mutex_lock(&tinyc_send_order_mutex);
    if (mark == 10){//获去tinyc机芯参数列表
        ret = getTinyCParams(params, func_tinyc);
    } else if (mark == 100){//纯指令， 打挡  返回AD流
        LOGE("=========mark == 100======= === %d=======" ,*((int*)params));
        ret = sendTinyCOrder((uint32_t*) params, func_tinyc);
    } else if ( mark > 0 && mark < 10){//设置机芯参数
        ret = sendTinyCParamsModification((float*)(params),func_tinyc,mark);
    } else if (mark == 20 || mark == 21){
        LOGE("=========mark == 20 || mark == 21=======" );
        ret = getTinyCUserData(params,func_tinyc,mark);
    }
    pthread_mutex_unlock(&tinyc_send_order_mutex);
    LOGE("=======sendTinyCAllOrder === unlock=Thread id ===%d=== mark = %d====" , gettid(), mark);
    RETURN(ret,int);
}
int UVCPreviewIR::sendTinyCOrder(uint32_t* value,diy func_diy){
    int ret = UVC_ERROR_IO;
    if (*value == 32772){//画面设置指令 0X8004 , 32773 == 0X8005
        unsigned char data[8] = {0x14,0x85,0x00,0x03,0x00,0x00,0x00,0x00};
        //输出温度数据(AD值)
        data[0] = 0x0a;
        data[1] = 0x01;
        data[2] = 0x00;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;
        data[6] = 0x00;
        data[7] = 0x00;
        ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d00,data, sizeof(data),1000);
    }
    if (*value == 32768){//打挡指令 0X8000
        unsigned char data[8] = {0x0d,0xc1,0x00,0x00,0x00,0x00,0x00,0x00};
        ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d00,data, sizeof(data),1000);
    }
    RETURN(ret,int);
}
int UVCPreviewIR::sendTinyCParamsModification(float * value,diy func_diy , uint32_t mark){
    int ret = UVC_ERROR_IO;

    unsigned char data[8] = {0x14,0xc5,0x00,0x00,0x00,0x00,0x00,0x00};
    unsigned char data2[8] = {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x02};
    data[0] = 0x14;
    data[1] = 0xc5;
    data[2] = 0x00;
    data[3] = 0x01; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段；0x03 发射率
    data[4] = 0x00;
    data[5] = 0x00;

//    setIsVerifySn();
    if(mark == 1 ){         //反射温度  ， 温度转 K 华氏度
        LOGE("=================== 反射温度===============%f======" ,(*value));
        int val = (int )(*value + 273.15f);
        data[3] = 0x01; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段
        data[6] = (val >> 8);
        data[7] = (val & 0xff);
    }
    if(mark == 2 ){         //大气温度
        LOGE("=================== 大气温度===============%f======" ,(*value));
        int val = (int )(*value + 273.15f);
        data[3] = 0x02; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段
        data[6] = (val >> 8);
        data[7] = (val & 0xff);
    }
    if(mark == 3 ){         //发射率
        LOGE("=================== 发射率===============%f======" ,(*value));
        int val = (int )(*value * 128);
        data[3] = 0x03; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段
        data[6] = (val >> 8);
        data[7] = (val & 0xff);

    }
    if(mark == 4 ){         //大气透过率
        LOGE("=================== 大气透过率===============%f======" ,(*value));
        int val = (int )(*value * 128);
        data[3] = 0x04; //0x01 = 反射温度；0x02 = 大气温度；0x04 = 大气透过率；0x05=高低温度段
        data[6] = (val >> 8);
        data[7] = (val & 0xff);
    }

    if (data[3] != 0x00){//保证修改的 值有数据
        LOGE("========== 修改设置前 ========= ret === %d ==================",ret);
        ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x9d00,data, sizeof(data),1000);
        ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d08,data2, sizeof(data2),1000);
        LOGE("========== 修改设置后 ========= ret === %d ==================",ret);
//		保存设置
        if (ret == 0){
            ret = UVC_ERROR_IO;

            data[0] = 0x01;
            data[1] = 0x0B;
            data[2] = 0x0C;
            data[3] = 0x00;
            data[4] = 0x00;
            data[5] = 0x00;
            data[6] = 0x00;
            data[7] = 0x00;
            ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d00,data, sizeof(data),1000);
            LOGE("======== 保存设置 ============ ret === %d ==================",ret);
        }
    }
    RETURN(ret,int);
}
//20 读取 用户区SN ， 21 读取机器Sn
int UVCPreviewIR::getTinyCUserData(void * returnData ,diy func_diy,int userMark){
    int ret = UVC_ERROR_IO;
    //读取用户区数据
    unsigned char data[8] = {0x0d,0xc1,0x00,0x00,0x00,0x00,0x00,0x00};
    if(userMark == 20){//读取用户区SN
        // 读取用户区域
        int dataLen = 15; //获取或读取数据大小
        unsigned char * readData = (unsigned char * )returnData;
//        LOGE("=============  redaData length = > %d ",sizeof (readData));
        int dwStartAddr = 0x7FF000;// 用户区域首地址

        data[0] = 0x01;
        data[1] = 0x82;
        data[2] = ((dwStartAddr&0xff000000)>>24);
        data[3] = ((dwStartAddr&0x00ff0000)>>16);
        data[4] = ((dwStartAddr&0x0000ff00)>>8);
        data[5] = (dwStartAddr&0x000000ff);
        data[6] = (dataLen>>8);
        data[7] = (dataLen&0xff);
        ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d00,data, sizeof(data),1000);
        unsigned char status;
        for(int index = 0;index < 1000;index++){
            func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x0200,&status, 1,1000);
            if((status & 0x01) == 0x00){
                if((status & 0x02) == 0x00){
                    break;
                }else if((status & 0xFC) != 0x00){
                    RETURN(-1,int);
//                    snIsRight = false;
                }
            }
        }
        ret = func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x1d08,readData, 15,1000);
        LOGE("==========read Data ================= %s" , readData);
        readData = NULL;
    } else if (userMark == 21){//读取 机器SN
        unsigned char * flashId = (unsigned char *)returnData;
        returnData = flashId;
        data[0] = 0x05;
        data[1] = 0x84;
        data[2] = 0x07;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x10;
        data[6] = 0x00;
        data[7] = 0x10;
        ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d00,data, sizeof(data),1000);
        unsigned char status1;
        for(int index = 0;index < 1000;index++){
            func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x0200,&status1, 1,1000);
            if((status1 & 0x01) == 0x00){
                if((status1 & 0x02) == 0x00){
                    break;
                }else if((status1 & 0xFC) != 0x00){
                    RETURN(-1,int);
                    LOGE("=====读取Sn ====RETURN================= ");
                }
            }
        }
        ret = func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x1d08,flashId, 15,1000);
        flashId = NULL;
    }

    RETURN(ret,int);
}

int UVCPreviewIR::getTinyCParams(void * rdata , diy func_diy){
    int ret = UVC_ERROR_IO;
    unsigned char * backData = (unsigned char *) rdata;
    // 发射率
    unsigned char flashId[2] = {0};
    unsigned char data[8] = {0x14,0x85,0x00,0x03,0x00,0x00,0x00,0x00};
    unsigned char data2[8] = {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x02};
    ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x9d00,data, sizeof(data),1000);
    ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d08,data2, sizeof(data2),1000);

    unsigned char status = 0;
    for(int index = 0;index < 1000;index++){
        func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x0200,&status, 1,1000);
        if((status & 0x01) == 0x00){
            if((status & 0x02) == 0x00){
                break;
            }else if((status & 0xFC) != 0x00){
                LOGE("===========================return ==========================");
                RETURN(-1,int);
            }
        }
    }
    ret = func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x1d10,flashId, sizeof(flashId),1000);
    *backData = flashId[0];
    backData++;
    *backData = flashId[1];
    LOGE("发射率 0 ==== %d",flashId[0]);
    LOGE("发射率 1 ==== %d",flashId[1]);

    // 获取 反射温度（已成功）
    status = 0;
    data[3] = 0x01;
    ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x9d00,data, sizeof(data),1000);
    ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d08,data2, sizeof(data2),1000);
    for(int index = 0;index < 1000;index++){
        func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x0200,&status, 1,1000);
        if((status & 0x01) == 0x00){
            if((status & 0x02) == 0x00){
                break;
            }else if((status & 0xFC) != 0x00){
                LOGE("===========================return ==========================");
                RETURN(-1,int);
            }
        }
    }
    ret = func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x1d10,flashId, sizeof(flashId),1000);
    backData++;
    *backData = flashId[0];
    backData++;
    *backData = flashId[1];
    LOGE("反射温度 0 ==== %d",flashId[0]);
    LOGE("反射温度 1 ==== %d",flashId[1]);

    // 获取 大气温度（已成功）
    data[3] = 0x02;
    status = 0;
    ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x9d00,data, sizeof(data),1000);
    ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d08,data2, sizeof(data2),1000);
    for(int index = 0;index < 1000;index++){
        func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x0200,&status, 1,1000);
        if((status & 0x01) == 0x00){
            if((status & 0x02) == 0x00){
                break;
            }else if((status & 0xFC) != 0x00){
                LOGE("===========================return ==========================");
                RETURN(-1,int);
            }
        }
    }
    ret = func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x1d10,flashId, sizeof(flashId),1000);
    backData++;
    *backData = flashId[0];
    backData++;
    *backData = flashId[1];
    LOGE("大气温度 0 ==== %d",flashId[0]);
    LOGE("大气温度 1 ==== %d",flashId[1]);

    // 获取 大气透过率（已成功）
    data[3] = 0x04;
    status = 0;
    ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x9d00,data, sizeof(data),1000);
    ret = func_diy(mDeviceHandle,0x41,0x45,0x0078,0x1d08,data2, sizeof(data2),1000);
    for(int index = 0;index < 1000;index++){
        func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x0200,&status, 1,1000);
        if((status & 0x01) == 0x00){
            if((status & 0x02) == 0x00){
                break;
            }else if((status & 0xFC) != 0x00){
                LOGE("===========================return ==========================");
                RETURN(-1,int);
            }
        }
    }
    ret = func_diy(mDeviceHandle,0xc1,0x44,0x0078,0x1d10,flashId, sizeof(flashId),1000);
    LOGE("大气透过率 0 ==== %d",flashId[0]);
    LOGE("大气透过率 1 ==== %d",flashId[1]);
    backData++;
    *backData = flashId[0];
    backData++;
    *backData = flashId[1];

    backData = NULL;
    RETURN(ret,int);
}


int UVCPreviewIR::stopPreview() {
    ENTER();
    bool b = isRunning();
    if (LIKELY(b)) {
        mIsCapturing=false;
        mIsRunning = false;
//        pthread_cond_signal(&capture_sync);
//        if (pthread_join(capture_thread, NULL) != EXIT_SUCCESS) {
//        	LOGE("UVCPreviewIR::stopPreview capture thread: pthread_join failed");
//        }

        pthread_cond_signal(&preview_sync);
//        int *result_preview_join = NULL;
        if (pthread_join(preview_thread, NULL) != EXIT_SUCCESS)
        {
//            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_failed  result =  %d" ,*result_preview_join);
            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_failed");
        }else{
//            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_SUCCESS result =  %d" ,*result_preview_join);
            LOGE("UVCPreviewIR::stopPreview preview thread: EXIT_SUCCESS");
        }

        if(mIsTemperaturing){
            mIsTemperaturing=false;
            if (pthread_join(temperature_thread, NULL) != EXIT_SUCCESS) {
                LOGE("UVCPreviewIR::stopPreview temperature_thread: pthread_join failed");
            }else{
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
//    delete[] OutBuffer;
//    delete[] HoldBuffer;
//    delete[] RgbaOutBuffer;
//    delete[] RgbaHoldBuffer;
    pthread_mutex_unlock(&preview_mutex);

//    SAFE_DELETE(OutBuffer)
//    SAFE_DELETE(HoldBuffer)
//    SAFE_DELETE(RgbaOutBuffer)
//    SAFE_DELETE(RgbaHoldBuffer)

    LOGE("UVCPreviewIR::stopPreview ============== delete begin ");
//    if(OutBuffer!=NULL){
//        delete[] OutBuffer;
//    }
//    if(HoldBuffer!=NULL){
//        delete[] HoldBuffer;
//    }
//    if(RgbaOutBuffer!=NULL){
//        delete[] RgbaOutBuffer;
//    }
//    if(RgbaHoldBuffer!=NULL){
//        delete[] RgbaHoldBuffer;
//    }
//    LOGE("UVCPreviewIR::stopPreview ============== delete over ");
    RETURN(0, int);
}

//线程 preview_thread
void *UVCPreviewIR::preview_thread_func(void *vptr_args)
{
    int result;
    ENTER();
    UVCPreviewIR *preview = reinterpret_cast<UVCPreviewIR *>(vptr_args);
    if (LIKELY(preview))
    {
        uvc_stream_ctrl_t ctrl;
        result = preview->prepare_preview(&ctrl);
        if (LIKELY(!result))
        {
            preview->do_preview(&ctrl);
        }
    }
    PRE_EXIT();
    pthread_exit(NULL);
}

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
    OutBuffer=new unsigned char[requestWidth*(requestHeight)*2];//分配内存：为啥要*2 在draw_preview_one中会赋值给 unsigned short 类型
    HoldBuffer=new unsigned char[requestWidth*(requestHeight)*2];
    if (mPid == 1 && mVid == 5396){
        RgbaOutBuffer=new unsigned char[requestWidth*(requestHeight-4)*4];
        RgbaHoldBuffer=new unsigned char[requestWidth*(requestHeight-4)*4];
    }else if (mPid == 22592 && mVid == 3034){
        RgbaOutBuffer=new unsigned char[requestWidth*(requestHeight)*4];
        RgbaHoldBuffer=new unsigned char[requestWidth*(requestHeight)*4];
    }

    picOutBuffer=new unsigned char[requestWidth*(requestHeight)*2];
//    picRgbaOutBuffer=new unsigned char[requestWidth*(requestHeight-4)*4];

    mCurrentAndroidVersion=0;

//    LOGE("uvc_get_stream_ctrl_format_size_fps1111111111111");
    result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, ctrl,
                                                 !requestMode ? UVC_FRAME_FORMAT_YUYV : UVC_FRAME_FORMAT_MJPEG,
                                                 requestWidth, requestHeight, requestMinFps, requestMaxFps);
//    LOGE("result：%d",result);
    //LOGE("re:%d,frameSize=(%d,%d)@%d,%d",result, requestWidth, requestHeight, requestMinFps,requestMaxFps);
    if (LIKELY(!result))
    {
#if LOCAL_DEBUG
        uvc_print_stream_ctrl(ctrl, stderr);
#endif
        uvc_frame_desc_t *frame_desc;
        result = uvc_get_frame_desc(mDeviceHandle, ctrl, &frame_desc);
        if (LIKELY(!result))
        {
            frameWidth = frame_desc->wWidth;
            frameHeight = frame_desc->wHeight;
            LOGE("frameSize=(%d,%d)@%s", frameWidth, frameHeight, (!requestMode ? "YUYV" : "MJPEG"));//frameSize=(256,196)@YUYV
            pthread_mutex_lock(&preview_mutex);
            if (LIKELY(mPreviewWindow)) {
                if (mPid == 1 && mVid == 5396){
                    ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                     frameWidth, frameHeight-4, previewFormat);//ir软件384*292中，实质384*288图像数据，4行其他数据
                }else if (mPid == 22592 && mVid == 3034){
                    ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                     frameWidth, frameHeight, previewFormat);//ir软件384*292中，实质384*288图像数据，4行其他数据
                }
//                ANativeWindow_setBuffersGeometry(mPreviewWindow,
//                                                 frameWidth, frameHeight-4, previewFormat);//ir软件384*292中，实质384*288图像数据，4行其他数据
                //LOGE("ANativeWindow_setBuffersGeometry:(%d,%d)", frameWidth, frameHeight);
            }
            pthread_mutex_unlock(&preview_mutex);
        } else {
            frameWidth = requestWidth;
            frameHeight = requestHeight;
        }
        frameMode = requestMode;
        frameBytes = frameWidth * frameHeight * (!requestMode ? 2 : 4);
        LOGE(" frameBytes ==== %d  , requestMode ========  %d", frameBytes , requestMode);
        previewBytes = frameWidth * frameHeight * PREVIEW_PIXEL_BYTES;
    }else{
        LOGE("could not negotiate with camera:err=%d", result);
    }
    RETURN(result, int);
}

//加密 标识符
char * UVCPreviewIR::EncryptTag(char * tag){
    int tagLength = strlen(tag);
    char tagChar[tagLength];
    strcpy(tagChar,tag);

    for (int i = 0; i < tagLength; ++i) {
//        if (tagChar[i] >= 65 && tagChar[i] < 90){
            tagChar[i] = tagChar[i]-25;
//        }
    }
    strcpy(tag,tagChar);
    return tag;
}
//解密标识符
char * UVCPreviewIR::DecryptTag(char * tag){
    int tagLength = strlen(tag);
    char tagChar[tagLength];
    for (int i = 0; i < tagLength; ++i) {
//        if (tagChar[i] >= 65 && tagChar[i] < 90){
        tagChar[i] = tagChar[i]+25;
//        }
    }
    strcpy(tag,tagChar);
    return tag;
}


string replace(string& base, string src, string dst)
{
//    LOGE(" DecryptSN =====步骤十一 ===== >");
    int pos = 0, srclen = src.size(), dstlen = dst.size();
    while ((pos = base.find(src, pos)) && pos!= string::npos) //不能写成 ((pos = base.find(src, pos))!= string::npos)
    {
        base.replace(pos, srclen, dst);
        pos += dstlen;
    }
    return base;
}

//字符串分割函数
std::vector<std::string> split(std::string str, std::string pattern)
{
    std::string::size_type pos;
    std::vector<std::string> result;
    str += pattern;//扩展字符串以方便操作
    int size = str.size();
    for (int i = 0; i < size; i++)
    {
        pos = str.find(pattern, i);
        if (pos < size)
        {
            std::string s = str.substr(i, pos - i);
            result.push_back(s);
            i = pos + pattern.size() - 1;
        }
    }
    return result;
}

// 解密sn
// <param name="sn">用户sn</param>
// <param name="ir_sn">设备sn</param>
// <returns></returns>
void *  UVCPreviewIR::DecryptSN(void * userSn, void * robotSn){
    unsigned char * sn = (unsigned char*)userSn;
    unsigned char * ir_sn = (unsigned char*)robotSn;
    //string str = ir_sn.Replace("JD", "");
//    LOGE(" DecryptSN ir_sn ======= > %s =======**===== > %d",sn, strlen(sn));
//    LOGE(" DecryptSN sn_char ===== > %s",ir_sn);
    string s = (char *)ir_sn;
    s = replace(s,"\0", "0");
//    LOGE(" DecryptSN =====步骤一 ===== >s==== %s" , s.c_str());
    string str = s.substr(2,4);
//    LOGE(" DecryptSN =====步骤一 ===== > str === >  %s" , str.c_str());
//    int sn_sum = strtol(str.c_str(),NULL,0) % 127;
    int sn_sum=  atoi(str.c_str())%127;
//    LOGE("======取值：========》%d", atoi(str.c_str()));
    char strs[sn_length];
//    for (int i = 0; i < strlen(sn); i++) {
//        LOGE(" >>>DecryptSN sn ======= > %d",(int)sn[i]);
//    }
    mempcpy(strs,sn,15);
    int strs_len = strlen(strs);
//    LOGE(" DecryptSN =====步骤四222 ===== > %d", strlen(strs));
    //strcpy(strs,sn);
//    for (int i = 0; i < strs_len; i++) {
//        LOGE(" DecryptSN sn ======= > %d",(int)strs[i]);
//    }
//    LOGE(" DecryptSN =====步骤二 ===== >strs ===>  %s==%d==%d " ,strs,strlen(strs),strlen(sn));
    LOGE(" DecryptSN =====步骤二 ===== >sn_sum ===>  %d " ,sn_sum);
    strs[0] = (char)(strs[0] ^ 18);
    strs[8] = (char)(strs[8] ^ 18);
    strs[11] = (char)(strs[11] ^ sn_sum);
    strs[3] = (char)(strs[3] ^ sn_sum);
    strs[7] = (char)(strs[7] ^ 18);
    strs[9] = (char)(strs[9] ^ sn_sum);
//    LOGE(" DecryptSN =====步骤三 ===== >%d==%d",strs[9],strs_len);
    for (int i = 0; i < strs_len; i++)
    {
        strs[i] = (char)(strs[i] ^ 29);
    }
    char a;
    // 前后0,2,4与7+0,7+2,7+4兑换
    for (int i = 0; i < 5; i += 2)
    {
        a = strs[i];
        strs[i] = strs[7 + i];
        strs[7 + i] = a;
    }
//    LOGE(" DecryptSN =====步骤四 ===== >");
//    LOGE(" DecryptSN =====步骤四 ===== > %d", strs_len);
    for (int i = 0; i < 7; i += 3)
    {
        if (i == 0)
        {
            strs[i] = (char)(strs[i] ^ strs[strs_len - 3]);
            strs[i + 1] = (char)(strs[i + 1] ^ strs[strs_len - 2]);
            strs[i + 2] = (char)(strs[i + 2] ^ strs[strs_len - 1]);
        }
        else if (i == 3)
        {
            strs[i] = (char)(strs[i] ^ strs[strs_len - 1]);
            strs[i + 1] = (char)(strs[i + 1] ^ strs[strs_len - 2]);
            strs[i + 2] = (char)(strs[i + 2] ^ strs[strs_len - 3]);
        }
        else
        {
            strs[i] = (char)(strs[i] ^ strs[i + 3]);
            strs[i + 1] = (char)(strs[i + 1] ^ strs[i + 4]);
            strs[i + 2] = (char)(strs[i + 2] ^ strs[i + 5]);
        }
    }
//    for (int index = 0 ; index < 15; index ++) {
//        LOGE(" DecryptSN =====步骤五 ==strs=== >%s" ,strs);
//    }
//    strcpy(sn,strs);
    mempcpy(sn,strs,15);
    char * returnSn ;
    returnSn= (char *)(sn + 15);
    *returnSn = 0;
    LOGE("===== returnSn ========== %s ========" ,returnSn);

    sn = NULL;
    ir_sn = NULL;
    returnSn = NULL;
//    for (int i = 0; i < strs_len; i++) {
//        LOGE(" DecryptSN sn ======= > %d",(int)sn[i]);
//    }
    return userSn;
}

//DYTCA10D DYTCA10Q DYTCA10L
const char g_key[17] = "dyt1101c";
const char g_iv[17] = "gfdertfghjkuyrtg";//ECB MODE不需要关心chain，可以填空
string EncryptionAES(const string& strSrc) //AES加密
{
    size_t length = strSrc.length();
    int block_num = length / BLOCK_SIZE + 1;
    //明文
    char* szDataIn = new char[block_num * BLOCK_SIZE + 1];
    memset(szDataIn, 0x00, block_num * BLOCK_SIZE + 1);
    strcpy(szDataIn, strSrc.c_str());

    //进行PKCS7Padding填充。
    int k = length % BLOCK_SIZE;
    int j = length / BLOCK_SIZE;
    int padding = BLOCK_SIZE - k;
    for (int i = 0; i < padding; i++)
    {
        szDataIn[j * BLOCK_SIZE + k + i] = padding;
    }
    szDataIn[block_num * BLOCK_SIZE] = '\0';

    //加密后的密文
    char *szDataOut = new char[block_num * BLOCK_SIZE + 1];
    memset(szDataOut, 0, block_num * BLOCK_SIZE + 1);

    //进行进行AES的CBC模式加密
    AES aes;
    aes.MakeKey(g_key, g_iv, 16, 16);
    aes.Encrypt(szDataIn, szDataOut, block_num * BLOCK_SIZE, AES::CBC);
    string str = base64_encode((unsigned char*) szDataOut,
                               block_num * BLOCK_SIZE);
    delete[] szDataIn;
    delete[] szDataOut;

    return str;
}
string DecryptionAES(const string& strSrc) //AES解密
{
    string strData = base64_decode(strSrc);
    size_t length = strData.length();
    //密文
    char *szDataIn = new char[length + 1];
    memcpy(szDataIn, strData.c_str(), length+1);
    //明文
    char *szDataOut = new char[length + 1];
    memcpy(szDataOut, strData.c_str(), length+1);

    //进行AES的CBC模式解密
    AES aes;
    aes.MakeKey(g_key, g_iv, 8, 16);
    aes.Decrypt(szDataIn, szDataOut, length, AES::CBC);

    //去PKCS7Padding填充
    if (0x00 < szDataOut[length - 1] <= 0x16)
    {
        int tmp = szDataOut[length - 1];
        for (int i = length - 1; i >= length - tmp; i--)
        {
            if (szDataOut[i] != tmp)
            {
                memset(szDataOut, 0, length);
                LOGE( "去填充失败！解密出错！！");
//                cout << "去填充失败！解密出错！！" << endl;
                break;
            }
            else
                szDataOut[i] = 0;
        }
    }
    string strDest(szDataOut);
    delete[] szDataIn;
    delete[] szDataOut;
    return strDest;
}

int  flag = 0;

/**
 *
 * @param ctrl
 */
void UVCPreviewIR::do_preview(uvc_stream_ctrl_t *ctrl) {
    ENTER();
    ////LOGE("do_preview");
    //uvc_stop_streaming(mDeviceHandle);
    LOGI("======================do_preview======================");
    uvc_error_t result = uvc_start_streaming_bandwidth(mDeviceHandle, ctrl, uvc_preview_frame_callback, (void *)this, requestBandwidth, 0);
    if (LIKELY(!result)){
        //pthread_create(&capture_thread, NULL, capture_thread_func, (void *)this);
        //pthread_create(&temperature_thread, NULL, temperature_thread_func, (void *)this);
#if LOCAL_DEBUG
        LOGI("Streaming...");
#endif
        // yuvyv mode
        for ( ; LIKELY(isRunning()) ; )
        {
            //LOGE("do_preview0");
            pthread_mutex_lock(&preview_mutex);
            {
                //LOGE("get data and show wupei");
                //等待数据 初始化到位,之后运行下面的代码。
                pthread_cond_wait(&preview_sync, &preview_mutex);
//                 LOGE("waitPreviewFrame02============================");
                if (isCopyPicturing()){//判断截屏
                    LOGE("======mutex===========");
                    memset(picOutBuffer,0,256*196*2);
                    memcpy(picOutBuffer,OutBuffer,256*196*2);
                    mIsCopyPicture = false;
                    signal_save_picture_thread();
                }

                uint8_t *tmp_buf=NULL;
                //if(OutPixelFormat==3)//RGBA 32bit输出
                mIsComputed=false;
                //S0机芯 获取机芯的参数，环境温度  反射率 等等 用于 温度对照表。
                if (mPid == 1 && mVid == 5396){
                    mFrameImage->getCameraPara(HoldBuffer);
                }
                // swap the buffers rgba
                tmp_buf =RgbaOutBuffer;
                RgbaOutBuffer= RgbaHoldBuffer;
                RgbaHoldBuffer=tmp_buf;
                //初始时outbuffer里面没有数据了，数据给holdbuffer，自己指向原来holdbuffer分配的内存首地址
                //char强转成short类型 长度由requestWidth*requestHeight*2 变成requestWidth*requestHeight
//                unsigned short* orgData=(unsigned short*)HoldBuffer;// char a数组[1,2] 经过强转short可能变成513(256*2+1)，或258(256*1+2)。

                if (isVerifySN()) {
                    char *dytSn;
                    string dytSnStr;
                    if (mPid == 22592 && mVid == 3034){//tinyC机芯
                        snIsRight = true;
                        unsigned char * robotSN = (unsigned char *)malloc(sizeof (char )*15);
                        unsigned char * userSn = (unsigned char *)malloc(sizeof (char )*15);
                        unsigned char * userSnSixLast = robotSN + 6;
                        int pa = 0x8004;
//                        sendTinyCAllOrder(&pa,uvc_diy_communicate,100);

                        sendTinyCAllOrder(robotSN,uvc_diy_communicate,21);
                        sendTinyCAllOrder(userSn,uvc_diy_communicate,20);

                        sendTinyCAllOrder(&pa,uvc_diy_communicate,100);
    //                    LOGE("========robotSn == %s=========",robotSN);
    //                    LOGE("========userSn == %s=========",userSn);
    //                    LOGE("========userSnSixLast == %s=========",userSnSixLast);
                        dytSn =(char *)DecryptSN(userSn, userSnSixLast);
    //                    LOGE("==============destr ============= %s", dytSn);
                        dytSnStr = dytSn;
                        dytSnStr = dytSnStr.substr(0, 8);
    //                    LOGE("========robotSn == %s=========",robotSN);
    //                    LOGE("========userSn == %s=========",userSn);
                        free(robotSN);
                        free(userSn);
                        robotSN = NULL;
                        userSn = NULL;
                        userSnSixLast = NULL;
                    }else  if (mVid == 5396 && mPid==1){//S0机芯

                        //根据 标识  去设置是否渲染 画面 读取SN号
    //                    if (isVerifySN()) {
                            unsigned char *fourLinePara = HoldBuffer + ((256 * (192)) << 1);
                            int amountPixels = (256 << 1);

                            int userArea = amountPixels + (127 << 1);
                            memcpy((void *) &machine_sn, fourLinePara + amountPixels + (32 << 1),
                                   (sizeof(char) << 5)); // ir序列号
                            memcpy((void *) &user_sn, fourLinePara + userArea + 100,
                                   sizeof(char) * sn_length);//序列号
    //                        LOGE(" ir_sn ======= > %s",machine_sn);
    //                        LOGE(" sn_char ===== > %s",user_sn);
                            //解密之后的数据
                            dytSn =(char *)DecryptSN(user_sn, machine_sn);
                            LOGE("==============destr ============= %s", dytSn);
                            dytSnStr = dytSn;
                            dytSnStr = dytSnStr.substr(0, 8);
    //                    LOGE("==============ss ============= %s",ss.c_str());
                            //释放用到的指针资源
                            fourLinePara = NULL;
                    }

                    if (!snIsRight){
                        FILE *inFile = NULL;
                        inFile = fopen(
                                "/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/configs.txt",
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
    //                        LOGE("sssss======File length =======> %d",length);
                            if (fread(fileStore, length, 1, inFile) != length) {
                            }
                            fclose(inFile);
                        }
                        //切割结果
                        std::vector<std::string> split_result = split(fileStore, ";");
                        LOGE("==================configs.txt split size == %d=========",split_result.size());
                        //遍历 结果 是否 符合筛选
                        for (int i = 0; i < split_result.size(); i++) {
                            string decryptionSplitChild = DecryptionAES(split_result[i]).substr(0,8);
                            if (dytSnStr == decryptionSplitChild) {
                                LOGE("=============SN匹配成功========");
                                snIsRight = snIsRight | 1;
                            } else {
                                LOGE("==============SN匹配不成功========");
                                snIsRight = snIsRight | 0;
                            }
                        }

                        inFile = NULL;
                        free(fileStore);
                        fileStore = NULL;
                    }
                    dytSn = NULL;
                    //分支结束 之前将标识 设置为 false
                    mIsVerifySn = false;
                }

                //判断完了之后去 渲染成图
                if (snIsRight){//s0 的 sn 是否符合规定。
                    draw_preview_one(HoldBuffer, &mPreviewWindow, NULL, 4);
                }

                tmp_buf=NULL;

                mIsComputed=true;
            }
            pthread_mutex_unlock(&preview_mutex);

            if(mIsTemperaturing)//绘制的时候唤醒温度绘制的线程
            {
                //LOGE("do_preview1");
                pthread_cond_signal(&temperature_sync);
            }
            //LOGE("do_preview4");
        }
        //pthread_cond_signal(&capture_sync);
#if LOCAL_DEBUG
        LOGI("preview_thread_func:wait for all callbacks complete");
#endif
        uvc_stop_streaming(mDeviceHandle);
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
void UVCPreviewIR::uvc_preview_frame_callback(uint8_t *frameData, void *vptr_args,size_t hold_bytes)
{
//    LOGE("   ======tid= %d  =====pid =  %d",gettid(),getpid());
    //LOGE("uvc_preview_frame_callback00");
    UVCPreviewIR *preview = reinterpret_cast<UVCPreviewIR *>(vptr_args);
    //unsigned short* tmp_buf=(unsigned short*)frameData;
    //LOGE("uvc_preview_frame_callback00  tmp_buf:%d,%d,%d,%d",tmp_buf[384*144*4],tmp_buf[384*144*4+1],tmp_buf[384*144*4+2],tmp_buf[384*144*4+3]);
    //LOGE("uvc_preview_frame_callback hold_bytes:%d,preview->frameBytes:%d",hold_bytes,preview->frameBytes);

    if(UNLIKELY(hold_bytes < preview->frameBytes))//判断hold_bytes 不小于preview.frameBytes则跳转到 后面正常运行     UNLIKELY期待值大几率为false时,等价于if(value)
    {   //有手机两帧才返回一帧的数据。
        LOGE("uvc_preview_frame_callback hold_bytes ===> %d < preview->frameBytes  ==== > %d" , hold_bytes, preview->frameBytes);
        return;
    }
//    char i = frameData[0];

//    LOGE("======================uvc_preview_frame_callback======================");
    //preview->isComputed()是否绘制完
    if(LIKELY( preview->isRunning() && preview->isComputed()))
    {
//        pthread_mutex_lock(&preview->data_callback_mutex);
//        {
//        LOGE("======================uvc_preview_frame_callback01======================");
//            unsigned  char * thedata = preview->OutBuffer;

            //void *memcpy(void *destin, void *source, unsigned n);从源source中拷贝n个字节到目标destin中
            memcpy(preview->OutBuffer,frameData,(preview->requestWidth)*(preview->requestHeight)*2);

//        LOGE("===================frameSize ===========%s",sizeof(OutBuffer));
            //LOGE("uvc_preview_frame_callback02");
            /* swap the buffers org */
//            LOGE("======callback===11111111111111111111111========");
            uint8_t* tmp_buf = NULL;
            tmp_buf =preview->OutBuffer;
            preview->OutBuffer=preview->HoldBuffer;
            preview->HoldBuffer = tmp_buf;
//            LOGE("======callback===222222222222222222222222222=======");
            tmp_buf=NULL;
            preview->signal_receive_frame_data();
//        }
//        pthread_mutex_unlock(&preview->data_callback_mutex);
    }
    //LOGE("uvc_preview_frame_callback03");
}
void UVCPreviewIR::signal_receive_frame_data()
{
    //唤醒 preview_thread    线程
    pthread_cond_signal(&preview_sync);
}
void UVCPreviewIR::signal_save_picture_thread()
{
    //唤醒 screenShot_thread    截屏线程
    pthread_cond_signal(&screenShot_sync);
}

//绘制每一帧的图像
void UVCPreviewIR::draw_preview_one(uint8_t *frameData, ANativeWindow **window, convFunc_t convert_func, int pixcelBytes)
{
    if (mCurrentAndroidVersion == 0) {
        RgbaHoldBuffer = mFrameImage->onePreviewData(frameData);
    }
    if (LIKELY(*window)) {
        copyToSurface(RgbaHoldBuffer, window);
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
            // source = frame data
            const uint8_t *src = frameData;
            const int src_w = requestWidth * PREVIEW_PIXEL_BYTES;//256*4  代表一行 颜色 的RBGA值
            const int src_step = src_w; //一次绘制一行？
            // destination = Surface(ANativeWindow)
            uint8_t *dest = (uint8_t *)buffer.bits;
            const int dest_w = buffer.width * PREVIEW_PIXEL_BYTES;
            const int dest_step = buffer.stride * PREVIEW_PIXEL_BYTES;
            // use lower transfer bytes
            const int w = src_w < dest_w ? src_w : dest_w;//取两者的最小值
            // use lower height
            const int h = frameHeight < buffer.height ? frameHeight : buffer.height;//取两者的最小值
            //LOGE("copyToSurface");
            // transfer from frame data to the Surface
            //LOGE("copyToSurface:w:%d,h,%d",w,h);
//            if (requestHeight ==196){
            mFrameImage->copyFrame(src, dest, w, h, src_step, dest_step);
//            }else{
//                mFrameImage->copyFrameTO292(src, dest, w, h, src_step, dest_step);
//            }
//            copyFrame(src, dest, w, h, src_step, dest_step);
            src=NULL;
            dest=NULL;
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
//    return result;
    RETURN(result, int);
}
/*************************************预览 结束****************************************************/

/***************************************截屏相关  开始**************************************/
//不同的
//int UVCPreviewIR::setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format)
//{
//    //LOGE("setFrameCallback01");
//    LOGE("setFrameCallback步骤8");
//    pthread_mutex_lock(&capture_mutex);
//    {
//        mFrameImage->setFrameCallback(env,frame_callback_obj,pixel_format);
//    }
//    pthread_mutex_unlock(&capture_mutex);
//    RETURN(0, int);
//}

//int UVCPreviewIR::startCapture(){
//    ENTER();
//    pthread_mutex_lock(&screenShot_mutex);
//    {
//        if (isRunning()&&(!mIsCapturing))
//        {
//            //LOGE("startTemp");
//            mIsCapturing = true;
//        }
//    }
//    pthread_mutex_unlock(&screenShot_mutex);
//    if(pthread_create(&screenShot_thread, NULL, screenShot_thread_func, (void *)this)==0){
//        //LOGE("UVCPreviewIR::startCapture capture_thread: pthread_create success");
//    }else{
//        //LOGE("UVCPreviewIR::startCapture capture_thread: pthread_create failed");
//    }
//    RETURN(0, int);
//}

int UVCPreviewIR::savePicture(const char *path) {
//    LOGE("UVCPreviewIR savePicture  pathlength ==   %d" , strlen(path));
    mIsCopyPicture = true;
//    memset(savePicPath,'0',strlen(savePicPath));
    memcpy(&savePicPath,path,strlen(path));

    if(pthread_create(&screenShot_thread, NULL, screenShot_thread_func, (void *)this)==0){
        LOGE("UVCPreviewIR::savePicture screenShot_thread_func: pthread_create success");
    }else{
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
    if(LOCAL_DEBUG){LOGE("capture_thread_func");}
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
    if(isRunning()){
        pthread_mutex_lock(&screenShot_mutex);
        {
          LOGE("=======do_savePicture======pthread_cond_wait====");
            pthread_cond_wait(&screenShot_sync,&screenShot_mutex);
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
    memset(fileHead.reserved,0,16);
    len += sizeof(fileHead);
//    LOGE("===D_FILE_HEAD=========%d",sizeof(fileHead));

    // 热数据说明
    unsigned short * adPicbuffer = (unsigned short *) picOutBuffer;

    D_IR_DATA_PAR irDataPar;
    irDataPar.ir_w = 256;
    irDataPar.ir_h = 192;
    int fourLineIndex = 256*192;

    irDataPar.raw_max = adPicbuffer[fourLineIndex+4];
    irDataPar.raw_min = adPicbuffer[fourLineIndex+7];
//    irDataPar.raw_max = 8000;
//    irDataPar.raw_min = 5000;

    irDataPar.pointSize = 2;
    irDataPar.typeAndSize = (1 << 4) | 4;;
    memset(irDataPar.reserved,0,18);
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
    memcpy(cameraInfo.devName, devName.c_str(),4);
    memcpy(cameraInfo.devSN,devSN.c_str(),18);
    memcpy(cameraInfo.devFirmware,devf.c_str(),12);
    memset(cameraInfo.reserved,0,52);
    len += sizeof(cameraInfo);
//    LOGE("===D_CAMERA_INFO=========%d",sizeof(cameraInfo));

    // 传感器信息
    D_SENSOR_INFO sensorInfo;
    string senName = "sensor1234567890";
    memcpy(sensorInfo.sensorName,senName.c_str(),16);
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
    memcpy(lensInfo.lensName,lensName.c_str(),16);
    memcpy(lensInfo.lensPn, lensPn.c_str(), 16);
    memcpy(lensInfo.lensSn, lensSn.c_str(), 16);
    lensInfo.HFoV = 55.5f;
    lensInfo.VFov = 33.3f;
    lensInfo.Transm = 88.8;
    lensInfo.FocalLength = 5;
    lensInfo.length = 8;
    lensInfo.Aperture = 77.7;
    memset(lensInfo.reserved,0,20);
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
    memset(cameraPar.reserved,0,44);
    int siezPar = sizeof(cameraPar);
    len += sizeof(cameraPar);
//    LOGE("===D_CAMERA_PAR=========%d",sizeof(cameraPar));

    // 图像信息
    D_IMAGE_INFO imageInfo;
    imageInfo.tempUnit = 1;
    imageInfo.lutCode = 10;
    memset(imageInfo.reserved,0,42);
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
    memset(fuseInfo.reserved,0,10);
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
    for (int i = 0; i < 1024;i++) {
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
//    LOGE("===D_SHAPE_INFO=========%d",sizeof(dShapeInfo));
//    LOGE("===D_SHAPE_INFO==此时len=======%d",len);

//    len += (sizeof(D_POINT) * 2);
//    len += (sizeof(D_LINE) * 3);
//    len += (sizeof(D_REC) * 2);
//    len += (sizeof(D_POLYGON) * 2);

//    D_POINT p1;
//    D_POINT p2;
//    p1.AD = 1111;
//    p1.distance[0] = 1;
//    p1.distance[1] = 1;
//    p1.emissivity = 11;
//    p1.point.x = 555;
//    p1.point.y = 555;
//    p1.reflexTemp = 0.11;
//    p1.shapeTyte = 1;
//    p1.temp = 11;
//
//    p2.AD = 2222;
//    p2.distance[0] = 2;
//    p2.distance[1] = 2;
//    p2.emissivity = 22;
//    p2.point.x = 333;
//    p2.point.y = 333;
//    p2.reflexTemp = 0.22;
//    p2.shapeTyte = 1;
//    p2.temp = 22;

//    D_LINE l1;
//    D_LINE l2;
//    D_LINE l3;
//
//    l1.avgTemp = 11;
//    l1.emissivity = 11;
//    l1.maxAD = 1111;
//    l1.maxPoint.x = 11;
//    l1.maxPoint.y = 11;
//    l1.maxTemp = 111;
//    l1.minAD = 111;
//    l1.minPoint.x = 1;
//    l1.minPoint.y = 1;
//    l1.minTemp = 11;
//    l1.points[0].x = 1;
//    l1.points[0].y = 1;
//    l1.points[1].x = 11;
//    l1.points[1].y = 11;
//    l1.shapeTyte = 2;
//    l1.size = 2;
//
//    l2.avgTemp = 22;
//    l2.emissivity = 22;
//    l2.maxAD = 2222;
//    l2.maxPoint.x = 22;
//    l2.maxPoint.y = 22;
//    l2.maxTemp = 222;
//    l2.minAD = 222;
//    l2.minPoint.x = 2;
//    l2.minPoint.y = 2;
//    l2.minTemp = 22;
//    l2.points[0].x = 2;
//    l2.points[0].y = 2;
//    l2.points[1].x = 22;
//    l2.points[1].y = 22;
//    l2.points[2].x = 222;
//    l2.points[2].y = 222;
//    l2.shapeTyte = 2;
//    l2.size = 3;
//
//    l3.avgTemp = 33;
//    l3.emissivity = 33;
//    l3.maxAD = 3333;
//    l3.maxPoint.x = 33;
//    l3.maxPoint.y = 33;
//    l3.maxTemp = 333;
//    l3.minAD = 333;
//    l3.minPoint.x = 3;
//    l3.minPoint.y = 3;
//    l3.minTemp = 33;
//    l3.points[0].x = 3;
//    l3.points[0].y = 3;
//    l3.points[1].x = 33;
//    l3.points[1].y = 33;
//    l3.points[2].x = 333;
//    l3.points[2].y = 333;
//    l3.points[3].x = 3333;
//    l3.points[3].y = 3333;
//    l3.shapeTyte = 2;
//    l3.size = 4;

//    D_REC r1;
//    D_REC r2;
//    r1.avgTemp = 11;
//    r1.emissivity = 11;
//    r1.maxAD = 111;
//    r1.maxPoint.x = 11;
//    r1.maxPoint.y = 11;
//    r1.maxTemp = 1111;
//    r1.minAD = 11;
//    r1.minPoint.x = 111;
//    r1.minPoint.y = 111;
//    r1.minTemp = 111;
//    r1.points[0].x = 11;
//    r1.points[0].y = 11;
//    r1.points[1].x = 111;
//    r1.points[1].y = 111;
//    r1.shapeTyte = 3;
//
//    r2.avgTemp = 22;
//    r2.emissivity = 22;
//    r2.maxAD = 222;
//    r2.maxPoint.x = 22;
//    r2.maxPoint.y = 22;
//    r2.maxTemp = 2222;
//    r2.minAD = 22;
//    r2.minPoint.x = 222;
//    r2.minPoint.y = 222;
//    r2.minTemp = 222;
//    r2.points[0].x = 22;
//    r2.points[0].y = 22;
//    r2.points[1].x = 222;
//    r2.points[1].y = 222;
//    r2.shapeTyte = 3;

//    D_POLYGON dpo1;
//    D_POLYGON dpo2;
//    dpo1.avgTemp = 11;
//    dpo1.emissivity = 11;
//    dpo1.maxAD = 1111;
//    dpo1.maxPoint.x = 11;
//    dpo1.maxPoint.y = 11;
//    dpo1.maxTemp = 111;
//    dpo1.minAD = 111;
//    dpo1.minPoint.x = 1;
//    dpo1.minPoint.y = 1;
//    dpo1.minTemp = 11;
//    dpo1.points[0].x = 1;
//    dpo1.points[0].y = 1;
//    dpo1.points[1].x = 11;
//    dpo1.points[1].y = 11;
//    dpo1.shapeTyte = 5;
//    dpo1.size = 2;
//
//    dpo2.avgTemp = 22;
//    dpo2.emissivity = 22;
//    dpo2.maxAD = 2222;
//    dpo2.maxPoint.x = 22;
//    dpo2.maxPoint.y = 22;
//    dpo2.maxTemp = 222;
//    dpo2.minAD = 222;
//    dpo2.minPoint.x = 2;
//    dpo2.minPoint.y = 2;
//    dpo2.minTemp = 22;
//    dpo2.points[0].x = 2;
//    dpo2.points[0].y = 2;
//    dpo2.points[1].x = 22;
//    dpo2.points[1].y = 22;
//    dpo2.points[2].x = 222;
//    dpo2.points[2].y = 222;
//    dpo2.shapeTyte = 5;
//    dpo2.size = 3;

    LOGE("the len ==============   %d ", len);

    fileHead.offset = len;//结构体的长度。即偏移量。  用于快速的到ad值位置的偏移量

    /******************开始合并自定义的数据集合**********************/
    int dataLen = irDataPar.ir_w * irDataPar.ir_h * 2;//ad值的长度
    // 开辟等长内存
    unsigned char* dataParBuf = (unsigned char*)malloc(len+ dataLen);//dataParBuf   的长度  为包装自定义信息加上 ad值的长度
    unsigned char* old_dataParBuf = dataParBuf;
    int cpyLen = 0;//以拷贝的长度

    //从自定义结构体长度开始拷贝
    cpyLen = sizeof(fileHead);//得到fileHead对象结构体 的长度
    //memset(void *s ,int ch , size_t n );//将s中当前位置后面的n个字节，用 ch 替换 并返回s;
    memcpy(dataParBuf,&fileHead,cpyLen);//从fileHead 从拷贝 cpylen个字节到 dataParBuf 中
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
    memcpy(dataParBuf, &dShapeInfo,cpyLen);
    dataParBuf += cpyLen;

//    cpyLen = sizeof(p1);
//    memcpy(dataParBuf, &p1, cpyLen);
//    dataParBuf += cpyLen;
//    cpyLen = sizeof(p2);
//    memcpy(dataParBuf, &p2, cpyLen);
//    dataParBuf += cpyLen;
//
//
//    cpyLen = sizeof(l1);
//    memcpy(dataParBuf, &l1, cpyLen);
//    dataParBuf += cpyLen;
//    cpyLen = sizeof(l2);
//    memcpy(dataParBuf, &l2, cpyLen);
//    dataParBuf += cpyLen;
//    cpyLen = sizeof(l3);
//    memcpy(dataParBuf, &l3, cpyLen);
//    dataParBuf += cpyLen;
//
//
//    cpyLen = sizeof(r1);
//    memcpy(dataParBuf, &r1, cpyLen);
//    dataParBuf += cpyLen;
//    cpyLen = sizeof(r1);
//    memcpy(dataParBuf, &r1, cpyLen);
//    dataParBuf += cpyLen;
//
//
//    cpyLen = sizeof(dpo1);
//    memcpy(dataParBuf, &dpo1, cpyLen);
//    dataParBuf += cpyLen;
//    cpyLen = sizeof(dpo2);
//    memcpy(dataParBuf, &dpo2, cpyLen);
//    dataParBuf += cpyLen;//此时dataParBuf里面塞满了 自定义的结构体数据


    //dataLen等于 ad值的长度 即w*h*2(没有减去4)
    //unsigned char* dataBuf = (unsigned char*)malloc(dataLen);
//    int val = 0;
    for (int i = 0; i < dataLen;i++) {
//        if (i !=0 && (i % 256)==0) {
//            val++;
//        }
//        dataParBuf[i] = val;
        dataParBuf[i] = picOutBuffer[i];
    }
//    memcpy(dataParBuf, picOutBuffer, 256*192*2);

    LOGE("====================dataLen===============%d",dataLen);
    LOGE("====================len===============%d",len);

    int fileLen = len + dataLen;
    LOGE("====================fileLen===============%d",fileLen);
    int result = D_saveData(savePicPath,old_dataParBuf,fileLen);
    if (result){
        //todo java 截屏成功方法
    } else{
        //todo java  截屏失败
    }
    dataParBuf = NULL;
    free(old_dataParBuf);
    old_dataParBuf = NULL;
}
/***************************************录制相关 结束**************************************/

/***********************************温度相关 开始******************************************************/
//传递一个温度回调的对象，还传递一个回调的方法，实现通用
int UVCPreviewIR::setTemperatureCallback(JNIEnv *env,jobject temperature_callback_obj){
    ENTER();
    //LOGE("setTemperatureCallback01");
    LOGE("setTemperatureCallback步骤9");
    pthread_mutex_lock(&temperature_mutex);
    {
        mFrameImage->setTemperatureCallback(env,temperature_callback_obj);
    }
    pthread_mutex_unlock(&temperature_mutex);
    RETURN(0, int);
}
int UVCPreviewIR::startTemp(){
    ENTER();
    pthread_mutex_lock(&temperature_mutex);
    {
        if (isRunning()&&(!mIsTemperaturing)){
            mIsTemperaturing = true; //LOGE("startTemp");
        }
    }
    pthread_mutex_unlock(&temperature_mutex);
    //在这里开始传递测温数据
    if(pthread_create(&temperature_thread, NULL, temperature_thread_func, (void *)this)==0)
    {
        //LOGE("UVCPreviewIR::startTemp temperature_thread: pthread_create success");
    }else{
        //LOGE("UVCPreviewIR::startTemp temperature_thread: pthread_create failed");
    }
    RETURN(0, int);
}
int UVCPreviewIR::stopTemp(){
    ENTER();
    pthread_mutex_lock(&temperature_mutex);
    {
        if (isRunning() && mIsTemperaturing)
        {
            LOGE("stopTemp");
            mIsTemperaturing = false;
            pthread_cond_signal(&temperature_sync);
            pthread_cond_wait(&temperature_sync, &temperature_mutex);	// wait finishing Temperatur
        }
    }
    pthread_mutex_unlock(&temperature_mutex);
    if (pthread_join(temperature_thread, NULL) != EXIT_SUCCESS){
        LOGE("UVCPreviewIR::stopTemp temperature_thread: pthread_join failed");
    }else{
        LOGE("UVCPreviewIR::stopTemp temperature_thread: pthread_join success");
    }
    RETURN(0, int);
}

void *UVCPreviewIR::temperature_thread_func(void *vptr_args)
{
    int result;
    LOGE("temperature_thread_func步骤0");
    ENTER();
    UVCPreviewIR *preview = reinterpret_cast<UVCPreviewIR *>(vptr_args);
    if (LIKELY(preview))
    {
        JavaVM *vm = getVM();
        JNIEnv *env;
        //attach to JavaVM
        vm->AttachCurrentThread(&env, NULL);
        LOGE("temperature_thread_func do_temperature");
        preview->do_temperature(env);	// never return until finish previewing
        //detach from JavaVM
        vm->DetachCurrentThread();
        MARK("DetachCurrentThread");
    }
    PRE_EXIT();
    pthread_exit(NULL);
}
void UVCPreviewIR::do_temperature(JNIEnv *env)
{
    ENTER();
    LOGE("do_temperature温度步骤3");
    ////LOGE("do_temperature mIsTemperaturing:%d",mIsTemperaturing);
    for (;isRunning()&&mIsTemperaturing;){
        pthread_mutex_lock(&temperature_mutex);
        {
//            LOGE("do_temperature01");
            pthread_cond_wait(&temperature_sync, &temperature_mutex);
//            LOGE("do_temperature02");
            if(mIsTemperaturing)
            {
                do_temperature_callback(env, HoldBuffer);
            }
//            LOGE("do_temperature03");
        }
        pthread_mutex_unlock(&temperature_mutex);
    }
    pthread_cond_broadcast(&temperature_sync);
//    LOGE("do_temperature EXIT");
//    EXIT();
}

//读取原始数据holdbuffer .将原始YUV数据查表之后的温度数据回调。10+ 256*192
void UVCPreviewIR::do_temperature_callback(JNIEnv *env, uint8_t *frameData)
{
//   LOGE("=========do_temperature_callback ======");
    if (mPid == 1 && mVid == 5396){
        mFrameImage->do_temperature_callback(env,frameData);
    } else if (mPid == 22592 && mVid == 3034){
        mFrameImage->do_temperature_callback(env,frameData);
    }

}
/***************************************温度相关 结束**************************************/


void UVCPreviewIR::clearDisplay() {//
    ENTER();
//LOGE("clearDisplay");
    ANativeWindow_Buffer buffer;
//    pthread_mutex_lock(&capture_mutex);
//    {
//        if (LIKELY(mCaptureWindow)) {
//            if (LIKELY(ANativeWindow_lock(mCaptureWindow, &buffer, NULL) == 0)) {
//                uint8_t *dest = (uint8_t *)buffer.bits;
//                const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
//                const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
//                for (int i = 0; i < buffer.height; i++) {
//                    memset(dest, 0, bytes);
//                    dest += stride;
//                }
//                ANativeWindow_unlockAndPost(mCaptureWindow);
//            }
//        }
//    }
//    pthread_mutex_unlock(&capture_mutex);
    pthread_mutex_lock(&preview_mutex);
    {
        if (LIKELY(mPreviewWindow)) {
            if (LIKELY(ANativeWindow_lock(mPreviewWindow, &buffer, NULL) == 0)) {
                uint8_t *dest = (uint8_t *)buffer.bits;
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

int UVCPreviewIR:: getByteArrayTemperaturePara(uint8_t* para){
    if (mPid == 1 && mVid == 5396){
        mFrameImage->getByteArrayTemperaturePara(para,HoldBuffer);
    }
    return true;
}

//void UVCPreviewIR::setCameraLens(int mCameraLens){
//    ENTER();
//    cameraLens=mCameraLens;
//    EXIT();
//}