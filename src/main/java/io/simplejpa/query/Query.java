package io.simplejpa.query;

import java.util.List;

public interface Query {
    List<Object> getResultList();

    Object getSingleResult();

    Query setParameter(String name, Object value); // named

    Query setParameter(int position, Object value); // positional
}
