package io.simplejpa.metadata;

import lombok.Getter;

import java.lang.reflect.Field;

/**
 * id metadata
 */
@Getter
public class IdentifierMetadata {
    private final String fieldName;
    private final String columnName;
    private final Class<?> javaType;
    private final Field field;

    public IdentifierMetadata(String fieldName, String columnName, Class<?> javaType, Field field) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.javaType = javaType;
        this.field = field;
        this.field.setAccessible(true);
    }

    public Object getValue(Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field to get identifier value is not accessible.", e);
        }
    }

    public void setValue(Object entity, Object value) {
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field to set identifier value is not accessible.", e);
        }
    }
}

