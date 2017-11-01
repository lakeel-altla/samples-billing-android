package com.lakeel.altla.sample.billing.item;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lakeel.altla.android.log.Log;
import com.lakeel.altla.android.log.LogFactory;
import com.lakeel.altla.sample.billing.lib.purchasing.helper.BillingResponseCode;
import com.lakeel.altla.sample.billing.lib.purchasing.helper.IabHelper;
import com.lakeel.altla.sample.billing.lib.purchasing.helper.Purchase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This sample app is for static response test. Actually, billing will not be done.
 * See https://developer.android.com/google/play/billing/billing_testing.html#billing-testing-static
 */
public final class MainActivity extends AppCompatActivity {

    @BindView(R.id.container)
    View container;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    private static final Log LOG = LogFactory.getLog(MainActivity.class);

    private static final String ITEM_ID_TEST_PURCHASED = "android.test.purchased";

    private static final String ITEM_ID_TEST_CANCELED = "android.test.canceled";

    private static final String ITEM_ID_TEST_REFUNDED = "android.test.refunded";

    private static final String ITEM_ID_TEST_UNAVAILABLE = "android.test.item_unavailable";

    private static final int REQUEST_CODE_PURCHASE = 1;

    private IabHelper helper;

    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(MainActivity.this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new Adapter();
        recyclerView.setAdapter(adapter);

        // Bind the InAppBillingService that connects to Google Play using the helper class.
        helper = new IabHelper(this, getString(R.string.google_play_license_key));
        helper.startSetup(result -> {
            if (result.isFailure()) {
                // Cause: not supported api, failed to connect the service.
                LOG.e("Problem setting up In-app Billing: " + result);
                return;
            }

            fetchInventoriesItems();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                // API returns purchased response.
                purchaseItem(ITEM_ID_TEST_PURCHASED);
                break;
            case R.id.action_canceled:
                // API returns canceled response when cancel purchasing.
                purchaseItem(ITEM_ID_TEST_CANCELED);
                break;
            case R.id.action_refunded:
                // API returns refunded response.
                purchaseItem(ITEM_ID_TEST_REFUNDED);
                break;
            case R.id.action_unavailabled:
                // API returns unavailable response when the item doesn't exist.
                purchaseItem(ITEM_ID_TEST_UNAVAILABLE);
                break;
        }
        return super.onOptionsItemSelected(item);
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

        LOG.d("Unbind the service to prevent decreasing of the application performance.");

        if (helper != null) {
            helper.disposeWhenFinished();
            helper = null;
        }
    }

    private void fetchInventoriesItems() {
        try {
            helper.queryInventoryAsync((result, inventory) -> {
                if (result.isFailure()) {
                    LOG.e("Failed to query inventory: " + result);
                    showSnackBar(R.string.snackbar_query_failed);
                    return;
                }

                List<Purchase> allPurchases = inventory.getAllPurchases();
                if (allPurchases.isEmpty()) LOG.d("No purchased items.");

                adapter.setItems(allPurchases);
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
            helper.launchPurchaseFlow(this, itemId, REQUEST_CODE_PURCHASE,
                    (result, purchase) -> {
                        if (result.isFailure()) {
                            LOG.e("Error purchasing: " + result);

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

                        // Send orderId and signature to server. This app doesn't send these data.

                        fetchInventoriesItems();
                    }, developerPayload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            LOG.e("Error launching purchase flow. Another async operation in progress.", e);
            showSnackBar(R.string.snackbar_purchase_failed);
        }
    }

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

                // Save item data to server.
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

        private List<Purchase> items = new ArrayList<>();

        void setItems(List<Purchase> purchases) {
            items.clear();
            items.addAll(purchases);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase, parent, false);
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

        final class ViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.container)
            View container;

            @BindView(R.id.textViewItemId)
            TextView textViewItemId;

            @BindView(R.id.textViewPurchasedTime)
            TextView textViewPurchasedTime;

            ViewHolder(View itemView) {
                super(itemView);

                ButterKnife.bind(this, itemView);
            }

            void showItem(@NonNull Purchase purchase) {
                LOG.d(purchase.toString());

                textViewItemId.setText(purchase.getSku());

                Date date = new Date(purchase.getPurchaseTime());
                SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm", Locale.getDefault());
                textViewPurchasedTime.setText(format.format(date));

                container.setOnClickListener(v -> {
                    consumeItem(purchase);

                    items.remove(purchase);
                    notifyDataSetChanged();

                    showSnackBar(R.string.snackbar_consumed);
                });
            }
        }
    }
}
