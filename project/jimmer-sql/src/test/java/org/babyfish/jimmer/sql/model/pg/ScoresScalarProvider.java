package org.babyfish.jimmer.sql.model.pg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public class ScoresScalarProvider implements ScalarProvider<Map<Long, Integer>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<Long, Integer>> TYPE_REFERENCE =
            new TypeReference<Map<Long, Integer>>() {};

    @Override
    public @NonNull Map<Long, Integer> toScalar(@NonNull String sqlValue) throws Exception {
        return MAPPER.readValue(sqlValue, TYPE_REFERENCE);
    }

    @Override
    public @NonNull String toSql(@NonNull Map<Long, Integer> scalarValue) throws Exception {
        return MAPPER.writeValueAsString(scalarValue);
    }

    @Override
    public boolean isJsonScalar() {
        return true;
    }
}
