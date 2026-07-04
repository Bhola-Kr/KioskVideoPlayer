package com.koisk.videokiosk.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.koisk.videokiosk.R;
import com.koisk.videokiosk.ads.AdManager;
import com.koisk.videokiosk.storage.Constant;
import com.koisk.videokiosk.storage.LocalData;
import com.koisk.videokiosk.storage.SpDatabase;
import com.koisk.videokiosk.storage.StorageUtil;
import com.koisk.videokiosk.utils.RemoteConfigManager;
import com.koisk.videokiosk.utils.UserRegistrar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button btPlayKiosk;
    private ImageView ivSettings;
    private RadioGroup mediaTypeRadioGroup;
    private RadioButton rbVideo, rbImage, rbBoth;
    private EditText etTimeInSec;

    private SpDatabase spDatabase;
    private android.app.ProgressDialog progressDialog;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    private String storedMediaType = "BOTH";

    private boolean isPlayRequested = false;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spDatabase = new SpDatabase(this);

        btPlayKiosk = findViewById(R.id.btPlayKiosk);
        ivSettings = findViewById(R.id.ivSettings);
        mediaTypeRadioGroup = findViewById(R.id.mediaTypeRadioGroup);

        rbVideo = findViewById(R.id.rbVideo);
        rbImage = findViewById(R.id.rbImage);
        rbBoth = findViewById(R.id.rbBoth);

        etTimeInSec = findViewById(R.id.editTextImageShowTime);

        TextView tvFooter = findViewById(R.id.tvFooterEmail);

        tvFooter.setOnClickListener(v -> openSupportEmail());


        // ✅ Permission callback
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {

                    boolean granted = false;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        granted = result.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false)
                                && result.getOrDefault(Manifest.permission.READ_MEDIA_VIDEO, false);
                    } else {
                        granted = result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);
                    }

                    if (granted) {
                        // ✅ Only proceed if user pressed Play
                        if (isPlayRequested) {
                            startKioskFlow();
                        }
                    } else {
                        // ❌ Permission denied
                        hideProgress();
                        isPlayRequested = false;

                        if (isPermissionPermanentlyDenied()) {
                            showPermissionSettingsDialog();
                        } else {
                            Toast.makeText(this, "Permission denied. Please allow permission to continue.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        // ❌ Removed auto permission request on app start
        // requestPermissions();

        btPlayKiosk.setOnClickListener(view -> {
            isPlayRequested = true;

            if (hasStoragePermission()) {
                startKioskFlow();
            } else {
                showProgress();
                requestPermissions();
            }
        });

        ivSettings.setOnClickListener(view ->
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class))
        );

        setupImageShowTime();
        setupMediaTypeSelection();
        UserRegistrar.registerIfNeeded(this);
    }

    // ✅ Main play flow (safe + background scan)
    private void startKioskFlow() {
        try {
            showProgress();

            // Clear list
            if (LocalData.allMediaList != null) {
                LocalData.allMediaList.clear();
            }

            // Save media type
            String saveMediaType = new SpDatabase(this).getString(Constant.KEY_MEDIA_TYPE);
            LocalData.setSupportMedia(saveMediaType);

            // Image interval
            String imageDisplayTime = etTimeInSec.getText().toString();
            int imageDisplayInterval = 15;

            if (!imageDisplayTime.isEmpty()) {
                imageDisplayInterval = Integer.parseInt(imageDisplayTime);
                if (imageDisplayInterval <= 0) {
                    Toast.makeText(this, "Interval must be greater than 0", Toast.LENGTH_LONG).show();
                    hideProgress();
                    isPlayRequested = false;
                    return;
                }
            }

            LocalData.setImageDisplayInterval(imageDisplayInterval);

            // ✅ Run file scan in background thread (NO ANR)
            executorService.execute(() -> {
                try {
                    StorageUtil.readFilesFromFolder(getApplicationContext());

                    runOnUiThread(() -> {
                        hideProgress();

                        AdManager.showInterstitial(MainActivity.this, () -> {
                            if (LocalData.allMediaList == null || LocalData.allMediaList.isEmpty()) {
                                showNoFilesFoundAlert();
                            } else {
                                startActivity(new Intent(getApplicationContext(), VideoActivity.class));
                            }
                            isPlayRequested = false;
                        });
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        hideProgress();
                        isPlayRequested = false;
                        Toast.makeText(MainActivity.this, "Error reading media files", Toast.LENGTH_SHORT).show();
                    });
                    Log.d("error", e.toString());
                }
            });

        } catch (Exception e) {
            hideProgress();
            isPlayRequested = false;
            Log.d("error", e.toString());
        }
    }

    private void setupImageShowTime() {
        String savedImageShowTime = spDatabase.getString(Constant.KEY_IMAGE_SHOW_TIME);
        etTimeInSec.setText(savedImageShowTime);

        etTimeInSec.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String imageShowTime = etTimeInSec.getText().toString().trim();
                if (!TextUtils.isEmpty(imageShowTime)) {
                    try {
                        int timeValue = Integer.parseInt(imageShowTime);
                        if (timeValue > 0) {
                            spDatabase.putString(Constant.KEY_IMAGE_SHOW_TIME, imageShowTime);
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a valid time (greater than 0)", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Invalid time format", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    spDatabase.putString(Constant.KEY_IMAGE_SHOW_TIME, "");
                }
            }
        });
    }

    private void setupMediaTypeSelection() {
        storedMediaType = new SpDatabase(this).getString(Constant.KEY_MEDIA_TYPE);

        if ("VIDEO".equals(storedMediaType)) {
            rbVideo.setChecked(true);
        } else if ("IMAGE".equals(storedMediaType)) {
            rbImage.setChecked(true);
        } else {
            rbBoth.setChecked(true);
        }

        mediaTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbVideo) {
                storedMediaType = "VIDEO";
            } else if (checkedId == R.id.rbImage) {
                storedMediaType = "IMAGE";
            } else {
                storedMediaType = "BOTH";
            }
            new SpDatabase(this).putString(Constant.KEY_MEDIA_TYPE, storedMediaType);
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            });
        } else {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            });
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean isPermissionPermanentlyDenied() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean showRationaleImages = shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES);
            boolean showRationaleVideos = shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VIDEO);

            // if false -> blocked permanently
            return !showRationaleImages || !showRationaleVideos;
        } else {
            boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
            return !showRationale;
        }
    }

    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Storage Permission Required")
                .setMessage(
                        "To play your videos/images, this app needs storage permission.\n\n" +
                                "How to enable:\n" +
                                "1) Tap Open Settings\n" +
                                "2) Go to Permissions\n" +
                                "3) Allow Photos & Videos (or Storage)\n\n" +
                                "Then come back and start again."
                )
                .setCancelable(false)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void showNoFilesFoundAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Media Files Found");
        builder.setMessage("No video or image files were found. Please make sure storage permissions are enabled and your device contains media files.");
        builder.setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        RemoteConfigManager.startListening(this, showAds -> {
            AdManager.loadBanner(MainActivity.this, R.id.adView);
            AdManager.loadInterstitial(MainActivity.this);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        RemoteConfigManager.stopListening(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void openSupportEmail() {

        String supportEmail = "recentchathelp@gmail.com";

        // Your existing device ID logic
        String deviceId = RemoteConfigManager.getDeviceId(getApplicationContext());

        String subject = "Dev Support";
        String body =
                "Hello Support Team,\n\n" +
                        "I need help regarding Premium / App usage.\n\n" +
                        "Device ID:\n" + deviceId + "\n\n" +
                        "Issue Description:\n" +
                        "[Please describe your issue here]\n\n" +
                        "Thank you.";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(android.net.Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{supportEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(intent, "Contact Support"));
        } catch (Exception e) {
            // Optional fallback
        }
    }
}
