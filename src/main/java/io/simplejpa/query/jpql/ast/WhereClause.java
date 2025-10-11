package io.simplejpa.query.jpql.ast;

import java.util.List;

public record WhereClause(
        List<Condition> conditions
) {
}
