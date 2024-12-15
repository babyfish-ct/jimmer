package org.babyfish.jimmer.sql.dialect;

public class TiDBDialect extends MySqlStyleDialect {

    @Override
    public boolean isForeignKeySupported() {
        return false;
    }
}
