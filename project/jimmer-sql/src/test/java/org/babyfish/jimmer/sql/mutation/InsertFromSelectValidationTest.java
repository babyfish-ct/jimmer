package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.mutation.MutableInsert;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Constants.oreillyId;
import static org.junit.jupiter.api.Assertions.*;

public class InsertFromSelectValidationTest extends AbstractMutationTest {

    @Test
    public void testStrictInsertDoesNotSuppressConflict() {
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = uuidAndString(oreillyId, "CONFLICT");
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> assertThrows(
                RuntimeException.class,
                () -> getSqlClient()
                        .createInsert(table, source)
                        .set(table.id(), source.get_1())
                        .set(table.name(), source.get_2())
                        .execute(con)
        ));
    }

    @Test
    public void testAssignmentMustBelongToTarget() {
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = uuidAndString(oreillyId, "VALUE");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient()
                        .createInsert(BookStoreTable.$, source)
                        .set(BookTable.$.id(), source.get_1())
        );
        assertTrue(ex.getMessage().contains("does not belong to the mutation target"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testAssignmentTypesMustMatch() {
        BaseTable1<NumericExpression<Integer>> source = getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(1))
                .asBaseTable();
        MutableInsert raw = getSqlClient().createInsert(BookStoreTable.$, source);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> raw.set(
                        BookStoreTable.$.name(),
                        source.get_1()
                )
        );
        assertTrue(ex.getMessage().contains("is incompatible with target expression type"));
    }

    @Test
    public void testExplicitConflictTargetValidation() {
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = uuidAndString(oreillyId, "VALUE");
        BookStoreTable store = BookStoreTable.$;

        IllegalArgumentException wrongTable = assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient()
                        .createInsert(store, source)
                        .set(store.id(), source.get_1())
                        .onConflictDoNothing(BookTable.$.id())
        );
        assertTrue(wrongTable.getMessage().contains("does not belong to the mutation target"));

        jdbc(con -> {
            IllegalArgumentException duplicate = assertThrows(
                    IllegalArgumentException.class,
                    () -> getSqlClient()
                            .createInsert(store, source)
                            .set(store.id(), source.get_1())
                            .set(store.name(), source.get_2())
                            .onConflictDoNothing(store.id(), store.id())
                            .execute(con)
            );
            assertTrue(duplicate.getMessage().contains("Duplicate conflict target property"));

            IllegalArgumentException unassigned = assertThrows(
                    IllegalArgumentException.class,
                    () -> getSqlClient()
                            .createInsert(store, source)
                            .set(store.id(), source.get_1())
                            .onConflictDoNothing(store.name())
                            .execute(con)
            );
            assertTrue(unassigned.getMessage().contains("has no insert assignment"));
        });
    }

    @Test
    public void testExplicitlyEmptyConflictTargetIsRejected() {
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = uuidAndString(oreillyId, "VALUE");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient()
                        .createInsert(BookStoreTable.$, source)
                        .onConflictDoNothing(new PropExpression<?>[0])
        );
        assertEquals(
                "Explicit conflict properties cannot be empty; use onConflictDoNothing() for inference",
                ex.getMessage()
        );
    }

    @Test
    public void testConflictTargetMustBeUnique() {
        BaseTable1<NumericExpression<BigDecimal>> source = getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(new BigDecimal("12.34")))
                .asBaseTable();
        BookTable table = BookTable.$;
        jdbc(con -> {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> getSqlClient()
                            .createInsert(table, source)
                            .set(table.price(), source.get_1())
                            .onConflictDoNothing(table.price())
                            .execute(con)
            );
            assertTrue(ex.getMessage().contains("do not uniquely identify a target row"));
        });
    }

    @Test
    public void testConflictTargetInferenceCanFail() {
        BaseTable1<NumericExpression<Integer>> source = getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(7))
                .asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> {
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> getSqlClient()
                            .createInsert(table, source)
                            .set(table.version(), source.get_1())
                            .onConflictDoNothing()
                            .execute(con)
            );
            assertTrue(ex.getMessage().contains("Cannot infer an eligible conflict key"));
        });
    }

    @Test
    public void testUpsertRequiresUniqueAssignedKey() {
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = uuidAndString(oreillyId, "VALUE");
        BookStoreTable store = BookStoreTable.$;
        jdbc(con -> {
            IllegalStateException noKey = assertThrows(
                    IllegalStateException.class,
                    () -> getSqlClient()
                            .createUpsert(store, source)
                            .insert(store.id(), source.get_1())
                            .merge(store.name(), source.get_2())
                            .execute(con)
            );
            assertTrue(noKey.getMessage().contains("At least one key assignment is required"));
        });
    }

    @Test
    public void testReturningSelectionMustBelongToTarget() {
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = uuidAndString(oreillyId, "VALUE");
        BookStoreTable store = BookStoreTable.$;
        jdbc(con -> {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> getSqlClient()
                            .createInsert(store, source)
                            .set(store.id(), source.get_1())
                            .set(store.name(), source.get_2())
                            .returning(BookTable.$.id())
                            .execute(con)
            );
            assertTrue(ex.getMessage().contains("does not belong to the mutation target"));
        });
    }

    @Test
    public void testJoinedTableInheritanceTargetIsRejected() {
        BaseTable1<NumericExpression<Long>> source = getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(1L))
                .asBaseTable();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getSqlClient().createInsert(
                        org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationTable.$,
                        source
                )
        );
        assertTrue(ex.getMessage().contains("does not support joined-inheritance target type"));
    }

    private BaseTable2<ComparableExpression<UUID>, StringExpression> uuidAndString(UUID id, String value) {
        return getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(id))
                .addSelect(Expression.value(value))
                .asBaseTable();
    }
}
