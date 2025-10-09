package io.simplejpa.core;

import io.simplejpa.engine.connection.ConnectionProvider;
import io.simplejpa.metadata.MetadataRegistry;

public class EntityManagerImpl implements EntityManager {
    private final MetadataRegistry metadataRegistry;
    private final ConnectionProvider connectionProvider;

    public EntityManagerImpl(
            MetadataRegistry metadataRegistry,
            ConnectionProvider connectionProvider
    ) {
        this.metadataRegistry = metadataRegistry;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void close() {
        // TODO
    }
}
