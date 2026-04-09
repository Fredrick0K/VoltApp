package com.titanium.lightdex;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import androidx.activity.ComponentActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String FILE_PROVIDER_AUTHORITY = "com.titanium.lightdex.fileprovider";

    private String userAgent;
    private WeakReference<ComponentActivity> activityRef;

    private final Context context;
    private final String githubUser;
    private final String repoName;
    private final Handler mainHandler;
    private final ExecutorService executor;

    public UpdateChecker(ComponentActivity activity, String githubUser, String repoName) {
        this.context = activity.getApplicationContext();
        this.activityRef = new WeakReference<>(activity);
        this.githubUser = githubUser;
        this.repoName = repoName;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.userAgent = "VoltApp-Android/" + getCurrentVersion();
    }

    public void checkForUpdate() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SecureLogger.d(TAG, "=== Update Check Started ===");

                    ReleaseInfo releaseInfo = fetchReleaseInfo();
                    if (releaseInfo == null) {
                        SecureLogger.e(TAG, "Could not fetch release info");
                        return;
                    }

                    String currentVersion = getCurrentVersion();
                    SecureLogger.i(TAG, "Current version: " + currentVersion);
                    SecureLogger.i(TAG, "Latest version: " + releaseInfo.version);

                    boolean needsUpdate = isNewerVersion(releaseInfo.version, currentVersion);
                    SecureLogger.i(TAG, "Update needed: " + needsUpdate);

                    if (needsUpdate) {
                        SecureLogger.i(TAG, "Update available! Showing dialog...");
                        final ReleaseInfo info = releaseInfo;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                showUpdateDialog(info);
                            }
                        });
                    } else {
                        SecureLogger.d(TAG, "App is up to date - no action needed");
                    }

                    SecureLogger.d(TAG, "=== Update Check Complete ===");
                } catch (Exception e) {
                    SecureLogger.e(TAG, "Error checking for updates: " + e.getMessage());
                }
            }
        });
    }

    private ReleaseInfo fetchReleaseInfo() throws Exception {
        String apiUrl = String.format(GITHUB_API_URL, githubUser, repoName);
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return parseReleaseResponse(response.toString());
        } else {
            SecureLogger.w(TAG, "GitHub API returned: " + responseCode);
            return null;
        }
    }

    private ReleaseInfo parseReleaseResponse(String jsonString) throws Exception {
        JSONObject jsonResponse = new JSONObject(jsonString);

        String tagName = jsonResponse.getString("tag_name");
        tagName = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        tagName = tagName.split("-")[0];

        String downloadUrl = null;
        JSONArray assets = jsonResponse.getJSONArray("assets");
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getString("name");
            if (name.endsWith(".apk")) {
                downloadUrl = asset.getString("browser_download_url");
                break;
            }
        }

        if (downloadUrl == null) {
            downloadUrl = jsonResponse.getString("html_url");
        }

        return new ReleaseInfo(tagName, downloadUrl);
    }

    private String getCurrentVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        try {
            latestVersion = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;
            currentVersion = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;

            String[] latestParts = latestVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");

            int maxLength = Math.max(latestParts.length, currentParts.length);

            SecureLogger.d(TAG, "Comparing versions - Latest: " + latestVersion + ", Current: " + currentVersion);

            for (int i = 0; i < maxLength; i++) {
                int latest = i < latestParts.length ?
                        Integer.parseInt(latestParts[i]) : 0;
                int current = i < currentParts.length ?
                        Integer.parseInt(currentParts[i]) : 0;

                SecureLogger.d(TAG, "Part[" + i + "] - Latest: " + latest + ", Current: " + current);

                if (latest > current) {
                    SecureLogger.d(TAG, "Latest is newer - returning true");
                    return true;
                }
                if (latest < current) {
                    SecureLogger.d(TAG, "Current is newer - returning false");
                    return false;
                }
            }
            SecureLogger.d(TAG, "Versions are equal - returning false");
            return false;
        } catch (NumberFormatException e) {
            SecureLogger.e(TAG, "Error parsing version: " + e.getMessage());
            return false;
        }
    }

    private void showUpdateDialog(final ReleaseInfo releaseInfo) {
        ComponentActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            SecureLogger.d(TAG, "Activity not available, skipping dialog");
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Nueva version disponible")
                .setMessage("La version " + releaseInfo.version + " esta disponible. Desea actualizar ahora?")
                .setPositiveButton("Actualizar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadAndInstall(releaseInfo.downloadUrl);
                    }
                })
                .setNegativeButton("Mas tarde", null)
                .setCancelable(true)
                .create();

        dialog.show();
    }

    private void downloadAndInstall(final String downloadUrl) {
        ComponentActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            SecureLogger.d(TAG, "Activity not available, cannot show progress");
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Descargando actualizacion...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File apkFile = downloadApk(downloadUrl);
                    if (apkFile != null && apkFile.exists()) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                installApk(apkFile);
                            }
                        });
                    } else {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Toast.makeText(context, "Error al descargar", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (final Exception e) {
                    SecureLogger.e(TAG, "Download error: " + e.getMessage());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private File downloadApk(String downloadUrl) throws Exception {
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + responseCode);
        }

        String fileName = "volt_update.apk";
        File outputDir = new File(context.getCacheDir(), "updates");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File outputFile = new File(outputDir, fileName);

        java.io.InputStream input = connection.getInputStream();
        FileOutputStream output = new FileOutputStream(outputFile);

        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }

        output.close();
        input.close();
        connection.disconnect();

        SecureLogger.d(TAG, "Download complete: " + outputFile.length() + " bytes");
        return outputFile;
    }

    private void installApk(File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    FILE_PROVIDER_AUTHORITY,
                    apkFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
        } catch (Exception e) {
            SecureLogger.e(TAG, "Install error: " + e.getMessage());
            Toast.makeText(context, "No se pudo iniciar la instalacion", Toast.LENGTH_SHORT).show();
        }
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private static class ReleaseInfo {
        final String version;
        final String downloadUrl;

        ReleaseInfo(String version, String downloadUrl) {
            this.version = version;
            this.downloadUrl = downloadUrl;
        }
    }
}
