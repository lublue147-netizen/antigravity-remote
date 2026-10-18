# Keep WebView Javascript Interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Keep data models and serialization
-keep class com.antigravity.remote.data.model.** { *; }
-keep class com.antigravity.remote.util.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Suppress harmless warnings for libraries
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**