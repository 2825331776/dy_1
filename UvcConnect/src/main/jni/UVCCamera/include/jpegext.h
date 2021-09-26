// jpegext.h: 目标的头文件。
#ifndef _JPEGEXT_H_
#define _JPEGEXT_H_
#pragma once

#if defined( _WINDOWS )
#define EXPORT_DLL extern "C" __declspec(dllexport) //导出dll声明
#else
#define EXPORT_DLL extern "C" // 导出so.a 不需要加声明
#endif



#include "android/log.h"
#define LOG_TAG "===jpegext==="
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

// TODO: 在此处引用程序需要的其他标头。
// 文件头

typedef enum enIR_OBJ_TYPE  {
	spot 		= 1,
	lines		= 2,
	rectangle = 3,
	ellipse		= 4,
	polygon		= 5
} IR_OBJ_TYPE;

typedef struct D_FILE_HEAD
{
	char dytName[4];	//点扬标识
	unsigned short version;	// 文件版本
	unsigned short offset;	// 文件偏移
	char reserved[16];

}dFileHead;

// 相机硬件部分
typedef struct D_CAMERA_INFO
{
	char devName[4];	// 相机编号
	char devSN[16];		// 相机序列号
	char devFirmware[12];	// 相机固件号
	char reserved[52];
}dCameraInfo;


// 热数据说明
typedef struct D_IR_DATA_PAR
{
	unsigned short ir_w;	//图像宽度
	unsigned short ir_h;	// 图像 高度
	unsigned short raw_max;	// 热数据最大值
	unsigned short raw_min; // 热数据最小值
	char pointSize;			//像素点大小
	char typeAndSize;		// 数据类型/小数位
	char reserved[14];
}dIrDataPar;

// 镜头信息
typedef struct D_LENS_INFO
{
	char lensName[16];	// 镜头名称
	char lensPn[16];	// 镜头零件号
	char lensSn[16];	// 镜头序列号
	float HFoV;			// 镜头水平视场角
	float VFov;			// 镜头垂直视场角
	float Transm;		// 传输率
	unsigned short FocalLength;	// 镜头焦距
	unsigned short length;		// 镜头长度
	float Aperture;				// 光圈值
	char reserved[20];
}dLensInfo;

// 传感器信息
typedef struct D_SENSOR_INFO
{
	char sensorName[16];	//	传感器名称
	char sensorType[16];	//	传感器型号
	char sensorP1[12];
	char sensorP2[12];
	char sensorP3[12];
	char sensorP4[12];
	char reserved[20];
}dSensorInfo;

// 图像信息
typedef struct D_IMAGE_INFO
{
	float maxTemp;	//	最高温度
	float minTemp;	//	最低温度
	unsigned short maxPointX;	
	unsigned short maxPointY;
	unsigned short minPointX;
	unsigned short minPointY;
	char tempUnit;	//	温度单位
	char lutCode;	//  调色板编号
	char reserved[26];

}dImageInfo;

// 相机设置部分
typedef struct D_CAMERA_PAR
{
	char emissivity;	//	反射率
	char humidity;		//  湿度
	char distance[2];	//	目标距离
	float ambientTemp;	//	环境温度
	float reflexTemp;	//  反射温度
	float fix;			//	修正值
	float startFPA;		//	启动FPA
	float currentFPA;	//  当前FPA
	float shutterFPA;	//	挡片温度
	float shellFPA;		//	外框温度
	char reserved[52];
}dCameraPar;



// 融合信息
typedef struct D_FUSE_INFO
{
	float zoomFactor;	//	可见光缩放比例
	short xpanVal;		//	可将中心的X偏移量
	short vpanVal;
	short firstFusionX;	//	在IR图像上融合区域X方向起点
	short lastFusionX;
	short firstFusionY;
	short lastFusionY;
	unsigned char fusionMode;	//	融合模式
	unsigned char fusionLevel;	//	融合度
	char reserved[90];


}dFuseInfo;

// GPS
typedef struct D_GPS_INFO
{
	double longitude; // 经度
	double latitude; //  纬度
	float altitude;  //  海拔
	char reserved2[4]; // 保留
	unsigned long time; //  时间戳
	char directionIndicator; // 经纬度标识
	char reserved[15];
}dGpsInfo;

//2byte
typedef struct M_POINT {
	unsigned short x;
	unsigned short y;
}mPoint;


//26byte
typedef struct D_POINT
{
	unsigned char shapeTyte;
	unsigned char emissivity;
	char distance[2];
	float reflexTemp;
	float temp;
	unsigned short AD;
	char reserved[2];
	char name[8];
	M_POINT point;//2
	

}dPoint;
typedef struct D_LINE
{
	unsigned char shapeTyte;
	unsigned char size;
	char distance[2];
	unsigned char emissivity;
	char reserved[3];
	char name[8];
	float reflexTemp;
	float maxTemp;
	float minTemp;
	float avgTemp;
	M_POINT maxPoint;
	M_POINT minPoint;
	unsigned short maxAD;
	unsigned short minAD;
	M_POINT points[15];

}dLine;

