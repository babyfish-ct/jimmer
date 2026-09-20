package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class SaveModeInteractionTest extends AbstractMutationTest {

    @ParameterizedTest
    @MethodSource("conditionalCases")
    public void testConditionalBatch(SaveMode mode, Strategy strategy, boolean forbidUpdate, boolean fetch) {
        JSqlClient client = client(strategy);
        UUID newId = UUID.randomUUID();
        Book accepted = book(graphQLInActionId3, "Accepted input", 100);
        Book rejected = book(graphQLInActionId2, "Rejected input", 1);
        Book inserted = book(newId, "Inserted input", 1);
        jdbc(con -> {
            BatchEntitySaveCommand<Book> command = client.saveEntitiesCommand(Arrays.asList(accepted, rejected, inserted))
                    .setMode(mode)
                    .setUpdateWhere(BookTable.class, (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE)));
            command = forbidUpdate ? command.forbidUpdate() : command.setUpsertMask(BookProps.PRICE);
            BatchSaveResult<Book> result = fetch ? command.execute(con, BookFetcher.$.allScalarFields()) : command.execute(con);
            assertEquals(3, result.getItems().size());
            assertSame(accepted, result.getItems().get(0).getOriginalEntity());
            assertSame(rejected, result.getItems().get(1).getOriginalEntity());
            assertSame(inserted, result.getItems().get(2).getOriginalEntity());
            assertTrue(result.getItems().get(0).isAccepted());
            assertFalse(result.getItems().get(1).isAccepted());
            assertEquals(mode != SaveMode.UPDATE_ONLY, result.getItems().get(2).isAccepted());
            boolean updates = mode == SaveMode.UPDATE_ONLY || !forbidUpdate;
            if (fetch) {
                Book modified = result.getItems().get(0).getModifiedEntity();
                assertEquals(graphQLInActionId3, modified.id());
                assertEquals(mode == SaveMode.UPDATE_ONLY ? "Accepted input" : "GraphQL in Action", modified.name());
                assertEquals(mode == SaveMode.UPDATE_ONLY ? 9 : 3, modified.edition());
                assertEquals(0, BigDecimal.valueOf(updates ? 100 : 80).compareTo(modified.price()));
                if (mode != SaveMode.UPDATE_ONLY) {
                    assertEquals(newId, result.getItems().get(2).getModifiedEntity().id());
                    assertEquals("Inserted input", result.getItems().get(2).getModifiedEntity().name());
                }
            }
            // Even with preselection, the DML must recheck the condition before accepting an existing row.
            assertEquals(mode == SaveMode.UPDATE_ONLY ? 1 : 2, result.getTotalAffectedRowCount());
            assertConditionalSql(mode, strategy, forbidUpdate, fetch, accepted, rejected, inserted);
            Book storedAccepted = client.getEntities().forConnection(con).findById(Book.class, graphQLInActionId3);
            assertEquals(mode == SaveMode.UPDATE_ONLY ? "Accepted input" : "GraphQL in Action", storedAccepted.name());
            assertEquals(new BigDecimal(updates ? "100.00" : "80.00"), storedAccepted.price());
            Book storedRejected = client.getEntities().forConnection(con).findById(Book.class, graphQLInActionId2);
            assertEquals("GraphQL in Action", storedRejected.name());
            assertEquals(2, storedRejected.edition());
            assertEquals(new BigDecimal("81.00"), storedRejected.price());
            Book storedInserted = client.getEntities().forConnection(con).findById(Book.class, newId);
            if (mode == SaveMode.UPDATE_ONLY) {
                assertNull(storedInserted);
            } else {
                assertEquals("Inserted input", storedInserted.name());
                assertEquals(new BigDecimal("1.00"), storedInserted.price());
            }
        });
    }

    @ParameterizedTest
    @MethodSource("conflictCases")
    public void testUnloadedReturningFieldOnConflict(SaveMode mode, Strategy strategy, boolean suppliedId) {
        SysUser existing = user(suppliedId ? 1L : null, suppliedId ? "ignored_account" : "sysusr_001");
        SysUser inserted = user(suppliedId ? 900L : null, "new_account");
        JSqlClient client = client(strategy);
        jdbc(con -> {
            BatchEntitySaveCommand<SysUser> command = client.saveEntitiesCommand(Arrays.asList(existing, inserted))
                    .setMode(mode).setKeyProps(SysUserProps.ACCOUNT).forbidUpdate();
            if (!suppliedId) {
                command = command.matchByKey();
            }
            BatchSaveResult<SysUser> result = command.execute(con, SysUserFetcher.$.description());
            boolean acceptExisting = mode == SaveMode.UPSERT;
            assertEquals(acceptExisting, result.getItems().get(0).isAccepted());
            if (acceptExisting) {
                assertEquals(1L, result.getItems().get(0).getModifiedEntity().id());
                assertEquals("description_001", result.getItems().get(0).getModifiedEntity().description());
            } else {
                assertFalse(ImmutableObjects.isLoaded(result.getItems().get(0).getModifiedEntity(), SysUserProps.DESCRIPTION));
            }
            assertTrue(result.getItems().get(1).isAccepted());
            SysUser modified = result.getItems().get(1).getModifiedEntity();
            assertEquals("DEFAULT_DESCRIPTION", modified.description());
            if (suppliedId) {
                assertEquals(900L, modified.id());
            } else {
                assertTrue(modified.id() >= 100L);
            }
            for (Execution execution : getExecutions()) {
                if (!acceptExisting) {
                    assertFalse(execution.getSql().contains("/* fake update"), execution.getSql());
                    assertFalse(execution.getSql().contains("when matched"), execution.getSql());
                    assertFalse(execution.getSql().startsWith("update "), execution.getSql());
                }
            }
            boolean fakeUpdate = acceptExisting && (strategy == Strategy.RETURNING ||
                    strategy == Strategy.NO_RETURNING && !suppliedId);
            assertEquals(fakeUpdate ? 2 : 1, result.getTotalAffectedRowCount());
            assertEquals(strategy == Strategy.RETURNING ? 1 : strategy == Strategy.NO_RETURNING ? 2 : 3,
                    getExecutions().size());
            if (strategy != Strategy.PRESELECT) {
                Execution dml = getExecutions().get(0);
                String sql = dml.getSql();
                String key = suppliedId ? "ID" : "ACCOUNT";
                assertTrue(sql.contains("on tb_1_." + key + " = tb_2_." + key + " "), sql);
                assertEquals(fakeUpdate, sql.contains("/* fake update"), sql);
                assertEquals(strategy == Strategy.RETURNING, sql.startsWith("select ID, DESCRIPTION"), sql);
                assertEquals(strategy == Strategy.RETURNING ? 1 : 2, dml.getBatchCount());
                List<Object> variables = new ArrayList<>();
                int index = 0;
                for (SysUser user : Arrays.asList(existing, inserted)) {
                    List<Object> row = new ArrayList<>();
                    if (suppliedId) {
                        row.add(user.id());
                    }
                    row.addAll(Arrays.asList(user.account(), user.email(), user.area(), user.nickName()));
                    variables.addAll(row);
                    if (strategy == Strategy.NO_RETURNING) {
                        assertEquals(row, dml.getVariables(index++));
                    }
                }
                if (strategy == Strategy.RETURNING) {
                    assertEquals(variables, dml.getVariables(0));
                }
            }
            if (strategy != Strategy.RETURNING) {
                Execution read = getExecutions().get(getExecutions().size() - 1);
                assertTrue(read.getSql().startsWith("select tb_1_.ID, tb_1_.DESCRIPTION from SYS_USER "));
                List<?> ids = read.getVariables(0);
                if (acceptExisting && strategy == Strategy.NO_RETURNING) {
                    ids = (List<?>) ids.get(0);
                }
                // An insert-if-absent conflict must not be materialized by a follow-up lookup.
                assertEquals(acceptExisting ? Arrays.asList(1L, modified.id()) : Arrays.asList(modified.id()), ids);
            }
            SysUser stored = client.getEntities().forConnection(con).findById(SysUser.class, 1L);
            assertEquals("sysusr_001", stored.account());
            assertEquals("description_001", stored.description());
            assertEquals("new_account", client.getEntities().forConnection(con).findById(SysUser.class, modified.id()).account());
        });
    }

    @ParameterizedTest
    @MethodSource("associationCases")
    public void testConditionalNoOpControlsDependentAssociations(Strategy strategy, boolean accepted) {
        JSqlClient client = client(strategy);
        Book input = BookDraft.$.produce(book(graphQLInActionId3, "Ignored input", accepted ? 100 : 1),
                draft -> draft.addIntoAuthors(author -> author.setId(alexId)));
        jdbc(con -> {
            SimpleSaveResult<Book> result = client.saveCommand(input).forbidUpdate()
                    .setUpdateWhere(BookTable.class, (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE)))
                    .setAssociatedMode(BookProps.AUTHORS, AssociatedSaveMode.MERGE).execute(con);
            assertEquals(accepted, result.isAccepted());
            assertEquals(accepted ? 2 : 0, result.getTotalAffectedRowCount());
            assertEquals(strategy == Strategy.PRESELECT ? (accepted ? 5 : 2) : (accepted ? 2 : 1), getExecutions().size());
            if (accepted) {
                Execution association = getExecutions().get(getExecutions().size() - 1);
                assertEquals(strategy == Strategy.PRESELECT ?
                        "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values(?, ?)" :
                        "merge into BOOK_AUTHOR_MAPPING tb_1_ using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                                "on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                                "when not matched then insert(BOOK_ID, AUTHOR_ID) values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)",
                        association.getSql());
                assertEquals(Arrays.asList(graphQLInActionId3, alexId), association.getVariables(0));
                assertEquals(1, association.getBatchCount());
            } else {
                getExecutions().forEach(execution -> assertFalse(execution.getSql().contains("BOOK_AUTHOR_MAPPING")));
            }
            Book stored = client.getEntities().forConnection(con).findById(BookFetcher.$.price().authors(), graphQLInActionId3);
            assertEquals(new BigDecimal("80.00"), stored.price());
            assertEquals(accepted ? 2 : 1, stored.authors().size());
            assertTrue(stored.authors().stream().anyMatch(author -> author.id().equals(sammerId)));
            assertEquals(accepted, stored.authors().stream().anyMatch(author -> author.id().equals(alexId)));
        });
    }

    @ParameterizedTest
    @MethodSource("insertModes")
    public void testInsertModesRejectUpdateWhereBeforeSql(SaveMode mode, boolean modeFirst) {
        jdbc(con -> {
            BatchEntitySaveCommand<Book> command = client(Strategy.RETURNING)
                    .saveEntitiesCommand(Arrays.asList(book(UUID.randomUUID(), "New input", 100),
                            book(graphQLInActionId3, "Existing input", 100)))
                    .forbidUpdate();
            if (modeFirst) {
                command = command.setMode(mode);
            }
            command = command.setUpdateWhere(BookTable.class, (table, values) -> table.price().isNotNull());
            if (!modeFirst) {
                command = command.setMode(mode);
            }
            BatchEntitySaveCommand<Book> finalCommand = command;
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> finalCommand.execute(con));
            assertEquals("Update-where predicates can only be used with UPDATE_ONLY, UPSERT or NON_IDEMPOTENT_UPSERT mode",
                    ex.getMessage());
            assertTrue(getExecutions().isEmpty());
        });
    }

    @ParameterizedTest
    @EnumSource(value = SaveMode.class, names = {"INSERT_ONLY", "INSERT_IF_ABSENT"})
    public void testNullConditionAndForbidUpdateDoNotSuppressInsert(SaveMode mode) {
        UUID id = UUID.randomUUID();
        JSqlClient client = client(Strategy.RETURNING);
        jdbc(con -> {
            SimpleSaveResult<Book> result = client.saveCommand(book(id, "Inserted input", 1))
                    .setMode(mode).forbidUpdate()
                    .setUpdateWhere(BookTable.class, (table, values) -> null)
                    .execute(con, BookFetcher.$.allScalarFields());
            assertTrue(result.isAccepted());
            assertEquals(1, result.getTotalAffectedRowCount());
            assertEquals(id, result.getModifiedEntity().id());
            assertEquals("Inserted input", result.getModifiedEntity().name());
            assertEquals(0, BigDecimal.ONE.compareTo(result.getModifiedEntity().price()));
            assertFalse(getExecutions().get(0).getSql().contains("when matched"));
            assertFalse(getExecutions().get(0).getSql().contains("/* fake update"));
            assertEquals(2, getExecutions().size());
            assertEquals("Inserted input", client.getEntities().forConnection(con).findById(Book.class, id).name());
        });
    }

    @ParameterizedTest
    @EnumSource(Strategy.class)
    public void testOptimisticLockStillFailsWithForbiddenUpdate(Strategy strategy) {
        jdbc(con -> {
            assertThrows(SaveException.OptimisticLockError.class, () -> client(strategy)
                    .saveCommand(book(graphQLInActionId3, "Ignored input", 100)).forbidUpdate()
                    .setUpdateWhere(BookTable.class, (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE)))
                    .setOptimisticLock(BookTable.class, (table, values) -> table.price().gt(BigDecimal.valueOf(1000)))
                    .execute(con, BookFetcher.$.allScalarFields()));
        });
    }

    private JSqlClient client(Strategy strategy) {
        return getSqlClient(it -> it.setDialect(strategy == Strategy.PRESELECT ? DefaultDialect.INSTANCE : new H2Dialect() {
            @Override
            public boolean isUpsertReturningSupported() {
                return strategy == Strategy.RETURNING;
            }
        }));
    }

    private void assertConditionalSql(
            SaveMode mode, Strategy strategy, boolean forbidUpdate, boolean fetch, Book accepted, Book rejected, Book inserted
    ) {
        if (mode == SaveMode.UPDATE_ONLY) {
            assertEquals(fetch ? 2 : 1, getExecutions().size());
            assertEquals("update BOOK set NAME = ?, EDITION = ?, PRICE = ? where ID = ? and PRICE < ?",
                    getExecutions().get(0).getSql());
            assertEquals(3, getExecutions().get(0).getBatchCount());
            int index = 0;
            for (Book book : Arrays.asList(accepted, rejected, inserted)) {
                assertEquals(Arrays.asList(book.name(), book.edition(), book.price(), book.id(), book.price()),
                        getExecutions().get(0).getVariables(index++));
            }
        } else if (strategy == Strategy.PRESELECT) {
            assertEquals(fetch ? 6 : 5, getExecutions().size());
            assertEquals("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ where tb_1_.ID in (?, ?, ?)",
                    getExecutions().get(0).getSql());
            assertEquals(Arrays.asList(accepted.id(), rejected.id(), inserted.id()), getExecutions().get(0).getVariables(0));
            for (int index = 1; index <= 2; index++) {
                Book book = index == 1 ? accepted : rejected;
                assertEquals("select tb_1_.ID from BOOK tb_1_ where tb_1_.ID = ? and tb_1_.PRICE < ?",
                        getExecutions().get(index).getSql());
                assertEquals(Arrays.asList(book.id(), book.price()), getExecutions().get(index).getVariables(0));
            }
            assertEquals("insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)", getExecutions().get(3).getSql());
            assertEquals(Arrays.asList(inserted.id(), inserted.name(), inserted.edition(), inserted.price()),
                    getExecutions().get(3).getVariables(0));
            assertEquals("update BOOK set " +
                            (forbidUpdate ? "/* fake update to return all ids */ PRICE = PRICE" : "PRICE = ?") +
                            " where ID = ? and PRICE < ?", getExecutions().get(4).getSql());
            assertEquals(forbidUpdate ? Arrays.asList(accepted.id(), accepted.price()) :
                    Arrays.asList(accepted.price(), accepted.id(), accepted.price()), getExecutions().get(4).getVariables(0));
            getExecutions().forEach(execution -> assertEquals(1, execution.getBatchCount()));
        } else {
            boolean returning = strategy == Strategy.RETURNING;
            assertEquals(!returning && fetch ? 2 : 1, getExecutions().size());
            String columns = "ID" + (fetch ? ", NAME, EDITION" + (forbidUpdate ? ", PRICE" : "") : "");
            assertEquals((returning ? "select " + columns + " from final table (" : "") +
                    "merge into BOOK tb_1_ using(values(?, ?, ?, ?)" +
                    (returning ? ", (?, ?, ?, ?), (?, ?, ?, ?)" : "") +
                    ") tb_2_(ID, NAME, EDITION, PRICE) on tb_1_.ID = tb_2_.ID " +
                    "when matched and tb_1_.PRICE < tb_2_.PRICE then update set " +
                    (forbidUpdate ? "/* fake update to return all ids */ PRICE = tb_1_.PRICE" : "PRICE = tb_2_.PRICE") +
                    " when not matched then insert(ID, NAME, EDITION, PRICE) " +
                    "values(tb_2_.ID, tb_2_.NAME, tb_2_.EDITION, tb_2_.PRICE)" + (returning ? ")" : ""),
                    getExecutions().get(0).getSql());
            assertEquals(returning ? 1 : 3, getExecutions().get(0).getBatchCount());
            List<Object> variables = new ArrayList<>();
            int index = 0;
            for (Book book : Arrays.asList(accepted, rejected, inserted)) {
                List<Object> row = Arrays.asList(book.id(), book.name(), book.edition(), book.price());
                variables.addAll(row);
                if (!returning) {
                    assertEquals(row, getExecutions().get(0).getVariables(index++));
                }
            }
            if (returning) {
                assertEquals(variables, getExecutions().get(0).getVariables(0));
            }
        }
        if (fetch && (mode == SaveMode.UPDATE_ONLY || strategy != Strategy.RETURNING)) {
            Execution read = getExecutions().get(getExecutions().size() - 1);
            assertEquals("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION" + (forbidUpdate ? ", tb_1_.PRICE" : "") +
                    " from BOOK tb_1_ where tb_1_.ID " + (mode == SaveMode.UPDATE_ONLY ? "= ?" :
                    strategy == Strategy.PRESELECT ? "in (?, ?)" : "= any(?)"), read.getSql());
            List<?> ids = read.getVariables(0);
            if (mode != SaveMode.UPDATE_ONLY && strategy == Strategy.NO_RETURNING) {
                ids = (List<?>) ids.get(0);
            }
            assertEquals(mode == SaveMode.UPDATE_ONLY ? Arrays.asList(accepted.id()) : Arrays.asList(accepted.id(), inserted.id()), ids);
        }
    }

    private static SysUser user(Long id, String account) {
        return SysUserDraft.$.produce(draft -> {
            if (id != null) {
                draft.setId(id);
            }
            draft.setAccount(account).setEmail(account + "@example.com").setArea("test").setNickName(account);
        });
    }

    private static Book book(UUID id, String name, int price) {
        return BookDraft.$.produce(draft -> draft.setId(id).setName(name).setEdition(9).setPrice(BigDecimal.valueOf(price)));
    }

    private static Stream<Arguments> conditionalCases() {
        return Stream.of(SaveMode.UPSERT, SaveMode.NON_IDEMPOTENT_UPSERT, SaveMode.UPDATE_ONLY)
                .flatMap(mode -> Arrays.stream(Strategy.values()).flatMap(strategy ->
                        Stream.of(false, true).flatMap(forbidUpdate -> Stream.of(false, true)
                                .map(fetch -> Arguments.of(mode, strategy, forbidUpdate, fetch)))));
    }

    private static Stream<Arguments> insertModes() {
        return Stream.of(SaveMode.INSERT_ONLY, SaveMode.INSERT_IF_ABSENT).flatMap(mode ->
                Stream.of(false, true).map(modeFirst -> Arguments.of(mode, modeFirst)));
    }

    private static Stream<Arguments> conflictCases() {
        return Stream.of(SaveMode.UPSERT, SaveMode.INSERT_IF_ABSENT).flatMap(mode ->
                Arrays.stream(Strategy.values()).flatMap(strategy ->
                        Stream.of(false, true).map(suppliedId -> Arguments.of(mode, strategy, suppliedId))));
    }

    private static Stream<Arguments> associationCases() {
        return Arrays.stream(Strategy.values()).flatMap(strategy ->
                Stream.of(false, true).map(accepted -> Arguments.of(strategy, accepted)));
    }

    enum Strategy {
        RETURNING,
        NO_RETURNING,
        PRESELECT
    }
}
