
#
# ----------------------------- 默认保留 -----------------------------
#
#----------------------------------------------------
# 保持哪些类不被混淆
##继承activity,application,service,broadcastReceiver,contentprovider....不进行混淆
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class * extends android.app.backup.BackupAgentHelper
#-keep public class * extends android.preference.Preference
#-keep public class * extends android.view.View
#-keep class android.support.** {*;}## 保留support下的所有类及其内部类

## easyphotos 库的混淆处理
-keep class com.huantansheng.easyphotos.models.** { *; }

#----------------------------------------------------

# 保留继承的
#-keep public class * extends android.support.v4.**
#-keep public class * extends android.support.v7.**
#-keep public class * extends android.support.annotation.**
#
#
##表示不混淆任何包含native方法的类的类名以及native方法名，这个和我们刚才验证的结果是一致
#-keepclasseswithmembernames class * {
#    native <methods>;
#}
#
#
##表示不混淆任何一个View中的setXxx()和getXxx()方法，
##因为属性动画需要有相应的setter和getter的方法实现，混淆了就无法工作了。
#-keep public class * extends android.view.View{
#    *** get*();
#    void set*(***);
#    public <init>(android.content.Context);
#    public <init>(android.content.Context, android.util.AttributeSet);
#    public <init>(android.content.Context, android.util.AttributeSet, int);
#}
#-keepclasseswithmembers class * {
#    public <init>(android.content.Context, android.util.AttributeSet);
#    public <init>(android.content.Context, android.util.AttributeSet, int);
#}