package io.simplejpa.metadata;

import io.simplejpa.mapping.AnnotationProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataRegistry {
    private final Map<Class<?>, EntityMetadata> metadataCache;
    private final AnnotationProcessor annotationProcessor;

    public MetadataRegistry() {
        this.metadataCache = new ConcurrentHashMap<>();
        this.annotationProcessor = new AnnotationProcessor();
    }

    MetadataRegistry(
            Map<Class<?>, EntityMetadata> metadataCache,
            AnnotationProcessor annotationProcessor
    ) {
        this.metadataCache = metadataCache;
        this.annotationProcessor = annotationProcessor;
    }

    public EntityMetadata getMetadata(Class<?> entityClass) {
        EntityMetadata metadata = metadataCache.get(entityClass);
        if (metadata == null) {
            throw new IllegalArgumentException(
                    "No metadata found for " + entityClass.getName()
            );
        }
        return metadata;
    }

    public void register(Class<?> entityClass, EntityMetadata entityMetadata) {
        metadataCache.put(entityClass, entityMetadata);
    }

    public void scanAndRegister(Class<?> entityClass) {
        if (!hasMetadata(entityClass)) {
            register(entityClass, annotationProcessor.processEntity(entityClass));
        }
    }

    public boolean hasMetadata(Class<?> entityClass) {
        return metadataCache.containsKey(entityClass);
    }
}
