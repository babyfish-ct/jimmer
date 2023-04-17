package org.babyfish.jimmer.sql.joinsql;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.joinsql.CategoryFetcher;
import org.babyfish.jimmer.sql.model.joinsql.CategoryTable;
import org.babyfish.jimmer.sql.model.joinsql.PostFetcher;
import org.babyfish.jimmer.sql.model.joinsql.PostTable;
import org.junit.jupiter.api.Test;

public class FetchBySqlJoinTest extends AbstractQueryTest {

    @Test
    public void testFetchPostWithCategories() {
        PostTable table = PostTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        PostFetcher.$
                                                .allScalarFields()
                                                .categories(
                                                        CategoryFetcher.$
                                                                .allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from POST as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_2_.ID, tb_1_.ID, tb_1_.NAME " +
                                    "from CATEGORY as tb_1_ " +
                                    "inner join POST as tb_2_ " +
                                    "--->on contains_id(tb_2_.category_ids, tb_1_.id) " +
                                    "where tb_2_.ID in (?, ?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"post-1\"," +
                                    "--->--->\"categories\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"category-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"category-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"post-2\"," +
                                    "--->--->\"categories\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"category-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"category-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":3," +
                                    "--->--->\"name\":\"post-3\"," +
                                    "--->--->\"categories\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"category-2\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"category-3\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":4," +
                                    "--->--->\"name\":\"post-4\"," +
                                    "--->--->\"categories\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"category-2\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"category-3\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFetchCategoriesWithPost() {
        CategoryTable table = CategoryTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        CategoryFetcher.$
                                                .allScalarFields()
                                                .posts(
                                                        PostFetcher.$
                                                                .allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from CATEGORY as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_2_.ID, tb_1_.ID, tb_1_.NAME " +
                                    "from POST as tb_1_ " +
                                    "inner join CATEGORY as tb_2_ " +
                                    "--->on contains_id(tb_1_.category_ids, tb_2_.id) " +
                                    "where tb_2_.ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"category-1\"," +
                                    "--->--->\"posts\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"post-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"post-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"category-2\"," +
                                    "--->--->\"posts\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"post-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"post-2\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"post-3\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":4," +
                                    "--->--->--->--->\"name\":\"post-4\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":3," +
                                    "--->--->\"name\":\"category-3\"," +
                                    "--->--->\"posts\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"post-3\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":4," +
                                    "--->--->--->--->\"name\":\"post-4\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
