package io.simplejpa.core;

public interface EntityManager {
    EntityTransaction getTransaction();
    void close();
    boolean isOpen();
}
