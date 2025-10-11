package io.simplejpa.cache;

import io.simplejpa.cache.action.DeleteAction;
import io.simplejpa.cache.action.EntityAction;
import io.simplejpa.cache.action.InsertAction;
import io.simplejpa.cache.action.UpdateAction;
import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.sql.DeleteSqlGenerator;
import io.simplejpa.engine.sql.InsertSqlGenerator;
import io.simplejpa.engine.sql.UpdateSqlGenerator;
import io.simplejpa.metadata.MetadataRegistry;
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

    private final MetadataRegistry metadataRegistry;
    private final InsertSqlGenerator insertSqlGenerator;
    private final UpdateSqlGenerator updateSqlGenerator;
    private final DeleteSqlGenerator deleteSqlGenerator;
    private final JdbcExecutor jdbcExecutor;
    private final EntityPersister entityPersister;
    private final EntityUpdater entityUpdater;
    private final EntityDeleter entityDeleter;

    public ActionQueue(
            MetadataRegistry metadataRegistry,
            InsertSqlGenerator insertSqlGenerator,
            UpdateSqlGenerator updateSqlGenerator,
            DeleteSqlGenerator deleteSqlGenerator,
            JdbcExecutor jdbcExecutor,
            EntityPersister entityPersister,
            EntityUpdater entityUpdater,
            EntityDeleter entityDeleter
    ) {
        this.metadataRegistry = metadataRegistry;
        this.insertSqlGenerator = insertSqlGenerator;
        this.updateSqlGenerator = updateSqlGenerator;
        this.deleteSqlGenerator = deleteSqlGenerator;
        this.jdbcExecutor = jdbcExecutor;
        this.entityPersister = new EntityPersister(jdbcExecutor, insertSqlGenerator, metadataRegistry);
        this.entityUpdater = new EntityUpdater(metadataRegistry, updateSqlGenerator, jdbcExecutor);
        this.entityDeleter = new EntityDeleter(metadataRegistry, deleteSqlGenerator, jdbcExecutor);
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

    private void executeList(List<EntityAction> insertions, Connection connection) {
        for (EntityAction insertion : insertions) {
            insertion.execute(connection);
        }
    }

    public void clear() {
        insertions.clear();
        updates.clear();
        deletions.clear();
    }

}
