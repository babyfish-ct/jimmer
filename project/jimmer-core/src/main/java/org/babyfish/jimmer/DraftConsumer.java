package org.babyfish.jimmer;

@FunctionalInterface
public interface DraftConsumer<D> {

    void accept(D draft) throws Throwable;
}
