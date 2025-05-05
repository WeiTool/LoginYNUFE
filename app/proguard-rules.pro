# ===================== 基础保留规则 =====================
-keepattributes Signature, InnerClasses, Exceptions, Annotation
-keepattributes SourceFile, LineNumberTable
-dontwarn sun.misc.**
-dontwarn javax.annotation.**

# ===================== 保留数据模型类（GSON 反序列化） =====================
-keepclassmembers class com.srun.campuslogin.data.model.** {
!transient <fields>;
    <fields>;
    <methods>;
}
-keep class com.srun.campuslogin.data.model.** { *; }

# ===================== Room 数据库保留 =====================
# 保留 RoomDatabase 及其子类
-keep class * extends androidx.room.RoomDatabase { *; }

# 保留所有 Entity 类及字段（防止字段被混淆导致数据库升级失败）
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
}

# 保留所有 Dao 接口及方法（防止 SQL 语句映射失败）
-keepclassmembers @androidx.room.Dao class * {
    <methods>;
}

# 保留 Migration 实现类（数据库迁移必须）
-keep class * extends androidx.room.migration.Migration {
    public <init>(...);
}

# ===================== GSON 序列化保留 =====================
# 保留所有模型类的字段和注解（防止反序列化失败）
-keepclassmembers class com.srun.campuslogin.data.model.** {
    *** *;
    @com.google.gson.annotations.SerializedName <fields>;
    !transient <fields>;
}

# 保留所有泛型信息（防止类型擦除问题）
-keepattributes Signature

# ===================== Parcelable 保留 =====================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
    public <fields>;
}

# ===================== JNI/Native 方法保留 =====================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===================== OkHttp/Retrofit 保留 =====================
# OkHttp3 核心类保留
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit 接口方法保留
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# ===================== ViewModel/LiveData 保留 =====================
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ===================== 防止资源混淆 =====================
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ===================== 其他关键保留 =====================
# 保留所有 JSON 序列化模型类（按包名过滤）
-keep class com.srun.campuslogin.data.model.** {
    *;
}

# 保留日志类（如使用 SLF4J 或 Log4j）
-keep class org.slf4j.** { *; }
-keep class org.apache.log4j.** { *; }

# 保留自定义回调接口（防止 JNI 回调失效）
-keep class com.srun.campuslogin.core.LoginBridge {
    native <methods>;
}
-keep class com.srun.campuslogin.core.LoginBridge$LoginCallback { *; }

-keep class * implements com.srun.campuslogin.core.LoginBridge$LoginCallback {
    *;
}

# ===================== 通用防崩溃规则 =====================
# 保留所有 Activity/Fragment 的子类（反射可能用到）
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment

# 保留自定义 View 的构造方法（XML 反射需要）
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class **.R$* {
    public static <fields>;
}