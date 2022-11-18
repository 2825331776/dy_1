// jpegext.cpp: 目标的源文件。
//

#include "jpegext.h"
#include "stdio.h"
#include "stdlib.h"
#include "malloc.h"
#include "string.h"

using namespace std;

int add(int a, int b) {
	return a + b;
}
void T_openFile(char * fileName){
	FILE* fp = fopen(fileName, "wb");
	if (fp == NULL)
	{
		LOGE("打开test.jpg失败\n");
	}
	fclose(fp);
    fp = NULL;
}

const int D_IR_DATA_PAR_OFFSET = 24;
const int D_CAMERA_INFO_OFFSET = 48;
const int D_SENSOR_INFO_OFFSET = 132;
const int D_LENS_INFO_OFFSET = 232;
const int D_CAMERA_PAR_OFFSET = 320;
const int D_IMAGE_INFO_OFFSET = 404;
const int D_FUSE_INFO_OFFSET = 448;
const int D_GPS_INFO_OFFSET = 556;
const int D_TABLE_OFFSET = 604;
const int D_SHAPE_INFO_OFFSET = 1628;

bool is_update = false;
char* m_fileName = NULL;

int D_updateData(const char* fileName, unsigned char* dataBuf, int fileLen) {

	is_update = false;
	FILE* fp = fopen(fileName, "rb");
	if (fp == NULL)
	{
		LOGE("打开.jpg失败\n");
		return -1;
	}
	// 当前流指向文件尾部
	fseek(fp, 0, SEEK_END);
	// 获取文件长度
	int oldFileLen = ftell(fp);
	// 当前文件指针指向文件开头
	fseek(fp, 0, SEEK_SET);

	// 定义app2之前长度
	int app2_index = 0;

	// 开辟与源文件相同大小内存
	unsigned char* pOriginFileBuffer = (unsigned char*)malloc(oldFileLen);
	// 清空开辟内存中历史数据
	memset(pOriginFileBuffer, 0, oldFileLen);
	/// <summary>
	/// 将文件流中数据读取到新开辟的内存中
	/// </summary>
	/// <param name="pOriginFileBuffer">将要读取到的地址</param>
	/// <param name="1">每个对象的大小（单位是字节）</param>
	/// <param name="oldFileLen">长度</param>
	/// <param name="fp">文件流</param>
	int ret = fread(pOriginFileBuffer, 1, oldFileLen, fp);
	if (ret != oldFileLen) {
		LOGE("读取文件失败: %d\n", ret);
		fclose(fp);
		return -3;
	}
	// 读取完毕关闭文件
	fclose(fp);
    fp = NULL;
	// 删除文件
	remove(fileName);

	// 循环跳转，跳转到app2前一个位置 偏移量 文件开始前两位为FFD8
	int nOriginalHeadCopyLength = 1;
	int sum = 0;
	int flag = 0;
	while (true) {

		if (flag == 0) {
			if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE0) {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8)  | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
				flag = 1;
			}
			else {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
			}
		}
		else {
			if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE1) {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
			}
			else {
				nOriginalHeadCopyLength -= 2;
				break;
			}
		}
	}
	// 去除dyt文件后长度
	// 计算空jpeg文件长度

	int a = dataBuf[6];//(parDataBuf)[26];
	int b = dataBuf[7];//(parDataBuf)[27];
	int parLen = (b << 8) | a;

	int dataLen = fileLen - parLen;

	if (dataLen % (0xffff - 2) == 0) {

		sum = (int)(dataLen / (0xffff - 2));
	}
	else {
		sum = (int)(dataLen / (0xffff - 2)) + 1;
	}
	//文件大小扩大len字节，开辟新空间
	int len = (parLen + 4) + dataLen + (sum * 4);


	int ndataEndLength = nOriginalHeadCopyLength;

	//int nCopyLength = 0;
	//拷贝JPG头包括APPE2之前的数据
	//将从头部开始到app2标记之前的内容拷贝到*pTempPosition中
	++nOriginalHeadCopyLength; //前面用该字段当下标，故++；

	int d_len = 0;
	while (true)
	{
		if (pOriginFileBuffer[++ndataEndLength] == 0xFF && pOriginFileBuffer[++ndataEndLength] == 0xE2) {
			unsigned char c1 = pOriginFileBuffer[++ndataEndLength];
			unsigned char c2 = pOriginFileBuffer[++ndataEndLength];
			d_len = (c1 << 8) | c2;
			//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8)  | pOriginFileBuffer[++nOriginalHeadCopyLength];
			ndataEndLength += (d_len - 2);
			flag = 1;
		}
		else {
			ndataEndLength -= 2;

			break;
		}
	}
	++ndataEndLength; // 下标变长度
	int jpegFileDataLen = oldFileLen + nOriginalHeadCopyLength - ndataEndLength;
	int nNewFileLen = jpegFileDataLen + len;

	unsigned char* pNewFileBuffer = (unsigned char*)malloc(nNewFileLen);
	unsigned char* pTempPosition = pNewFileBuffer;

	memcpy(pTempPosition, pOriginFileBuffer, nOriginalHeadCopyLength);
	pTempPosition += nOriginalHeadCopyLength;

	// 开辟一块65533大小的空间
	unsigned char* lsBuff = (unsigned char*)malloc(0xffff + 2);

	// 写入图像信息
	lsBuff[0] = 0xff;
	lsBuff[1] = 0xe2;
	lsBuff[2] = (parLen + 2) >> 8;
	lsBuff[3] = (parLen + 2) & 0x00ff;
	memcpy(lsBuff + 4, dataBuf, (parLen));
	memcpy(pTempPosition, lsBuff, parLen + 4);
	pTempPosition += (parLen + 4);
	//nCopyLength += (parLen + 4);
	dataBuf += parLen;


	// 写入data数据

	int setLen = 0;
	for (int i = 0; i < sum; i++) {
		lsBuff[0] = 0xff;
		lsBuff[1] = 0xe2;


		int j = dataLen - setLen;
		if (j > (0xffff - 2)) {
			lsBuff[2] = 0xff;
			lsBuff[3] = 0xff;
			memcpy(lsBuff + 4, dataBuf + setLen, (0xffff - 2));
			memcpy(pTempPosition, lsBuff, (0xffff + 2));
			pTempPosition += (0xffff + 2);
			setLen += (0xffff - 2);
		}
		else {
			lsBuff[2] = (j + 2) >> 8;
			lsBuff[3] = (j + 2) & 0x00ff;
			memcpy(lsBuff + 4, dataBuf + setLen, j);
			memcpy(pTempPosition, lsBuff, j + 4);
			pTempPosition += (j + 4);
			setLen += j;
		}
	}
	memcpy(pTempPosition, (pOriginFileBuffer + ndataEndLength),
		(oldFileLen - ndataEndLength));

	//写入新文件
	FILE* fwp = fopen(fileName, "wb");
	if (fwp)
	{
		fwrite(pNewFileBuffer, 1, nNewFileLen, fwp);
	}
	fclose(fwp);
	fwp = NULL;

    free(pNewFileBuffer);
    pNewFileBuffer = NULL;
	pTempPosition = NULL;

	free(pOriginFileBuffer);
    pOriginFileBuffer = NULL;
	free(lsBuff);
    lsBuff = NULL;
	return 0;
}

