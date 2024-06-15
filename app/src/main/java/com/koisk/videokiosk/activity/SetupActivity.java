package com.koisk.videokiosk.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.koisk.videokiosk.R;
import com.koisk.videokiosk.storage.Constant;
import com.koisk.videokiosk.storage.SpDatabase;

public class SetupActivity extends AppCompatActivity {

    private RadioGroup orientationRadioGroup;
    private RadioGroup playInLoopRadioGroup;
    private RadioGroup controlRadioGroup;
    private RadioGroup statusRadioGroup;
    private RadioGroup volumeRadioGroup;
    private RadioGroup backButtonRadioGroup;
    private RadioGroup recentButtonRadioGroup;
    private SpDatabase spDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        spDatabase = new SpDatabase(this);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(view -> onBackPressed());

        orientationRadioGroup = findViewById(R.id.RadioGroup);
        playInLoopRadioGroup = findViewById(R.id.playInLoopRadioGroup);
        controlRadioGroup = findViewById(R.id.controlRadioGroup);
        statusRadioGroup = findViewById(R.id.statusRadioGroup);
        volumeRadioGroup = findViewById(R.id.volumeRadioGroup);
        backButtonRadioGroup = findViewById(R.id.backButtonRadioGroup);
        recentButtonRadioGroup = findViewById(R.id.recentRadioGroup);

        setupRadioGroupListeners();
        initializeRadioGroups();
    }

    private void initializeRadioGroups() {
        setRadioGroupValue(orientationRadioGroup, spDatabase.getBoolean(Constant.KEY_ORIENTATION));
        setRadioGroupValue(playInLoopRadioGroup, spDatabase.getBoolean(Constant.KEY_PLAY_IN_LOOP));
        setRadioGroupValue(controlRadioGroup, spDatabase.getBoolean(Constant.KEY_SHOW_VIDEO_CONTROLS));
        setRadioGroupValue(statusRadioGroup, spDatabase.getBoolean(Constant.KEY_STATUS_BAR));
        setRadioGroupValue(volumeRadioGroup, spDatabase.getBoolean(Constant.KEY_VOLUME));
        setRadioGroupValue(backButtonRadioGroup, spDatabase.getBoolean(Constant.KEY_BACK_BUTTON));
    }

    private void setRadioGroupValue(RadioGroup group, boolean isAllowed) {
        int childCount = group.getChildCount();
        for (int i = 0; i < childCount; i++) {
            RadioButton radioButton = (RadioButton) group.getChildAt(i);
            if (radioButton.getText().toString().equalsIgnoreCase(isAllowed ? "Allow" : "Restrict")) {
                radioButton.setChecked(true);
                break;
            }
        }
    }

    private void setupRadioGroupListeners() {
        orientationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            spDatabase.putBoolean(Constant.KEY_ORIENTATION, isAllow(checkedId));
        });

        playInLoopRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            spDatabase.putBoolean(Constant.KEY_PLAY_IN_LOOP, isAllow(checkedId));
        });

        controlRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            spDatabase.putBoolean(Constant.KEY_SHOW_VIDEO_CONTROLS, isAllow(checkedId));
        });

        statusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            spDatabase.putBoolean(Constant.KEY_STATUS_BAR, isAllow(checkedId));
        });

        volumeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            spDatabase.putBoolean(Constant.KEY_VOLUME, isAllow(checkedId));
        });

        backButtonRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            spDatabase.putBoolean(Constant.KEY_BACK_BUTTON, isAllow(checkedId));
        });

        recentButtonRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            spDatabase.putBoolean(Constant.KEY_RECENT_BUTTON, isAllow(checkedId));
        });
    }

    private boolean isAllow(int checkedId) {
        RadioButton radioButton = findViewById(checkedId);
        return radioButton != null && radioButton.getText().toString().equalsIgnoreCase("Allow");
    }

}