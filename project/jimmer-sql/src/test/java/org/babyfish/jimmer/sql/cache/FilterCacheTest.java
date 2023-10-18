package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.common.ParameterizedCaches;
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig;
import org.babyfish.jimmer.sql.filter.common.CacheableFileFilter;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.model.filter.File;
import org.babyfish.jimmer.sql.model.filter.FileFetcher;
import org.babyfish.jimmer.sql.model.filter.FileTable;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterCacheTest extends AbstractQueryTest {

    private JSqlClient sqlClient;

    private Map<String, Map<String, byte[]>> valueMap;

    @BeforeEach
    public void initialize() {
        valueMap = new HashMap<>();
        sqlClient = getSqlClient(it -> {
            it.addFilters(new CacheableFileFilter());
            it.setConnectionManager(
                    new ConnectionManager() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public <R> R execute(Function<Connection, R> block) {
                            R[] resultBox = (R[])new Object[1];
                            jdbc(con -> {
                                resultBox[0] = block.apply(con);
                            });
                            return resultBox[0];
                        }
                    }
            );
            it.setCacheFactory(
                    new CacheFactory() {
                        @Override
                        public Cache<?, ?> createObjectCache(@NotNull ImmutableType type) {
                            return new CacheImpl<>(type);
                        }

                        @Override
                        public Cache<?, ?> createAssociatedIdCache(@NotNull ImmutableProp prop) {
                            return ParameterizedCaches.create(prop, null, valueMap);
                        }

                        @Override
                        public Cache<?, List<?>> createAssociatedIdListCache(@NotNull ImmutableProp prop) {
                            return ParameterizedCaches.create(prop, null, valueMap);
                        }

                        @Override
                        public Cache<?, ?> createResolverCache(@NotNull ImmutableProp prop) {
                            return ParameterizedCaches.create(prop, null, valueMap);
                        }
                    }
            );
        });
    }

    @Test
    public void testById() {
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> {
                        List<File>[] ref = new List[1];
                        FileFilter.withUser(2L, () -> {
                            ref[0] = sqlClient.findByIds(File.class, Arrays.asList(1L, 2L, 3L, 4L, 11L, 12L, 13L, 14L, 100L));
                        });
                        return ref[0];
                    },
                    ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                            "from FILE tb_1_ " +
                                            "where " +
                                            "--->tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                            "and " +
                                            "--->exists(" +
                                            "--->--->select 1 " +
                                            "--->--->from FILE_USER_MAPPING tb_2_ " +
                                            "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                            "--->)"
                            );
                        }
                        ctx.rows(
                                "[" +
                                        "--->{\"id\":1,\"name\":\"usr\",\"parent\":null}," +
                                        "--->{\"id\":2,\"name\":\"bin\",\"parent\":{\"id\":1}}," +
                                        "--->{\"id\":3,\"name\":\"cd\",\"parent\":{\"id\":2}}," +
                                        "--->{\"id\":4,\"name\":\"vim\",\"parent\":{\"id\":2}}," +
                                        "--->{\"id\":11,\"name\":\"purge\",\"parent\":{\"id\":8}}," +
                                        "--->{\"id\":12,\"name\":\"ssh\",\"parent\":{\"id\":8}}" +
                                        "]"
                        );
                    }
            );
        }
    }
    @Test
    public void testRecursive() {
        FileTable table = FileTable.$;
        FileFilter.withUser(2L, () -> {
            for (int i = 0; i < 2; i++) {
                boolean useSql = i == 0;
                executeAndExpect(
                        sqlClient
                                .createQuery(table)
                                .where(table.parentId().isNull())
                                .orderBy(table.id().asc())
                                .select(
                                        table.fetch(
                                                FileFetcher.$
                                                        .allScalarFields()
                                                        .childFiles(
                                                                FileFetcher.$.allScalarFields(),
                                                                RecursiveListFieldConfig::recursive
                                                        )
                                        )
                                ),
                        ctx -> {
                            ctx.sql(
                                    "select tb_1_.ID, tb_1_.NAME " +
                                            "from FILE tb_1_ " +
                                            "where " +
                                            "--->tb_1_.PARENT_ID is null " +
                                            "and " +
                                            "--->exists(" +
                                            "--->--->select 1 " +
                                            "--->--->from FILE_USER_MAPPING tb_3_ " +
                                            "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                            "--->) " +
                                            "order by tb_1_.ID asc"
                            );
                            if (useSql) {
                                ctx.statement(1).sql(
                                        "select tb_1_.PARENT_ID, tb_1_.ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.PARENT_ID in (?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                                "--->) " +
                                                "order by tb_1_.ID asc"
                                );
                                ctx.statement(2).sql(
                                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                                "--->)"
                                );
                                ctx.statement(3).sql(
                                        "select tb_1_.PARENT_ID, tb_1_.ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                                "--->) " +
                                                "order by tb_1_.ID asc"
                                );
                                ctx.statement(4).sql(
                                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                                "--->)"
                                );
                                ctx.statement(5).sql(
                                        "select tb_1_.PARENT_ID, tb_1_.ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                                "--->) " +
                                                "order by tb_1_.ID asc"
                                );
                                ctx.statement(6).sql(
                                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.ID in (?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                                "--->)"
                                );
                                ctx.statement(7).sql(
                                        "select tb_1_.PARENT_ID, tb_1_.ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.PARENT_ID in (?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                                "--->) " +
                                                "order by tb_1_.ID asc"
                                );
                                ctx.statement(8).sql(
                                        "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.ID in (?, ?, ?, ?, ?, ?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_2_ " +
                                                "--->--->where tb_2_.FILE_ID = tb_1_.ID and tb_2_.USER_ID = ?" +
                                                "--->)"
                                );
                                ctx.statement(9).sql(
                                        "select tb_1_.PARENT_ID, tb_1_.ID " +
                                                "from FILE tb_1_ " +
                                                "where " +
                                                "--->tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?) " +
                                                "and " +
                                                "--->exists(" +
                                                "--->--->select 1 " +
                                                "--->--->from FILE_USER_MAPPING tb_3_ " +
                                                "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                                "--->) " +
                                                "order by tb_1_.ID asc"
                                );
                            }
                            ctx.rows(
                                    "[" +
                                            "--->{" +
                                            "--->--->\"id\":1," +
                                            "--->--->\"name\":\"usr\"," +
                                            "--->--->\"childFiles\":[" +
                                            "--->--->--->{" +
                                            "--->--->--->--->\"id\":2," +
                                            "--->--->--->--->\"name\":\"bin\"," +
                                            "--->--->--->--->\"childFiles\":[" +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":3," +
                                            "--->--->--->--->--->--->\"name\":\"cd\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}," +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":4," +
                                            "--->--->--->--->--->--->\"name\":\"vim\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}," +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":6," +
                                            "--->--->--->--->--->--->\"name\":\"wait\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}," +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":7," +
                                            "--->--->--->--->--->--->\"name\":\"which\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}" +
                                            "--->--->--->--->]" +
                                            "--->--->--->}," +
                                            "--->--->--->{" +
                                            "--->--->--->--->\"id\":8," +
                                            "--->--->--->--->\"name\":\"sbin\"," +
                                            "--->--->--->--->\"childFiles\":[" +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":9," +
                                            "--->--->--->--->--->--->\"name\":\"ipconfig\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}," +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":11," +
                                            "--->--->--->--->--->--->\"name\":\"purge\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}," +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":12," +
                                            "--->--->--->--->--->--->\"name\":\"ssh\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}" +
                                            "--->--->--->--->]" +
                                            "--->--->--->}," +
                                            "--->--->--->{" +
                                            "--->--->--->--->\"id\":20," +
                                            "--->--->--->--->\"name\":\"share\"," +
                                            "--->--->--->--->\"childFiles\":[" +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":22," +
                                            "--->--->--->--->--->--->\"name\":\"dict\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}," +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":23," +
                                            "--->--->--->--->--->--->\"name\":\"sandbox\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}," +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":25," +
                                            "--->--->--->--->--->--->\"name\":\"locale\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->}" +
                                            "--->--->--->--->]" +
                                            "--->--->--->}," +
                                            "--->--->--->{" +
                                            "--->--->--->--->\"id\":26," +
                                            "--->--->--->--->\"name\":\"local\"," +
                                            "--->--->--->--->\"childFiles\":[" +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":27," +
                                            "--->--->--->--->--->--->\"name\":\"include\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[" +
                                            "--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->\"id\":28," +
                                            "--->--->--->--->--->--->--->--->\"name\":\"node\"," +
                                            "--->--->--->--->--->--->--->--->\"childFiles\":[" +
                                            "--->--->--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->--->--->\"id\":29," +
                                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"v8-external.h\"," +
                                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->--->--->--->--->}," +
                                            "--->--->--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->--->--->\"id\":30," +
                                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"v8-internal.h\"," +
                                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->--->--->--->--->}," +
                                            "--->--->--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->--->--->\"id\":32," +
                                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"v8-object.h\"," +
                                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->--->--->--->--->}," +
                                            "--->--->--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->--->--->\"id\":33," +
                                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"v8-platform.h\"," +
                                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->--->--->--->--->}" +
                                            "--->--->--->--->--->--->--->--->]" +
                                            "--->--->--->--->--->--->--->}" +
                                            "--->--->--->--->--->--->]" +
                                            "--->--->--->--->--->}," +
                                            "--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->\"id\":34," +
                                            "--->--->--->--->--->--->\"name\":\"lib\"," +
                                            "--->--->--->--->--->--->\"childFiles\":[" +
                                            "--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->\"id\":35," +
                                            "--->--->--->--->--->--->--->--->\"name\":\"node_modules\"," +
                                            "--->--->--->--->--->--->--->--->\"childFiles\":[" +
                                            "--->--->--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->--->--->\"id\":36," +
                                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"npm\"," +
                                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->--->--->--->--->}," +
                                            "--->--->--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->--->--->\"id\":37," +
                                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"corepack\"," +
                                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->--->--->--->--->}," +
                                            "--->--->--->--->--->--->--->--->--->{" +
                                            "--->--->--->--->--->--->--->--->--->--->\"id\":39," +
                                            "--->--->--->--->--->--->--->--->--->--->\"name\":\"docsify-cli\"," +
                                            "--->--->--->--->--->--->--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->--->--->--->--->--->--->}" +
                                            "--->--->--->--->--->--->--->--->]" +
                                            "--->--->--->--->--->--->--->}" +
                                            "--->--->--->--->--->--->]" +
                                            "--->--->--->--->--->}" +
                                            "--->--->--->--->]" +
                                            "--->--->--->}" +
                                            "--->--->]" +
                                            "--->}," +
                                            "--->{" +
                                            "--->--->\"id\":40," +
                                            "--->--->\"name\":\"etc\"," +
                                            "--->--->\"childFiles\":[" +
                                            "--->--->--->{" +
                                            "--->--->--->--->\"id\":41," +
                                            "--->--->--->--->\"name\":\"passwd\"," +
                                            "--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->}," +
                                            "--->--->--->{" +
                                            "--->--->--->--->\"id\":43," +
                                            "--->--->--->--->\"name\":\"ssh\"," +
                                            "--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->}," +
                                            "--->--->--->{" +
                                            "--->--->--->--->\"id\":44," +
                                            "--->--->--->--->\"name\":\"profile\"," +
                                            "--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->}," +
                                            "--->--->--->{" +
                                            "--->--->--->--->\"id\":45," +
                                            "--->--->--->--->\"name\":\"services\"," +
                                            "--->--->--->--->\"childFiles\":[]" +
                                            "--->--->--->}" +
                                            "--->--->]" +
                                            "--->}" +
                                            "]"
                            );
                        }
                );
            }
        });
        List<String> subKeys = valueMap
                .values()
                .stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toList());
        Assertions.assertEquals(31, subKeys.size());
        for (String subKey : subKeys) {
            Assertions.assertEquals("{\"userId\":2}", subKey);
        }
    }
}
