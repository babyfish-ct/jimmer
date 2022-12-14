package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.client.generator.ts.Context;
import org.babyfish.jimmer.client.generator.ts.File;
import org.babyfish.jimmer.client.generator.ts.ServiceWriter;
import org.babyfish.jimmer.client.generator.ts.TypeDefinitionWriter;
import org.babyfish.jimmer.client.model.Book;
import org.babyfish.jimmer.client.model.BookInput;
import org.babyfish.jimmer.client.model.Gender;
import org.babyfish.jimmer.client.model.Page;
import org.babyfish.jimmer.client.service.*;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Parameter;

public class MetadataTest {

    private final Metadata metadata = Metadata.newBuilder()
            .addServiceType(BookService.class)
            .setOperationParser(
                    javaMethod -> {
                        GetMapping getMapping = javaMethod.getAnnotation(GetMapping.class);
                        if (getMapping != null) {
                            return new Tuple2<>(getMapping.value(), Operation.HttpMethod.GET);
                        }
                        PutMapping putMapping = javaMethod.getAnnotation(PutMapping.class);
                        if (putMapping != null) {
                            return new Tuple2<>(putMapping.value(), Operation.HttpMethod.PUT);
                        }
                        return null;
                    }
            )
            .setParameterParser(
                    new Metadata.ParameterParser() {

                        @Nullable
                        @Override
                        public String requestParamName(Parameter javaParameter) {
                            RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
                            return requestParam != null ? requestParam.name() : null;
                        }

                        @Nullable
                        @Override
                        public String pathVariableName(Parameter javaParameter) {
                            PathVariable pathVariable = javaParameter.getAnnotation(PathVariable.class);
                            return pathVariable != null ? pathVariable.value() : null;
                        }

                        @Override
                        public boolean isRequestBody(Parameter javaParameter) {
                            return javaParameter.isAnnotationPresent(RequestBody.class);
                        }
                    }
            )
            .build();

    @Test
    public void test() throws NoSuchMethodException {
        Type pageType = metadata
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

    @Test
    public void testServiceWriter() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = new Context(metadata, out, "    ");
        Service service = metadata.getServices().get(BookService.class);
        new ServiceWriter(ctx, service).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testFetchedBook() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = new Context(metadata, out, "    ");
        ImmutableObjectType bookType = metadata.getFetchedImmutableObjectTypes().get(BookService.BOOK_FETCHER);
        new TypeDefinitionWriter(ctx, bookType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testFetchedAuthor() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = new Context(metadata, out, "    ");
        ImmutableObjectType authorType = metadata.getFetchedImmutableObjectTypes().get(BookService.AUTHOR_FETCHER);
        new TypeDefinitionWriter(ctx, authorType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testRawBook() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = new Context(metadata, out, "    ");
        ImmutableObjectType bookType = metadata.getRawImmutableObjectTypes().get(ImmutableType.get(Book.class));
        new TypeDefinitionWriter(ctx, bookType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testBookInput() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = new Context(metadata, out, "    ");
        StaticObjectType bookInputType = metadata.getStaticTypes().get(new StaticObjectType.Key(BookInput.class, null));
        new TypeDefinitionWriter(ctx, bookInputType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testPage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = new Context(metadata, out, "    ");
        StaticObjectType pageType = metadata.getGenericTypes().get(Page.class);
        new TypeDefinitionWriter(ctx, pageType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testTuple2() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = new Context(metadata, out, "    ");
        StaticObjectType tupleType = metadata.getGenericTypes().get(Tuple2.class);
        new TypeDefinitionWriter(ctx, tupleType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testGender() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = new Context(metadata, out, "    ");
        EnumType genderType = metadata.getEnumTypes().get(Gender.class);
        new TypeDefinitionWriter(ctx, genderType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    private static void assertType(String expect, Type type) {
        Assertions.assertEquals(
                expect.replace("--->", ""),
                type.toString()
        );
    }
}
