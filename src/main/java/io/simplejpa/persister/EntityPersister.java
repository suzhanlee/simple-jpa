package io.simplejpa.persister;

import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.sql.InsertSqlGenerator;
import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.MetadataRegistry;

import java.sql.Connection;
import java.util.List;

public class EntityPersister {
    private final JdbcExecutor jdbcExecutor;
    private final InsertSqlGenerator insertSqlGenerator;
    private final MetadataRegistry metadataRegistry;

    public EntityPersister(
            JdbcExecutor jdbcExecutor,
            InsertSqlGenerator insertSqlGenerator,
            MetadataRegistry metadataRegistry
    ) {
        this.jdbcExecutor = jdbcExecutor;
        this.insertSqlGenerator = insertSqlGenerator;
        this.metadataRegistry = metadataRegistry;
    }

    public Object insert(Connection connection, Object entity) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());
        Object generatedId = jdbcExecutor.executeInsert(
                connection,
                insertSqlGenerator.generate(metadata, entity).sql(),
                extractAttributes(entity, metadata)
        );

        if (generatedId != null) {
            metadata.getIdentifierMetadata().setValue(entity, generatedId);
        }

        return generatedId;
    }

    private Object[] extractAttributes(Object entity, EntityMetadata metadata) {
        List<AttributeMetadata> attributeMetadatas = metadata.getAttributeMetadatas();
        Object[] params = new Object[attributeMetadatas.size()];
        for (int i = 0; i < attributeMetadatas.size(); i++) {
            params[i] = attributeMetadatas.get(i).getValue(entity);
        }
        return params;
    }
}
