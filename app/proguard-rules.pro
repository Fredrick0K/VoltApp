# ProGuard rules for Thunder App - Security Hardening

# Keep model classes
-keep class com.titanium.lightdex.models.** { *; }

# Keep API service (Retrofit needs this)
-keep class com.titanium.lightdex.ElectricityApiService { *; }

# Keep MainActivity and other activities
-keep class com.titanium.lightdex.MainActivity { *; }
-keep class com.titanium.lightdex.NotificationScheduler { *; }
-keep class com.titanium.lightdex.NotificationScheduler$* { *; }
-keep class com.titanium.lightdex.ErrorCatcher { *; }

# Keep HoraAdapter for RecyclerView
-keep class com.titanium.lightdex.HoraAdapter { *; }
-keep class com.titanium.lightdex.HoraAdapter$* { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Remove logging in release - SECURITY IMPROVEMENT
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Also remove System.out and printStackTrace
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

# Retrofit and OkHttp
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**

# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Remove debug info
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Obfuscate package names
-repackageclasses 'com.titanium.lightdex'
