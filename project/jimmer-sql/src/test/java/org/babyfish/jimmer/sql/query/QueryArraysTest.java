package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.arrays.ArrayModel;
import org.junit.jupiter.api.Test;

public class QueryArraysTest extends AbstractQueryTest {

  @Test
  public void testQueryArrayProperties() {
    connectAndExpect(
        con -> {
          return getSqlClient()
              .getEntities()
              .forConnection(con)
              .findById(ArrayModel.class, Constants.arrayModelId);
        },
        ctx -> {
          ctx.sql(
              "select tb_1_.ID, tb_1_.STRINGS, tb_1_.BYTES, tb_1_.INTS, tb_1_.INTEGERS, tb_1_.LONGS, tb_1_.UUIDS, tb_1_.FLOATS " +
                      "from ARRAY_MODEL tb_1_ " +
                      "where tb_1_.ID = ?"
          );
          ctx.rows(
              "[" +
                  "--->{" +
                  "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db635\"," +
                  "--->--->\"strings\":[\"3\",\"2\",\"1\"]," +
                  "--->--->\"bytes\":[3,2,1]," +
                  "--->--->\"ints\":[6,5,4]," +
                  "--->--->\"integers\":[3,2,1]," +
                  "--->--->\"longs\":[3,2,1]," +
                  "--->--->\"uuids\":[\"e110c564-23cc-4811-9e81-d587a13db635\"]," +
                  "--->--->\"floats\":[3.0,2.0,1.0]" +
                  "--->}" +
                  "]"
          );
        });
  }
}
