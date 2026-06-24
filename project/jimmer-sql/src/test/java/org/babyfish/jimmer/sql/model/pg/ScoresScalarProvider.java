package org.babyfish.jimmer.sql.model.pg;

import com.fasterxml.jackson.core.type.TypeReference;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class ScoresScalarProvider implements ScalarProvider<Map<Long, Integer>, String> {

    private static final TypeReference<Map<Long, Integer>> TYPE_REFERENCE =
            new TypeReference<Map<Long, Integer>>() {
            };

    @Override
    public @NonNull Map<Long, Integer> toScalar(@NonNull String sqlValue) throws Exception {
        return jsonCodec()
                .readerForMapOf(Long.class, Integer.class)
                .read(sqlValue);
    }

    @Override
    public @NonNull String toSql(@NonNull Map<Long, Integer> scalarValue) throws Exception {
        return jsonCodec().writer().writeAsString(scalarValue);
    }

    @Override
    public boolean isJsonScalar() {
        return true;
    }
}
