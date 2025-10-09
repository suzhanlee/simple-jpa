package io.simplejpa.core;

public interface EntityTransaction {
    void begin();
    void commit();
    void rollback();
    boolean isActive();
}
