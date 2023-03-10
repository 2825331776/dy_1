
#ifndef UVCPREVIEW_IR_H_
#define UVCPREVIEW_IR_H_

#include "libUVCCamera.h"
#include <pthread.h>
#include <android/native_window.h>
#include "objectarray.h"
#include "cstring"
//#include "toojpeg.h"



#include "AES.h"
#include "jpegext.h"
#define LOG_TAG "===UVCPREVIEW_IR==="

#define DEFAULT_PREVIEW_WIDTH 640
#define DEFAULT_PREVIEW_HEIGHT 480
#define DEFAULT_PREVIEW_FPS_MIN 1
#define DEFAULT_PREVIEW_FPS_MAX 30
#define DEFAULT_PREVIEW_MODE 0
#define DEFAULT_BANDWIDTH 1.0f

//自定义UVC通讯接口
typedef uvc_error_t (*diy)(uvc_device_handle_t *devh, uint8_t request_type, uint8_t bRequest,
                           uint16_t wValue, uint16_t wIndex, unsigned char *data, uint16_t wLength,
                           unsigned int timeout);

typedef uvc_error_t (*convFunc_t)(uvc_frame_t *in, uvc_frame_t *out);

typedef struct {
    jmethodID onUVCCurrentStatus;
} Fields_iUVCStatusCallback;//结构体: 温度回调时回调java层对象的onReceiveTemperature函数

typedef struct {
    jmethodID onUpdateMedia;
} Fields_iUVCUpdateMedia; //结构体:回调 媒体库更新方法


//S0发送指令函数
typedef uvc_error_t (*paramset_func_u16)(uvc_device_handle_t *devh, uint16_t value);

struct irBuffer//使用专业级图像算法所需要的缓存
{
    size_t **midVar;
    unsigned char *destBuffer;
};
#define PIXEL_FORMAT_RAW 0        // same as PIXEL_FORMAT_YUV
#define PIXEL_FORMAT_YUV 1
#define PIXEL_FORMAT_RGB565 2
#define PIXEL_FORMAT_RGBX 3
#define PIXEL_FORMAT_YUV20SP 4
#define PIXEL_FORMAT_NV21 5        // YVU420SemiPlanar

#include "FrameImage.h"

class UVCPreviewIR {
private:
    FrameImage *mFrameImage;
    uvc_device_handle_t *mDeviceHandle;
    volatile bool isReleased = false;

    inline const bool isRunning() const;

    inline const bool isComputed() const;

    inline const bool isCopyPicturing() const;

    volatile bool mIsComputed;
    volatile bool mIsRunning;
    /******************截屏**********************/
    volatile bool mIsCopyPicture;//是否在拷贝图片
    unsigned char *picOutBuffer;//生成图片时保存的原始数据
//	unsigned char *picRgbaOutBuffer;// 截屏时， 拷贝的rgba数据
    char savePicPath[100];
    //设备类型 vid pid
    int mVid;
    int mPid;
    volatile int UVC_STATUS = -1;
    jobject mUvcStatusCallbackObj = NULL;//java 回调接口的对象
    Fields_iUVCStatusCallback iUvcStatusCallback;
    char dyt_flag[3];

//更新媒体库
    jobject mUpdateMediaObj = NULL;//java 回调接口的对象
    Fields_iUVCUpdateMedia  iUpdateMedia;

    ANativeWindow *mPreviewWindow;

    int requestWidth, requestHeight, requestMode;//amountPixels
    int requestMinFps, requestMaxFps;
    float requestBandwidth;
    int frameWidth, frameHeight;
    int frameMode;
    char app_private_path[100];

//	int callbackCount;
//	unsigned char *CacheBuffer;
    unsigned char *OutBuffer;//使用完的buffer; 获取到新数据后,与holdBuffer互换。
    unsigned char *HoldBuffer;// 充满新数据的buffer
    unsigned char *RgbaOutBuffer;//上一张图幅
    unsigned char *RgbaHoldBuffer;//当前图幅
    unsigned char *backUpBuffer;
    unsigned char JPEGBuffer;
    irBuffer *irBuffers;//使用专业级图像算法所需要的缓存
    size_t frameBytes;

//	int OutPixelFormat;//回调图像输出形式,rgba=0, 原始输出或者yuyv=1
    //预览画面线程、 条件变量、 互斥变量
    pthread_t preview_thread;
    pthread_cond_t preview_sync;
    pthread_mutex_t preview_mutex;

