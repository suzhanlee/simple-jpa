package io.simplejpa.cache;

import io.simplejpa.cache.action.DeleteAction;
import io.simplejpa.cache.action.EntityAction;
import io.simplejpa.cache.action.InsertAction;
import io.simplejpa.cache.action.UpdateAction;
import io.simplejpa.persister.EntityDeleter;
import io.simplejpa.persister.EntityPersister;
import io.simplejpa.persister.EntityUpdater;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ActionQueue {
    private final List<EntityAction> insertions = new ArrayList<>();
    private final List<EntityAction> updates = new ArrayList<>();
    private final List<EntityAction> deletions = new ArrayList<>();

    private final EntityPersister entityPersister;
    private final EntityUpdater entityUpdater;
    private final EntityDeleter entityDeleter;

    public ActionQueue(
            EntityPersister entityPersister,
            EntityUpdater entityUpdater,
            EntityDeleter entityDeleter
    ) {
        this.entityPersister = entityPersister;
        this.entityUpdater = entityUpdater;
        this.entityDeleter = entityDeleter;
    }

    public void addInsertion(Object entity) {
        insertions.add(new InsertAction(entity, entityPersister));
    }

    public void addUpdate(Object entity, EntityEntry entityEntry) {
        updates.add(new UpdateAction(entity, entityEntry, entityUpdater));
    }

    public void addDeletion(Object entity) {
        deletions.add(new DeleteAction(entity, entityDeleter));
    }

    public void executeActions(Connection connection) {
        executeList(insertions, connection);
        executeList(updates, connection);
        executeList(deletions, connection);
        clear();
    }

    private void executeList(List<EntityAction> entityActions, Connection connection) {
        for (EntityAction action : entityActions) {
            action.execute(connection);
        }
    }

    public void clear() {
        insertions.clear();
        updates.clear();
        deletions.clear();
    }

    public boolean hasDeleteActions() {
        return !this.deletions.isEmpty();
    }

    public void clearDeleteActions() {
        this.deletions.clear();
    }

    public List<EntityAction> getDeleteActions() {
        return this.deletions;
    }
}
