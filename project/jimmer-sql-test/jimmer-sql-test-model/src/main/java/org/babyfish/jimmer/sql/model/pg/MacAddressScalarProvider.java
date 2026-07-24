package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jspecify.annotations.NonNull;
import org.postgresql.util.PGobject;

public class MacAddressScalarProvider implements ScalarProvider<String, PGobject> {

    @Override
    public String toScalar(@NonNull PGobject sqlValue) throws Exception {
        return sqlValue.getValue();
    }

    @Override
    public PGobject toSql(@NonNull String scalarValue) throws Exception {
        PGobject po = new PGobject();
        po.setType("macaddr");
        po.setValue(scalarValue);
        return po;
    }
}
