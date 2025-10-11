package io.simplejpa.core;

public interface EntityTransaction {
    void begin();

    void commit();

    void rollback();

    boolean isActive();

    void setFlushCallback(Runnable callback);

    void setClearCallback(Runnable callback);
}
