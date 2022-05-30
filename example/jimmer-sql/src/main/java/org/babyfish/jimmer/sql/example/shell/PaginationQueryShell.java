package org.babyfish.jimmer.sql.example.shell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.example.model.*;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

@ShellComponent
public class PaginationQueryShell {

    private final SqlClient sqlClient;

    private final ObjectWriter prettyWriter;

    public PaginationQueryShell(SqlClient sqlClient, ObjectWriter prettyWriter) {
        this.sqlClient = sqlClient;
        this.prettyWriter = prettyWriter;
    }

    @ShellMethod(
            "Find books by --name, --store-ame, --author-name, --page-size and --fetch" +
            "(Example: books --store-name M --fetch)"
    )
    public void books(
            @ShellOption(defaultValue = "") String name,
            @ShellOption(defaultValue = "") String storeName,
            @ShellOption(defaultValue = "") String authorName,
            @ShellOption(defaultValue = "2") int pageSize,
            @ShellOption(defaultValue = "false") boolean fetch
    ) throws JsonProcessingException {
        ConfigurableTypedRootQuery<BookTable, Tuple3<Book, Integer, Integer>> query =
                sqlClient.createQuery(BookTable.class, (q, book) -> {
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
                            fetch ?
                                book.fetch(
                                        BookFetcher.$
                                                .allScalarFields()
                                                .store(BookStoreFetcher.$.allScalarFields())
                                                .authors(AuthorFetcher.$.allScalarFields())
                                ) :
                                book,
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
        System.out.println("----Output start---------------------------------");
        System.out.println("Total row count: " + rowCount + ", pageCount: " + pageCount);
        System.out.println("-------------------------------------------------");

        for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
            System.out.println("----Page no: " + pageNo + "-----------");
            int offset = pageSize * (pageNo - 1);
            List<Tuple3<Book, Integer, Integer>> rows =
                    query
                            .limit(pageSize, offset)
                            .execute();
            for (Tuple3<Book, Integer, Integer> row : rows) {
                System.out.println(
                        "Book: " + prettyWriter.writeValueAsString(row._1())
                );
                System.out.println("Global rank: " + row._2());
                System.out.println("Partition rank: " + row._3());
            }
        }

        System.out.println("----Output end-----------------------------------");
        System.out.println();
    }
}
