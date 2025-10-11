package io.simplejpa.query.jpql.ast;

public record SelectStatement(
        String alias,
        String entityName,
        WhereClause whereClause
) {

}
