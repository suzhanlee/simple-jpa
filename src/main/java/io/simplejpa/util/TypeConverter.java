package io.simplejpa.util;

import io.simplejpa.exception.JdbcException;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TypeConverter {
    public Object convert(ResultSet rs, String columnName, Class<?> targetType) throws SQLException {
        Object value = rs.getObject(columnName);

        if (value == null) {
            return null;
        }

        return convertType(value, targetType);
    }

    public Object convertType(Object value, Class<?> targetType) {
        if (targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        }

        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            return Boolean.parseBoolean(value.toString());
        }

        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        }

        if (targetType == LocalDateTime.class) {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            }
            if (value instanceof java.util.Date) {
                return new Timestamp(((java.util.Date)
                        value).getTime()).toLocalDateTime();
            }
        }

        if (targetType == LocalDate.class) {
            if (value instanceof Date) {
                return ((Date) value).toLocalDate();
            }
            if (value instanceof java.util.Date) {
                return new Date(((java.util.Date)
                        value).getTime()).toLocalDate();
            }
        }

        throw new JdbcException("Cannot convert " + value.getClass() + " to " + targetType);
    }
}
