package org.babyfish.jimmer.spring.parser;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.java.model.Book;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.parser.Context;
import org.babyfish.jimmer.spring.repository.parser.QueryMethod;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class QueryMethodParserTest {

    @Test
    public void testEntityMethod() throws NoSuchMethodException {

        Method method = Dao.class.getMethod("findByNameOrderByName", String.class, Pageable.class, Fetcher.class);
        assertQueryMethod(
                QueryMethod.of(new Context(), ImmutableType.get(Book.class), method),
                "QueryMethod{" +
                        "--->javaMethod=public abstract org.springframework.data.domain.Page " +
                        "--->org.babyfish.jimmer.spring.parser.QueryMethodParserTest$Dao.findByNameOrderByName(" +
                        "--->--->java.lang.String," +
                        "--->--->org.springframework.data.domain.Pageable," +
                        "--->--->org.babyfish.jimmer.sql.fetcher.Fetcher" +
                        "--->), " +
                        "--->query=Query{" +
                        "--->--->action=FIND, " +
                        "--->--->limit=2147483647, " +
                        "--->--->distinct=false, " +
                        "--->--->selectedPath=null, " +
                        "--->--->predicate=ResolvedPredicate{" +
                        "--->--->--->path=name, op=EQ, insensitive=false, likeMode=EXACT, " +
                        "--->--->--->paramIndex=0, logicParamIndex=0, paramIndex2=-1, logicParamIndex2=-1" +
                        "--->--->}, " +
                        "--->--->orders=[" +
                        "--->--->--->Order{path=name, orderMode=ASC}" +
                        "--->--->]" +
                        "--->}, " +
                        "--->pageableParamIndex=1, " +
                        "--->sortParamIndex=-1, " +
                        "--->fetcherParamIndex=2" +
                        "}"
        );

        method = Dao.class.getMethod("findByNameAndEditionInOrderByNameAscEditionDesc", String.class, Collection.class);
        assertQueryMethod(
                QueryMethod.of(new Context(), ImmutableType.get(Book.class), method),
                "QueryMethod{" +
                        "--->javaMethod=" +
                        "--->--->public abstract java.util.List " +
                        "--->--->org.babyfish.jimmer.spring.parser.QueryMethodParserTest$Dao.findByNameAndEditionInOrderByNameAscEditionDesc(" +
                        "--->--->--->java.lang.String,java.util.Collection" +
                        "--->--->), " +
                        "--->query=Query{" +
                        "--->--->action=FIND, " +
                        "--->--->limit=2147483647, " +
                        "--->--->distinct=false, " +
                        "--->--->selectedPath=null, " +
                        "--->--->predicate=AndPredicate{" +
                        "--->--->--->predicates=[" +
                        "--->--->--->--->ResolvedPredicate{" +
                        "--->--->--->--->--->path=name, op=EQ, insensitive=false, likeMode=EXACT, " +
                        "--->--->--->--->--->paramIndex=0, logicParamIndex=0, paramIndex2=-1, logicParamIndex2=-1" +
                        "--->--->--->--->}, ResolvedPredicate{" +
                        "--->--->--->--->--->path=edition, op=IN, insensitive=false, likeMode=EXACT, " +
                        "--->--->--->--->--->paramIndex=1, logicParamIndex=1, paramIndex2=-1, logicParamIndex2=-1" +
                        "--->--->--->--->}" +
                        "--->--->--->]" +
                        "--->--->}, " +
                        "--->--->orders=[" +
                        "--->--->--->Order{path=name, orderMode=ASC}, Order{path=edition, orderMode=DESC}" +
                        "--->--->]" +
                        "--->}, " +
                        "--->pageableParamIndex=-1, " +
                        "--->sortParamIndex=-1, " +
                        "--->fetcherParamIndex=-1" +
                        "}"
        );
    }

    interface Dao extends JRepository<Book, Long> {

        // Dynamic entity
        Page<Book> findByNameOrderByName(
                String name,
                Pageable pageable,
                Fetcher<Book> fetcher
        );

        List<Book> findByNameAndEditionInOrderByNameAscEditionDesc(
                String name,
                Collection<Integer> editions // Test boxing for element type
        );
    }

    private static void assertQueryMethod(QueryMethod queryMethod, String expectedText) {
        Assertions.assertEquals(
                expectedText
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("--->", ""),
                queryMethod.toString()
        );
    }
}
