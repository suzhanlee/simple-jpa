package io.simplejpa.query;

import java.util.List;

public interface Query {
    List<Object> getResultList();

    Object getSingleResult();

    void setParameter(String name, Object value); // named

    void setParameter(int position, Object value); // positional
}
