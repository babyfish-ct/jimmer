package org.babyfish.jimmer.sql.model.pg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class TagsScalarProvider implements ScalarProvider<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<List<String>> TAGS_REFERENCE =
            new TypeReference<List<String>>() {};

    @Override
    public List<String> toScalar(@NonNull String sqlValue) throws Exception {
        return MAPPER.readValue(sqlValue, TAGS_REFERENCE);
    }

    @Override
    public String toSql(@NonNull List<String> scalarValue) throws Exception {
        return MAPPER.writeValueAsString(scalarValue);
    }

    @Override
    public boolean isJsonScalar() {
        return true;
    }
}
