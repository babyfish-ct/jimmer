package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.dto.BookView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryTest extends AbstractQueryTest {

    @Test
    public void testQuery() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.name().eq("GraphQL in Action"))
                        .orderBy(table.edition().desc())
                        .select(
                                table.fetch(BookView.class)
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ where tb_1_.NAME = ? " +
                                    "order by tb_1_.EDITION desc"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "--->tb_2_.BOOK_ID, " +
                                    "--->tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ " +
                                    "--->on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?)"
                    );
                    ctx.rows(rows -> {
                        assertContentEquals(
                                "[" +
                                        "--->BookView(" +
                                        "--->--->name=GraphQL in Action, " +
                                        "--->--->edition=3, " +
                                        "--->--->id=780bdf07-05af-48bf-9be9-f8c65236fecc, " +
                                        "--->--->store=BookView.TargetOf_store(" +
                                        "--->--->--->name=MANNING" +
                                        "--->--->), " +
                                        "--->--->authors=[" +
                                        "--->--->--->BookView.TargetOf_authors(" +
                                        "--->--->--->--->firstName=Samer, " +
                                        "--->--->--->--->lastName=Buna" +
                                        "--->--->--->)" +
                                        "--->--->]" +
                                        "--->), " +
                                        "--->BookView(" +
                                        "--->--->name=GraphQL in Action, " +
                                        "--->--->edition=2, " +
                                        "--->--->id=e37a8344-73bb-4b23-ba76-82eac11f03e6, " +
                                        "--->--->store=BookView.TargetOf_store(" +
                                        "--->--->--->name=MANNING" +
                                        "--->--->), " +
                                        "--->--->authors=[" +
                                        "--->--->--->BookView.TargetOf_authors(" +
                                        "--->--->--->--->firstName=Samer, " +
                                        "--->--->--->--->lastName=Buna" +
                                        "--->--->--->)" +
                                        "--->--->]" +
                                        "--->), " +
                                        "--->BookView(" +
                                        "--->--->name=GraphQL in Action, " +
                                        "--->--->edition=1, " +
                                        "--->--->id=a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                                        "--->--->store=BookView.TargetOf_store(name=MANNING), " +
                                        "--->--->authors=[" +
                                        "--->--->--->BookView.TargetOf_authors(" +
                                        "--->--->--->--->firstName=Samer, " +
                                        "--->--->--->--->lastName=Buna" +
                                        "--->--->--->)" +
                                        "--->--->]" +
                                        "--->)" +
                                        "]",
                                rows
                        );
                    });
                }
        );
    }

    @Test
    public void findById() {
        connectAndExpect(
                con -> {
                    return getSqlClient()
                            .getEntities()
                            .forConnection(con)
                            .findById(BookView.class, Constants.programmingTypeScriptId2);
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID from BOOK tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME from AUTHOR tb_1_ inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID where tb_2_.BOOK_ID = ?"
                    );
                    ctx.rows(rows -> {
                        assertContentEquals(
                                "[" +
                                        "--->BookView(" +
                                        "--->--->name=Programming TypeScript, " +
                                        "--->--->edition=2, " +
                                        "--->--->id=058ecfd0-047b-4979-a7dc-46ee24d08f08, " +
                                        "--->--->store=BookView.TargetOf_store(name=O'REILLY), " +
                                        "--->--->authors=[" +
                                        "--->--->--->BookView.TargetOf_authors(firstName=Boris, lastName=Cherny)" +
                                        "--->--->]" +
                                        "--->)" +
                                        "]",
                                rows
                        );
                    });
                }
        );
    }

    @Test
    public void testFindAll() {
        connectAndExpect(
                con -> {
                    return getSqlClient()
                            .getEntities()
                            .forConnection(con)
                            .findAll(BookView.class);
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.statement(2).sql(
                            "select " +
                                    "--->tb_2_.BOOK_ID, " +
                                    "--->tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from AUTHOR tb_1_ inner join BOOK_AUTHOR_MAPPING tb_2_ " +
                                    "--->on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                    ctx.rows(rows -> {
                        Assertions.assertEquals(12, rows.size());
                        for (Object o : rows) {
                            Assertions.assertTrue(o instanceof BookView);
                        }
                    });
                }
        );
    }
}
