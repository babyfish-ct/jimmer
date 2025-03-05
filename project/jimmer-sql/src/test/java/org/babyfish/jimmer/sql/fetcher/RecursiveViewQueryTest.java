package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.link.CourseFetcher;
import org.babyfish.jimmer.sql.model.link.CourseTable;
import org.junit.jupiter.api.Test;

public class RecursiveViewQueryTest extends AbstractQueryTest {

    @Test
    public void testPrevCourses() {
        CourseTable table = CourseTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.name().eq("Kotlin"))
                        .select(
                                table.fetch(
                                        CourseFetcher.$
                                                .allScalarFields()
                                                .recursivePrevCourses()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.ACADEMIC_CREDIT " +
                                    "from COURSE tb_1_ " +
                                    "where tb_1_.NAME = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.PREV_COURSE_ID " +
                                    "from COURSE_DEPENDENCY tb_1_ " +
                                    "where tb_1_.NEXT_COURSE_ID = ?"
                    ).variables(2L);
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.ACADEMIC_CREDIT " +
                                    "from COURSE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(1L);
//                    ctx.statement(3).sql(
//                            "select tb_1_.ID, tb_1_.NAME, tb_1_.ACADEMIC_CREDIT " +
//                                    "from COURSE tb_1_ " +
//                                    "inner join COURSE_DEPENDENCY tb_2_ on tb_1_.ID = tb_2_.NEXT_COURSE_ID " +
//                                    "where tb_2_.PREV_COURSE_ID = ?"
//                    ).variables(3L);
                    ctx.rows(
                            ""
                    );
                }
        );
    }
}
