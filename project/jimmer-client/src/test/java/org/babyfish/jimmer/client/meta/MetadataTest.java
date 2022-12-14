package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.client.generator.ts.Context;
import org.babyfish.jimmer.client.generator.ts.ServiceWriter;
import org.babyfish.jimmer.client.generator.ts.TypeDefinitionWriter;
import org.babyfish.jimmer.client.model.Book;
import org.babyfish.jimmer.client.model.BookInput;
import org.babyfish.jimmer.client.model.Gender;
import org.babyfish.jimmer.client.model.Page;
import org.babyfish.jimmer.client.service.*;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MetadataTest {

    @Test
    public void test() {
        Type pageType = Constants.METADATA
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
                        "--->--->firstName: String" +
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
