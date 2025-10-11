package io.simplejpa.cache.action;

import io.simplejpa.persister.EntityDeleter;

import java.sql.Connection;

public class DeleteAction implements EntityAction {
    private final Object entity;
    private final EntityDeleter entityDeleter;

    public DeleteAction(
            Object entity,
            EntityDeleter entityDeleter
    ) {
        this.entity = entity;
        this.entityDeleter = entityDeleter;
    }

    @Override
    public void execute(Connection connection) {
        entityDeleter.delete(connection, entity);
    }

    @Override
    public Object getEntity() {
        return entity;
    }
}
