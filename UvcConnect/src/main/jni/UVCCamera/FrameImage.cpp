//
// Created by stefa on 2021/5/6.
//

#include "FrameImage.h"
#include "pthread.h"
#include <cstring>
#include "UVCPreviewIR.h"

#include <iostream>
#include <fstream>
using namespace std;

FrameImage::FrameImage(uvc_device_handle_t *devh)  {
            mPreviewWindow = NULL;
            mDeviceHandle = devh;
            requestWidth = DEFAULT_PREVIEW_WIDTH;
            requestHeight = DEFAULT_PREVIEW_HEIGHT;
            requestMode = DEFAULT_PREVIEW_MODE;

            mIsAreachecked = false;
            isshowtemprange = false;
            mTypeOfPalette = 1;
            isFixedTempStrip = false;
            /**
             * void *memset(void *s, int ch, size_t n);
              函数解释：将s中当前位置后面的n个字节 （typedef unsigned int size_t ）用 ch 替换并返回 s 。
             */
            memset(palette, 0, 3*256*sizeof(unsigned char));
            memset(cameraSoftVersion, 0, 16);

//            pthread_mutex_init(&temperature_mutex,NULL);
              isNeedWriteTable=true;//是否需要刷新温度对照表，刚开始要刷新。
              floatFpaTmp = correction = Refltmp = Airtmp = humi = emiss = distance =0;
              cameraLens=130;
              shutterFix=0;
              rangeMode=120;//可更改
              mTemperatureCallbackObj = NULL;

              maxThumbAD =0;
              minThumbAD = 0;
//              pthread_mutex_init(&fixed_mutex,NULL);
//              pthread_cond_init(&fixed_cond,NULL);

             isNeedFreshAD = false;

//              mFrameCallbackObj = NULL;
//              OutPixelFormat=3;
}

FrameImage::~FrameImage() {
    ENTER();
//    pthread_mutex_destroy(&temperature_mutex);
    delete [] mBuffer;
//    pthread_mutex_destroy(&fixed_mutex);
//    pthread_cond_destroy(&fixed_cond);
    EXIT();
}

//通过resPath，拿到色板的的数据流
const unsigned char* FrameImage::DYgetPalette(int typeOfPalette) {
    char name[12];
    sprintf(name, "/%d.dat", typeOfPalette);
    char path[100];
    strcpy(path, resPath);
    strcat(path, name);
    ifstream ifs(path, std::ios::binary | std::ios::ate);
    ifs.seekg(0, ios::beg);
    if (ifs) {
        ifs.read((char *) palette, sizeof(char) * 256 * 3);
    }
    ifs.close();
    return palette;
}

