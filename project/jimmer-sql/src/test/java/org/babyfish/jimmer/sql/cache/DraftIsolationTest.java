package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.jackson.codec.ImmutableModuleCustomization;
import org.babyfish.jimmer.jackson.codec.JacksonVersion;
import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.jackson.v2.JsonCodecProviderV2;
import org.babyfish.jimmer.jackson.v3.JsonCodecProviderV3;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.*;
import java.sql.Connection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class DraftIsolationTest extends AbstractTest {

    @Test
    public void testUnresolvedForeignDraftIsStillRejected() {
        BookStore foreignDraft = Internal.requiresNewDraftContext(ctx -> BookStoreDraft.$.produce(draft -> draft.setId(manningId)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BookDraft.$.produce(draft -> draft.setId(graphQLInActionId1).setStore(foreignDraft)));
        assertEquals("Cannot resolve the draft object because it belong to another draft context", ex.getMessage());
    }

    @ParameterizedTest
    @EnumSource(JacksonVersion.class)
    public void testDeserializedIdViewSurvivesDraftContext(JacksonVersion version) {
        JsonCodec<?> codec = (version == JacksonVersion.V2 ? new JsonCodecProviderV2().create() : new JsonCodecProviderV3().create())
                .withCustomizations(new ImmutableModuleCustomization());
        ValueSerializer<Book> serializer = new ValueSerializer<>(ImmutableType.get(Book.class), codec);
        byte[] bytes = serializer.serialize(new BookDraft.Builder().id(graphQLInActionId1).name("GraphQL in Action")
                .storeId(manningId).authorIds(Collections.singletonList(sammerId)).build());
        Book cached = Internal.requiresNewDraftContext(ctx -> serializer.deserialize(bytes));
        Book reshaped = BookDraft.$.produce(cached, draft -> draft.setName("Changed"));
        assertEquals(manningId, reshaped.store().id());
        assertEquals(sammerId, reshaped.authors().get(0).id());
        assertGraph(cached);
    }

    @ParameterizedTest
    @EnumSource(CacheMode.class)
    public void testColdAndWarmReads(CacheMode mode) {
        Harness harness = new Harness(mode);
        readMatrix(harness.client);
        assertTrue(harness.sqlCount.get() > 0);
        harness.sqlCount.set(0);
        for (int i = 0; i < 10; i++) {
            readMatrix(harness.client);
        }
        assertEquals(0, harness.sqlCount.get(), "Warm reads must stay in the cache");
        harness.assertStable();
    }

    @ParameterizedTest
    @EnumSource(CacheMode.class)
    public void testConcurrentReads(CacheMode mode) throws Exception {
        Harness harness = new Harness(mode);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            for (boolean warm : new boolean[] {false, true}) {
                harness.sqlCount.set(0);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    futures.add(pool.submit(() -> {
                        assertTrue(start.await(10, TimeUnit.SECONDS));
                        for (int j = 0; j < 25; j++) {
                            readMatrix(harness.client);
                        }
                        return null;
                    }));
                }
                start.countDown();
                for (Future<?> future : futures) {
                    future.get(30, TimeUnit.SECONDS);
                }
                if (warm) {
                    assertEquals(0, harness.sqlCount.get(), "Concurrent warm reads must stay in the cache");
                }
                harness.assertStable();
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static void readMatrix(JSqlClient client) {
        Book detail = client.findById(BookFetcher.$.allScalarFields()
                .store(BookStoreFetcher.$.name()).authors(AuthorFetcher.$.firstName()), graphQLInActionId1);
        assertNotNull(detail);
        assertEquals("GraphQL in Action", detail.name());
        assertEquals("MANNING", detail.store().name());
        assertEquals("Samer", detail.authors().get(0).firstName());
        assertGraph(detail);

        Book ids = client.findById(BookFetcher.$.storeId().authorIds(), graphQLInActionId1);
        assertEquals(manningId, ids.storeId());
        assertEquals(Collections.singletonList(sammerId), ids.authorIds());
        assertGraph(ids);

        Book nested = client.findById(BookFetcher.$.name()
                .store(BookStoreFetcher.$.name().books(BookFetcher.$.name().store(BookStoreFetcher.$.name()))), graphQLInActionId1);
        assertEquals(manningId, nested.store().id());
        assertFalse(nested.store().books().isEmpty());
        for (Book book : nested.store().books()) {
            assertEquals(manningId, book.store().id());
            assertEquals("MANNING", book.store().name());
        }
        assertGraph(nested);
        assertEquals("MANNING", detail.store().name());

        TreeNode node = client.findById(TreeNodeFetcher.$.name()
                .parent(TreeNodeFetcher.$.name().parent(TreeNodeFetcher.$.name())), 3L);
        assertEquals("Drinks", node.name());
        assertEquals("Food", node.parent().name());
        assertEquals("Home", node.parent().parent().name());
        assertGraph(node);
    }

    private static void assertGraph(Object value) {
        assertGraph(value, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void assertGraph(Object value, Set<Object> visited) {
        if (value == null || !visited.add(value)) {
            return;
        }
        assertFalse(value instanceof Draft, "A cached or returned graph must not contain drafts");
        if (value instanceof ImmutableSpi) {
            ImmutableSpi spi = (ImmutableSpi) value;
            for (ImmutableProp prop : spi.__type().getProps().values()) {
                if (!prop.isView() && spi.__isLoaded(prop.getId())) {
                    assertGraph(spi.__get(prop.getId()), visited);
                }
            }
        } else if (value instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) value) {
                assertGraph(item, visited);
            }
        }
    }

    private final class Harness {
        final AtomicInteger sqlCount = new AtomicInteger();
        final List<CheckingCache> caches = new CopyOnWriteArrayList<>();
        final JSqlClient client;

        Harness(CacheMode mode) {
            client = getSqlClient(builder -> builder.setConnectionManager(testConnectionManager())
                    .setExecutor(new Executor() {
                        @Override
                        public <R> R execute(@NotNull Args<R> args) {
                            sqlCount.incrementAndGet();
                            return DefaultExecutor.INSTANCE.execute(args);
                        }

                        @Override
                        public BatchContext executeBatch(
                                @NotNull Connection con,
                                @NotNull String sql,
                                @Nullable ImmutableProp generatedIdProp,
                                @NotNull ExecutionPurpose purpose,
                                @NotNull JSqlClientImplementor sqlClient,
                                boolean constraintViolationTranslatable
                        ) {
                            throw new AssertionError("Read-only cache tests must not execute batches");
                        }
                    })
                    .setCaches(cfg -> cfg.setCacheFactory(new CacheFactory() {
                        private final Set<Class<?>> cachedTypes = new HashSet<>(Arrays.asList(
                                Book.class, BookStore.class, Author.class, TreeNode.class
                        ));

                        @Override
                        public Cache<?, ?> createObjectCache(ImmutableType type) {
                            return add(type, null);
                        }

                        @Override
                        public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                            return add(prop.getDeclaringType(), prop);
                        }

                        @Override
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                            return (Cache) add(prop.getDeclaringType(), prop);
                        }

                        private CheckingCache add(ImmutableType type, ImmutableProp prop) {
                            if (!cachedTypes.contains(type.getJavaClass()) ||
                                    prop != null && !cachedTypes.contains(prop.getTargetType().getJavaClass())) {
                                return null;
                            }
                            CheckingCache cache = new CheckingCache(type, prop, mode);
                            caches.add(cache);
                            return cache;
                        }
                    })));
        }

        void assertStable() {
            caches.forEach(CheckingCache::assertStable);
        }
    }

    enum CacheMode {
        MEMORY, SERIALIZED, TWO_LEVEL
    }

    private static final class CheckingCache implements Cache<Object, Object> {
        private static final Object NULL = new Object();
        private final ImmutableType type;
        private final ImmutableProp prop;
        private final CacheMode mode;
        private final ValueSerializer<Object> serializer;
        private final Map<Object, Entry> values = new ConcurrentHashMap<>();

        CheckingCache(ImmutableType type, ImmutableProp prop, CacheMode mode) {
            this.type = type;
            this.prop = prop;
            this.mode = mode;
            serializer = prop != null ? new ValueSerializer<>(prop) : new ValueSerializer<>(type);
        }

        @Override
        public @NotNull Map<Object, Object> getAll(@NotNull Collection<Object> keys, @NotNull CacheEnvironment<Object, Object> env) {
            Set<Object> missing = new LinkedHashSet<>(keys);
            missing.removeAll(values.keySet());
            if (!missing.isEmpty()) {
                Map<Object, Object> loaded = env.getLoader().loadAll(missing);
                for (Object key : missing) {
                    Object value = loaded.get(key);
                    assertGraph(value);
                    byte[] bytes = serializer.serialize(value);
                    values.putIfAbsent(key, new Entry(mode != CacheMode.MEMORY ? bytes : value != null ? value : NULL, bytes));
                }
            }
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Object key : keys) {
                Entry entry = values.get(key);
                Object stored = entry.value;
                Object value = stored instanceof byte[] ? serializer.deserialize((byte[]) stored) : stored == NULL ? null : stored;
                assertGraph(value);
                if (mode == CacheMode.TWO_LEVEL) {
                    entry.value = value != null ? value : NULL;
                }
                result.put(key, value);
            }
            return result;
        }

        void assertStable() {
            if (mode != CacheMode.SERIALIZED) {
                for (Entry entry : values.values()) {
                    Object value = entry.value == NULL ? null : entry.value;
                    assertGraph(value);
                    assertArrayEquals(entry.snapshot, serializer.serialize(value), "Reading must not mutate cached objects");
                }
            }
        }

        @Override
        public @NotNull ImmutableType type() {
            return type;
        }

        @Override
        public @Nullable ImmutableProp prop() {
            return prop;
        }

        @Override
        public void deleteAll(@NotNull Collection<Object> keys, @Nullable Object reason) {
            keys.forEach(values::remove);
        }

        private static class Entry {
            volatile Object value;
            final byte[] snapshot;

            Entry(Object value, byte[] snapshot) {
                this.value = value;
                this.snapshot = snapshot;
            }
        }
    }
}