int D_saveData(const char* fileName, unsigned char*& dataBuf_ptr, int fileLen) {
//	LOGE("============D_saveData fileName=============  %s",fileName);
	unsigned char* dataBuf = dataBuf_ptr;
	//if (parLen > (0xffff - 2)) {
	//	return -2;
	//}
	FILE* fp = fopen(fileName, "rb");
	if (fp == NULL)
	{
		LOGE("打开.jpg失败\n");
		return -1;
	}

	// 当前流指向文件尾部
	fseek(fp, 0, SEEK_END);
	// 获取文件长度 byte
	int oldFileLen = ftell(fp);

	// 当前文件指针指向文件开头
	fseek(fp, 0, SEEK_SET);

	// 定义app2之前长度
	//int app2_index = 0;


	// 开辟与源文件相同大小内存
	unsigned char* pOriginFileBuffer = (unsigned char*)malloc(oldFileLen);
	// 清空开辟内存中历史数据
	memset(pOriginFileBuffer, 0, oldFileLen);
	/// <summary>
	/// 将文件流中数据读取到新开辟的内存中
	/// </summary>
	/// <param name="pOriginFileBuffer">将要读取到的地址</param>
	/// <param name="1">每个对象的大小（单位是字节）</param>
	/// <param name="oldFileLen">长度</param>
	/// <param name="fp">文件流</param>
	int ret = fread(pOriginFileBuffer, 1, oldFileLen, fp);
	if (ret != oldFileLen) {
		LOGE("读取文件失败: %d\n", ret);
		fclose(fp);
		return -1;
	}
	// 读取完毕关闭文件

	fclose(fp);
	fp = NULL;
//	LOGE("删除图片之前============");
	// 删除文件
	remove(fileName);
//	LOGE("删除图片之后========  %s" , fileName);

	// 循环跳转，跳转到app2前一个位置 偏移量 文件开始前两位为FFD8

	int nOriginalHeadCopyLength = 1;
	int sum = 0;
	int flag = 0;
	while (true) {
		if (flag == 0) {
//			LOGE("==========================error condition flag =0 ============%d",nOriginalHeadCopyLength);
			if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE0) {//每个图片有且只找到一个这样的标识
//				LOGE("==========================error condition flag =0 ==========before== %d" ,nOriginalHeadCopyLength);
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
//				LOGE("==========================error condition flag =0 ==========behind== %d" ,nOriginalHeadCopyLength);
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8)  | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
				flag = 1;
			}
			else {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
			}
		}
		else {
//			LOGE("==========================error condition flag != 0 ============");
			if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE1) {//在原始图像中  没有找到这样的标识
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
			}
			else {
				nOriginalHeadCopyLength -= 2;
				break;
			}
		}
	}