/**********************************用户操作区 开始*************************************/
/**********************************************************************************/
void FrameImage::setResourcePath(const char* path){
    ENTER();
    strcpy(resPath,path);
    EXIT();
}
//打快门 更新 温度对照表
void FrameImage::shutRefresh()
{
    isNeedWriteTable=true;
//    LOGE("===================shutRefresh=============update isNeedWriteTable===============");
}
//二分法查找
int FrameImage::getDichotomySearch(const float* data, int length,float  value, int startIndex,
                                   int endIndex) {
//    LOGE("=================    > %f" , data[8000]);
//    LOGE("enter :  length =  %d, value = %f , startIndex = %d  , endIndex = %d" ,length,value , startIndex , endIndex);
    if (endIndex > length) return -1;
    int start = startIndex;
    int end = endIndex -1;//下标
    int valueIndex = -1;

    if (end<=0)return -1 ;
    while (start <= end){
        int midPont = (start + end+1)/2;//记录的是坐标，所以要在取中点之前+1
        if (value == data[midPont]){//如果中点坐标的值 恰好等于 目标值，则返回这个 中点坐标
            valueIndex = midPont;
            return valueIndex;
        }

        if (value > data[midPont] ){
            start = midPont +1;
            if ((start) <= end && value < data[start]){
                valueIndex = midPont;
                return valueIndex;
            }
        }
        if (value < data[midPont]){
            end = midPont -1;
            if ((end) >= start && value > data[end]){//中点値小志 目标值。但中点値前一位的值又大于了目标值。则返回中点前一位的坐标
                valueIndex = end;
                return valueIndex;
            }
        }
    }
    LOGE("exit");
//    position = NULL;
    return valueIndex;
}
//更改色板
void FrameImage::changePalette(int typeOfPalette){
    mTypeOfPalette=typeOfPalette;
    mIsPaletteChanged= true;
}
//温幅最高最低修改  温度访问改变
void FrameImage::showTempRange(float maxPercent,float minPercent,float maxValue ,float minValue){
    isshowtemprange =true;
    maxpercent = (unsigned  short )maxPercent;
    minpercent = (unsigned  short )minPercent;
//    pthread_mutex_lock(&fixed_mutex);{
        maxThumbValue = maxValue;
        minThumbValue = minValue;
        isNeedFreshAD = true;
//    }
//    pthread_mutex_unlock(&fixed_mutex);
    LOGE( " isFixedTempStrip   === > %d" , isFixedTempStrip );
//    if (isFixedTempStrip) {
////        //todo 查询最大值滑块的温度 对应的ad值 ；最小值滑块对应的 ad值
//        maxThumbAD = getDichotomySearch(temperatureTable,16384,&maxThumbValue,1000,16384);
//        minThumbAD = getDichotomySearch(temperatureTable,16384,&minThumbValue,1000,16384);
//        roThumb = maxThumbAD - minThumbAD;
//        LOGE(" maxThumbAD =   %d  minThumbAD = %d  roThumb =   %d" , maxThumbAD, minThumbAD ,roThumb);
//    }
    LOGE("temp maxThumbValue = %f , min == %f" ,maxThumbValue,minThumbValue);
}
void FrameImage::disTempRange() {//在下一帧图像绘制的时候就不会绘制,是否是拉温宽
    isshowtemprange = false;
}
void FrameImage::fixedTempStripChange(bool state) {
    isNeedFreshAD = true;
    if (state){
        isFixedTempStrip = true;
    }
    else{
        isFixedTempStrip = false;
    }
//    LOGE(" fixed temp strip  state =  %d" ,isFixedTempStrip);
}
void FrameImage::setArea(int *area, int lenght) {//设置区域检查的区域大小
    for(int i=0;i<lenght;i++){
        mCheckArea[i]=*(area + i);
    }
    areasize=lenght;
    if (areasize > 0){
        setAreaCheck(true);
    } else{
        setAreaCheck(false);
    }
}
void FrameImage::setAreaCheck(int isAreaCheck) {//是否设置区域检查
    if(isAreaCheck){
        mIsAreachecked= true;
    } else{
        mIsAreachecked= false;
    }
}

