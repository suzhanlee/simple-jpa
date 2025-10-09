package io.simplejpa.cache;

import lombok.Getter;

@Getter
public enum EntityStatus {
    MANAGED("영속 상태"),
    REMOVED("삭제 예정"),
    DETACHED("준영속 상태");

    private final String description;

    EntityStatus(String description) {
        this.description = description;
    }
}
