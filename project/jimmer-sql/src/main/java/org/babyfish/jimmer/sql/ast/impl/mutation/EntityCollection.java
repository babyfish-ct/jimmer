package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.PropId;

import java.util.Collection;

public interface EntityCollection<E> extends Collection<E> {

    Iterable<Item<E>> items();

    interface Item<E> {
        E getEntity();
        Iterable<E> getOriginalEntities();
    }

    static <E> EntityCollection<E> of(PropId[] propIds) {
        if (propIds.length == 0) {
            return new EntityList<>();
        }
        return new EntitySet<>(propIds);
    }
}
