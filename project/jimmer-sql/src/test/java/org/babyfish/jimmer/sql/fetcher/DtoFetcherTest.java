package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.babyfish.jimmer.sql.model.dto.BookInput;
import org.babyfish.jimmer.sql.model.dto.BookStoreDto;
import org.babyfish.jimmer.sql.model.dto.TreeDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DtoFetcherTest extends AbstractQueryTest {

    @Test
    public void testBookStoreDto() {
        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(Constants.manningId))
                        .select(table.fetch(BookStoreDto.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.STORE_ID = ?"
                    );
                    ctx.statement(2).sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?)"
                    );
                    ctx.rows(rows -> {
                        BookStoreDto dto = rows.get(0);
                        expect(
                                "BookStoreDto{" +
                                        "--->id=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                                        "--->name=MANNING, " +
                                        "--->website=null, " +
                                        "--->version=0, " +
                                        "--->books=[" +
                                        "--->--->BookStoreDto.TargetOf_books{" +
                                        "--->--->--->id=a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                                        "--->--->--->name=GraphQL in Action, " +
                                        "--->--->--->edition=1, " +
                                        "--->--->--->price=80.00, " +
                                        "--->--->--->authors=[" +
                                        "--->--->--->--->BookStoreDto.TargetOf_books.TargetOf_authors{" +
                                        "--->--->--->--->--->id=eb4963fd-5223-43e8-b06b-81e6172ee7ae, " +
                                        "--->--->--->--->--->firstName=Samer, " +
                                        "--->--->--->--->--->lastName=Buna, " +
                                        "--->--->--->--->--->gender=MALE" +
                                        "--->--->--->--->}" +
                                        "--->--->--->]" +
                                        "--->--->}, BookStoreDto.TargetOf_books{" +
                                        "--->--->--->id=e37a8344-73bb-4b23-ba76-82eac11f03e6, " +
                                        "--->--->--->name=GraphQL in Action, " +
                                        "--->--->--->edition=2, " +
                                        "--->--->--->price=81.00, " +
                                        "--->--->--->authors=[" +
                                        "--->--->--->--->BookStoreDto.TargetOf_books.TargetOf_authors{" +
                                        "--->--->--->--->--->id=eb4963fd-5223-43e8-b06b-81e6172ee7ae, " +
                                        "--->--->--->--->--->firstName=Samer, " +
                                        "--->--->--->--->--->lastName=Buna, " +
                                        "--->--->--->--->--->gender=MALE" +
                                        "--->--->--->--->}" +
                                        "--->--->--->]" +
                                        "--->--->}, BookStoreDto.TargetOf_books{" +
                                        "--->--->--->id=780bdf07-05af-48bf-9be9-f8c65236fecc, " +
                                        "--->--->--->name=GraphQL in Action, " +
                                        "--->--->--->edition=3, " +
                                        "--->--->--->price=80.00, " +
                                        "--->--->--->authors=[" +
                                        "--->--->--->--->BookStoreDto.TargetOf_books.TargetOf_authors{" +
                                        "--->--->--->--->--->id=eb4963fd-5223-43e8-b06b-81e6172ee7ae, " +
                                        "--->--->--->--->--->firstName=Samer, " +
                                        "--->--->--->--->--->lastName=Buna, " +
                                        "--->--->--->--->--->gender=MALE" +
                                        "--->--->--->--->}" +
                                        "--->--->--->]" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}",
                                dto
                        );
                    });
                }
        );
    }

    @Test
    public void testBookInput() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(Constants.graphQLInActionId3))
                        .select(table.fetch(BookInput.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.AUTHOR_ID from BOOK_AUTHOR_MAPPING as tb_1_ " +
                                    "where tb_1_.BOOK_ID = ?"
                    );
                    ctx.rows(rows -> {
                        BookInput input = rows.get(0);
                        expect(
                                "BookInput{" +
                                        "--->id=780bdf07-05af-48bf-9be9-f8c65236fecc, " +
                                        "--->name=GraphQL in Action, " +
                                        "--->edition=3, " +
                                        "--->price=80.00, " +
                                        "--->storeId=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                                        "--->authorIds=[eb4963fd-5223-43e8-b06b-81e6172ee7ae]" +
                                        "}",
                                input
                        );
                    });
                }
        );
    }

    @Test
    public void testTreeDto() {
        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.parent().isNull())
                        .select(table.fetch(TreeDto.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID is null"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID = ? order by tb_1_.NODE_ID asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?) order by tb_1_.NODE_ID asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?, ?, ?) order by tb_1_.NODE_ID asc"
                    );
                    ctx.statement(4).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?) order by tb_1_.NODE_ID asc"
                    );
                    ctx.statement(5).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?) order by tb_1_.NODE_ID asc"
                    );
                    ctx.rows(dtoList -> {
                        assertContent(
                                "[" +
                                        "--->TreeDto{" +
                                        "--->--->id=1, " +
                                        "--->--->name=Home, " +
                                        "--->--->childNodes=[" +
                                        "--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->id=2, " +
                                        "--->--->--->--->name=Food, " +
                                        "--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->id=3, " +
                                        "--->--->--->--->--->--->name=Drinks, " +
                                        "--->--->--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->id=4, " +
                                        "--->--->--->--->--->--->--->--->name=Coca Cola, " +
                                        "--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->id=5, " +
                                        "--->--->--->--->--->--->--->--->name=Fanta, " +
                                        "--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->}" +
                                        "--->--->--->--->--->--->]" +
                                        "--->--->--->--->--->}, " +
                                        "--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->id=6, " +
                                        "--->--->--->--->--->--->name=Bread, " +
                                        "--->--->--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->id=7, " +
                                        "--->--->--->--->--->--->--->--->name=Baguette, " +
                                        "--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->id=8, " +
                                        "--->--->--->--->--->--->--->--->name=Ciabatta, " +
                                        "--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->}" +
                                        "--->--->--->--->--->--->]" +
                                        "--->--->--->--->--->}" +
                                        "--->--->--->--->]" +
                                        "--->--->--->}, " +
                                        "--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->id=9, " +
                                        "--->--->--->--->name=Clothing, " +
                                        "--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->id=10, " +
                                        "--->--->--->--->--->--->name=Woman, " +
                                        "--->--->--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->id=11, " +
                                        "--->--->--->--->--->--->--->--->name=Casual wear, " +
                                        "--->--->--->--->--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=12, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Dress, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=13, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Miniskirt, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=14, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Jeans, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}" +
                                        "--->--->--->--->--->--->--->--->]" +
                                        "--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->id=15, " +
                                        "--->--->--->--->--->--->--->--->name=Formal wear, " +
                                        "--->--->--->--->--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=16, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Suit, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=17, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Shirt, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}" +
                                        "--->--->--->--->--->--->--->--->]" +
                                        "--->--->--->--->--->--->--->}" +
                                        "--->--->--->--->--->--->]" +
                                        "--->--->--->--->--->}, " +
                                        "--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->id=18, " +
                                        "--->--->--->--->--->--->name=Man, " +
                                        "--->--->--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->id=19, " +
                                        "--->--->--->--->--->--->--->--->name=Casual wear, " +
                                        "--->--->--->--->--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=20, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Jacket, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=21, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Jeans, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}" +
                                        "--->--->--->--->--->--->--->--->]" +
                                        "--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->id=22, " +
                                        "--->--->--->--->--->--->--->--->name=Formal wear, " +
                                        "--->--->--->--->--->--->--->--->childNodes=[" +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=23, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Suit, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}, " +
                                        "--->--->--->--->--->--->--->--->--->TreeDto.TargetOf_childNodes{" +
                                        "--->--->--->--->--->--->--->--->--->--->id=24, " +
                                        "--->--->--->--->--->--->--->--->--->--->name=Shirt, " +
                                        "--->--->--->--->--->--->--->--->--->--->childNodes=[]" +
                                        "--->--->--->--->--->--->--->--->--->}" +
                                        "--->--->--->--->--->--->--->--->]" +
                                        "--->--->--->--->--->--->--->}" +
                                        "--->--->--->--->--->--->]" +
                                        "--->--->--->--->--->}" +
                                        "--->--->--->--->]" +
                                        "--->--->--->}" +
                                        "--->--->]" +
                                        "--->}" +
                                        "]",
                                dtoList.toString()
                        );
                    });
                }
        );
    }

    private static void assertContent(String json, Object o) {
        Assertions.assertEquals(
                json.replace("--->", ""),
                o.toString()
        );
    }
}
