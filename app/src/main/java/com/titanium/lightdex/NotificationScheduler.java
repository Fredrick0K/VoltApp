package com.titanium.lightdex;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.titanium.lightdex.models.PrecioHora;

import java.util.Calendar;
import java.util.List;

import com.titanium.lightdex.ElectricityApiService;

/**
 * Clase para programar y mostrar notificaciones diarias de precios de electricidad
 */
public class NotificationScheduler {
    
    private static final String TAG = "NotificationScheduler";
    private static final String CHANNEL_ID = "electricidad_precios";
    private static final int NOTIFICATION_ID = 1001;
    private static final int ALARM_REQUEST_CODE = 2001;
    
    /**
     * Programa una notificación diaria a las 8:00 AM
     * @param context Contexto de la aplicación
     */
    public static void programarNotificacionDiaria(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Configurar para las 8:00 AM
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        // Si ya pasaron las 8 AM hoy, programar para mañana
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Programar alarma repetitiva diaria
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
            }
            
            SecureLogger.d(TAG, "Notificación diaria programada para las 8:00 AM");
        }
    }
    
    /**
     * Crea el canal de notificaciones (necesario en Android 8.0+)
     * @param context Contexto de la aplicación
     */
    public static void crearCanalNotificacion(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombre = "Precios de Electricidad";
            String descripcion = "Notificaciones diarias con los precios de la luz";
            int importancia = NotificationManager.IMPORTANCE_DEFAULT;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, nombre, importancia);
            channel.setDescription(descripcion);
            
            NotificationManager notificationManager = 
                    context.getSystemService(NotificationManager.class);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                SecureLogger.d(TAG, "Canal de notificación creado");
            }
        }
    }
    
    /**
     * Muestra una notificación con el resumen de precios del día
     * @param context Contexto de la aplicación
     * @param precios Lista de precios del día
     */
    public static void mostrarNotificacion(Context context, List<PrecioHora> precios) {
        crearCanalNotificacion(context);
        
        if (precios == null || precios.isEmpty()) {
            SecureLogger.w(TAG, "No hay precios para mostrar en la notificación");
            return;
        }
        
        ElectricityApiService apiService = new ElectricityApiService(context);
        PrecioHora masCaro = apiService.obtenerPrecioMasAlto(precios);
        PrecioHora masBarato = apiService.obtenerPrecioMasBajo(precios);
        
        String rangoCaro = obtenerRangoHorario(precios, masCaro);
        String rangoBarato = obtenerRangoHorario(precios, masBarato);
        
        String precioCaroFormateado = String.format("%.4f", masCaro.getPrecioKwh()).replace(".", ",");
        String precioBaratoFormateado = String.format("%.4f", masBarato.getPrecioKwh()).replace(".", ",");
        
        String contenido = "🔴 Más caro: " + rangoCaro + " - " + precioCaroFormateado + " €/kWh\n" +
                          "🟢 Más barato: " + rangoBarato + " - " + precioBaratoFormateado + " €/kWh";
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("⚡ Precios de la Luz")
                .setContentText("Más caro: " + precioCaroFormateado + " - Más barato: " + precioBaratoFormateado)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contenido))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            SecureLogger.d(TAG, "Notificación mostrada");
        }
    }
    
    private static String obtenerRangoHorario(List<PrecioHora> precios, PrecioHora precioObj) {
        if (precioObj == null || precios.isEmpty()) return "--:--";
        
        int indice = -1;
        for (int i = 0; i < precios.size(); i++) {
            if (precios.get(i).getHora().equals(precioObj.getHora())) {
                indice = i;
                break;
            }
        }
        
        if (indice == -1) return precioObj.getHora();
        
        String horaInicio = precios.get(indice).getHora();
        String horaFin;
        
        if (indice + 1 < precios.size()) {
            horaFin = precios.get(indice + 1).getHora();
        } else {
            horaFin = "23:59";
        }
        
        return horaInicio + " - " + horaFin;
    }
    
    /**
     * BroadcastReceiver que se ejecuta cuando se dispara la alarma
     */
    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SecureLogger.d(TAG, "Alarma recibida, obteniendo precios...");
            
            // Obtener precios en un hilo secundario
            new Thread(() -> {
                try {
                    ElectricityApiService apiService = new ElectricityApiService(context);
                    List<PrecioHora> precios = apiService.obtenerPreciosHoy();
                    
                    if (precios != null && !precios.isEmpty()) {
                        mostrarNotificacion(context, precios);
                    }
                    
                    // Re-programar para el día siguiente
                    programarNotificacionDiaria(context);
                    
                } catch (Exception e) {
                    SecureLogger.e(TAG, "Error al obtener precios para notificación: " + e.getMessage());
                }
            }).start();
        }
    }
    
    /**
     * Cancela las notificaciones programadas
     * @param context Contexto de la aplicación
     */
    public static void cancelarNotificaciones(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            SecureLogger.d(TAG, "Notificaciones canceladas");
        }
    }
}
