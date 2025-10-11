package io.simplejpa.query;

import java.util.List;

public interface TypedQuery<T> {
    List<T> getResultList();

    T getSingleResult();

    void setParameter(String name, Object value); // named

    void setParameter(int position, Object value); // positional
}
