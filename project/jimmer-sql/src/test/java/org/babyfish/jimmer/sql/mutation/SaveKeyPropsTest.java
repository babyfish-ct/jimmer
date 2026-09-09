package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SaveKeyPropsTest extends AbstractMutationTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testExplicitKeysWithLoadedIdAndForbiddenUpdate(boolean inferKeys) {
        Book input = BookDraft.$.produce(draft -> {
            draft.setId(UUID.randomUUID());
            draft.setName("GraphQL in Action");
            draft.setEdition(3);
            draft.setPrice(BigDecimal.ONE);
        });
        jdbc(con -> {
            SimpleSaveResult<Book> result = keyCommand(input, inferKeys)
                    .forbidUpdate()
                    .execute(con, BookFetcher.$.allScalarFields());
            assertSame(input, result.getOriginalEntity());
            assertEquals(Constants.graphQLInActionId3, result.getModifiedEntity().id());
            assertEquals(new BigDecimal("80.00"), result.getModifiedEntity().price());
            assertTrue(result.isAccepted());
            // The matched arm performs a fake update to return the existing row atomically.
            assertEquals(1, result.getTotalAffectedRowCount());
            assertNativeKeyMerge(1);
        });
    }

    @ParameterizedTest
    @MethodSource("matchingModes")
    public void testExplicitKeysWithLoadedId(SaveMode mode, boolean inferKeys) {
        Book input = book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.ONE);
        jdbc(con -> {
            SimpleSaveResult<Book> result = keyCommand(input, inferKeys)
                    .setMode(mode)
                    .execute(con, BookFetcher.$.allScalarFields());
            boolean updated = mode != SaveMode.INSERT_IF_ABSENT;
            assertEquals(updated ? Constants.graphQLInActionId3 : input.id(), result.getModifiedEntity().id());
            assertEquals(updated, result.isAccepted());
            assertEquals(updated ? 1 : 0, result.getTotalAffectedRowCount());
            if (mode != SaveMode.UPDATE_ONLY) {
                assertNativeKeyMerge(1);
            }
            Book stored = getSqlClient().getEntities().forConnection(con).findById(Book.class, Constants.graphQLInActionId3);
            assertEquals(new BigDecimal(updated ? "1.00" : "80.00"), stored.price());
        });
    }

    @ParameterizedTest
    @MethodSource("insertingModes")
    public void testInsertPreservesSuppliedId(SaveMode mode, boolean inferKeys) {
        Book input = book(UUID.randomUUID(), "New book", BigDecimal.ONE);
        jdbc(con -> {
            SimpleSaveResult<Book> result = keyCommand(input, inferKeys)
                    .setMode(mode)
                    .execute(con);
            assertEquals(input.id(), result.getModifiedEntity().id());
            assertTrue(result.isAccepted());
            assertEquals(1, result.getTotalAffectedRowCount());
            if (mode != SaveMode.INSERT_ONLY) {
                assertNativeKeyMerge(1);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testBatchWithExistingAndNewKeys(boolean inferKeys) {
        Book existing = book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.ONE);
        Book inserted = book(UUID.randomUUID(), "New book", BigDecimal.TEN);
        jdbc(con -> {
            BatchSaveResult<Book> result = keyBatchCommand(Arrays.asList(existing, inserted), inferKeys)
                    .forbidUpdate()
                    .execute(con, BookFetcher.$.allScalarFields());
            assertEquals(2, result.getItems().size());
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(0).getModifiedEntity().id());
            assertEquals(new BigDecimal("80.00"), result.getItems().get(0).getModifiedEntity().price());
            assertEquals(inserted.id(), result.getItems().get(1).getModifiedEntity().id());
            assertEquals(2, result.getTotalAffectedRowCount());
            assertNativeKeyMerge(1);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testUpdateDoesNotFallBackToIdWhenKeyDoesNotMatch(boolean inferKeys) {
        Book input = book(Constants.graphQLInActionId3, "Unknown book", BigDecimal.ONE);
        jdbc(con -> {
            SimpleSaveResult<Book> result = keyCommand(input, inferKeys)
                    .setMode(SaveMode.UPDATE_ONLY)
                    .execute(con);
            assertFalse(result.isAccepted());
            assertEquals(0, result.getTotalAffectedRowCount());
            assertEquals("GraphQL in Action", getSqlClient().getEntities().forConnection(con)
                    .findById(Book.class, input.id()).name());
        });
    }

    @Test
    public void testExplicitKeysDoNotEnableKeyMatching() {
        Book input = book(Constants.graphQLInActionId3, "Renamed book", BigDecimal.ONE);
        jdbc(con -> {
            SimpleSaveResult<Book> result = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveCommand(input)
                    .setKeyProps(BookProps.NAME, BookProps.EDITION)
                    .execute(con);
            assertTrue(result.isAccepted());
            assertEquals(input.id(), result.getModifiedEntity().id());
            assertEquals(1, result.getTotalAffectedRowCount());
            assertEquals("Renamed book", getSqlClient().getEntities().forConnection(con).findById(Book.class, input.id()).name());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testMatchWithoutFetcher(boolean returningSupported) {
        Book input = book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.ONE);
        jdbc(con -> {
            SimpleSaveResult<Book> result = getSqlClient(it -> it.setDialect(keyDialect(returningSupported)))
                    .saveCommand(input).matchByKey().forbidUpdate().execute(con);
            assertTrue(result.isAccepted());
            assertEquals(Constants.graphQLInActionId3, result.getModifiedEntity().id());
            assertEquals(returningSupported ? 1 : 0, result.getTotalAffectedRowCount());
            assertNativeKeyMerge(returningSupported ? 1 : 2);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testBatchDoesNotDeduplicateBySuppliedId(boolean returningSupported) {
        Book first = book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.ONE);
        Book second = BookDraft.$.produce(first, draft -> draft.setEdition(2));
        jdbc(con -> {
            BatchSaveResult<Book> result = getSqlClient(it -> it.setDialect(keyDialect(returningSupported)))
                    .saveEntitiesCommand(Arrays.asList(first, second)).matchByKey().execute(con);
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(0).getModifiedEntity().id());
            assertEquals(Constants.graphQLInActionId2, result.getItems().get(1).getModifiedEntity().id());
            assertTrue(result.getItems().stream().allMatch(BatchSaveResult.Item::isAccepted));
            assertEquals(2, result.getTotalAffectedRowCount());
            assertNativeKeyMerge(returningSupported ? 1 : 2);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testBatchDeduplicatesByKey(boolean returningSupported) {
        Book first = book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.ONE);
        Book second = book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.ONE);
        jdbc(con -> {
            BatchSaveResult<Book> result = getSqlClient(it -> it.setDialect(keyDialect(returningSupported)))
                    .saveEntitiesCommand(Arrays.asList(first, second)).matchByKey().execute(con);
            for (BatchSaveResult.Item<Book> item : result.getItems()) {
                assertTrue(item.isAccepted());
                assertEquals(Constants.graphQLInActionId3, item.getModifiedEntity().id());
            }
            assertEquals(1, result.getTotalAffectedRowCount());
            assertNativeKeyMerge(returningSupported ? 1 : 2);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testInsertIfAbsentBatchByKey(boolean returningSupported) {
        Book existing = book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.ONE);
        Book inserted = book(UUID.randomUUID(), "New book", BigDecimal.TEN);
        jdbc(con -> {
            BatchSaveResult<Book> result = getSqlClient(it -> it.setDialect(keyDialect(returningSupported)))
                    .saveEntitiesCommand(Arrays.asList(existing, inserted)).matchByKey()
                    .setMode(SaveMode.INSERT_IF_ABSENT).execute(con);
            assertFalse(result.getItems().get(0).isAccepted());
            assertEquals(existing.id(), result.getItems().get(0).getModifiedEntity().id());
            assertTrue(result.getItems().get(1).isAccepted());
            assertEquals(inserted.id(), result.getItems().get(1).getModifiedEntity().id());
            assertEquals(1, result.getTotalAffectedRowCount());
            assertNativeKeyMerge(1);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testUpdateWhereByKey(boolean returningSupported) {
        Book accepted = book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.valueOf(100));
        Book duplicate = BookDraft.$.produce(accepted, draft -> draft.setId(UUID.randomUUID()));
        Book rejected = BookDraft.$.produce(accepted, draft -> {
            draft.setId(UUID.randomUUID());
            draft.setEdition(2);
            draft.setPrice(BigDecimal.ONE);
        });
        jdbc(con -> {
            BatchSaveResult<Book> result = getSqlClient(it -> it.setDialect(keyDialect(returningSupported)))
                    .saveEntitiesCommand(Arrays.asList(accepted, duplicate, rejected)).matchByKey()
                    .setUpdateWhere(BookTable.class, (table, values) -> table.price().lt(values.newNumber(BookProps.PRICE)))
                    .execute(con);
            assertTrue(result.getItems().get(0).isAccepted());
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(0).getModifiedEntity().id());
            assertTrue(result.getItems().get(1).isAccepted());
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(1).getModifiedEntity().id());
            assertFalse(result.getItems().get(2).isAccepted());
            assertEquals(rejected.id(), result.getItems().get(2).getModifiedEntity().id());
            assertEquals(1, result.getTotalAffectedRowCount());
            assertNativeKeyMerge(returningSupported ? 1 : 2);
        });
    }

    private static H2Dialect keyDialect(boolean returningSupported) {
        return new H2Dialect() {
            @Override
            public boolean isUpsertReturningSupported() {
                return returningSupported;
            }
        };
    }

    @Test
    public void testExplicitKeysInMixedBatch() {
        Book byId = book(Constants.graphQLInActionId3, "Renamed book", BigDecimal.ONE);
        Book byKey = BookDraft.$.produce(draft -> {
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(BigDecimal.TEN);
        });
        Book inserted = BookDraft.$.produce(draft -> {
            draft.setName("New book");
            draft.setEdition(1);
            draft.setPrice(BigDecimal.ONE);
        });
        jdbc(con -> {
            BatchSaveResult<Book> result = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveEntitiesCommand(Arrays.asList(byId, byKey, inserted))
                    .setKeyProps(BookProps.NAME, BookProps.EDITION)
                    .execute(con);
            assertEquals(3, result.getTotalAffectedRowCount());
            assertEquals(Constants.graphQLInActionId3, result.getItems().get(0).getModifiedEntity().id());
            assertEquals(Constants.graphQLInActionId2, result.getItems().get(1).getModifiedEntity().id());
            assertNotNull(result.getItems().get(2).getModifiedEntity().id());
            assertEquals("Renamed book", getSqlClient().getEntities().forConnection(con)
                    .findById(Book.class, byId.id()).name());
        });
    }

    @Test
    public void testExplicitKeysAllowIdOnlyInput() {
        Book input = BookDraft.$.produce(draft -> draft.setId(Constants.graphQLInActionId3));
        executeAndExpectResult(
                getSqlClient().saveCommand(input).setKeyProps(BookProps.NAME, BookProps.EDITION),
                ctx -> {
                    ctx.totalRowCount(0);
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void testAnnotatedKeysStillAllowUpdatingById() {
        Book input = book(Constants.graphQLInActionId3, "Renamed book", BigDecimal.ONE);
        jdbc(con -> {
            SimpleSaveResult<Book> result = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveCommand(input).execute(con);
            assertEquals(input.id(), result.getModifiedEntity().id());
            assertEquals(1, result.getTotalAffectedRowCount());
            assertEquals("Renamed book", getSqlClient().getEntities().forConnection(con).findById(Book.class, input.id()).name());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRootKeysDoNotChangeAssociatedIdentity(boolean inferKeys) {
        Book input = BookDraft.$.produce(book(UUID.randomUUID(), "GraphQL in Action", BigDecimal.ONE), draft ->
                draft.applyStore(store -> {
                    store.setId(Constants.manningId);
                    store.setName("Renamed store");
                })
        );
        jdbc(con -> {
            SimpleSaveResult<Book> result = keyCommand(input, inferKeys)
                    .execute(con);
            assertEquals(Constants.graphQLInActionId3, result.getModifiedEntity().id());
            assertEquals(Constants.manningId, result.getModifiedEntity().store().id());
            assertEquals("Renamed store", getSqlClient().getEntities().forConnection(con)
                    .findById(BookStore.class, Constants.manningId).name());
        });
    }

    @Test
    public void testExplicitAssociatedKeysWithoutId() {
        Book input = BookDraft.$.produce(book(Constants.graphQLInActionId3, "Renamed book", BigDecimal.ONE), draft ->
                draft.applyStore(store -> {
                    store.setName("MANNING");
                })
        );
        jdbc(con -> {
            SimpleSaveResult<Book> result = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveCommand(input)
                    .setKeyProps(BookStoreProps.NAME)
                    .execute(con);
            assertEquals(Constants.graphQLInActionId3, result.getModifiedEntity().id());
            assertEquals("Renamed book", result.getModifiedEntity().name());
            assertEquals(Constants.manningId, result.getModifiedEntity().store().id());
        });
    }

    private SimpleEntitySaveCommand<Book> keyCommand(Book input, boolean inferKeys) {
        SimpleEntitySaveCommand<Book> command = getSqlClient(it -> it.setDialect(new H2Dialect())).saveCommand(input);
        return inferKeys ? command.matchByKey() : command.setKeyProps(BookProps.NAME, BookProps.EDITION).matchByKey();
    }

    private void assertNativeKeyMerge(int statementCount) {
        assertEquals(statementCount, getExecutions().size());
        String sql = getExecutions().get(0).getSql();
        assertTrue(sql.contains("merge into BOOK "), sql);
        assertTrue(sql.contains("on tb_1_.NAME = tb_2_.NAME and tb_1_.EDITION = tb_2_.EDITION"), sql);
        assertFalse(sql.contains("tb_1_.ID = tb_2_.ID"), sql);
    }

    private BatchEntitySaveCommand<Book> keyBatchCommand(List<Book> input, boolean inferKeys) {
        BatchEntitySaveCommand<Book> command = getSqlClient(it -> it.setDialect(new H2Dialect())).saveEntitiesCommand(input);
        return inferKeys ? command.matchByKey() : command.setKeyProps(BookProps.NAME, BookProps.EDITION).matchByKey();
    }

    private static Stream<Arguments> matchingModes() {
        return keyModes(SaveMode.UPSERT, SaveMode.UPDATE_ONLY, SaveMode.INSERT_IF_ABSENT, SaveMode.NON_IDEMPOTENT_UPSERT);
    }

    private static Stream<Arguments> insertingModes() {
        return keyModes(SaveMode.UPSERT, SaveMode.INSERT_ONLY, SaveMode.INSERT_IF_ABSENT, SaveMode.NON_IDEMPOTENT_UPSERT);
    }

    private static Stream<Arguments> keyModes(SaveMode... modes) {
        return Arrays.stream(modes).flatMap(mode -> Stream.of(Arguments.of(mode, false), Arguments.of(mode, true)));
    }

    private static Book book(UUID id, String name, BigDecimal price) {
        return BookDraft.$.produce(draft -> {
            draft.setId(id);
            draft.setName(name);
            draft.setEdition(3);
            draft.setPrice(price);
        });
    }
}
