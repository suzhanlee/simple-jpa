package io.simplejpa.cache;

import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.MetadataRegistry;

import java.sql.Connection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class PersistenceContext {
    private final Map<EntityKey, Object> firstLevelCache = new HashMap<>();
    private final Map<Object, EntityEntry> entityEntries = new IdentityHashMap<>(); // 객체 동일성 비교 필요
    private final ActionQueue actionQueue;
    private final MetadataRegistry metadataRegistry;

    public PersistenceContext(ActionQueue actionQueue, MetadataRegistry metadataRegistry) {
        this.actionQueue = actionQueue;
        this.metadataRegistry = metadataRegistry;
    }

    public void addEntity(Object entity) {
        addFirstCacheAndSnapShot(entity);
        actionQueue.addInsertion(entity);
    }

    private void addFirstCacheAndSnapShot(Object entity) {
        Class<?> entityClass = entity.getClass();
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        Object idValue = metadata.getIdentifierMetadata().getValue(entity);
        firstLevelCache.put(new EntityKey(entityClass, idValue), entity);
        entityEntries.put(entity, createEntityEntry(entity, metadata));
    }

    private EntityEntry createEntityEntry(Object entity, EntityMetadata metadata) {
        List<AttributeMetadata> metadataAttributeMetadatas = metadata.getAttributeMetadatas();
        Object[] stateSnapShot = new Object[metadataAttributeMetadatas.size()];
        for (int i = 0; i < metadataAttributeMetadatas.size(); i++) {
            stateSnapShot[i] = metadataAttributeMetadatas.get(i).getValue(entity);
        }
        return new EntityEntry(entity, stateSnapShot);
    }

    public <T> T getEntity(Class<T> entityClass, Object id) {
        return (T) firstLevelCache.get(new EntityKey(entityClass, id));
    }

    public boolean contains(Object entity) {
        return entityEntries.containsKey(entity);
    }

    public void removeEntity(Object entity) {
        EntityEntry entry = entityEntries.get(entity);
        if (entry != null && entry.isManaged()) {
            entry.markAsRemoved();
            actionQueue.addDeletion(entity);
        }
    }

    public void flush(Connection connection) {
        detectDirtyEntities();
        actionQueue.executeActions(connection);
        // flush 하더라도 1차 캐시는 유지된다.
    }

    private void detectDirtyEntities() {
        entityEntries.forEach(this::detectDirtyEntity);
    }

    private void detectDirtyEntity(Object entity, EntityEntry entityEntry) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());
        if (entityEntry.isManaged() && entityEntry.isModified(metadata)) {
            actionQueue.addUpdate(entity, entityEntry);
        }
    }

    public void clear() {
        firstLevelCache.clear();
        entityEntries.clear();
        actionQueue.clear();
    }

}
