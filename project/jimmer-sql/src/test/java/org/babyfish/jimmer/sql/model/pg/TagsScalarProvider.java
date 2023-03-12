package org.babyfish.jimmer.sql.model.pg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.postgresql.util.PGobject;

import java.util.List;

public class TagsScalarProvider extends ScalarProvider<List<String>, PGobject> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<List<String>> TAGS_REFERENCE =
            new TypeReference<List<String>>() {};

    @Override
    public List<String> toScalar(PGobject sqlValue) throws Exception {
        return MAPPER.readValue(sqlValue.getValue(), TAGS_REFERENCE);
    }

    @Override
    public PGobject toSql(List<String> scalarValue) throws Exception {
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        obj.setValue(MAPPER.writeValueAsString(scalarValue));
        return obj;
    }
}
