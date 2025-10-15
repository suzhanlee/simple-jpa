package io.simplejpa.mapping;

import io.simplejpa.annotation.Entity;
import io.simplejpa.annotation.Table;
import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.IdentifierMetadata;
import io.simplejpa.metadata.relation.RelationShipMetadata;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 엔티티 클래스의 애노테이션을 처리하여 EntityMetadata를 생성하는 조정자(Coordinator)
 *
 * 책임:
 * - 엔티티 검증
 * - 테이블 정보 추출
 * - 하위 Extractor들을 조정하여 메타데이터 생성
 */
public class AnnotationProcessor {

    private final IdentifierMetadataExtractor identifierExtractor;
    private final AttributeMetadataExtractor attributeExtractor;
    private final RelationshipMetadataExtractor relationshipExtractor;

    public AnnotationProcessor() {
        this.identifierExtractor = new IdentifierMetadataExtractor();
        this.attributeExtractor = new AttributeMetadataExtractor();
        this.relationshipExtractor = new RelationshipMetadataExtractor();
    }

    public EntityMetadata processEntity(Class<?> entityClass) {
        validateEntity(entityClass);

        Field[] fields = entityClass.getDeclaredFields();

        IdentifierMetadata identifierMetadata = identifierExtractor.extract(fields);
        List<AttributeMetadata> attributeMetadatas = attributeExtractor.extract(fields);
        Map<String, RelationShipMetadata> relationShipMetadatas = relationshipExtractor.extract(fields);

        validateIdentifierExists(entityClass, identifierMetadata);

        return new EntityMetadata(
                entityClass,
                entityClass.getSimpleName(),
                extractTableName(entityClass),
                extractSchemaName(entityClass),
                extractCatalogName(entityClass),
                identifierMetadata,
                attributeMetadatas,
                relationShipMetadatas
        );
    }

    private void validateEntity(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + entityClass.getName() + " is not annotated with @Entity");
        }
        try {
            entityClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + entityClass.getName() + " has no default constructor");
        } catch (Exception e) {
            throw new RuntimeException("Failed to check default constructor of " + entityClass.getName(), e);
        }
    }

    private void validateIdentifierExists(Class<?> entityClass, IdentifierMetadata identifierMetadata) {
        if (identifierMetadata == null) {
            throw new IllegalArgumentException("Class " + entityClass.getName() + " has no identifier field");
        }
    }

    private String extractTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return entityClass.getSimpleName();
    }

    private String extractSchemaName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            return table.schema();
        }
        return "";
    }

    private String extractCatalogName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            return table.catalog();
        }
        return "";
    }
}
