package io.simplejpa.metadata.relation;

import io.simplejpa.annotation.CascadeType;
import io.simplejpa.annotation.FetchType;

public class RelationShipMetadata {
    private RelationType relationType;
    private Class<?> targetEntityClass;
    private String fieldName;
    private FetchType fetchType;
    private CascadeType[] cascadeTypes;
    private String foreignKeyColumn;
    private String mappedBy;
    private JoinTableMetadata joinTableMetadata;
}
