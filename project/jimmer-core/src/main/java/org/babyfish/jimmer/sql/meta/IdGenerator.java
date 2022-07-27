package org.babyfish.jimmer.sql.meta;

public interface IdGenerator {

    class None implements IdGenerator {
        private None() {}
    }
}
