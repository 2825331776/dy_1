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

}