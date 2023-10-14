package org.babyfish.jimmer.sql.fetcher;

public enum IdOnlyReferenceType {

    /**
     * When fetch one-to-one/many-to-one association mapped by foreign key directly,
     * if there are some filters can affect the associated type(include built-in logical deleted filter),
     * apply them.
     */
    DEFAULT,

    /**
     * When fetch one-to-one/many-to-one association mapped by foreign key directly,
     * if there are some filters can affect the associated type(include built-in logical deleted filter),
     * ignore them.
     */
    RAW
}
