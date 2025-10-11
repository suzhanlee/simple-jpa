package io.simplejpa.cache.action;

import io.simplejpa.persister.EntityPersister;

import java.sql.Connection;

public class InsertAction implements EntityAction {
    private final Object entity;
    private final EntityPersister entityPersister;

    public InsertAction(
            Object entity,
            EntityPersister entityPersister
    ) {
        this.entity = entity;
        this.entityPersister = entityPersister;
    }

    @Override
    public void execute(Connection connection) {
        entityPersister.insert(connection, entity);
    }

    @Override
    public Object getEntity() {
        return entity;
    }
}
