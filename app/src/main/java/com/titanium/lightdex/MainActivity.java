package com.titanium.lightdex;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
    private static final String GITHUB_USER = "Fredrick0K";
    private static final String REPO_NAME = "VoltApp";
    
    private TextView tvPrecioActual;
    private TextView tvFecha;
    private TextView tvPromedio;
    private TextView tvMasCaro;
    private TextView tvMasBarato;
    private TextView tvPrecioMasCaro;
    private TextView tvPrecioMasBarato;
    private LinearLayout tilesContainer;
    private LineChart priceChart;
    private ProgressBar progressBar;
    private LinearLayout btnInfo;
    
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
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        
        setContentView(R.layout.activity_main);
        
        inicializarVistas();
        inicializarServicios();
        solicitarPermisos();
        obtenerUbicacion();
        cargarPrecios();
        
        checkForUpdates();
    }
    
    private void inicializarVistas() {
        tvPrecioActual = findViewById(R.id.tv_precio_actual);
        tvFecha = findViewById(R.id.tv_fecha);
        tvPromedio = findViewById(R.id.tv_promedio);
        tvMasCaro = findViewById(R.id.tv_mas_caro);
        tvMasBarato = findViewById(R.id.tv_mas_barato);
        tvPrecioMasCaro = findViewById(R.id.tv_precio_mas_caro);
        tvPrecioMasBarato = findViewById(R.id.tv_precio_mas_barato);
        tilesContainer = findViewById(R.id.tiles_container);
        priceChart = findViewById(R.id.price_chart);
        progressBar = findViewById(R.id.progress_bar);
        btnInfo = findViewById(R.id.btn_info);
        
        setupChart();
        
        tvFecha.setText(obtenerFechaActual());
        
        btnInfo.setOnClickListener(v -> mostrarAcercaDe());
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, 0, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
    
    private void setupChart() {
        priceChart.setBackgroundColor(Color.TRANSPARENT);
        priceChart.getDescription().setEnabled(false);
        priceChart.getLegend().setEnabled(false);
        priceChart.setTouchEnabled(false);
        priceChart.setDragEnabled(false);
        priceChart.setScaleEnabled(false);
        priceChart.setPinchZoom(false);
        priceChart.setDrawGridBackground(false);
        
        XAxis xAxis = priceChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setGranularity(1f);
        
        YAxis leftAxis = priceChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setTextColor(Color.GRAY);
        
        priceChart.getAxisRight().setEnabled(false);
    }
    
    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM • HH:mm", new Locale("es", "ES"));
        return sdf.format(new Date()).toUpperCase();
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
                }
            }).addOnFailureListener(e -> {
                SecureLogger.e(TAG, "Error GPS: " + e.getMessage());
            });
        }
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
                        actualizarUI();
                        errorCatcher.showSuccess("Data loaded");
                    } else {
                        errorCatcher.captureApiError("Load Prices", "No data");
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
    
    private void actualizarUI() {
        actualizarPrecioActual();
        actualizarResumen();
        actualizarChart();
        crearTiles();
    }
    
    private void actualizarPrecioActual() {
        int horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        for (PrecioHora ph : preciosDelDia) {
            String horaStr = ph.getHora();
            try {
                int horaPrecio = Integer.parseInt(horaStr.split(":")[0]);
                if (horaPrecio == horaActual) {
                    String precio = String.format("%.3f", ph.getPrecioKwh()).replace(".", ",");
                    tvPrecioActual.setText(precio);
                    return;
                }
            } catch (Exception e) {
                // Ignorar
            }
        }
        
        tvPrecioActual.setText("--,--");
    }
    
    private void actualizarResumen() {
        double promedio = apiService.calcularPromedio(preciosDelDia) / 1000;
        PrecioHora masCaro = apiService.obtenerPrecioMasAlto(preciosDelDia);
        PrecioHora masBarato = apiService.obtenerPrecioMasBajo(preciosDelDia);
        
        tvPromedio.setText(String.format("%.3f €/kWh", promedio).replace(".", ","));
        
        if (masCaro != null) {
            tvMasCaro.setText(masCaro.getHora());
            tvPrecioMasCaro.setText(String.format("%.3f", masCaro.getPrecioKwh()).replace(".", ","));
        }
        
        if (masBarato != null) {
            tvMasBarato.setText(masBarato.getHora());
            tvPrecioMasBarato.setText(String.format("%.3f", masBarato.getPrecioKwh()).replace(".", ","));
        }
    }
    
    private void actualizarChart() {
        List<Entry> entries = new ArrayList<>();
        
        for (int i = 0; i < preciosDelDia.size(); i++) {
            float precio = (float) preciosDelDia.get(i).getPrecioKwh();
            entries.add(new Entry(i, precio));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Precios");
        dataSet.setColor(ContextCompat.getColor(this, R.color.metro_primary));
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.metro_primary_dim));
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2.5f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        LineData lineData = new LineData(dataSet);
        priceChart.setData(lineData);
        priceChart.invalidate();
    }
    
    private void crearTiles() {
        tilesContainer.removeAllViews();
        
        int horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        for (int i = 0; i < preciosDelDia.size(); i++) {
            PrecioHora ph = preciosDelDia.get(i);
            String horaStr = ph.getHora();
            int hora;
            try {
                hora = Integer.parseInt(horaStr.split(":")[0]);
            } catch (Exception e) {
                hora = i;
            }
            
            boolean esActual = (hora == horaActual);
            boolean esBarato = ph.getPrecioKwh() < 0.13;
            boolean esCaro = ph.getPrecioKwh() > 0.17;
            
            LinearLayout tile = crearTile(ph, horaStr, esActual, esBarato, esCaro);
            tilesContainer.addView(tile);
        }
    }
    
    private LinearLayout crearTile(PrecioHora ph, String hora, boolean esActual, boolean esBarato, boolean esCaro) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(100), dpToPx(100));
        params.setMargins(0, 0, dpToPx(8), 0);
        tile.setLayoutParams(params);
        
        if (esActual) {
            tile.setBackgroundColor(ContextCompat.getColor(this, R.color.metro_primary));
        } else {
            tile.setBackgroundColor(ContextCompat.getColor(this, R.color.metro_surface_tile));
        }
        
        TextView label = new TextView(this);
        label.setText(esActual ? "AHORA" : (esBarato ? "VALLE" : (esCaro ? "PUNTA" : "NORMAL")));
        label.setTextSize(10);
        label.setTextColor(esActual ? Color.BLACK : Color.GRAY);
        label.setAllCaps(true);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        
        TextView precio = new TextView(this);
        precio.setText(String.format("%.3f", ph.getPrecioKwh()).replace(".", ","));
        precio.setTextSize(20);
        precio.setTextColor(esActual ? Color.BLACK : Color.WHITE);
        precio.setTypeface(null, android.graphics.Typeface.BOLD);
        precio.setPadding(0, dpToPx(8), 0, 0);
        
        TextView horaTv = new TextView(this);
        horaTv.setText(hora);
        horaTv.setTextSize(11);
        horaTv.setTextColor(esActual ? Color.BLACK : Color.GRAY);
        horaTv.setPadding(0, dpToPx(4), 0, 0);
        
        LinearLayout priceContainer = new LinearLayout(this);
        priceContainer.setOrientation(LinearLayout.VERTICAL);
        priceContainer.addView(precio);
        priceContainer.addView(horaTv);
        
        tile.addView(label);
        tile.addView(priceContainer);
        
        return tile;
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private void checkForUpdates() {
        UpdateChecker updateChecker = new UpdateChecker(this, GITHUB_USER, REPO_NAME);
        updateChecker.checkForUpdate();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacion();
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
    
    private void mostrarAcercaDe() {
        String versionName = "1.0";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Use default
        }
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_about, null);
        
        TextView tvVersion = dialogView.findViewById(R.id.tv_version);
        tvVersion.setText("⚡ " + versionName);
        
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        btnClose.setColorFilter(Color.BLACK);
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_Volt_Dialog)
                .setView(dialogView)
                .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(R.color.metro_surface);
    }
}
