package com.titanium.lightdex;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.ComponentActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s/releases/latest";

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

        this.client = new OkHttpClient.Builder().build();
    }

    public void checkForUpdate() {
        executor.execute(() -> {
            try {
                ReleaseInfo releaseInfo = fetchReleaseInfo();
                if (releaseInfo == null) return;

                String currentVersion = getCurrentVersion();
                if (isNewerVersion(releaseInfo.version, currentVersion)) {
                    mainHandler.post(() -> showUpdateDialog(releaseInfo));
                }
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

        String downloadUrl = jsonResponse.getString("html_url");
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

        return new ReleaseInfo(version, downloadUrl);
    }

    private String getCurrentVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        try {
            latest = latest.startsWith("v") ? latest.substring(1) : latest;
            current = current.startsWith("v") ? current.substring(1) : current;

            String[] v1 = latest.split("\\.");
            String[] v2 = current.split("\\.");
            int len = Math.max(v1.length, v2.length);

            for (int i = 0; i < len; i++) {
                int n1 = i < v1.length ? Integer.parseInt(v1[i]) : 0;
                int n2 = i < v2.length ? Integer.parseInt(v2[i]) : 0;
                if (n1 > n2) return true;
                if (n1 < n2) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void showUpdateDialog(ReleaseInfo releaseInfo) {
        ComponentActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle(R.string.update_available_title)
                .setMessage(context.getString(R.string.update_available_msg, releaseInfo.version))
                .setPositiveButton(R.string.actualizar, (d, w) -> openDownloadUrl(releaseInfo.downloadUrl))
                .setNegativeButton(R.string.mas_tarde, null)
                .show();
    }

    private void openDownloadUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            SecureLogger.e(TAG, "Error opening browser: " + e.getMessage());
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
