#指定cmake最小支持版本
cmake_minimum_required(VERSION 3.10.2)




if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    include_directories(${ANDROID_SYSROOT}/usr/include/arm-linux-androideabi)
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    include_directories(${ANDROID_SYSROOT}/usr/include/aarch64-linux-android)
else()
    include_directories(${ANDROID_SYSROOT}/usr/include/arm-linux-androideabi)
endif()



#C 的编译选项是 CMAKE_C_FLAGS
# 指定编译参数，可选
SET(CMAKE_CXX_FLAGS "-Wno-error=format-security -Wno-error=pointer-sign")


#设置第三方so库路径
set(my_lib_path ${CMAKE_SOURCE_DIR}/../libs)


#将第三方库作为动态库引用
add_library(libsimplePictureProcessing
        SHARED
        IMPORTED)


#指定第三方库的绝对路径
set_target_properties(libsimplePictureProcessing
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/${ANDROID_ABI}/libsimplePictureProcessing.so)


#将第三方库作为动态库引用
add_library(libthermometry
        SHARED
        IMPORTED)


#指定第三方库的绝对路径
set_target_properties(libthermometry
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/${ANDROID_ABI}/libthermometry.so)



#将第三方库作为动态库引用
add_library(libjpeg-turbo1500
        SHARED
        IMPORTED)


#指定第三方库的绝对路径
set_target_properties(libjpeg-turbo1500
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/${ANDROID_ABI}/libjpeg-turbo1500.so)


#将第三方库作为动态库引用
add_library(libuvc
        SHARED
        IMPORTED)

#指定第三方库的绝对路径
set_target_properties(libuvc
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/${ANDROID_ABI}/libuvc.so)


#将第三方库作为动态库引用
add_library(libusb100
        SHARED
        IMPORTED)

#指定第三方库的绝对路径
set_target_properties(libusb100
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/${ANDROID_ABI}/libusb100.so)

#add_library()方法的第一个参数 给第三方库 起的一个别名，随意命名
add_library(dyjpegext
        #表明这个库是动态库 .so 静态库 .a
        SHARED
        #指明是导入库
        IMPORTED)

#set_target_properties()第一个参数是 第三方库的名称/别名
set_target_properties(dyjpegext
        #指明配置导入第三方库的路径
        PROPERTIES IMPORTED_LOCATION
        #设置路径,CMAKE_CURRENT_SOURCE_DIR 就是当前CMakeLists的路径main/cpp
        #/../代表回溯一层 main/
        ${my_lib_path}/${ANDROID_ABI}/libDYTjpegext.so)

find_library(log-lib log)

#设置头文件搜索路径（和此txt同个路径的头文件无需设置），可选

include_directories(${CMAKE_SOURCE_DIR}/../
                    ${CMAKE_SOURCE_DIR}
                    ${CMAKE_SOURCE_DIR}/include
                    ${CMAKE_SOURCE_DIR}/../rapidjson/include
                   )

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK. Gradle
set(SRC_LISTS
        _onload.cpp
        utilbase.cpp
        UVCCamera.cpp
        UVCPreviewIR.cpp
#
        FrameImage.cpp
#
        UVCButtonCallback.cpp
        UVCStatusCallback.cpp
        Parameters.cpp
        time_cal.cpp
        serenegiant_usb_UVCCamera.cpp

        )

add_library( # Sets the name of the library.  设置库的名称。
        TheUVCCamera

        # Sets the library as a shared library. 将library设置为一个共享库
        SHARED

        # Provides a relative path to your source file(s).为源文件提供一个相对路径
        ${SRC_LISTS})


target_link_libraries(TheUVCCamera libusb100)
target_link_libraries(TheUVCCamera libuvc)
target_link_libraries(TheUVCCamera libjpeg-turbo1500)
target_link_libraries(TheUVCCamera libthermometry)
target_link_libraries(TheUVCCamera libsimplePictureProcessing)
target_link_libraries(TheUVCCamera ${log-lib})
target_link_libraries(TheUVCCamera android)
target_link_libraries(TheUVCCamera dyjpegext)#通过jpegext.cpp源文件生成 这个.so库,然后引用进来


# 设置生成的so动态库最后输出的路径，set后面两个参数，
# 前面一个是命令，后面是路径。
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/oplibs/${ANDROID_ABI})