//	LOGE("====================测试断点one ==============");
	int a = dataBuf[6];//(parDataBuf)[26];
	int b = dataBuf[7];//(parDataBuf)[27];
	int parLen = (b << 8) | a;//得到文件偏移量的值。

	int dataLen = fileLen - parLen;//ad值的偏移量

	if (dataLen % (0xffff - 2) == 0) {

		sum = (int)(dataLen / (0xffff - 2));
	}
	else {
		sum = (int)(dataLen / (0xffff - 2)) + 1;
	}
	//文件大小扩大len字节，开辟新空间
	int len = (parLen + 4) + dataLen + (sum * 4);
	int nNewFileLen = oldFileLen + len;
	unsigned char* pNewFileBuffer = (unsigned char*)malloc(nNewFileLen);
	unsigned char* pTempPosition = pNewFileBuffer;
//	LOGE("====================测试断点two ==============");
	//int nCopyLength = 0;
	//拷贝JPG头包括APPE2之前的数据
	//将从头部开始到app2标记之前的内容拷贝到*pTempPosition中
	++nOriginalHeadCopyLength; //前面用该字段当下标，故++；
	memcpy(pTempPosition, pOriginFileBuffer, nOriginalHeadCopyLength);
	pTempPosition += nOriginalHeadCopyLength;

	// 开辟一块65533大小的空间
	unsigned char* lsBuff = (unsigned char*)malloc(0xffff + 2);

	// 写入图像信息
	lsBuff[0] = 0xff;
	lsBuff[1] = 0xe2;
	lsBuff[2] = (parLen + 2) >> 8;
	lsBuff[3] = (parLen + 2) & 0x00ff;
	memcpy(lsBuff + 4, dataBuf, (parLen));
	memcpy(pTempPosition, lsBuff, parLen + 4);
	pTempPosition += (parLen + 4);
	//nCopyLength += (parLen + 4);
	dataBuf += parLen;

	// 写入data数据
