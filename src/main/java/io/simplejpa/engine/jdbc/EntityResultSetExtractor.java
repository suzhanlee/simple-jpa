package io.simplejpa.engine.jdbc;

import io.simplejpa.exception.JdbcException;
import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.util.TypeConverter;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EntityResultSetExtractor<T> implements ResultSetExtractor<T> {
    private final EntityMetadata entityMetadata;
    private final TypeConverter typeConverter;

    public EntityResultSetExtractor(
            EntityMetadata entityMetadata,
            TypeConverter typeConverter
    ) {
        this.entityMetadata = entityMetadata;
        this.typeConverter = typeConverter;
    }

    @Override
    public T extractData(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }

        return extractEntityFromResultSet(rs);
    }

    private T extractEntityFromResultSet(ResultSet rs) throws SQLException {
        Object entity = createEntityDefaultInstance();

        for (AttributeMetadata attr : entityMetadata.getAttributeMetadatas()) {
            String columnName = attr.getColumnName();
            Object value = typeConverter.convert(rs, columnName, attr.getJavaType());
            attr.setValue(entity, value);
        }

        return (T) entity;
    }

    private Object createEntityDefaultInstance() {
        try {
            return entityMetadata.getEntityClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new JdbcException("Failed to create entity instance", e);
        }
    }

}
