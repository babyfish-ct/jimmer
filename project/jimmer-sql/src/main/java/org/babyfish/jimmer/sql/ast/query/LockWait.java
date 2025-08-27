package org.babyfish.jimmer.sql.ast.query;

import java.util.function.IntSupplier;

public abstract class LockWait {

    public static final LockWait DEFAULT = new LockWait() {
        @Override
        public String toString() {
            return "LockWait.DEFAULT";
        }
    };

    public static final LockWait NO_WAIT = new LockWait() {

        @Override
        public String toString() {
            return "LockWait.NO_WAIT";
        }
    };

    public static final LockWait SKIP_LOCKED = new LockWait() {
        @Override
        public String toString() {
            return "LockWait.SKIP_LOCKED";
        }
    };

    LockWait() {}

    public static LockWait seconds(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("The wait seconds cannot be negative");
        }
        if (value == 0) {
            return NO_WAIT;
        }
        return new BySeconds(value);
    }

    private static class BySeconds extends LockWait implements IntSupplier {

        final int value;

        private BySeconds(int value) {
            this.value = value;
        }

        @Override
        public int getAsInt() {
            return value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof BySeconds)) {
                return false;
            }
            BySeconds bySeconds = (BySeconds) o;
            return value == bySeconds.value;
        }

        @Override
        public String toString() {
            return "LockWait.seconds(" + value + ')';
        }
    }
}
