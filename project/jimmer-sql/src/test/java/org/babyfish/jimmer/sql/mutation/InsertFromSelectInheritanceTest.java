package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.model.inheritance.singletable.OrganizationTable;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InsertFromSelectInheritanceTest extends AbstractMutationTest {

    @Test
    public void testInsertAddsSingleTableDiscriminator() {
        BaseTable3<NumericExpression<Long>, StringExpression, StringExpression> source =
                source(399L, "New Organization", "NEW-399");
        OrganizationTable table = OrganizationTable.$;
        connectAndExpect(
                con -> {
                    int rowCount = getSqlClient()
                            .createInsert(table, source)
                            .set(table.id(), source.get_1())
                            .set(table.name(), source.get_2())
                            .set(table.taxCode(), source.get_3())
                            .execute(con);
                    return rowCount + "; " + clientRow(con, 399L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into CLIENT(ID, NAME, TAX_CODE, CLIENT_TYPE) " +
                                        "select tb_1_.c1, tb_1_.c2, tb_1_.c3, ? from (" +
                                        "select cast(? as bigint) as c1, " +
                                        "cast(? as varchar) as c2, " +
                                        "cast(? as varchar) as c3" +
                                        ") tb_1_"
                        );
                        it.variables("ORG", 399L, "New Organization", "NEW-399");
                    });
                    ctx.value("1; [ORG, New Organization, NEW-399, null, null]");
                }
        );
    }

    @Test
    public void testUpsertUpdatesMatchingSingleTableSubtype() {
        BaseTable3<NumericExpression<Long>, StringExpression, StringExpression> source =
                source(100L, "Acme+", "ACME-002");
        OrganizationTable table = OrganizationTable.$;
        connectAndExpect(
                con -> {
                    int rowCount = getSqlClient()
                            .createUpsert(table, source)
                            .key(table.id(), source.get_1())
                            .merge(table.name(), source.get_2())
                            .merge(table.taxCode(), source.get_3())
                            .execute(con);
                    return rowCount + "; " + clientRow(con, 100L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(upsertSql());
                        it.variables(100L, "Acme+", "ACME-002", "ORG", "ORG");
                    });
                    ctx.value("1; [ORG, Acme+, ACME-002, null, null]");
                }
        );
    }

    @Test
    public void testUpsertDoesNotUpdateRowWithDifferentDiscriminator() {
        BaseTable3<NumericExpression<Long>, StringExpression, StringExpression> source =
                source(101L, "Should not update", "SHOULD-NOT-WRITE");
        OrganizationTable table = OrganizationTable.$;
        connectAndExpect(
                con -> {
                    int rowCount = getSqlClient()
                            .createUpsert(table, source)
                            .key(table.id(), source.get_1())
                            .merge(table.name(), source.get_2())
                            .merge(table.taxCode(), source.get_3())
                            .execute(con);
                    return rowCount + "; " + clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(upsertSql());
                        it.variables(101L, "Should not update", "SHOULD-NOT-WRITE", "ORG", "ORG");
                    });
                    ctx.value("0; [Person, Bob, null, Bob, Brown]");
                }
        );
    }

    @Test
    public void testMaterializedUpsertRejectsDifferentDiscriminator() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        OrganizationTable sourceTable = OrganizationTable.$;
        BaseTable3<NumericExpression<Long>, StringExpression, StringExpression> source =
                sqlClient
                        .createBaseQuery(sourceTable)
                        .where(sourceTable.id().eq(100L))
                        .addSelect(sourceTable.id().plus(1L))
                        .addSelect(sourceTable.name().concat(" should not update"))
                        .addSelect(sourceTable.taxCode().concat("-SHOULD-NOT-WRITE"))
                        .asBaseTable();
        OrganizationTable table = OrganizationTable.$;
        connectAndExpect(
                con -> {
                    List<Long> ids = sqlClient
                            .createUpsert(table, source)
                            .key(table.id(), source.get_1())
                            .merge(
                                    table.name(),
                                    source.get_2(),
                                    table.name().concat(source.get_2())
                            )
                            .merge(table.taxCode(), source.get_3())
                            .updateWhere(source.get_2().ne(table.name()))
                            .returning(table.id())
                            .execute(con);
                    return ids + "; " + clientRow(con, 101L);
                },
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.c1, tb_1_.c2, tb_1_.c3, ? from (" +
                                    "select tb_1_.ID + ? c1, " +
                                    "concat(tb_1_.NAME, ?) c2, " +
                                    "concat(tb_1_.TAX_CODE, ?) c3 " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?) tb_1_"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID from CLIENT tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE from CLIENT tb_1_ where tb_1_.ID = ?"
                    ));
                    ctx.value("[]; [Person, Bob, null, Bob, Brown]");
                }
        );
    }

    private BaseTable3<NumericExpression<Long>, StringExpression, StringExpression> source(
            long id,
            String name,
            String taxCode
    ) {
        return getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(id))
                .addSelect(Expression.value(name))
                .addSelect(Expression.value(taxCode))
                .asBaseTable();
    }

    private static String upsertSql() {
        return "merge into CLIENT tb_2_ using (" +
                "select cast(? as bigint) as c1, " +
                "cast(? as varchar) as c2, " +
                "cast(? as varchar) as c3" +
                ") tb_1_ on tb_2_.ID = tb_1_.c1 " +
                "when matched and tb_2_.CLIENT_TYPE = ? " +
                "then update set tb_2_.NAME = tb_1_.c2, tb_2_.TAX_CODE = tb_1_.c3 " +
                "when not matched then insert(ID, NAME, TAX_CODE, CLIENT_TYPE) " +
                "values(tb_1_.c1, tb_1_.c2, tb_1_.c3, ?)";
    }

    private static String clientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select CLIENT_TYPE, NAME, TAX_CODE, FIRST_NAME, LAST_NAME from CLIENT where ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return "[" +
                        rs.getString(1) + ", " +
                        rs.getString(2) + ", " +
                        rs.getString(3) + ", " +
                        rs.getString(4) + ", " +
                        rs.getString(5) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
