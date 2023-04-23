package org.babyfish.jimmer.sql.link;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.link.CourseFetcher;
import org.babyfish.jimmer.sql.model.link.StudentFetcher;
import org.babyfish.jimmer.sql.model.link.StudentTable;
import org.junit.jupiter.api.Test;

public class LinkDSLTest extends AbstractQueryTest {

    @Test
    public void test() {
        StudentTable table = StudentTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.asTableEx().courses().name().eq("SQL"))
                        .where(table.asTableEx().learningLinks().score().isNotNull())
                        .select(
                                table.asTableEx().courses().fetch(
                                        CourseFetcher.$
                                                .allScalarFields()
                                                .students(
                                                        StudentFetcher.$
                                                                .allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_3_.ID, tb_3_.NAME, tb_3_.ACADEMIC_CREDIT " +
                                    "from STUDENT tb_1_ " +
                                    "inner join LEARNING_LINK tb_2_ " +
                                    "--->on tb_1_.ID = tb_2_.STUDENT_ID " +
                                    "inner join COURSE tb_3_ " +
                                    "--->on tb_2_.COURSE_ID = tb_3_.ID " +
                                    "where " +
                                    "--->tb_3_.NAME = ? " +
                                    "and " +
                                    "--->tb_2_.SCORE is not null"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.STUDENT_ID " +
                                    "from LEARNING_LINK tb_1_ " +
                                    "where tb_1_.COURSE_ID = ?"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from STUDENT tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":3," +
                                    "--->--->\"name\":\"SQL\"," +
                                    "--->--->\"academicCredit\":2," +
                                    "--->--->\"students\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"Oakes\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"Roach\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
