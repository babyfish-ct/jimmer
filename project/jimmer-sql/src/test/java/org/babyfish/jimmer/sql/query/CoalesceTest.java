package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.DateExpression;
import org.babyfish.jimmer.sql.ast.TemporalExpression;
import org.babyfish.jimmer.sql.model.time.TimeRowTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;

public class CoalesceTest {

    @Test
    public void coalesceTemporalColumns() {
        TimeRowTable table = TimeRowTable.$;
        TemporalExpression<LocalDateTime> expr = table.value7().coalesce(table.value7());
        Assertions.assertNotNull(expr.ge(LocalDateTime.of(2000, 1, 1, 0, 0)));
    }

    @Test
    public void coalesceTemporalColumnWithValue() {
        TimeRowTable table = TimeRowTable.$;
        TemporalExpression<LocalDateTime> expr = table.value7().coalesce(LocalDateTime.of(2000, 1, 1, 0, 0));
        Assertions.assertNotNull(expr.ge(LocalDateTime.of(2001, 1, 1, 0, 0)));
    }

    @Test
    public void coalesceDateColumns() {
        TimeRowTable table = TimeRowTable.$;
        DateExpression<Date> expr = table.value1().coalesce(table.value1());
        Assertions.assertNotNull(expr.ge(new Date(0)));
    }
}
