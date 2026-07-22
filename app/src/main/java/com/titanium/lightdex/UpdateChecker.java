package com.titanium.lightdex;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.activity.ComponentActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String FILE_PROVIDER_AUTHORITY = "com.titanium.lightdex.fileprovider";
    private static final String APK_FILE_NAME = "volt_update.apk";
    private static final String EXPECTED_PACKAGE = "com.titanium.lightdex";
    private static final String ALLOWED_HOST_GITHUB = "github.com";
    private static final String ALLOWED_HOST_CDN = "objects.githubusercontent.com";
    private static final String ALLOWED_HOST_API = "api.github.com";

    private final String userAgent;
    private final WeakReference<ComponentActivity> activityRef;
    private final Context context;
    private final String githubUser;
    private final String repoName;
    private final Handler mainHandler;
    private final ExecutorService executor;
    private final OkHttpClient client;

    public UpdateChecker(ComponentActivity activity, String githubUser, String repoName) {
        this.context = activity.getApplicationContext();
        this.activityRef = new WeakReference<>(activity);
        this.githubUser = githubUser;
        this.repoName = repoName;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.userAgent = "VoltApp/" + getCurrentVersion() + " (Android; +https://github.com/Fredrick0K/VoltApp)";
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public void checkForUpdate() {
        executor.execute(() -> {
            try {
                SecureLogger.d(TAG, "=== Update Check Started ===");
                
                ReleaseInfo releaseInfo = fetchReleaseInfo();
                if (releaseInfo == null) return;

                String currentVersion = getCurrentVersion();
                if (isNewerVersion(releaseInfo.version, currentVersion)) {
                    mainHandler.post(() -> showUpdateDialog(releaseInfo));
                }

                SecureLogger.d(TAG, "=== Update Check Complete ===");
            } catch (Exception e) {
                SecureLogger.e(TAG, "Error checking updates: " + e.getMessage());
            }
        });
    }

    private ReleaseInfo fetchReleaseInfo() throws Exception {
        String apiUrl = String.format(GITHUB_API_URL, githubUser, repoName);
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", userAgent)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            return parseReleaseResponse(response.body().string());
        }
    }

    private ReleaseInfo parseReleaseResponse(String jsonString) throws Exception {
        JSONObject jsonResponse = new JSONObject(jsonString);
        String tagName = jsonResponse.getString("tag_name");
        String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        
        String downloadUrl = null;
        JSONArray assets = jsonResponse.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (asset.getString("name").toLowerCase().endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url");
                    break;
                }
            }
        }
        
        if (downloadUrl == null) downloadUrl = jsonResponse.getString("html_url");
        return new ReleaseInfo(version, downloadUrl);
    }

    private String getCurrentVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        try {
            String[] v1 = (latest.startsWith("v") ? latest.substring(1) : latest).split("\\.");
            String[] v2 = (current.startsWith("v") ? current.substring(1) : current).split("\\.");
            int len = Math.max(v1.length, v2.length);
            for (int i = 0; i < len; i++) {
                int n1 = i < v1.length ? Integer.parseInt(v1[i].split("-")[0]) : 0;
                int n2 = i < v2.length ? Integer.parseInt(v2[i].split("-")[0]) : 0;
                if (n1 > n2) return true;
                if (n1 < n2) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void showUpdateDialog(final ReleaseInfo releaseInfo) {
        ComponentActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle(R.string.update_available_title)
                .setMessage(context.getString(R.string.update_available_msg, releaseInfo.version))
                .setPositiveButton(R.string.actualizar, (d, w) -> {
                    if (releaseInfo.downloadUrl.endsWith(".apk")) startDownload(releaseInfo.downloadUrl);
                    else openInBrowser(releaseInfo.downloadUrl);
                })
                .setNegativeButton(R.string.mas_tarde, null)
                .show();
    }

    private void openInBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            SecureLogger.e(TAG, "Error opening browser");
        }
    }

    private void startDownload(final String downloadUrl) {
        ComponentActivity activity = activityRef.get();
        if (activity == null) return;

        if (!isTrustedDownloadUrl(downloadUrl)) {
            SecureLogger.e(TAG, "Rejected untrusted download URL: " + downloadUrl);
            mainHandler.post(() ->
                Toast.makeText(context, "URL de descarga no válida", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(R.string.descargando_update);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        executor.execute(() -> {
            File outputDir = new File(context.getCacheDir(), "updates");
            if (!outputDir.exists()) outputDir.mkdirs();
            File apkFile = new File(outputDir, APK_FILE_NAME);
            Request request = new Request.Builder().url(downloadUrl).header("User-Agent", userAgent).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) throw new IOException("Download failed");

                long totalSize = response.body().contentLength();
                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(apkFile)) {
                    
                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        downloaded += read;
                        if (totalSize > 0) {
                            int progress = (int) (downloaded * 100 / totalSize);
                            mainHandler.post(() -> progressDialog.setProgress(progress));
                        }
                    }
                    fos.flush();
                }

                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    if (verifyApkPackage(apkFile)) {
                        installApk(apkFile);
                    } else {
                        SecureLogger.e(TAG, "APK package verification failed");
                        Toast.makeText(context, "El APK no corresponde a esta aplicación", Toast.LENGTH_SHORT).show();
                        apkFile.delete();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, R.string.error_descarga, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean isTrustedDownloadUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            String host = uri.getHost();
            if (host == null) return false;

            if (host.equals(ALLOWED_HOST_GITHUB) || host.equals(ALLOWED_HOST_CDN)) {
                String path = uri.getPath();
                if (path != null && path.contains("/Fredrick0K/VoltApp/")) return true;
            }

            SecureLogger.e(TAG, "Untrusted host: " + host);
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean verifyApkPackage(File apkFile) {
        try {
            android.content.pm.PackageInfo packageInfo = context.getPackageManager()
                    .getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            if (packageInfo == null) return false;
            return EXPECTED_PACKAGE.equals(packageInfo.packageName);
        } catch (Exception e) {
            SecureLogger.e(TAG, "Package verification error: " + e.getMessage());
            return false;
        }
    }

    private void installApk(File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.error_instalacion, Toast.LENGTH_SHORT).show();
        }
    }

    public void shutdown() {
        if (!executor.isShutdown()) executor.shutdown();
    }

    private static class ReleaseInfo {
        final String version;
        final String downloadUrl;
        ReleaseInfo(String v, String u) { this.version = v; this.downloadUrl = u; }
    }
}
