
#ifndef UVCPREVIEW_IR_H_
#define UVCPREVIEW_IR_H_

#include "libUVCCamera.h"
#include <pthread.h>
#include <android/native_window.h>
#include "objectarray.h"
#include "cstring"




#define DEFAULT_PREVIEW_WIDTH 640
#define DEFAULT_PREVIEW_HEIGHT 480
#define DEFAULT_PREVIEW_FPS_MIN 1
#define DEFAULT_PREVIEW_FPS_MAX 30
#define DEFAULT_PREVIEW_MODE 0
#define DEFAULT_BANDWIDTH 1.0f



typedef uvc_error_t (*convFunc_t)(uvc_frame_t *in, uvc_frame_t *out);
struct irBuffer//使用专业级图像算法所需要的缓存
{
	size_t** midVar;
	unsigned char* destBuffer;
};
#define PIXEL_FORMAT_RAW 0		// same as PIXEL_FORMAT_YUV
#define PIXEL_FORMAT_YUV 1
#define PIXEL_FORMAT_RGB565 2
#define PIXEL_FORMAT_RGBX 3
#define PIXEL_FORMAT_YUV20SP 4
#define PIXEL_FORMAT_NV21 5		// YVU420SemiPlanar

#include "FrameImage.h"

class UVCPreviewIR{
private:
	FrameImage *mFrameImage;
	uvc_device_handle_t *mDeviceHandle;
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


	ANativeWindow *mPreviewWindow;

	int requestWidth, requestHeight, requestMode;//amountPixels
	int requestMinFps, requestMaxFps;
	float requestBandwidth;
	int frameWidth, frameHeight;
	int frameMode;
	unsigned char *OutBuffer;//使用完的buffer; 获取到新数据后,与holdBuffer互换。
	unsigned char *HoldBuffer;// 充满新数据的buffer
	unsigned char *RgbaOutBuffer;//上一张图幅
	unsigned char *RgbaHoldBuffer;//当前图幅
	irBuffer* irBuffers;//使用专业级图像算法所需要的缓存
	size_t frameBytes;

//	int OutPixelFormat;//回调图像输出形式,rgba=0, 原始输出或者yuyv=1
	//预览画面线程、 条件变量、 互斥变量
	pthread_t preview_thread;
	pthread_cond_t preview_sync;
	pthread_mutex_t preview_mutex;
	void signal_receive_frame_data();//唤醒preview_thread线程
	int previewFormat;
	size_t previewBytes;
	int mcount ;
	int mCurrentAndroidVersion;     //标志是否使用OpenCL加速渲染成图

//	//SN 校验
	volatile bool mIsVerifySn = true; // 是否去刷新 SN
	inline const bool isVerifySN() const;
	bool snIsRight = false;//是否显示画面的标识
	int sn_length = 15 ;
	char machine_sn[32];//设备的SN号
	char user_sn[20];//用户区的 SN号
//	string theTag[2] = {"+@;*(\u0018\u0017+","+@;*(\u0018\u00178"};
//	char myVerify[40];//存储的SN号




	int mPixelFormat;
	/*****************************录制 拍照相关,不负责具体实现 ********************************/
	pthread_mutex_t data_callback_mutex;//初始化数据 互斥锁 互斥量

	bool isRecording;
	ANativeWindow *mCaptureWindow;
	//截屏线程 条件变量  互斥变量
	pthread_t screenShot_thread;
	pthread_cond_t screenShot_sync;
	pthread_mutex_t screenShot_mutex;
    static void *screenShot_thread_func(void *vptr_args);//screenShot_thread线程的具体定义
	void signal_save_picture_thread();//唤醒preview_thread线程
	void do_savePicture();//保存照片的函数
	void savePicDefineData();//保存自定义结构体数据到图片里面

	volatile bool mIsCapturing;
	int OutPixelFormat;

/*****************************预览画面相关  函数**********************************************/
	static void *preview_thread_func(void *vptr_args);//preview_thread 线程的具体定义
	int prepare_preview(uvc_stream_ctrl_t *ctrl);
	void do_preview(uvc_stream_ctrl_t *ctrl);
	static void uvc_preview_frame_callback(uint8_t *frame, void *vptr_args,size_t hold_bytes);//数据来源
	void draw_preview_one(uint8_t* frameData, ANativeWindow **window, convFunc_t func, int pixelBytes);
	void clearDisplay();
	int copyToSurface(uint8_t *frameData, ANativeWindow **window);
	//SN 相关 ，sn 用户区 SN, ir_sn 设备sn
	char * DecryptSN(char * sn, char * ir_sn);
//	string replace(string& base, string src, string dst);
	//加密 标识符
	char * EncryptTag(char * tag);
	//解密标识符
	char * DecryptTag(char * tag);

/****************************温度 私有函数 变量********************************/

	static void *temperature_thread_func(void *vptr_args);//temperature_thread 线程的具体定义
	void do_temperature(JNIEnv *env);
	void do_temperature_callback(JNIEnv *env, uint8_t *frameData);
	//温度线程  条件变量  互斥变量
	pthread_t temperature_thread;
	pthread_cond_t temperature_sync;
	pthread_mutex_t temperature_mutex;

//	pthread_mutex_t fixed_mutex;

	int mTypeOfPalette=0;
	bool mIsTemperaturing;//是否去绘制温度信息

    float mCbTemper[640*512+10];//回调给Java层的温度数据，大小是不是太大了
    unsigned short detectAvg;


public:
    UVCPreviewIR();
	UVCPreviewIR(uvc_device_handle_t *devh ,FrameImage * frameImage);
	~UVCPreviewIR();
/***************************预览*****************************/
//    void whenShutRefresh();
	int setPreviewSize(int width, int height, int min_fps, int max_fps, int mode, float bandwidth ,int currentAndroidVersion);
	int setPreviewDisplay(ANativeWindow *preview_window);
    int startPreview();
    int stopPreview();
	void setVidPid(int vid ,int pid);

/***************************录制*****************************/
//	int setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format);//把当前数据回调给Java层
//	int stopCapture();
//    int startCapture();
    int savePicture(const char* path);

	void fixedTempStripChange(bool state);

/***************************温度*****************************/
	int stopTemp();
	int startTemp();
	int setTemperatureCallback(JNIEnv *env, jobject temperature_callback_obj);//关联温度回调接口 的对象
	int getByteArrayTemperaturePara(uint8_t* para);//得到机芯参数

};

#endif /* UVCPREVIEW_H_ */
