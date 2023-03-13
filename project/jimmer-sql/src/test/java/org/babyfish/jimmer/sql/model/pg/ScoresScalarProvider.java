package org.babyfish.jimmer.sql.model.pg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGobject;

import java.util.Map;

public class ScoresScalarProvider extends ScalarProvider<Map<Long, Integer>, PGobject> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<Long, Integer>> TYPE_REFERENCE =
            new TypeReference<Map<Long, Integer>>() {};

    @Override
    public @NotNull Map<Long, Integer> toScalar(@NotNull PGobject sqlValue) throws Exception {
        return MAPPER.readValue(sqlValue.getValue(), TYPE_REFERENCE);
    }

    @Override
    public @NotNull PGobject toSql(@NotNull Map<Long, Integer> scalarValue) throws Exception {
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        obj.setValue(MAPPER.writeValueAsString(scalarValue));
        return obj;
    }
}
