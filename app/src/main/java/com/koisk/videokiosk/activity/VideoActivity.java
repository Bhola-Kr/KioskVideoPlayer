package com.koisk.videokiosk.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.koisk.videokiosk.R;
import com.koisk.videokiosk.ads.AdManager;
import com.koisk.videokiosk.firebase.FirebaseConstants;
import com.koisk.videokiosk.payment.SubscriptionActivity;
import com.koisk.videokiosk.storage.Constant;
import com.koisk.videokiosk.storage.LocalData;
import com.koisk.videokiosk.storage.SpDatabase;
import com.koisk.videokiosk.utils.RemoteConfigManager;
import com.koisk.videokiosk.utils.VideoPlayer;

public class VideoActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageView imageView, exitIcon, removeWatermarkIcon, removeWatermarkIconTop;
    private TextView watermarkText, watermarkTextTop;

    private VideoPlayer mVideoPlayer;
    private SpDatabase spDatabase;

    private boolean orientation, showExitIcon, showControls,
            statusBar, volume, backButton, recentButton;

    // 🔐 Premium & Internet
    private boolean isPremiumUser = false;
    private AlertDialog offlineDialog;

    // 🌐 Real-time network
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        spDatabase = new SpDatabase(this);

        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        exitIcon = findViewById(R.id.exitIcon);
        removeWatermarkIcon = findViewById(R.id.removeWatermarkIcon);
        removeWatermarkIconTop = findViewById(R.id.removeWatermarkIconTop);
        watermarkText = findViewById(R.id.watermarkText);
        watermarkTextTop = findViewById(R.id.watermarkTextTop);

        watermarkText.setSelected(true);
        watermarkTextTop.setSelected(true);

        exitIcon.setOnClickListener(v -> showExitConfirmationDialog());
        removeWatermarkIcon.setOnClickListener(v -> showUpgradeDialog());
        removeWatermarkIconTop.setOnClickListener(v -> showUpgradeDialog());
        watermarkText.setOnClickListener(v -> showUpgradeDialog());
        watermarkTextTop.setOnClickListener(v -> showUpgradeDialog());

        videoSetup();
        listenPremiumAndUpdateWatermark();
    }

    // 🔥 PREMIUM STATUS LISTENER
    private void listenPremiumAndUpdateWatermark() {

        String deviceId = RemoteConfigManager.getDeviceId(getApplicationContext());

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference(FirebaseConstants.APP_REF)
                .child(FirebaseConstants.USERS_REF)
                .child(deviceId);

        userRef.child("isPremium").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                isPremiumUser = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));

                if (isPremiumUser) {
                    hideWatermarks();
                    dismissOfflineDialog();
                    LocalData.bannerAd = false;
                    LocalData.interstitialAd = false;
                    AdManager.hideBannerAd(VideoActivity.this, R.id.adView);
                } else {
                    showWatermarks();
                    LocalData.bannerAd = true;
                    LocalData.interstitialAd = true;
                    AdManager.loadBanner(VideoActivity.this, R.id.adView);
                    checkOfflineRestriction();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // 🌐 SIMPLE INTERNET CHECK (fallback)
    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    // 🚫 OFFLINE RESTRICTION
    private void checkOfflineRestriction() {
        if (!isInternetAvailable() && !isPremiumUser) {
            showOfflineDialog();
            if (videoView.isPlaying()) videoView.pause();
        } else {
            dismissOfflineDialog();
            if (!videoView.isPlaying()) videoView.start();
        }
    }

    // 🚨 NON-CANCELABLE DIALOG
    private void showOfflineDialog() {
        if (offlineDialog != null && offlineDialog.isShowing()) return;

        offlineDialog = new AlertDialog.Builder(this)
                .setTitle("Offline Access Restricted")
                .setMessage(
                        "Offline playback is available only for Premium users.\n\n" +
                                "Please connect to the internet or upgrade to Premium."
                )
                .setCancelable(false)
                .setPositiveButton("Upgrade",
                        (d, w) -> startActivity(
                                new Intent(this, SubscriptionActivity.class)))
                .create();

        offlineDialog.show();
    }

    private void dismissOfflineDialog() {
        if (offlineDialog != null && offlineDialog.isShowing()) {
            offlineDialog.dismiss();
            offlineDialog = null;
        }
    }

    // 📡 REAL-TIME NETWORK MONITOR
    private void registerNetworkCallback() {

        connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> checkOfflineRestriction());
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> checkOfflineRestriction());
            }
        };

        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private void unregisterNetworkCallback() {
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {}
    }

    private void videoSetup() {

        orientation = spDatabase.getBoolean(Constant.KEY_ORIENTATION);
        showControls = spDatabase.getBoolean(Constant.KEY_SHOW_VIDEO_CONTROLS);
        showExitIcon = spDatabase.getBoolean(Constant.KEY_EXIT_VIDEO_CONTROLS);
        statusBar = spDatabase.getBoolean(Constant.KEY_STATUS_BAR);
        volume = spDatabase.getBoolean(Constant.KEY_VOLUME);
        backButton = spDatabase.getBoolean(Constant.KEY_BACK_BUTTON);
        recentButton = spDatabase.getBoolean(Constant.KEY_RECENT_BUTTON);

        exitIcon.setVisibility(showExitIcon ? View.VISIBLE : View.GONE);

        setRequestedOrientation(
                orientation ? ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        );

        if (showControls) {
            videoView.setMediaController(new MediaController(this));
        }

        if (statusBar) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }

        mVideoPlayer = new VideoPlayer(this, videoView, imageView);
        mVideoPlayer.videoSetup();
    }

    private void showUpgradeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Upgrade to Premium")
                .setMessage("Remove watermarks and unlock offline playback.")
                .setPositiveButton("Upgrade",
                        (d, w) -> startActivity(
                                new Intent(this, SubscriptionActivity.class)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void hideWatermarks() {
        removeWatermarkIcon.setVisibility(View.GONE);
        removeWatermarkIconTop.setVisibility(View.GONE);
        watermarkText.setVisibility(View.GONE);
        watermarkTextTop.setVisibility(View.GONE);
    }

    private void showWatermarks() {
        removeWatermarkIcon.setVisibility(View.VISIBLE);
        removeWatermarkIconTop.setVisibility(View.VISIBLE);
        watermarkText.setVisibility(View.VISIBLE);
        watermarkTextTop.setVisibility(View.VISIBLE);
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Kiosk")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (d, w) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerNetworkCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterNetworkCallback();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!recentButton) {
            ActivityManager am =
                    (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            am.moveTaskToFront(getTaskId(), 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOfflineRestriction();
    }

    @Override
    public void onBackPressed() {
        if (backButton) super.onBackPressed();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ||
                event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return !volume;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoPlayer != null) mVideoPlayer.stopPlayback();
    }
}
