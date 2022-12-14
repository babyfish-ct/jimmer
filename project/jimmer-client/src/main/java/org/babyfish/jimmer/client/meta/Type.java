package org.babyfish.jimmer.client.meta;

public interface Type extends Node {

    default boolean hasDefinition() {
        return false;
    }
}
