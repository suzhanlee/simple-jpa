package io.simplejpa.core;

import io.simplejpa.engine.connection.ConnectionConfiguration;
import io.simplejpa.engine.connection.ConnectionProvider;
import io.simplejpa.engine.connection.DriverManagerConnectionProvider;
import io.simplejpa.metadata.MetadataRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class EntityManagerFactoryImpl implements EntityManagerFactory {
    private final MetadataRegistry metadataRegistry;
    private final ConnectionProvider connectionProvider;
    private boolean open;
    private final Set<EntityManager> activeEntityManagers;

    private EntityManagerFactoryImpl(
            MetadataRegistry metadataRegistry,
            ConnectionProvider connectionProvider,
            Set<EntityManager> activeEntityManagers
    ) {
        this.metadataRegistry = metadataRegistry;
        this.connectionProvider = connectionProvider;
        this.activeEntityManagers = activeEntityManagers;
        this.open = true;
    }

    public static EntityManagerFactoryImpl createEntityManagerFactoryInstance(PersistenceConfiguration configuration) {
        MetadataRegistry registry = registerEntityClasses(configuration);
        return new EntityManagerFactoryImpl(
                registry,
                new DriverManagerConnectionProvider(new ConnectionConfiguration(
                        configuration.getUrl(),
                        configuration.getUsername(),
                        configuration.getPassword(),
                        configuration.getDriver()
                )),
                new HashSet<>()
        );
    }

    private static MetadataRegistry registerEntityClasses(PersistenceConfiguration configuration) {
        MetadataRegistry registry = new MetadataRegistry();
        Set<Class<?>> entityClasses = configuration.getEntityClasses();

        if (entityClasses != null && !entityClasses.isEmpty()) {
            for (Class<?> entityClass : entityClasses) {
                registry.scanAndRegister(entityClass);
                log.info("Entity class registered: {}", entityClass.getName());
            }
        }

        return registry;
    }

    @Override
    public synchronized EntityManager createEntityManager() {
        if (!isOpen()) {
            throw new IllegalStateException("EntityManagerFactory is closed");
        }
        EntityManager entityManager = new EntityManagerImpl(metadataRegistry, connectionProvider);
        activeEntityManagers.add(entityManager);
        return entityManager;
    }

    @Override
    public synchronized void close() {
        if (!isOpen()) {
            return;
        }
        for (EntityManager activeEntityManager : activeEntityManagers) {
            activeEntityManager.close();
        }
        activeEntityManagers.clear();
        connectionProvider.shutDown();
        open = false;
    }

    @Override
    public synchronized boolean isOpen() {
        return this.open;
    }
}
