package io.simplejpa.metadata.relation;

public record JoinTableMetadata(
        String tableName,
        String[] joinColumns,
        String[] inverseJoinColumns
) {

}
