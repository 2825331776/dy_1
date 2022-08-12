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
    // 将文件流中数据读取到新开辟的内存中
    int ret = fread(pOriginFileBuffer, 1, oldFileLen, fp);
    if (ret != oldFileLen) {
        LOGE("拷贝 图片文件流 失败: %d\n", ret);
        fclose(fp);
        return -1;
    }
    // 读取完毕关闭文件
    fclose(fp);
    // 删除文件
    remove(fileName);

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
        if (pFFE2Marker[(nOriginalHeadCopyLength + 1)] == 0xFF &&
                pFFE2Marker[(nOriginalHeadCopyLength + 2)] == 0xE0) {
            int E0_high = pFFE2Marker[nOriginalHeadCopyLength + 3];
            int E0_low = pFFE2Marker[nOriginalHeadCopyLength + 4];
            int this_E0_len = ((E0_high << 8) | E0_low);
            nOriginalHeadCopyLength = nOriginalHeadCopyLength + this_E0_len + 2;//指向 FFE0数据的末尾
        }
        /**
         * 处理FF E1
         */
        if (pFFE2Marker[(nOriginalHeadCopyLength + 1)] == 0xFF &&
                pFFE2Marker[(nOriginalHeadCopyLength + 2)] == 0xE1) {
            int E1_high = pFFE2Marker[nOriginalHeadCopyLength + 3];
            int E1_low = pFFE2Marker[nOriginalHeadCopyLength + 4];
            int this_E1_len = ((E1_high << 8) | E1_low);
            nOriginalHeadCopyLength = nOriginalHeadCopyLength + this_E1_len + 2;//指向 FFE1数据的末尾
        }
        /**
         * 处理FF E2
         */
        if (pFFE2Marker[(nOriginalHeadCopyLength + 1)] == 0xFF &&
                pFFE2Marker[(nOriginalHeadCopyLength + 2)] == 0xE2) {
            int E2_high = pFFE2Marker[nOriginalHeadCopyLength + 3];
            int E2_low = pFFE2Marker[nOriginalHeadCopyLength + 4];
            int this_E2_len = ((E2_high << 8) | E2_low);
            FFE2Maker_total_len += this_E2_len;//计算FFE2 的 总长度
            nOriginalHeadCopyLength = nOriginalHeadCopyLength + this_E2_len + 2;//指向 FFE1数据的末尾
            FFE2Maker_count++;
            LOGE("=============FFE2=========次数为：%d", FFE2Maker_count);
        }
        /**
         * 跳出循环条件 识别 FFDB
         */
        if (pFFE2Marker[(nOriginalHeadCopyLength + 1)] == 0xFF &&
                pFFE2Marker[(nOriginalHeadCopyLength + 2)] == 0xDB) {//是 ff db 跳出循环
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
    LOGE("=========newFile Len =%d============",newFileLen);
    unsigned char *pNewFileHead = pNewFileNoE2;
    // 清空开辟内存中历史数据
    memset(pNewFileNoE2, 0, newFileLen);

    //拷贝数据到新内存中，除了E2数据。
    unsigned char *oldFileHead = pOriginFileBuffer;
    int oldHeadLen = nOriginalHeadCopyLength - FFE2Maker_total_len;
    memcpy(pNewFileHead, oldFileHead, oldHeadLen);
    pNewFileHead += oldHeadLen;//指针都向后移动
    oldFileHead += (oldHeadLen + FFE2Maker_total_len);//
    //剩下 拷贝数据

    int shengxia = oldFileLen - oldHeadLen - FFE2Maker_total_len;
    LOGE("=====剩下的字节数为：==%d,oldFileLen==%d=,FFD8和FFE0的长度oldHeadLen=%d,FFE2的总长度为：%d",shengxia,oldFileLen,oldHeadLen,FFE2Maker_total_len);
    memcpy(pNewFileHead, oldFileHead,(newFileLen - oldHeadLen) );

//    const char * testFilename = "/storage/emulated/0/DCIM/DYTCamera/testFile.jpg";
    //写入到文件
    FILE* fwp = fopen(fileName, "w+");
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

    return 0;
}