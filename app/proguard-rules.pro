# Crash deobfuscation (upload app/build/outputs/mapping/release/mapping.txt to Play Console)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin / coroutines
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# App models & repositories
-keep class com.reyzie.hymns.BuildConfig { *; }
-keep class com.reyzie.hymns.data.** { *; }
-keep class com.reyzie.hymns.carols.data.model.** { *; }

# Supabase / Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.github.jan.**

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Media3 / Cast
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.google.android.gms.cast.** { *; }
-dontwarn com.google.android.gms.**

# OneSignal / PostHog
-keep class com.onesignal.** { *; }
-dontwarn com.onesignal.**
-keep class com.posthog.** { *; }
-dontwarn com.posthog.**

# OkHttp / Ktor logging stubs
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
