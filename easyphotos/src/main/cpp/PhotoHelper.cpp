//
// Created by stefa on 2022/8/9.
//

//#include "j"


#include "PhotoHelper.h"


int PhotoHelper::DYT_DeleteAPP2(const char *fileName) {
//	LOGE("============D_saveData fileName=============  %s",fileName);
    FILE *fp = fopen(fileName, "rb");
    if (fp == NULL) {
        LOGE("打开.jpg失败\n");
        return -1;
    }

    // 当前流指向文件尾部
    fseek(fp, 0, SEEK_END);
    // 获取文件长度 byte
    int oldFileLen = ftell(fp);

    // 当前文件指针指向文件开头
    fseek(fp, 0, SEEK_SET);

    // 开辟与源文件相同大小内存
    unsigned char *pOriginFileBuffer = (unsigned char *) malloc(oldFileLen);
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
        LOGE("拷贝 图片文件流 失败: %d\n", ret);
        fclose(fp);
        return -1;
    }
    // 读取完毕关闭文件

    fclose(fp);
//	LOGE("删除图片之前============");
    // 删除文件
    remove(fileName);
//	LOGE("删除图片之后========  %s" , fileName);

    /**
     * 思路： 当前已经读取图片文件流 到  unsigned char* pOriginFileBuffer当中。
     * 循环遍历 此指针的数据，在 ff  db 之前 ff e0之后， 有无 ffe2
     * 找到 ffe2，并记录他的其实位置（ff的其实指针坐标），还有长度（读取的长度+2（是Marker表示ffe2的长度））
     *
     * 最后声明一个原始长度减去 FFE2 长度的数组，把所有的数据都拷贝到这个数组，并保存成源文件。
     * 引申： 记录所有带FFE2 的Maker ，一起删除。
     * 升华：穿入Maker，删除。（是否需要传入 起始 Maker？）
     */
    // 循环跳转，跳转到app2前一个位置 偏移量 文件开始前两位为FFD8

    unsigned char *pFFE2Marker = pOriginFileBuffer;//指向FFE2 的Marker的指针。
    //删除的字节数等于 （总长度 + 数量*2 ）字节
    int FFE2Maker_count = 0;//数量
    int FFE2Maker_total_len = 0;//总长度 FFE2后面的长度相加

    int nOriginalHeadCopyLength = 1;//刚开始指向FFD9 的 D9。 +1 等于FF， +2 等于E0

    while (true) {
        /**
         * 处理FF E0
         */
        if (pOriginFileBuffer[(nOriginalHeadCopyLength + 1)] == 0xFF &&
            pOriginFileBuffer[(nOriginalHeadCopyLength + 2)] == 0xE0) {
            int E0_high = pOriginFileBuffer[nOriginalHeadCopyLength + 3];
            int E0_low = pOriginFileBuffer[nOriginalHeadCopyLength + 4];
            int this_E0_len = ((E0_high << 8) | E0_low);
            nOriginalHeadCopyLength = nOriginalHeadCopyLength + this_E0_len + 2;//指向 FFE0数据的末尾
        }
        /**
         * 处理FF E1
         */
        if (pOriginFileBuffer[(nOriginalHeadCopyLength + 1)] == 0xFF &&
            pOriginFileBuffer[(nOriginalHeadCopyLength + 2)] == 0xE1) {
            int E1_high = pOriginFileBuffer[nOriginalHeadCopyLength + 3];
            int E1_low = pOriginFileBuffer[nOriginalHeadCopyLength + 4];
            int this_E1_len = ((E1_high << 8) | E1_low);
            nOriginalHeadCopyLength = nOriginalHeadCopyLength + this_E1_len + 2;//指向 FFE1数据的末尾
        }
        /**
         * 处理FF E2
         */
        if (pOriginFileBuffer[(nOriginalHeadCopyLength + 1)] == 0xFF &&
            pOriginFileBuffer[(nOriginalHeadCopyLength + 2)] == 0xE2) {
            int E2_high = pOriginFileBuffer[nOriginalHeadCopyLength + 3];
            int E2_low = pOriginFileBuffer[nOriginalHeadCopyLength + 4];
            int this_E2_len = ((E2_high << 8) | E2_low);
            FFE2Maker_total_len += this_E2_len;//计算FFE2 的 总长度
            nOriginalHeadCopyLength = nOriginalHeadCopyLength + this_E2_len + 2;//指向 FFE1数据的末尾
            FFE2Maker_count++;
            LOGE("=============FFE2=========次数为：%d", FFE2Maker_count);
        }
        /**
         * 跳出循环条件 识别 FFDB
         */
        if (pOriginFileBuffer[(nOriginalHeadCopyLength + 1)] == 0xFF &&
            pOriginFileBuffer[(nOriginalHeadCopyLength + 2)] == 0xDB) {//是 ff db 跳出循环
            break;
        }
    }
    LOGE("=============FFE2===while循环借宿======总次数为===：%d", FFE2Maker_count);
    FFE2Maker_total_len += (FFE2Maker_count * 2);
    LOGE("===此时数据的指针指向坐标为=nOriginalHeadCopyLength:%d,=E2表示的长度为FFE2Maker_total_len：%d=",
         nOriginalHeadCopyLength, FFE2Maker_total_len);
    //开始申请一段去掉E2 的内存。

    int newFileLen = oldFileLen - FFE2Maker_total_len;
    // 开辟与源文件相同大小内存
    unsigned char *pNewFileNoE2 = (unsigned char *) malloc(newFileLen);
    unsigned char *pNewFileHead = pNewFileNoE2;
    // 清空开辟内存中历史数据
    memset(pNewFileNoE2, 0, newFileLen);

    //拷贝数据到新内存中，除了E2数据。
    unsigned char *oldFileHead = pOriginFileBuffer;
    int oldHeadLen = nOriginalHeadCopyLength - FFE2Maker_total_len;
    memcpy(pNewFileHead, oldFileHead, oldHeadLen);
    pNewFileHead = pNewFileHead + oldHeadLen;//指针都向后移动
    oldFileHead = oldFileHead + oldHeadLen + FFE2Maker_total_len;//
    //剩下 拷贝数据

    int shengxia = oldFileLen - oldHeadLen - FFE2Maker_total_len;
    LOGE("=====剩下的字节数为：==%d,oldFileLen==%d=,FFD8和FFE0的长度oldHeadLen=%d,FFE2的总长度为：%d",shengxia,oldFileLen,oldHeadLen,FFE2Maker_total_len);
    memcpy(pNewFileHead, oldFileHead,shengxia );

    const char * testFilename = "/storage/emulated/0/DCIM/DYTCamera/testFile.jpg";
    //写入到文件
    FILE* fwp = fopen(testFilename, "w+");
    if (fwp)
    {
        fwrite(pNewFileNoE2, 1, shengxia+oldHeadLen, fwp);
    }
    fclose(fwp);

    oldFileHead = NULL;
    pNewFileHead = NULL;
    pFFE2Marker = NULL;
    free(pNewFileNoE2);
    free(pOriginFileBuffer);

