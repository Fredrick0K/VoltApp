package com.titanium.lightdex;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    
    private final Context context;
    private final String githubUser;
    private final String repoName;
    private final Handler mainHandler;

    public UpdateChecker(Context context, String githubUser, String repoName) {
        this.context = context;
        this.githubUser = githubUser;
        this.repoName = repoName;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void checkForUpdate() {
        new Thread(() -> {
            try {
                String currentVersion = getCurrentVersion();
                String latestVersion = getLatestVersion();
                
                if (latestVersion != null && isNewerVersion(latestVersion, currentVersion)) {
                    String downloadUrl = getDownloadUrl();
                    mainHandler.post(() -> showUpdateDialog(latestVersion, downloadUrl));
                } else {
                    Log.d(TAG, "App is up to date");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates: " + e.getMessage());
            }
        }).start();
    }

    private String getCurrentVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private String getLatestVersion() throws Exception {
        String apiUrl = String.format(GITHUB_API_URL, githubUser, repoName);
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
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
            
            JSONObject jsonResponse = new JSONObject(response.toString());
            String tagName = jsonResponse.getString("tag_name");
            
            tagName = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            tagName = tagName.split("-")[0];
            
            return tagName;
        } else {
            Log.w(TAG, "GitHub API returned: " + responseCode);
            return null;
        }
    }

    private String getDownloadUrl() throws Exception {
        String apiUrl = String.format(GITHUB_API_URL, githubUser, repoName);
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray assets = jsonResponse.getJSONArray("assets");
        
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getString("name");
            if (name.endsWith(".apk")) {
                return asset.getString("browser_download_url");
            }
        }
        
        return jsonResponse.getString("html_url");
    }

    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        try {
            String[] latestParts = latestVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int latest = i < latestParts.length ? 
                        Integer.parseInt(latestParts[i]) : 0;
                int current = i < currentParts.length ? 
                        Integer.parseInt(currentParts[i]) : 0;
                
                if (latest > current) return true;
                if (latest < current) return false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing version: " + e.getMessage());
        }
        return false;
    }

    private void showUpdateDialog(String latestVersion, String downloadUrl) {
        new AlertDialog.Builder(context)
                .setTitle("New Version Available")
                .setMessage("Version " + latestVersion + " is available. Would you like to update?")
                .setPositiveButton("Update", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }
}
