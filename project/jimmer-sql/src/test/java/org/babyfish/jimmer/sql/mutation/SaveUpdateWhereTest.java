package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.PartTimeEmployeeDraft;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.PartTimeEmployeeProps;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.PartTimeEmployeeTable;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationProps;
import org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class SaveUpdateWhereTest extends AbstractMutationTest {

    @Test
    public void testSharedUpdateConditionApiShape() {
        UpdateCondition<Book, BookTable> condition =
                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE));
        SimpleEntitySaveCommand<Book> command = getSqlClient().saveCommand(book(BigDecimal.valueOf(100)));
        assertNotNull(command.setUpdateWhere(BookTable.class, condition));
        assertNotNull(command.setOptimisticLock(BookTable.class, condition));
    }

    @Test
    public void testNativeUpsertAcceptsAndRejectsUpdateArm() {
        connectAndExpect(
                con -> {
                    SimpleSaveResult<Book> accepted = getSqlClient()
                            .saveCommand(book(BigDecimal.valueOf(100)))
                            .setMode(SaveMode.UPSERT)
                            .setUpdateWhere(
                                    BookTable.class,
                                    (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                            )
                            .execute(con, BookFetcher.$.price());
                    SimpleSaveResult<Book> rejected = getSqlClient()
                            .saveCommand(book(BigDecimal.valueOf(70)))
                            .setMode(SaveMode.UPSERT)
                            .setUpdateWhere(
                                    BookTable.class,
                                    (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                            )
                            .execute(con, BookFetcher.$.price());
                    return asList(accepted, rejected);
                },
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select ID from final table (" +
                                    "merge into BOOK tb_1_ using(values(?, ?)) tb_2_(ID, PRICE) " +
                                    "on tb_1_.ID = tb_2_.ID " +
                                    "when matched and tb_1_.PRICE < tb_2_.PRICE " +
                                    "then update set PRICE = tb_2_.PRICE " +
                                    "when not matched then insert(ID, PRICE) values(tb_2_.ID, tb_2_.PRICE))"
                    ));
                    ctx.statement(it -> it.sql(
                            "select ID from final table (" +
                                    "merge into BOOK tb_1_ using(values(?, ?)) tb_2_(ID, PRICE) " +
                                    "on tb_1_.ID = tb_2_.ID " +
                                    "when matched and tb_1_.PRICE < tb_2_.PRICE " +
                                    "then update set PRICE = tb_2_.PRICE " +
                                    "when not matched then insert(ID, PRICE) values(tb_2_.ID, tb_2_.PRICE))"
                    ));
                    ctx.value(results -> {
                        SimpleSaveResult<Book> accepted = results.get(0);
                        SimpleSaveResult<Book> rejected = results.get(1);
                        assertTrue(accepted.isAccepted());
                        assertEquals(1, accepted.getTotalAffectedRowCount());
                        assertEquals(BigDecimal.valueOf(100), accepted.getModifiedEntity().price());
                        assertFalse(rejected.isAccepted());
                        assertEquals(0, rejected.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testNativeUpdateOnlyUsesWhere() {
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(book(BigDecimal.valueOf(70)))
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .execute(con, BookFetcher.$.price()),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "update BOOK set PRICE = ? where ID = ? and PRICE < ?"
                    ));
                    ctx.value(result -> {
                        assertFalse(result.isAccepted());
                        assertEquals(0, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testCustomAssignmentAndUpdateWhereRemainIndependent() {
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(book(BigDecimal.valueOf(100)))
                        .setMode(SaveMode.UPSERT)
                        .set(
                                BookTable.class,
                                BookProps.PRICE,
                                (target, values) -> target.price().plus(values.newNumber(BookProps.PRICE))
                        )
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .execute(con, BookFetcher.$.price()),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select ID, PRICE from final table (" +
                                    "merge into BOOK tb_1_ using(values(?, ?)) tb_2_(ID, PRICE) " +
                                    "on tb_1_.ID = tb_2_.ID " +
                                    "when matched and tb_1_.PRICE < tb_2_.PRICE " +
                                    "then update set PRICE = tb_1_.PRICE + tb_2_.PRICE " +
                                    "when not matched then insert(ID, PRICE) values(tb_2_.ID, tb_2_.PRICE))"
                    ));
                    ctx.value(result -> {
                        assertTrue(result.isAccepted());
                        assertEquals(new BigDecimal("180.00"), result.getModifiedEntity().price());
                    });
                }
        );
    }

    @Test
    public void testUpdateWhereDoesNotSuppressOptimisticLockFailure() {
        jdbc(con -> assertThrows(
                SaveException.class,
                () -> getSqlClient()
                        .saveCommand(book(BigDecimal.valueOf(100)))
                        .setMode(SaveMode.UPSERT)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .setOptimisticLock(
                                BookTable.class,
                                (table, values) -> table.price().gt(BigDecimal.valueOf(1000))
                        )
                        .execute(con)
        ));
    }

    @Test
    public void testAcceptanceContractForFakeUpdateAndInsertIfAbsentConflict() {
        jdbc(con -> {
            Book idOnlyBook = BookDraft.$.produce(draft -> draft.setId(graphQLInActionId3));
            SimpleSaveResult<Book> fakeUpdate = getSqlClient()
                    .saveCommand(idOnlyBook)
                    .setMode(SaveMode.UPSERT)
                    .execute(con);
            SimpleSaveResult<Book> insertIfAbsentConflict = getSqlClient()
                    .saveCommand(idOnlyBook)
                    .setMode(SaveMode.INSERT_IF_ABSENT)
                    .execute(con);

            assertTrue(fakeUpdate.isAccepted());
            assertFalse(insertIfAbsentConflict.isAccepted());
        });
    }

    @Test
    public void testBatchContainsAcceptedAndRejectedItems() {
        connectAndExpect(
                con -> getSqlClient()
                        .saveEntitiesCommand(
                                asList(
                                        book(graphQLInActionId3, BigDecimal.valueOf(100)),
                                        book(graphQLInActionId2, BigDecimal.valueOf(70))
                                )
                        )
                        .setMode(SaveMode.UPSERT)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select ID from final table (" +
                                    "merge into BOOK tb_1_ using(values(?, ?), (?, ?)) tb_2_(ID, PRICE) " +
                                    "on tb_1_.ID = tb_2_.ID " +
                                    "when matched and tb_1_.PRICE < tb_2_.PRICE " +
                                    "then update set PRICE = tb_2_.PRICE " +
                                    "when not matched then insert(ID, PRICE) values(tb_2_.ID, tb_2_.PRICE))"
                    ));
                    ctx.value(result -> {
                        assertEquals(2, result.getItems().size());
                        assertTrue(result.getItems().get(0).isAccepted());
                        assertFalse(result.getItems().get(1).isAccepted());
                        assertEquals(1, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testMixedNativeAndPreselectedBatchUsesPredicatePerItem() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect() {
            @Override
            public boolean isNoIdUpsertSupported() {
                return false;
            }
        }));
        Book keyBasedBook = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(BigDecimal.valueOf(100));
        });
        connectAndExpect(
                con -> sqlClient
                        .saveEntitiesCommand(asList(book(BigDecimal.valueOf(70)), keyBasedBook))
                        .setMode(SaveMode.UPSERT)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID from BOOK tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.PRICE < ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "update BOOK set PRICE = ? where ID = ? and PRICE < ?"
                    ));
                    ctx.value(result -> {
                        assertFalse(result.getItems().get(0).isAccepted());
                        assertTrue(result.getItems().get(1).isAccepted());
                        assertEquals(1, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testFallbackRejectsBeforeMutation() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        connectAndExpect(
                con -> sqlClient
                        .saveCommand(book(BigDecimal.valueOf(70)))
                        .setMode(SaveMode.UPSERT)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ where tb_1_.ID = ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID from BOOK tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.PRICE < ?"
                    ));
                    ctx.value(result -> {
                        assertFalse(result.isAccepted());
                        assertEquals(0, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testFallbackUsesPessimisticLockOnlyWhenExplicitlyRequested() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        connectAndExpect(
                con -> sqlClient
                        .saveCommand(book(BigDecimal.valueOf(70)))
                        .setMode(SaveMode.UPSERT)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .setPessimisticLock(Book.class)
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ where tb_1_.ID = ? for update"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID from BOOK tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.PRICE < ? for update"
                    ));
                    ctx.value(result -> {
                        assertFalse(result.isAccepted());
                        assertEquals(0, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testIdOnlyPreAssociationIsAllowed() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId3);
            draft.setPrice(BigDecimal.valueOf(100));
            draft.store(true).setId(manningId);
        });
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(book)
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "update BOOK set PRICE = ?, STORE_ID = ? where ID = ? and PRICE < ?"
                    ));
                    ctx.value(result -> {
                        assertTrue(result.isAccepted());
                        assertEquals(1, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testMutablePreAssociationIsRejectedBeforeSqlForWholeBatch() {
        Book idOnlyAssociationBook = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId3);
            draft.setPrice(BigDecimal.valueOf(100));
            draft.store(true).setId(manningId);
        });
        Book mutableAssociationBook = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setPrice(BigDecimal.valueOf(100));
            draft.store(true).setId(manningId).setName("Changed");
        });
        executeAndExpectResult(
                getSqlClient()
                        .saveEntitiesCommand(asList(idOnlyAssociationBook, mutableAssociationBook))
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        ),
                ctx -> ctx.throwable(it -> {
                    it.type(IllegalArgumentException.class);
                    it.message(
                            "Cannot save the owning-side reference property " +
                                    "\"org.babyfish.jimmer.sql.model.Book.store\" because update-where is " +
                                    "configured for its owner type and the reference is neither null nor id-only; " +
                                    "saving it before the owner may execute DML even if the owner is rejected"
                    );
                })
        );
    }

    @Test
    public void testAssociationPipelineOrderIsUnchangedWithoutUpdateWhere() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId3);
            draft.setPrice(BigDecimal.valueOf(100));
            draft.store(true)
                    .setId(manningId)
                    .setName("MANNING+")
                    .setVersion(0);
            draft.addIntoAuthors(author -> author.setId(alexId));
        });
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(book)
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setAssociatedMode(BookProps.AUTHORS, AssociatedSaveMode.APPEND)
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
                    ctx.statement(it -> it.sql(
                            "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values(?, ?)"
                    ));
                    ctx.value(result -> {
                        assertTrue(result.isAccepted());
                        assertEquals(3, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testRejectedOwnerSkipsDependentAssociations() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId3);
            draft.setPrice(BigDecimal.valueOf(70));
            draft.addIntoAuthors(author -> author.setId(alexId));
        });
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(book)
                        .setMode(SaveMode.UPSERT)
                        .setUpdateWhere(
                                BookTable.class,
                                (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE))
                        )
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select ID from final table (" +
                                    "merge into BOOK tb_1_ using(values(?, ?)) tb_2_(ID, PRICE) " +
                                    "on tb_1_.ID = tb_2_.ID " +
                                    "when matched and tb_1_.PRICE < tb_2_.PRICE " +
                                    "then update set PRICE = tb_2_.PRICE " +
                                    "when not matched then insert(ID, PRICE) values(tb_2_.ID, tb_2_.PRICE))"
                    ));
                    ctx.value(result -> assertFalse(result.isAccepted()));
                }
        );
    }

    @Test
    public void testInsertArmDoesNotEvaluateUpdateWhere() {
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000091");
        Book book = BookDraft.$.produce(draft -> {
            draft.setId(id);
            draft.setName("New selective upsert");
            draft.setEdition(1);
            draft.setPrice(BigDecimal.valueOf(90));
            draft.setStoreId(manningId);
        });
        jdbc(con -> {
            SimpleSaveResult<Book> result = getSqlClient()
                    .saveCommand(book)
                    .setMode(SaveMode.UPSERT)
                    .setUpdateWhere(
                            BookTable.class,
                            (table, values) -> table.price().gt(BigDecimal.valueOf(1000))
                    )
                    .execute(con);

            assertTrue(result.isAccepted());
            assertEquals(1, result.getTotalAffectedRowCount());
        });
    }

    @Test
    public void testNullUpdateWhereResultIsAllowedWithInsertOnly() {
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000092");
        Book book = BookDraft.$.produce(draft -> {
            draft.setId(id);
            draft.setName("No update restriction");
            draft.setEdition(1);
            draft.setPrice(BigDecimal.valueOf(90));
        });
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(book)
                        .setMode(SaveMode.INSERT_ONLY)
                        .setUpdateWhere(BookTable.class, (table, values) -> null)
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)");
                        it.variables(id, "No update restriction", 1, BigDecimal.valueOf(90));
                    });
                    ctx.value(result -> {
                        assertTrue(result.isAccepted());
                        assertEquals(1, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testNullUpdateWhereResultAllowsMutablePreAssociation() {
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
                        .setUpdateWhere(BookTable.class, (table, values) -> null)
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
                    });
                }
        );
    }

    @Test
    public void testRejectedInsertIfAbsentTargetRemainsAvailableForAssociation() {
        UUID authorId = UUID.fromString("a0000000-0000-0000-0000-000000000093");
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect()));
        Author author = AuthorDraft.$.produce(draft -> {
            draft.setId(authorId);
            draft.setFirstName("Michael");
            draft.setLastName("Simpson");
            draft.setGender(Gender.MALE);
            draft.addIntoBooks(book -> {
                book.setName("Learning GraphQL");
                book.setEdition(1);
                book.setPrice(BigDecimal.ONE);
            });
        });
        connectAndExpect(
                con -> sqlClient
                        .getEntities()
                        .forConnection(con)
                        .saveCommand(author)
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedMode(AuthorProps.BOOKS, AssociatedSaveMode.APPEND_IF_ABSENT)
                        .execute(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?)");
                        it.variables(authorId, "Michael", "Simpson", "M");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("Learning GraphQL", 1);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) values(?, ?)");
                        it.variables(authorId, learningGraphQLId1);
                    });
                    ctx.value(result -> {
                        assertTrue(result.isAccepted());
                        assertEquals(2, result.getTotalAffectedRowCount());
                        assertEquals(learningGraphQLId1, result.getModifiedEntity().books().get(0).id());
                    });
                }
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void nullableInputInUpdateWhere(boolean fallback) {
        JSqlClient client = getSqlClient(it -> it.setDialect(fallback ? DefaultDialect.INSTANCE : new H2Dialect()));
        jdbc(con -> {
            assertTrue(client.saveCommand(BookStoreDraft.$.produce(draft -> {
                        draft.setId(oreillyId);
                        draft.setWebsite(null);
                    }))
                    .setMode(SaveMode.UPSERT)
                    .setVersionMode(VersionMode.ASSIGNMENT)
                    .setUpdateWhere(BookStoreTable.class, (table, values) -> Predicate.and(
                            values.newString(BookStoreProps.WEBSITE).isNull(),
                            values.newComparable(BookStoreProps.WEBSITE).isNull()
                    ))
                    .execute(con)
                    .isAccepted());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void nullableNumericInputInUpdateWhere(boolean fallback) {
        JSqlClient client = getSqlClient(it -> it.setDialect(fallback ? DefaultDialect.INSTANCE : new H2Dialect()));
        jdbc(con -> assertTrue(client.saveCommand(PartTimeEmployeeDraft.$.produce(draft -> {
                    draft.setId(6002L);
                    draft.setAnnualSalary(null);
                    draft.setHourlyRate(null);
                }))
                .setMode(SaveMode.UPSERT)
                .setUpdateWhere(PartTimeEmployeeTable.class, (table, values) -> Predicate.and(
                        values.newNumber(PartTimeEmployeeProps.ANNUAL_SALARY).isNull(),
                        values.newNumber(PartTimeEmployeeProps.HOURLY_RATE).isNull(),
                        values.newComparable(PartTimeEmployeeProps.ANNUAL_SALARY).isNull()
                ))
                .execute(con)
                .isAccepted()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void nullInputEqualityKeepsSqlNullSemantics(boolean fallback) {
        JSqlClient client = getSqlClient(it -> it.setDialect(fallback ? DefaultDialect.INSTANCE : new H2Dialect()));
        jdbc(con -> assertFalse(client.saveCommand(BookStoreDraft.$.produce(draft -> {
                    draft.setId(oreillyId);
                    draft.setWebsite(null);
                }))
                .setMode(SaveMode.UPSERT)
                .setVersionMode(VersionMode.ASSIGNMENT)
                .setUpdateWhere(BookStoreTable.class, (table, values) -> Predicate.or(
                        values.newValue(BookStoreProps.WEBSITE).eq(table.website()),
                        values.newString(BookStoreProps.WEBSITE).eq(table.website())
                ))
                .execute(con)
                .isAccepted()));
    }

    @Test
    public void testValidation() {
        assertThrows(
                NullPointerException.class,
                () -> getSqlClient()
                        .saveCommand(book(BigDecimal.valueOf(90)))
                        .setUpdateWhere(BookTable.class, null)
        );
        jdbc(con -> {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> getSqlClient()
                            .saveCommand(book(BigDecimal.valueOf(90)))
                            .setMode(SaveMode.INSERT_ONLY)
                            .setUpdateWhere(BookTable.class, (table, values) -> table.price().isNotNull())
                            .execute(con)
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> getSqlClient()
                            .saveCommand(BookDraft.$.produce(draft -> draft.setId(graphQLInActionId3)))
                            .setMode(SaveMode.UPSERT)
                            .setUpdateWhere(
                                    BookTable.class,
                                    (table, values) -> values.newNumber(BookProps.PRICE).gt(table.price())
                            )
                            .execute(con)
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> getSqlClient()
                            .saveCommand(book(BigDecimal.valueOf(90)))
                            .setMode(SaveMode.UPSERT)
                            .setUpdateWhere(
                                    OrganizationTable.class,
                                    (table, values) -> table.name().isNotNull()
                            )
                            .execute(con)
            );
            assertThrows(
                    IllegalStateException.class,
                    () -> getSqlClient()
                            .saveCommand(book(BigDecimal.valueOf(90)))
                            .setMode(SaveMode.UPSERT)
                            .setUpdateWhere(
                                    BookTable.class,
                                    (table, values) -> table.store().name().isNotNull()
                            )
                            .execute(con)
            );
        });
    }

    @Test
    public void testDifferentSingleTableDiscriminatorIsRejected() {
        Organization organization = OrganizationDraft.$.produce(draft -> {
            draft.setId(101L);
            draft.setName("Should not update");
            draft.setTaxCode("SHOULD-NOT-WRITE");
        });
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(organization)
                        .setMode(SaveMode.UPSERT)
                        .setUpdateWhere(
                                OrganizationTable.class,
                                (table, values) -> table.name().ne(values.newString(OrganizationProps.NAME))
                        )
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select ID from final table (" +
                                    "merge into CLIENT tb_1_ " +
                                    "using(values(?, ?, ?, ?)) tb_2_(ID, NAME, TAX_CODE, CLIENT_TYPE) " +
                                    "on tb_1_.ID = tb_2_.ID " +
                                    "when matched and tb_1_.NAME <> tb_2_.NAME " +
                                    "and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                    "then update set NAME = tb_2_.NAME, TAX_CODE = tb_2_.TAX_CODE " +
                                    "when not matched then insert(ID, NAME, TAX_CODE, CLIENT_TYPE) " +
                                    "values(tb_2_.ID, tb_2_.NAME, tb_2_.TAX_CODE, tb_2_.CLIENT_TYPE))"
                    ));
                    ctx.value(result -> {
                        assertFalse(result.isAccepted());
                        assertEquals(0, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    @Test
    public void testDifferentSingleTableDiscriminatorIsRejectedWithoutUserPredicate() {
        Organization organization = OrganizationDraft.$.produce(draft -> {
            draft.setId(101L);
            draft.setName("Should not update");
            draft.setTaxCode("SHOULD-NOT-WRITE");
        });
        connectAndExpect(
                con -> getSqlClient()
                        .saveCommand(organization)
                        .setMode(SaveMode.UPSERT)
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "merge into CLIENT tb_1_ " +
                                    "using(values(?, ?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                    "on tb_1_.ID = tb_2_.ID " +
                                    "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                    "then update set NAME = tb_2_.NAME, TAX_CODE = tb_2_.TAX_CODE " +
                                    "when not matched then insert(ID, CLIENT_TYPE, NAME, TAX_CODE) " +
                                    "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME, tb_2_.TAX_CODE)"
                    ));
                    ctx.value(result -> {
                        assertFalse(result.isAccepted());
                        assertEquals(0, result.getTotalAffectedRowCount());
                    });
                }
        );
    }

    private static Book book(BigDecimal price) {
        return book(graphQLInActionId3, price);
    }

    private static Book book(UUID id, BigDecimal price) {
        return BookDraft.$.produce(draft -> {
            draft.setId(id);
            draft.setPrice(price);
        });
    }
}
