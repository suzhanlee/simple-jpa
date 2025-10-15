package io.simplejpa.mapping;

import io.simplejpa.annotation.Column;
import io.simplejpa.annotation.Entity;
import io.simplejpa.annotation.Id;
import io.simplejpa.annotation.Table;
import io.simplejpa.annotation.relation.*;
import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.IdentifierMetadata;
import io.simplejpa.metadata.relation.JoinTableMetadata;
import io.simplejpa.metadata.relation.RelationShipMetadata;
import io.simplejpa.metadata.relation.RelationType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnnotationProcessor {

    public EntityMetadata processEntity(Class<?> entityClass) {
        validateEntity(entityClass);

        Field[] fields = entityClass.getDeclaredFields();

        IdentifierMetadata identifierMetadata = createIdentifierMetadata(fields);
        List<AttributeMetadata> attributeMetadatas = createAttributeMetadata(fields);
        Map<String, RelationShipMetadata> relationShipMetadatas = createRelationMetadatas(fields, entityClass);

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

    private List<AttributeMetadata> createAttributeMetadata(Field[] fields) {
        return Arrays.stream(fields)
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .filter(field -> !isRelationShipField(field))
                .map(this::createColumnField)
                .collect(Collectors.toList());
    }

    private boolean isRelationShipField(Field field) {
        return field.isAnnotationPresent(ManyToOne.class) ||
                field.isAnnotationPresent(OneToMany.class) ||
                field.isAnnotationPresent(OneToOne.class) ||
                field.isAnnotationPresent(ManyToMany.class);
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

    private Map<String, RelationShipMetadata> createRelationMetadatas(Field[] fields, Class<?> entityClass) {
        Map<String, RelationShipMetadata> relationShipMetadatas = new HashMap<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne manyToOneRelation = field.getAnnotation(ManyToOne.class);
                String joinColumnName = extractJoinColumnName(field);
                relationShipMetadatas.put(field.getName(), new RelationShipMetadata(
                        RelationType.MANY_TO_ONE,
                        field.getType(),
                        field.getName(),
                        manyToOneRelation.fetch(),
                        manyToOneRelation.cascade(),
                        joinColumnName,
                        null,
                        null
                ));
            }

            if (field.isAnnotationPresent(OneToMany.class)) {
                OneToMany oneToManyRelation = field.getAnnotation(OneToMany.class);
                relationShipMetadatas.put(field.getName(), new RelationShipMetadata(
                        RelationType.ONE_TO_MANY,
                        extractTargetEntityClass(field),
                        field.getName(),
                        oneToManyRelation.fetch(),
                        oneToManyRelation.cascade(),
                        null,
                        oneToManyRelation.mappedBy(),
                        null
                ));
            }

            if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne oneToOneRelation = field.getAnnotation(OneToOne.class);

                String joinColumnName = null;
                if (oneToOneRelation.mappedBy().isEmpty()) {
                    joinColumnName = extractJoinColumnName(field);
                }

                relationShipMetadatas.put(field.getName(), new RelationShipMetadata(
                        RelationType.ONE_TO_ONE,
                        field.getType(),
                        field.getName(),
                        oneToOneRelation.fetch(),
                        oneToOneRelation.cascade(),
                        joinColumnName,
                        oneToOneRelation.mappedBy(),
                        null
                ));
            }

            if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany manyToManyRelation = field.getAnnotation(ManyToMany.class);
                JoinTableMetadata joinTableMetadata = createJoinTableMetadata(field);
                relationShipMetadatas.put(field.getName(), new RelationShipMetadata(
                        RelationType.MANY_TO_MANY,
                        extractTargetEntityClass(field),
                        field.getName(),
                        manyToManyRelation.fetch(),
                        manyToManyRelation.cascade(),
                        null,
                        manyToManyRelation.mappedBy(),
                        joinTableMetadata
                ));
            }
        }
        return relationShipMetadatas;
    }

    private Class<?> extractTargetEntityClass(Field field) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        return (Class<?>) genericType.getActualTypeArguments()[0];
    }

    private JoinTableMetadata createJoinTableMetadata(Field field) {
        if (field.isAnnotationPresent(JoinTable.class)) {
            JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);
            String joinTableName = joinTableAnnotation.name();
            String[] joinColumns = Arrays.stream(joinTableAnnotation.joinColumns())
                    .map(JoinColumn::name)
                    .toArray(String[]::new);
            String[] inverseJoinColumns = Arrays.stream(joinTableAnnotation.inverseJoinColumns())
                    .map(JoinColumn::name)
                    .toArray(String[]::new);
            return new JoinTableMetadata(joinTableName, joinColumns, inverseJoinColumns);
        }
        return null;
    }

    private String extractJoinColumnName(Field field) {
        if (field.isAnnotationPresent(JoinColumn.class)) {
            return field.getAnnotation(JoinColumn.class).name();
        }
        return createDefaultJoinColumnName(field);
    }

    private String createDefaultJoinColumnName(Field field) {
        return field.getName() + "_id";
    }
}
