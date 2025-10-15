package io.simplejpa.metadata;

import io.simplejpa.metadata.relation.RelationShipMetadata;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class EntityMetadata {
    private final Class<?> entityClass;
    private final String entityName;
    private final String tableName;
    private final String schemaName;
    private final String catalogName;
    private final IdentifierMetadata identifierMetadata;
    private final List<AttributeMetadata> attributeMetadatas;
    private final Map<String, RelationShipMetadata> relationShips;

    public EntityMetadata(
            Class<?> entityClass,
            String entityName,
            String tableName,
            String schemaName,
            String catalogName,
            IdentifierMetadata identifierMetadata,
            List<AttributeMetadata> attributeMetadatas,
            Map<String, RelationShipMetadata> relationShips
    ) {
        this.entityClass = entityClass;
        this.entityName = entityName;
        this.tableName = tableName;
        this.schemaName = schemaName;
        this.catalogName = catalogName;
        this.identifierMetadata = identifierMetadata;
        this.attributeMetadatas = attributeMetadatas;
        this.relationShips = relationShips;
    }

    public AttributeMetadata getAttributeMetadata(String fieldName) {
        return attributeMetadatas.stream()
                .filter(m -> m.getFieldName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }

    public Object newInstance() {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new instance of " +
                    entityClass.getName(), e);
        }
    }

    public String getQualifiedTableName() {
        StringBuilder sb = new StringBuilder();
        if (catalogName != null) {
            sb.append(catalogName).append(".");
        }
        if (schemaName != null) {
            sb.append(schemaName).append(".");
        }
        sb.append(tableName);
        return sb.toString();
    }
}
