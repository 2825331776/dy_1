
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("easyphotos")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

# 设置生成的so动态库最后输出的路径，set后面两个参数，
# 前面一个是命令，后面是路径。
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/oplibs/${ANDROID_ABI})

#--------------设置第三方libsimplePictureProcessing.so库路径---------------------------------
set(my_libs_path ${CMAKE_SOURCE_DIR}/libs)

#设置头文件搜索路径（和此txt同个路径的头文件无需设置），可选
include_directories(${CMAKE_SOURCE_DIR}/../
        ${CMAKE_SOURCE_DIR}/include
        )


#add_library()方法的第一个参数 给第三方库 起的一个别名，随意命名
add_library(dytjpegaec
        #表明这个库是动态库 .so 静态库 .a
        SHARED
        #指明是导入库
        IMPORTED)

#set_target_properties()第一个参数是 第三方库的名称/别名
set_target_properties(dytjpegaec
        #指明配置导入第三方库的路径
        PROPERTIES IMPORTED_LOCATION
        #设置路径,CMAKE_CURRENT_SOURCE_DIR 就是当前CMakeLists的路径main/cpp
        #/../代表回溯一层 main/
        ${my_libs_path}/${ANDROID_ABI}/libDYTJpegAes.so)

set(EASYPHOTO_SRC_LISTS
        PhotoHelper.cpp
        easyphotos.cpp
        jpegext.cpp
        )

add_library( # Sets the name of the library.
        PhotoNativeHelper

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
       ${EASYPHOTO_SRC_LISTS})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        PhotoNativeHelper

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})


