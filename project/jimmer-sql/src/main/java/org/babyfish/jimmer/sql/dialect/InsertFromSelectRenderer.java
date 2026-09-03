package org.babyfish.jimmer.sql.dialect;

/**
 * Renders a native insert/upsert-from-select statement for a dialect.
 */
public interface InsertFromSelectRenderer {

    /**
     * Whether this renderer can preserve all semantics described by {@code ctx}.
     */
    boolean isSupported(InsertFromSelectContext ctx);

    /**
     * Whether the native statement exposes enough change images for precise
     * transaction-trigger events.
     */
    default boolean isTransactionTriggerSupported(InsertFromSelectContext ctx) {
        return false;
    }

    void render(InsertFromSelectContext ctx);
}
