package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGobject;

public class MacAddressScalarProvider implements ScalarProvider<String, PGobject> {

    @Override
    public String toScalar(@NotNull PGobject sqlValue) throws Exception {
        return sqlValue.getValue();
    }

    @Override
    public PGobject toSql(@NotNull String scalarValue) throws Exception {
        PGobject po = new PGobject();
        po.setType("macaddr");
        po.setValue(scalarValue);
        return po;
    }
}
