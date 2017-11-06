# app-real-purchase-test

## Description
This is a sample to test with purchase by own product IDs.

See the following link.

[Testing In-app Billing - Testing In-app Purchases](https://developer.android.com/google/play/billing/billing_testing.html#testing-purchases)

## Usage

## Rename the package
Rename ```com.lakeel.altla.sample.billing.real.purchase.test``` to ```<your package name>``` to publish app in the Play Store.

## Set up secrets.xml
You create the ```secrets.xml``` in the ```lib-res/src/main/res/values/secrets.xml``` that include the license key and own product ids.

```xml:secrets.xml

<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_play_license_key">Your licence key</string>

    <string name="item_id_1">Your item id</string>
    <string name="item_id_2">Your item id</string>

    <string name="subscription_id_1">Your subscription item id</string>
    <string name="subscription_id_2">Your subscription item id</string>
</resources>

```

The licence key and own product ids are needed for publishing app in the Play Store.

See following link.
[Setting Up for Test Purchases](https://developer.android.com/google/play/billing/billing_testing.html#billing-testing-test)
