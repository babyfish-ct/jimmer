package org.babyfish.jimmer.sql.model.pg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TagsScalarProvider implements ScalarProvider<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<List<String>> TAGS_REFERENCE =
            new TypeReference<List<String>>() {};

    @Override
    public List<String> toScalar(@NotNull String sqlValue) throws Exception {
        return MAPPER.readValue(sqlValue, TAGS_REFERENCE);
    }

    @Override
    public String toSql(@NotNull List<String> scalarValue) throws Exception {
        return MAPPER.writeValueAsString(scalarValue);
    }
}
