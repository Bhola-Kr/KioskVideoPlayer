package com.koisk.videokiosk.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.koisk.videokiosk.BuildConfig;
import com.koisk.videokiosk.storage.LocalData;

public class AdManager {

    private static InterstitialAd mInterstitialAd;
    private static boolean loading, showing;
    private static final String TEST_INTERSTITIAL_ID =
            "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_BANNER_ID =
            "ca-app-pub-3940256099942544/6300978111";

    private static final String RELEASE_INTERSTITIAL_ID =
            "ca-app-pub-3230017247689957/2787940083";
    private static final String RELEASE_BANNER_ID =
            "ca-app-pub-3230017247689957/9270174689";

    private static String getInterstitialAdId() {
        return BuildConfig.DEBUG
                ? TEST_INTERSTITIAL_ID
                : RELEASE_INTERSTITIAL_ID;
    }

    private static String getBannerAdId() {
        return BuildConfig.DEBUG
                ? TEST_BANNER_ID
                : RELEASE_BANNER_ID;
    }



    public static void Init(Context context) {
        if (!LocalData.isInterstitialAd()) return;
        MobileAds.initialize(context, initializationStatus -> {
            loadInterstitial(context);
        });
    }

    public static void loadInterstitial(Context context) {
        if (!LocalData.isInterstitialAd()) return;
        if (context == null)
            context = App.getContext();
        if (mInterstitialAd != null) {
            return;
        }
        if (loading) {
            return;
        }
        loading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, getInterstitialAdId(), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                loading = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
                loading = false;
            }
        });
    }

    public static void showInterstitial(Activity activity, @NonNull AdListener listener) {
        if (showing) {
            listener.onCompleted();
            return;
        }
        if (mInterstitialAd != null) {
            showNow(activity, listener);
        } else {
            listener.onCompleted();
            loadInterstitial(activity.getApplicationContext());
        }
    }

    private static void showNow(Activity activity, AdListener listener) {
        if (mInterstitialAd != null && !showing) {
            showing = true;
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    listener.onCompleted();
                    showing = false;
                    // times to load an new interstitial ad content
                    loadInterstitial(activity.getApplicationContext());
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    mInterstitialAd = null;
                    listener.onCompleted();
                    showing = false;
                    // times to load an new interstitial ad content
                    loadInterstitial(activity.getApplicationContext());
                }
            });
            //  Now show the ad
            mInterstitialAd.show(activity);
        } else {
            listener.onCompleted();
        }
    }

    public static void loadBanner(Context context, int adViewId) {
        if (LocalData.isBannerAd()) {
            AdView adView = new AdView(context);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId(getInterstitialAdId());
            AdRequest adRequest = new AdRequest.Builder().build();
            AdView mAdView = ((Activity) context).findViewById(adViewId);
            mAdView.loadAd(adRequest);
        }
    }

    public interface AdListener {
        void onCompleted();
    }

}

