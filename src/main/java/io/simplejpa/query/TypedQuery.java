package io.simplejpa.query;

import java.util.List;

public interface TypedQuery<T> {
    List<T> getResultList();

    T getSingleResult();

    TypedQuery<T> setParameter(String name, Object value); // named

    TypedQuery<T> setParameter(int position, Object value); // positional
}
