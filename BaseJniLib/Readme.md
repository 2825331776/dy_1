职责：
1、生成一个base_jni.so ，图库 及其 app module 通过 baselib 或者其他module（也可自己生成一个utils）引用。 
    base_jni:
            AES加解密
            图片结构体，自定义数据的 拆封
            色板解析提取。
            socket tools helper
            

//---------------------------------测试---------------------------------------
Q:
A 、B、C 三个文件打包成一个so库 native-lib，都有各自的头文件 a,b,c
在其他 module 中引用 native-lib ,但只include a头文件，（但在A中引用了B的头文件及其函数。）
是否能成功调用？
A:
