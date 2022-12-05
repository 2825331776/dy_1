package com.dyt.wcc.basejnilib

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/11/17  16:07     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.basejnilib     </p>
 */
class BaseJniUtil {

    init {
        System.loadLibrary("BaseJni-lib")
    }

    //    companion object {//静态方法
    /*---------图库调用 ------------*/
    //自定义图片
    external fun judgeIsDYPicture(picPath: String): Int

    //返回数组
    external fun parseDYPicture(picPath: String): ByteArray

    /*----------n2n （不是base）-----------*/

    /* ---------socket（不是base） ------------*/

    /* ---------截图生成图片 ---*/
    /**
     * 检验并修正jpg图片数据格式，并保存自定义的 数据
     * @param picPath 路径
     * @param insertData 插入的数据： 自定义的 jpeg对象。包含ad流,自定义数据数组。
     */
    external fun inspectAndInsert(picPath: String, insertData: ByteArray): Int//检验修正并插入自定义数据

    /*  加解密 */
//    external fun encrypt(data: String): String //加密
//
//    external fun decrypt(data: String): String //解密
//    }

    //----------------------------native method ----------------------------------------

    //----------------------------native method ----------------------------------------
    /**
     *  判断数据格式 通过加解密方式。
     * @param path
     * @return
     */
    external fun judgeJpgFormat(path: String?): Boolean

    /**
     * jpg的所有数据 等等数据。
     * @param path
     * @return
     */
    external fun getJpgSourceData(path: String?): ByteArray?

    /**
     * 设置色板：通过数据源，和色板编号 获取一个完成 rgba 图像数组。
     * @param originalSource
     * @param paletteId
     * @param width
     * @param height
     * @return
     */
    external fun setPalette(
        originalSource: ByteArray?,
        paletteId: Int,
        width: Int,
        height: Int
    ): ByteArray?

}