package com.koisk.videokiosk.payment;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.koisk.videokiosk.R;
import com.koisk.videokiosk.utils.RemoteConfigManager;

import java.util.List;

public class SubscriptionActivity extends AppCompatActivity {

    private BillingManager billingManager;
    private ProductDetails subscriptionDetails;

    private Button btnUpgrade;
    private TextView tvRestore;
    private TextView tvPlanPrice;
    private ImageView ivBack;

    private static final String SUBSCRIPTION_ID = "premium_monthly";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        btnUpgrade = findViewById(R.id.btnUpgrade);
        tvRestore = findViewById(R.id.tvRestore);
        tvPlanPrice = findViewById(R.id.tvPlanPrice);
        ivBack = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> finish());

        billingManager = new BillingManager(this);

        billingManager.startConnection(new BillingManager.BillingConnectionListener() {
            @Override
            public void onConnected() {

                // Load subscription details
                billingManager.querySubscriptionDetails(SUBSCRIPTION_ID, new BillingManager.ProductDetailsListener() {
                    @Override
                    public void onProductDetailsLoaded(@NonNull List<ProductDetails> productDetailsList) {
                        subscriptionDetails = productDetailsList.get(0);

                        // If you want to show real price from Play Console
                        try {
                            if (subscriptionDetails.getSubscriptionOfferDetails() != null
                                    && !subscriptionDetails.getSubscriptionOfferDetails().isEmpty()
                                    && subscriptionDetails.getSubscriptionOfferDetails().get(0).getPricingPhases() != null
                                    && subscriptionDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList() != null
                                    && !subscriptionDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().isEmpty()) {

                                String price = subscriptionDetails.getSubscriptionOfferDetails()
                                        .get(0)
                                        .getPricingPhases()
                                        .getPricingPhaseList()
                                        .get(0)
                                        .getFormattedPrice();

                                tvPlanPrice.setText(price + " / month");
                            }
                        } catch (Exception ignored) {}
                    }

                    @Override
                    public void onError(@NonNull String error) {
                        Toast.makeText(SubscriptionActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });

                // Restore subscription
                billingManager.restoreSubscriptions(new BillingManager.PurchaseListener() {
                    @Override
                    public void onPurchaseSuccess(@NonNull Purchase purchase) {
                        Toast.makeText(SubscriptionActivity.this, "Already Subscribed ✅", Toast.LENGTH_SHORT).show();
                        btnUpgrade.setText("Premium Active ✅");
                        btnUpgrade.setEnabled(false);
                    }

                    @Override
                    public void onPurchaseFailed(@NonNull String message) {
                        // no active subscription
                    }

                    @Override
                    public void onUserCancelled() {}

                    @Override
                    public void onRestoreEmpty() {}
                });
            }

            @Override
            public void onDisconnected() {
                Toast.makeText(SubscriptionActivity.this, "Billing disconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull String error) {
                Toast.makeText(SubscriptionActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });

        btnUpgrade.setOnClickListener(v -> {
            if (subscriptionDetails == null) {
                Toast.makeText(this, "Subscription not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            billingManager.launchSubscriptionPurchase(this, subscriptionDetails, new BillingManager.PurchaseListener() {
                @Override
                public void onPurchaseSuccess(@NonNull Purchase purchase) {
                    Toast.makeText(SubscriptionActivity.this, "Subscribed 🎉", Toast.LENGTH_SHORT).show();
                    btnUpgrade.setText("Premium Active ✅");
                    btnUpgrade.setEnabled(false);
                    RemoteConfigManager.activatePremium(getApplicationContext());
                }

                @Override
                public void onPurchaseFailed(@NonNull String message) {
                    Toast.makeText(SubscriptionActivity.this, message, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onUserCancelled() {
                    Toast.makeText(SubscriptionActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onRestoreEmpty() {}
            });
        });

        tvRestore.setOnClickListener(v -> {
            billingManager.restoreSubscriptions(new BillingManager.PurchaseListener() {
                @Override
                public void onPurchaseSuccess(@NonNull Purchase purchase) {
                    Toast.makeText(SubscriptionActivity.this, "Subscription Restored ✅", Toast.LENGTH_SHORT).show();
                    btnUpgrade.setText("Premium Active ✅");
                    btnUpgrade.setEnabled(false);
                }

                @Override
                public void onPurchaseFailed(@NonNull String message) {
                    Toast.makeText(SubscriptionActivity.this, "No active subscription found", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUserCancelled() {}

                @Override
                public void onRestoreEmpty() {}
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) billingManager.endConnection();
    }
}
