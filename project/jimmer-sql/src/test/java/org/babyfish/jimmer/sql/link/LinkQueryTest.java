package org.babyfish.jimmer.sql.link;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.link.CourseFetcher;
import org.babyfish.jimmer.sql.model.link.StudentFetcher;
import org.babyfish.jimmer.sql.model.link.StudentTable;
import org.junit.jupiter.api.Test;

public class LinkQueryTest extends AbstractQueryTest {

    @Test
    public void fetchStudentsWithView() {
        StudentTable table = StudentTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        StudentFetcher.$
                                                .allScalarFields()
                                                .courses(
                                                        CourseFetcher.$
                                                                .allScalarFields(),
                                                        cfg -> cfg.filter(
                                                                args -> args.orderBy(
                                                                        args.getTable().name().desc()
                                                                )
                                                        )
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from STUDENT as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.STUDENT_ID, tb_1_.ID, tb_1_.COURSE_ID " +
                                    "from LEARNING_LINK as tb_1_ " +
                                    "inner join COURSE as tb_3_ on tb_1_.COURSE_ID = tb_3_.ID " +
                                    "where tb_1_.STUDENT_ID in (?, ?) " +
                                    "order by tb_3_.NAME desc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME from COURSE as tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Oakes\"," +
                                    "--->--->\"courses\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"SQL\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"Kotlin\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"Roach\"," +
                                    "--->--->\"courses\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"SQL\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"Java\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
