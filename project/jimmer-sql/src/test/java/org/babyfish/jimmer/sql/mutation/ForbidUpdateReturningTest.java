package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.dto.BookFoldView;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ForbidUpdateReturningTest extends AbstractMutationTest {

    @ParameterizedTest
    @MethodSource("singleCases")
    public void testWithoutFetcher(SaveMode mode, boolean returning, boolean existing) {
        JSqlClient client = client(returning);
        UUID id = existing ? Constants.graphQLInActionId3 : UUID.randomUUID();
        Book input = book(id, "Incoming name", 9, BigDecimal.ONE);
        jdbc(con -> {
            SimpleSaveResult<Book> result = client.saveCommand(input).setMode(mode).forbidUpdate().execute(con);
            assertEquals(!existing || mode != SaveMode.INSERT_IF_ABSENT, result.isAccepted());
            assertEquals(existing ? 0 : 1, result.getTotalAffectedRowCount());
            assertEquals(id, result.getModifiedEntity().id());
            assertEquals(1, getExecutions().size());
            assertEquals(
                    "merge into BOOK tb_1_ using(values(?, ?, ?, ?)) tb_2_(ID, NAME, EDITION, PRICE) " +
                            "on tb_1_.ID = tb_2_.ID when not matched then insert(ID, NAME, EDITION, PRICE) " +
                            "values(tb_2_.ID, tb_2_.NAME, tb_2_.EDITION, tb_2_.PRICE)",
                    getExecutions().get(0).getSql()
            );
            assertEquals(Arrays.asList(id, "Incoming name", 9, BigDecimal.ONE), getExecutions().get(0).getVariables(0));
            Book stored = client.getEntities().forConnection(con).findById(Book.class, id);
            assertEquals(existing ? "GraphQL in Action" : "Incoming name", stored.name());
            assertEquals(new BigDecimal(existing ? "80.00" : "1.00"), stored.price());
        });
    }

    @ParameterizedTest
    @MethodSource("singleCases")
    public void testSuppliedId(SaveMode mode, boolean returning, boolean existing) {
        JSqlClient client = client(returning);
        UUID id = existing ? Constants.graphQLInActionId3 : UUID.randomUUID();
        Book input = book(id, "Incoming name", 9, BigDecimal.ONE);
        jdbc(con -> {
            SimpleSaveResult<Book> result = client.saveCommand(input).setMode(mode).forbidUpdate()
                    .execute(con, BookFetcher.$.allScalarFields());
            boolean accepted = !existing || mode != SaveMode.INSERT_IF_ABSENT;
            assertEquals(accepted, result.isAccepted());
            assertSame(input, result.getOriginalEntity());
            if (accepted) {
                assertEquals(id, result.getModifiedEntity().id());
                assertEquals(existing ? "GraphQL in Action" : "Incoming name", result.getModifiedEntity().name());
                assertEquals(existing ? 3 : 9, result.getModifiedEntity().edition());
                assertEquals(0, new BigDecimal(existing ? "80" : "1").compareTo(result.getModifiedEntity().price()));
            }
            assertEquals(!existing || returning && accepted ? 1 : 0, result.getTotalAffectedRowCount());
            assertEquals(!accepted || returning && mode != SaveMode.INSERT_IF_ABSENT ? 1 : 2, getExecutions().size());
            String sql = getExecutions().get(0).getSql();
            assertEquals(returning && mode != SaveMode.INSERT_IF_ABSENT, sql.contains("/* fake update"), sql);
            assertTrue(sql.contains("on tb_1_.ID = tb_2_.ID"), sql);
            if (getExecutions().size() == 2) {
                assertTrue(getExecutions().get(1).getSql().startsWith("select "));
                assertTrue(getExecutions().get(1).getSql().contains("tb_1_.PRICE"));
            }
            Book stored = client.getEntities().forConnection(con).findById(Book.class, id);
            assertEquals(existing ? "GraphQL in Action" : "Incoming name", stored.name());
            assertEquals(existing ? 3 : 9, stored.edition());
            assertEquals(new BigDecimal(existing ? "80.00" : "1.00"), stored.price());
        });
    }

    @ParameterizedTest
    @MethodSource("modes")
    public void testMixedBatchWithDuplicateIds(SaveMode mode, boolean returning) {
        JSqlClient client = client(returning);
        UUID newId = UUID.randomUUID();
        Book existing = book(Constants.graphQLInActionId3, "Incoming name", 9, BigDecimal.ONE);
        Book inserted = book(newId, "New book", 1, BigDecimal.TEN);
        Book duplicate = book(Constants.graphQLInActionId3, "Duplicate name", 8, BigDecimal.TEN);
        jdbc(con -> {
            BatchSaveResult<Book> result = client.saveEntitiesCommand(Arrays.asList(existing, inserted, duplicate))
                    .setMode(mode).forbidUpdate().execute(con, BookFetcher.$.allScalarFields());
            assertEquals(3, result.getItems().size());
            boolean acceptExisting = mode != SaveMode.INSERT_IF_ABSENT;
            for (int index : new int[] {0, 2}) {
                BatchSaveResult.Item<Book> item = result.getItems().get(index);
                assertEquals(acceptExisting, item.isAccepted());
                if (acceptExisting) {
                    assertEquals(Constants.graphQLInActionId3, item.getModifiedEntity().id());
                    assertEquals("GraphQL in Action", item.getModifiedEntity().name());
                    assertEquals(3, item.getModifiedEntity().edition());
                    assertEquals(new BigDecimal("80.00"), item.getModifiedEntity().price());
                }
            }
            assertTrue(result.getItems().get(1).isAccepted());
            assertEquals(newId, result.getItems().get(1).getModifiedEntity().id());
            assertEquals("New book", result.getItems().get(1).getModifiedEntity().name());
            assertEquals(returning && acceptExisting ? 2 : 1, result.getTotalAffectedRowCount());
            assertEquals(returning && acceptExisting ? 1 : 2, getExecutions().size());
            String sql = getExecutions().get(0).getSql();
            assertEquals(returning && acceptExisting, sql.contains("/* fake update"), sql);
            assertEquals(returning && acceptExisting ? 1 : 2, getExecutions().get(0).getBatchCount());
            if (returning && acceptExisting) {
                assertEquals(Arrays.asList(existing.id(), duplicate.name(), duplicate.edition(), duplicate.price(),
                        newId, inserted.name(), inserted.edition(), inserted.price()), getExecutions().get(0).getVariables(0));
            } else {
                assertEquals(Arrays.asList(existing.id(), duplicate.name(), duplicate.edition(), duplicate.price()),
                        getExecutions().get(0).getVariables(0));
                assertEquals(Arrays.asList(newId, inserted.name(), inserted.edition(), inserted.price()),
                        getExecutions().get(0).getVariables(1));
                assertTrue(getExecutions().get(1).getSql().startsWith("select "));
                assertTrue(getExecutions().get(1).getSql().contains("tb_1_.PRICE"));
            }
        });
    }

    @ParameterizedTest
    @MethodSource("cascadeCases")
    public void testCascadeWithView(boolean existing, boolean forbidUpdate) {
        JSqlClient client = client(true);
        UUID id = existing ? Constants.graphQLInActionId3 : UUID.randomUUID();
        Book input = BookDraft.$.produce(book(id, "Incoming name", 9, BigDecimal.ONE), draft ->
                draft.applyStore(store -> store.setName("O'REILLY").setWebsite("ignored website"))
        );
        jdbc(con -> {
            SimpleEntitySaveCommand<Book> command = client.saveCommand(input)
                    .setKeyProps(BookStoreProps.NAME)
                    .setAssociatedMode(BookProps.STORE, AssociatedSaveMode.MERGE)
                    .setUpsertMask(UpsertMask.of(BookStore.class).forbidUpdate());
            command = forbidUpdate ? command.forbidUpdate() : command.setUpsertMask(BookProps.PRICE);
            SimpleSaveResult.View<Book, BookFoldView> result = command.execute(con, BookFoldView.class);
            assertTrue(result.isAccepted());
            assertEquals(id, result.getModifiedView().getId());
            assertEquals(existing ? "GraphQL in Action" : "Incoming name", result.getModifiedView().getSummary().getName());
            assertEquals(existing ? 3 : 9, result.getModifiedView().getSummary().getEdition());
            // Both the key-matched store and id-matched book are returned by their own DML.
            assertEquals(2, result.getTotalAffectedRowCount());
            assertEquals(2, getExecutions().size());
            String storeSql = getExecutions().get(0).getSql();
            assertTrue(storeSql.startsWith("select ID, VERSION, NAME from final table (merge into BOOK_STORE "), storeSql);
            assertTrue(storeSql.contains("/* fake update"), storeSql);
            String bookSql = getExecutions().get(1).getSql();
            assertEquals(
                    "select ID, NAME, EDITION from final table (merge into BOOK tb_1_ " +
                            "using(values(?, ?, ?, ?, ?)) tb_2_(ID, NAME, EDITION, PRICE, STORE_ID) " +
                            "on tb_1_.ID = tb_2_.ID when matched then update set " +
                            (forbidUpdate ? "/* fake update to return all ids */ PRICE = tb_1_.PRICE " : "PRICE = tb_2_.PRICE ") +
                            "when not matched then insert(ID, NAME, EDITION, PRICE, STORE_ID) " +
                            "values(tb_2_.ID, tb_2_.NAME, tb_2_.EDITION, tb_2_.PRICE, tb_2_.STORE_ID))",
                    bookSql
            );
            assertEquals(Arrays.asList(id, "Incoming name", 9, BigDecimal.ONE, Constants.oreillyId),
                    getExecutions().get(1).getVariables(0));
            Book stored = client.getEntities().forConnection(con).findById(BookFetcher.$.allScalarFields().storeId(), id);
            assertEquals(new BigDecimal(existing && forbidUpdate ? "80.00" : "1.00"), stored.price());
            assertEquals(existing ? Constants.manningId : Constants.oreillyId, stored.storeId());
            BookStore store = client.getEntities().forConnection(con).findById(BookStore.class, Constants.oreillyId);
            assertNotEquals("ignored website", store.website());
            assertEquals(0, store.version());
        });
    }

    private JSqlClient client(boolean returning) {
        return getSqlClient(it -> it.setDialect(new H2Dialect() {
            @Override
            public boolean isUpsertReturningSupported() {
                return returning;
            }
        }));
    }

    private static Book book(UUID id, String name, int edition, BigDecimal price) {
        return BookDraft.$.produce(draft -> draft.setId(id).setName(name).setEdition(edition).setPrice(price));
    }

    private static Stream<Arguments> modes() {
        return Stream.of(SaveMode.UPSERT, SaveMode.NON_IDEMPOTENT_UPSERT, SaveMode.INSERT_IF_ABSENT)
                .flatMap(mode -> Stream.of(false, true).map(returning -> Arguments.of(mode, returning)));
    }

    private static Stream<Arguments> singleCases() {
        return modes().flatMap(args -> Stream.of(false, true).map(existing ->
                Arguments.of(args.get()[0], args.get()[1], existing)));
    }

    private static Stream<Arguments> cascadeCases() {
        return Stream.of(false, true).flatMap(existing ->
                Stream.of(false, true).map(forbidUpdate -> Arguments.of(existing, forbidUpdate)));
    }
}