void FrameImage::getCameraPara(uint8_t *frame){
    unsigned short* orgData=(unsigned short*)frame;// char a数组[1,2] 经过强转short可能变成513(256*2+1)，或258(256*1+2)。

    unsigned short* fourLinePara=orgData+requestWidth*(requestHeight-4);//后四行参数
//    LOGE("cpyPara  amountPixels:%d ",amountPixels);
    memcpy(&shutTemper,fourLinePara+amountPixels+1,sizeof(unsigned short));
//    LOGE("cpyPara  shutTemper:%d ",shutTemper);
    floatShutTemper=shutTemper/10.0f-273.15f;//快门片
    memcpy(&coreTemper,fourLinePara+amountPixels+2,sizeof(unsigned short));//外壳
//    LOGE("cpyPara  coreTemper:%d ",coreTemper);
    floatCoreTemper=coreTemper/10.0f-273.15f;
//    LOGE("cpyPara  floatShutTemper:%f,floatCoreTemper:%f,floatFpaTmp:%f\n",floatShutTemper,floatCoreTemper,floatFpaTmp);
    memcpy((uint8_t*)cameraSoftVersion,fourLinePara+amountPixels+24,16*sizeof(uint8_t));//camera soft version
//    LOGE("cameraSoftVersion:%s\n",cameraSoftVersion);
    memcpy((uint8_t*)sn,fourLinePara+amountPixels+32,32*sizeof(uint8_t));//SN
//    LOGE("sn:%s\n",sn);
    int userArea=amountPixels+127;

    memcpy(&correction,fourLinePara+userArea,sizeof( float));//修正
    userArea=userArea+2;
    memcpy(&Refltmp,fourLinePara+userArea,sizeof( float));//反射温度
    userArea=userArea+2;
    memcpy(&Airtmp,fourLinePara+userArea,sizeof( float));//环境温度
    userArea=userArea+2;
    memcpy(&humi,fourLinePara+userArea,sizeof( float));//湿度
    userArea=userArea+2;
    memcpy(&emiss,fourLinePara+userArea,sizeof( float));//发射率
    userArea=userArea+2;
    memcpy(&distance,fourLinePara+userArea,sizeof(float));//距离
//    LOGE("<<<<<<<<<<correction==%f Refltmp==%f Airtmp==%f humi==%f emiss==%f distance==%f\n",correction,Refltmp,Airtmp,humi,emiss,distance);
}

/*
在这里可以返回测温相关参数
fix       float 0-3
Refltmp   float 3-7
Airtmp    float 7-11
humi      float 11-15
emiss     float 15-19
distance  float  20-23
version          112-127
*/
int FrameImage:: getByteArrayTemperaturePara(uint8_t* para , uint8_t* data){
    uint8_t* TempPara;
    switch (requestWidth)
    {
        case 384:
            TempPara=data+(requestWidth*(requestHeight-1)+127)*2;
            break;
        case 240:
            TempPara=data+(requestWidth*(requestHeight-3)+127)*2;
            break;
        case 256:
            TempPara=data+(requestWidth*(requestHeight-3)+127)*2;//倒数第二行的 127个元素，一行256个
            break;
        case 640:
            TempPara=data+(requestWidth*(requestHeight-1)+127)*2;
            break;
    }
    memcpy(para, TempPara, 128*sizeof(uint8_t));//拷贝第二行后一半short 数据到 para中
    //TempPara=TempPara-127*2+24*2;//version
    float dis=0;
    memcpy(&dis, TempPara+20, sizeof(float));
    LOGE("getByteArrayTemperaturePara dis:%f",dis);
    memcpy(para+128-16, cameraSoftVersion, 16*sizeof(uint8_t));//倒数16个字节储存 相机版本号
    //for(int j=0;j<16;j++){
    //LOGE("getByteArrayTemperaturePara version:%c",TempPara[j]);
    //}
    //LOGE("getByteArrayTemperaturePara:%d,%d,%d,%d,%d,%d",para[16],para[17],para[18],para[19],para[20],para[21]);
    return true;
}
/**********************************用户操作区 结束*************************************/

/*********************************预览方法区  开始**************************************/
void FrameImage::setPreviewSize(int width,int height ,int mode ){
    requestWidth = width,requestHeight = height;
    requestMode = mode;
    mBuffer = new unsigned char[requestWidth*(requestHeight-4)*4];

    switch (requestWidth)
        {
            case 384:
                amountPixels=requestWidth*(4-1);
                break;
            case 240:
                amountPixels=requestWidth*(4-3);
                break;
            case 256:
                amountPixels=requestWidth*(4-3);
                break;
            case 640:
                amountPixels=requestWidth*(4-1);
                break;
        }
}

