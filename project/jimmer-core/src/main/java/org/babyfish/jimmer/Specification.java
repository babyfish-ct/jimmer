package org.babyfish.jimmer;

import org.babyfish.jimmer.client.ApiIgnore;

@ApiIgnore
public interface Specification<E> {

    Class<E> entityType();
}
