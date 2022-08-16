// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("easyphotos");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("easyphotos")
//      }
//    }
#include "stdio.h"
#include "stdlib.h"
#include "malloc.h"
#include "string.h"
#include "android/log.h"
#define LOG_TAG "===easyPhotos==="
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#include "PhotoHelper.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_huantansheng_easyphotos_picNative_PhotoNativeHelper_nativeVerifyFilePath(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jstring file_path) {
    unsigned char * pic_path = (unsigned char *)env->GetStringUTFChars(file_path, NULL);

    LOGE("==========Java_com_huantansheng_easyphotos_picNative_PhotoNativeHelper_nativeVerifyFilePath====%s====",pic_path);
    pic_path = NULL;
    return 0;
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_huantansheng_easyphotos_picNative_PhotoNativeHelper_nativeGetByteArrayByFilePath(
        JNIEnv *env, jclass clazz, jstring file_path) {
    const char * path = env->GetStringUTFChars(file_path,NULL);
    PhotoHelper * photoHelper = new PhotoHelper();
    //格式化图片 附带数据。       在拍照之后调用
    photoHelper->DYT_DeleteAPP2(path);

    /**
     * 解析出数据，并 回调java层。
     * 回调 温度数据给java层。
     * rgba 图幅数据。
     *
     */

    free(photoHelper);

//    FILE * fp = fopen(path,"r");
//    if (fp == NULL)
//    {
//        LOGE("======打开 jpg 文件 失败====");
//        return NULL;
//    }
//    // 当前流指向文件尾部
//    fseek(fp, 0, SEEK_END);
//    // 获取文件长度
//    int oldFileLen = ftell(fp);
//    LOGE("=====文件的长度为：%d=====",oldFileLen);
//    // 当前文件指针指向文件开头
//    fseek(fp, 0, SEEK_SET);
//
//    fclose(fp);
    path = NULL;
    return NULL;
}