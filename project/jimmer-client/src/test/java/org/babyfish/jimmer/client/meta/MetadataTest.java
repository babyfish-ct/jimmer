package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.client.java.service.BookService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetadataTest {

    @Test
    public void test() {
        Type pageType = Constants.JAVA_METADATA
                .getServices()
                .get(BookService.class)
                .getOperations()
                .stream()
                .filter(it -> it.getName().equals("findTuples"))
                .findFirst()
                .get()
                .getType();
        Assertions.assertTrue(pageType instanceof StaticObjectType);

        Type entitiesType = ((StaticObjectType)pageType).getProperties().get("entities").getType();
        Assertions.assertTrue(entitiesType instanceof ArrayType);

        Type tupleType = ((ArrayType)entitiesType).getElementType();
        Assertions.assertTrue(tupleType instanceof StaticObjectType);

        Type bookType = ((StaticObjectType)tupleType).getProperties().get("_1").getType();
        Assertions.assertTrue(bookType instanceof ImmutableObjectType);

        assertType(
                "{" +
                        "--->id: long, " +
                        "--->name: String, " +
                        "--->edition: int, " +
                        "--->price: BigDecimal, " +
                        "--->store: {" +
                        "--->--->id: long, " +
                        "--->--->name: String" +
                        "--->}?, " +
                        "--->authors: Array<{" +
                        "--->--->id: long, " +
                        "--->--->firstName: String, " +
                        "--->--->lastName: String" +
                        "--->}>" +
                        "}",
                bookType
        );

        Type authorType = ((StaticObjectType)tupleType).getProperties().get("_2").getType();
        Assertions.assertTrue(authorType instanceof ImmutableObjectType);
        assertType(
                "{" +
                        "--->id: long, " +
                        "--->firstName: String, " +
                        "--->lastName: String, " +
                        "--->gender: MALE | FEMALE, " +
                        "--->books: Array<{" +
                        "--->--->id: long, " +
                        "--->--->name: String, " +
                        "--->--->store: {" +
                        "--->--->--->id: long, " +
                        "--->--->--->name: String" +
                        "--->--->}?" +
                        "--->}>" +
                        "}",
                authorType
        );
    }

    private static void assertType(String expect, Type type) {
        Assertions.assertEquals(
                expect.replace("--->", ""),
                type.toString()
        );
    }
}
