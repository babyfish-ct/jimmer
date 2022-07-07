package org.babyfish.jimmer.sql.event;

interface MiddleTableListener {

    void delete(Object sourceId, Object targetId);

    void insert(Object sourceId, Object targetId);
}
