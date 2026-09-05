package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheFactory;
import org.babyfish.jimmer.sql.cache.CacheOperator;
import org.babyfish.jimmer.sql.cache.UsedCache;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class InsertFromSelectEventsAndCacheTest extends AbstractMutationTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testUpdateOnlyEventAndCacheInvalidation(boolean accepted) {
        List<EntityEvent<?>> events = new ArrayList<>();
        List<String> evictions = new ArrayList<>();
        JSqlClient client = cachedClient(evictions, false);
        client.getTriggers(true).addEntityListener(BookStore.class, events::add);
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = source(client, oreillyId, "UPDATE-ONLY");
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> assertEquals(accepted ? singletonList("UPDATE-ONLY") : java.util.Collections.emptyList(),
                client.createUpsert(table, source)
                        .update(table.website(), source.get_2())
                        .key(table.id(), source.get_1())
                        .updateWhere(table.name().eq(accepted ? "O'REILLY" : "OTHER"))
                        .returning(table.website())
                        .execute(con)));

        if (accepted) {
            assertEquals(1, events.size());
            assertNull(((BookStore) events.get(0).getOldEntity()).website());
            assertEquals("UPDATE-ONLY", ((BookStore) events.get(0).getNewEntity()).website());
            assertEquals(singletonList("BookStore-" + oreillyId), evictions);
        } else {
            assertTrue(events.isEmpty());
            assertTrue(evictions.isEmpty());
        }
    }

    @Test
    public void testInsertEventAndObjectCacheInvalidation() {
        List<EntityEvent<?>> events = new ArrayList<>();
        List<String> evictions = new ArrayList<>();
        JSqlClient client = cachedClient(evictions, false);
        client.getTriggers(true).addEntityListener(BookStore.class, events::add);
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000031");
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = source(client, id, "EVENT-INSERT");
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> assertEquals(
                1,
                client.createInsert(table, source)
                        .set(table.id(), source.get_1())
                        .set(table.name(), source.get_2())
                        .execute(con)
        ));

        assertEquals(1, events.size());
        EntityEvent<?> event = events.get(0);
        assertNull(event.getOldEntity());
        assertEquals(id, event.getId());
        assertEquals("EVENT-INSERT", ((BookStore) event.getNewEntity()).name());
        assertEquals(singletonList("BookStore-" + id), evictions);
    }

    @Test
    public void testUpdateEventAndObjectCacheInvalidation() {
        List<EntityEvent<?>> events = new ArrayList<>();
        List<String> evictions = new ArrayList<>();
        JSqlClient client = cachedClient(evictions, false);
        client.getTriggers(true).addEntityListener(BookStore.class, events::add);
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = source(
                client,
                oreillyId,
                "O'REILLY-EVENT"
        );
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> assertEquals(
                1,
                client.createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .execute(con)
        ));

        assertEquals(1, events.size());
        EntityEvent<?> event = events.get(0);
        assertEquals("O'REILLY", ((BookStore) event.getOldEntity()).name());
        assertEquals("O'REILLY-EVENT", ((BookStore) event.getNewEntity()).name());
        assertEquals(singletonList("BookStore-" + oreillyId), evictions);
    }

    @Test
    public void testRejectedUpdateProducesNoEventOrInvalidation() {
        List<EntityEvent<?>> events = new ArrayList<>();
        List<String> evictions = new ArrayList<>();
        JSqlClient client = cachedClient(evictions, false);
        client.getTriggers(true).addEntityListener(BookStore.class, events::add);
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = source(
                client,
                oreillyId,
                "REJECTED"
        );
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> assertEquals(
                0,
                client.createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .updateWhere(source.get_2().eq("ACCEPTED"))
                        .execute(con)
        ));

        assertTrue(events.isEmpty());
        assertTrue(evictions.isEmpty());
    }

    @Test
    public void testFakeUpdateEventAndObjectCacheInvalidation() {
        List<EntityEvent<?>> events = new ArrayList<>();
        List<String> evictions = new ArrayList<>();
        JSqlClient client = cachedClient(evictions, false);
        client.getTriggers(true).addEntityListener(BookStore.class, events::add);
        BaseTable1<ComparableExpression<UUID>> source = client
                .createBaseQuery()
                .addSelect(Expression.value(oreillyId))
                .asBaseTable();
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> assertEquals(
                1,
                client.createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .execute(con)
        ));

        assertEquals(1, events.size());
        assertNotNull(events.get(0).getOldEntity());
        assertNotNull(events.get(0).getNewEntity());
        assertEquals(singletonList("BookStore-" + oreillyId), evictions);
    }

    @Test
    public void testAssociationEventAndBidirectionalCacheInvalidation() {
        List<AssociationEvent> events = new ArrayList<>();
        List<String> evictions = new ArrayList<>();
        JSqlClient client = cachedClient(evictions, true);
        client.getTriggers(true).addAssociationListener(events::add);
        BaseTable2<ComparableExpression<UUID>, ComparableExpression<UUID>> source = client
                .createBaseQuery()
                .addSelect(Expression.value(effectiveTypeScriptId1))
                .addSelect(Expression.value(alexId))
                .asBaseTable();
        AssociationTable<Book, BookTableEx, Author, AuthorTableEx> association =
                AssociationTable.of(BookTableEx.class, BookTableEx::authors);

        jdbc(con -> assertEquals(
                1,
                client.createInsert(association, source)
                        .set(association.<UUID>sourceId(), source.get_1())
                        .set(association.<UUID>targetId(), source.get_2())
                        .execute(con)
        ));

        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(it ->
                it.getSourceId().equals(effectiveTypeScriptId1) &&
                        alexId.equals(it.getAttachedTargetId())
        ));
        assertTrue(events.stream().anyMatch(it ->
                it.getSourceId().equals(alexId) &&
                        effectiveTypeScriptId1.equals(it.getAttachedTargetId())
        ));
        assertEquals(
                asList(
                        "Book.authors-" + effectiveTypeScriptId1,
                        "Author.books-" + alexId
                ),
                evictions
        );
    }

    private JSqlClient cachedClient(List<String> evictions, boolean associationCaches) {
        return getSqlClient(builder -> {
            builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
            builder.setCaches(cfg -> cfg
                    .setCacheFactory(new CacheFactory() {
                        @Override
                        public Cache<?, ?> createObjectCache(ImmutableType type) {
                            Class<?> javaType = type.getJavaClass();
                            return javaType == BookStore.class ||
                                    associationCaches && (javaType == Book.class || javaType == Author.class) ?
                                    new CacheImpl<>(type) :
                                    null;
                        }

                        @Override
                        public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                            return associationCaches &&
                                    (prop == BookProps.AUTHORS.unwrap() || prop == AuthorProps.BOOKS.unwrap()) ?
                                    new CacheImpl<>(prop) :
                                    null;
                        }
                    })
                    .setCacheOperator(new CacheOperator() {
                        @Override
                        public void delete(UsedCache<Object, ?> cache, Object key, Object reason) {
                            evictions.add(cacheName(cache) + '-' + key);
                            CacheOperator.suspending(() -> cache.delete(key));
                        }

                        @Override
                        public void deleteAll(
                                UsedCache<Object, ?> cache,
                                Collection<Object> keys,
                                Object reason
                        ) {
                            for (Object key : keys) {
                                evictions.add(cacheName(cache) + '-' + key);
                            }
                            CacheOperator.suspending(() -> cache.deleteAll(keys));
                        }
                    }));
        });
    }

    private static BaseTable2<ComparableExpression<UUID>, StringExpression> source(
            JSqlClient client,
            UUID id,
            String name
    ) {
        return client
                .createBaseQuery()
                .addSelect(Expression.value(id))
                .addSelect(Expression.value(name))
                .asBaseTable();
    }

    private static String cacheName(UsedCache<?, ?> cache) {
        ImmutableProp prop = cache.prop();
        if (prop == null) {
            return cache.type().getJavaClass().getSimpleName();
        }
        return prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName();
    }
}
