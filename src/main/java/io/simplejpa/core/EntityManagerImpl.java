package io.simplejpa.core;

import io.simplejpa.cache.PersistenceContext;
import io.simplejpa.engine.connection.ConnectionProvider;
import io.simplejpa.metadata.MetadataRegistry;
import io.simplejpa.transaction.JdbcTransaction;

public class EntityManagerImpl implements EntityManager {
    private final PersistenceContext persistenceContext;
    private final MetadataRegistry metadataRegistry;
    private final JdbcTransaction jdbcTransaction;
    private boolean open;

    public EntityManagerImpl(
            PersistenceContext persistenceContext,
            MetadataRegistry metadataRegistry,
            ConnectionProvider connectionProvider
    ) {
        this.persistenceContext = persistenceContext;
        this.metadataRegistry = metadataRegistry;
        this.jdbcTransaction = new JdbcTransaction(connectionProvider);
        this.open = true;
    }

    @Override
    public EntityTransaction getTransaction() {
        validateOpen();
        return jdbcTransaction;
    }

    @Override
    public void close() {
        if (!isOpen()) {
            return;
        }
        try {
            if (jdbcTransaction.isActive()) {
                throw new IllegalStateException(
                        "Cannot close EntityManager with active transaction. " +
                                "Call commit() or rollback() first."
                );
            }
            persistenceContext.clear();
        } finally {
            open = false;
        }
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public void persist(Object entity) {
        validateOpen();
        persistenceContext.addEntity(entity);
    }

    private void validateOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("EntityManager is closed");
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        validateOpen();

        T entity = persistenceContext.getEntity(entityClass, primaryKey);
        if (entity != null) {
            return entity;
        }
        // TODO DB 조회 필요
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public <T> T merge(T entity) {
        validateOpen();
        // TODO DB 조회 필요
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void remove(Object entity) {
        validateOpen();
        persistenceContext.removeEntity(entity);
    }

    @Override
    public void flush() {
        validateOpen();
        validateTransactionIsActive();
        persistenceContext.flush(jdbcTransaction.getConnection());
    }

    private void validateTransactionIsActive() {
        if (!jdbcTransaction.isActive()) {
            throw new IllegalStateException("Transaction is not active");
        }
    }

    @Override
    public boolean contains(Object entity) {
        validateOpen();
        return persistenceContext.contains(entity);
    }
}