typedef struct D_REC
{
	unsigned char shapeTyte;
	unsigned char emissivity;
	char distance[2];
	float reflexTemp;
	float maxTemp;
	float minTemp;
	float avgTemp;
	M_POINT maxPoint;
	M_POINT minPoint;
	unsigned short maxAD;
	unsigned short minAD;
	char reserved[4];
	char name[8];
	M_POINT points[2];

}dRec;

typedef struct D_POLYGON
{
	unsigned char shapeTyte;
	unsigned char size;
	char distance[2];
	unsigned char emissivity;
	char reserved[3];
	char name[8];
	float reflexTemp;
	float maxTemp;
	float minTemp;
	float avgTemp;
	M_POINT maxPoint;
	M_POINT minPoint;
	unsigned short maxAD;
	unsigned short minAD;
	M_POINT points[15];

}dPolygon;

typedef struct D_SHAPE_INFO {
	unsigned char pointSize;
	unsigned char lineSize;
	unsigned char recSize;
	unsigned char ellipseSize;
	unsigned char polygonSize;
	char reserved[7];

}dShapeInfo;


/********测试函数*********/
EXPORT_DLL int add(int a, int b);
EXPORT_DLL void T_openFile(char * fileName);//新增的测试函数：针对于fopen()函数打开文件出错。发现是在CMakeList.txt编译的时候出现


EXPORT_DLL int D_saveData(const char* fileName, unsigned char*& dataBuf, int fileLen);
EXPORT_DLL int D_getDytFileLength(const char* fileName,int* dataLen);
EXPORT_DLL int D_jpegOpen(const char* fileName, unsigned char*& jpegDataBuf, int* dataLen);
EXPORT_DLL int D_jpegClose(const char* fileName);
EXPORT_DLL int D_getFileHead(unsigned char*& dataBuf,unsigned char*& fileHeadBuf);
EXPORT_DLL int D_getIrDataPar(unsigned char*& dataBuf,unsigned char*& irDataParBuf);
EXPORT_DLL int D_getCameraInfo(unsigned char*& dataBuf,unsigned char*& cameraInfoBuf);
EXPORT_DLL int D_getSensorInfo(unsigned char*& dataBuf,unsigned char*& sensorInfoBuf);
EXPORT_DLL int D_getLensInfo(unsigned char*& dataBuf,unsigned char*& lensInfoBuf);
EXPORT_DLL int D_getCameraPar(unsigned char*& dataBuf,unsigned char*& cameraParBuf);
EXPORT_DLL int D_getImageInfo(unsigned char*& dataBuf,unsigned char*& imageInfoBuf);
EXPORT_DLL int D_getFuseInfo(unsigned char*& dataBuf,unsigned char*& fuseInfoBuf);
EXPORT_DLL int D_getGpsInfo(unsigned char*& dataBuf,unsigned char*& gpsInfoBuf);
EXPORT_DLL int D_getTable(unsigned char*& dataBuf, unsigned char*& tableBuf);
EXPORT_DLL int D_getShapeInfo(unsigned char*& dataBuf, unsigned char*& shapeInfoBuf);
EXPORT_DLL int D_getShapePoints(unsigned char*& dataBuf,unsigned char*& shapesBuf,int bufLen);
EXPORT_DLL int D_getShapeLines(unsigned char*& dataBuf, unsigned char*& shapesBuf, int bufLen);
EXPORT_DLL int D_getShapeRecs(unsigned char*& dataBuf, unsigned char*& shapesBuf, int bufLen);
EXPORT_DLL int D_getShapePolygons(unsigned char*& dataBuf, unsigned char*& shapesBuf, int bufLen);
EXPORT_DLL int D_getRawData(unsigned char*& dataBuf, unsigned char*& rawBuf, int bufLen);

EXPORT_DLL int D_updateFileHead(unsigned char*& dataBuf, unsigned char*& fileHeadBuf);
EXPORT_DLL int D_updateIrDataPar(unsigned char*& dataBuf, unsigned char*& irDataParBuf);
EXPORT_DLL int D_updateCameraInfo(unsigned char*& dataBuf, unsigned char*& cameraInfoBuf);
EXPORT_DLL int D_updateSensorInfo(unsigned char*& dataBuf, unsigned char*& sensorInfoBuf);
EXPORT_DLL int D_updateLensInfo(unsigned char*& dataBuf, unsigned char*& lensInfoBuf);
EXPORT_DLL int D_updateCameraPar(unsigned char*& dataBuf, unsigned char*& cameraParBuf);
EXPORT_DLL int D_updateImageInfo(unsigned char*& dataBuf, unsigned char*& imageInfoBuf);
EXPORT_DLL int D_updateFuseInfo(unsigned char*& dataBuf, unsigned char*& fuseInfoBuf);
EXPORT_DLL int D_updateGpsInfo(unsigned char*& dataBuf, unsigned char*& gpsInfoBuf);
EXPORT_DLL int D_updateTable(unsigned char*& dataBuf, unsigned char*& tableBuf);
EXPORT_DLL int D_updateShape(unsigned char*& dataBuf, unsigned char*& newDataBuf, int dataBufLen, unsigned char*& shapeBuf, int shapeLen);


EXPORT_DLL int D_updateData(const char* fileName, unsigned char* dataBuf, int fileLen);


#endif // !1



