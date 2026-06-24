package org.babyfish.jimmer.sql.event.binlog;

import org.babyfish.jimmer.jackson.codec.Node;

/**
 * Accepts row-change events from an external CDC/binlog/message-queue integration
 * and converts them into Jimmer trigger events.
 */
public interface BinLog {

    /**
     * Accept a row-change event without a custom reason.
     *
     * @param tableName The changed table name. See
     * {@link #accept(String, Node, Node, String)} for the name resolution contract.
     * @param oldData The old row data, or {@code null} for insert events
     * @param newData The new row data, or {@code null} for delete events
     */
    void accept(String tableName, Node oldData, Node newData);

    /**
     * Accept a row-change event.
     *
     * <p>The {@code tableName} can be either unqualified, such as {@code BOOK}, or
     * database/schema-qualified, such as {@code PUBLIC.BOOK}. Qualified names are
     * resolved exactly first. If no exact managed table is found, Jimmer falls back
     * to suffix lookup, so {@code PUBLIC.BOOK} can still match an entity mapped to
     * {@code BOOK}.</p>
     *
     * <p>For CDC streams that include several databases/schemas with the same table
     * names, pass the database/schema-qualified table name and map the corresponding
     * entities to qualified table names as well. Otherwise an unqualified fallback
     * cannot distinguish which physical table produced the event.</p>
     *
     * <ul>
     *     <li>For MySQL, use the database name as the qualifying prefix.</li>
     *     <li>For PostgreSQL, use the schema name as the qualifying prefix.</li>
     * </ul>
     *
     * @param tableName The changed table name
     * @param oldData The old row data, or {@code null} for insert events
     * @param newData The new row data, or {@code null} for delete events
     * @param reason Optional reason propagated to trigger events
     */
    void accept(String tableName, Node oldData, Node newData, String reason);
}
