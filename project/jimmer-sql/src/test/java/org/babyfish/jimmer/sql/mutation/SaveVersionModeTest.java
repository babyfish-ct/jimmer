package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.UpsertMask;
import org.babyfish.jimmer.sql.ast.mutation.VersionMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class SaveVersionModeTest extends AbstractMutationTest {

    @Test
    public void testUnloadedVersionUsesInitialValueOnInsert() {
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000092");
        BookStore store = BookStoreDraft.$.produce(draft -> {
            draft.setId(id);
            draft.setName("VERSION MODE");
        });
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(store)
                        .setMode(SaveMode.INSERT_ONLY)
                        .setVersionMode(VersionMode.ASSIGNMENT)
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)"
                    ));
                    ctx.value(result -> {
                        assertTrue(result.isAccepted());
                        assertEquals(1, result.getTotalAffectedRowCount());
                        assertEquals(0, result.getModifiedEntity().version());
                    });
                }
        );
    }

    @Test
    public void testLoadedVersionIsOrdinaryUpdateAssignment() {
        BookStore store = store(oreillyId, 7);
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(store)
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setVersionMode(VersionMode.ASSIGNMENT)
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "update BOOK_STORE set VERSION = ? where ID = ?"
                    ));
                    ctx.value(result -> {
                        assertTrue(result.isAccepted());
                        assertEquals(1, result.getTotalAffectedRowCount());
                        assertEquals(7, result.getModifiedEntity().version());
                    });
                }
        );
    }

    @Test
    public void testCustomVersionAssignmentIsIndependentOfFluentOrder() {
        connectAndExpect(
                con -> asList(
                        getSqlClient()
                                .saveCommand(store(oreillyId, 0))
                                .setMode(SaveMode.UPDATE_ONLY)
                                .set(
                                        BookStoreTable.class,
                                        BookStoreProps.VERSION,
                                        (target, values) -> target.version().plus(1)
                                )
                                .setVersionMode(VersionMode.ASSIGNMENT)
                                .setUpdateWhere(
                                        BookStoreTable.class,
                                        (table, values) -> table.version().eq(values.newNumber(BookStoreProps.VERSION))
                                )
                                .execute(con),
                        getSqlClient()
                                .saveCommand(store(manningId, 0))
                                .setMode(SaveMode.UPDATE_ONLY)
                                .setVersionMode(VersionMode.ASSIGNMENT)
                                .set(
                                        BookStoreTable.class,
                                        BookStoreProps.VERSION,
                                        (target, values) -> target.version().plus(1)
                                )
                                .setUpdateWhere(
                                        BookStoreTable.class,
                                        (table, values) -> table.version().eq(values.newNumber(BookStoreProps.VERSION))
                                )
                                .execute(con)
                ),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "update BOOK_STORE set VERSION = VERSION + ? where ID = ? and VERSION = ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "update BOOK_STORE set VERSION = VERSION + ? where ID = ? and VERSION = ?"
                    ));
                    ctx.value(results -> {
                        assertTrue(results.get(0).isAccepted());
                        assertTrue(results.get(1).isAccepted());
                    });
                }
        );
    }

    @Test
    public void testUpsertMaskCanMakeVersionInsertOnly() {
        BookStore store = store(oreillyId, 7);
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(store)
                        .setMode(SaveMode.UPSERT)
                        .setVersionMode(VersionMode.ASSIGNMENT)
                        .setUpsertMask(
                                UpsertMask
                                        .of(BookStore.class)
                                        .addInsertableProp(BookStoreProps.VERSION)
                                        .forbidUpdate()
                        )
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "merge into BOOK_STORE tb_1_ " +
                                    "using(values(?, ?)) tb_2_(ID, VERSION) " +
                                    "on tb_1_.ID = tb_2_.ID " +
                                    "when not matched then insert(ID, VERSION) values(tb_2_.ID, tb_2_.VERSION)"
                    ));
                    ctx.value(result -> {
                        assertEquals(0, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testExplicitOptimisticLockStillWorksInAssignmentMode() {
        executeAndExpectResult(
                getSqlClient()
                        .saveCommand(store(oreillyId, 7))
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setVersionMode(VersionMode.ASSIGNMENT)
                        .setOptimisticLock(
                                BookStoreTable.class,
                                (table, values) -> table.version().eq(values.newNumber(BookStoreProps.VERSION))
                        ),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "update BOOK_STORE set VERSION = ? where ID = ? and VERSION = ?"
                    ));
                    ctx.throwable(it -> it.type(SaveException.OptimisticLockError.class));
                }
        );
    }

    @Test
    public void testVersionModeDoesNotPropagateToAssociatedEntities() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId3);
            draft.setPrice(BigDecimal.valueOf(100));
            draft.store(true)
                    .setId(manningId)
                    .setName("MANNING+")
                    .setVersion(0);
        });
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(book)
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setVersionMode(VersionMode.ASSIGNMENT)
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.ID = ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "update BOOK_STORE set NAME = ?, VERSION = VERSION + 1 where ID = ? and VERSION = ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?"
                    ));
                    ctx.value(result -> {
                        assertTrue(result.isAccepted());
                        assertEquals(2, result.getTotalAffectedRowCount());
                        assertEquals(1, result.getModifiedEntity().store().version());
                    });
                }
        );
    }

    @Test
    public void testVersionAssignmentRequiresAssignmentMode() {
        executeAndExpectResult(
                getSqlClient()
                        .saveCommand(store(oreillyId, 0))
                        .setMode(SaveMode.UPDATE_ONLY)
                        .set(
                                BookStoreTable.class,
                                BookStoreProps.VERSION,
                                (target, values) -> target.version().plus(1)
                        ),
                ctx -> ctx.throwable(it -> {
                    it.type(IllegalArgumentException.class);
                    it.message(
                            "The version property \"org.babyfish.jimmer.sql.model.BookStore.version\" " +
                                    "can only be a save assignment target in ASSIGNMENT version mode"
                    );
                })
        );
        assertThrows(
                NullPointerException.class,
                () -> getSqlClient().saveCommand(store(oreillyId, 0)).setVersionMode(null)
        );
    }

    private static BookStore store(UUID id, int version) {
        return BookStoreDraft.$.produce(draft -> {
            draft.setId(id);
            draft.setVersion(version);
        });
    }
}