//	LOGE("====================测试断点three ==============");
	int setLen = 0;
	for (int i = 0; i < sum; i++) {
		lsBuff[0] = 0xff;
		lsBuff[1] = 0xe2;


		int j = dataLen - setLen;
		if (j > (0xffff - 2)) {
			lsBuff[2] = 0xff;
			lsBuff[3] = 0xff;
			memcpy(lsBuff + 4, dataBuf + setLen, (0xffff - 2));
			memcpy(pTempPosition, lsBuff, (0xffff + 2));
			pTempPosition += (0xffff + 2);
			//dataIndex += (0xffff - 2);
			setLen += (0xffff - 2);
		}
		else {
			lsBuff[2] = (j + 2) >> 8;
			lsBuff[3] = (j + 2) & 0x00ff;
			memcpy(lsBuff + 4, dataBuf + setLen, j);
			memcpy(pTempPosition, lsBuff, j + 4);
			pTempPosition += (j + 4);
			setLen += j;
		}
	}
//	LOGE("====================测试断点four ==============");
	//memset(pTempPosition, 0, 8);
	memcpy(pTempPosition, (pOriginFileBuffer + nOriginalHeadCopyLength),
		   (oldFileLen - nOriginalHeadCopyLength));

//	LOGE("开始新建  图片  然后写入数据======");
//	//写入新文件
	FILE* fwp = fopen(fileName, "wb");
	if (fwp)
	{
		fwrite(pNewFileBuffer, 1, nNewFileLen, fwp);
	}
//
	fclose(fwp);
	fwp = NULL;

    dataBuf = NULL;
	pTempPosition = NULL;

	free(pNewFileBuffer);
	free(pOriginFileBuffer);
	free(lsBuff);
    pNewFileBuffer = NULL;
    pOriginFileBuffer = NULL;
    lsBuff = NULL;
	return 0;
}

int D_getDytFileLength(const char* fileName,int* dataLen) {
	*dataLen = 0;
	FILE* fp = fopen(fileName, "rb");
	if (fp == NULL)
	{
		LOGE("打开.jpg失败\n");
		return -1;
	}

	// 当前流指向文件尾部
	fseek(fp, 0, SEEK_END);
	// 获取文件长度
	int oldFileLen = ftell(fp);

	// 当前文件指针指向文件开头
	fseek(fp, 0, SEEK_SET);

	// 定义app2之前长度
	int app2_index = 0;


	// 开辟与源文件相同大小内存
	unsigned char* pOriginFileBuffer = (unsigned char*)malloc(oldFileLen);
	unsigned char* pOriginFileBuffer_old = pOriginFileBuffer;
	// 清空开辟内存中历史数据
	memset(pOriginFileBuffer, 0, oldFileLen);
	/// <summary>
	/// 将文件流中数据读取到新开辟的内存中
	/// </summary>
	/// <param name="pOriginFileBuffer">将要读取到的地址</param>
	/// <param name="1">每个对象的大小（单位是字节）</param>
	/// <param name="oldFileLen">长度</param>
	/// <param name="fp">文件流</param>
	int ret = fread(pOriginFileBuffer, 1, oldFileLen, fp);
	if (ret != oldFileLen) {
		LOGE("读取.jpg文件失败\n");
		fclose(fp);
		return -1;
	}
	// 读取完毕关闭文件
	fclose(fp);

	// 循环跳转，跳转到app2前一个位置 偏移量 文件开始前两位为FFD8
	int nOriginalHeadCopyLength = 1;
	int sum = 0;
	int flag = 0;
	while (true) {

		if (flag == 0) {
			if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE0) {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8)  | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
				flag = 1;
			}
			else {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
			}
		}
		else {
			if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE1) {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
			}
			else {
				nOriginalHeadCopyLength -= 2;
				break;
			}
		}
	}
	// 跳过标题
	++nOriginalHeadCopyLength;
	++nOriginalHeadCopyLength;
	// 读取信息段长度
	int num1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
	int num2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
	// 由下标变成长度，故+1
	pOriginFileBuffer += (nOriginalHeadCopyLength + 1);
	sum = (num1 << 8) | num2;
	//parDataBuf = (unsigned char*)malloc(sum-2);

	int readNum = 0;
	int a = pOriginFileBuffer[24];
	int b = pOriginFileBuffer[25];
	int c = pOriginFileBuffer[26];
	int d = pOriginFileBuffer[27];
	int data_w = (b << 8) | a;
	int data_h = (d << 8) | c;
	int pointSize = pOriginFileBuffer[32];

	pOriginFileBuffer += (sum - 2);
	//nOriginalHeadCopyLength+= (sum - 2);
	*dataLen += (sum - 2);


	while (true)
	{
		if (*(pOriginFileBuffer++) == 0xFF && *(pOriginFileBuffer++) == 0xE2) {
			int num1 = *(pOriginFileBuffer++);
			int num2 = *(pOriginFileBuffer++);
			sum = (num1 << 8) | num2;
			pOriginFileBuffer += (sum - 2);
			*dataLen += (sum - 2);
		}
		else {
			break;
		}

	}

	free(pOriginFileBuffer);
	pOriginFileBuffer = NULL;
	pOriginFileBuffer_old = NULL;
	return 0;
}

int D_jpegOpen(const char* fileName, unsigned char*& jpegDataBuf, int* dataLen) {
	
	FILE* fp = fopen(fileName, "rb");
	if (fp == NULL)
	{
		LOGE("打开.jpg失败");
		return -1;
	}

	// 当前流指向文件尾部
	fseek(fp, 0, SEEK_END);
	// 获取文件长度
	int oldFileLen = ftell(fp);

	// 当前文件指针指向文件开头
	fseek(fp, 0, SEEK_SET);

	// 定义app2之前长度
	int app2_index = 0;


	// 开辟与源文件相同大小内存
	unsigned char* pOriginFileBuffer = (unsigned char*)malloc(oldFileLen);
	unsigned char* pOriginFileBuffer_old = pOriginFileBuffer;
	// 清空开辟内存中历史数据
	memset(pOriginFileBuffer, 0, oldFileLen);
	/// <summary>
	/// 将文件流中数据读取到新开辟的内存中
	/// </summary>
	/// <param name="pOriginFileBuffer">将要读取到的地址</param>
	/// <param name="1">每个对象的大小（单位是字节）</param>
	/// <param name="oldFileLen">长度</param>
	/// <param name="fp">文件流</param>
	int ret = fread(pOriginFileBuffer, 1, oldFileLen, fp);
	if (ret != oldFileLen) {
		LOGE("读取.jpg文件失败\n");
		fclose(fp);
		return -1;
	}
	// 读取完毕关闭文件
	fclose(fp);

	// 循环跳转，跳转到app2前一个位置 偏移量 文件开始前两位为FFD8
	int nOriginalHeadCopyLength = 1;
	int sum = 0;
	int flag = 0;
	while (true) {

		if (flag == 0) {
			if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE0) {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8)  | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
				flag = 1;
			}
			else {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
			}
		}
		else {
			if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE1) {
				unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
				sum = (c1 << 8) | c2;
				//sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
				nOriginalHeadCopyLength += (sum - 2);
			}
			else {
				nOriginalHeadCopyLength -= 2;
				break;
			}
		}
	}
	// 跳过标题
	++nOriginalHeadCopyLength;
	++nOriginalHeadCopyLength;
	// 读取信息段长度
	int num1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
	int num2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
	// 由下标变成长度，故+1
	pOriginFileBuffer += (nOriginalHeadCopyLength + 1);
	sum = (num1 << 8) | num2;
	//parDataBuf = (unsigned char*)malloc(sum-2);

	int readNum = 0;
	int a = pOriginFileBuffer[24];
	int b = pOriginFileBuffer[25];
	int c = pOriginFileBuffer[26];
	int d = pOriginFileBuffer[27];
	int data_w = (b << 8) | a;
	int data_h = (d << 8) | c;
	int pointSize = pOriginFileBuffer[32];

	unsigned char* jpegDataBuf_offset = jpegDataBuf;
	memcpy(jpegDataBuf_offset, pOriginFileBuffer, sum - 2);
	pOriginFileBuffer += (sum - 2);
	//nOriginalHeadCopyLength+= (sum - 2);
	jpegDataBuf_offset += (sum - 2);
	*dataLen += (sum - 2);


	while (true)
	{
		if (*(pOriginFileBuffer++) == 0xFF && *(pOriginFileBuffer++) == 0xE2) {
			int num1 = *(pOriginFileBuffer++);
			int num2 = *(pOriginFileBuffer++);
			sum = (num1 << 8) | num2;
			memcpy(jpegDataBuf_offset, pOriginFileBuffer, sum - 2);
			pOriginFileBuffer += (sum - 2);
			jpegDataBuf_offset += (sum - 2);
			*dataLen += (sum - 2);
		}
		else {
			break;
		}

	}
	free(pOriginFileBuffer);
	pOriginFileBuffer = NULL;
	pOriginFileBuffer_old = NULL;
	jpegDataBuf_offset = NULL;
	return 0;
}

