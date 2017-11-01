package com.lakeel.altla.sample.billing.lib.purchasing.helper;

public enum BillingResponseCode {

    BILLING_RESPONSE_RESULT_OK(0),
    BILLING_RESPONSE_RESULT_USER_CANCELED(1),
    BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE(2),
    BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE(3),
    BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE(4),
    BILLING_RESPONSE_RESULT_DEVELOPER_ERROR(5),
    BILLING_RESPONSE_RESULT_ERROR(6),
    BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED(7),
    BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED(8),
    BILLING_RESPONSE_UNKNOWN(9);

    private final int value;

    BillingResponseCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static BillingResponseCode toStatus(int value) {
        for (BillingResponseCode status : BillingResponseCode.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return BILLING_RESPONSE_UNKNOWN;
    }
}
