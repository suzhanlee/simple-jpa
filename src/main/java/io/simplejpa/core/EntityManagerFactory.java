package io.simplejpa.core;

public interface EntityManagerFactory {
    EntityManager createEntityManager();
    void close();
    boolean isOpen();
}
