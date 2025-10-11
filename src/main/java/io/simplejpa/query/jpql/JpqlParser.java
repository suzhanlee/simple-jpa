package io.simplejpa.query.jpql;

import io.simplejpa.query.jpql.ast.Condition;
import io.simplejpa.query.jpql.ast.SelectStatement;
import io.simplejpa.query.jpql.ast.WhereClause;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class JpqlParser {
    private static final String WHITE_SPACE_REGEX = "\\s+";
    private static final int DEFAULT_INDEX = -1;
    private static final JpqlParser INSTANCE = new JpqlParser();

    public SelectStatement parse(String jpql) {
        String[] tokens = jpql.trim().split(WHITE_SPACE_REGEX);
        String alias = tokens[1];
        String entityName = tokens[3];

        List<Condition> conditions = extractConditions(tokens);
        WhereClause whereClause = new WhereClause(conditions);

        return new SelectStatement(alias, entityName, whereClause);
    }

    private List<Condition> extractConditions(String[] tokens) {
        int whereIndex = findWhereClauseIndex(tokens);
        if (whereIndex == DEFAULT_INDEX) {
            return new ArrayList<>();
        }

        List<Condition> conditions = new ArrayList<>();

        for (int i = whereIndex + 1; i < tokens.length; i++) {
            if (isOperator(tokens[i])) {
                String leftSide = tokens[i - 1];
                String operator = tokens[i];
                String rightSide = tokens[i + 1];
                conditions.add(new Condition(leftSide, operator, rightSide));
                i++;
            }
        }

        return conditions;
    }

    private int findWhereClauseIndex(String[] tokens) {
        return IntStream.range(0, tokens.length)
                .filter(i -> tokens[i].equalsIgnoreCase("where"))
                .findFirst()
                .orElse(DEFAULT_INDEX);
    }

    private boolean isOperator(String token) {
        return token.equals("=") || token.equals(">") || token.equals("<");
    }
}
