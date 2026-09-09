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
            assertEquals(0, result.getTotalAffectedRowCount());
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
            assertEquals(1, result.getTotalAffectedRowCount());
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
