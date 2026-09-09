package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftPreProcessor;
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
import org.junit.jupiter.params.provider.EnumSource;
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
            assertEquals(returning || !update ? 1 : 2, getExecutions().size());
            if (!returning && update) {
                assertEquals(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ " +
                                "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))",
                        getExecutions().get(1).getSql()
                );
            }
            assertEquals(update, getExecutions().get(0).getSql().contains("when matched"));
            assertBookBatch(returning, generatedIds);
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
            assertEquals(returning ? 1 : 2, getExecutions().size());
            if (!returning) {
                assertTrue(getExecutions().get(1).getSql().contains("tb_1_.PRICE"));
                assertTrue(getExecutions().get(1).getSql().contains("(tb_1_.NAME, tb_1_.EDITION) = (?, ?)"));
            }
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
            assertExecutedSql("merge into BOOK(ID, NAME, EDITION, PRICE) key(ID) values(?, ?, ?, ?)");
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
            assertEquals(1, getExecutions().size());
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
            assertEquals(1, getExecutions().size());
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
            Author before = client.getEntities().forConnection(con).findById(AuthorFetcher.$.books(), Constants.danId);
            clearExecutions();
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
            assertEquals(2 + idQueries, getExecutions().size());
            assertEquals(upsert, getExecutions().get(0).getSql());
            assertBookBatch(returning, generatedIds);
            if (idQueries != 0) {
                assertEquals(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ " +
                                (mode == AssociatedSaveMode.APPEND_IF_ABSENT ?
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)" :
                                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"),
                        getExecutions().get(1).getSql()
                );
            }
            Execution mapping = getExecutions().get(getExecutions().size() - 1);
            assertEquals(
                    "merge into BOOK_AUTHOR_MAPPING tb_1_ using(values(?, ?)) tb_2_(AUTHOR_ID, BOOK_ID) " +
                            "on tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID and tb_1_.BOOK_ID = tb_2_.BOOK_ID " +
                            "when not matched then insert(AUTHOR_ID, BOOK_ID) values(tb_2_.AUTHOR_ID, tb_2_.BOOK_ID)",
                    mapping.getSql()
            );
            assertEquals(2, mapping.getBatchCount());
            assertEquals(Arrays.asList(Constants.danId, Constants.graphQLInActionId3), mapping.getVariables(0));
            assertEquals(Arrays.asList(Constants.danId, generatedIds.get(1)), mapping.getVariables(1));
            Author stored = client.getEntities().forConnection(con).findById(
                    AuthorFetcher.$.books(BookFetcher.$.price()), Constants.danId
            );
            Set<UUID> storedIds = new HashSet<>();
            stored.books().forEach(book -> storedIds.add(book.id()));
            before.books().forEach(book -> assertTrue(storedIds.contains(book.id()), "Existing association must be preserved"));
            assertEquals(before.books().size() + 2, stored.books().size());
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

    @ParameterizedTest
    @MethodSource("rejectedGraphModes")
    public void testRejectedInsertIfAbsentSkipsDependentGraph(boolean returning, boolean nested) {
        List<UUID> generatedIds = new ArrayList<>();
        JSqlClient client = client(returning, generatedIds);
        UUID parentId = UUID.randomUUID();
        UUID descendantId = UUID.randomUUID();
        Book input = BookDraft.$.produce(book("GraphQL in Action", BigDecimal.ONE), draft -> {
            draft.addIntoAuthors(author -> author.setId(Constants.sammerId).setFirstName("Must not change"));
            draft.addIntoAuthors(author -> author.setId(descendantId)
                    .setFirstName("Must not").setLastName("Insert").setGender(Gender.MALE));
        });
        jdbc(con -> {
            Book before = client.getEntities().forConnection(con)
                    .findById(BookFetcher.$.price().authors(AuthorFetcher.$.allScalarFields()), Constants.graphQLInActionId3);
            clearExecutions();
            if (nested) {
                Author parent = AuthorDraft.$.produce(draft -> draft.setId(parentId)
                        .setFirstName("New").setLastName("Parent").setGender(Gender.MALE)
                        .setBooks(Collections.singletonList(input)));
                SimpleSaveResult<Author> result = client.saveCommand(parent).setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedMode(AuthorProps.BOOKS, AssociatedSaveMode.APPEND_IF_ABSENT).execute(con);
                assertTrue(result.isAccepted());
                assertEquals(2, result.getTotalAffectedRowCount());
                assertEquals(Constants.graphQLInActionId3, result.getModifiedEntity().books().get(0).id());
            } else {
                SimpleSaveResult<Book> result = client.saveCommand(input).setMode(SaveMode.INSERT_IF_ABSENT).execute(con);
                assertFalse(result.isAccepted());
                assertEquals(0, result.getTotalAffectedRowCount());
                assertFalse(ImmutableObjects.isLoaded(result.getModifiedEntity(), BookProps.ID));
            }
            assertEquals(1, generatedIds.size());
            assertEquals(nested ? 4 : 1, getExecutions().size());
            int bookIndex = nested ? 1 : 0;
            String bookSql = getExecutions().get(bookIndex).getSql();
            assertTrue(bookSql.contains("merge into BOOK "), bookSql);
            assertFalse(bookSql.contains("when matched"), bookSql);
            assertEquals(Arrays.asList(generatedIds.get(0), "GraphQL in Action", 3, BigDecimal.ONE),
                    getExecutions().get(bookIndex).getVariables(0));
            if (nested) {
                assertEquals("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?)",
                        getExecutions().get(0).getSql());
                assertEquals("select tb_1_.ID from BOOK tb_1_ where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)",
                        getExecutions().get(2).getSql());
                assertEquals("insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) values(?, ?)",
                        getExecutions().get(3).getSql());
                assertEquals(Collections.singletonList(Constants.graphQLInActionId3), client.getEntities().forConnection(con)
                        .findById(AuthorFetcher.$.books(), parentId).books().stream().map(Book::id)
                        .collect(java.util.stream.Collectors.toList()));
            }
            Book stored = client.getEntities().forConnection(con)
                    .findById(BookFetcher.$.price().authors(AuthorFetcher.$.allScalarFields()), Constants.graphQLInActionId3);
            assertEquals(before.price(), stored.price());
            assertEquals(before.authors().size() + (nested ? 1 : 0), stored.authors().size());
            assertTrue(stored.authors().containsAll(before.authors()));
            assertNull(client.getEntities().forConnection(con).findById(Author.class, descendantId));
        });
    }

    private static Stream<Arguments> rejectedGraphModes() {
        return Stream.of(false, true).flatMap(returning -> Stream.of(false, true).map(nested -> Arguments.of(returning, nested)));
    }

    @ParameterizedTest
    @EnumSource(value = AssociatedSaveMode.class, names = {"MERGE", "APPEND_IF_ABSENT", "REPLACE"})
    public void testOwningSideDeduplicatesLinks(AssociatedSaveMode mode) {
        JSqlClient client = getSqlClient();
        Book input = BookDraft.$.produce(draft -> draft.setId(Constants.graphQLInActionId3)
                .setAuthorIds(Arrays.asList(Constants.danId, Constants.borisId, Constants.danId)));
        jdbc(con -> {
            SimpleSaveResult<Book> result = client.saveCommand(input).setAssociatedMode(BookProps.AUTHORS, mode).execute(con);
            boolean replace = mode == AssociatedSaveMode.REPLACE;
            assertEquals(replace ? 2 : 1, getExecutions().size());
            if (replace) {
                assertEquals("delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID not in (?, ?)",
                        getExecutions().get(0).getSql());
                assertEquals(Arrays.asList(Constants.graphQLInActionId3, Constants.danId, Constants.borisId),
                        getExecutions().get(0).getVariables(0));
            }
            Execution mapping = getExecutions().get(replace ? 1 : 0);
            assertEquals(
                    "merge into BOOK_AUTHOR_MAPPING tb_1_ using(values(?, ?)) tb_2_(BOOK_ID, AUTHOR_ID) " +
                            "on tb_1_.BOOK_ID = tb_2_.BOOK_ID and tb_1_.AUTHOR_ID = tb_2_.AUTHOR_ID " +
                            "when not matched then insert(BOOK_ID, AUTHOR_ID) values(tb_2_.BOOK_ID, tb_2_.AUTHOR_ID)",
                    mapping.getSql()
            );
            assertEquals(2, mapping.getBatchCount());
            assertEquals(Arrays.asList(Constants.graphQLInActionId3, Constants.danId), mapping.getVariables(0));
            assertEquals(Arrays.asList(Constants.graphQLInActionId3, Constants.borisId), mapping.getVariables(1));
            assertEquals(3, result.getModifiedEntity().authors().size());
            assertEquals(replace ? 3 : 2, result.getTotalAffectedRowCount());
            Book stored = client.getEntities().forConnection(con).findById(BookFetcher.$.authors(), Constants.graphQLInActionId3);
            Set<UUID> expectedIds = new HashSet<>(Arrays.asList(Constants.danId, Constants.borisId));
            if (!replace) {
                expectedIds.add(Constants.sammerId);
            }
            assertEquals(expectedIds, stored.authors().stream().map(Author::id).collect(java.util.stream.Collectors.toSet()));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testPreProcessorKeepsNativeBatch(boolean returning) {
        List<UUID> generatedIds = new ArrayList<>();
        JSqlClient client = client(returning, generatedIds, builder -> builder.addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
            @Override
            public void beforeSave(BookDraft draft) {
                draft.setPrice(BigDecimal.TEN);
            }
        }));
        Book existing = BookDraft.$.produce(draft -> draft.setName("GraphQL in Action").setEdition(3));
        Book inserted = BookDraft.$.produce(draft -> draft.setName("New book").setEdition(3));
        jdbc(con -> {
            BatchSaveResult<Book> result = client.saveEntitiesCommand(Arrays.asList(existing, inserted)).execute(con);
            assertEquals(returning ? 1 : 2, getExecutions().size());
            assertNativeMerge();
            assertEquals(2, generatedIds.size());
            assertEquals(returning ? 1 : 2, getExecutions().get(0).getBatchCount());
            if (!returning) {
                assertEquals("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ " +
                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))", getExecutions().get(1).getSql());
            }
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(0).getModifiedEntity().id());
            assertEquals(generatedIds.get(1), result.getItems().get(1).getModifiedEntity().id());
            assertEquals(2, result.getTotalAffectedRowCount());
            assertFalse(ImmutableObjects.isLoaded(existing, BookProps.PRICE));
            for (BatchSaveResult.Item<Book> item : result.getItems()) {
                assertEquals(0, BigDecimal.TEN.compareTo(client.getEntities().forConnection(con)
                        .findById(Book.class, item.getModifiedEntity().id()).price()));
            }
        });
    }

    @ParameterizedTest
    @MethodSource("versionModes")
    public void testKeyUpsertReturnsActualVersion(boolean returning, boolean existing, boolean nested) {
        JSqlClient client = getSqlClient(builder -> builder
                .setDialect(new H2Dialect() {
                    @Override
                    public boolean isUpsertReturningSupported() {
                        return returning;
                    }
                })
                .setIdGenerator(BookStore.class, new UUIDIdGenerator()));
        BookStore store = BookStoreDraft.$.produce(draft -> {
            draft.setName(existing ? "MANNING" : "New store");
            draft.setWebsite("https://store.example");
        });
        jdbc(con -> {
            try (java.sql.Statement statement = con.createStatement()) {
                statement.executeUpdate("update BOOK_STORE set VERSION = 7 where NAME = 'MANNING'");
            }
            BookStore result;
            if (nested) {
                Book input = BookDraft.$.produce(draft -> {
                    draft.setId(Constants.graphQLInActionId3);
                    draft.setPrice(BigDecimal.ONE);
                    draft.setStore(store);
                });
                result = client.saveCommand(input).execute(con).getModifiedEntity().store();
            } else {
                result = client.saveCommand(store).execute(con).getModifiedEntity();
            }
            assertEquals(existing ? 7 : 0, result.version());
            assertFalse(ImmutableObjects.isLoaded(store, BookStoreProps.VERSION));
            assertEquals((returning ? 1 : 2) + (nested ? 1 : 0), getExecutions().size());
            if (existing) {
                assertEquals(Constants.manningId, result.id());
            }
        });
    }

    private static Stream<Arguments> versionModes() {
        return Stream.of(false, true).flatMap(returning -> Stream.of(false, true)
                .flatMap(existing -> Stream.of(false, true).map(nested -> Arguments.of(returning, existing, nested))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testForbidUpdateFetchesMixedBatch(boolean returning) {
        List<UUID> generatedIds = new ArrayList<>();
        jdbc(con -> {
            BatchSaveResult<Book> result = client(returning, generatedIds).saveEntitiesCommand(Arrays.asList(
                    book("GraphQL in Action", BigDecimal.ONE),
                    book("New book", BigDecimal.TEN),
                    book("GraphQL in Action", BigDecimal.ONE)
            )).forbidUpdate().execute(con, BookFetcher.$.allScalarFields());
            assertEquals(returning ? 1 : 2, getExecutions().size());
            assertEquals(2, generatedIds.size());
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(0).getModifiedEntity().id());
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(2).getModifiedEntity().id());
            assertEquals(new BigDecimal("80.00"), result.getItems().get(0).getModifiedEntity().price());
            assertEquals(new BigDecimal("80.00"), result.getItems().get(2).getModifiedEntity().price());
            assertEquals(generatedIds.get(1), result.getItems().get(1).getModifiedEntity().id());
            assertEquals(0, BigDecimal.TEN.compareTo(result.getItems().get(1).getModifiedEntity().price()));
            assertTrue(result.getItems().stream().allMatch(BatchSaveResult.Item::isAccepted));
            assertEquals(returning ? 2 : 1, result.getTotalAffectedRowCount());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testForbidUpdateReturnsDatabaseVersion(boolean returning) {
        JSqlClient client = getSqlClient(builder -> builder.setDialect(new H2Dialect() {
            @Override
            public boolean isUpsertReturningSupported() {
                return returning;
            }
        }));
        BookStore input = BookStoreDraft.$.produce(draft -> {
            draft.setId(UUID.randomUUID());
            draft.setName("MANNING");
            draft.setVersion(100);
        });
        jdbc(con -> {
            try (java.sql.Statement statement = con.createStatement()) {
                statement.executeUpdate("update BOOK_STORE set VERSION = 7 where NAME = 'MANNING'");
            }
            BookStore result = client.saveCommand(input).matchByKey().setVersionMode(VersionMode.ASSIGNMENT)
                    .forbidUpdate().execute(con).getModifiedEntity();
            assertEquals(Constants.manningId, result.id());
            assertEquals(7, result.version());
            assertEquals(100, input.version());
            assertEquals(returning ? 1 : 2, getExecutions().size());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testInsertIfAbsentVersion(boolean returning) {
        JSqlClient client = getSqlClient(builder -> builder
                .setDialect(new H2Dialect() {
                    @Override
                    public boolean isUpsertReturningSupported() {
                        return returning;
                    }
                })
                .setIdGenerator(BookStore.class, new UUIDIdGenerator()));
        jdbc(con -> {
            BatchSaveResult<BookStore> result = client.saveEntitiesCommand(Arrays.asList(
                    BookStoreDraft.$.produce(draft -> draft.setName("MANNING")),
                    BookStoreDraft.$.produce(draft -> draft.setName("New store"))
            )).setMode(SaveMode.INSERT_IF_ABSENT).execute(con);
            assertEquals(1, getExecutions().size());
            assertFalse(result.getItems().get(0).isAccepted());
            assertFalse(ImmutableObjects.isLoaded(result.getItems().get(0).getModifiedEntity(), BookStoreProps.VERSION));
            assertTrue(result.getItems().get(1).isAccepted());
            assertEquals(0, result.getItems().get(1).getModifiedEntity().version());
        });
    }

    private JSqlClient client(boolean returning, List<UUID> generatedIds) {
        return client(returning, generatedIds, builder -> {});
    }

    private JSqlClient client(boolean returning, List<UUID> generatedIds, java.util.function.Consumer<JSqlClient.Builder> configure) {
        return getSqlClient(builder -> {
            builder.setDialect(new H2Dialect() {
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
                });
            configure.accept(builder);
        });
    }

    private void assertNativeMerge() {
        String sql = getExecutions().get(0).getSql();
        assertTrue(sql.contains("merge into BOOK "), sql);
        assertTrue(sql.contains("on tb_1_.NAME = tb_2_.NAME and tb_1_.EDITION = tb_2_.EDITION"), sql);
        assertFalse(sql.contains("tb_1_.ID = tb_2_.ID"), sql);
    }

    private void assertBookBatch(boolean returning, List<UUID> ids) {
        Execution upsert = getExecutions().get(0);
        assertEquals(returning ? 1 : 2, upsert.getBatchCount());
        if (returning) {
            assertEquals(Arrays.asList(ids.get(0), "GraphQL in Action", 3, BigDecimal.ONE,
                    ids.get(1), "New book", 3, BigDecimal.TEN), upsert.getVariables(0));
        } else {
            assertEquals(Arrays.asList(ids.get(0), "GraphQL in Action", 3, BigDecimal.ONE), upsert.getVariables(0));
            assertEquals(Arrays.asList(ids.get(1), "New book", 3, BigDecimal.TEN), upsert.getVariables(1));
        }
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

    private void assertExecutedSql(String... sql) {
        assertArrayEquals(sql, getExecutions().stream().map(Execution::getSql).toArray(String[]::new));
    }
}
