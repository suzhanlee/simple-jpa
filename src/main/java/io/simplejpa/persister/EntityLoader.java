package io.simplejpa.persister;

import io.simplejpa.cache.PersistenceContext;
import io.simplejpa.engine.jdbc.EntityResultSetExtractor;
import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.sql.SelectSqlGenerator;
import io.simplejpa.engine.sql.SqlWithParameters;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.MetadataRegistry;
import io.simplejpa.util.TypeConverter;

import java.sql.Connection;

public class EntityLoader {
    private final MetadataRegistry metadataRegistry;
    private final SelectSqlGenerator selectSqlGenerator;
    private final JdbcExecutor jdbcExecutor;

    public EntityLoader(
            MetadataRegistry metadataRegistry,
            SelectSqlGenerator selectSqlGenerator,
            JdbcExecutor jdbcExecutor
    ) {
        this.metadataRegistry = metadataRegistry;
        this.selectSqlGenerator = selectSqlGenerator;
        this.jdbcExecutor = jdbcExecutor;
    }

    public <T> T load(Connection connection, Class<T> entityClass, Object id) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        SqlWithParameters sqlWithParameters = selectSqlGenerator.generateFindById(metadata, id);
        return jdbcExecutor.executeQuery(
                connection,
                sqlWithParameters.sql(),
                new EntityResultSetExtractor<T>(metadata, new TypeConverter()),
                sqlWithParameters.parameters()
        );
    }
}
