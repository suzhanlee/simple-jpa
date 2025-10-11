package io.simplejpa.query;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class QueryImpl implements Query {
    private final String jpql;
    private final Map<String, Object> namedParameters;
    private final Map<Integer, Object> positionalParameters;

    public QueryImpl(String jpql) {
        this.jpql = jpql;
        this.namedParameters = new HashMap<>();
        this.positionalParameters = new HashMap<>();
    }

    @Override
    public List<Object> getResultList() {
        return List.of();
    }

    @Override
    public Object getSingleResult() {
        return null;
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