//根据色板去渲染出一帧的画面
unsigned char* FrameImage::onePreviewData(uint8_t* frameData) {
//    LOGE("=========================onePreviewData==========================================");
    unsigned short *tmp_buf = (unsigned short *) frameData;
    /*if(mcount < 2){
        FILE* outFile = NULL;
        outFile =fopen("/storage/emulated/0/Android/data/com.Infrared.camera/files/twodata.txt", "a");
        if(outFile != NULL)
        {
            for (int i = 0; i < (196); i++) {
                for (int j = 0; j < 256; j++) {
                    fprintf(outFile, "%d ", *tmp_buf++);
                    if((i==193) && (j==127)){
                        fprintf(outFile, "\n ");
                    }
                }
                fprintf(outFile, "\n ");
            }
            fclose(outFile);
        } else{
            LOGE("===========文件创建失败============================================");
        }
        mcount++;
    }*/


    /**
     * 渲染逻辑： 功能需求： 框内细查， 非框内细查， 固定温度条。
     * 固定温度条的时候
     *
     */
    if (mIsPaletteChanged){
        currentpalette = DYgetPalette(mTypeOfPalette);
        mIsPaletteChanged = false;
    }
    //获取图幅中的 最大最小AD值
    int amountPixels1 = requestWidth*(requestHeight-4);
    amountPixels1 = amountPixels1 + 4;//倒数第四行的 第四位
    max = tmp_buf[amountPixels1];
    amountPixels1 = amountPixels1 + 3;//倒数第四行的 第七位
    min = tmp_buf[amountPixels1];
    ro = max - min;

    if (isshowtemprange) {//拉温宽
        //LOGE("非区域检查+拉温宽");
        min = (int) (min + ro * minpercent / 100);
        ro = (int) (ro * (maxpercent - minpercent) / 100);
    }
//    LOGE(" isfixed temp strip  == %d",isFixedTempStrip);
    if (isFixedTempStrip){//固定温度条
        roThumb = maxThumbAD - minThumbAD;
        min = minThumbAD;
        ro = roThumb;
    }

    //框内细查 先绘制灰度图,根据原有的ad值
    if (mIsAreachecked){
        int loopnum=areasize/4;

        if (loopnum != 0){//有框
            for (int i = 0; i < requestHeight - 4; i++) {
                for (int j = 0; j < requestWidth; j++) {
                    int gray = (int) (255 * (tmp_buf[i * requestWidth + j] - min * 1.0) / ro);
                    if (gray < 0) {
                        gray = 0;
                    }
                    if (gray > 255) {
                        gray = 255;
                    }
                    mBuffer[4 * (i * requestWidth + j)] = gray;
                    mBuffer[4 * (i * requestWidth + j) + 1] = gray;
                    mBuffer[4 * (i * requestWidth + j) + 2] = gray;
                    mBuffer[4 * (i * requestWidth + j) + 3] = 1;
                }
            }
        } else{//无框。直接渲染成 彩色
            for (int i = 0; i < requestHeight - 4; i++) {
                for (int j = 0; j < requestWidth; j++) {
//              LOGE("this requestHeight and requestwidth==============%d========================%d",requestHeight,requestWidth);
                    //黑白：灰度值0-254单通道。 paletteIronRainbow：（0-254）×3三通道。两个都是255，所以使用254
//              LOGE("====================%d======================",tmp_buf[i * requestWidth + j]);
                    int gray = (int) (255 * (tmp_buf[i * requestWidth + j] - min * 1.0) / ro);
                    if (gray < 0) {
                        gray = 0;
                    }
                    if (gray > 255) {
                        gray = 255;
                    }
                    int paletteNum = 3 * gray;
                    mBuffer[4 * (i * requestWidth +
                                 j)] = (unsigned char) currentpalette[paletteNum];
                    mBuffer[4 * (i * requestWidth + j) + 1] = (unsigned char) currentpalette[
                            paletteNum + 1];
                    mBuffer[4 * (i * requestWidth + j) + 2] = (unsigned char) currentpalette[
                            paletteNum + 2];
                    mBuffer[4 * (i * requestWidth + j) + 3] = 1;
                }
            }
        }

        for(int m=0;m<loopnum;m++){//渲染区域 为框内
            for (int i = mCheckArea[4 * m + 2]; i < mCheckArea[4 * m + 3]; i++) {
                for (int j = mCheckArea[4 * m]; j < mCheckArea[4 * m + 1]; j++) {
                        int gray = (int) (255 * (tmp_buf[i * requestWidth + j] - min * 1.0) / ro);
                        if (gray < 0) {
                            gray = 0;
                        }
                        if (gray > 255) {
                            gray = 255;
                        }
                        int paletteNum = 3 * gray;
                        mBuffer[4 * (i * requestWidth +
                                     j)] = (unsigned char) currentpalette[paletteNum];
                        mBuffer[4 * (i * requestWidth + j) +
                                1] = (unsigned char) currentpalette[
                                paletteNum + 1];
                        mBuffer[4 * (i * requestWidth + j) +
                                2] = (unsigned char) currentpalette[
                                paletteNum + 2];
                        mBuffer[4 * (i * requestWidth + j) + 3] = 1;
                }
            }
        }
    } else{
        //非框内细查，则根据 色板的 拖动条设置去渲染

        for (int i = 0; i < requestHeight - 4; i++) {
            for (int j = 0; j < requestWidth; j++) {
//              LOGE("this requestHeight and requestwidth==============%d========================%d",requestHeight,requestWidth);
                //黑白：灰度值0-254单通道。 paletteIronRainbow：（0-254）×3三通道。两个都是255，所以使用254
//              LOGE("====================%d======================",tmp_buf[i * requestWidth + j]);
                int gray = (int) (255 * (tmp_buf[i * requestWidth + j] - min * 1.0) / ro);
                if (gray < 0) {
                    gray = 0;
                }
                if (gray > 255) {
                    gray = 255;
                }
                int paletteNum = 3 * gray;
                mBuffer[4 * (i * requestWidth +
                             j)] = (unsigned char) currentpalette[paletteNum];
                mBuffer[4 * (i * requestWidth + j) + 1] = (unsigned char) currentpalette[
                        paletteNum + 1];
                mBuffer[4 * (i * requestWidth + j) + 2] = (unsigned char) currentpalette[
                        paletteNum + 2];
                mBuffer[4 * (i * requestWidth + j) + 3] = 1;
            }
        }
    }
    tmp_buf = NULL;
    return mBuffer;
}

