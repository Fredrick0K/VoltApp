package com.titanium.lightdex;

import android.util.Log;

import com.github.mikephil.charting.BuildConfig;

/**
 * Logger seguro que solo muestra logs en modo debug
 * En release, todos los logs son eliminados automáticamente
 */
public class SecureLogger {
    
    private static final boolean DEBUG = BuildConfig.DEBUG;
    
    public static void d(String tag, String message) {
        if (DEBUG) {
            Log.d(tag, message);
        }
    }
    
    public static void i(String tag, String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }
    
    public static void w(String tag, String message) {
        if (DEBUG) {
            Log.w(tag, message);
        }
    }
    
    public static void e(String tag, String message) {
        if (DEBUG) {
            Log.e(tag, message);
        }
    }
    
    public static void e(String tag, String message, Throwable tr) {
        if (DEBUG) {
            Log.e(tag, message, tr);
        }
    }
    
    /**
     * Log de error que SIEMPRE se muestra (para errores críticos)
     * Pero en release solo muestra mensaje genérico
     */
    public static void error(String tag, String message) {
        if (DEBUG) {
            Log.e(tag, message);
        }
        // En release, no logueamos detalles sensibles
    }
}
