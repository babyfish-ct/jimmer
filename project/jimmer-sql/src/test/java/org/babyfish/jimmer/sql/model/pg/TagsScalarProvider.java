package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class TagsScalarProvider implements ScalarProvider<List<String>, String> {
    @Override
    public List<String> toScalar(@NonNull String sqlValue) throws Exception {
        return jsonCodec().readerForListOf(String.class).read(sqlValue);
    }

    @Override
    public String toSql(@NonNull List<String> scalarValue) throws Exception {
        return jsonCodec().writer().writeAsString(scalarValue);
    }

    @Override
    public boolean isJsonScalar() {
        return true;
    }
}
