package org.babyfish.jimmer.sql.event;

@FunctionalInterface
public interface EntityListener<E> {

    void onChange(EntityEvent<E> e);
}
