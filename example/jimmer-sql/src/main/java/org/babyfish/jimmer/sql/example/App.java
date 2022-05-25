package org.babyfish.jimmer.sql.example;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.example.model.*;

import java.io.IOException;
import java.util.List;

import static org.babyfish.jimmer.sql.example.AppContext.*;

public class App {

    public static void main(String[] args) throws IOException {
        try {
            showData(
                    Console.readLine("Please input name filter of current book object (Optional): "),
                    Console.readLine("Please input name filter of parent store object (Optional): "),
                    Console.readLine("Please input name filter of child author object (Optional): "),
                    2
            );
        } finally {
            AppContext.close();
        }
    }

    private static void showData(
            String name,
            String storeName,
            String authorName,
            int pageSize
    ) {
        ConfigurableTypedRootQuery<BookTable, Tuple3<Book, Integer, Integer>> query =
                SQL_CLIENT.createQuery(BookTable.class, (q, book) -> {
                    if (name != null && !name.isEmpty()) {
                        q.where(book.name().ilike(name));
                    }
                    if (storeName != null && !storeName.isEmpty()) {
                        q.where(book.store().name().ilike(storeName));
                    }
                    if (authorName != null && !authorName.isEmpty()) {
                        q.where(
                                book.id().in(
                                        q.createSubQuery(AuthorTableEx.class, (sq, author) -> {
                                            sq.where(
                                                    author.firstName().ilike(authorName).or(
                                                            author.lastName().ilike(authorName)
                                                    )
                                            );
                                            return sq.select(author.books().id());
                                        })
                                )
                        );
                    }
                    q.orderBy(book.name());
                    q.orderBy(book.edition(), OrderMode.DESC);
                    return q.select(
                            book.fetch(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .store(BookStoreFetcher.$.allScalarFields())
                                            .authors(AuthorFetcher.$.allScalarFields())
                            ),
                            Expression.numeric().sql(
                                    Integer.class,
                                    "rank() over(order by %e desc)",
                                    it -> it.expression(book.price())
                            ),
                            Expression.numeric().sql(
                                    Integer.class,
                                    "rank() over(partition by %e order by %e desc)",
                                    it -> it
                                            .expression(book.store().id())
                                            .expression(book.price())
                            )
                    );
                });

        TypedRootQuery<Long> countQuery = query
                .reselect((oldQuery, book) -> oldQuery.select(book.count()))
                .withoutSortingAndPaging();

        int rowCount = countQuery.execute().get(0).intValue();
        int pageCount = (rowCount + pageSize - 1) / pageSize;
        System.out.println("-------------------------------------------------");
        System.out.println("Total row count: " + rowCount + ", pageCount: " + pageCount);
        System.out.println("-------------------------------------------------");
        System.out.println();

        for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
            System.out.println("-----------Page no: " + pageNo + "-----------");
            System.out.println();
            int offset = pageSize * (pageNo - 1);
            List<Tuple3<Book, Integer, Integer>> rows =
                    query
                            .limit(pageSize, offset)
                            .execute();
            for (Tuple3<Book, Integer, Integer> row : rows) {
                System.out.println("book object: " + row._1());
                System.out.println("global price rank: " + row._2());
                System.out.println("price rank in own store: " + row._3());
                System.out.println();
            }
        }
    }
}
