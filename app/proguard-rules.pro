# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# iText 8 & Bouncy Castle Hardening
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**

# Kotlinx Serialization Hardening
-keep class com.byajbook.data.backup.** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
-keep @kotlinx.serialization.Serializable class ** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** { *** Companion; }
-keepclasseswithmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Hilt & Dagger
-keep class dagger.hilt.** { *; }
-keep class com.byajbook.di.** { *; }
