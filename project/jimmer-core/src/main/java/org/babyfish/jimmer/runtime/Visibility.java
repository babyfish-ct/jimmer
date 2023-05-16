package org.babyfish.jimmer.runtime;

import java.util.Arrays;

/**
 * Cheaper than `java.util.BitSet`
 */
public abstract class Visibility {

    public abstract boolean visible(int propId);

    public abstract void show(int propId, boolean visible);

    public static Visibility of(int propCount) {
        int longCount = (propCount + 63) / 64;
        switch (longCount) {
            case 0:
            case 1:
                return new Simple();
            case 2:
                return new Composite2();
            case 3:
                return new Composite3();
            case 4:
                return new Composite4();
            default:
                if (propCount < 0) {
                    throw new IllegalArgumentException("`propCount` cannot be negative number");
                }
                return new CompositeAny(longCount);
        }
    }

    abstract int longCount();

    @Override
    public String toString() {
        int lc = longCount();
        int propId = 0;
        boolean addComma = false;
        StringBuilder builder = new StringBuilder();
        builder.append("Visibility{hiddenPropIds=[");
        for (int i = 0; i < lc; i++) {
            if (!visible(++propId)) {
                if (addComma) {
                    builder.append(',');
                } else {
                    addComma = true;
                }
                builder.append(propId);
            }
        }
        builder.append("]}");
        return builder.toString();
    }

    private static class Simple extends Visibility {

        private long hidden;

        @Override
        int longCount() {
            return 1;
        }

        @Override
        public final boolean visible(int propId) {
            long mask = 1L << (propId - 1);
            return (hidden & mask) == 0;
        }

        @Override
        public final void show(int propId, boolean visible) {
            long mask = 1L << (propId - 1);
            if (visible) {
                hidden &= ~mask;
            } else {
                hidden |= mask;
            }
        }

        @Override
        public int hashCode() {
            return Long.hashCode(hidden);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Simple)) return false;
            Simple simple = (Simple) o;
            return hidden == simple.hidden;
        }
    }

    public static abstract class Composite extends Visibility {

        abstract long hidden(int index);

        abstract void hidden(int index, long value);

        Composite() {
        }

        @Override
        public final boolean visible(int propId) {
            int index = propId - 1;
            int arrIndex = index / 64;
            long mask = 1L << (index - arrIndex * 64);
            return (hidden(arrIndex) & mask) == 0;
        }

        @Override
        public final void show(int propId, boolean visible) {
            int index = propId - 1;
            int arrIndex = index / 64;
            long mask = 1L << (index - arrIndex * 64);
            if (visible) {
                hidden(arrIndex, hidden(arrIndex) & ~mask);
            } else {
                hidden(arrIndex, hidden(arrIndex) | mask);
            }
        }
    }

    private static class Composite2 extends Composite {

        private long hidden0;

        private long hidden1;

        @Override
        int longCount() {
            return 2;
        }

        @Override
        long hidden(int index) {
            switch (index) {
                case 0:
                    return hidden0;
                case 1:
                    return hidden1;
                default:
                    throw new AssertionError("Internal bug");
            }
        }

        @Override
        void hidden(int index, long value) {
            switch (index) {
                case 0:
                    hidden0 = value;
                    break;
                case 1:
                    hidden1 = value;
                    break;
                default:
                    throw new AssertionError("Internal bug");
            }
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new long[] {hidden0, hidden1});
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Composite2)) return false;
            Composite2 composite = (Composite2) o;
            return hidden0 == composite.hidden0 && hidden1 == composite.hidden1;
        }
    }

    private static class Composite3 extends Composite {

        private long hidden0;

        private long hidden1;

        private long hidden2;

        @Override
        int longCount() {
            return 3;
        }

        @Override
        long hidden(int index) {
            switch (index) {
                case 0:
                    return hidden0;
                case 1:
                    return hidden1;
                case 2:
                    return hidden2;
                default:
                    throw new AssertionError("Internal bug");
            }
        }

        @Override
        void hidden(int index, long value) {
            switch (index) {
                case 0:
                    hidden0 = value;
                    break;
                case 1:
                    hidden1 = value;
                    break;
                case 2:
                    hidden2 = value;
                    break;
                default:
                    throw new AssertionError("Internal bug");
            }
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new long[] {hidden0, hidden1, hidden2});
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Composite3)) return false;
            Composite3 composite = (Composite3) o;
            return hidden0 == composite.hidden0 &&
                    hidden1 == composite.hidden1 &&
                    hidden2 == composite.hidden2;
        }
    }

    private static class Composite4 extends Composite {

        private long hidden0;

        private long hidden1;

        private long hidden2;

        private long hidden3;

        @Override
        int longCount() {
            return 4;
        }

        @Override
        long hidden(int index) {
            switch (index) {
                case 0:
                    return hidden0;
                case 1:
                    return hidden1;
                case 2:
                    return hidden2;
                case 3:
                    return hidden3;
                default:
                    throw new AssertionError("Internal bug");
            }
        }

        @Override
        void hidden(int index, long value) {
            switch (index) {
                case 0:
                    hidden0 = value;
                    break;
                case 1:
                    hidden1 = value;
                    break;
                case 2:
                    hidden2 = value;
                    break;
                case 3:
                    hidden3 = value;
                    break;
                default:
                    throw new AssertionError("Internal bug");
            }
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new long[] {hidden0, hidden1, hidden2, hidden3});
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Composite4)) return false;
            Composite4 composite = (Composite4) o;
            return hidden0 == composite.hidden0 &&
                    hidden1 == composite.hidden1 &&
                    hidden2 == composite.hidden2 &&
                    hidden3 == composite.hidden3;
        }
    }

    private static class CompositeAny extends Composite {

        private final long[] hiddenArr;

        public CompositeAny(int longCount) {
            hiddenArr = new long[longCount];
        }

        @Override
        int longCount() {
            return hiddenArr.length;
        }

        @Override
        long hidden(int index) {
            return hiddenArr[index];
        }

        @Override
        void hidden(int index, long value) {
            hiddenArr[index] = value;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hiddenArr);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CompositeAny)) return false;
            CompositeAny composite = (CompositeAny) o;
            return Arrays.equals(hiddenArr, composite.hiddenArr);
        }
    }
}
