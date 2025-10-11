package io.simplejpa.cache;

import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import io.simplejpa.metadata.MetadataRegistry;
import lombok.Getter;

import java.sql.Connection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Getter
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
        validateRemovable(entry);

        entry.markAsRemoved();
        actionQueue.addDeletion(entity);
    }

    private void validateRemovable(EntityEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entity is not managed by the persistence context");
        }

        if (!entry.isManaged()) {
            throw new IllegalStateException("Entity is in detached state and cannot be removed");
        }

        if (entry.isRemoved()) {
            throw new IllegalStateException("Entity is already removed");
        }
    }

    public void flush(Connection connection) {
        detectDirtyEntities();
        actionQueue.executeActions(connection);
        removeEntityFromFirstCacheAndEntries();
    }

    private void removeEntityFromFirstCacheAndEntries() {
        for (Object removedEntity : findRemovedEntities()) {
            EntityMetadata metadata = metadataRegistry.getMetadata(removedEntity.getClass());
            Object idValue = metadata.getIdentifierMetadata().getValue(removedEntity);
            firstLevelCache.remove(new EntityKey(removedEntity.getClass(), idValue));
            entityEntries.remove(removedEntity);
        }
    }

    private List<Object> findRemovedEntities() {
        return entityEntries.entrySet().stream()
                .filter(entry -> entry.getValue().isRemoved())
                .map(Map.Entry::getKey)
                .toList();
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
