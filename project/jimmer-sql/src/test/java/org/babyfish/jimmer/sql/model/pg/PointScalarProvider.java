package org.babyfish.jimmer.sql.model.pg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.postgresql.util.PGobject;

public class PointScalarProvider extends ScalarProvider<Point, PGobject> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Point toScalar(PGobject sqlValue) throws JsonProcessingException {
        return MAPPER.readValue(sqlValue.toString(), Point.class);
    }

    @Override
    public PGobject toSql(Point scalarValue) throws Exception {
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        obj.setValue(MAPPER.writeValueAsString(scalarValue));
        return obj;
    }
}
