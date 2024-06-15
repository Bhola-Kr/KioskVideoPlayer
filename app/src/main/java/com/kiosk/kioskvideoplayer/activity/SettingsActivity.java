package com.kiosk.kioskvideoplayer.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kiosk.kioskvideoplayer.R;
import com.kiosk.kioskvideoplayer.ads.AdManager;
import com.kiosk.kioskvideoplayer.utils.Utility;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvMemberShip, tvKiosk, tvContact, tvShare, tvRate, tvAbout, tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvMemberShip = findViewById(R.id.tvSubscription);
        tvKiosk = findViewById(R.id.tvVideoSetup);
        tvContact = findViewById(R.id.tvContact);
        tvShare = findViewById(R.id.tvShare);
        tvRate = findViewById(R.id.tvRate);
        tvAbout = findViewById(R.id.tvAbout);
        tvVersion = findViewById(R.id.tvVersion);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(view -> onBackPressed());

        AdManager.loadBanner(SettingsActivity.this, R.id.adView);

        tvMemberShip.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), PaymentsActivity.class)));

        tvKiosk.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SetupActivity.class)));

        tvContact.setOnClickListener(v -> {
            Utility.sendEmail(this);
        });
        tvRate.setOnClickListener(v -> {
            Utility.rateApp(this);
        });
        tvShare.setOnClickListener(v -> {
            Utility.shareApp(this);
        });

        tvAbout.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AboutUsActivity.class));
        });

        tvVersion.setText("Version: " + Utility.getAppVersion(getApplicationContext()));

    }
}