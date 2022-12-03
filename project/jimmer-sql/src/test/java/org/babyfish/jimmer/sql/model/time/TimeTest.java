package org.babyfish.jimmer.sql.model.time;

import org.babyfish.jimmer.sql.ast.PropExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimeTest {

    @Test
    public void test() throws NoSuchMethodException {
        Assertions.assertSame(
                PropExpression.Cmp.class,
                LogProps.class.getMethod("createdTime").getReturnType()
        );
    }
}
