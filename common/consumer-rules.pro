-dontobfuscate
-dontwarn android.databinding.**
-keep class android.databinding.** { *; }


#--------------------------1.实体类---------------------------------
# 如果使用了Gson之类的工具要使被它解析的JavaBean类即实体类不被混淆。（这里填写自己项目中存放bean对象的具体路径）
-keep class com.dytest.wcc.common.widget.dragView.**{*;}