//    while (true) {
//        if (flag == 0) {
////			LOGE("==========================error condition flag =0 ============%d",nOriginalHeadCopyLength);
//            if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE0) {//每个图片有且只找到一个这样的标识
////				LOGE("==========================error condition flag =0 ==========before== %d" ,nOriginalHeadCopyLength);
//                unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
//                unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
////				LOGE("==========================error condition flag =0 ==========behind== %d" ,nOriginalHeadCopyLength);
//                sum = (c1 << 8) | c2;
//                //sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8)  | pOriginFileBuffer[++nOriginalHeadCopyLength];
//                nOriginalHeadCopyLength += (sum - 2);
//                flag = 1;
//            }
//            else {
//                unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
//                unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
//                sum = (c1 << 8) | c2;
//                //sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
//                nOriginalHeadCopyLength += (sum - 2);
//            }
//        }
//        else {
////			LOGE("==========================error condition flag != 0 ============");
//            if (pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xFF && pOriginFileBuffer[++nOriginalHeadCopyLength] == 0xE1) {//在原始图像中  没有找到这样的标识
//                unsigned char c1 = pOriginFileBuffer[++nOriginalHeadCopyLength];
//                unsigned char c2 = pOriginFileBuffer[++nOriginalHeadCopyLength];
//                sum = (c1 << 8) | c2;
//                //sum = (pOriginFileBuffer[++nOriginalHeadCopyLength] << 8) | pOriginFileBuffer[++nOriginalHeadCopyLength];
//                nOriginalHeadCopyLength += (sum - 2);
//            }
//            else {
//                nOriginalHeadCopyLength -= 2;
//                break;
//            }
//        }
//    }
////	LOGE("====================测试断点one ==============");
//    int a = dataBuf[6];//(parDataBuf)[26];
//    int b = dataBuf[7];//(parDataBuf)[27];
//    int parLen = (b << 8) | a;//得到文件偏移量的值。
//
//    int dataLen = fileLen - parLen;//ad值的偏移量
//
//    if (dataLen % (0xffff - 2) == 0) {
//
//        sum = (int)(dataLen / (0xffff - 2));
//    }
//    else {
//        sum = (int)(dataLen / (0xffff - 2)) + 1;
//    }
//    //文件大小扩大len字节，开辟新空间
//    int len = (parLen + 4) + dataLen + (sum * 4);
//    int nNewFileLen = oldFileLen + len;
//    unsigned char* pNewFileBuffer = (unsigned char*)malloc(nNewFileLen);
//    unsigned char* pTempPosition = pNewFileBuffer;
////	LOGE("====================测试断点two ==============");
//    //int nCopyLength = 0;
//    //拷贝JPG头包括APPE2之前的数据
//    //将从头部开始到app2标记之前的内容拷贝到*pTempPosition中
//    ++nOriginalHeadCopyLength; //前面用该字段当下标，故++；
//    memcpy(pTempPosition, pOriginFileBuffer, nOriginalHeadCopyLength);
//    pTempPosition += nOriginalHeadCopyLength;
//
//    // 开辟一块65533大小的空间  注：不是 65537？ 无关键字代码打印相加为65537
//    unsigned char* lsBuff = (unsigned char*)malloc(0xffff + 2);
//
//    // 写入图像信息
//    lsBuff[0] = 0xff;
//    lsBuff[1] = 0xe2;
//    lsBuff[2] = (parLen + 2) >> 8;
//    lsBuff[3] = (parLen + 2) & 0x00ff;
//    memcpy(lsBuff + 4, dataBuf, (parLen));
//    memcpy(pTempPosition, lsBuff, parLen + 4);
//    pTempPosition += (parLen + 4);
//    //nCopyLength += (parLen + 4);
//    dataBuf += parLen;
//
//    // 写入data数据
////	LOGE("====================测试断点three ==============");
//    int setLen = 0;
//    for (int i = 0; i < sum; i++) {
//        lsBuff[0] = 0xff;
//        lsBuff[1] = 0xe2;
//
//
//        int j = dataLen - setLen;
//        if (j > (0xffff - 2)) {
//            lsBuff[2] = 0xff;
//            lsBuff[3] = 0xff;
//            memcpy(lsBuff + 4, dataBuf + setLen, (0xffff - 2));
//            memcpy(pTempPosition, lsBuff, (0xffff + 2));
//            pTempPosition += (0xffff + 2);
//            //dataIndex += (0xffff - 2);
//            setLen += (0xffff - 2);
//        }
//        else {
//            lsBuff[2] = (j + 2) >> 8;
//            lsBuff[3] = (j + 2) & 0x00ff;
//            memcpy(lsBuff + 4, dataBuf + setLen, j);
//            memcpy(pTempPosition, lsBuff, j + 4);
//            pTempPosition += (j + 4);
//            setLen += j;
//        }
//    }
////	LOGE("====================测试断点four ==============");
//    //memset(pTempPosition, 0, 8);
//    memcpy(pTempPosition, (pOriginFileBuffer + nOriginalHeadCopyLength),
//           (oldFileLen - nOriginalHeadCopyLength));
//
////	LOGE("开始新建  图片  然后写入数据======");
////	//写入新文件
//    FILE* fwp = fopen(fileName, "wb");
//    if (fwp)
//    {
//        fwrite(pNewFileBuffer, 1, nNewFileLen, fwp);
//    }
//
////
////
//    fclose(fwp);
//
//
//    free(pNewFileBuffer);
//    free(pOriginFileBuffer);
//    free(lsBuff);
    return 0;
}