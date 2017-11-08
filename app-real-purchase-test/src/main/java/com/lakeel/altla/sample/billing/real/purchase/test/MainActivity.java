package com.lakeel.altla.sample.billing.real.purchase.test;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.lakeel.altla.android.log.Log;
import com.lakeel.altla.android.log.LogFactory;
import com.lakeel.altla.sample.billing.lib.purchasing.helper.BillingResponseCode;
import com.lakeel.altla.sample.billing.lib.purchasing.helper.IabHelper;
import com.lakeel.altla.sample.billing.lib.purchasing.helper.Purchase;
import com.lakeel.altla.sample.billing.lib.purchasing.helper.SkuDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.lakeel.altla.sample.billing.lib.purchasing.helper.IabHelper.ITEM_TYPE_INAPP;

/**
 * This sample app is for real purchases.
 * Testing real in-app purchases enables you to test the end-to-end In-app Billing experience,
 * including the actual purchases from Google Play and the actual checkout flow that users will experience in your application.
 * <p>
 * See https://developer.android.com/google/play/billing/billing_testing.html#billing-testing-test
 */
public final class MainActivity extends AppCompatActivity {

    @BindView(R.id.container)
    View container;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    private static final Log LOG = LogFactory.getLog(MainActivity.class);

    private static final int REQUEST_CODE_PURCHASE_ITEM = 1;

    private static final int REQUEST_CODE_PURCHASE_SUBSCRIPTION = 2;

    private IabHelper helper;

    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(MainActivity.this);

        setTitle(getString(R.string.title_items));

        adapter = new Adapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        String base64EncodedPublicKey = getString(R.string.google_play_license_key);

        // Google highly recommends that you do not hard-code the exact public license key string value as provided by Google Play.
        // Instead, construct the whole public license key string at runtime from substrings or retrieve it from an encrypted store before passing it to the constructor.
        // This approach makes it more difficult for malicious third parties to modify the public license key string in your APK file.
        // See https://developer.android.com/training/in-app-billing/preparing-iab-app.html#Connect
        helper = new IabHelper(this, base64EncodedPublicKey);

