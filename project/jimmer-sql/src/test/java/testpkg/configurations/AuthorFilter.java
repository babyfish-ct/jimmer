package testpkg.configurations;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.FieldFilterArgs;
import org.babyfish.jimmer.sql.model.AuthorTable;

public class AuthorFilter implements FieldFilter<AuthorTable> {

    @Override
    public void apply(FieldFilterArgs<AuthorTable> args) {
        AuthorTable table = args.getTable();
        args
                .where(
                        Predicate.and(
                                table.firstName().ne("Alex"),
                                table.lastName().ne("Banks")
                        )
                )
                .orderBy(
                        table.firstName().asc(),
                        table.lastName().asc()
                );
    }
}