int D_jpegClose(const char* fileNme) {
	
	
	int ret = 0;
	if (is_update) {
		is_update = false;
	}
	return ret;
};
int D_getFileHead(unsigned char*& dataBuf,unsigned char*& fileHeadBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	memcpy(fileHeadBuf,dataBuf,24);
	return 0;
};
int D_getIrDataPar(unsigned char*& dataBuf,unsigned char*& irDataParBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_IR_DATA_PAR);
	memcpy(irDataParBuf, dataBuf+ D_IR_DATA_PAR_OFFSET, len);
	return 0;
};
int D_getCameraInfo(unsigned char*& dataBuf,unsigned char*& cameraInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_CAMERA_INFO);
	memcpy(cameraInfoBuf, dataBuf + D_CAMERA_INFO_OFFSET, len);
	return 0;
};
int D_getSensorInfo(unsigned char*& dataBuf,unsigned char*& sensorInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_SENSOR_INFO);
	memcpy(sensorInfoBuf, dataBuf + D_SENSOR_INFO_OFFSET, len);
	return 0;
};
int D_getLensInfo(unsigned char*& dataBuf,unsigned char*& lensInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_LENS_INFO);
	memcpy(lensInfoBuf, dataBuf + D_LENS_INFO_OFFSET, len);
	return 0;
};
int D_getCameraPar(unsigned char*& dataBuf,unsigned char*& cameraParBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_CAMERA_PAR);
	memcpy(cameraParBuf, dataBuf + D_CAMERA_PAR_OFFSET, len);
	return 0;
};
int D_getImageInfo(unsigned char*& dataBuf,unsigned char*& imageInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_IMAGE_INFO);
	memcpy(imageInfoBuf, dataBuf + D_IMAGE_INFO_OFFSET, len);
	return 0;
};
int D_getFuseInfo(unsigned char*& dataBuf,unsigned char*& fuseInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_FUSE_INFO);
	memcpy(fuseInfoBuf, dataBuf + D_FUSE_INFO_OFFSET, len);
	return 0;
};
int D_getGpsInfo(unsigned char*& dataBuf,unsigned char*& gpsInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_GPS_INFO);
	memcpy(gpsInfoBuf, dataBuf + D_GPS_INFO_OFFSET, len);
	return 0;
};
int D_getTable(unsigned char*& dataBuf, unsigned char*& tableBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	memcpy(tableBuf, dataBuf + D_TABLE_OFFSET, 1024);
	return 0;
};

int D_getShapeInfo(unsigned char*& dataBuf, unsigned char*& shapeInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int shapeInfoSize = sizeof(D_SHAPE_INFO);
	memcpy(shapeInfoBuf, dataBuf + D_SHAPE_INFO_OFFSET, shapeInfoSize);
	return 0;
};

