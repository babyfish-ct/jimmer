package org.babyfish.jimmer.spring.parser;

import org.babyfish.jimmer.Static;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.java.model.Book;
import org.babyfish.jimmer.spring.java.model.dto.ComplexBook;
import org.babyfish.jimmer.spring.java.model.dto.SimpleBook;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.parser.Context;
import org.babyfish.jimmer.spring.repository.parser.QueryMethod;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;

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
                        "--->staticType=null, " +
                        "--->pageableParamIndex=1, " +
                        "--->sortParamIndex=-1, " +
                        "--->fetcherParamIndex=2, " +
                        "--->staticTypeParamIndex=-1" +
                        "}"
        );
    }

    @Test
    public void testKnownDTOMethod() throws NoSuchMethodException {
        Method method = Dao.class.getMethod("findSimpleBooksByName", String.class, Pageable.class);
        assertQueryMethod(
                QueryMethod.of(new Context(), ImmutableType.get(Book.class), method),
                "QueryMethod{" +
                        "--->javaMethod=public abstract org.springframework.data.domain.Page " +
                        "--->org.babyfish.jimmer.spring.parser.QueryMethodParserTest$Dao.findSimpleBooksByName(" +
                        "--->--->java.lang.String," +
                        "--->--->org.springframework.data.domain.Pageable" +
                        "--->), " +
                        "--->query=Query{" +
                        "--->--->action=FIND, " +
                        "--->--->limit=2147483647, " +
                        "--->--->distinct=false, " +
                        "--->--->selectedPath=null, " +
                        "--->--->predicate=ResolvedPredicate{" +
                        "--->--->--->path=name, " +
                        "--->--->--->op=EQ, " +
                        "--->--->--->insensitive=false, " +
                        "--->--->--->likeMode=EXACT, " +
                        "--->--->--->paramIndex=0, " +
                        "--->--->--->logicParamIndex=0, " +
                        "--->--->--->paramIndex2=-1, " +
                        "--->--->--->logicParamIndex2=-1" +
                        "--->--->}, " +
                        "--->--->orders=[]" +
                        "--->}, " +
                        "--->staticType=class org.babyfish.jimmer.spring.java.model.dto.SimpleBook, " +
                        "--->pageableParamIndex=1, " +
                        "--->sortParamIndex=-1, " +
                        "--->fetcherParamIndex=-1, " +
                        "--->staticTypeParamIndex=-1" +
                        "}"
        );

        method = Dao.class.getMethod("findComplexBooksByName", String.class, Pageable.class);
        assertQueryMethod(
                QueryMethod.of(new Context(), ImmutableType.get(Book.class), method),
                "QueryMethod{" +
                        "--->javaMethod=public abstract org.springframework.data.domain.Page " +
                        "--->org.babyfish.jimmer.spring.parser.QueryMethodParserTest$Dao.findComplexBooksByName(" +
                        "--->--->java.lang.String," +
                        "--->--->org.springframework.data.domain.Pageable" +
                        "--->), " +
                        "--->query=Query{" +
                        "--->--->action=FIND, " +
                        "--->--->limit=2147483647, " +
                        "--->--->distinct=false, " +
                        "--->--->selectedPath=null, " +
                        "--->--->predicate=ResolvedPredicate{" +
                        "--->--->--->path=name, " +
                        "--->--->--->op=EQ, " +
                        "--->--->--->insensitive=false, " +
                        "--->--->--->likeMode=EXACT, " +
                        "--->--->--->paramIndex=0, " +
                        "--->--->--->logicParamIndex=0, " +
                        "--->--->--->paramIndex2=-1, " +
                        "--->--->--->logicParamIndex2=-1" +
                        "--->--->}, " +
                        "--->--->orders=[]" +
                        "--->}, " +
                        "--->staticType=class org.babyfish.jimmer.spring.java.model.dto.ComplexBook, " +
                        "--->pageableParamIndex=1, " +
                        "--->sortParamIndex=-1, " +
                        "--->fetcherParamIndex=-1, " +
                        "--->staticTypeParamIndex=-1" +
                        "}"
        );
    }

    @Test
    public void testUnknownDTOMethod() throws NoSuchMethodException {
        Method method = Dao.class.getMethod("findStaticBookByName", String.class, Pageable.class, Class.class);
        assertQueryMethod(
                QueryMethod.of(new Context(), ImmutableType.get(Book.class), method),
                "QueryMethod{" +
                        "--->javaMethod=public abstract org.springframework.data.domain.Page " +
                        "--->org.babyfish.jimmer.spring.parser.QueryMethodParserTest$Dao.findStaticBookByName(" +
                        "--->--->java.lang.String," +
                        "--->--->org.springframework.data.domain.Pageable," +
                        "--->--->java.lang.Class" +
                        "--->), " +
                        "--->query=Query{" +
                        "--->--->action=FIND, " +
                        "--->--->limit=2147483647, " +
                        "--->--->distinct=false, " +
                        "--->--->selectedPath=null, " +
                        "--->--->predicate=ResolvedPredicate{" +
                        "--->--->--->path=name, " +
                        "--->--->--->op=EQ, " +
                        "--->--->--->insensitive=false, " +
                        "--->--->--->likeMode=EXACT, " +
                        "--->--->--->paramIndex=0, " +
                        "--->--->--->logicParamIndex=0, " +
                        "--->--->--->paramIndex2=-1, " +
                        "--->--->--->logicParamIndex2=-1" +
                        "--->--->}, " +
                        "--->--->orders=[]" +
                        "--->}, " +
                        "--->staticType=null, " +
                        "--->pageableParamIndex=1, " +
                        "--->sortParamIndex=-1, " +
                        "--->fetcherParamIndex=-1, " +
                        "--->staticTypeParamIndex=2" +
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

        // Known static DTO
        Page<SimpleBook> findSimpleBooksByName(
                String name,
                Pageable pageable
        );

        Page<ComplexBook> findComplexBooksByName(
                String name,
                Pageable pageable
        );

        // Unknown static DTO
        <S extends Static<Book>> Page<S> findStaticBookByName(
                String name,
                Pageable pageable,
                Class<S> staticType
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
