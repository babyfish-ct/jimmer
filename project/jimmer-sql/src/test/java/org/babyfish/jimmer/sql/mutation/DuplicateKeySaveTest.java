package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.babyfish.jimmer.sql.model.steam.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DuplicateKeySaveTest extends AbstractMutationTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stringIdsAndKeyOnlyTargets(boolean fallback) {
        AtomicInteger gameIds = new AtomicInteger();
        AtomicInteger bundleIds = new AtomicInteger();
        JSqlClient client = getSqlClient(builder -> builder
                .setDialect(dialect(fallback))
                .setIdGenerator(Game.class, (UserIdGenerator<String>) type -> "game-" + gameIds.incrementAndGet())
                .setIdGenerator(Bundle.class, (UserIdGenerator<String>) type -> "bundle-" + bundleIds.incrementAndGet()));
        List<Bundle> roots = Arrays.asList(
                BundleDraft.$.produce(draft -> draft.setBundleId(1).addIntoGames(game -> game.setAppId(42))),
                BundleDraft.$.produce(draft -> draft.setBundleId(2).addIntoGames(game -> game.setAppId(42)))
        );
        jdbc(con -> {
            BatchSaveResult<Bundle> result = client.saveEntitiesCommand(roots)
                    .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT).execute(con);
            assertEquals(1, gameIds.get());
            assertEquals(2, bundleIds.get());
            assertEquals(5, result.getTotalAffectedRowCount());
            for (BatchSaveResult.Item<Bundle> item : result.getItems()) {
                assertEquals("game-1", item.getModifiedEntity().games().get(0).id());
            }
            // Fallback adds one key lookup per entity type; new roots need no lookup of existing links.
            assertEquals(fallback ? 5 : 3, getExecutions().size(), this::executedSql);
            List<Execution> gameWrites = new ArrayList<>();
            for (Execution execution : getExecutions()) {
                if (execution.getSql().contains("into STEAM_GAME")) {
                    gameWrites.add(execution);
                }
            }
            assertEquals(1, gameWrites.size());
            assertEquals(1, gameWrites.get(0).getBatchCount());
            assertEquals(2, getExecutions().get(getExecutions().size() - 1).getBatchCount());
            clearExecutions();
            BatchSaveResult<Bundle> repeated = client.saveEntitiesCommand(roots)
                    .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT).execute(con);
            for (BatchSaveResult.Item<Bundle> item : repeated.getItems()) {
                assertEquals("game-1", item.getModifiedEntity().games().get(0).id());
            }
            assertEquals(fallback ? 1 : 2, gameIds.get());
            try (java.sql.Statement stmt = con.createStatement()) {
                for (String table : Arrays.asList("STEAM_BUNDLE", "STEAM_GAME", "STEAM_BUNDLE_GAME")) {
                    try (java.sql.ResultSet rs = stmt.executeQuery("select count(*) from " + table)) {
                        assertTrue(rs.next());
                        assertEquals(table.equals("STEAM_GAME") ? 1 : 2, rs.getInt(1));
                    }
                }
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void twoRootsShareNewTarget(boolean fallback) {
        List<UUID> generatedIds = new ArrayList<>();
        JSqlClient client = getSqlClient(builder -> builder
                .setDialect(dialect(fallback))
                .setIdGenerator(Book.class, (UserIdGenerator<UUID>) type -> {
                    UUID id = UUID.randomUUID();
                    generatedIds.add(id);
                    return id;
                }));
        List<Author> roots = Arrays.asList(root(Constants.danId), root(Constants.borisId));
        assertNotSame(roots.get(0).books().get(0), roots.get(1).books().get(0));
        jdbc(con -> {
            BatchSaveResult<Author> result = client.saveEntitiesCommand(roots)
                    .setAssociatedMode(AuthorProps.BOOKS, AssociatedSaveMode.APPEND_IF_ABSENT).execute(con);
            assertEquals(1, generatedIds.size());
            UUID id = generatedIds.get(0);
            for (BatchSaveResult.Item<Author> item : result.getItems()) {
                assertEquals(id, item.getModifiedEntity().books().get(0).id());
            }
            assertEquals(3, result.getTotalAffectedRowCount());
            // Fallback looks up authors, books and existing links, then writes one book and two links.
            assertEquals(fallback ? 5 : 2, getExecutions().size(), this::executedSql);
            List<Execution> writes = new ArrayList<>();
            for (Execution execution : getExecutions()) {
                if (!execution.getSql().startsWith("select ") || execution.getSql().contains("final table")) {
                    writes.add(execution);
                }
            }
            assertEquals(2, writes.size());
            assertTrue(writes.get(0).getSql().contains("BOOK"));
            assertEquals(1, writes.get(0).getBatchCount());
            assertTrue(writes.get(1).getSql().contains("BOOK_AUTHOR_MAPPING"));
            assertEquals(2, writes.get(1).getBatchCount());
            clearExecutions();
            BatchSaveResult<Author> repeated = client.saveEntitiesCommand(roots)
                    .setAssociatedMode(AuthorProps.BOOKS, AssociatedSaveMode.APPEND_IF_ABSENT).execute(con);
            assertEquals(0, repeated.getTotalAffectedRowCount());
            assertEquals(fallback ? 1 : 2, generatedIds.size());
            for (BatchSaveResult.Item<Author> item : repeated.getItems()) {
                assertEquals(id, item.getModifiedEntity().books().get(0).id());
            }
        });
    }

    private static Author root(UUID id) {
        return AuthorDraft.$.produce(draft -> draft.setId(id).addIntoBooks(book ->
                book.setName("Shared new book").setEdition(1).setPrice(BigDecimal.TEN)));
    }

    @ParameterizedTest
    @EnumSource(value = SaveMode.class, names = {"INSERT_ONLY", "UPSERT", "INSERT_IF_ABSENT"})
    void callbacksSeeSharedIdAndKeepLastValue(SaveMode mode) {
        List<UUID> generatedIds = new ArrayList<>();
        List<UUID> interceptedIds = new ArrayList<>();
        AtomicInteger processed = new AtomicInteger();
        JSqlClient client = getSqlClient(builder -> builder
                .setDialect(dialect(true))
                .setIdGenerator(Book.class, (UserIdGenerator<UUID>) type -> {
                    UUID id = UUID.randomUUID();
                    generatedIds.add(id);
                    return id;
                })
                .addDraftPreProcessor(new DraftPreProcessor<BookDraft>() {
                    @Override
                    public void beforeSave(BookDraft draft) {
                        processed.incrementAndGet();
                        draft.setName("Normalized name");
                    }
                })
                .addDraftInterceptor(new DraftInterceptor<Book, BookDraft>() {
                    @Override
                    public void beforeSave(BookDraft draft, Book original) {
                        assertNull(original);
                        interceptedIds.add(draft.id());
                    }
                }));
        List<Book> books = Arrays.asList(
                BookDraft.$.produce(draft -> draft.setName("first").setEdition(1).setPrice(BigDecimal.ONE)),
                BookDraft.$.produce(draft -> draft.setName("second").setEdition(1).setPrice(BigDecimal.TEN))
        );
        jdbc(con -> {
            BatchSaveResult<Book> result = client.saveEntitiesCommand(books).setMode(mode).execute(con);
            assertEquals(2, processed.get());
            assertEquals(1, generatedIds.size());
            assertEquals(Arrays.asList(generatedIds.get(0), generatedIds.get(0)), interceptedIds);
            assertEquals(1, result.getTotalAffectedRowCount());
            assertEquals(mode == SaveMode.INSERT_ONLY ? 1 : 2, getExecutions().size());
            for (BatchSaveResult.Item<Book> item : result.getItems()) {
                assertEquals(generatedIds.get(0), item.getModifiedEntity().id());
            }
            Book stored = client.getEntities().forConnection(con).findById(Book.class, generatedIds.get(0));
            assertEquals(0, BigDecimal.TEN.compareTo(stored.price()));
        });
    }

    @ParameterizedTest
    @EnumSource(value = SaveMode.class, names = {"INSERT_ONLY", "UPSERT", "INSERT_IF_ABSENT"})
    void databaseIdentityIsPropagated(SaveMode mode) {
        jdbc(con -> {
            BatchSaveResult<Department> result = getSqlClient(builder -> builder.setDialect(dialect(true)))
                    .saveEntitiesCommand(Arrays.asList(
                            DepartmentDraft.$.produce(draft -> draft.setName("Shared department")),
                            DepartmentDraft.$.produce(draft -> draft.setName("Shared department"))
                    )).setMode(mode).execute(con);
            assertEquals(result.getItems().get(0).getModifiedEntity().id(), result.getItems().get(1).getModifiedEntity().id());
            assertEquals(1, result.getTotalAffectedRowCount());
            assertEquals(mode == SaveMode.INSERT_ONLY ? 1 : 2, getExecutions().size());
        });
    }

    @ParameterizedTest
    @EnumSource(value = SaveMode.class, names = {"INSERT_ONLY", "UPSERT", "INSERT_IF_ABSENT"})
    void sequenceIsAllocatedOnce(SaveMode mode) {
        JSqlClient client = getSqlClient(builder -> builder.setDialect(dialect(true))
                .setIdGenerator(Department.class, new SequenceIdGenerator("DUPLICATE_KEY_ID_SEQ")));
        jdbc(con -> {
            BatchSaveResult<Department> result = client.saveEntitiesCommand(Arrays.asList(
                    DepartmentDraft.$.produce(draft -> draft.setName("Shared department")),
                    DepartmentDraft.$.produce(draft -> draft.setName("Shared department"))
            )).setMode(mode).execute(con);
            assertEquals(result.getItems().get(0).getModifiedEntity().id(), result.getItems().get(1).getModifiedEntity().id());
            assertEquals(1, result.getTotalAffectedRowCount());
            assertEquals(1, getExecutions().stream().filter(it -> it.getSql().contains("DUPLICATE_KEY_ID_SEQ")).count());
            assertEquals(mode == SaveMode.INSERT_ONLY ? 2 : 3, getExecutions().size(), this::executedSql);
        });
    }

    private static H2Dialect dialect(boolean fallback) {
        return new H2Dialect() {
            @Override
            public boolean isUpsertSupported() {
                return !fallback;
            }
        };
    }

    @Test
    @SuppressWarnings("deprecation")
    void transactionEventsDescribeOneTargetAndTwoLinks() {
        List<EntityEvent<Book>> books = new ArrayList<>();
        List<AssociationEvent> links = new ArrayList<>();
        AtomicInteger generated = new AtomicInteger();
        UUID id = UUID.randomUUID();
        JSqlClient client = getSqlClient(builder -> builder
                .setTriggerType(TriggerType.TRANSACTION_ONLY)
                .setIdGenerator(Book.class, (UserIdGenerator<UUID>) type -> {
                    generated.incrementAndGet();
                    return id;
                }));
        client.getTriggers(true).addEntityListener(Book.class, books::add);
        client.getTriggers(true).addAssociationListener(AuthorProps.BOOKS, links::add);
        jdbc(con -> {
            BatchSaveResult<Author> result = client.saveEntitiesCommand(Arrays.asList(root(Constants.danId), root(Constants.borisId)))
                    .setAssociatedMode(AuthorProps.BOOKS, AssociatedSaveMode.APPEND_IF_ABSENT).execute(con);
            assertEquals(1, generated.get());
            assertEquals(3, result.getTotalAffectedRowCount());
            assertEquals(1, books.size());
            assertNull(books.get(0).getOldEntity());
            assertEquals(id, books.get(0).getNewEntity().id());
            assertEquals(2, links.size());
            Set<Object> sourceIds = new HashSet<>();
            for (AssociationEvent link : links) {
                assertEquals(id, link.getAttachedTargetId());
                assertNull(link.getDetachedTargetId());
                sourceIds.add(link.getSourceId());
            }
            assertEquals(new HashSet<>(Arrays.asList(Constants.danId, Constants.borisId)), sourceIds);
        });
    }

    @Test
    void explicitIdsAreNotUnifiedByKey() {
        AtomicInteger generated = new AtomicInteger();
        JSqlClient client = getSqlClient(builder -> builder.setDialect(dialect(true))
                .setIdGenerator(Game.class, (UserIdGenerator<String>) type -> "generated-" + generated.incrementAndGet()));
        jdbc(con -> {
            assertThrows(SaveException.NotUnique.class, () -> client.saveEntitiesCommand(Arrays.asList(
                    GameDraft.$.produce(draft -> draft.setId("first").setAppId(42)),
                    GameDraft.$.produce(draft -> draft.setId("second").setAppId(42))
            )).execute(con));
            assertEquals(0, generated.get());
        });
    }

    @Test
    void equalValuesInDifferentKeyGroupsGetDifferentIds() {
        AtomicInteger generated = new AtomicInteger(100);
        JSqlClient client = getSqlClient(builder -> builder.setDialect(dialect(true))
                .setIdGenerator(SysUser.class, (UserIdGenerator<Long>) type -> (long) generated.incrementAndGet())
                .addDraftInterceptor(new DraftInterceptor<SysUser, SysUserDraft>() {
                    @Override
                    public void beforeSave(SysUserDraft draft, SysUser original) {
                        if (!ImmutableObjects.isLoaded(draft, SysUserProps.ACCOUNT)) {
                            draft.setAccount("other-account");
                        }
                        if (!ImmutableObjects.isLoaded(draft, SysUserProps.EMAIL)) {
                            draft.setEmail("other-email");
                        }
                        draft.setArea("area").setNickName("user-" + draft.id());
                    }
                }));
        jdbc(con -> {
            BatchSaveResult<SysUser> result = client.saveEntitiesCommand(Arrays.asList(
                    SysUserDraft.$.produce(draft -> draft.setAccount("same-value")),
                    SysUserDraft.$.produce(draft -> draft.setEmail("same-value"))
            )).execute(con);
            assertEquals(102, generated.get());
            assertNotEquals(result.getItems().get(0).getModifiedEntity().id(), result.getItems().get(1).getModifiedEntity().id());
            assertEquals(2, result.getTotalAffectedRowCount());
        });
    }

    private String executedSql() {
        return getExecutions().stream().map(Execution::getSql).collect(java.util.stream.Collectors.joining("\n"));
    }
}
