package io.simplejpa.cache;

import java.util.Objects;

public class EntityKey {
    private final Class<?> entityClass;
    private final Object id;

    public EntityKey(Class<?> entityClass, Object id) {
        validateEntityKey(entityClass, id);
        this.entityClass = entityClass;
        this.id = id;
    }

    private void validateEntityKey(Class<?> entityClass, Object id) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("Entity id cannot be null");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityKey entityKey = (EntityKey) o;
        return Objects.equals(entityClass, entityKey.entityClass) &&
                Objects.equals(id, entityKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, id);
    }

    @Override
    public String toString() {
        return "EntityKey{" +
                "entityClass=" + entityClass +
                ", id=" + id +
                '}';
    }
}