    void signal_receive_frame_data();//唤醒preview_thread线程
    int previewFormat;
    size_t previewBytes;
    int mcount;
    int mCurrentAndroidVersion;     //标志是否使用OpenCL加速渲染成图

//	//SN 校验
    volatile bool mIsVerifySn = true; // 是否去刷新 SN
    inline const bool isVerifySN() const;
    int sn_verify_count = 0;

    volatile bool snIsRight = false;//是否显示画面的标识
    inline const bool isSnRight() const;

    int sn_length = 15;
    char machine_sn[32];//设备的SN号
    char user_sn[20];//用户区的 SN号

    const char * app_private_file_path_log = "/storage/emulated/0/Android/data/com.dytest.wcc/files/dy_test_log.txt";

    unsigned char dytTinyCSn[15];
    unsigned char TinyUserSN[15];
    unsigned char TinyRobotSn[15];

    //tinyc 加密之后的 机器sn号
    unsigned char encryption_robot_sn[15];

//	Tinyc使用锁 相关变量
//    pthread_t tinyC_send_order_thread; //tinyc发送指令的线程
//    pthread_cond_t tinyC_send_order_sync;//tinyc线程 条件变量
//    pthread_mutex_t tinyC_send_order_mutex;//tinyc线程 互斥量
    int getTinyCDevicesStatus();//获取TinyC机芯的状态
    int getTinyCParams(void *returnData, diy func_diy);//获取tinyc 机芯参数。仅获取
    int
    getTinyCParams_impl(void *reData, diy func_diy, unsigned char data[8], unsigned char data2[8]);

    int sendTinyCOrder(uint32_t *value, diy func_diy);// tinyc 打挡  获取数据 纯标识位 指令
    int sendTinyCParamsModification(float *value, diy func_diy, uint32_t mark);//tinyc 机芯参数 修改
    int getTinyCUserData(void *returnData, diy func_diy, int userMark);//读取用户区数据

//    int tinyCControl();

    int doTinyCOrder();

    void *tinyC_params;
    volatile int tinyC_mark;
    //表示是否是第一次连接。
    volatile bool is_first_run ;


    int mPixelFormat;
    /*****************************录制 拍照相关,不负责具体实现 ********************************/
//    pthread_mutex_t data_callback_mutex;//初始化数据 互斥锁 互斥量

    bool isRecording;
    ANativeWindow *mCaptureWindow;
    //截屏线程 条件变量  互斥变量
    pthread_t screenShot_thread;
    pthread_cond_t screenShot_sync;
    pthread_mutex_t screenShot_mutex;

    static void *screenShot_thread_func(void *vptr_args);//screenShot_thread线程的具体定义
    void signal_save_picture_thread();//唤醒preview_thread线程
    void do_savePicture(JNIEnv * env);//保存照片的函数
    void savePicDefineData();//保存自定义结构体数据到图片里面

    volatile bool mIsCapturing;
    int OutPixelFormat;

    void doRobotSnEncryption();

/*****************************预览画面相关  函数**********************************************/
    static void *preview_thread_func(void *vptr_args);//preview_thread 线程的具体定义
    int prepare_preview(uvc_stream_ctrl_t *ctrl);

    void do_preview(uvc_stream_ctrl_t *ctrl);

    static void
    uvc_preview_frame_callback(uint8_t *frame, void *vptr_args, size_t hold_bytes);//数据来源
    void
    draw_preview_one(uint8_t *frameData, ANativeWindow **window, convFunc_t func, int pixelBytes);

    inline const bool IsRotateMatrix_180() const;

