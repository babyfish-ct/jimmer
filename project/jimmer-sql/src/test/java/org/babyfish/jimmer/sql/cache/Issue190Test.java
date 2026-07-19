package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookProps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class Issue190Test extends AbstractQueryTest {

    @BeforeAll
    public static void hasAuthorIds() {
        Assertions.assertFalse(BookProps.AUTHORS.unwrap().getPropsDependOnSelf().isEmpty());
    }

    @Test
    public void test() {
        JSqlClient sqlClient = getSqlClient(builder -> {
            builder.setCacheFactory(new CF());
        });
        connectAndExpect(con ->
            sqlClient.getEntities().forConnection(con).findById(
                    BookFetcher.$
                            .allScalarFields()
                            .authors(
                                    AuthorFetcher.$
                                            .allScalarFields()
                            ),
                    Constants.learningGraphQLId1
            ),
            ctx -> {
                ctx.rows(
                        "[{" +
                                "--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                "--->\"name\":\"Learning GraphQL\"," +
                                "--->\"edition\":1," +
                                "--->\"price\":50," +
                                "--->\"authors\":[" +
                                "--->--->{" +
                                "--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                "--->--->--->\"firstName\":\"Eve\"," +
                                "--->--->--->\"lastName\":\"Procello\"," +
                                "--->--->--->\"gender\":\"FEMALE\"" +
                                "--->--->},{" +
                                "--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                "--->--->--->\"firstName\":\"Alex\"," +
                                "--->--->--->\"lastName\":\"Banks\"," +
                                "--->--->--->\"gender\":\"MALE\"" +
                                "--->--->}" +
                                "--->]" +
                                "}]"
                );
            }
        );
    }

    private static class CF implements CacheFactory {
        @Override
        public @Nullable Cache<?, ?> createObjectCache(@NotNull ImmutableType type) {
            return new ChainCacheBuilder<>()
                    .add(new ObjectBinder(type))
                    .build();
        }

        @Override
        public @Nullable Cache<?, List<?>> createAssociatedIdListCache(@NotNull ImmutableProp prop) {
            return new ChainCacheBuilder<Object, List<?>>()
                    .add(new ListBinder(prop))
                    .build();
        }
    }

    private static class ObjectBinder implements SimpleBinder<Object, Object> {

        private final ImmutableType type;

        private final String prefix;

        private final Map<String, String> map;

        public ObjectBinder(ImmutableType type) {
            this.type = type;
            Map<String, String> map = new HashMap<>();
            map.put(
                    "Book-e110c564-23cc-4811-9e81-d587a13db634",
                    "{" +
                            "\"id\": \"e110c564-23cc-4811-9e81-d587a13db634\", " +
                            "\"name\": \"Learning GraphQL\", " +
                            "\"price\": 50, " +
                            "\"edition\": 1, " +
                            "\"store\": null" +
                            "}"
            );
            map.put(
                    "Author-fd6bb6cf-336d-416c-8005-1ae11a6694b5",
                    "{" +
                            "\"id\": \"fd6bb6cf-336d-416c-8005-1ae11a6694b5\", " +
                            "\"firstName\": \"Eve\", " +
                            "\"lastName\": \"Procello\", " +
                            "\"gender\": \"FEMALE\"" +
                            "}"
            );
            map.put(
                    "Author-1e93da94-af84-44f4-82d1-d8a9fd52ea94",
                    "{" +
                            "\"id\": \"1e93da94-af84-44f4-82d1-d8a9fd52ea94\", " +
                            "\"firstName\": \"Alex\", " +
                            "\"lastName\": \"Banks\", " +
                            "\"gender\": \"MALE\"" +
                            "}"
            );
            this.prefix = type.getJavaClass().getSimpleName() + '-';
            this.map = map;
        }

        @Override
        public Map<Object, Object> getAll(Collection<Object> keys) {
            Map<Object, Object> resultMap = new HashMap<>();
            Internal.requiresNewDraftContext(ctx -> {
                try {
                    for (Object key : keys) {
                        resultMap.put(key, jsonCodec().readerFor(type.getJavaClass()).read(map.get(prefix + key)));
                    }
                } catch (Exception ex) {
                    Assertions.fail(ex);
                }
                for (Map.Entry<Object, Object> e : resultMap.entrySet()) {
                    e.setValue(ctx.resolveObject(e.getValue()));
                }
                return null;
            });
            return resultMap;
        }

        @Override
        public void setAll(Map<Object, Object> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable ImmutableType type() {
            return type;
        }

        @Override
        public @Nullable ImmutableProp prop() {
            return null;
        }

        @Override
        public void deleteAll(Collection<Object> keys, Object reason) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ListBinder implements SimpleBinder<Object, List<?>> {

        private final ImmutableProp prop;

        private final String prefix;

        private final Map<String, List<?>> map;

        public ListBinder(ImmutableProp prop) {
            this.prop = prop;
            Map<String, List<?>> map = new HashMap<>();
            map.put(
                    "Book.authors-e110c564-23cc-4811-9e81-d587a13db634",
                    Arrays.asList(
                            Constants.eveId,
                            Constants.alexId
                    )
            );
            this.prefix = prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + '-';
            this.map = map;
        }

        @Override
        public Map<Object, List<?>> getAll(Collection<Object> keys) {
            Map<Object, List<?>> resultMap = new HashMap<>();
            for (Object key : keys) {
                resultMap.put(key, map.get(prefix + key));
            }
            return resultMap;
        }

        @Override
        public void setAll(Map<Object, List<?>> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable ImmutableType type() {
            return null;
        }

        @Override
        public @Nullable ImmutableProp prop() {
            return prop;
        }

        @Override
        public void deleteAll(Collection<Object> keys, Object reason) {
            throw new UnsupportedOperationException();
        }
    }
}
