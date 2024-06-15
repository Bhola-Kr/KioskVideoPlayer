package com.koisk.videokiosk.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.koisk.videokiosk.R;
import com.koisk.videokiosk.ads.AdManager;
import com.koisk.videokiosk.storage.Constant;
import com.koisk.videokiosk.storage.SpDatabase;
import com.koisk.videokiosk.storage.StorageUtil;

public class DirectoryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_FOLDER = 123;
    private static final int PERMISSION_REQUEST_CODE = 122;
    private Button btDir, btPlayKiosk;
    private ImageView ivSettings;
    private TextView tvPath;

    private SpDatabase spDatabase;
    private final String readImagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            Manifest.permission.READ_MEDIA_VIDEO :
            Manifest.permission.READ_EXTERNAL_STORAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spDatabase = new SpDatabase(getApplicationContext());
        btDir = findViewById(R.id.btnDir);
        tvPath = findViewById(R.id.tvPath);
        btPlayKiosk = findViewById(R.id.btPlayKiosk);
        ivSettings = findViewById(R.id.ivSettings);
        ivSettings.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SettingsActivity.class)));

        AdManager.loadInterstitial(getApplicationContext());
        AdManager.loadBanner(DirectoryActivity.this, R.id.adView);

        btDir.setOnClickListener(view -> checkPermissions());
        btPlayKiosk.setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            StorageUtil.readFilesFromFolder(getApplicationContext());
            new Handler().postDelayed(() -> startActivity(new Intent(DirectoryActivity.this, VideoActivity.class)), 1000);
        }));

        setPath();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, readImagePermission) == PackageManager.PERMISSION_GRANTED) {
            selectDirectory();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{readImagePermission}, PERMISSION_REQUEST_CODE);
        }
    }


    // Handle the result of the folder picker intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri treeUri = data.getData();
                if (treeUri != null) {
                    spDatabase.putString(Constant.KEY_DIRECTORY_PATH, treeUri.toString());
                    StorageUtil.readFilesFromFolder(getApplicationContext());
                    setPath();
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectDirectory();
            } else {
                // Permission denied, handle accordingly (inform the user or take alternative actions)
            }
        }
    }

    private void selectDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    private void setPath() {
        String path = spDatabase.getString(Constant.KEY_DIRECTORY_PATH);
        if (path.isEmpty()) {
            tvPath.setText("Current Path: " + "Internal Movies Folder");
        } else {
            tvPath.setText("Current Path: " + path);
        }
    }
}
