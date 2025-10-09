package io.simplejpa.core;

public interface EntityManager {
    EntityTransaction getTransaction();

    void close();

    boolean isOpen();

    void persist(Object entity);

    <T> T find(Class<T> entityClass, Object primaryKey);

    <T> T merge(T entity);

    void remove(Object entity);

    void flush();
}
