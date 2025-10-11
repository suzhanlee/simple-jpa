package io.simplejpa.cache;

import io.simplejpa.metadata.AttributeMetadata;
import io.simplejpa.metadata.EntityMetadata;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class EntityEntry {
    private final Object entity;
    private final Object[] stateSnapShot;
    private EntityStatus status;

    public EntityEntry(
            Object entity,
            Object[] stateSnapShot
    ) {
        this.entity = entity;
        this.stateSnapShot = stateSnapShot;
        this.status = EntityStatus.MANAGED;
    }

    public boolean isModified(EntityMetadata metadata) {
        if (!isManaged()) {
            return false;
        }
        Object[] currentState = extractCurrentState(metadata);
        return !Arrays.equals(stateSnapShot, currentState);
    }

    private Object[] extractCurrentState(EntityMetadata metadata) {
        List<AttributeMetadata> attributeMetadatas = metadata.getAttributeMetadatas();
        Object[] state = new Object[attributeMetadatas.size()];
        for (int i = 0; i < attributeMetadatas.size(); i++) {
            state[i] = attributeMetadatas.get(i).getValue(entity);
        }
        return state;
    }

    public void updateSnapShot(Object[] newSnapshot) {
        if (newSnapshot.length != this.stateSnapShot.length) {
            throw new IllegalArgumentException("Snapshot size mismatch");
        }
        System.arraycopy(newSnapshot, 0, this.stateSnapShot, 0, newSnapshot.length);
    }

    public void markAsRemoved() {
        this.status = EntityStatus.REMOVED;
    }

    public void markAsDetached() {
        this.status = EntityStatus.DETACHED;
    }

    public boolean isManaged() {
        return this.status == EntityStatus.MANAGED;
    }

    public boolean isRemoved() {
        return this.status == EntityStatus.REMOVED;
    }

    public boolean isDetached() {
        return this.status == EntityStatus.DETACHED;
    }
}
