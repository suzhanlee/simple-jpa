package io.simplejpa.transaction;

import java.sql.Connection;

public interface TransactionCoordinator {
    Connection getConnection();
    void begin();
    void commit();
    void rollback();
    boolean isActive();
    TransactionStatus getStatus();
}
