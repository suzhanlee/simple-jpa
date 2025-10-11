package io.simplejpa.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypedQueryImpl<T> implements TypedQuery<T> {
    private final String jpql;
    private final Map<String, Object> namedParameters;
    private final Map<Integer, Object> positionalParameters;

    public TypedQueryImpl(String jpql) {
        this.jpql = jpql;
        this.namedParameters = new HashMap<>();
        this.positionalParameters = new HashMap<>();
    }

    @Override
    public List<T> getResultList() {
        return List.of();
    }

    @Override
    public T getSingleResult() {
        return null;
    }

    @Override
    public void setParameter(String name, Object value) {
        namedParameters.put(name, value);
    }

    @Override
    public void setParameter(int position, Object value) {
        positionalParameters.put(position, value);
    }
}
