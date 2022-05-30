package org.babyfish.jimmer.sql.fetcher;

public interface RecursionStrategy<E> {

    boolean isRecursive(Args<E> args);

    class Args<E> {

        private E entity;

        private int depth;

        public Args(E entity, int depth) {
            this.entity = entity;
            this.depth = depth;
        }

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
