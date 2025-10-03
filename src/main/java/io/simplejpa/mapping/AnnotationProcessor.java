package io.simplejpa.mapping;

import io.simplejpa.annotation.Column;
import io.simplejpa.annotation.Entity;
import io.simplejpa.annotation.Id;
import io.simplejpa.annotation.Table;
import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.IdentifierMetadata;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnnotationProcessor {

    public EntityMetadata processEntity(Class<?> entityClass) {
        validateEntity(entityClass);

        Field[] fields = entityClass.getDeclaredFields();

        IdentifierMetadata identifierMetadata = createIdentifierMetadata(fields);
        List<AttributeMetadata> attributeMetadatas = createAttributeMetadata(fields);

        validateIdentifierExists(entityClass, identifierMetadata);

        return new EntityMetadata(
                entityClass,
                entityClass.getSimpleName(),
                extractTableName(entityClass),
                extractSchemaName(entityClass),
                extractCatalogName(entityClass),
                identifierMetadata,
                attributeMetadatas
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

    private List<AttributeMetadata> createAttributeMetadata(Field[] fields) {
        return Arrays.stream(fields)
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .map(this::createColumnField)
                .collect(Collectors.toList());
    }

    private IdentifierMetadata createIdentifierMetadata(Field[] fields) {
        IdentifierMetadata identifier = null;
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                if (identifier != null) {
                    throw new IllegalStateException("Entity has multiple @Id fields");
                }
                identifier = createIdField(field);
            }
        }
        return identifier;
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

    private String extractTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return entityClass.getSimpleName();
    }

    private IdentifierMetadata createIdField(Field field) {
        String fieldName = field.getName();
        String columnName = fieldName; // default

        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            if (!column.name().isEmpty()) {
                columnName = column.name();
            }
        }
        return new IdentifierMetadata(fieldName, columnName, field.getType(), field);
    }

    private AttributeMetadata createColumnField(Field field) {
        String fieldName = field.getName();
        String columnName = fieldName;

        boolean nullable = true;
        boolean unique = false;
        int length = 255;
        boolean insertable = true;
        boolean updatable = true;

        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            if (!column.name().isEmpty()) {
                columnName = column.name();
            }

            nullable = column.nullable();
            unique = column.unique();
            length = column.length();
            insertable = column.insertable();
            updatable = column.updatable();
        }

        return new AttributeMetadata(
                fieldName,
                columnName,
                field.getType(),
                field,
                nullable,
                unique,
                length,
                insertable,
                updatable
        );
    }

    private void validateIdentifierExists(Class<?> entityClass, IdentifierMetadata identifierMetadata) {
        if (identifierMetadata == null) {
            throw new IllegalArgumentException("Class " + entityClass.getName() + " has no identifier field");
        }
    }
}
