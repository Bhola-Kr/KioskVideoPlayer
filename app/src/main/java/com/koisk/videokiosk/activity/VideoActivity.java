package com.koisk.videokiosk.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
    private ImageView imageView;
    private VideoPlayer mVideoPlayer;
    private ImageView exitIcon, removeWatermarkIcon, removeWatermarkIconTop;
    private TextView watermarkText, watermarkTextTop;
    private SpDatabase spDatabase;
    private boolean orientation, showExitIcon, showControls, statusBar, volume, backButton, recentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        spDatabase = new SpDatabase(this);

        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        exitIcon = findViewById(R.id.exitIcon);
        exitIcon.setOnClickListener(v -> showExitConfirmationDialog());

        removeWatermarkIcon = findViewById(R.id.removeWatermarkIcon);
        removeWatermarkIconTop = findViewById(R.id.removeWatermarkIconTop);
        watermarkText = findViewById(R.id.watermarkText);
        watermarkTextTop = findViewById(R.id.watermarkTextTop);

        watermarkText.setSelected(true);
        watermarkTextTop.setSelected(true);


        removeWatermarkIcon.setOnClickListener(view -> showUpgradeDialog());
        watermarkText.setOnClickListener(view -> showUpgradeDialog());
        removeWatermarkIconTop.setOnClickListener(view -> showUpgradeDialog());
        watermarkTextTop.setOnClickListener(view -> showUpgradeDialog());

        videoSetup();
        listenPremiumAndUpdateWatermark();
    }

    private void listenPremiumAndUpdateWatermark() {

        String deviceId = RemoteConfigManager.getDeviceId(getApplicationContext());

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference(FirebaseConstants.APP_REF)
                .child(FirebaseConstants.USERS_REF)
                .child(deviceId);

        userRef.child("isPremium").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                boolean isPremium = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));

                if (isPremium) {
                    hideWatermarks();
                    LocalData.bannerAd = false;
                    LocalData.interstitialAd = false;
                    AdManager.hideBannerAd(VideoActivity.this, R.id.adView);

                } else {
                    showWatermarks();
                    LocalData.bannerAd = true;
                    LocalData.interstitialAd = true;
                    AdManager.loadBanner(VideoActivity.this, R.id.adView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    private void showWatermarks() {
        if (removeWatermarkIcon != null) removeWatermarkIcon.setVisibility(View.VISIBLE);
        if (removeWatermarkIconTop != null) removeWatermarkIconTop.setVisibility(View.VISIBLE);

        if (watermarkText != null) watermarkText.setVisibility(View.VISIBLE);
        if (watermarkTextTop != null) watermarkTextTop.setVisibility(View.VISIBLE);
    }

    private void hideWatermarks() {
        if (removeWatermarkIcon != null) removeWatermarkIcon.setVisibility(View.GONE);
        if (removeWatermarkIconTop != null) removeWatermarkIconTop.setVisibility(View.GONE);

        if (watermarkText != null) watermarkText.setVisibility(View.GONE);
        if (watermarkTextTop != null) watermarkTextTop.setVisibility(View.GONE);
    }


    private void showUpgradeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Upgrade to Premium")
                .setMessage(
                        "Upgrade to Premium to remove all watermarks and on-screen text.\n" +
                                "Enjoy an ad-free experience with no interruptions."
                )
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    startActivity(new Intent(getApplicationContext(), SubscriptionActivity.class));
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                orientation
                        ? ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        );

        if (showControls) {
            videoView.setMediaController(new MediaController(this));
        }

        if (statusBar) {
            windowSetup();
        }

        mVideoPlayer = new VideoPlayer(this, videoView, imageView);
        mVideoPlayer.videoSetup();
    }

    private void windowSetup() {
        try {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        } catch (Exception e) {
            Log.d("error: ", "" + e.getLocalizedMessage());
        }
    }

    private void showExitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit Kiosk");
        builder.setMessage("Are you sure you want to exit from the kiosk?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish(); // Finish the activity and return to the previous screen
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Dismiss the dialog
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!videoView.isPlaying()) {
            videoView.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!videoView.isPlaying()) {
            videoView.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }
    }

    @Override
    public void onBackPressed() {
        if (backButton) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean result;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                result = !volume;
                break;
            default:
                result = super.dispatchKeyEvent(event);
                break;
        }
        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoPlayer != null) {
            mVideoPlayer.stopPlayback();
        }
        if (!recentButton) {
            ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.moveTaskToFront(getTaskId(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoPlayer != null) {
            mVideoPlayer.stopPlayback();
        }
    }
}
