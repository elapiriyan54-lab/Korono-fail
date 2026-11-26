# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserve stack trace line numbers safely
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Type-safe Navigation Compose + kotlinx.serialization routes
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.ui.navigation.**$$serializer { *; }
-keepclassmembers class com.example.ui.navigation.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.ui.navigation.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.example.ui.navigation.** { *; }

# Room entities, DAOs, database & data models
-keep class com.example.data.local.** { *; }
-keep class com.example.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# KronoCrypto & AndroidKeyStore AES-GCM helpers
-keep class com.example.util.KronoCrypto { *; }

# Media3 playback service & codecs
-keep class com.example.media.KronoPlaybackService { *; }
-dontwarn androidx.media3.**

# Biometric & Google Play Services
-keep class androidx.biometric.** { *; }
-dontwarn com.google.android.gms.**

# Junrar offline RAR4 extractor & OkHttp
-keep class com.github.junrar.** { *; }
-dontwarn com.github.junrar.**
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn java.beans.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.bouncycastle.**