/**
 * 拷贝一帧数据到具体控件的引用
 * @param src 数据源
 * @param dest buffer.bits //The actual bits.//窗体的数据源
 * @param width     目标窗体宽度 与 数据源宽度 中的最小宽度
 * @param height    目标窗体高度 与 数据源高度 中的最小高度
 * @param stride_src    数据源的步幅
 * @param stride_dest   窗体的步幅
 */
void FrameImage::copyFrame(const uint8_t *src, uint8_t *dest, const int width, int height, const int stride_src, const int stride_dest){
//    LOGE("===================copyFrame========================");
    memcpy(dest, src, width*height);//一次性拷贝完所有的数据  。从src 中拷贝 width*height 个字节到 dest 中
}

//具体如何去复制 数据到对应的控件引用上
void FrameImage::copyFrameTO292(const uint8_t *src, uint8_t *dest, const int width, int height, const int stride_src, const int stride_dest) {
    const int h8 = height % 8;
    for (int i = 0; i < h8; i++) {
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
    }
    for (int i = 0; i < height; i += 8) {
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest; src += stride_src;
    }
}
/*******************************预览方法区 结束****************************************/

/*******************************录制方法区 开始****************************************/
//设置 帧回调的java  对应的方法
//int FrameImage::setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format){
//    ENTER();
//    LOGE("setFrameCallback步骤8");
//        OutPixelFormat=pixel_format;
//        if (!env->IsSameObject(mFrameCallbackObj, frame_callback_obj))
//        {
//            iframecallback_fields.onFrame = NULL;
//            if (mFrameCallbackObj){
//                env->DeleteGlobalRef(mFrameCallbackObj);
//            }
//            mFrameCallbackObj = frame_callback_obj;
//            if (frame_callback_obj){
//                // get method IDs of Java object for callback
//                jclass clazz = env->GetObjectClass(frame_callback_obj);
//                if (LIKELY(clazz)){
//                    iframecallback_fields.onFrame = env->GetMethodID(clazz,"onFrame",	"(Ljava/nio/ByteBuffer;)V");
//                }else{
//                    LOGE("failed to get object class");
//                }
//                env->ExceptionClear();
//                if (!iframecallback_fields.onFrame){
//                    //LOGE("Can't find IFrameCallback#onFrame");
//                    env->DeleteGlobalRef(frame_callback_obj);
//                    mFrameCallbackObj = frame_callback_obj = NULL;
//                }
//            }
//        }
//    LOGE("setFrameCallback finish");
//    RETURN(0, int);
//}

