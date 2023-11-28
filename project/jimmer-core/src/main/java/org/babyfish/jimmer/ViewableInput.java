package org.babyfish.jimmer;

import org.babyfish.jimmer.client.ApiIgnore;

@ApiIgnore
public interface ViewableInput<E> extends View<E>, Input<E> {
}
