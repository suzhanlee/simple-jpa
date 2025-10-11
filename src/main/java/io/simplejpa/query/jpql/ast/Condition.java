package io.simplejpa.query.jpql.ast;

public record Condition(
        String leftSide,
        String operator,
        String rightSide
) {
}
