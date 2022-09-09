package org.babyfish.jimmer.sql.example.shell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.fluent.Fluent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

@ShellComponent
public class PaginationQueryShell {

    private final JSqlClient sqlClient;

    private final ObjectWriter prettyWriter;

    public PaginationQueryShell(JSqlClient sqlClient, ObjectWriter prettyWriter) {
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

        Fluent fluent = sqlClient.createFluent();
        BookTable book = new BookTable();
        AuthorTableEx author = new AuthorTableEx();

        ConfigurableRootQuery<BookTable, Tuple3<Book, Integer, Integer>> query = fluent
                .query(book)
                .whereIf(
                        name != null && !name.isEmpty(),
                        () -> book.name().ilike(name)
                )
                .whereIf(
                        storeName != null && !storeName.isEmpty(),
                        () -> book.store().name().ilike(storeName)
                )
                .whereIf(
                        authorName != null && !authorName.isEmpty(),
                        () -> book.id().in(fluent
                                .subQuery(author)
                                .where(
                                        Predicate.or(
                                                author.firstName().ilike(authorName),
                                                author.lastName().ilike(authorName)
                                        )
                                )
                                .select(author.books().id())
                        )
                )
                .select(
                        fetch ?
                                book.fetch(
                                        BookFetcher.$
                                                .allScalarFields()
                                                .store(
                                                        BookStoreFetcher.$
                                                                .allScalarFields()
                                                                .avgPrice()
                                                )
                                                .authors(
                                                        AuthorFetcher.$
                                                                .allScalarFields()
                                                )
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
        TypedRootQuery<Long> countQuery = query
                .reselect((q, t) -> q.select(t.count()))
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
                        "Book: " + prettyWriter.writeValueAsString(row.get_1())
                );
                System.out.println("Global rank: " + row.get_2());
                System.out.println("Partition rank: " + row.get_3());
            }
        }

        System.out.println("----Output end-----------------------------------");
        System.out.println();
    }
}
