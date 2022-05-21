//
// Created by stefa on 2021/5/6.
//

#ifndef CAMERA_FRAMEIMAGE_H
#define CAMERA_FRAMEIMAGE_H

#include <android/native_window.h>
#include "libUVCCamera.h"
#include "objectarray.h"

#define OUTPUTMODE 4
#define PREVIEW_PIXEL_BYTES 4

#define DEFAULT_PREVIEW_WIDTH 640
#define DEFAULT_PREVIEW_HEIGHT 480
#define DEFAULT_PREVIEW_FPS_MIN 1
#define DEFAULT_PREVIEW_FPS_MAX 30
#define DEFAULT_PREVIEW_MODE 0
#define DEFAULT_BANDWIDTH 1.0f

typedef struct {
    jmethodID onReceiveTemperature;
} Fields_iTemperatureCallback;//结构体: 温度回调时回调java层对象的onReceiveTemperature函数
typedef struct {
    jmethodID onFrame;
} Fields_iframecallback;	//结构体：包含录制时回调的java层对象的 函数名

typedef uvc_error_t (*convFunc_t)(uvc_frame_t *in, uvc_frame_t *out);
/**
 * 图像帧处理类
 */
class FrameImage {

private:
//    ANativeWindow *mPreviewWindow;
    uvc_device_handle_t *mDeviceHandle;
    unsigned char *mBuffer;//显示图像的buffer大小 width*(height-4)*4

    int requestWidth, requestHeight ,amountPixels;//请求宽度，请求高度，请求模式，像素数量
    int requestMode;
    bool mIsPaletteChanged;
    char resPath[100];
    int mTypeOfPalette=0;
	unsigned char palette[256*3];
	const unsigned char* currentpalette;
	const unsigned char* DYgetPalette(int typeOfPalette);

	bool mIsAreachecked = false;
	int mCheckArea[12];
	int areasize = 0;//区域检查绘制灰度图

	unsigned short max , min ,ro;//图幅的最大值，最小值，差值
    //温度范围更改：

	unsigned short maxpercent;//画板绘制的 最高的百分比，eg:最低高低差100度，百分比为90，则90-100均为一个 RGBA值
	unsigned short minpercent;//画板绘制的 最低的百分比
	float maxThumbValue;//最大值滑块 百分比对应温度
	float minThumbValue;//最小值滑块 百分比对应温度
	volatile bool isFixedTempStrip;//是否固定温度条
	volatile bool isNeedFreshAD;
	volatile int minThumbAD;
	volatile int maxThumbAD;
	int roThumb;
	int frameWidth;
	int frameHeight;
//	pthread_mutex_t fixed_mutex;
//	pthread_cond_t  fixed_cond;
//	float tempData[16384];
//	pthread_t  fixed_thread;
    int tinyCorrection = 0;

	int mCurrentAndroidVersion;     //标志是否使用OpenCL加速渲染成图
	/***********************温度************************************/
	//温度线程
	volatile bool isNeedWriteTable;//是否刷新 温度对照表
	bool isshowtemprange;//是否绘制温度范围的取值，否则绘制 最低温到最高温 的差异
	float temperatureTable[16384];//存储温度对照表的 数组

	//			fpa温度(初始化后无修改),温度整体修正值,反射温度,环境温度，湿度，发射率，距离
	float floatFpaTmp , correction, Refltmp, Airtmp, humi, emiss, distance;
    int cameraLens;//设置，镜头大小:目前支持两种，68：使用6.8mm镜头，130：使用13mm镜头,默认130。
    float shutterFix;//设置，快门校正，一般为0.
	int rangeMode;//设置，测温范围：120：温度范围为-20-120摄氏度。400：温度范围为-20-400摄氏度,另外人体测温产品T3H也使用这种模式
	float mCbTemper[640*512+10];//回调给Java层的温度数据，大小是不是太大了//初始化设置小
	jobject mTemperatureCallbackObj;//java 回调接口的对象
	Fields_iTemperatureCallback iTemperatureCallback;


    char sn[32];//camera序列码
    char cameraSoftVersion[16];//camera软件版本
    unsigned short shutTemper;
    float floatShutTemper;//快门温度
    unsigned short coreTemper;
    float floatCoreTemper;//外壳温度
	//设备类型 vid pid
	int mVid;
	int mPid;

	//读写文件的操作
//	int file_count = 0;
//	int file_count_limit = 50;
//	volatile bool isWriteFile = false;


	/**********************录制*******************************/
//	Fields_iframecallback iframecallback_fields;//对java层回调的函数onFrame
//	jobject mFrameCallbackObj;
//	int OutPixelFormat;//回调图像输出形式,rgba=0, 原始输出或者yuyv=1


//	int mcount = 0;
public:
    FrameImage();
    FrameImage(uvc_device_handle_t *devh);
    ~FrameImage();
    /*************************************测试函数**********************************/
    bool setIsWriteFile(int status);

    /************************************操作 设置 类*********************************************/
    void setResourcePath(const char* path);//设置画板的资源路径
    void setPreviewSize(int width ,int height ,int mode);
	unsigned char*  onePreviewData(uint8_t* frameData);
    void copyFrame(const uint8_t *src, uint8_t *dest, const int width, int height, const int stride_src, const int stride_dest);
	void copyFrameTO292(const uint8_t *src, uint8_t *dest, const int width, int height, const int stride_src, const int stride_dest);
	//滑动条更改高低温显示的百分比
	void showTempRange( float maxPercent,float minPercent,float maxValue ,float minValue);
	void disTempRange();
	void fixedTempStripChange(bool state);
	//设置区域检查
	void setAreaCheck(int isAreaCheck);
	void setArea(int* area,int lenght);
	void changePalette(int typeOfPalette);//更改了色板底层调用
//	void whenShutRefresh();
	void getCameraPara(uint8_t *frame);//得到机芯的参数,用于查表(温度对照表)
	int getByteArrayTemperaturePara(uint8_t* para ,uint8_t * data);
	void setVidPid(int vid ,int pid);

	/*******************************温度数据*******************************************/
	int setTemperatureCallback(JNIEnv *env, jobject temperature_callback_obj);//设置温度回调对象
	void do_temperature_callback(JNIEnv *env, uint8_t *frameData);//设置温度回调
	void shutRefresh();

	int getDichotomySearch( const float * data, int length ,float value, int startIndex, int endIndex);//二分法查找

/*******************************录制*******************************************/
//	int setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format);
//	void do_capture_callback(JNIEnv *env, uint8_t *frame);


	//视频录制的
	/*static void *capture_thread_func(void *vptr_args);
	void do_capture(JNIEnv *env);
	void do_capture_surface(JNIEnv *env);
	void do_capture_idle_loop(JNIEnv *env);
	void do_capture_callback(JNIEnv *env, uint8_t *frame);*/
};

#endif //CAMERA_FRAMEIMAGE_H
