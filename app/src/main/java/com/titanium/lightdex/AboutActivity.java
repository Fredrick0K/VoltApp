package com.titanium.lightdex;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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
        setupCopyright();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.about_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

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
        View cardDev = findViewById(R.id.card_dev);
        if (cardDev != null) {
            cardDev.setOnClickListener(v -> openUrl(GITHUB_PROFILE));
        }

        View cardGithub = findViewById(R.id.card_github);
        if (cardGithub != null) {
            cardGithub.setOnClickListener(v -> openUrl(GITHUB_REPO));
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void setupCopyright() {
        TextView tvCopyright = findViewById(R.id.tv_copyright);
        if (tvCopyright != null) {
            tvCopyright.setText(getString(R.string.derechos_autor, getString(R.string.user_git)));
        }
    }
}
