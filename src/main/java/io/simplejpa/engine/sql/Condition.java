package io.simplejpa.engine.sql;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record Condition(
        String columnName,
        Operator operator,
        int parameterCount
) {
    public String toSql() {
        return switch (operator) {
            case EQUALS,
                 NOT_EQUALS,
                 GREATER_THAN,
                 GREATER_THAN_OR_EQUALS,
                 LESS_THAN,
                 LESS_THAN_OR_EQUALS -> single();

            case IN,
                 NOT_IN -> plural();

            case BETWEEN -> two();

            case IS_NULL,
                 IS_NOT_NULL -> zero();
        };
    }

    private String single() {
        StringBuilder sql = new StringBuilder();
        return sql.append(columnName)
                .append(" ")
                .append(operator.getSql())
                .append(" ?")
                .toString();
    }

    private String plural() {
        StringBuilder sql = new StringBuilder();
        sql.append(columnName)
                .append(" ")
                .append(operator.getSql())
                .append(" (");

         String placeholders = IntStream.range(0, parameterCount)
             .mapToObj(i -> "?")
             .collect(Collectors.joining(", "));

        return sql.append(placeholders)
                .append(")")
                .toString();
    }

    private String zero() {
        StringBuilder sql = new StringBuilder();
        return sql.append(columnName)
                .append(" ")
                .append(operator.getSql())
                .toString();
    }

    private String two() {
        StringBuilder sql = new StringBuilder();
        return sql.append(columnName)
                .append(" ")
                .append(operator.getSql())
                .append(" ?")
                .append(" AND ")
                .append("?")
                .toString();
    }

}