int D_getShapePoints(unsigned char*& dataBuf, unsigned char*& shapesBuf, int bufLen) {
	if (dataBuf == NULL) {
		return -2;
	}
	D_SHAPE_INFO dShapeInfo;
	int shapeInfoSize = sizeof(D_SHAPE_INFO);
	memcpy(&dShapeInfo, dataBuf + D_SHAPE_INFO_OFFSET, shapeInfoSize);
	int shapeSize = sizeof(D_POINT);
	if (bufLen != (dShapeInfo.pointSize * shapeSize)) {
		return -1;
	}

	memcpy(shapesBuf,dataBuf + D_SHAPE_INFO_OFFSET + shapeInfoSize, bufLen);
	return 0;


};
int D_getShapeLines(unsigned char*& dataBuf, unsigned char*& shapesBuf, int bufLen) {
	if (dataBuf == NULL) {
		return -2;
	}
	D_SHAPE_INFO dShapeInfo;
	int shapeInfoSize = sizeof(D_SHAPE_INFO);
	memcpy(&dShapeInfo, dataBuf + D_SHAPE_INFO_OFFSET, shapeInfoSize);
	int shapeSize = sizeof(D_LINE);
	int shapeSizePoint = sizeof(D_POINT);
	if (bufLen != (dShapeInfo.lineSize * shapeSize)) {
		return -1;
	}
	int offsetNum = D_SHAPE_INFO_OFFSET + shapeInfoSize + dShapeInfo.pointSize * shapeSizePoint;

	memcpy(shapesBuf, dataBuf + offsetNum, bufLen);
	return 0;

};
int D_getShapeRecs(unsigned char*& dataBuf, unsigned char*& shapesBuf, int bufLen) {
	if (dataBuf == NULL) {
		return -2;
	}
	D_SHAPE_INFO dShapeInfo;
	int shapeInfoSize = sizeof(D_SHAPE_INFO);
	memcpy(&dShapeInfo, dataBuf + D_SHAPE_INFO_OFFSET, shapeInfoSize);
	int shapeSizeRec = sizeof(D_REC);
	int shapeSizeLine = sizeof(D_LINE);
	int shapeSizePoint = sizeof(D_POINT);
	if (bufLen != (dShapeInfo.recSize * shapeSizeRec)) {
		return -1;
	}
	int offsetNum = D_SHAPE_INFO_OFFSET + shapeInfoSize + (dShapeInfo.pointSize * shapeSizePoint) + (dShapeInfo.lineSize * shapeSizeLine);

	memcpy(shapesBuf, dataBuf + offsetNum, bufLen);
	return 0;
};
int D_getShapePolygons(unsigned char*& dataBuf, unsigned char*& shapesBuf, int bufLen) {
	if (dataBuf == NULL) {
		return -2;
	}
	D_SHAPE_INFO dShapeInfo;
	int shapeInfoSize = sizeof(D_SHAPE_INFO);
	memcpy(&dShapeInfo, dataBuf + D_SHAPE_INFO_OFFSET, shapeInfoSize);
	int shapeSizePol = sizeof(D_POLYGON);
	int shapeSizeRec = sizeof(D_REC);
	int shapeSizeLine = sizeof(D_LINE);
	int shapeSizePoint = sizeof(D_POINT);
	if (bufLen != (dShapeInfo.polygonSize * shapeSizePol)) {
		return -1;
	}
	int offsetNum = D_SHAPE_INFO_OFFSET + shapeInfoSize + (dShapeInfo.pointSize * shapeSizePoint) + (dShapeInfo.lineSize * shapeSizeLine) + (dShapeInfo.recSize * shapeSizeRec);

	memcpy(shapesBuf, dataBuf + offsetNum, bufLen);
	return 0;
};
int D_getRawData(unsigned char*& dataBuf, unsigned char*& rawBuf, int bufLen) {
	if (dataBuf == NULL) {
		return -2;
	}
	D_FILE_HEAD dFileHead;
	int fileHeadSize = sizeof(D_FILE_HEAD);
	memcpy(&dFileHead, dataBuf, fileHeadSize);
	int offsetNum = dFileHead.offset;
	memcpy(rawBuf,dataBuf + offsetNum,bufLen);
	return 0;
};


