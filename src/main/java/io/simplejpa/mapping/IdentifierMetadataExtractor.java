package io.simplejpa.mapping;

import io.simplejpa.annotation.Column;
import io.simplejpa.annotation.Id;
import io.simplejpa.metadata.IdentifierMetadata;

import java.lang.reflect.Field;

public class IdentifierMetadataExtractor {

    public IdentifierMetadata extract(Field[] fields) {
        IdentifierMetadata identifier = null;
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                if (identifier != null) {
                    throw new IllegalStateException("Entity has multiple @Id fields");
                }
                identifier = createIdentifierMetadata(field);
            }
        }
        return identifier;
    }

    private IdentifierMetadata createIdentifierMetadata(Field field) {
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
}