package org.babyfish.jimmer.quarkus.runtime;

import org.babyfish.jimmer.sql.dialect.*;

import java.util.function.Supplier;

public enum DBKindEnum {

    PG("postgresql", PostgresDialect::new),
    H2("h2", H2Dialect::new),
    DB2("db2", () -> DefaultDialect.INSTANCE),
    DERBY("derby", () -> DefaultDialect.INSTANCE),
    MARIADB("mariadb", () -> DefaultDialect.INSTANCE),
    MSSQL("mssql", () -> DefaultDialect.INSTANCE),
    MYSQL("mysql", MySqlDialect::new),
    ORACLE("oracle", OracleDialect::new),
    TIDB("tidb", TiDBDialect::new);

    private String dbKInd;

    private Supplier<Dialect> dialectSupplier;

    public String getDbKInd() {
        return dbKInd;
    }

    public void setDbKInd(String dbKInd) {
        this.dbKInd = dbKInd;
    }

    public Supplier<Dialect> getDialectSupplier() {
        return dialectSupplier;
    }

    public void setDialectSupplier(Supplier<Dialect> dialectSupplier) {
        this.dialectSupplier = dialectSupplier;
    }

    DBKindEnum(String dbKInd, Supplier<Dialect> dialectSupplier) {
        this.dbKInd = dbKInd;
        this.dialectSupplier = dialectSupplier;
    }

    public static Dialect selectDialect(String dbKInd) {
        for (DBKindEnum value : values()) {
            if (value.getDbKInd().equals(dbKInd)) {
                return value.dialectSupplier.get();
            }
        }
        return DefaultDialect.INSTANCE;
    }
}
