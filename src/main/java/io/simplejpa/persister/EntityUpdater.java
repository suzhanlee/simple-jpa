package io.simplejpa.persister;

import io.simplejpa.cache.EntityEntry;
import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.sql.SqlWithParameters;
import io.simplejpa.engine.sql.UpdateSqlGenerator;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.MetadataRegistry;

import java.sql.Connection;

public class EntityUpdater {
    private final MetadataRegistry metadataRegistry;
    private final UpdateSqlGenerator updateSqlGenerator;
    private final JdbcExecutor jdbcExecutor;

    public EntityUpdater(
            MetadataRegistry metadataRegistry,
            UpdateSqlGenerator updateSqlGenerator,
            JdbcExecutor jdbcExecutor
    ) {
        this.metadataRegistry = metadataRegistry;
        this.updateSqlGenerator = updateSqlGenerator;
        this.jdbcExecutor = jdbcExecutor;
    }

    public void update(Connection connection, Object entity, EntityEntry entityEntry) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());
        SqlWithParameters sqlWithParameters = updateSqlGenerator.generateUpdateSql(
                metadata,
                entity
        );
        jdbcExecutor.executeUpdate(
                connection,
                sqlWithParameters.sql(),
                sqlWithParameters.parameters()
        );
    }

    public Object[] extractUpdateValues(Object entity) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());
        return metadata.getAttributeMetadatas().stream()
                .map(attributeMetadata -> attributeMetadata.getValue(entity))
                .toArray();
    }

}
