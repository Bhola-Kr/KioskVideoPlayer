package com.kiosk.kioskvideoplayer.storage;

import java.io.File;
import java.util.ArrayList;

public class LocalData {

    public static boolean isLocked = false;
    public static boolean supportVideo = true;
    public static boolean supportImage = true;
    public static int imageDisplayInterval = 30; // In second
    public static boolean interstitialAd = true;
    public static boolean bannerAd = true;

    public static String directoryPath = "";
    public static ArrayList<File> allMediaList = new ArrayList<>();

    public static void setAllMediaList(ArrayList<File> allMediaList) {
        LocalData.allMediaList = allMediaList;
    }

    public static void setIsLocked(boolean isLocked) {
        LocalData.isLocked = isLocked;
    }

    public static void setSupportVideo(boolean supportVideo) {
        LocalData.supportVideo = supportVideo;
    }

    public static void setSupportImage(boolean supportImage) {
        LocalData.supportImage = supportImage;
    }

    public static void setImageDisplayInterval(int imageDisplayInterval) {
        LocalData.imageDisplayInterval = imageDisplayInterval;
    }

    public static void setDirectoryPath(String directoryPath) {
        LocalData.directoryPath = directoryPath;
    }

    public static boolean isInterstitialAd() {
        return interstitialAd;
    }

    public static void setInterstitialAd(boolean interstitialAd) {
        LocalData.interstitialAd = interstitialAd;
    }

    public static boolean isBannerAd() {
        return bannerAd;
    }

    public static void setBannerAd(boolean bannerAd) {
        LocalData.bannerAd = bannerAd;
    }
}
