package io.simplejpa.engine.sql;

import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;

import java.util.List;

public class UpdateSqlGenerator {
    private final ParameterCollector parameterCollector;

    public UpdateSqlGenerator(ParameterCollector parameterCollector) {
        this.parameterCollector = parameterCollector;
    }

    public SqlWithParameters generateUpdateSql(
            EntityMetadata entityMetadata,
            Object entity
    ) {
        List<Object> parameters = parameterCollector.collectUpdateParameters(entityMetadata, entity);
        String sql = createUpdateSql(entityMetadata);
        return new SqlWithParameters(sql, parameters);
    }

    private String createUpdateSql(EntityMetadata entityMetadata) {
        SqlBuilder sqlBuilder = new SqlBuilder();
        String updatePart = sqlBuilder.append("UPDATE ")
                .appendTable(entityMetadata.getTableName())
                .append(" SET ")
                .appendSetClause(extractNonIdColumnNames(entityMetadata))
                .build();

        WhereClauseBuilder whereClauseBuilder = new WhereClauseBuilder();
        String idColumnName = entityMetadata.getIdentifierMetadata().getColumnName();
        String wherePart = whereClauseBuilder
                .whereEquals(idColumnName)
                .build();

        return updatePart + " " + wherePart;
    }

    private List<String> extractNonIdColumnNames(EntityMetadata metadata) {
        String idColumnName = metadata.getIdentifierMetadata().getColumnName();

        return metadata.getAttributeMetadatas()
                .stream()
                .map(AttributeMetadata::getColumnName)
                .filter(column -> !column.equals(idColumnName))
                .toList();
    }
}
