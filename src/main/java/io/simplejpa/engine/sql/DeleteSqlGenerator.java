package io.simplejpa.engine.sql;

import io.simplejpa.metadata.EntityMetadata;

import java.util.List;

public class DeleteSqlGenerator {
    public SqlWithParameters generateSql(
            EntityMetadata entityMetadata,
            Object id
    ) {
        SqlBuilder sqlBuilder = new SqlBuilder();
        String deleteSqlPart = sqlBuilder.append("DELETE FROM ")
                .appendTable(entityMetadata.getTableName())
                .build();

        WhereClauseBuilder whereClauseBuilder = new WhereClauseBuilder();
        String idColumnName = entityMetadata.getIdentifierMetadata().getColumnName();
        String wherePart = whereClauseBuilder.whereEquals(idColumnName).build();

        String sql = deleteSqlPart + " " + wherePart;

        return new SqlWithParameters(sql, List.of(id));
    }

}
