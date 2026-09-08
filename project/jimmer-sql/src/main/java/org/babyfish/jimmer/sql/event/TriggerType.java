package org.babyfish.jimmer.sql.event;

public enum TriggerType {
    BINLOG_ONLY,

    /**
     * The original intention of the transaction trigger was to facilitate users who were
     * too lazy to even set up a binlog infrastructure.
     *
     * <p>However, as mutations (DML, save commands) have evolved to become increasingly
     * advanced, transaction triggers have had a growing impact on software architecture
     * and an ever-increasing impact on performance. Therefore:
     *
     * <ul>
     *   <li>Transaction triggers will first be marked as {@code @Deprecated}.
     *   <li>More advanced mutation features may check the {@code TriggerType} and, if it
     *       is not {@link #BINLOG_ONLY}, directly throw an exception to explicitly indicate
     *       that it is not supported.
     * </ul>
     *
     * <p>Users are requested to gradually stop relying on transaction triggers.
     *
     * @deprecated Transaction triggers are deprecated due to architectural and performance
     *             concerns. Please migrate away from relying on them.
     */
    @Deprecated
    TRANSACTION_ONLY,

    /**
     * The original intention of the transaction trigger was to facilitate users who were
     * too lazy to even set up a binlog infrastructure.
     *
     * <p>However, as mutations (DML, save commands) have evolved to become increasingly
     * advanced, transaction triggers have had a growing impact on software architecture
     * and an ever-increasing impact on performance. Therefore:
     *
     * <ul>
     *   <li>Transaction triggers will first be marked as {@code @Deprecated}.
     *   <li>More advanced mutation features may check the {@code TriggerType} and, if it
     *       is not {@code BINLOG_ONLY}, directly throw an exception to explicitly indicate
     *       that it is not supported.
     * </ul>
     *
     * <p>Users are requested to gradually stop relying on transaction triggers.
     *
     * @deprecated Transaction triggers are deprecated due to architectural and performance
     *             concerns. Please migrate away from relying on them.
     */
    @Deprecated
    BOTH
}
