package org.babyfish.jimmer.sql.link;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.link.*;
import org.junit.jupiter.api.Test;

public class LinkFetcherTest extends AbstractQueryTest {

    @Test
    public void testFetchView() {
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
                                                                .name(),
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
                                    "from STUDENT tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.STUDENT_ID, tb_1_.ID, tb_1_.COURSE_ID " +
                                    "from LEARNING_LINK tb_1_ " +
                                    "inner join COURSE tb_3_ on tb_1_.COURSE_ID = tb_3_.ID " +
                                    "where tb_1_.STUDENT_ID in (?, ?) " +
                                    "order by tb_3_.NAME desc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME from COURSE tb_1_ " +
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

    @Test
    public void testFetchViewAndRaw() {
        StudentTable table = StudentTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        StudentFetcher.$
                                                .allScalarFields()
                                                .learningLinks(
                                                        LearningLinkFetcher.$
                                                                .score()
                                                )
                                                .courses(
                                                        CourseFetcher.$
                                                                .name()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from STUDENT tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.STUDENT_ID, tb_1_.ID, tb_1_.SCORE, tb_1_.COURSE_ID " +
                                    "from LEARNING_LINK tb_1_ " +
                                    "where tb_1_.STUDENT_ID in (?, ?)"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from COURSE tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Oakes\"," +
                                    "--->--->\"learningLinks\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"course\":{" +
                                    "--->--->--->--->--->\"id\":2," +
                                    "--->--->--->--->--->\"name\":\"Kotlin\"" +
                                    "--->--->--->--->}," +
                                    "--->--->--->--->\"score\":78" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"course\":{" +
                                    "--->--->--->--->--->\"id\":3," +
                                    "--->--->--->--->--->\"name\":\"SQL\"" +
                                    "--->--->--->--->}," +
                                    "--->--->--->--->\"score\":null" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"courses\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"Kotlin\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"SQL\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"Roach\"," +
                                    "--->--->\"learningLinks\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"course\":{" +
                                    "--->--->--->--->--->\"id\":3," +
                                    "--->--->--->--->--->\"name\":\"SQL\"" +
                                    "--->--->--->--->}," +
                                    "--->--->--->--->\"score\":87" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":4," +
                                    "--->--->--->--->\"course\":{" +
                                    "--->--->--->--->--->\"id\":1," +
                                    "--->--->--->--->--->\"name\":\"Java\"" +
                                    "--->--->--->--->}," +
                                    "--->--->--->--->\"score\":null" +
                                    "--->--->--->}" +
                                    "--->--->]," +
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

    @Test
    public void testFetchViewAndRawWithChild() {
        StudentTable table = StudentTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        StudentFetcher.$
                                                .allScalarFields()
                                                .learningLinks(
                                                        LearningLinkFetcher.$
                                                                .score()
                                                                .course(
                                                                        CourseFetcher.$
                                                                                .name()
                                                                )
                                                )
                                                .courses(
                                                        CourseFetcher.$
                                                                .academicCredit()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from STUDENT tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.STUDENT_ID, tb_1_.ID, tb_1_.SCORE, tb_1_.COURSE_ID " +
                                    "from LEARNING_LINK tb_1_ " +
                                    "where tb_1_.STUDENT_ID in (?, ?)"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.ACADEMIC_CREDIT " +
                                    "from COURSE tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"Oakes\"," +
                                    "--->--->\"learningLinks\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"course\":{" +
                                    "--->--->--->--->--->\"id\":2," +
                                    "--->--->--->--->--->\"name\":\"Kotlin\"," +
                                    "--->--->--->--->--->\"academicCredit\":2" +
                                    "--->--->--->--->}," +
                                    "--->--->--->--->\"score\":78" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"course\":{" +
                                    "--->--->--->--->--->\"id\":3," +
                                    "--->--->--->--->--->\"name\":\"SQL\"," +
                                    "--->--->--->--->--->\"academicCredit\":2" +
                                    "--->--->--->--->}," +
                                    "--->--->--->--->\"score\":null" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"courses\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"Kotlin\"," +
                                    "--->--->--->--->\"academicCredit\":2" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"SQL\"," +
                                    "--->--->--->--->\"academicCredit\":2" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"Roach\"," +
                                    "--->--->\"learningLinks\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"course\":{" +
                                    "--->--->--->--->--->\"id\":3," +
                                    "--->--->--->--->--->\"name\":\"SQL\"," +
                                    "--->--->--->--->--->\"academicCredit\":2" +
                                    "--->--->--->--->}," +
                                    "--->--->--->--->\"score\":87" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":4," +
                                    "--->--->--->--->\"course\":{" +
                                    "--->--->--->--->--->\"id\":1," +
                                    "--->--->--->--->--->\"name\":\"Java\"," +
                                    "--->--->--->--->--->\"academicCredit\":2" +
                                    "--->--->--->--->}," +
                                    "--->--->--->--->\"score\":null" +
                                    "--->--->--->}" +
                                    "--->--->]," +
                                    "--->--->\"courses\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"SQL\"," +
                                    "--->--->--->--->\"academicCredit\":2" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"Java\"," +
                                    "--->--->--->--->\"academicCredit\":2" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
