package io.simplejpa.cache.action;

import io.simplejpa.cache.EntityEntry;
import io.simplejpa.engine.sql.SqlWithParameters;
import io.simplejpa.persister.EntityUpdater;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

@Slf4j
public class UpdateAction implements EntityAction {
    private final Object entity;
    private final EntityEntry entityEntry;
    private final EntityUpdater entityUpdater;

    public UpdateAction(
            Object entity,
            EntityEntry entityEntry,
            EntityUpdater entityUpdater
    ) {
        this.entity = entity;
        this.entityEntry = entityEntry;
        this.entityUpdater = entityUpdater;
    }

    @Override
    public void execute(Connection connection) {
        entityUpdater.update(connection, entity, entityEntry);
        Object[] updateValues = entityUpdater.extractUpdateValues(entity);
        entityEntry.updateSnapShot(updateValues);
    }

    @Override
    public Object getEntity() {
        return entity;
    }
}
