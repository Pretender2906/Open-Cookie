# Standard attributes
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# Kotlin Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontnote kotlinx.serialization.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.util.NativeUtilsKt

# Solana Mobile / web3
-keep class com.solana.** { *; }
-keep class com.solana.mobilewalletadapter.** { *; }

# BouncyCastle Ed25519 (PDA on-curve check)
-keep class org.bouncycastle.math.ec.rfc8032.Ed25519 { *; }

# OkHttp (Ktor okhttp engine)
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# General Compose
-keep class androidx.compose.ui.platform.** { *; }

# JVM management (not available on Android; referenced by Ktor debug detector)
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
