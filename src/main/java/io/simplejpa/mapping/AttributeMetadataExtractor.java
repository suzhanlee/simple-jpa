package io.simplejpa.mapping;

import io.simplejpa.annotation.Column;
import io.simplejpa.annotation.Id;
import io.simplejpa.annotation.relation.ManyToMany;
import io.simplejpa.annotation.relation.ManyToOne;
import io.simplejpa.annotation.relation.OneToMany;
import io.simplejpa.annotation.relation.OneToOne;
import io.simplejpa.metadata.AttributeMetadata;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AttributeMetadataExtractor {

    public List<AttributeMetadata> extract(Field[] fields) {
        return Arrays.stream(fields)
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .filter(field -> !isRelationshipField(field))
                .map(this::createAttributeMetadata)
                .collect(Collectors.toList());
    }

    private boolean isRelationshipField(Field field) {
        return field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(OneToOne.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    private AttributeMetadata createAttributeMetadata(Field field) {
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
}