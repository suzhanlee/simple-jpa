package io.simplejpa.persister;

import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.sql.DeleteSqlGenerator;
import io.simplejpa.engine.sql.SqlWithParameters;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.MetadataRegistry;

import java.sql.Connection;

public class EntityDeleter {
    private final MetadataRegistry metadataRegistry;
    private final DeleteSqlGenerator deleteSqlGenerator;
    private final JdbcExecutor jdbcExecutor;

    public EntityDeleter(
            MetadataRegistry metadataRegistry,
            DeleteSqlGenerator deleteSqlGenerator,
            JdbcExecutor jdbcExecutor
    ) {
        this.metadataRegistry = metadataRegistry;
        this.deleteSqlGenerator = deleteSqlGenerator;
        this.jdbcExecutor = jdbcExecutor;
    }

    public void delete(Connection connection, Object entity) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());
        Object idValue = metadata.getIdentifierMetadata().getValue(entity);
        SqlWithParameters sqlWithParameters = deleteSqlGenerator.generateSql(metadata, idValue);
        jdbcExecutor.executeUpdate(
                connection,
                sqlWithParameters.sql(),
                sqlWithParameters.parameters()
        );
    }
}