//the actual function for capturing 录制传递数据到java层的 回调接口
//void FrameImage::do_capture_callback(JNIEnv *env, uint8_t *frameData) {
////    ENTER();
////    LOGE("=====================do_capture_callback==========================");
//    if (LIKELY(mFrameCallbackObj))
//    {
//        jobject buf;
//        //LOGE("do_capture_callback NewDirectByteBuffer");
//        if(LIKELY(OutPixelFormat==3))//RGBA 32bit输出
//        {
//            buf = env->NewDirectByteBuffer(frameData, requestWidth*(requestHeight-4)*4);
//            env->CallVoidMethod(mFrameCallbackObj, iframecallback_fields.onFrame, buf);
//        }
//        else if(UNLIKELY(OutPixelFormat==1))//YUYV或者原始数据输出16bit
//        {
//            buf = env->NewDirectByteBuffer(frameData, requestWidth*(requestHeight-4)*2);
//            env->CallVoidMethod(mFrameCallbackObj, iframecallback_fields.onFrame, buf);
//        }
//        env->ExceptionClear();
//        env->DeleteLocalRef(buf);
//    }
////    EXIT();
//}
/*******************************录制方法区 结束****************************************/

/***************************************温度 开始****************************************/
//设置温度回调接口 对象及其回调的具体方法
int FrameImage::setTemperatureCallback(JNIEnv *env, jobject temperature_callback_obj){
    if (!env->IsSameObject(mTemperatureCallbackObj, temperature_callback_obj))	{
//            LOGE("setTemperatureCallback !env->IsSameObject");
            iTemperatureCallback.onReceiveTemperature = NULL;
            if (mTemperatureCallbackObj) {
//                LOGE("setTemperatureCallback !env->IsSameObject mTemperatureCallbackObj1");
                env->DeleteGlobalRef(mTemperatureCallbackObj);
            }
            mTemperatureCallbackObj = temperature_callback_obj;
            if (mTemperatureCallbackObj) {
                // get method IDs of Java object for callback
//                LOGE("setTemperatureCallback !env->IsSameObject mTemperatureCallbackObj2");
                jclass clazz = env->GetObjectClass(mTemperatureCallbackObj);
                if (LIKELY(clazz)) {
                    iTemperatureCallback.onReceiveTemperature = env->GetMethodID(clazz,"onReceiveTemperature",	"([F)V");
//                    LOGE("setTemperatureCallback !env->IsSameObject mTemperatureCallbackObj3");
                } else {
                    LOGE("failed to get object class");
                }
                env->ExceptionClear();
                if (!iTemperatureCallback.onReceiveTemperature) {
                    LOGE("Can't find IFrameCallback#onFrame");
                    env->DeleteGlobalRef(temperature_callback_obj);
                    mTemperatureCallbackObj = temperature_callback_obj = NULL;
                }
            }
        }
    RETURN(0, int);
}
//查询温度对照表，将查询之后的具体数据回调给java层onReceiveTemperature函数
void FrameImage::do_temperature_callback(JNIEnv *env, uint8_t *frameData){
    unsigned short* orgData=(unsigned short *)frameData;
    unsigned short* fourLinePara=orgData+requestWidth*(requestHeight-4);//后四行参数
    if(UNLIKELY(isNeedWriteTable))
    {
//        LOGE("===================isNeedWriteTable================update temperatureTable===============");
        //根据后四行参数，填充整个的温度对照表 rangeMode设置的固定120
        thermometryT4Line(requestWidth,requestHeight,temperatureTable,fourLinePara,
                          &floatFpaTmp,&correction,&Refltmp,&Airtmp,&humi,&emiss,&distance,
                          cameraLens,shutterFix,rangeMode);
        isNeedWriteTable=false;
        isNeedFreshAD = true;
    }

//   LOGE(" 8000 %f",temperatureTable[8000]);
//    LOGE(" 4000   %f",temperatureTable[4000]);
//    LOGE("12000  %f",temperatureTable[12000]);
//    LOGE("6000  %f",temperatureTable[6000]);
//    LOGE("10000  %f",temperatureTable[10000]);
//    LOGE("7000  %f",temperatureTable[7000]);
//    LOGE("9000  %f",temperatureTable[9000]);
//    LOGE("7500  %f",temperatureTable[7500]);
//    LOGE("9500  %f",temperatureTable[9500]);

    float* temperatureData=mCbTemper;//temperatureData指向mCbTemper的首地址，更改temperatureData也就是更改mCbTemper
    //根据8004或者8005模式来查表，8005模式下仅输出以上注释的10个参数，8004模式下数据以上参数+全局温度数据
    thermometrySearch(requestWidth,requestHeight,temperatureTable,orgData,temperatureData,rangeMode,OUTPUTMODE);
//    LOGE("centerTmp:%.2f,maxTmp:%.2f,minTmp:%.2f,avgTmp:%.2f\n",temperatureData[0],temperatureData[3],temperatureData[6],temperatureData[9]);
    jfloatArray mNCbTemper= env->NewFloatArray(requestWidth*(requestHeight-4)+10);
    /**
     *从mCbTemper 取出10+requestWidth*(requestHeight-4) 个长度的值给 mNCbTemper（这个值要传递给Java层）
     */
    env->SetFloatArrayRegion(mNCbTemper, 0, 10+requestWidth*(requestHeight-4), mCbTemper);
    if (mTemperatureCallbackObj!=NULL)
    {
        //调用java层的 onReceiveTemperature ，传回mNCbTemper（整个图幅温度数据+ 后四行10个温度分析 的数据） 实参
        env->CallVoidMethod(mTemperatureCallbackObj, iTemperatureCallback.onReceiveTemperature, mNCbTemper);
        env->ExceptionClear();


    }
//    pthread_mutex_lock(&fixed_mutex);
    if (isNeedFreshAD){
//        LOGE(" 8000 %f",temperatureTable[8000]);
//        LOGE(" 4000   %f",temperatureTable[4000]);
//        LOGE("12000  %f",temperatureTable[12000]);
//        LOGE("6000  %f",temperatureTable[6000]);
//        float* a = &temperatureTable[0];
//        memcpy(tempData,temperatureTable,16384*4);
        maxThumbAD = getDichotomySearch(temperatureTable,16384,maxThumbValue,4000,12000);
        minThumbAD = getDichotomySearch(temperatureTable,16384,minThumbValue,4000,12000);
        isNeedFreshAD = false;
//        LOGE("maxt == %f    mint ==> %f   maxad %d  minad =%d ",temperatureData[3],temperatureData[6],maxThumbAD,minThumbAD);
//        LOGE("max temp   = %f  , min temp = %f",)
    }


//    pthread_mutex_unlock(&fixed_mutex);

    env->DeleteLocalRef(mNCbTemper);

    temperatureData=NULL;
    fourLinePara=NULL;
    orgData=NULL;
//    LOGE("do_temperature_callback EXIT();");
//    EXIT();
}

/***************************************温度结束****************************************/