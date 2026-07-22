# ProGuard rules for VoltApp

# Keep model classes
-keep class com.titanium.lightdex.models.** { *; }

# Keep API service
-keep class com.titanium.lightdex.ElectricityApiService { *; }

# Keep activities
-keep class com.titanium.lightdex.MainActivity { *; }
-keep class com.titanium.lightdex.AboutActivity { *; }
-keep class com.titanium.lightdex.ErrorCatcher { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Remove System.out and printStackTrace
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Remove debug info
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Obfuscate package names
-repackageclasses 'com.titanium.lightdex'
