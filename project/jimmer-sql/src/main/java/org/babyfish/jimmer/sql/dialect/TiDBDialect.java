package org.babyfish.jimmer.sql.dialect;

public class TiDBDialect extends MySqlDialect {

    @Override
    public boolean isForeignKeySupported() {
        return false;
    }
}
