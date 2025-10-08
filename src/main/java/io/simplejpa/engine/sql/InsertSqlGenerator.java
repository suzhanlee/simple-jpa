package io.simplejpa.engine.sql;

import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;

import java.util.List;

public class InsertSqlGenerator {
    private final ParameterCollector parameterCollector;

    public InsertSqlGenerator(ParameterCollector parameterCollector) {
        this.parameterCollector = parameterCollector;
    }

    public SqlWithParameters generate(
            EntityMetadata entityMetadata,
            Object entity
    ) {
        List<Object> parameters = parameterCollector.collectInsertParameters(entityMetadata, entity);

        List<String> columnNames = entityMetadata.getAttributeMetadatas().stream()
                .map(AttributeMetadata::getColumnName)
                .toList();

        String insertSql = createInsertSql(entityMetadata, columnNames, parameters);
        return new SqlWithParameters(insertSql, parameters);
    }

    private String createInsertSql(EntityMetadata entityMetadata, List<String> columnNames, List<Object> parameters) {
        SqlBuilder sqlBuilder = new SqlBuilder(new StringBuilder());
        return sqlBuilder.append("INSERT INTO ")
                .appendTable(entityMetadata.getTableName())
                .append("(")
                .appendColumns(columnNames)
                .append(")")
                .append("VALUES(")
                .appendPlaceholders(parameters.size())
                .append(")")
                .build();
    }
}
