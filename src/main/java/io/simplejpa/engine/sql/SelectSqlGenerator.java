package io.simplejpa.engine.sql;

import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;

import java.util.List;

public class SelectSqlGenerator {
    public SqlWithParameters generateFindById(EntityMetadata metadata, Object id) {
        String selectSql = createSelectSql(metadata);
        return new SqlWithParameters(selectSql, List.of(id));
    }

    private List<String> extractColumNames(EntityMetadata metadata) {
        return metadata.getAttributeMetadatas()
                .stream()
                .map(AttributeMetadata::getColumnName)
                .toList();
    }

    private String extractIdColumName(EntityMetadata metadata) {
        return metadata.getIdentifierMetadata().getColumnName();
    }

    private String createSelectSql(EntityMetadata metadata) {
        SqlBuilder builder = new SqlBuilder();
        String selectPart = builder
                .append("SELECT ")
                .appendColumns(extractColumNames(metadata))
                .append(" FROM ")
                .appendTable(metadata.getTableName())
                .build();

        WhereClauseBuilder whereClauseBuilder = new WhereClauseBuilder();
        String wherePart = whereClauseBuilder
                .where(extractIdColumName(metadata))
                .equals()
                .build();

        return selectPart + " " + wherePart;
    }
}
