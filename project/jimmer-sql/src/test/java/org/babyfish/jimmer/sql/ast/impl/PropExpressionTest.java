package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSelections;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.embedded.ProductTable;
import org.babyfish.jimmer.sql.model.embedded.TransformTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PropExpressionTest extends AbstractQueryTest {

    @Test
    public void testScalarPropertyEquality() {
        BookStoreTable table = BookStoreTable.$;
        assertEquivalent(table.name(), table.name());
        assertEquivalent(table.website(), table.website());
        assertDistinct(table.name(), table.website(), new BookStoreTable().name());
    }

    @Test
    public void testTypedEmbeddedWrapperEquality() {
        PropExpression<?> expression = ProductTable.$.id();
        assertEquivalent(expression, ((PropExpressionImplementor<?>) expression).unwrap(), ProductTable.$.id());
    }

    @Test
    public void testDifferentEmbeddedPathsRemainDistinct() {
        TransformTable table = TransformTable.$;
        assertEquivalent(table.source().leftTop(), table.source().leftTop());
        assertEquivalent(table.source().leftTop().x(), table.source().leftTop().x());
        assertDistinct(
                table.source().leftTop(),
                table.source().rightBottom(),
                table.target().leftTop(),
                new TransformTable().source().leftTop(),
                table.source().leftTop().x(),
                table.source().leftTop().y()
        );
    }

    @Test
    public void testScalarBaseQueryExportsRetainTheirOwner() {
        BookStoreTable table = BookStoreTable.$;
        BaseTable source = getSqlClient().createBaseQuery(table).addSelect(table.name()).addSelect(table.name()).asBaseTable();
        BaseTable otherSource = getSqlClient().createBaseQuery(table).addSelect(table.name()).asBaseTable();
        PropExpression<?> exported = BaseTableSelections.of(table.name(), source, 0);
        assertEquivalent(exported, BaseTableSelections.of(table.name(), source, 0));
        assertDistinct(
                table.name(),
                exported,
                BaseTableSelections.of(table.name(), source, 1),
                BaseTableSelections.of(table.name(), otherSource, 0)
        );
    }

    @Test
    public void testEmbeddedBaseQueryExportsRetainTheirOwner() {
        ProductTable table = ProductTable.$;
        PropExpression<?> raw = ((PropExpressionImplementor<?>) table.id()).unwrap();
        BaseTable source = getSqlClient().createBaseQuery(table).addSelect(raw).addSelect(raw).asBaseTable();
        BaseTable otherSource = getSqlClient().createBaseQuery(table).addSelect(raw).asBaseTable();
        PropExpression<?> exported = BaseTableSelections.of(table.id(), source, 0);
        assertEquivalent(
                exported,
                BaseTableSelections.of(table.id(), source, 0),
                BaseTableSelections.of(raw, source, 0)
        );
        assertDistinct(
                raw,
                exported,
                BaseTableSelections.of(table.id(), source, 1),
                BaseTableSelections.of(table.id(), otherSource, 0)
        );
    }

    private static void assertEquivalent(PropExpression<?>... expressions) {
        for (PropExpression<?> stored : expressions) {
            assertFalse(stored.equals(null));
            assertFalse(stored.equals(new Object()));
            Map<PropExpression<?>, String> map = new HashMap<>();
            map.put(stored, "value");
            Set<PropExpression<?>> set = new HashSet<>();
            set.add(stored);
            for (PropExpression<?> expression : expressions) {
                assertEquals(stored, expression);
                assertEquals(stored.hashCode(), expression.hashCode());
                assertEquals("value", map.get(expression));
                assertFalse(set.add(expression));
            }
        }
    }

    private static void assertDistinct(PropExpression<?>... expressions) {
        assertEquals(expressions.length, new HashSet<>(Arrays.asList(expressions)).size());
        for (int i = 0; i < expressions.length; i++) {
            for (int j = 0; j < expressions.length; j++) {
                if (i != j) {
                    assertNotEquals(expressions[i], expressions[j]);
                }
            }
        }
    }
}
