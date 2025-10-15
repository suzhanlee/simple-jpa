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

    public RelationShipMetadata(
            RelationType relationType,
            Class<?> targetEntityClass,
            String fieldName,
            FetchType fetchType,
            CascadeType[] cascadeTypes,
            String foreignKeyColumn,
            String mappedBy,
            JoinTableMetadata joinTableMetadata
    ) {
        this.relationType = relationType;
        this.targetEntityClass = targetEntityClass;
        this.fieldName = fieldName;
        this.fetchType = fetchType;
        this.cascadeTypes = cascadeTypes;
        this.foreignKeyColumn = foreignKeyColumn;
        this.mappedBy = mappedBy;
        this.joinTableMetadata = joinTableMetadata;
    }
}
