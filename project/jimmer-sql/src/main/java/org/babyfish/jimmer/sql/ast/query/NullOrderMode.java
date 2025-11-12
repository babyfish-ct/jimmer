package org.babyfish.jimmer.sql.ast.query;

/**
 * This type has been moved into {@code jimmer-core},
 * its new name is {@link org.babyfish.jimmer.meta.NullOrderMode}
 */
@Deprecated
public class NullOrderMode {

    private NullOrderMode() {}

    public static org.babyfish.jimmer.meta.NullOrderMode UNSPECIFIED =
            org.babyfish.jimmer.meta.NullOrderMode.UNSPECIFIED;

    public static org.babyfish.jimmer.meta.NullOrderMode NULLS_FIRST =
            org.babyfish.jimmer.meta.NullOrderMode.NULLS_FIRST;

    public static org.babyfish.jimmer.meta.NullOrderMode NULLS_LAST =
            org.babyfish.jimmer.meta.NullOrderMode.NULLS_LAST;
}
