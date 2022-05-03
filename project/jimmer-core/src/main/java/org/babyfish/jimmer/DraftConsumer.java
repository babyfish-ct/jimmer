package org.babyfish.jimmer;

@FunctionalInterface
public interface DraftConsumer<D extends Draft> {

    void accept(D draft) throws Throwable;
}
