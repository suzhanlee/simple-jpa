package io.simplejpa.query;

import io.simplejpa.query.jpql.QueryExecutor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class QueryImpl implements Query {
    private final String jpql;
    private final QueryExecutor queryExecutor;
    private final Map<String, Object> namedParameters;
    private final Map<Integer, Object> positionalParameters;

    public QueryImpl(String jpql, QueryExecutor queryExecutor) {
        this.jpql = jpql;
        this.queryExecutor = queryExecutor;
        this.namedParameters = new HashMap<>();
        this.positionalParameters = new HashMap<>();
    }

    @Override
    public List<Object> getResultList() {
        return queryExecutor.executeQuery(jpql, Object.class, namedParameters, positionalParameters);
    }

    @Override
    public Object getSingleResult() {
        List<Object> results = getResultList();

        if (results.isEmpty()) {
            throw new RuntimeException("No result found");
        }

        if (results.size() > 1) {
            throw new RuntimeException("Non-unique result");
        }

        return results.get(0);
    }

    @Override
    public Query setParameter(String name, Object value) {
        namedParameters.put(name, value);
        return this;
    }

    @Override
    public Query setParameter(int position, Object value) {
        positionalParameters.put(position, value);
        return this;
    }
}