    volatile bool isRotateMatrix_180 = false;//是否旋转180
    //旋转180度
    void rotateMatrix_180(short *src_frameData, short dst_frameData[], int width, int height);

    void clearDisplay();

    int copyToSurface(uint8_t *frameData, ANativeWindow **window);

    //SN 相关 ，sn 用户区 SN, ir_sn 设备sn
    void *DecryptSN(void *sn, void *ir_sn, void *returnData);

//	string replace(string& base, string src, string dst);
    //加密 标识符
    char *EncryptTag(char *tag);

    //解密标识符
    char *DecryptTag(char *tag);

/****************************温度 私有函数 变量********************************/

    static void *temperature_thread_func(void *vptr_args);//temperature_thread 线程的具体定义
    void do_temperature(JNIEnv *env);

    void do_temperature_callback(JNIEnv *env, uint8_t *frameData);

    //温度线程  条件变量  互斥变量
    pthread_t temperature_thread;
    pthread_cond_t temperature_sync;
    pthread_mutex_t temperature_mutex;
    //uvc状态回调 互斥变量。
//    pthread_mutex_t uvc_status_mutex;

//	pthread_mutex_t fixed_mutex;

    int mTypeOfPalette = 0;
    bool mIsTemperaturing;//是否去绘制温度信息

    float mCbTemper[640 * 512 + 10];//回调给Java层的温度数据，大小是不是太大了
    unsigned short detectAvg;

    //打挡策略  只有在确定出了预览图之后生效
    int general_block_strategy_frame_interval = 25 * 60 * 10;//十分钟
    int all_frame_count = 0;//总帧率计数器
    int newADValue;//现 打挡 AD值
    int oldADValue;//旧 打挡 AD值
    //so 打挡的值
    int s0_value_difference = 15;//打挡的差值
    //TinyC 当打策略
    int tinyC_frame_count = 0;
    int tinyC_block_order_interval = 25;//读取指令间隔
    int tinyC_block_value_difference = 5;//差值间隔

    volatile int sendCount = 0;

public:
    UVCPreviewIR();

    UVCPreviewIR(uvc_device_handle_t *devh, FrameImage *frameImage);

    ~UVCPreviewIR();
/***************************预览*****************************/
//    void whenShutRefresh();
    int setPreviewSize(int width, int height, int min_fps, int max_fps, int mode, float bandwidth,
                       int currentAndroidVersion);

    int setPreviewDisplay(ANativeWindow *preview_window);

    int startPreview();

    int stopPreview();

    void setVidPid(int vid, int pid);

    int setIsVerifySn();

    int sendTinyCAllOrder(void *params, diy func_tinyc, int mark);

    bool snRightIsPreviewing();

    void setRotateMatrix_180(bool isRotate);//设置是否旋转180

    //2022年5月17日16:46:17 设置机芯参数
    bool setMachineSetting(int value,
                            int mark);

    //2022年5月17日16:46:17 获取机芯参数
    float getMachineSetting(int flag, int value,
                            int mark);

    void setResourcePath(const char* private_path);//设置软件的私有路径地址

/***************************录制*****************************/
//	int setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format);//把当前数据回调给Java层
//	int stopCapture();
//    int startCapture();
    int savePicture(const char *path);

    void fixedTempStripChange(bool state);

    void setTinySaveCameraParams();

/***************************温度*****************************/
    int stopTemp();

    int startTemp();
    //add by 吴长城 获取UVC连接状态回调
    int setUVCStatusCallBack(JNIEnv *env, jobject uvc_connect_status_callback);

    //add by 吴长城 获取UVC连接状态回调
    int setUpdateMediaCallBack(JNIEnv *env, jobject update_media_callback_obj);

    int setTemperatureCallback(JNIEnv *env, jobject temperature_callback_obj);//关联温度回调接口 的对象
    int getByteArrayTemperaturePara(uint8_t *para);//得到机芯参数


    void testJNI(const char * phoneStr);

    void shutRefresh();

    int DYT_DeleteAPP2(const char* fileName) ;
};

#endif /* UVCPREVIEW_H_ */
