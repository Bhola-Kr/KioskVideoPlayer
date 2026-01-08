package com.koisk.videokiosk.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.koisk.videokiosk.R;
import com.koisk.videokiosk.ads.AdManager;
import com.koisk.videokiosk.storage.Constant;
import com.koisk.videokiosk.storage.LocalData;
import com.koisk.videokiosk.storage.SpDatabase;
import com.koisk.videokiosk.storage.StorageUtil;

public class MainActivity extends AppCompatActivity {

    private Button btPlayKiosk;
    private ImageView ivSettings;
    private RadioGroup mediaTypeRadioGroup;
    private RadioButton rbVideo, rbImage, rbBoth;
    private EditText etTimeInSec;
    private SpDatabase spDatabase;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    private String storedMediaType = "BOTH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spDatabase = new SpDatabase(this);
        btPlayKiosk = findViewById(R.id.btPlayKiosk);
        ivSettings = findViewById(R.id.ivSettings);
        mediaTypeRadioGroup = findViewById(R.id.mediaTypeRadioGroup);
        mediaTypeRadioGroup = findViewById(R.id.mediaTypeRadioGroup);
        rbVideo = findViewById(R.id.rbVideo);
        rbImage = findViewById(R.id.rbImage);
        rbBoth = findViewById(R.id.rbBoth);
        etTimeInSec = findViewById(R.id.editTextImageShowTime);

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false) &&
                            result.getOrDefault(Manifest.permission.READ_MEDIA_VIDEO, false) &&
                            result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)) {
                        // All permissions granted
                        StorageUtil.readFilesFromFolder(getApplicationContext());
                    }
                });

        requestPermissions();
        btPlayKiosk.setOnClickListener(view -> {
            try {
                AdManager.showInterstitial(this, () -> {
                    LocalData.setSupportMedia(storedMediaType);
                    String imageDisplayTime = etTimeInSec.getText().toString();
                    int imageDisplayInterval = 15;
                    if (!imageDisplayTime.isEmpty()) {
                        imageDisplayInterval = Integer.parseInt(imageDisplayTime);
                    }
                    LocalData.setImageDisplayInterval(imageDisplayInterval);

                    StorageUtil.readFilesFromFolder(getApplicationContext());
                    if (LocalData.allMediaList.isEmpty()) {
                        showNoFilesFoundAlert();
                    } else {
                        startActivity(new Intent(getApplicationContext(), VideoActivity.class));
                    }
                });

            } catch (Exception e) {
                Log.d("error", e.toString());
            }

        });

        ivSettings.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SettingsActivity.class)));

        AdManager.loadBanner(MainActivity.this, R.id.adView);
        AdManager.loadInterstitial(MainActivity.this);

        setupImageShowTime();
        setupMediaTypeSelection();
    }

    private void setupImageShowTime() {
        String savedImageShowTime = spDatabase.getString(Constant.KEY_IMAGE_SHOW_TIME);
        etTimeInSec.setText(savedImageShowTime);

        etTimeInSec.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // No implementation needed
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // No implementation needed
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

    private void showNoFilesFoundAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Media Files Found");
        builder.setMessage("No video or image files were found in the storage directory.");
        builder.setPositiveButton("OK", (dialog, id) -> {
            // Close the dialog
            dialog.dismiss();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
