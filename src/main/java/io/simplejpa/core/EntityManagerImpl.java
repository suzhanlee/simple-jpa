package io.simplejpa.core;

import io.simplejpa.engine.connection.ConnectionProvider;
import io.simplejpa.metadata.MetadataRegistry;
import io.simplejpa.transaction.JdbcTransaction;

public class EntityManagerImpl implements EntityManager {
    private final MetadataRegistry metadataRegistry;
    private final JdbcTransaction jdbcTransaction;
    private boolean open;

    public EntityManagerImpl(
            MetadataRegistry metadataRegistry,
            ConnectionProvider connectionProvider
    ) {
        this.metadataRegistry = metadataRegistry;
        this.jdbcTransaction = new JdbcTransaction(connectionProvider);
        this.open = true;
    }

    @Override
    public EntityTransaction getTransaction() {
        if (!isOpen()) {
            throw new IllegalStateException("EntityManager is closed");
        }
        return jdbcTransaction;
    }

    @Override
    public void close() {
        this.open = false;
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }
}
