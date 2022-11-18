//
// Created by stefa on 2022/5/30.
//

#ifndef UVCCAMERAX_ALLBASE_H
#define UVCCAMERAX_ALLBASE_H
#include "jni.h"


#include "android/log.h"
#define LOG_TAG "===AllBase==="
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG ,__VA_ARGS__) // //调用Android打印
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,LOG_TAG ,__VA_ARGS__) // 定义LOGF类型

#endif //UVCCAMERAX_ALLBASE_H


