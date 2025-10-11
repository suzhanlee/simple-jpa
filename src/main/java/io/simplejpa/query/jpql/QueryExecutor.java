package io.simplejpa.query.jpql;

import io.simplejpa.query.Query;
import io.simplejpa.query.TypedQuery;

import java.util.List;
import java.util.Map;

public interface QueryExecutor {
    <T> List<T> executeQuery(
            String jpql,
            Class<T> resultClass,
            Map<String, Object> namedParameters,
            Map<Integer, Object> positionalParameters
    );

    Query createQuery(String jpql);
    <T> TypedQuery<T> createQuery(String jpql, Class<T> resultClass);
}
