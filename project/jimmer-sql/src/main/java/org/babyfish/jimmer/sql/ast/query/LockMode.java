package org.babyfish.jimmer.sql.ast.query;

public enum LockMode {

    /**
     * <ul>
     *     <li>SQLServer: WITH (UPDLOCK)</li>
     *     <li>Other database: FOR UPDATE</li>
     * </ul>
     */
    UPDATE(false, false),

    /**
     * <ul>
     *     <li>Postgres: FOR SHARE</li>
     *     <li>MySQL: LOCK IN SHARE MODE</li>
     * </ul>
     */
    SHARE(true, false),

    /**
     * Postgres only, {@code for no key update}
     */
    PG_NO_KEY_UPDATE(false, true),

    /**
     * Postgres only, {@code for key share}
     */
    PG_KEY_SHARE(true, true);

    private final boolean shared;

    private final boolean postgresOnly;

    LockMode(boolean shared, boolean postgresOnly) {
        this.shared = shared;
        this.postgresOnly = postgresOnly;
    }

    public boolean isShared() {
        return shared;
    }

    public boolean isPostgresOnly() {
        return postgresOnly;
    }
}
