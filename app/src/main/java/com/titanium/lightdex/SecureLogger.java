package com.titanium.lightdex;

import android.util.Log;
import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * Logger seguro que solo muestra logs en modo debug
 */
public class SecureLogger {
    
    private static Boolean isDebuggable;

    private static boolean isDebug(Context context) {
        if (isDebuggable == null) {
            isDebuggable = (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        }
        return isDebuggable;
    }

    public static void init(Context context) {
        isDebuggable = (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }
    
    public static void d(String tag, String message) {
        if (Boolean.TRUE.equals(isDebuggable)) {
            Log.d(tag, message);
        }
    }
    
    public static void i(String tag, String message) {
        if (Boolean.TRUE.equals(isDebuggable)) {
            Log.i(tag, message);
        }
    }
    
    public static void w(String tag, String message) {
        if (Boolean.TRUE.equals(isDebuggable)) {
            Log.w(tag, message);
        }
    }
    
    public static void e(String tag, String message) {
        if (Boolean.TRUE.equals(isDebuggable)) {
            Log.e(tag, message);
        }
    }
    
    public static void e(String tag, String message, Throwable tr) {
        if (Boolean.TRUE.equals(isDebuggable)) {
            Log.e(tag, message, tr);
        }
    }

    public static void error(String tag, String message) {
        if (Boolean.TRUE.equals(isDebuggable)) {
            Log.e(tag, message);
        }
    }
}
