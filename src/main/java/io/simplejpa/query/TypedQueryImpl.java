package io.simplejpa.query;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
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
