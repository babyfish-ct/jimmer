package org.babyfish.jimmer.sql.fetcher;

import org.jetbrains.annotations.NotNull;

public interface RecursionStrategy<E> {

    boolean isRecursive(Args<E> args);

    class Args<E> {

        private final E entity;

        private final int depth;

        public Args(E entity, int depth) {
            this.entity = entity;
            this.depth = depth;
        }

        @NotNull
        public E getEntity() {
            return entity;
        }

        public int getDepth() {
            return depth;
        }

        @Override
        public String toString() {
            return "Args{" +
                    "entity=" + entity +
                    ", depth=" + depth +
                    '}';
        }
    }
}
