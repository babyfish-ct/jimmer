package org.babyfish.jimmer.meta;

public enum EmbeddedLevel {

    SCALAR(true, false),
    REFERENCE(false, true),
    BOTH(true, true);

    private final boolean hasScalar;

    private final boolean hasReference;

    EmbeddedLevel(boolean hasScalar, boolean hasReference) {
        this.hasScalar = hasScalar;
        this.hasReference = hasReference;
    }

    public boolean hasScalar() {
        return hasScalar;
    }

    public boolean hasReference() {
        return hasReference;
    }
}
