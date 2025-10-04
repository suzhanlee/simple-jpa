package io.simplejpa.metadata;

import lombok.Getter;

import java.lang.reflect.Field;

/**
 * attribute metadata
 */
@Getter
public class AttributeMetadata {
    private final String fieldName;
    private final String columnName;
    private final Class<?> javaType;
    private final Field field;

    private final boolean nullable;
    private final boolean unique;
    private final int length;
    private final boolean insertable;
    private final boolean updatable;

    public AttributeMetadata(String fieldName, String columnName, Class<?> javaType, Field field, boolean nullable, boolean unique, int length, boolean insertable, boolean updatable) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.javaType = javaType;
        this.field = field;
        this.nullable = nullable;
        this.unique = unique;
        this.length = length;
        this.insertable = insertable;
        this.updatable = updatable;
    }

    public Object getValue(Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field to get attribute value is not accessible.", e);
        }
    }

    public void setValue(Object entity, Object value) {
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field to set attribute value is not accessible.", e);
        }
    }
}
