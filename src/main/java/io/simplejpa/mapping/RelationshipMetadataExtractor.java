package io.simplejpa.mapping;

import io.simplejpa.annotation.relation.*;
import io.simplejpa.metadata.relation.JoinTableMetadata;
import io.simplejpa.metadata.relation.RelationShipMetadata;
import io.simplejpa.metadata.relation.RelationType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RelationshipMetadataExtractor {

    public Map<String, RelationShipMetadata> extract(Field[] fields) {
        Map<String, RelationShipMetadata> relationships = new HashMap<>();

        for (Field field : fields) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                relationships.put(field.getName(), extractManyToOne(field));
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                relationships.put(field.getName(), extractOneToMany(field));
            }
            if (field.isAnnotationPresent(OneToOne.class)) {
                relationships.put(field.getName(), extractOneToOne(field));
            }
            if (field.isAnnotationPresent(ManyToMany.class)) {
                relationships.put(field.getName(), extractManyToMany(field));
            }
        }

        return relationships;
    }

    private RelationShipMetadata extractManyToOne(Field field) {
        ManyToOne annotation = field.getAnnotation(ManyToOne.class);
        String joinColumnName = extractJoinColumnName(field);

        return new RelationShipMetadata(
                RelationType.MANY_TO_ONE,
                field.getType(),
                field.getName(),
                annotation.fetch(),
                annotation.cascade(),
                joinColumnName,
                null,
                null
        );
    }

    private RelationShipMetadata extractOneToMany(Field field) {
        OneToMany annotation = field.getAnnotation(OneToMany.class);
        Class<?> targetClass = extractGenericType(field);

        return new RelationShipMetadata(
                RelationType.ONE_TO_MANY,
                targetClass,
                field.getName(),
                annotation.fetch(),
                annotation.cascade(),
                null,
                annotation.mappedBy(),
                null
        );
    }

    private RelationShipMetadata extractOneToOne(Field field) {
        OneToOne annotation = field.getAnnotation(OneToOne.class);

        String joinColumnName = null;
        if (annotation.mappedBy().isEmpty()) {
            joinColumnName = extractJoinColumnName(field);
        }

        return new RelationShipMetadata(
                RelationType.ONE_TO_ONE,
                field.getType(),
                field.getName(),
                annotation.fetch(),
                annotation.cascade(),
                joinColumnName,
                annotation.mappedBy(),
                null
        );
    }

    private RelationShipMetadata extractManyToMany(Field field) {
        ManyToMany annotation = field.getAnnotation(ManyToMany.class);
        Class<?> targetClass = extractGenericType(field);
        JoinTableMetadata joinTableMetadata = extractJoinTable(field);

        return new RelationShipMetadata(
                RelationType.MANY_TO_MANY,
                targetClass,
                field.getName(),
                annotation.fetch(),
                annotation.cascade(),
                null,
                annotation.mappedBy(),
                joinTableMetadata
        );
    }

    private String extractJoinColumnName(Field field) {
        if (field.isAnnotationPresent(JoinColumn.class)) {
            return field.getAnnotation(JoinColumn.class).name();
        }
        return field.getName() + "_id"; // default
    }

    private JoinTableMetadata extractJoinTable(Field field) {
        if (field.isAnnotationPresent(JoinTable.class)) {
            JoinTable annotation = field.getAnnotation(JoinTable.class);
            String tableName = annotation.name();
            String[] joinColumns = Arrays.stream(annotation.joinColumns())
                    .map(JoinColumn::name)
                    .toArray(String[]::new);
            String[] inverseJoinColumns = Arrays.stream(annotation.inverseJoinColumns())
                    .map(JoinColumn::name)
                    .toArray(String[]::new);
            return new JoinTableMetadata(tableName, joinColumns, inverseJoinColumns);
        }
        return null;
    }

    private Class<?> extractGenericType(Field field) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        return (Class<?>) genericType.getActualTypeArguments()[0];
    }
}