package org.babyfish.jimmer.sql.ast.query;

public enum NullOrderMode {
    UNSPECIFIED,
    NULLS_FIRST,
    NULLS_LAST;

    /**
     * Transform {@link org.babyfish.jimmer.meta.NullOrderMode} to {@link NullOrderMode}
     */
    public static NullOrderMode transformNullOrderMode(org.babyfish.jimmer.meta.NullOrderMode metaNullOrderMode) {
        if (metaNullOrderMode == null) {
            return null;
        }
        switch (metaNullOrderMode) {
            case UNSPECIFIED: return UNSPECIFIED;
            case NULLS_FIRST: return NULLS_FIRST;
            case NULLS_LAST: return NULLS_LAST;
        }
        throw new IllegalArgumentException("Unknown NullOrderMode: " + metaNullOrderMode);
    }
}
