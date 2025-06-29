package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.model.Author;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeakJoinLambdaTest {

    @Test
    public void testBySimpleLambda() {
        WeakJoin<BookTableEx, AuthorTableEx> join1 =
                (book, author) -> book.authors().eq(author);
        WeakJoin<BookTableEx, AuthorTableEx> join2 =
                (b, a) -> b.authors().eq(a);
        WeakJoin<BookTableEx, AuthorTableEx> join3 =
                (book, author) -> book.eq(author.books());
        WeakJoinLambda lambda1 = JWeakJoinLambdaFactory.get(join1);
        WeakJoinLambda lambda2 = JWeakJoinLambdaFactory.get(join2);
        WeakJoinLambda lambda3 = JWeakJoinLambdaFactory.get(join3);
        Assertions.assertNotNull(lambda1);
        Assertions.assertNotNull(lambda2);
        Assertions.assertNotNull(lambda3);
        Assertions.assertEquals(lambda1.hashCode(), lambda2.hashCode());
        Assertions.assertNotEquals(lambda1.hashCode(), lambda3.hashCode());
        Assertions.assertEquals(lambda1, lambda2);
        Assertions.assertNotEquals(lambda1, lambda3);
    }

    @Test
    public void testByComplexLambda() {
        WeakJoin<BookTableEx, AuthorTableEx> join1 = (book, author) -> {
            List<Predicate> predicates = new ArrayList<>();
            Map<String, ImmutableProp> authorPropMap = author.getImmutableType().getProps();
            for (ImmutableProp prop : book.getImmutableType().getProps().values()) {
                if (prop.isScalar(TargetLevel.ENTITY)) {
                    ImmutableProp authorProp = authorPropMap.get(prop.getName());
                    if (authorProp != null && prop.getReturnClass() == authorProp.getReturnClass()) {
                        predicates.add(book.get(prop).eq(author.get(authorProp)));
                    }
                }
            }
            return Predicate.and(predicates.toArray(new Predicate[0]));
        };
        WeakJoin<BookTableEx, AuthorTableEx> join2 = (b, a) -> {
            List<Predicate> list = new ArrayList<>();
            Map<String, ImmutableProp> m = a.getImmutableType().getProps();
            for (ImmutableProp p : b.getImmutableType().getProps().values()) {
                if (p.isScalar(TargetLevel.ENTITY)) {
                    ImmutableProp authorProp = m.get(p.getName());
                    if (authorProp != null && p.getReturnClass() == authorProp.getReturnClass()) {
                        list.add(b.get(p).eq(a.get(authorProp)));
                    }
                }
            }
            return Predicate.and(list.toArray(new Predicate[0]));
        };
        WeakJoinLambda lambda1 = JWeakJoinLambdaFactory.get(join1);
        WeakJoinLambda lambda2 = JWeakJoinLambdaFactory.get(join2);
        Assertions.assertNotNull(lambda1);
        Assertions.assertNotNull(lambda2);
        Assertions.assertEquals(lambda1.hashCode(), lambda2.hashCode());
        Assertions.assertEquals(lambda1, lambda2);
    }
}
