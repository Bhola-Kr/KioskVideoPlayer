package com.koisk.videokiosk.payment;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.*;

import java.util.ArrayList;
import java.util.List;

public class BillingManager {

    private static final String TAG = "BillingManager";

    private final Context context;
    private BillingClient billingClient;

    public interface BillingConnectionListener {
        void onConnected();
        void onDisconnected();
        void onError(@NonNull String error);
    }

    public interface ProductDetailsListener {
        void onProductDetailsLoaded(@NonNull List<ProductDetails> productDetailsList);
        void onError(@NonNull String error);
    }

    public interface PurchaseListener {
        void onPurchaseSuccess(@NonNull Purchase purchase);
        void onPurchaseFailed(@NonNull String message);
        void onUserCancelled();
        void onRestoreEmpty();
    }

    // ✅ Keep both listeners alive (purchase + restore)
    private PurchaseListener newPurchaseListener;
    private PurchaseListener restorePurchaseListener;

    public BillingManager(Context context) {
        this.context = context;

        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .enablePrepaidPlans()
                                .build()
                )
                .build();
    }

    // ============================================================
    // 🔥 Purchase callback
    // ============================================================
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {

        int responseCode = billingResult.getResponseCode();

        Log.d(TAG, "PurchasesUpdatedListener -> code=" + responseCode
                + ", msg=" + billingResult.getDebugMessage());

        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {

            Log.d(TAG, "Purchases size=" + purchases.size());

            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }

        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {

            Log.d(TAG, "User cancelled purchase");
            if (newPurchaseListener != null) newPurchaseListener.onUserCancelled();

        } else {

            Log.e(TAG, "Purchase error: " + billingResult.getDebugMessage());
            if (newPurchaseListener != null)
                newPurchaseListener.onPurchaseFailed(billingResult.getDebugMessage());
        }
    };

    // ============================================================
    // ✅ Connect
    // ============================================================
    public void startConnection(@NonNull BillingConnectionListener listener) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected successfully");
                    listener.onConnected();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                    listener.onError(billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e(TAG, "Billing service disconnected");
                listener.onDisconnected();
            }
        });
    }

    public boolean isReady() {
        return billingClient != null && billingClient.isReady();
    }

    // ============================================================
    // ✅ INAPP (One-time products)
    // ============================================================
    public void queryInAppProductDetails(@NonNull String productId,
                                         @NonNull ProductDetailsListener listener) {

        if (!isReady()) {
            listener.onError("BillingClient is not ready");
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, queryProductDetailsResult) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                List<ProductDetails> productDetailsList = queryProductDetailsResult.getProductDetailsList();

                if (productDetailsList != null && !productDetailsList.isEmpty()) {
                    listener.onProductDetailsLoaded(productDetailsList);
                } else {
                    listener.onError("INAPP Product not found. Check productId in Play Console.");
                }

            } else {
                listener.onError("Error: " + billingResult.getDebugMessage());
            }
        });
    }

    public void launchInAppPurchase(@NonNull Activity activity,
                                    @NonNull ProductDetails productDetails,
                                    @NonNull PurchaseListener listener) {

        if (!isReady()) {
            listener.onPurchaseFailed("BillingClient is not ready");
            return;
        }

        // ✅ Save purchase listener
        this.newPurchaseListener = listener;

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
        );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        Log.d(TAG, "Launch INAPP Flow Result: " + result.getResponseCode());
    }

    // ============================================================
    // ✅ SUBS (Monthly subscription)
    // ============================================================
    public void querySubscriptionDetails(@NonNull String subscriptionId,
                                         @NonNull ProductDetailsListener listener) {

        if (!isReady()) {
            listener.onError("BillingClient is not ready");
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(subscriptionId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, queryProductDetailsResult) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                List<ProductDetails> productDetailsList = queryProductDetailsResult.getProductDetailsList();

                if (productDetailsList != null && !productDetailsList.isEmpty()) {
                    listener.onProductDetailsLoaded(productDetailsList);
                } else {
                    listener.onError("Subscription not found. Check subscriptionId/base plan active in Play Console.");
                }

            } else {
                listener.onError("Error: " + billingResult.getDebugMessage());
            }
        });
    }

    public void launchSubscriptionPurchase(@NonNull Activity activity,
                                           @NonNull ProductDetails productDetails,
                                           @NonNull PurchaseListener listener) {

        if (!isReady()) {
            listener.onPurchaseFailed("BillingClient is not ready");
            return;
        }

        // ✅ Save purchase listener
        this.newPurchaseListener = listener;

        String offerToken = getOfferToken(productDetails);

        if (offerToken == null) {
            listener.onPurchaseFailed("OfferToken not found. Check base plan/offer in Play Console.");
            return;
        }

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
        );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        Log.d(TAG, "Launch SUBS Flow Result: " + result.getResponseCode());
    }

    private String getOfferToken(@NonNull ProductDetails productDetails) {
        try {
            List<ProductDetails.SubscriptionOfferDetails> offerDetailsList =
                    productDetails.getSubscriptionOfferDetails();

            if (offerDetailsList != null && !offerDetailsList.isEmpty()) {
                return offerDetailsList.get(0).getOfferToken();
            }
        } catch (Exception e) {
            Log.e(TAG, "OfferToken error: " + e.getMessage());
        }
        return null;
    }

    // ============================================================
    // ✅ Restore Purchases (INAPP / SUBS)
    // ============================================================
    public void restoreInAppPurchases(@NonNull PurchaseListener listener) {
        this.restorePurchaseListener = listener;
        restorePurchasesByType(BillingClient.ProductType.INAPP);
    }

    public void restoreSubscriptions(@NonNull PurchaseListener listener) {
        this.restorePurchaseListener = listener;
        restorePurchasesByType(BillingClient.ProductType.SUBS);
    }

    private void restorePurchasesByType(@NonNull String productType) {

        if (!isReady()) {
            if (restorePurchaseListener != null)
                restorePurchaseListener.onPurchaseFailed("BillingClient is not ready");
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchasesList) -> {

            Log.d(TAG, "Restore query -> type=" + productType
                    + ", code=" + billingResult.getResponseCode()
                    + ", msg=" + billingResult.getDebugMessage());

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                if (purchasesList != null && !purchasesList.isEmpty()) {

                    for (Purchase purchase : purchasesList) {
                        handlePurchase(purchase);
                    }

                } else {
                    if (restorePurchaseListener != null) restorePurchaseListener.onRestoreEmpty();
                }

            } else {
                if (restorePurchaseListener != null)
                    restorePurchaseListener.onPurchaseFailed(billingResult.getDebugMessage());
            }
        });
    }

    // ============================================================
    // ✅ Handle Purchase (ACK required)
    // ============================================================
    private void handlePurchase(@NonNull Purchase purchase) {

        Log.d(TAG, "handlePurchase -> state=" + purchase.getPurchaseState()
                + ", acknowledged=" + purchase.isAcknowledged());

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

            if (!purchase.isAcknowledged()) {

                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {

                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully");
                        notifySuccessToAll(purchase);
                    } else {
                        Log.e(TAG, "Acknowledge failed: " + billingResult.getDebugMessage());
                        notifyFailedToAll(billingResult.getDebugMessage());
                    }
                });

            } else {
                Log.d(TAG, "Purchase already acknowledged");
                notifySuccessToAll(purchase);
            }

        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            notifyFailedToAll("Purchase pending. Please complete payment.");
        } else {
            notifyFailedToAll("Purchase not completed. State: " + purchase.getPurchaseState());
        }
    }

    // ============================================================
    // ✅ Notify BOTH listeners always (fix consumer callback issue)
    // ============================================================
    private void notifySuccessToAll(@NonNull Purchase purchase) {
        if (newPurchaseListener != null) {
            newPurchaseListener.onPurchaseSuccess(purchase);
        }
        if (restorePurchaseListener != null) {
            restorePurchaseListener.onPurchaseSuccess(purchase);
        }
    }

    private void notifyFailedToAll(@NonNull String message) {
        if (newPurchaseListener != null) {
            newPurchaseListener.onPurchaseFailed(message);
        }
        if (restorePurchaseListener != null) {
            restorePurchaseListener.onPurchaseFailed(message);
        }
    }

    public void endConnection() {
        if (billingClient != null) billingClient.endConnection();
    }
}
