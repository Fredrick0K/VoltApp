package com.titanium.lightdex;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.security.ProviderInstaller;
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
    private TextView tvCiudad;
    private TextView tvPromedio;
    private TextView tvMasCaro;
    private TextView tvMasBarato;
    private TextView tvPrecioMasCaro;
    private TextView tvPrecioMasBarato;
    private LinearLayout tilesContainer;
    private HorizontalScrollView tilesScroll;
    private CombinedChart priceChart;
    private View layoutSkeleton;
    private View scrollView;
    private View layoutError;
    private Button btnRetry;
    private ImageButton btnThemeToggle;
    private ElectricityApiService apiService;
    private UpdateChecker updateChecker;
    private List<PrecioHora> preciosDelDia;
    private ErrorCatcher errorCatcher;
    private FusedLocationProviderClient fusedLocationClient;
    private String ciudadUsuarioDefo = "Madrid";
    private ExecutorService executorService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (Exception e) {
            SecureLogger.w(TAG, "GMS Provider install failed: " + e.getMessage());
        }

        SecureLogger.init(this);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        actualizarAparienciaBarras();
        
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
        tvCiudad = findViewById(R.id.tv_ciudad);
        tvPromedio = findViewById(R.id.tv_promedio);
        tvMasCaro = findViewById(R.id.tv_mas_caro);
        tvMasBarato = findViewById(R.id.tv_mas_barato);
        tvPrecioMasCaro = findViewById(R.id.tv_precio_mas_caro);
        tvPrecioMasBarato = findViewById(R.id.tv_precio_mas_barato);
        tilesContainer = findViewById(R.id.tiles_container);
        tilesScroll = findViewById(R.id.tiles_scroll);
        priceChart = findViewById(R.id.price_chart);
        layoutSkeleton = findViewById(R.id.layout_skeleton);
        scrollView = findViewById(R.id.scroll_view);
        layoutError = findViewById(R.id.layout_error);
        btnRetry = findViewById(R.id.btn_retry);
        btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        
        startSkeletonAnimation();
        
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> cargarPrecios());
        }

        if (btnThemeToggle != null) {
            actualizarIconoTema();
            btnThemeToggle.setOnClickListener(v -> toggleTheme());
        }

        ImageButton btnInfo = findViewById(R.id.btn_info);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }
        
        setupChart();
        
        tvFecha.setText(obtenerFechaActual());
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
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
        
        priceChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
        });
        
        XAxis xAxis = priceChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        xAxis.setGranularity(1f);
        
        YAxis leftAxis = priceChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.border_subtle));
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        leftAxis.setDrawAxisLine(false);
        
        priceChart.getAxisRight().setEnabled(false);
    }
    
    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM ツ HH:mm", new Locale("es", "ES"));
        return sdf.format(new Date()).toUpperCase();
    }
    
    private void inicializarServicios() {
        apiService = new ElectricityApiService(this);
        updateChecker = new UpdateChecker(this, GITHUB_USER, REPO_NAME);
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
                        ciudadUsuarioDefo = ciudad;
                        runOnUiThread(() -> {
                            tvCiudad.setText(ciudadUsuarioDefo);
                        });
                    }
                }
            } catch (IOException e) {
                SecureLogger.e(TAG, "Error Geocoder: " + e.getMessage());
            }
        });
    }
    
    private void startSkeletonAnimation() {
        Animation shimmer = AnimationUtils.loadAnimation(this, R.anim.shimmer);
        layoutSkeleton.startAnimation(shimmer);
    }

    private void cargarPrecios() {
        layoutSkeleton.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        startSkeletonAnimation();
        
        executorService.execute(() -> {
            try {
                preciosDelDia = apiService.obtenerPreciosHoy();
                
                runOnUiThread(() -> {
                    layoutSkeleton.clearAnimation();
                    layoutSkeleton.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    
                    if (preciosDelDia != null && !preciosDelDia.isEmpty()) {
                        actualizarUI();
                        errorCatcher.showSuccess(getString(R.string.datos_cargados));
                    } else {
                        mostrarError();
                        errorCatcher.captureApiError("Load Prices", getString(R.string.sin_datos));
                    }
                });
                
            } catch (Exception e) {
                SecureLogger.e(TAG, "Error: " + e.getMessage());
                runOnUiThread(() -> {
                    mostrarError();
                    errorCatcher.captureError("Load Prices", e);
                });
            }
        });
    }

    private void mostrarError() {
        layoutSkeleton.clearAnimation();
        layoutSkeleton.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
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
        
        tvPrecioActual.setText(getString(R.string.placeholder_precio));
    }
    
    private void actualizarResumen() {
        double promedio = apiService.calcularPromedio(preciosDelDia) / 1000;
        PrecioHora masCaro = apiService.obtenerPrecioMasAlto(preciosDelDia);
        PrecioHora masBarato = apiService.obtenerPrecioMasBajo(preciosDelDia);
        
        tvPromedio.setText(String.format("%.3f %s", promedio, getString(R.string.unidad_kwh)).replace(".", ","));
        
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
        List<BarEntry> barEntries = new ArrayList<>();
        List<Entry> lineEntries = new ArrayList<>();
        
        for (int i = 0; i < preciosDelDia.size(); i++) {
            float precio = (float) preciosDelDia.get(i).getPrecioKwh();
            barEntries.add(new BarEntry(i, precio));
            lineEntries.add(new Entry(i, precio));
        }
        
        BarDataSet barDataSet = new BarDataSet(barEntries, "Precios Bar");
        barDataSet.setColor(ContextCompat.getColor(this, R.color.accent_primary_dim));
        barDataSet.setDrawValues(false);
        
        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Precios Line");
        lineDataSet.setColor(ContextCompat.getColor(this, R.color.accent_primary));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(3f);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawValues(false);
        lineDataSet.setCubicIntensity(0.1f);
        
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.8f);
        
        LineData lineData = new LineData(lineDataSet);
        
        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        combinedData.setData(lineData);
        
        priceChart.setData(combinedData);
        priceChart.invalidate();
    }
    
    private void crearTiles() {
        tilesContainer.removeAllViews();
        
        if (preciosDelDia == null || preciosDelDia.isEmpty()) return;

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (PrecioHora ph : preciosDelDia) {
            double p = ph.getPrecioKwh();
            if (p < min) min = p;
            if (p > max) max = p;
        }

        double range = max - min;
        double thresholdLow = min + (range / 3.0);
        double thresholdHigh = min + (2.0 * range / 3.0);

        int horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        final int[] currentHourIndex = {-1};
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
            if (esActual) currentHourIndex[0] = i;
            
            boolean esBarato = ph.getPrecioKwh() <= thresholdLow;
            boolean esCaro = ph.getPrecioKwh() >= thresholdHigh;
            
            LinearLayout tile = crearTile(ph, horaStr, esActual, esBarato, esCaro);
            tilesContainer.addView(tile);
        }
        
        if (currentHourIndex[0] >= 0) {
            int targetIndex = currentHourIndex[0];
            tilesScroll.post(() -> {
                int tileWidth = dpToPx(116); // 104dp + 12dp margin
                int scrollTo = targetIndex * tileWidth;
                
                ObjectAnimator animator = ObjectAnimator.ofInt(tilesScroll, "scrollX", scrollTo);
                animator.setDuration(1500); 
                animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
                animator.start();
            });
        }
    }
    
    private LinearLayout crearTile(PrecioHora ph, String hora, boolean esActual, boolean esBarato, boolean esCaro) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dpToPx(16), dpToPx(20), dpToPx(16), dpToPx(20));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(104), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dpToPx(12), 0);
        tile.setLayoutParams(params);
        
        int bgColor;
        int textColor = ContextCompat.getColor(this, R.color.text_primary);
        int strokeColor;
        int labelColor;
        
        if (esActual) {
            bgColor = ContextCompat.getColor(this, R.color.accent_primary);
            textColor = Color.BLACK;
            strokeColor = bgColor;
            labelColor = Color.BLACK;
        } else if (esBarato) {
            bgColor = ContextCompat.getColor(this, R.color.price_low_bg);
            strokeColor = ContextCompat.getColor(this, R.color.price_low);
            labelColor = strokeColor;
        } else if (esCaro) {
            bgColor = ContextCompat.getColor(this, R.color.price_high_bg);
            strokeColor = ContextCompat.getColor(this, R.color.price_high);
            labelColor = strokeColor;
        } else {
            bgColor = ContextCompat.getColor(this, R.color.price_mid_bg);
            strokeColor = ContextCompat.getColor(this, R.color.price_mid);
            labelColor = strokeColor;
        }
        
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(20));
        shape.setColor(bgColor);
        shape.setStroke(dpToPx(1), strokeColor);
        tile.setBackground(shape);
        
        TextView label = new TextView(this);
        String labelText;
        if (esActual) {
            labelText = getString(R.string.ahora);
        } else if (esBarato) {
            labelText = getString(R.string.valle);
        } else if (esCaro) {
            labelText = getString(R.string.punta);
        } else {
            labelText = getString(R.string.llano);
        }
        
        label.setText(labelText);
        label.setTextSize(9);
        label.setTextColor(labelColor);
        label.setAllCaps(true);
        label.setLetterSpacing(0.1f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        
        TextView precio = new TextView(this);
        precio.setText(String.format(Locale.getDefault(), "%.3f", ph.getPrecioKwh()).replace(".", ","));
        precio.setTextSize(18);
        precio.setTextColor(textColor);
        precio.setTypeface(null, android.graphics.Typeface.BOLD);
        precio.setPadding(0, dpToPx(8), 0, 0);
        
        TextView horaTv = new TextView(this);
        horaTv.setText(hora);
        horaTv.setTextSize(11);
        horaTv.setTextColor(esActual ? Color.BLACK : ContextCompat.getColor(this, R.color.text_secondary));
        horaTv.setPadding(0, dpToPx(2), 0, 0);
        
        tile.addView(label);
        tile.addView(precio);
        tile.addView(horaTv);
        
        return tile;
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private void checkForUpdates() {
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
        if (updateChecker != null) {
            updateChecker.shutdown();
        }
    }

    private void actualizarAparienciaBarras() {
        int currentMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = (currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!isNightMode);
        controller.setAppearanceLightNavigationBars(!isNightMode);
    }

    private void toggleTheme() {
        int currentMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        }
        actualizarAparienciaBarras();
    }

    private void actualizarIconoTema() {
        int currentMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (btnThemeToggle != null) {
            if (currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                btnThemeToggle.setImageResource(R.drawable.ic_sun);
            } else {
                btnThemeToggle.setImageResource(R.drawable.ic_moon);
            }
        }
    }
}
