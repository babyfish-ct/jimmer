package org.babyfish.jimmer.meta;

public abstract class PropId {

    PropId() {}

    public static PropId byIndex(int index) {
        if (index < Index.CACHED_INDICES.length) {
            return Index.CACHED_INDICES[index];
        }
        return new Index(index);
    }

    public static PropId byName(String name) {
        return new Name(name);
    }

    public abstract int asIndex();

    public abstract String asName();

    private static final class Index extends PropId {

        final static Index[] CACHED_INDICES;

        final int value;

        Index(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("value can not be less than 0");
            }
            this.value = value;
        }

        @Override
        public int asIndex() {
            return value;
        }

        @Override
        public String asName() {
            return null;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Index other = (Index) o;
            return value == other.value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        static {
            Index[] cachedIndices = new Index[512];
            for (int i = 0; i < cachedIndices.length; i++) {
                cachedIndices[i] = new Index(i);
            }
            CACHED_INDICES = cachedIndices;
        }
    }

    private static final class Name extends PropId {

        final String value;

        Name(String value) {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("value can not be null or empty");
            }
            this.value = value;
        }

        @Override
        public int asIndex() {
            return -1;
        }

        @Override
        public String asName() {
            return value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Name other = (Name) o;
            return value.equals(other.value);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
