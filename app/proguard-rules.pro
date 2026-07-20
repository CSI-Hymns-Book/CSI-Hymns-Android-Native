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
-keep class com.reyzie.hymns.data.Hymn { *; }
-keep class com.reyzie.hymns.data.Keerthane { *; }
-keep class com.reyzie.hymns.data.ChristmasCarol { *; }
-keep class com.reyzie.hymns.data.InAppMessage { *; }
-keep class com.reyzie.hymns.data.JiraTicketRow { *; }
-keep class com.reyzie.hymns.data.TicketMessage { *; }
-keep class com.reyzie.hymns.data.CustomCategory { *; }
-keep class com.reyzie.hymns.data.CustomCategorySong { *; }
-keep class com.reyzie.hymns.data.ChangelogEntryData { *; }
-keep class com.reyzie.hymns.data.ResolvedTicketAckItem { *; }
-keep class com.reyzie.hymns.carols.data.model.** { *; }

# Supabase / Ktor
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

# AndroidX Startup + WorkManager + Room (OneSignal → work-runtime; R8 strips Room entities)
-keep class androidx.startup.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
-keepclassmembers class androidx.work.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep @androidx.room.Dao interface *
-keep @androidx.room.Database class *
-dontwarn androidx.room.paging.**

# OkHttp / Ktor logging stubs
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
