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

//------------------------------具体要实现的-------------------------------------
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_dyt_wcc_basejnilib_BaseJniUtil_judgeJpgFormat(JNIEnv *env, jobject thiz, jstring path) {
    // TODO: implement judgeJpgFormat()



    return false;
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_dyt_wcc_basejnilib_BaseJniUtil_getJpgSourceData(JNIEnv *env, jobject thiz, jstring path) {
    // TODO: implement getJpgSourceData()

    return nullptr;
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_dyt_wcc_basejnilib_BaseJniUtil_setPalette(JNIEnv *env, jobject thiz,
                                                   jbyteArray original_source, jint palette_id,
                                                   jint width, jint height) {
    // TODO: implement setPalette()
    return original_source;
}