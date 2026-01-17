package com.koisk.videokiosk.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.koisk.videokiosk.BuildConfig;
import com.koisk.videokiosk.firebase.FirebaseConstants;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class UserRegistrar {

    private UserRegistrar() {}

    public static void registerIfNeeded(Context context) {
        String deviceId = getDeviceId(context);

        DatabaseReference userRef = FirebaseDatabase
                .getInstance()
                .getReference(FirebaseConstants.APP_REF).child(FirebaseConstants.USERS_REF)
                .child(deviceId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    updateLastSeen(userRef);
                    return;
                }

                Map<String, Object> defaultUser = new HashMap<>();

                // Business flags
                defaultUser.put("isPremium", false);
                defaultUser.put("adsEnabled", true);

                // App info
                defaultUser.put("appVersionName", BuildConfig.VERSION_NAME);
                defaultUser.put("appVersionCode", BuildConfig.VERSION_CODE);

                // Device info
                defaultUser.put("androidVersion", Build.VERSION.RELEASE);
                defaultUser.put("sdkInt", Build.VERSION.SDK_INT);
                defaultUser.put("deviceBrand", Build.BRAND);
                defaultUser.put("deviceModel", Build.MODEL);

                // Geo (approx, non-PII)
                defaultUser.put("country", Locale.getDefault().getCountry());

                // Timestamps
                long now = System.currentTimeMillis();
                defaultUser.put("createdAt", now);
                defaultUser.put("lastSeenAt", now);

                userRef.setValue(defaultUser);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private static void updateLastSeen(DatabaseReference userRef) {
        userRef.child("lastSeenAt").setValue(System.currentTimeMillis());
    }

    private static String getDeviceId(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }
}
