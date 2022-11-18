#include <jni.h>

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("BaseJniLib");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("BaseJniLib")
//      }
//    }


extern "C"
JNIEXPORT jint JNICALL
Java_com_dyt_wcc_basejnilib_BaseJniUtil_judgeIsDYPicture(JNIEnv *env, jobject thiz,
                                                        jstring pic_path) {

    return 1000;
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_dyt_wcc_basejnilib_BaseJniUtil_parseDYPicture(JNIEnv *env, jobject thiz, jstring pic_path) {


    return nullptr;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_dyt_wcc_basejnilib_BaseJniUtil_inspectAndInsert(JNIEnv *env, jobject thiz,
                                                         jstring pic_path, jbyteArray insert_data) {
    // TODO: implement inspectAndInsert()
}