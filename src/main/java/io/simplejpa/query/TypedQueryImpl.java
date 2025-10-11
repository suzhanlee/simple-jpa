package io.simplejpa.query;

import io.simplejpa.query.jpql.QueryExecutor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class TypedQueryImpl<T> implements TypedQuery<T> {
    private final String jpql;
    private final QueryExecutor queryExecutor;
    private final Class<T> resultClass;
    private final Map<String, Object> namedParameters;
    private final Map<Integer, Object> positionalParameters;

    public TypedQueryImpl(String jpql, QueryExecutor queryExecutor, Class<T> resultClass) {
        this.jpql = jpql;
        this.queryExecutor = queryExecutor;
        this.resultClass = resultClass;
        this.namedParameters = new HashMap<>();
        this.positionalParameters = new HashMap<>();
    }

    @Override
    public List<T> getResultList() {
        return queryExecutor.executeQuery(jpql, resultClass, namedParameters, positionalParameters);
    }

    @Override
    public T getSingleResult() {
        List<T> results = getResultList();

        if (results.isEmpty()) {
            throw new RuntimeException("No result found");
        }

        if (results.size() > 1) {
            throw new RuntimeException("Non-unique result");
        }

        return results.get(0);
    }

    @Override
    public TypedQuery<T> setParameter(String name, Object value) {
        namedParameters.put(name, value);
        return this;
    }

    @Override
    public TypedQuery<T> setParameter(int position, Object value) {
        positionalParameters.put(position, value);
        return this;
    }
}
