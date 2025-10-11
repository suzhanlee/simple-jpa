package io.simplejpa.query.jpql;

import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.MetadataRegistry;
import io.simplejpa.query.jpql.ast.Condition;
import io.simplejpa.query.jpql.ast.SelectStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryTranslator {
    private final MetadataRegistry metadataRegistry;

    public QueryTranslator(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    public TranslatedQuery translate(SelectStatement selectStatement) {
        EntityMetadata metadata = metadataRegistry.getMetadataByEntityName(selectStatement.entityName());

        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(metadata.getTableName());

        List<Condition> conditions = selectStatement.whereClause().conditions();
        if (conditions.isEmpty()) {
            return new TranslatedQuery(sql.toString(), Collections.emptyList());
        }

        sql.append(" WHERE ");
        List<String> parameterOrder = new ArrayList<>();

        for (int i = 0; i < conditions.size(); i++) {
            Condition condition = conditions.get(i);
            String columnName = convertFieldNameToColumName(condition, metadata);

            // ex. "SELECT * FROM users WHERE name = ? WHERE age > ?"
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(columnName)
                    .append(" ")
                    .append(condition.operator())
                    .append(" ?");

            parameterOrder.add(condition.rightSide());
        }


        return new TranslatedQuery(sql.toString(), parameterOrder);
    }

    private String convertFieldNameToColumName(Condition condition, EntityMetadata metadata) {
        String fieldName = removeAliasFromFieldName(condition);
        return metadata.getAttributeMetadata(fieldName).getColumnName();
    }

    private String removeAliasFromFieldName(Condition condition) {
        return condition.leftSide().contains(".")
                ? condition.leftSide().split("\\.")[1]
                : condition.leftSide();
    }
}
