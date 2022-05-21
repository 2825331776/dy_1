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

FrameImage::FrameImage(uvc_device_handle_t *devh) {
//            mPreviewWindow = NULL;
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
    memset(palette, 0, 3 * 256 * sizeof(unsigned char));
    memset(cameraSoftVersion, 0, 16);

    isNeedWriteTable = true;//是否需要刷新温度对照表，刚开始要刷新。
    floatFpaTmp = correction = Refltmp = Airtmp = humi = emiss = distance = 0;
    cameraLens = 130;
    shutterFix = 0;
    rangeMode = 120;//可更改
    mTemperatureCallbackObj = NULL;

    maxThumbAD = 0;
    minThumbAD = 0;

    isNeedFreshAD = false;
}

FrameImage::~FrameImage() {
    ENTER();
    delete[] mBuffer;
    mDeviceHandle = NULL;
//    if (iTemperatureCallback.onReceiveTemperature){
//        iTemperatureCallback.onReceiveTemperature = NULL;
//    }
    EXIT();
}

//通过resPath，拿到色板的的数据流
const unsigned char *FrameImage::DYgetPalette(int typeOfPalette) {
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
void FrameImage::setResourcePath(const char *path) {
    ENTER();
    strcpy(resPath, path);
    EXIT();
}

//打快门 更新 温度对照表
void FrameImage::shutRefresh() {
    isNeedWriteTable = true;
//    LOGE("===================shutRefresh=============update isNeedWriteTable===============");
}

//二分法查找
int FrameImage::getDichotomySearch(const float *data, int length, float value, int startIndex,
                                   int endIndex) {
//    LOGE("=================    > %f" , data[8000]);
//    LOGE("enter :  length =  %d, value = %f , startIndex = %d  , endIndex = %d" ,length,value , startIndex , endIndex);
    if (endIndex > length) return -1;
    int start = startIndex;
    int end = endIndex - 1;//下标
    int valueIndex = -1;

    if (end <= 0)return -1;
    while (start <= end) {
        int midPont = (start + end + 1) / 2;//记录的是坐标，所以要在取中点之前+1
        if (value == data[midPont]) {//如果中点坐标的值 恰好等于 目标值，则返回这个 中点坐标
            valueIndex = midPont;
            return valueIndex;
        }

        if (value > data[midPont]) {
            start = midPont + 1;
            if ((start) <= end && value < data[start]) {
                valueIndex = midPont;
                return valueIndex;
            }
        }
        if (value < data[midPont]) {
            end = midPont - 1;
            if ((end) >= start && value > data[end]) {//中点値小志 目标值。但中点値前一位的值又大于了目标值。则返回中点前一位的坐标
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
void FrameImage::changePalette(int typeOfPalette) {
    mTypeOfPalette = typeOfPalette;
    mIsPaletteChanged = true;
}

//温幅最高最低修改  温度访问改变
void FrameImage::showTempRange(float maxPercent, float minPercent, float maxValue, float minValue) {
    isshowtemprange = true;
    maxpercent = (unsigned short) maxPercent;
    minpercent = (unsigned short) minPercent;
//    pthread_mutex_lock(&fixed_mutex);{
    maxThumbValue = maxValue;
    minThumbValue = minValue;
    isNeedFreshAD = true;
//    }
//    pthread_mutex_unlock(&fixed_mutex);
    LOGE(" isFixedTempStrip   === > %d", isFixedTempStrip);
////        //todo 查询最大值滑块的温度 对应的ad值 ；最小值滑块对应的 ad值
    LOGE(" maxThumbAD =   %d  minThumbAD = %d  roThumb =   %d", maxThumbAD, minThumbAD, roThumb);
    LOGE("temp maxThumbValue = %f , min == %f", maxThumbValue, minThumbValue);
}

void FrameImage::disTempRange() {//在下一帧图像绘制的时候就不会绘制,是否是拉温宽
    isshowtemprange = false;
}

void FrameImage::fixedTempStripChange(bool state) {
    isNeedFreshAD = true;
    if (state) {
        isFixedTempStrip = true;
    } else {
        isFixedTempStrip = false;
    }
//    LOGE(" fixed temp strip  state =  %d" ,isFixedTempStrip);
}

void FrameImage::setArea(int *area, int lenght) {//设置区域检查的区域大小
    for (int i = 0; i < lenght; i++) {
        mCheckArea[i] = *(area + i);
    }
    areasize = lenght;
}

void FrameImage::setAreaCheck(int isAreaCheck) {//是否设置区域检查
    if (isAreaCheck) {
        mIsAreachecked = true;
    } else {
        mIsAreachecked = false;
    }
}

void FrameImage::getCameraPara(uint8_t *frame) {
    unsigned short *orgData = (unsigned short *) frame;// char a数组[1,2] 经过强转short可能变成513(256*2+1)，或258(256*1+2)。

    unsigned short *fourLinePara = orgData + requestWidth * (requestHeight - 4);//后四行参数
//    LOGE("cpyPara  amountPixels:%d ",amountPixels);
    memcpy(&shutTemper, fourLinePara + amountPixels + 1, sizeof(unsigned short));
//    LOGE("cpyPara  shutTemper:%d ",shutTemper);
    floatShutTemper = shutTemper / 10.0f - 273.15f;//快门片
    memcpy(&coreTemper, fourLinePara + amountPixels + 2, sizeof(unsigned short));//外壳
//    LOGE("cpyPara  coreTemper:%d ",coreTemper);
    floatCoreTemper = coreTemper / 10.0f - 273.15f;
//    LOGE("cpyPara  floatShutTemper:%f,floatCoreTemper:%f,floatFpaTmp:%f\n",floatShutTemper,floatCoreTemper,floatFpaTmp);
    memcpy((uint8_t *) cameraSoftVersion, fourLinePara + amountPixels + 24,
           16 * sizeof(uint8_t));//camera soft version
//    LOGE("cameraSoftVersion:%s\n",cameraSoftVersion);
    memcpy((uint8_t *) sn, fourLinePara + amountPixels + 32, 32 * sizeof(uint8_t));//SN
//    LOGE("sn:%s\n",sn);
    int userArea = amountPixels + 127;

    //tinyC 这个地方需要手动查询
    memcpy(&correction, fourLinePara + userArea, sizeof(float));//修正
    userArea = userArea + 2;
    memcpy(&Refltmp, fourLinePara + userArea, sizeof(float));//反射温度
    userArea = userArea + 2;
    memcpy(&Airtmp, fourLinePara + userArea, sizeof(float));//环境温度
    userArea = userArea + 2;
    memcpy(&humi, fourLinePara + userArea, sizeof(float));//湿度
    userArea = userArea + 2;
    memcpy(&emiss, fourLinePara + userArea, sizeof(float));//发射率
    userArea = userArea + 2;
    memcpy(&distance, fourLinePara + userArea, sizeof(float));//距离
//    LOGE("<<<<<<<<<<correction==%f Refltmp==%f Airtmp==%f humi==%f emiss==%f distance==%f\n",correction,Refltmp,Airtmp,humi,emiss,distance);
    orgData = NULL;
    fourLinePara = NULL;
}

/**
*<p>在这里可以返回测温相关参数</p>
*<p>   fix       float 0-3     </p>
*<p>   Refltmp   float 3-7     </p>
*<p>   Airtmp    float 7-11    </p>
*<p>   humi      float 11-15   </p>
*<p>   emiss     float 15-19      </p>
*<p>   distance  float  20-23     </p>
*<p>   version          112-127   </p>
*/
int FrameImage::getByteArrayTemperaturePara(uint8_t *para, uint8_t *data) {
    uint8_t *TempPara;
    switch (requestWidth) {
        case 384:
            TempPara = data + (requestWidth * (requestHeight - 1) + 127) * 2;
            break;
        case 240:
            TempPara = data + (requestWidth * (requestHeight - 3) + 127) * 2;
            break;
        case 256:
            TempPara = data + (requestWidth * (requestHeight - 3) + 127) * 2;//倒数第二行的 127个元素，一行256个
            break;
        case 640:
            TempPara = data + (requestWidth * (requestHeight - 1) + 127) * 2;
            break;
    }
    memcpy(para, TempPara, 128 * sizeof(uint8_t));//拷贝第二行后一半short 数据到 para中
    //TempPara=TempPara-127*2+24*2;//version
    float dis = 0;
    memcpy(&dis, TempPara + 20, sizeof(float));
    LOGE("getByteArrayTemperaturePara dis:%f", dis);
    memcpy(para + 128 - 16, cameraSoftVersion, 16 * sizeof(uint8_t));//倒数16个字节储存 相机版本号
    //for(int j=0;j<16;j++){
    //LOGE("getByteArrayTemperaturePara version:%c",TempPara[j]);
    //}
    //LOGE("getByteArrayTemperaturePara:%d,%d,%d,%d,%d,%d",para[16],para[17],para[18],para[19],para[20],para[21]);
    TempPara = NULL;
    return true;
}

void FrameImage::setVidPid(int vid, int pid) {
    this->mVid = vid;
    this->mPid = pid;
}
/**********************************用户操作区 结束*************************************/

/*********************************预览方法区  开始**************************************/
void FrameImage::setPreviewSize(int width, int height, int mode) {
    requestWidth = width, requestHeight = height;
    requestMode = mode;
    if (mPid == 1 && mVid == 5396) {
        mBuffer = new unsigned char[requestWidth * (requestHeight - 4) * 4];
        frameWidth = requestWidth;
        frameHeight = requestHeight - 4;
    } else if (mPid == 22592 && mVid == 3034) {
        mBuffer = new unsigned char[requestWidth * (requestHeight) * 4];
        frameWidth = requestWidth;
        frameHeight = requestHeight;
    }
    switch (requestWidth) {
        case 384:
            amountPixels = requestWidth * (4 - 1);
            break;
        case 240:
            amountPixels = requestWidth * (4 - 3);
            break;
        case 256:
            amountPixels = requestWidth * (4 - 3);
            break;
        case 640:
            amountPixels = requestWidth * (4 - 1);
            break;
    }
}

/**
 *
 * @param tmp_buf
 * @param size
 * @param type
 */
void SearchMaxMin(unsigned short *tempAD_data, int size, unsigned short *max, unsigned short *min) {
    if (tempAD_data != NULL) {
        *min = tempAD_data[0];
        *max = tempAD_data[0];
        for (int i = 0; i < size; ++i) {
            if (*min > tempAD_data[i]) {
                *min = tempAD_data[i];
            }
            if (*max < tempAD_data[i]) {
                *max = tempAD_data[i];
            }
        }
    }
}

bool FrameImage::setIsWriteFile(int status) {
//    if (!status){
//        isWriteFile = true;
//    } else{
//        isWriteFile = false;
//    }
    return true;
}


//根据色板去渲染出一帧的画面
unsigned char *FrameImage::onePreviewData(uint8_t *frameData) {
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
//    /读写文件的操作
//    if (isWriteFile){
//        FILE* outFile = NULL;
//        outFile =fopen("/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/tempData0418.txt", "a+");
//        if(outFile != NULL) {
//            file_count++;
//            if (file_count > (file_count_limit)) {
//                LOGE("====== todo ==== write file with record VTemp、MaxTemp ========");
//                float maxT = max ;
//                fprintf(outFile, "maxTemp =%f,", ((maxT/64.0f)-273.15f));
//                //读取Vtemp指令。
//                unsigned char reData[2] = {0};
//                unsigned char data[8] = {0x0d, 0x8b, 0x00, 0x00, 0x00, 0x00, 0x00,
//                                         0x02};
//                if (mDeviceHandle) {
//                    uvc_diy_communicate(mDeviceHandle, 0x41, 0x45, 0x0078, 0x1d00, data,
//                                        sizeof(data),
//                                        1000);
//                    uvc_diy_communicate(mDeviceHandle, 0xc1, 0x44, 0x0078, 0x1d08,
//                                        reData, sizeof(reData),
//                                        1000);
//                }
//                unsigned char reData2[2] = {0};
//                reData2[1] = reData[0];
//                reData2[0] = reData[1];
//                unsigned short *dd = (unsigned short *) reData2;
//                int a = *dd;
//                fprintf(outFile, "vTemp=%d . \n", a);
//                dd = NULL;
//                file_count = 0;
//            }
//            fclose(outFile);
//        }
//    }




    /**
     * 渲染逻辑： 功能需求： 框内细查， 非框内细查， 固定温度条。
     * 固定温度条的时候
     */
    if (mIsPaletteChanged) {
        currentpalette = DYgetPalette(mTypeOfPalette);
        mIsPaletteChanged = false;
    }
    if (mVid == 5396 && mPid == 1) {
        int amountPixels1 = requestWidth * (requestHeight - 4);
//        amountPixels0 = amountPixels1 + 1;//机芯温度AD ，下降15打挡
        amountPixels1 = amountPixels1 + 4;//倒数第四行的 第四位 最大AD
        max = tmp_buf[amountPixels1];
        amountPixels1 = amountPixels1 + 3;//倒数第四行的 第七位
        min = tmp_buf[amountPixels1];
        ro = max - min;
    } else if (mPid == 22592 && mVid == 3034) {
//        LOGE("==============tmp_buf[100]==value  ==%d=============",tmp_buf[100]);
        SearchMaxMin(tmp_buf, (requestHeight * requestWidth), &max, &min);
//        LOGE( "==========max AD === %d  , min Ad ======= %d" ,max , min );
        ro = max - min;
    }

    //定义三个变量去绘制灰度图
    int grayMin = min;
    int grayMax = max;
    int grayRo = grayMax - grayMin;
//    LOGE("maxAD == %hu    minAD ==> %hu   maxad %d  minad =%d ",max,min,maxThumbAD,minThumbAD);

    if (isshowtemprange) {//拉温宽
        //LOGE("非区域检查+拉温宽");
        min = (int) (min + ro * minpercent / 100);
        ro = (int) (ro * (maxpercent - minpercent) / 100);
    }
//    LOGE(" isfixed temp strip  == %d",isFixedTempStrip);
    if (isFixedTempStrip) {//固定温度条
        roThumb = maxThumbAD - minThumbAD;
        min = minThumbAD;//刷新渲染的 边界AD值
        ro = roThumb;//刷新渲染的 范围AD值
    }
//    LOGE( " maxThumbAD  === %d , minThumbAd === %d   , roThumb === %d ",
//          maxThumbAD, minThumbAD , roThumb);
    int loopnum = areasize / 4;
    //如果是 框内细查 或者 是 固定温度条，优先绘制灰度图
    if ((mIsAreachecked && loopnum > 0) || isFixedTempStrip) {
        for (int i = 0; i < frameHeight; i++) {
            for (int j = 0; j < frameWidth; j++) {
                int gray = (int) (255 * (tmp_buf[i * requestWidth + j] - grayMin * 1.0) / grayRo);
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
    }
//    LOGE(" === areasize == %d  ", areasize);

    //框内细查 先绘制灰度图,根据原有的ad值
    if (mIsAreachecked) {
        if (loopnum > 0) {//框内细查 存在添加的框
//            LOGE(" === heckArea %d   === ", mCheckArea[1]);
            //根据框 拿色板去渲染
            for (int m = 0; m < loopnum; m++) {
                for (int i = mCheckArea[4 * m + 2]; i < mCheckArea[4 * m + 3]; i++) {
                    for (int j = mCheckArea[4 * m]; j < mCheckArea[4 * m + 1]; j++) {
                        if (tmp_buf[i * requestWidth + j] >= min &&
                            tmp_buf[i * requestWidth + j] <= (min + ro)) {

                            int gray = (int) (255 * (tmp_buf[i * requestWidth + j] - min * 1.0) /
                                              ro);
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
            }
        } else {//框内细查 并不存在矩形（绘制渲染全图， 是否固定温度条 ）
            for (int i = 0; i < frameHeight; i++) {
                for (int j = 0; j < frameWidth; j++) {
                    //黑白：灰度值0-254单通道。 paletteIronRainbow：（0-254）×3三通道。两个都是255，所以使用254
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
    } else { //非框内细查，
        if (isFixedTempStrip) {//非框内细查，固定温度条
//            LOGE("=====  quanfu Max ==%d , maxThumbAd===  %d =,=minThumbAD == %d ==, maxThumbValue = %f , == minThumbValue == %f ",grayMax, maxThumbAD, minThumbAD,maxThumbValue , minThumbValue );
            for (int i = 0; i < frameHeight; i++) {
                for (int j = 0; j < requestWidth; j++) {
                    if (tmp_buf[i * requestWidth + j] >= min &&
                        tmp_buf[i * requestWidth + j] <= (min + ro)) {
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
        } else {//非框内细查，非固定温度条
            for (int i = 0; i < frameHeight; i++) {
                for (int j = 0; j < frameWidth; j++) {
                    //黑白：灰度值0-254单通道。 paletteIronRainbow：（0-254）×3三通道。两个都是255，所以使用254
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
void FrameImage::copyFrame(const uint8_t *src, uint8_t *dest, const int width, int height,
                           const int stride_src, const int stride_dest) {
//    LOGE("===================copyFrame========================");
    memcpy(dest, src, width * height);//一次性拷贝完所有的数据  。从src 中拷贝 width*height 个字节到 dest 中
}

//具体如何去复制 数据到对应的控件引用上
void FrameImage::copyFrameTO292(const uint8_t *src, uint8_t *dest, const int width, int height,
                                const int stride_src, const int stride_dest) {
    const int h8 = height % 8;
    for (int i = 0; i < h8; i++) {
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
    }
    for (int i = 0; i < height; i += 8) {
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
    }
}
/*******************************预览方法区 结束****************************************/

/***************************************温度 开始****************************************/
//设置温度回调接口 对象及其回调的具体方法
int FrameImage::setTemperatureCallback(JNIEnv *env, jobject temperature_callback_obj) {
    if (!env->IsSameObject(mTemperatureCallbackObj, temperature_callback_obj)) {
        LOGE("=======mTemperatureCallbackObj, temperature_callback_obj============IsSameObject======00000========");
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
                iTemperatureCallback.onReceiveTemperature = env->GetMethodID(clazz,
                                                                             "onReceiveTemperature",
                                                                             "([F)V");
                LOGE("===============iTemperatureCallback.onReceiveTemperature============设置好对应函数====");
            } else {
                LOGE("failed to get object class");
            }
            env->ExceptionClear();
            if (!iTemperatureCallback.onReceiveTemperature) {
                LOGE("Can't find iTemperatureCallback#onReceiveTemperature");
                env->DeleteGlobalRef(temperature_callback_obj);
                mTemperatureCallbackObj = temperature_callback_obj = NULL;
            }
        }
    } else {
        LOGE("=======mTemperatureCallbackObj, temperature_callback_obj============IsSameObject==============");
    }
    RETURN(0, int);
}
/**
 *
 * @param env
 * @param frameData HoldBuffer = requestWidth * requestHeight * 2
 */
//查询温度对照表，将查询之后的具体数据回调给java层onReceiveTemperature函数
void FrameImage::do_temperature_callback(JNIEnv *env, uint8_t *frameData) {
    //共用的指针。
    LOGE("===========do_temperature_callback================mPid = %d =====================mVid=====%d===",
         mPid, mVid);
    unsigned short *orgData = (unsigned short *) frameData;
    if (mPid == 1 && mVid == 5396) {
        unsigned short *fourLinePara = orgData + requestWidth * (requestHeight - 4);//后四行参数
        if (UNLIKELY(isNeedWriteTable)) {
//        LOGE("===================isNeedWriteTable================update temperatureTable===============");
            //根据后四行参数，填充整个的温度对照表 rangeMode设置的固定120
            thermometryT4Line(requestWidth, requestHeight, temperatureTable, fourLinePara,
                              &floatFpaTmp, &correction, &Refltmp, &Airtmp, &humi, &emiss,
                              &distance,
                              cameraLens, shutterFix, rangeMode);
            isNeedFreshAD = true;
            isNeedWriteTable = false;
        }
        //temperatureData指向mCbTemper的首地址，更改temperatureData也就是更改mCbTemper
        float *temperatureData = mCbTemper;
        //根据8004或者8005模式来查表，8005模式下仅输出以上注释的10个参数，8004模式下数据以上参数+全局温度数据
        thermometrySearch(requestWidth, requestHeight, temperatureTable, orgData, temperatureData,
                          rangeMode, OUTPUTMODE);
        LOGE("centerTmp:%.2f,maxTmp:%.2f,minTmp:%.2f,avgTmp:%.2f\n", temperatureData[0],
             temperatureData[3], temperatureData[6], temperatureData[9]);

        jfloatArray mNCbTemper = env->NewFloatArray(requestWidth * (requestHeight - 4) + 10);

        /**
         *从mCbTemper 取出10+requestWidth*(requestHeight-4) 个长度的值给 mNCbTemper（这个值要传递给Java层）
         */
        env->SetFloatArrayRegion(mNCbTemper, 0, 10 + requestWidth * (requestHeight - 4), mCbTemper);
        if (mTemperatureCallbackObj != NULL) {
            LOGE("========================mTemperatureCallbackObj == null=========================");
            //调用java层的 onReceiveTemperature ，传回mNCbTemper（整个图幅温度数据+ 后四行10个温度分析 的数据） 实参
            env->CallVoidMethod(mTemperatureCallbackObj, iTemperatureCallback.onReceiveTemperature,
                                mNCbTemper);
            env->ExceptionClear();
//            if (env->ExceptionCheck()) { /* 异常检查 */
//                fourLinePara = NULL;
//                temperatureData = NULL;
//                env->DeleteLocalRef(mNCbTemper);
//                LOGE("========================mTemperatureCallbackObj == ExceptionCheck====occur=====================");
//                env->ExceptionClear();
//                return;
//            }
        } else {
            LOGE("========================mTemperatureCallbackObj == null1111=============================");
        }
        //固定温度条： 刷新 最大 最小AD的 ，S0通过查表刷新。
        if (isNeedFreshAD) {
//            LOGE("refresh AD ====  maxThumbValue == %f ,  minThumbValue ==== %f " , maxThumbValue , minThumbValue);
            maxThumbAD = getDichotomySearch(temperatureTable, 16384, maxThumbValue, 0, 16384);
            minThumbAD = getDichotomySearch(temperatureTable, 16384, minThumbValue, 0, 16384);
            isNeedFreshAD = false;
            //3 全幅最高温 ，6 是全幅最低温 AD值
//        LOGE("max temp   = %f  , min temp = %f",)
        }
        env->DeleteLocalRef(mNCbTemper);

        temperatureData = NULL;
        fourLinePara = NULL;
//    EXIT();
    } else if (mPid == 22592 && mVid == 3034) {
        //记录十个特征点温度。
        float minX, minY, minTemp, maxX, maxY, maxTemp, centerTemp;
        //创建返回Java层的 温度数组 。 returnTempData  返回的数据指针。
        jfloatArray mNCbTemper = env->NewFloatArray(requestWidth * requestHeight + 10);
        jfloat *returnTempData = (jfloat *) env->GetFloatArrayElements(mNCbTemper, 0);
        jfloat *controlTemp = returnTempData;
        //读取中心点温度 .centerData 中心点指针，centerTemp 中心点指针AD值转成的温度 ,四个点温度的均值。
        unsigned short *centerData = (unsigned short *) frameData;
//        LOGE(" =============TINYC ======requestWidth * requestHeight ======== %d" ,requestWidth * requestHeight);
        int centerAllADSum = 0;
        //顶部两个
        centerData = centerData + (requestWidth * (requestHeight / 2 - 1)) + requestWidth / 2;
        centerAllADSum += (*centerData);
        centerData++;
        centerAllADSum += (*centerData);
        //底部两个点
        centerData += (requestWidth - 1);
        centerAllADSum += (*centerData);
        centerData++;
        centerAllADSum += (*centerData);

        centerTemp = (centerAllADSum / 4.0f) / 64.0f - 273.15f;
//        LOGE(" =============TINYC ======centerTemp ======== %f" ,centerTemp);
        *controlTemp = centerTemp;
//        LOGE("========= orgData[100] ======== %d ===============" , orgData[100]);
        controlTemp = returnTempData;
        controlTemp = controlTemp + 10;
        //一次遍历拿到最大最小值。及其坐标。以及 更正没一个AD 转温度。
        for (int i = 0; i < requestHeight; i++) {
            for (int j = 0; j < requestWidth; j++) {
                //赋予初始值
                if (i == 0 && j == 0) {
                    minX = 0;
                    minY = 0;
                    minTemp = *orgData;
                    maxX = 0;
                    maxY = 0;
                    maxTemp = *orgData;
                }
                if (minTemp > *orgData) {
                    minX = j;
                    minY = i;
                    minTemp = *orgData;
                }
                if (maxTemp < *orgData) {
                    maxX = j;
                    maxY = i;
                    maxTemp = *orgData;
                }
                *controlTemp = (*orgData) / 64.0f - 273.15f;
                orgData++;
                controlTemp++;
            }
        }

        minTemp = minTemp / 64.0f - 273.15;
        maxTemp = maxTemp / 64.0f - 273.15;
        controlTemp = returnTempData;
        controlTemp++;
        *controlTemp = maxX;
        controlTemp++;
        *controlTemp = maxY;
        controlTemp++;
        *controlTemp = maxTemp;
        controlTemp++;
        *controlTemp = minX;
        controlTemp++;
        *controlTemp = minY;
        controlTemp++;
        *controlTemp = minTemp;
        controlTemp++;
        *controlTemp = 0;
        controlTemp++;
        *controlTemp = 0;
        controlTemp++;
        *controlTemp = 0;
        if (mTemperatureCallbackObj != NULL) {
            //调用java层的 onReceiveTemperature ，传回mNCbTemper（整个图幅温度数据+ 后四行10个温度分析 的数据） 实参
            env->CallVoidMethod(mTemperatureCallbackObj, iTemperatureCallback.onReceiveTemperature,
                                mNCbTemper);
            env->ExceptionClear();
        }
        //固定温度条： 刷新最大最小AD值 ，tinyC通过计算获取。
        if (isNeedFreshAD) {
//            LOGE("refresh AD ====  maxThumbValue == %f ,  minThumbValue ==== %f " , maxThumbValue , minThumbValue);
            maxThumbAD = (maxThumbValue + 273.15f) * 64;
            minThumbAD = (minThumbValue + 273.15f) * 64;
            isNeedFreshAD = false;
        }

        centerData = NULL;
        returnTempData = NULL;
        controlTemp = NULL;

        env->DeleteLocalRef(mNCbTemper);
    }

    orgData = NULL;
//    LOGE("do_temperature_callback EXIT();");
//    EXIT();
}

/***************************************温度结束****************************************/