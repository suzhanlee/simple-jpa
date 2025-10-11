package io.simplejpa.core;

import io.simplejpa.cache.PersistenceContext;
import io.simplejpa.engine.connection.ConnectionProvider;
import io.simplejpa.engine.jdbc.EntityResultSetExtractor;
import io.simplejpa.engine.jdbc.ParameterBinder;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.MetadataRegistry;
import io.simplejpa.persister.EntityLoader;
import io.simplejpa.query.Query;
import io.simplejpa.query.QueryImpl;
import io.simplejpa.query.TypedQuery;
import io.simplejpa.query.TypedQueryImpl;
import io.simplejpa.query.jpql.JpqlParser;
import io.simplejpa.query.jpql.QueryExecutor;
import io.simplejpa.query.jpql.QueryTranslator;
import io.simplejpa.query.jpql.TranslatedQuery;
import io.simplejpa.query.jpql.ast.SelectStatement;
import io.simplejpa.transaction.JdbcTransaction;
import io.simplejpa.util.TypeConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityManagerImpl implements EntityManager, QueryExecutor {
    private final MetadataRegistry metadataRegistry;
    private final PersistenceContext persistenceContext;
    private final JdbcTransaction jdbcTransaction;
    private final EntityLoader entityLoader;
    private final ParameterBinder parameterBinder;
    private boolean open;

    public EntityManagerImpl(
            MetadataRegistry metadataRegistry,
            PersistenceContext persistenceContext,
            ConnectionProvider connectionProvider,
            EntityLoader entityLoader,
            ParameterBinder parameterBinder
    ) {
        this.metadataRegistry = metadataRegistry;
        this.persistenceContext = persistenceContext;
        this.jdbcTransaction = new JdbcTransaction(connectionProvider);
        this.parameterBinder = parameterBinder;

        // call back
        this.jdbcTransaction.setFlushCallback(this::flush);
        this.jdbcTransaction.setClearCallback(persistenceContext::clear);

        this.entityLoader = entityLoader;
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
        validatePersistable(entity);
        persistenceContext.addEntity(entity);
    }

    private void validatePersistable(Object entity) {
        validateOpen();
        validateTransactionIsActive();
        if (entity == null) {
            throw new IllegalArgumentException("Entity is null");
        }

        if (persistenceContext.contains(entity)) {
            throw new IllegalArgumentException("Entity is already managed by the persistence context");
        }
    }

    private void validateOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("EntityManager is closed");
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        validateQueryable(entityClass, primaryKey);
        T entity = persistenceContext.getEntity(entityClass, primaryKey);
        if (entity != null) {
            return entity;
        }

        validateTransactionIsActive();

        entity = entityLoader.load(jdbcTransaction.getConnection(), entityClass, primaryKey);
        if (entity != null) {
            persistenceContext.addEntity(entity);
        }
        return entity;
    }

    private <T> void validateQueryable(Class<T> entityClass, Object primaryKey) {
        validateOpen();
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class must not be null");
        }

        if (primaryKey == null) {
            throw new IllegalArgumentException("Primary key must not be null");
        }
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
        validateFlushable();
        persistenceContext.flush(jdbcTransaction.getConnection());
    }

    private void validateFlushable() {
        validateOpen();
        validateTransactionIsActive();
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

    @Override
    public <T> List<T> executeQuery(
            String jpql,
            Class<T> resultClass,
            Map<String, Object> namedParameters,
            Map<Integer, Object> positionalParameters
    ) {
        JpqlParser jpqlParser = new JpqlParser();
        SelectStatement parsedStatement = jpqlParser.parse(jpql);

        QueryTranslator queryTranslator = new QueryTranslator(metadataRegistry);
        TranslatedQuery translatedQuery = queryTranslator.translate(parsedStatement);

        try {
            Connection connection = jdbcTransaction.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(translatedQuery.sql());

            parameterBinder.bindQueryParameters(
                    pstmt,
                    translatedQuery.parameterOrder(),
                    namedParameters,
                    positionalParameters
            );

            ResultSet resultSet = pstmt.executeQuery();
            return mapResultSet(resultSet, resultClass);
        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed", e);
        }
    }

    @Override
    public Query createQuery(String jpql) {
        validateOpen();
        return new QueryImpl(jpql, this);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String jpql, Class<T> resultClass) {
        validateOpen();
        return new TypedQueryImpl<>(jpql, this, resultClass);
    }

    private <T> List<T> mapResultSet(
            ResultSet rs,
            Class<T> resultClass
    ) throws SQLException {
        List<T> results = new ArrayList<>();
        EntityMetadata metadata = metadataRegistry.getMetadata(resultClass);
        TypeConverter typeConverter = new TypeConverter();

        while (rs.next()) {
            T entity = createEntityByResultSet(rs, resultClass, metadata, typeConverter);
            results.add(entity);
        }

        return results;
    }

    private <T> T createEntityByResultSet(
            ResultSet rs,
            Class<T> resultClass,
            EntityMetadata metadata,
            TypeConverter typeConverter
    ) throws SQLException {
        T entity = findFromFirstCache(rs, resultClass, metadata);
        if (entity == null) {
            EntityResultSetExtractor<T> extractor = new EntityResultSetExtractor<>(metadata, typeConverter);
            entity = extractor.extractData(rs);
            persistenceContext.addEntity(entity);
        }
        return entity;
    }

    private <T> T findFromFirstCache(
            ResultSet rs,
            Class<T> resultClass,
            EntityMetadata metadata
    ) throws SQLException {
        Object id = rs.getObject(metadata.getIdentifierMetadata().getColumnName());
        return persistenceContext.getEntity(resultClass, id);
    }
}
