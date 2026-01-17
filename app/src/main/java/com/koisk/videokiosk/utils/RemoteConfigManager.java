package com.koisk.videokiosk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.koisk.videokiosk.BuildConfig;
import com.koisk.videokiosk.firebase.FirebaseConstants;
import com.koisk.videokiosk.storage.LocalData;

import java.util.HashMap;
import java.util.Map;

public final class RemoteConfigManager {

    private static ValueEventListener appConfigListener;
    private static ValueEventListener userConfigListener;

    private RemoteConfigManager() {}

    public static void startListening(Activity activity, ConfigCallback callback) {

        DatabaseReference rootRef =
                FirebaseDatabase.getInstance().getReference(FirebaseConstants.APP_REF);

        appConfigListener = rootRef.child(FirebaseConstants.APP_CONFIG_REF)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot appSnapshot) {

                        boolean forceUpdate =
                                Boolean.TRUE.equals(
                                        appSnapshot.child("forceUpdate").getValue(Boolean.class)
                                );

                        Long latestVersion =
                                appSnapshot.child("latestVersionCode").getValue(Long.class);

                        boolean adsDefault =
                                Boolean.TRUE.equals(
                                        appSnapshot.child("adsEnabledByDefault").getValue(Boolean.class)
                                );

                        if (forceUpdate
                                && latestVersion != null
                                && BuildConfig.VERSION_CODE < latestVersion) {

                            showForceUpdateDialog(
                                    activity,
                                    appSnapshot.child("updateUrl").getValue(String.class)
                            );
                            return;
                        }

                        listenUserConfig(activity, adsDefault, callback);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private static void listenUserConfig(
            Activity activity,
            boolean adsDefault,
            ConfigCallback callback
    ) {
        String deviceId = getDeviceId(activity);

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference(FirebaseConstants.APP_REF)
                .child(FirebaseConstants.USERS_REF)
                .child(deviceId);

        if (userConfigListener != null) {
            userRef.removeEventListener(userConfigListener);
        }

        userConfigListener = userRef.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        boolean isPremium =
                                Boolean.TRUE.equals(
                                        snapshot.child("isPremium").getValue(Boolean.class)
                                );

                        boolean adsEnabled =
                                Boolean.TRUE.equals(
                                        snapshot.child("adsEnabled").getValue(Boolean.class)
                                );

                        Long premiumExpiryAt =
                                snapshot.child("premiumExpiryAt").getValue(Long.class);

                        long now = System.currentTimeMillis();

                        // ✅ If premium expired → turn OFF premium automatically
                        if (isPremium && premiumExpiryAt != null && now > premiumExpiryAt) {

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("isPremium", false);
                            updates.put("adsEnabled", true);
                            updates.put("premiumExpiredAt", now);

                            userRef.updateChildren(updates);

                            // Update local values after expiry
                            isPremium = false;
                            adsEnabled = true;
                        }

                        // Final Ads State
                        boolean finalAdsState = !isPremium && adsEnabled && adsDefault;

                        callback.onConfigReady(finalAdsState);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    public static void stopListening(Context context) {
        DatabaseReference rootRef =
                FirebaseDatabase.getInstance().getReference(FirebaseConstants.APP_REF);

        if (appConfigListener != null) {
            rootRef.child(FirebaseConstants.APP_CONFIG_REF).removeEventListener(appConfigListener);
        }

        if (userConfigListener != null) {
            rootRef.child(FirebaseConstants.USERS_REF)
                    .child(getDeviceId(context))
                    .removeEventListener(userConfigListener);
        }
    }

    private static void showForceUpdateDialog(Activity activity, String url) {
        new AlertDialog.Builder(activity)
                .setTitle("Update Required!!")
                .setMessage("Please update the app to continue using it.")
                .setCancelable(false)
                .setPositiveButton("Update", (d, w) -> {
                    activity.startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    );
                    activity.finish();
                })
                .show();
    }

    public interface ConfigCallback {
        void onConfigReady(boolean showAds);
    }

    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    // ============================================================
    // ✅ Activate Premium (1 month expiry)
    // ============================================================
    public static void activatePremium(Context context) {

        String deviceId = getDeviceId(context);

        DatabaseReference userRef = FirebaseDatabase
                .getInstance()
                .getReference(FirebaseConstants.APP_REF)
                .child(FirebaseConstants.USERS_REF)
                .child(deviceId);

        long now = System.currentTimeMillis();

        // ✅ Expiry after 30 days (1 month approx)
        long expiry = now + (30L * 24 * 60 * 60 * 1000);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isPremium", true);
        updates.put("adsEnabled", false);
        updates.put("premiumActivatedAt", now);
        updates.put("premiumExpiryAt", expiry);

        LocalData.interstitialAd = false;
        LocalData.bannerAd = false;

        userRef.updateChildren(updates);
    }
}
