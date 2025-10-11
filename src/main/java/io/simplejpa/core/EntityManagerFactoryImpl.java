package io.simplejpa.core;

import io.simplejpa.cache.ActionQueue;
import io.simplejpa.cache.PersistenceContext;
import io.simplejpa.engine.connection.ConnectionConfiguration;
import io.simplejpa.engine.connection.ConnectionProvider;
import io.simplejpa.engine.connection.DriverManagerConnectionProvider;
import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.jdbc.ParameterBinder;
import io.simplejpa.engine.sql.*;
import io.simplejpa.metadata.MetadataRegistry;
import io.simplejpa.persister.EntityDeleter;
import io.simplejpa.persister.EntityLoader;
import io.simplejpa.persister.EntityPersister;
import io.simplejpa.persister.EntityUpdater;
import io.simplejpa.transaction.JdbcTransaction;
import io.simplejpa.util.TypeConverter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class EntityManagerFactoryImpl implements EntityManagerFactory {
    private final PersistenceContext persistenceContext;
    private final MetadataRegistry metadataRegistry;
    private final ConnectionProvider connectionProvider;
    private boolean open;
    private final Set<EntityManager> activeEntityManagers;

    private EntityManagerFactoryImpl(
            PersistenceContext persistenceContext,
            MetadataRegistry metadataRegistry,
            ConnectionProvider connectionProvider,
            Set<EntityManager> activeEntityManagers
    ) {
        this.persistenceContext = persistenceContext;
        this.metadataRegistry = metadataRegistry;
        this.connectionProvider = connectionProvider;
        this.activeEntityManagers = activeEntityManagers;
        this.open = true;
    }

    public static EntityManagerFactoryImpl createEntityManagerFactoryInstance(PersistenceConfiguration configuration) {
        MetadataRegistry registry = registerEntityClasses(configuration);
        return new EntityManagerFactoryImpl(
                new PersistenceContext(createActionQueue(registry), registry),
                registry,
                new DriverManagerConnectionProvider(new ConnectionConfiguration(
                        configuration.getUrl(),
                        configuration.getUsername(),
                        configuration.getPassword(),
                        configuration.getDriver()
                )),
                new HashSet<>());
    }

    private static ActionQueue createActionQueue(MetadataRegistry registry) {
        return new ActionQueue(
                new EntityPersister(
                        new JdbcExecutor(new ParameterBinder()),
                        new InsertSqlGenerator(new ParameterCollector(new TypeConverter())),
                        registry
                ),
                new EntityUpdater(
                        registry,
                        new UpdateSqlGenerator(new ParameterCollector(new TypeConverter())),
                        new JdbcExecutor(new ParameterBinder())),
                new EntityDeleter(
                        registry,
                        new DeleteSqlGenerator(),
                        new JdbcExecutor(new ParameterBinder())

                )
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
        EntityManager entityManager = new EntityManagerImpl(
                persistenceContext,
                connectionProvider,
                new EntityLoader(
                        metadataRegistry,
                        new SelectSqlGenerator(),
                        new JdbcExecutor(new ParameterBinder())
                )
        );
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