// 更新数据
int D_updateFileHead(unsigned char*& dataBuf, unsigned char*& fileHeadBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_FILE_HEAD);
	memcpy(dataBuf, fileHeadBuf, len);
	is_update = true;
	return 0;
};
int D_updateIrDataPar(unsigned char*& dataBuf, unsigned char*& irDataParBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_IR_DATA_PAR);
	memcpy(dataBuf + D_IR_DATA_PAR_OFFSET, irDataParBuf, len);
	is_update = true;
	return 0;
};
int D_updateCameraInfo(unsigned char*& dataBuf, unsigned char*& cameraInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_CAMERA_INFO);
	memcpy(dataBuf + D_CAMERA_INFO_OFFSET, cameraInfoBuf,  len);
	is_update = true;
	return 0;
};
int D_updateSensorInfo(unsigned char*& dataBuf, unsigned char*& sensorInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_SENSOR_INFO);
	memcpy(dataBuf + D_SENSOR_INFO_OFFSET, sensorInfoBuf,  len);
	is_update = true;
	return 0;
};
int D_updateLensInfo(unsigned char*& dataBuf, unsigned char*& lensInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_LENS_INFO);
	memcpy(dataBuf + D_LENS_INFO_OFFSET, lensInfoBuf, len);
	is_update = true;
	return 0;
};
int D_updateCameraPar(unsigned char*& dataBuf, unsigned char*& cameraParBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_CAMERA_PAR);
	memcpy(dataBuf + D_CAMERA_PAR_OFFSET, cameraParBuf,  len);
	is_update = true;
	return 0;
};
int D_updateImageInfo(unsigned char*& dataBuf, unsigned char*& imageInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_IMAGE_INFO);
	memcpy(dataBuf + D_IMAGE_INFO_OFFSET, imageInfoBuf,  len);
	is_update = true;
	return 0;
};
int D_updateFuseInfo(unsigned char*& dataBuf, unsigned char*& fuseInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_FUSE_INFO);
	memcpy(dataBuf + D_FUSE_INFO_OFFSET, fuseInfoBuf,  len);
	is_update = true;
	return 0;
};
int D_updateGpsInfo(unsigned char*& dataBuf, unsigned char*& gpsInfoBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	int len = sizeof(D_GPS_INFO);
	memcpy(dataBuf + D_GPS_INFO_OFFSET, gpsInfoBuf,  len);
	is_update = true;
	return 0;
};
int D_updateTable(unsigned char*& dataBuf, unsigned char*& tableBuf) {
	if (dataBuf == NULL) {
		return -2;
	}
	memcpy(dataBuf + D_TABLE_OFFSET, tableBuf, 1024);
	is_update = true;
	return 0;
};
int D_updateShape(unsigned char*& dataBuf, unsigned char*& newDataBuf,int dataBufLen, unsigned char*& shapeBuf,int shapeLen ) {
	if (dataBuf == NULL || newDataBuf==NULL) {
		return -2;
	}
	D_FILE_HEAD dFileHead;
	int headSize = sizeof(D_FILE_HEAD);
	memcpy(&dFileHead, dataBuf , headSize);

	memcpy(newDataBuf , dataBuf, D_SHAPE_INFO_OFFSET);
	memcpy(newDataBuf+ D_SHAPE_INFO_OFFSET, shapeBuf,shapeLen);
	memcpy(newDataBuf + D_SHAPE_INFO_OFFSET+ shapeLen, dataBuf+ dFileHead.offset, dataBufLen - D_SHAPE_INFO_OFFSET - shapeLen);
	return 0;
};

