package com.titanium.lightdex;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.core.view.WindowCompat;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    private static final String GITHUB_PROFILE = "https://github.com/Fredrick0K";
    private static final String GITHUB_REPO = "https://github.com/Fredrick0K/VoltApp";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        
        setContentView(R.layout.page_about);
        
        setupVersion();
        setupBackButton();
        setupGithubLinks();
        
        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });
    }
    
    private void setupVersion() {
        String versionName = "1.0";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Use default
        }
        
        TextView tvVersionNumber = findViewById(R.id.tv_version_number);
        if (tvVersionNumber != null) {
            tvVersionNumber.setText("Versión " + versionName);
        }
        
        TextView tvVersionBuild = findViewById(R.id.tv_version_build);
        if (tvVersionBuild != null) {
            tvVersionBuild.setText(versionName);
        }
    }
    
    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }
    }

    private void setupGithubLinks() {
        TextView tvDev = findViewById(R.id.tv_dev);
        if (tvDev != null) {
            tvDev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openUrl(GITHUB_PROFILE);
                }
            });
        }

        TextView tvGithub = findViewById(R.id.tv_github);
        if (tvGithub != null) {
            tvGithub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openUrl(GITHUB_REPO);
                }
            });
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
