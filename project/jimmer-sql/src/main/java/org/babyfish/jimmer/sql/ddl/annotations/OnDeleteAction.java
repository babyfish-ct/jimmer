package org.babyfish.jimmer.sql.ddl.annotations;

/**
 *
 */
public enum OnDeleteAction {
    NONE(""),
    CASCADE("cascade"),
    RESTRICT("restrict"),
    SET_NULL("set null"),
    SET_DEFAULT("set default");

    public final String sql;

    OnDeleteAction(String sql) {
        this.sql = sql;
    }

}
