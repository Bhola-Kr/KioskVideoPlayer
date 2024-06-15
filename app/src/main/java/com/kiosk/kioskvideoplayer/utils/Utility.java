package com.kiosk.kioskvideoplayer.utils;


import static com.kiosk.kioskvideoplayer.storage.Constant.videoExtensions;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.kiosk.kioskvideoplayer.storage.Constant;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Utility {

    private static final String BASE_URL = "https://apantalla.cortier.net/screen1/";

    public static void removeOldFiles(File directory, long cutoffDate) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() < cutoffDate) {
                    file.delete();
                }
            }
        }
    }

    public static boolean isVideoFormat(String hrefValue) {
        for (String format : Constant.videoExtensions) {
            String formatExtension = format.substring(format.lastIndexOf(" ") + 1); // Extract the file extension
            if (hrefValue.endsWith(formatExtension)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVideoFile(File file) {
        String fileName = file.getName().toLowerCase();
        List<String> videoExtensionsList = Arrays.asList(videoExtensions);

        for (String extension : videoExtensionsList) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static void windowSetup(Activity activity) {
        if (activity != null && activity.getWindow() != null) {
            Window window = activity.getWindow();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            window.getDecorView().setSystemUiVisibility(uiOptions);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
            }
        }
    }

    public static boolean isNetworkAvailable(Activity activity) {
        if (activity != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }

    public static void shareApp(Context context) {
        try {
            final String appPackageName = context.getPackageName();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out the App at: https://play.google.com/store/apps/details?id=" + appPackageName);
            sendIntent.setType("text/plain");
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(sendIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void rateApp(Context context) {
        Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            myAppLinkToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Log.d("Error", "" + e.getMessage());
        }
    }

    public static void sendEmail(Context context) {

        String[] TO = {"recentchat@gmail.com"};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Recent chat email support");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Email message goes here");
        try {
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Log.d("Error", "" + ex.getMessage());
        }
    }


    public static String getAppVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

}
