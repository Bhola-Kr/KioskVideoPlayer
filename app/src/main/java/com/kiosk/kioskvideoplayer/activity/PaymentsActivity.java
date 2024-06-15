package com.kiosk.kioskvideoplayer.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.kiosk.kioskvideoplayer.R;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import games.moisoni.google_iab.BillingConnector;
import games.moisoni.google_iab.BillingEventListener;
import games.moisoni.google_iab.enums.ProductType;
import games.moisoni.google_iab.models.BillingResponse;
import games.moisoni.google_iab.models.ProductInfo;
import games.moisoni.google_iab.models.PurchaseInfo;


public class PaymentsActivity extends AppCompatActivity {

//    private TextView pf, ppf, pp, pa, pc;
    private Button buySubNow, cancelSubNow;

    private BillingConnector billingConnector;
    private String sub_key = "chatbot_subscription";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

//        pf = findViewById(R.id.pf);
//        ppf = findViewById(R.id.ppf);
//        pp = findViewById(R.id.pp);
//        pa = findViewById(R.id.pa);
//        pc = findViewById(R.id.pc);

        buySubNow = findViewById(R.id.buySubNow);
        cancelSubNow = findViewById(R.id.cancelSubNow);
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(view -> onBackPressed());

        //create a list with subscription ids
        List<String> subscriptionIds = new ArrayList<>();
        subscriptionIds.add(sub_key);

        billingConnector = new BillingConnector(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq2zDLporpM75YEwIzuiWNwzuY1zC73BvfW+2AhogKt2uymDkksxV0VuqgA5XlsI1UT+a5FvzDF7ILDH8N/VrVDqLzVAlAIj5UBJK+OfiVl0r4oDBDZro1ModuGFQyFpaeN8x5VPjkLtwBLQReUX64cVWkRDaFgWfHsPgJYMGApNEiX0wy9/mnHoV0P/Hrr/pjNta5J5jfTDN4rUzDao40KMc83KYPNTYxU8j4L8zO6616qmANIUsyHWucjuvxagsdNeyryE18BJkYZ1ERFvhF3O0H9fdleUvj4ZmU1If85CHaMu72AzHib8e+0ZNqr4FxbbSgXcDRV/RFEYbu2E1iwIDAQAB")
                .setSubscriptionIds(subscriptionIds)
                .autoAcknowledge()
                .autoConsume()
                .enableLogging()
                .connect();

        billingConnector.setBillingEventListener(new BillingEventListener() {
            @Override
            public void onProductsFetched(@NonNull List<ProductInfo> productDetails) {
                String product;
                String price;
                for (ProductInfo productInfo : productDetails) {
                    product = productInfo.getProduct();
                    price = productInfo.getOneTimePurchaseOfferFormattedPrice();
                    if (product.equalsIgnoreCase(sub_key)) {
//                        pf.setText("Product Name: " + product + "Price :" + price);
                    }
                }
            }

            @Override
            public void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases) {
                String product;
                for (PurchaseInfo purchaseInfo : purchases) {
                    product = purchaseInfo.getProduct();
                    if (product.equalsIgnoreCase(sub_key)) {
                        showButton(true);
                        // This will invoked when subscriptions is already activated.
//                        ppf.setText("Purchased product fetched: " + product);
                    } else {
                        showButton(false);
                    }
                }
            }

            @Override
            public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
                String product;
                String purchaseToken;

                for (PurchaseInfo purchaseInfo : purchases) {
                    product = purchaseInfo.getProduct();
                    purchaseToken = purchaseInfo.getPurchaseToken();
                    if (product.equalsIgnoreCase(sub_key)) {
                        showButton(true);
                        // This will invoked when subscriptions is activated instantly.
//                        pp.setText("Product purchased: " + product + "token: " + purchaseToken);
                    }
                }
            }

            @Override
            public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
                String acknowledgedProduct = purchase.getProduct();
                if (acknowledgedProduct.equalsIgnoreCase(sub_key)) {
//                    pa.setText("Acknowledged: " + acknowledgedProduct);
                }
            }

            @Override
            public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
                String consumedProduct = purchase.getProduct();
                if (consumedProduct.equalsIgnoreCase(sub_key)) {
//                    pc.setText("PurchaseConsumed: " + consumedProduct);
                    Toast.makeText(PaymentsActivity.this, "PurchaseConsumed: " + consumedProduct, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
                switch (response.getErrorType()) {
                    case CLIENT_NOT_READY:
                        //TODO - client is not ready yet
                        break;
                    case CLIENT_DISCONNECTED:
                        //TODO - client has disconnected
                        break;
                    case PRODUCT_NOT_EXIST:
                        //TODO - product does not exist
                        break;
                    case CONSUME_ERROR:
                        //TODO - error during consumption
                        break;
                    case CONSUME_WARNING:
                        /*
                         * This will be triggered when a consumable purchase has a PENDING state
                         * User entitlement must be granted when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase complete
                         * and to come back later to receive their purchase
                         * */
                        //TODO - warning during consumption
                        break;
                    case ACKNOWLEDGE_ERROR:
                        //TODO - error during acknowledgment
                        break;
                    case ACKNOWLEDGE_WARNING:
                        /*
                         * This will be triggered when a purchase can not be acknowledged because the state is PENDING
                         * A purchase can be acknowledged only when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase complete
                         * and to come back later to receive their purchase
                         * */
                        //TODO - warning during acknowledgment
                        break;
                    case FETCH_PURCHASED_PRODUCTS_ERROR:
                        //TODO - error occurred while querying purchased products
                        break;
                    case BILLING_ERROR:
                        //TODO - error occurred during initialization / querying product details
                        break;
                    case USER_CANCELED:
                        //TODO - user pressed back or canceled a dialog
                        break;
                    //TODO - network connection is down
                    case BILLING_UNAVAILABLE:
                        //TODO - billing API version is not supported for the type requested
                        break;
                    case ITEM_UNAVAILABLE:
                        //TODO - requested product is not available for purchase
                        break;
                    case DEVELOPER_ERROR:
                        //TODO - invalid arguments provided to the API
                        break;
                    case ERROR:
                        //TODO - fatal error during the API action
                        break;
                    case ITEM_ALREADY_OWNED:
                        //TODO - failure to purchase since item is already owned
                        break;
                    case ITEM_NOT_OWNED:
                        //TODO - failure to consume since item is not owned
                        break;
                }
            }
        });

        buySubNow.setOnClickListener(view -> billingConnector.subscribe(PaymentsActivity.this, "chatbot_subscription"));
        cancelSubNow.setOnClickListener(view -> billingConnector.unsubscribe(PaymentsActivity.this, "chatbot_subscription"));
    }

    private void showButton(boolean isSubscribed) {
        if (isSubscribed) {
            buySubNow.setVisibility(View.GONE);
            cancelSubNow.setVisibility(View.VISIBLE);
        } else {
            buySubNow.setVisibility(View.VISIBLE);
            cancelSubNow.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingConnector != null) {
            billingConnector.release();
        }
    }
}