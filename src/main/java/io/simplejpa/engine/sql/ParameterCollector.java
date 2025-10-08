package io.simplejpa.engine.sql;

import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.util.TypeConverter;

import java.util.ArrayList;
import java.util.List;

public class ParameterCollector {
    private final TypeConverter typeConverter;

    public ParameterCollector(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    public List<Object> collectInsertParameters(
            EntityMetadata  entityMetadata,
            Object entity
    ) {
        List<Object> parameters = new ArrayList<>();
        for (AttributeMetadata attr : entityMetadata.getAttributeMetadatas()) {
            Object value = attr.getValue(entity);
            Object convertedValue = typeConverter.convertType(value, attr.getJavaType());
            parameters.add(convertedValue);
        }
        return parameters;
    }
}
