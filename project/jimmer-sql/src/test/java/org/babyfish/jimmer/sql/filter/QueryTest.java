package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.model.filter.*;
import org.babyfish.jimmer.sql.model.inheritance.Permission;
import org.babyfish.jimmer.sql.model.inheritance.RoleFetcher;
import org.babyfish.jimmer.sql.model.inheritance.RoleTable;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class QueryTest extends AbstractQueryTest {

    private JSqlClient sqlClient = getSqlClient(it -> {
        it.addFilters(new FileFilter());
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
    });

    @SuppressWarnings("unchecked")
    @Test
    public void testById() {
        connectAndExpect(
                con -> {
                    List<File>[] ref = new List[1];
                    FileFilter.withUser(2L, () -> {
                        ref[0] = sqlClient.findByIds(File.class, Arrays.asList(1L, 2L, 3L, 4L, 11L, 12L, 13L, 14L, 100L));
                    });
                    return ref[0];
                },
                ctx -> {
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
                                    ")"
                    ).variables(1L, 2L, 3L, 4L, 11L, 12L, 13L, 14L, 100L, 2L);
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

    @Test
    public void testRecursive() {
        FileTable table = FileTable.$;
        FileFilter.withUser(2L, () -> {
            executeAndExpect(
                    sqlClient
                            .createQuery(table)
                            .where(table.parentId().isNull())
                            .orderBy(table.id().asc())
                            .select(
                                    table.fetch(
                                            FileFetcher.$
                                                    .allScalarFields()
                                                    .recursiveChildFiles()
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
                        ctx.statement(1).sql(
                                "select tb_1_.PARENT_ID, tb_1_.ID, tb_1_.NAME " +
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
                                "select tb_1_.PARENT_ID, tb_1_.ID, tb_1_.NAME " +
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
                        ctx.statement(3).sql(
                                "select tb_1_.PARENT_ID, tb_1_.ID, tb_1_.NAME " +
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
                        ctx.statement(4).sql(
                                "select tb_1_.PARENT_ID, tb_1_.ID, tb_1_.NAME " +
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
                        ctx.statement(5).sql(
                                "select tb_1_.PARENT_ID, tb_1_.ID, tb_1_.NAME " +
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
        });
    }

    @Test
    public void testByAssociation() {
        AssociationTable<File, FileTableEx, User, UserTableEx> table =
                AssociationTable.of(FileTableEx.class, FileTableEx::users);
        FileFilter.withUser(2L, () -> {
            executeAndExpect(
                    sqlClient
                            .createAssociationQuery(table)
                            .where(table.source().id().eq(20L))
                            .select(table),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.FILE_ID, tb_1_.USER_ID " +
                                        "from FILE_USER_MAPPING tb_1_ " +
                                        "inner join FILE tb_2_ on tb_1_.FILE_ID = tb_2_.ID " +
                                        "where tb_2_.ID = ? and exists(" +
                                        "--->select 1 " +
                                        "--->from FILE_USER_MAPPING tb_3_ " +
                                        "--->where tb_3_.FILE_ID = tb_2_.ID and tb_3_.USER_ID = ?" +
                                        ")"
                        ).variables(20L, 2L);
                        ctx.rows(rows -> {
                            assertContentEquals(
                                    "[" +
                                            "Association{source={\"id\":20}, target={\"id\":1}}, " +
                                            "Association{source={\"id\":20}, target={\"id\":2}}, " +
                                            "Association{source={\"id\":20}, target={\"id\":4}}" +
                                            "]",
                                    rows
                            );
                        });
                    }
            );
        });
    }

    @Test
    public void testSubQuery() {
        FileTable table = FileTable.$;
        FileTable table2 = new FileTable();
        FileFilter.withUser(2L, () -> {
            executeAndExpect(
                    sqlClient
                            .createQuery(table)
                            .where(
                                    sqlClient
                                            .createSubQuery(table2)
                                            .where(table2.parentId().eq(table.id()))
                                            .where(table2.name().ilike("a"))
                                            .exists()
                            )
                            .select(table),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from FILE tb_1_ " +
                                        "where exists(" +
                                        "--->select 1 from FILE tb_2_ " +
                                        "--->where tb_2_.PARENT_ID = tb_1_.ID and tb_2_.NAME ilike ? and exists(" +
                                        "--->--->select 1 " +
                                        "--->--->from FILE_USER_MAPPING tb_4_ " +
                                        "--->--->where tb_4_.FILE_ID = " +
                                        "--->--->tb_2_.ID and tb_4_.USER_ID = ?" +
                                        "--->)" +
                                        ") and exists(" +
                                        "--->select 1 from " +
                                        "--->--->FILE_USER_MAPPING tb_7_ " +
                                        "--->--->where tb_7_.FILE_ID = tb_1_.ID and tb_7_.USER_ID = ?" +
                                        ")"
                        ).variables("%a%", 2L, 2L);
                        ctx.rows(
                                "[" +
                                        "--->{\"id\":40,\"name\":\"etc\",\"parent\":null}," +
                                        "--->{\"id\":1,\"name\":\"usr\",\"parent\":null}," +
                                        "--->{\"id\":2,\"name\":\"bin\",\"parent\":{\"id\":1}}," +
                                        "--->{\"id\":20,\"name\":\"share\",\"parent\":{\"id\":1}}," +
                                        "--->{\"id\":28,\"name\":\"node\",\"parent\":{\"id\":27}}," +
                                        "--->{\"id\":35,\"name\":\"node_modules\",\"parent\":{\"id\":34}}" +
                                        "]"
                        );
                    }
            );
        });
    }

    @Test
    public void testByParentId() {

        FileTable table = FileTable.$;

        FileFilter.withUser(2, () -> {
            executeAndExpect(
                    sqlClient.createQuery(table).where(table.parent().id().eq(40L)).select(table),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from FILE tb_1_ " +
                                        "inner join FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID " +
                                        "where " +
                                        "--->tb_2_.ID = ? " +
                                        "and " +
                                        "--->exists(" +
                                        "--->--->select 1 " +
                                        "--->--->from FILE_USER_MAPPING tb_3_ " +
                                        "--->--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                        "--->) " +
                                        "and " +
                                        "--->exists(" +
                                        "--->--->select 1 " +
                                        "--->--->from FILE_USER_MAPPING tb_6_ " +
                                        "--->--->where tb_6_.FILE_ID = tb_2_.ID and tb_6_.USER_ID = ?" +
                                        ")"
                        ).variables(40L, 2L, 2L);
                        ctx.rows(
                                "[" +
                                        "--->{\"id\":41,\"name\":\"passwd\",\"parent\":{\"id\":40}}," +
                                        "--->{\"id\":44,\"name\":\"profile\",\"parent\":{\"id\":40}}," +
                                        "--->{\"id\":45,\"name\":\"services\",\"parent\":{\"id\":40}}," +
                                        "--->{\"id\":43,\"name\":\"ssh\",\"parent\":{\"id\":40}}" +
                                        "]"
                        );
                    }
            );
        });
    }

    @Test
    public void testByRawParentId() {

        FileTable table = FileTable.$;

        FileFilter.withUser(2, () -> {
            executeAndExpect(
                    sqlClient.createQuery(table).where(table.parentId().eq(40L)).select(table),
                    ctx -> {
                        ctx.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from FILE tb_1_ " +
                                        "where tb_1_.PARENT_ID = ? and exists(" +
                                        "--->select 1 " +
                                        "--->from FILE_USER_MAPPING tb_3_ " +
                                        "--->where tb_3_.FILE_ID = tb_1_.ID and tb_3_.USER_ID = ?" +
                                        ")"
                        ).variables(40L, 2L);
                        ctx.rows(
                                "[" +
                                        "--->{\"id\":41,\"name\":\"passwd\",\"parent\":{\"id\":40}}," +
                                        "--->{\"id\":44,\"name\":\"profile\",\"parent\":{\"id\":40}}," +
                                        "--->{\"id\":45,\"name\":\"services\",\"parent\":{\"id\":40}}," +
                                        "--->{\"id\":43,\"name\":\"ssh\",\"parent\":{\"id\":40}}" +
                                        "]"
                        );
                    }
            );
        });
    }

    @Test
    public void testFR604() {
        RoleTable table = RoleTable.$;
        connectAndExpect(
                con -> {
                    return getSqlClient()
                            .filters(it -> it.setBehavior(Permission.class, LogicalDeletedBehavior.REVERSED))
                            .getEntities()
                            .forConnection(con)
                            .findById(
                                    RoleFetcher.$
                                            .permissions(),
                                    100L
                            );
                }, ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from ROLE tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.DELETED <> ?"
                    ).variables(100L, true);
                    ctx.statement(1).sql(
                            "select tb_1_.ID " +
                                    "from PERMISSION tb_1_ " +
                                    "where tb_1_.ROLE_ID = ? " +
                                    "and tb_1_.DELETED = ?"
                    ).variables(100L, true);
                    ctx.rows("[{\"permissions\":[{\"id\":2000}],\"id\":100}]");
                }
        );
    }
}
