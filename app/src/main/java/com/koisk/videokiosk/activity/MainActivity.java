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
import android.widget.Button;

import com.koisk.videokiosk.R;
import com.koisk.videokiosk.storage.LocalData;
import com.koisk.videokiosk.storage.StorageUtil;

public class MainActivity extends AppCompatActivity {

    private Button btPlayKiosk;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btPlayKiosk = findViewById(R.id.btPlayKiosk);
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
            StorageUtil.readFilesFromFolder(getApplicationContext());
            if (LocalData.allMediaList.isEmpty()) {
                showNoFilesFoundAlert();
            } else {
                startActivity(new Intent(getApplicationContext(), VideoActivity.class));
            }
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
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Close the dialog
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
