package com.koisk.videokiosk.payment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.*;
import com.koisk.videokiosk.R;
import com.koisk.videokiosk.storage.LocalData;
import com.koisk.videokiosk.utils.RemoteConfigManager;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionActivity extends AppCompatActivity {

    private static final String TAG = "SubscriptionActivity";

    private static final String SUBSCRIPTION_ID = "premium_monthly";

    private BillingClient billingClient;
    private ProductDetails subscriptionDetails;

    private Button btnUpgrade;
    private TextView tvPlanPrice;
    private ImageView ivBack;

    // ============================================================
    // ✅ Purchase callback (runs sometimes on background thread)
    // ============================================================
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {

        Log.d(TAG, "PurchasesUpdated -> code=" + billingResult.getResponseCode()
                + ", msg=" + billingResult.getDebugMessage());

        runOnUiThread(() -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {

                for (Purchase purchase : purchases) {
                    Log.d(TAG, "Purchase received -> " + purchase.getProducts());
                    handlePurchase(purchase);
                }

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {

                showToast("Purchase Cancelled");

            } else {

                showToast("Error: " + billingResult.getDebugMessage());
            }
        });
    };

    // ============================================================
    // ✅ onCreate
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        btnUpgrade = findViewById(R.id.btnUpgrade);
        tvPlanPrice = findViewById(R.id.tvPlanPrice);
        ivBack = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> finish());

        btnUpgrade.setEnabled(false);
        btnUpgrade.setText("Loading...");

        setupBillingClient();
        connectBilling();

        btnUpgrade.setOnClickListener(v -> {
            if (subscriptionDetails == null) {
                showToast("Subscription not loaded yet");
                return;
            }
            launchSubscriptionPurchase();
        });
    }

    // ============================================================
    // ✅ Setup BillingClient
    // ============================================================
    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enablePrepaidPlans()
                                .enableOneTimeProducts()
                                .build()
                )
                .build();
    }

    // ============================================================
    // ✅ Connect Billing
    // ============================================================
    private void connectBilling() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                Log.d(TAG, "BillingSetupFinished -> code=" + billingResult.getResponseCode()
                        + ", msg=" + billingResult.getDebugMessage());

                runOnUiThread(() -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        loadSubscriptionDetails();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            statusListener();
                        }, 1000); // 1 second
                    } else {
                        showToast("Billing error: " + billingResult.getDebugMessage());
                    }
                });
            }

            @Override
            public void onBillingServiceDisconnected() {
                runOnUiThread(() -> showToast("Billing disconnected"));
            }
        });
    }

    // ============================================================
    // ✅ Load Subscription Details
    // ============================================================
    private void loadSubscriptionDetails() {

        if (billingClient == null || !billingClient.isReady()) {
            Log.e(TAG, "BillingClient not ready while loading details");
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, queryProductDetailsResult) -> {
            runOnUiThread(() -> {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    List<ProductDetails> list = queryProductDetailsResult.getProductDetailsList();

                    if (list != null && !list.isEmpty()) {
                        subscriptionDetails = list.get(0);
                        tvPlanPrice.setText(getPrice(subscriptionDetails));
                        btnUpgrade.setEnabled(true);
                        btnUpgrade.setText("Upgrade to Premium");

                    } else {
                        showToast("Subscription not found in Play Console");
                        btnUpgrade.setEnabled(false);
                        btnUpgrade.setText("Unavailable");
                    }

                } else {
                    showToast("Details error: " + billingResult.getDebugMessage());
                }
            });
        });
    }

    // ============================================================
    // ✅ Launch Purchase Flow
    // ============================================================
    private void launchSubscriptionPurchase() {

        if (billingClient == null || !billingClient.isReady()) {
            showToast("Billing not ready");
            return;
        }

        String offerToken = getOfferToken(subscriptionDetails);

        if (offerToken == null) {
            showToast("Offer token not found (check base plan)");
            return;
        }

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(subscriptionDetails)
                        .setOfferToken(offerToken)
                        .build()
        );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult result = billingClient.launchBillingFlow(this, billingFlowParams);

        Log.d(TAG, "launchBillingFlow -> code=" + result.getResponseCode()
                + ", msg=" + result.getDebugMessage());
    }

    private String getOfferToken(@NonNull ProductDetails productDetails) {
        try {
            List<ProductDetails.SubscriptionOfferDetails> offerDetails =
                    productDetails.getSubscriptionOfferDetails();

            if (offerDetails != null && !offerDetails.isEmpty()) {
                return offerDetails.get(0).getOfferToken();
            }
        } catch (Exception e) {
            Log.e(TAG, "OfferToken error: " + e.getMessage());
        }
        return null;
    }

    // ============================================================
    // ✅ Handle Purchase (ACK)
    // ============================================================
    private void handlePurchase(@NonNull Purchase purchase) {

        Log.d(TAG, "handlePurchase -> state=" + purchase.getPurchaseState()
                + ", acknowledged=" + purchase.isAcknowledged());

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

            if (!purchase.isAcknowledged()) {

                AcknowledgePurchaseParams params =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(params, billingResult -> {

                    Log.d(TAG, "acknowledgePurchase -> code=" + billingResult.getResponseCode()
                            + ", msg=" + billingResult.getDebugMessage());

                    runOnUiThread(() -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            // Your premium activation logic
                            RemoteConfigManager.activatePremium(getApplicationContext());
                            btnUpgrade.setText("Premium Active ✅");
                            btnUpgrade.setEnabled(false);
                            showToast("Premium Active ✅");
                        } else {
                            showToast("Acknowledge failed: " + billingResult.getDebugMessage());
                        }
                    });
                });

            } else {
                runOnUiThread(() -> {
                    RemoteConfigManager.activatePremium(getApplicationContext());
                    btnUpgrade.setText("Premium Active ✅");
                    btnUpgrade.setEnabled(false);
                    showToast("Premium Active ✅");
                });
            }

        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            showToast("Payment Pending...");
        } else {
            showToast("Purchase not completed");
        }
    }

    // ============================================================
    // ✅ Premium UI + Your Premium Activation Logic
    // ============================================================
    private void onPremiumActivated() {

        LocalData.interstitialAd = false;
        LocalData.bannerAd = false;

        btnUpgrade.setText("Premium Active ✅");
        btnUpgrade.setEnabled(false);

        showToast("Premium Activated ✅");
    }

    private String getPrice(ProductDetails details) {
        try {
            if (details.getSubscriptionOfferDetails() != null
                    && !details.getSubscriptionOfferDetails().isEmpty()
                    && details.getSubscriptionOfferDetails().get(0).getPricingPhases() != null
                    && details.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList() != null
                    && !details.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().isEmpty()) {

                return details.getSubscriptionOfferDetails()
                        .get(0)
                        .getPricingPhases()
                        .getPricingPhaseList()
                        .get(0)
                        .getFormattedPrice() + " / month";
            }
        } catch (Exception e) {
            Log.e(TAG, "Price error: " + e.getMessage());
        }
        return "-- / month";
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    public interface SubscriptionCheckListener {
        void onResult(boolean isSubscribed);
        void onError(String error);
    }

    private void statusListener(){
        checkIfUserSubscribed(new SubscriptionCheckListener() {
            @Override
            public void onResult(boolean isSubscribed) {
                if (isSubscribed) {
                    btnUpgrade.setText("Premium Active ✅");
                    btnUpgrade.setEnabled(false);
                } else {
                    btnUpgrade.setText("Upgrade to Premium");
                    btnUpgrade.setEnabled(true);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SubscriptionActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkIfUserSubscribed(@NonNull SubscriptionCheckListener listener) {

        if (billingClient == null || !billingClient.isReady()) {
            listener.onError("BillingClient not ready");
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchasesList) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                boolean isSubscribed = false;

                if (purchasesList != null && !purchasesList.isEmpty()) {
                    for (Purchase purchase : purchasesList) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            isSubscribed = true;
                            break;
                        }
                    }
                }

                boolean finalIsSubscribed = isSubscribed;
                runOnUiThread(() -> listener.onResult(finalIsSubscribed));

            } else {
                runOnUiThread(() -> listener.onError(billingResult.getDebugMessage()));
            }
        });
    }


    // ============================================================
    // ✅ Clean up
    // ============================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }


}
