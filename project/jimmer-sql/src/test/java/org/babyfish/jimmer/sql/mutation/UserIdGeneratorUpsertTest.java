package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class UserIdGeneratorUpsertTest extends AbstractMutationTest {

    @ParameterizedTest
    @MethodSource("modes")
    public void testBatch(SaveMode mode, boolean returning, boolean fetch) {
        List<UUID> generatedIds = new ArrayList<>();
        JSqlClient client = client(returning, generatedIds);
        Book existing = book("GraphQL in Action", BigDecimal.ONE);
        Book inserted = book("New book", BigDecimal.TEN);
        jdbc(con -> {
            BatchEntitySaveCommand<Book> command = client.saveEntitiesCommand(Arrays.asList(existing, inserted)).setMode(mode);
            BatchSaveResult<Book> result = fetch ? command.execute(con, BookFetcher.$.allScalarFields()) : command.execute(con);
            boolean update = mode != SaveMode.INSERT_IF_ABSENT;
            assertEquals(update, result.getItems().get(0).isAccepted());
            if (update) {
                assertEquals(Constants.graphQLInActionId3, result.getItems().get(0).getModifiedEntity().id());
            } else {
                assertFalse(ImmutableObjects.isLoaded(result.getItems().get(0).getModifiedEntity(), BookProps.ID));
            }
            assertTrue(result.getItems().get(1).isAccepted());
            assertEquals(2, generatedIds.size());
            assertEquals(generatedIds.get(1), result.getItems().get(1).getModifiedEntity().id());
            assertEquals(update ? 2 : 1, result.getTotalAffectedRowCount());
            assertNativeMerge();
            assertFalse(ImmutableObjects.isLoaded(existing, BookProps.ID));
            assertFalse(ImmutableObjects.isLoaded(inserted, BookProps.ID));
            Book stored = client.getEntities().forConnection(con).findById(Book.class, Constants.graphQLInActionId3);
            assertEquals(new BigDecimal(update ? "1.00" : "80.00"), stored.price());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testForbidUpdateAndDuplicateKeys(boolean returning) {
        List<UUID> generatedIds = new ArrayList<>();
        JSqlClient client = client(returning, generatedIds);
        Book existing = book("GraphQL in Action", BigDecimal.ONE);
        Book duplicate = book("GraphQL in Action", BigDecimal.TEN);
        jdbc(con -> {
            BatchSaveResult<Book> result = client.saveEntitiesCommand(Arrays.asList(existing, duplicate))
                    .forbidUpdate().execute(con, BookFetcher.$.allScalarFields());
            assertEquals(1, generatedIds.size());
            for (BatchSaveResult.Item<Book> item : result.getItems()) {
                assertTrue(item.isAccepted());
                assertEquals(Constants.graphQLInActionId3, item.getModifiedEntity().id());
                assertEquals(new BigDecimal("80.00"), item.getModifiedEntity().price());
            }
            assertEquals(returning ? 1 : 0, result.getTotalAffectedRowCount());
            assertNativeMerge();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testUuidGenerator(boolean explicitKeyMatching) {
        jdbc(con -> {
            SimpleEntitySaveCommand<Book> command = getSqlClient(it -> it.setIdGenerator(Book.class, new UUIDIdGenerator()))
                    .saveCommand(book("GraphQL in Action", BigDecimal.ONE));
            if (explicitKeyMatching) {
                command = command.matchByKey();
            }
            SimpleSaveResult<Book> result = command.execute(con);
            assertTrue(result.isAccepted());
            assertEquals(Constants.graphQLInActionId3, result.getModifiedEntity().id());
            assertNativeMerge();
            assertEquals(1, getExecutions().size());
        });
    }

    @Test
    public void testSuppliedIdDoesNotInvokeGenerator() {
        List<UUID> generatedIds = new ArrayList<>();
        Book input = BookDraft.$.produce(book("Renamed book", BigDecimal.ONE), draft -> draft.setId(Constants.graphQLInActionId3));
        jdbc(con -> {
            SimpleSaveResult<Book> result = client(true, generatedIds).saveCommand(input).execute(con);
            assertTrue(generatedIds.isEmpty());
            assertEquals(input.id(), result.getModifiedEntity().id());
            assertTrue(getExecutions().get(0).getSql().contains("key(ID)"));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testUpdateWhereRejectsUnusedId(boolean returning) {
        List<UUID> generatedIds = new ArrayList<>();
        jdbc(con -> {
            SimpleSaveResult<Book> result = client(returning, generatedIds)
                    .saveCommand(book("GraphQL in Action", BigDecimal.ONE))
                    .setUpdateWhere(BookTable.class, (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE)))
                    .execute(con);
            assertFalse(result.isAccepted());
            assertFalse(ImmutableObjects.isLoaded(result.getModifiedEntity(), BookProps.ID));
            assertEquals(0, result.getTotalAffectedRowCount());
            assertEquals(1, generatedIds.size());
            assertNativeMerge();
        });
    }

    @Test
    public void testMixedBatchKeepsInputMatching() {
        List<UUID> generatedIds = new ArrayList<>();
        Book byId = BookDraft.$.produce(book("Renamed book", BigDecimal.ONE), draft -> draft.setId(Constants.graphQLInActionId3));
        Book byKey = BookDraft.$.produce(book("GraphQL in Action", BigDecimal.TEN), draft -> draft.setEdition(2));
        jdbc(con -> {
            BatchSaveResult<Book> result = client(true, generatedIds).saveEntitiesCommand(Arrays.asList(byId, byKey)).execute(con);
            assertEquals(1, generatedIds.size());
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(0).getModifiedEntity().id());
            assertEquals(Constants.graphQLInActionId2, result.getItems().get(1).getModifiedEntity().id());
            assertEquals(2, getExecutions().size());
            assertTrue(getExecutions().get(0).getSql().contains("key(ID)"));
            assertTrue(getExecutions().get(1).getSql().contains("on tb_1_.NAME = tb_2_.NAME and tb_1_.EDITION = tb_2_.EDITION"));
        });
    }

    @Test
    public void testDialectWithoutExactConflictTargetKeepsPreselection() {
        List<Long> generatedIds = new ArrayList<>();
        JSqlClient client = getSqlClient(builder -> builder
                .setDialect(new H2Dialect() {
                    @Override
                    public boolean isUpsertWithMultipleUniqueConstraintSupported() {
                        return false;
                    }
                })
                .setIdGenerator(Department.class, (UserIdGenerator<Long>) type -> {
                    generatedIds.add(100L);
                    return 100L;
                }));
        jdbc(con -> {
            SimpleSaveResult<Department> result = client.saveCommand(DepartmentDraft.$.produce(draft -> draft.setName("Market")))
                    .execute(con);
            assertTrue(result.isAccepted());
            assertEquals(1L, result.getModifiedEntity().id());
            assertTrue(generatedIds.isEmpty());
            assertTrue(getExecutions().get(0).getSql().startsWith("select "));
            assertEquals(ExecutionPurpose.command(QueryReason.NO_MORE_UNIQUE_CONSTRAINTS_REQUIRED), getExecutions().get(0).getPurpose());
        });
    }

    @ParameterizedTest
    @MethodSource("cascadeModes")
    public void testCascadeResolvesMixedAndDuplicateKeys(boolean returning, AssociatedSaveMode mode) {
        List<UUID> generatedIds = new ArrayList<>();
        JSqlClient client = client(returning, generatedIds);
        Author input = AuthorDraft.$.produce(draft -> {
            draft.setId(Constants.danId);
            draft.setBooks(Arrays.asList(
                    book("GraphQL in Action", BigDecimal.ONE),
                    book("New book", BigDecimal.TEN),
                    book("GraphQL in Action", BigDecimal.ONE)
            ));
        });
        jdbc(con -> {
            Author result = client.saveCommand(input).setAssociatedMode(AuthorProps.BOOKS, mode).execute(con).getModifiedEntity();
            assertEquals(2, generatedIds.size());
            assertEquals(3, result.books().size());
            assertEquals(Constants.graphQLInActionId3, result.books().get(0).id());
            assertEquals(generatedIds.get(1), result.books().get(1).id());
            assertEquals(Constants.graphQLInActionId3, result.books().get(2).id());
            String upsert = getExecutions().stream().map(it -> it.getSql())
                    .filter(sql -> sql.contains("merge into BOOK ")).findFirst().orElseThrow(AssertionError::new);
            assertEquals(returning, upsert.startsWith("select ID, NAME, EDITION from final table ("));
            long idQueries = getExecutions().stream().map(it -> it.getSql())
                    .filter(sql -> sql.startsWith("select ") && sql.contains("from BOOK tb_1_")).count();
            // DO NOTHING still needs a lookup for existing rows, which are absent from RETURNING.
            assertEquals(returning && mode == AssociatedSaveMode.MERGE ? 0 : 1, idQueries);
            Author stored = client.getEntities().forConnection(con).findById(
                    AuthorFetcher.$.books(BookFetcher.$.price()), Constants.danId
            );
            assertTrue(stored.books().stream().anyMatch(book -> book.id().equals(generatedIds.get(1))));
            Book existing = stored.books().stream().filter(book -> book.id().equals(Constants.graphQLInActionId3))
                    .findFirst().orElseThrow(AssertionError::new);
            assertEquals(new BigDecimal(mode == AssociatedSaveMode.MERGE ? "1.00" : "80.00"), existing.price());
        });
    }

    private static Stream<Arguments> cascadeModes() {
        return Stream.of(false, true).flatMap(returning ->
                Stream.of(AssociatedSaveMode.MERGE, AssociatedSaveMode.APPEND_IF_ABSENT).map(mode -> Arguments.of(returning, mode))
        );
    }

    private JSqlClient client(boolean returning, List<UUID> generatedIds) {
        return getSqlClient(builder -> builder
                .setDialect(new H2Dialect() {
                    @Override
                    public boolean isUpsertReturningSupported() {
                        return returning;
                    }

                    @Override
                    public boolean isNoIdUpsertSupported() {
                        return false;
                    }
                })
                .setIdGenerator(Book.class, (UserIdGenerator<UUID>) type -> {
                    assertEquals(Book.class, type);
                    UUID id = UUID.randomUUID();
                    generatedIds.add(id);
                    return id;
                }));
    }

    private void assertNativeMerge() {
        String sql = getExecutions().get(0).getSql();
        assertTrue(sql.contains("merge into BOOK "), sql);
        assertTrue(sql.contains("on tb_1_.NAME = tb_2_.NAME and tb_1_.EDITION = tb_2_.EDITION"), sql);
        assertFalse(sql.contains("tb_1_.ID = tb_2_.ID"), sql);
    }

    private static Book book(String name, BigDecimal price) {
        return BookDraft.$.produce(draft -> {
            draft.setName(name);
            draft.setEdition(3);
            draft.setPrice(price);
        });
    }

    private static Stream<Arguments> modes() {
        return Stream.of(SaveMode.UPSERT, SaveMode.NON_IDEMPOTENT_UPSERT, SaveMode.INSERT_IF_ABSENT)
                .flatMap(mode -> Stream.of(false, true)
                        .flatMap(returning -> Stream.of(false, true).map(fetch -> Arguments.of(mode, returning, fetch))));
    }
}
