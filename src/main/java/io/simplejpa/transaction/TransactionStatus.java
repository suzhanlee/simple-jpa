package io.simplejpa.transaction;

public enum TransactionStatus {
    NOT_ACTIVE,
    ACTIVE,
    COMMITTED,
    ROLLED_BACK;
}
