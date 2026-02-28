package com.titanium.lightdex;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.titanium.lightdex.models.PrecioHora;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    
    private RecyclerView recyclerHoras;
    private TextView tvUbicacion;
    private TextView tvPromedio;
    private TextView tvMasCaro;
    private TextView tvMasBarato;
    private TextView tvPrecioMasCaro;
    private TextView tvPrecioMasBarato;
    private TextView tvPrecioActual;
    private TextView tvHoraActual;
    private TextView tvFecha;
    private ProgressBar progressBar;
    private ImageButton btnActualizarUbicacion;
    private Button btnNotificacion;
    
    private HoraAdapter horaAdapter;
    private ElectricityApiService apiService;
    private List<PrecioHora> preciosDelDia;
    private ErrorCatcher errorCatcher;
    
    private FusedLocationProviderClient fusedLocationClient;
    private String ciudadUsuario = "Madrid";
    
    private ExecutorService executorService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_main);
        
        inicializarVistas();
        inicializarServicios();
        solicitarPermisos();
        obtenerUbicacion();
        cargarPrecios();
        
        NotificationScheduler.programarNotificacionDiaria(this);
    }
    
    private void inicializarVistas() {
        recyclerHoras = findViewById(R.id.recycler_horas);
        tvUbicacion = findViewById(R.id.tv_ubicacion);
        tvPromedio = findViewById(R.id.tv_promedio);
        tvMasCaro = findViewById(R.id.tv_mas_caro);
        tvMasBarato = findViewById(R.id.tv_mas_barato);
        tvPrecioMasCaro = findViewById(R.id.tv_precio_mas_caro);
        tvPrecioMasBarato = findViewById(R.id.tv_precio_mas_barato);
        tvPrecioActual = findViewById(R.id.tv_precio_actual);
        tvHoraActual = findViewById(R.id.tv_hora_actual);
        tvFecha = findViewById(R.id.tv_fecha);
        progressBar = findViewById(R.id.progress_bar);
        btnActualizarUbicacion = findViewById(R.id.btn_actualizar_ubicacion);
        btnNotificacion = findViewById(R.id.btn_notificacion);
        
        btnActualizarUbicacion.setOnClickListener(v -> obtenerUbicacion());
        btnNotificacion.setOnClickListener(v -> enviarNotificacion());
        
        tvFecha.setText(obtenerFechaActual());
        
        horaAdapter = new HoraAdapter();
        recyclerHoras.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerHoras.setAdapter(horaAdapter);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scroll_view), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
    
    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"));
        return sdf.format(new Date());
    }
    
    private void inicializarServicios() {
        apiService = new ElectricityApiService(this);
        errorCatcher = new ErrorCatcher(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executorService = Executors.newSingleThreadExecutor();
        preciosDelDia = new ArrayList<>();
    }
    
    private void solicitarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1002);
            }
        }
    }
    
    private void obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            
            tvUbicacion.setText("Getting location...");
            
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
                @Override
                public boolean isCancellationRequested() {
                    return false;
                }
                
                @NonNull
                @Override
                public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener listener) {
                    return this;
                }
            }).addOnSuccessListener(this, location -> {
                if (location != null) {
                    obtenerNombreCiudad(location.getLatitude(), location.getLongitude());
                } else {
                    tryObtenerUbicacionAnterior();
                }
            }).addOnFailureListener(e -> {
                SecureLogger.e(TAG, "Error GPS: " + e.getMessage());
                errorCatcher.captureError("Get Location GPS", e);
                tvUbicacion.setText("Location: " + ciudadUsuario);
            });
        } else {
            tvUbicacion.setText("Location: " + ciudadUsuario);
        }
    }
    
    private void tryObtenerUbicacionAnterior() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        obtenerNombreCiudad(location.getLatitude(), location.getLongitude());
                    } else {
                        tvUbicacion.setText("Location: " + ciudadUsuario);
                    }
                })
                .addOnFailureListener(e -> {
                    errorCatcher.captureError("Get Last Location", e);
                    tvUbicacion.setText("Location: " + ciudadUsuario);
                });
    }
    
    private void obtenerNombreCiudad(double lat, double lng) {
        executorService.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    String ciudad = addresses.get(0).getLocality();
                    if (ciudad == null) {
                        ciudad = addresses.get(0).getSubAdminArea();
                    }
                    if (ciudad != null) {
                        ciudadUsuario = ciudad;
                    }
                }
            } catch (IOException e) {
                SecureLogger.e(TAG, "Error Geocoder: " + e.getMessage());
                errorCatcher.captureError("Get Location", e);
            } finally {
                runOnUiThread(() -> tvUbicacion.setText("Location: " + ciudadUsuario));
            }
        });
    }
    
    private void cargarPrecios() {
        progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                preciosDelDia = apiService.obtenerPreciosHoy();
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (preciosDelDia != null && !preciosDelDia.isEmpty()) {
                        horaAdapter.actualizarPrecios(preciosDelDia);
                        actualizarResumen();
                        actualizarPrecioActual();
                        errorCatcher.showSuccess("Data loaded successfully");
                    } else {
                        errorCatcher.captureApiError("Load Prices", "No data received from API");
                    }
                });
                
            } catch (Exception e) {
                SecureLogger.e(TAG, "Error: " + e.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    errorCatcher.captureError("Load Prices", e);
                });
            }
        });
    }
    
    private void actualizarResumen() {
        double promedio = apiService.calcularPromedio(preciosDelDia) / 1000;
        PrecioHora masCaro = apiService.obtenerPrecioMasAlto(preciosDelDia);
        PrecioHora masBarato = apiService.obtenerPrecioMasBajo(preciosDelDia);
        
        tvPromedio.setText(String.format("%.4f EUR/kWh", promedio).replace(".", ","));
        
        if (masCaro != null) {
            tvMasCaro.setText(masCaro.getHora());
            tvPrecioMasCaro.setText(String.format("%.4f EUR", masCaro.getPrecioKwh()).replace(".", ","));
        }
        
        if (masBarato != null) {
            tvMasBarato.setText(masBarato.getHora());
            tvPrecioMasBarato.setText(String.format("%.4f EUR", masBarato.getPrecioKwh()).replace(".", ","));
        }
    }
    
    private void actualizarPrecioActual() {
        int horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        for (PrecioHora ph : preciosDelDia) {
            String horaStr = ph.getHora();
            try {
                int horaPrecio = Integer.parseInt(horaStr.split(":")[0]);
                if (horaPrecio == horaActual) {
                    tvPrecioActual.setText(String.format("%.4f", ph.getPrecioKwh()).replace(".", ","));
                    tvHoraActual.setText("At " + horaStr);
                    return;
                }
            } catch (Exception e) {
                // Ignorar
            }
        }
        
        tvPrecioActual.setText("--,--");
        tvHoraActual.setText("No data");
    }
    
    private void enviarNotificacion() {
        if (preciosDelDia == null || preciosDelDia.isEmpty()) {
            errorCatcher.showInfo("Loading prices...");
            progressBar.setVisibility(View.VISIBLE);
            
            executorService.execute(() -> {
                try {
                    preciosDelDia = apiService.obtenerPreciosHoy();
                    
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (preciosDelDia != null && !preciosDelDia.isEmpty()) {
                            horaAdapter.actualizarPrecios(preciosDelDia);
                            NotificationScheduler.mostrarNotificacion(MainActivity.this, preciosDelDia);
                            errorCatcher.showSuccess("Notification sent");
                        } else {
                            errorCatcher.captureApiError("Send Notification", "Could not load prices");
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        errorCatcher.captureError("Send Notification", e);
                    });
                }
            });
            return;
        }
        
        NotificationScheduler.mostrarNotificacion(this, preciosDelDia);
        errorCatcher.showSuccess("Notification sent");
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacion();
            } else {
                Toast.makeText(this, "Using default location", Toast.LENGTH_SHORT).show();
                tvUbicacion.setText("Location: " + ciudadUsuario);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
