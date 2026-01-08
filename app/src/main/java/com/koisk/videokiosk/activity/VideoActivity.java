package com.koisk.videokiosk.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.koisk.videokiosk.R;
import com.koisk.videokiosk.storage.Constant;
import com.koisk.videokiosk.storage.SpDatabase;
import com.koisk.videokiosk.utils.VideoPlayer;

public class VideoActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageView imageView;
    private VideoPlayer mVideoPlayer;
    private ImageView exitIcon;
    private SpDatabase spDatabase;
    private boolean orientation, showControls, statusBar, volume, backButton, recentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        spDatabase = new SpDatabase(this);

        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        exitIcon = findViewById(R.id.exitIcon);
        exitIcon.setOnClickListener(v -> showExitConfirmationDialog());

        videoSetup();
    }

    private void videoSetup() {
        try {
            orientation = spDatabase.getBoolean(Constant.KEY_ORIENTATION);
//            playInLoop = spDatabase.getBoolean(Constant.KEY_PLAY_IN_LOOP);
            showControls = spDatabase.getBoolean(Constant.KEY_SHOW_VIDEO_CONTROLS);
            statusBar = spDatabase.getBoolean(Constant.KEY_STATUS_BAR);
            volume = spDatabase.getBoolean(Constant.KEY_VOLUME);
            backButton = spDatabase.getBoolean(Constant.KEY_BACK_BUTTON);
            recentButton = spDatabase.getBoolean(Constant.KEY_RECENT_BUTTON);

            if (orientation) {
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            } else {
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
            if (showControls) {
                videoView.setMediaController(new android.widget.MediaController(this));
            }
            if (statusBar) {
                windowSetup();
            }
            mVideoPlayer = new VideoPlayer(this, videoView, imageView);
            mVideoPlayer.videoSetup();
        } catch (Exception e) {
            Log.d("error", e.getLocalizedMessage());
        }
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
