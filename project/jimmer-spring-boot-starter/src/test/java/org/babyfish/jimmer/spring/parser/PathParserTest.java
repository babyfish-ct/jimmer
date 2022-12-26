package org.babyfish.jimmer.spring.parser;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.java.model.Book;
import org.babyfish.jimmer.spring.java.model.embedded.Transform;
import org.babyfish.jimmer.spring.repository.parser.Context;
import org.babyfish.jimmer.spring.repository.parser.Path;
import org.babyfish.jimmer.spring.repository.parser.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathParserTest {

    @Test
    public void testBookName() {
        Path path = Path.of(
                new Context(),
                false,
                new Source("Name"),
                ImmutableType.get(Book.class)
        );
        Assertions.assertEquals(
                "name",
                path.toString()
        );
    }

    @Test
    public void testTooLongBookName() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
                Path.of(
                        new Context(),
                        false,
                        new Source("Name2"),
                        ImmutableType.get(Book.class)
                )
        );
        Assertions.assertEquals(
                "Cannot resolve the property name \"[Name2]\" by " +
                        "\"org.babyfish.jimmer.spring.java.model.Book\"",
                ex.getMessage()
        );
    }

    @Test
    public void testTooShortBookName() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
                Path.of(
                        new Context(),
                        false,
                        new Source("Nam"),
                        ImmutableType.get(Book.class)
                )
        );
        Assertions.assertEquals(
                "Cannot resolve the property name \"[Nam]\" by " +
                        "\"org.babyfish.jimmer.spring.java.model.Book\"",
                ex.getMessage()
        );
    }

    @Test
    public void testPath() {
        Path path = Path.of(
                new Context(),
                false,
                new Source("StoreName"),
                ImmutableType.get(Book.class)
        );
        Assertions.assertEquals(
                "store.name",
                path.toString()
        );
    }

    @Test
    public void testTooLongPath() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
                Path.of(
                        new Context(),
                        false,
                        new Source("StoreName2"),
                        ImmutableType.get(Book.class)
                )
        );
        Assertions.assertEquals(
                "Cannot resolve the property name \"Store[Name2]\" by " +
                        "\"org.babyfish.jimmer.spring.java.model.BookStore\"",
                ex.getMessage()
        );
    }

    @Test
    public void testTooShortPath() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
                Path.of(
                        new Context(),
                        false,
                        new Source("StoreNam"),
                        ImmutableType.get(Book.class)
                )
        );
        Assertions.assertEquals(
                "Cannot resolve the property name \"[StoreNam]\" by " +
                        "\"org.babyfish.jimmer.spring.java.model.Book\"",
                ex.getMessage()
        );
    }

    @Test
    public void testEmbeddedPath() {
        Path path = Path.of(
                new Context(),
                false,
                new Source("TargetRightBottomY"),
                ImmutableType.get(Transform.class)
        );
        Assertions.assertEquals(
                "target.rightBottom.y",
                path.toString()
        );
    }

    @Test
    public void testTooLongEmbeddedPath() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
                Path.of(
                        new Context(),
                        false,
                        new Source("TargetRightBottomY2"),
                        ImmutableType.get(Transform.class)
                )
        );
        Assertions.assertEquals(
                "Cannot resolve the property name \"TargetRightBottom[Y2]\" by " +
                        "\"org.babyfish.jimmer.spring.java.model.embedded.Point\"",
                ex.getMessage()
        );
    }

    @Test
    public void testBadEmbeddedPath() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
                Path.of(
                        new Context(),
                        false,
                        new Source("TargetRightBottomZ"),
                        ImmutableType.get(Transform.class)
                )
        );
        Assertions.assertEquals(
                "Cannot resolve the property name \"[TargetRightBottomZ]\" by " +
                        "\"org.babyfish.jimmer.spring.java.model.embedded.Transform\"",
                ex.getMessage()
        );
    }
}
