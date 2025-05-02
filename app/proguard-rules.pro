# ===================== 基础保留规则 =====================
-keepattributes Signature,InnerClasses,Exceptions
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# ===================== 保留数据模型类 =====================
-keep class com.srun.campuslogin.data.model.** { *; }

# ===================== Room 数据库保留 =====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* *;
}

# ===================== GSON 序列化保留 =====================
-keepclassmembers class com.srun.campuslogin.data.model.** {
    <fields>;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===================== Parcelable 保留 =====================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ===================== JNI/Native 方法保留 =====================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===================== OkHttp 保留 =====================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**

# ===================== Retrofit/OkHttp 回调接口 =====================
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ===================== ViewModel/LiveData 保留 =====================
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ===================== 防止资源混淆 =====================
-keepclassmembers class **.R$* {
    public static <fields>;
}