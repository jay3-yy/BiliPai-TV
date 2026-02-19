# === BiliPai ProGuard Rules ===
# Fixes: java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType

# --- General ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# === 优化选项 ===
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# === CRITICAL: Retrofit + OkHttp ===
# Keep generic signature for Retrofit API interfaces
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Keep Call/Response generic types
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# Keep Retrofit service methods
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# === CRITICAL: Kotlinx Serialization ===
# Keep @Serializable classes and their serializers
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep generated serializers
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# === Data Models (MUST keep for serialization) ===
-keep class com.android.purebilibili.data.model.** { *; }
-keepclassmembers class com.android.purebilibili.data.model.** { *; }

# === API Interfaces ===
-keep interface com.android.purebilibili.core.network.** { *; }
-keep class com.android.purebilibili.core.network.** { *; }

# === ViewModel 保护 ===
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

# === Compose ===
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# === Haze (毛玻璃效果) ===
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# === Cupertino (iOS 风格组件) ===
-keep class io.github.alexzhirkevich.cupertino.** { *; }
-dontwarn io.github.alexzhirkevich.cupertino.**

# === DanmakuFlameMaster ===
-keep class master.flame.danmaku.** { *; }
-dontwarn master.flame.danmaku.**

# === ByteDance DanmakuRenderEngine ===
-keep class com.bytedance.danmaku.** { *; }
-dontwarn com.bytedance.danmaku.**

# === Room Database ===
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# === Media3 / ExoPlayer ===
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# === Coil Image Loading ===
-keep class coil.** { *; }
-dontwarn coil.**

# === ZXing (二维码) ===
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# === Lottie ===
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# === Shimmer ===
-keep class com.valentinilk.shimmer.** { *; }
-dontwarn com.valentinilk.shimmer.**

# === Kotlin Coroutines ===
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# === R8 full mode compatibility ===
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# === Enum 保护 ===
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# === Parcelable ===
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# === Cling (DLNA) ===
-keep class org.fourthline.cling.** { *; }
-dontwarn org.fourthline.cling.**
-dontwarn javax.enterprise.context.**
-dontwarn javax.inject.**
-dontwarn org.seamless.**