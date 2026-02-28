package com.titanium.lightdex;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Error handler for capturing and displaying technical error messages
 */
public class ErrorCatcher {
    
    private static final String TAG = "ErrorCatcher";
    private Context context;
    
    public ErrorCatcher(Context context) {
        this.context = context;
    }
    
    /**
     * Capture exception and display technical error toast
     * @param operation Failed operation name
     * @param error The exception
     */
    public void captureError(String operation, Throwable error) {
        String errorMessage = buildErrorMessage(operation, error);
        SecureLogger.error(TAG, errorMessage);
        
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * Capture API/network error
     * @param operation Failed operation name
     * @param error Error message
     */
    public void captureApiError(String operation, String error) {
        String errorMessage = "ERR: " + operation + " | " + error;
        SecureLogger.error(TAG, errorMessage);
        
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * Display success message
     * @param message Success message
     */
    public void showSuccess(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "OK: " + message, Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Display info message
     * @param message Info message
     */
    public void showInfo(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "INFO: " + message, Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Build technical error message
     */
    private String buildErrorMessage(String operation, Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("ERR: ").append(operation);
        
        if (error != null) {
            sb.append(" | ").append(error.getClass().getSimpleName());
            if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                sb.append(": ").append(error.getMessage());
            }
        }
        
        return sb.toString();
    }
}