        // Bind the InAppBillingService that connects to Google Play using the helper class.
        helper.startSetup(result -> {
            if (result.isFailure()) {
                // Cause: not supported api or failed to connect the service.
                LOG.e("Problem setting up In-app Billing: " + result);
                return;
            }

            LOG.i("Connected to Google Play App.");

            fetchItems();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOG.d("onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!helper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            LOG.d("onActivityResult handled by IABUtil.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LOG.i("Unbind the service to prevent decreasing of the application performance.");

        if (helper != null) {
            helper.disposeWhenFinished();
            helper = null;
        }
    }

    private void fetchItems() {
        try {
            helper.queryInventoryAsync(true,
                    Arrays.asList(getString(R.string.item_id_1), getString(R.string.item_id_2), getString(R.string.item_id_3)),
                    Arrays.asList(getString(R.string.subscription_id_1), getString(R.string.subscription_id_2)),
                    (result, inventory) -> {
                        if (result.isFailure()) {
                            LOG.e("Failed to query inventory: " + result);
                            showSnackBar(R.string.snackbar_query_failed);
                            return;
                        }

                        LOG.i("Query succeeded.");

                        List<SkuDetails> skuDetails = inventory.getAllSkuDetails();
                        if (skuDetails.isEmpty()) LOG.i("No items for purchasing.");

                        adapter.setItems(skuDetails);
                        adapter.notifyDataSetChanged();
                    });
        } catch (IabHelper.IabAsyncInProgressException e) {
            LOG.e("Error querying inventory. Another async operation in progress.", e);
            showSnackBar(R.string.snackbar_query_failed);
        }
    }

    private void purchaseItem(@NonNull String itemId) {
        if (!helper.subscriptionsSupported()) {
            LOG.w("Your device is not supported for subscription.");
            showSnackBar(R.string.snackbar_not_supported_subscription);
            return;
        }

        try {
            // The developerPayload is unique per purchasing request.
            String developerPayload = UUID.randomUUID().toString();
            helper.launchPurchaseFlow(this,
                    itemId,
                    REQUEST_CODE_PURCHASE_ITEM,
                    onIabPurchaseFinishedListener,
                    developerPayload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            LOG.e("Error launching purchase flow. Another async operation in progress.", e);
            showSnackBar(R.string.snackbar_purchase_failed);
        }
    }

    private void purchaseSubscription(@NonNull String itemId) {
        if (!helper.subscriptionsSupported()) {
            LOG.w("Your device is not supported for subscription.");
            showSnackBar(R.string.snackbar_not_supported_subscription);
            return;
        }

        try {
            // The developerPayload is unique per purchasing request.
            String developerPayload = UUID.randomUUID().toString();
            helper.launchSubscriptionPurchaseFlow(this,
                    itemId,
                    REQUEST_CODE_PURCHASE_SUBSCRIPTION,
                    onIabPurchaseFinishedListener
                    , developerPayload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            LOG.e("Error launching purchase flow. Another async operation in progress.", e);
            showSnackBar(R.string.snackbar_purchase_failed);
        }
    }

    private final IabHelper.OnIabPurchaseFinishedListener onIabPurchaseFinishedListener = (result, purchase) -> {
        if (result.isFailure()) {
            LOG.e("Error purchasing: " + result);

            if (purchase != null) {
                LOG.i(purchase.toString());
            }

            BillingResponseCode code = BillingResponseCode.toStatus(result.getResponse());
            if (BillingResponseCode.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED == code) {
                showSnackBar(R.string.snackbar_already_owned);
            } else {
                showSnackBar(R.string.snackbar_purchase_failed);
            }
            return;
        }

        // NOTE: It's highly recommended to validate purchase details on a server that you trust.
        // See https://developer.android.com/google/play/billing/billing_best_practices.html#validating-purchase-server

        // Check the returned data signature and the orderId, and verify that the orderId is a unique value that you have not previously processed.
        String orderId = purchase.getOrderId();

        // Verify that your app's key has signed the signature (INAPP_PURCHASE_DATA) that you process.
        String signature = purchase.getSignature();

        // Consume item if it is managed.
        if (purchase.getItemType().equals(ITEM_TYPE_INAPP)) {
            consumeItem(purchase);
        }
    };

    private void consumeItem(@NonNull Purchase purchased) {
        try {
            // Once you purchase the item, can not purchase it again until consume it.
            helper.consumeAsync(purchased, (purchase, result) -> {
                LOG.d("Consumption finished. Purchase: " + purchase + ", result: " + result);

                if (result.isFailure()) {
                    LOG.e("Error while consuming: " + result);
                    showSnackBar(R.string.snackbar_consume_failed);
                    return;
                }

                LOG.d("Consumption successful. Provisioning.");

                // Save consumption data to server.
            });
        } catch (IabHelper.IabAsyncInProgressException e) {
            LOG.e("Error consuming gas. Another async operation in progress.", e);
            showSnackBar(R.string.snackbar_consume_failed);
        }
    }

    private void showSnackBar(@StringRes int resId) {
        Snackbar.make(container, getString(resId), Snackbar.LENGTH_SHORT).show();
    }

    final class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private List<SkuDetails> items = new ArrayList<>();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sku_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.showItem(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void setItems(List<SkuDetails> items) {
            this.items.clear();
            this.items.addAll(items);
        }

        final class ViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.layoutSwipe)
            View layoutSwipe;

            @BindView(R.id.textViewTitle)
            TextView textViewTitle;

            @BindView(R.id.textViewDescription)
            TextView textViewDescription;

            @BindView(R.id.textViewPrice)
            TextView textViewPrice;

            @BindView(R.id.buttonPurchase)
            Button buttonPurchase;

            ViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            void showItem(@NonNull SkuDetails skuDetails) {
                LOG.d(skuDetails.toString());

                textViewTitle.setText(skuDetails.getTitle());
                textViewDescription.setText(skuDetails.getDescription());
                textViewPrice.setText(skuDetails.getPrice());

                buttonPurchase.setOnClickListener(v -> {
                    String type = skuDetails.getType();
                    if (ITEM_TYPE_INAPP.equals(type)) {
                        purchaseItem(skuDetails.getSku());
                    } else if (IabHelper.ITEM_TYPE_SUBS.equals(type)) {
                        purchaseSubscription(skuDetails.getSku());
                    }
                });

                layoutSwipe.setOnLongClickListener(v -> {
                    startActivity(DebugActivity.newIntent(MainActivity.this, skuDetails.getJson()));
                    return false;
                });
            }
        }
    }
}
