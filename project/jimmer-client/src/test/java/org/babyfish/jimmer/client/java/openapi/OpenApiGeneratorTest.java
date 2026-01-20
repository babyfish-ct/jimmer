package org.babyfish.jimmer.client.java.openapi;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.openapi.OpenApiGenerator;
import org.babyfish.jimmer.client.generator.openapi.OpenApiProperties;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.VirtualType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringWriter;
import java.util.Collections;

public class OpenApiGeneratorTest {

    @Test
    public void testBookService() {
        Metadata metadata = Metadata
                .newBuilder()
                .setOperationParser(new OperationParserImpl())
                .setParameterParser(new ParameterParserImpl())
                .setGroups(Collections.singleton("bookService"))
                .build();
        OpenApiGenerator generator = new OpenApiGenerator(
                metadata,
                OpenApiProperties.newBuilder()
                        .setInfo(
                                OpenApiProperties.newInfoBuilder()
                                        .setTitle("Book System")
                                        .setDescription("You can use this system the operate book data")
                                        .setVersion("2.0.0")
                                        .build()
                        )
                        .setSecurities(
                                Collections.singletonList(
                                        Collections.singletonMap("tenantHeader", Collections.emptyList())
                                )
                        )
                        .setServers(
                                Collections.singletonList(
                                        OpenApiProperties.newServerBuilder()
                                                .setUrl("http://localhost:8080")
                                                .build()
                                )
                        )
                        .setComponents(
                                OpenApiProperties.newComponentsBuilder()
                                        .setSecuritySchemes(
                                                Collections.singletonMap(
                                                        "tenantHeader",
                                                        OpenApiProperties.newSecuritySchemeBuilder()
                                                                .setType("apiKey")
                                                                .setName("tenant")
                                                                .setIn(OpenApiProperties.In.HEADER)
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                        .build()
        );
        StringWriter writer = new StringWriter();
        generator.generate(writer);
        Assertions.assertEquals(
                "openapi: 3.0.1\n" +
                        "info:\n" +
                        "  title: Book System\n" +
                        "  description: You can use this system the operate book data\n" +
                        "  version: 2.0.0\n" +
                        "security:\n" +
                        "  - tenantHeader: []\n" +
                        "servers:\n" +
                        "  - url: 'http://localhost:8080'\n" +
                        "tags:\n" +
                        "  - name: BookService\n" +
                        "    description: The book service\n" +
                        "paths:\n" +
                        "  /book:\n" +
                        "    post:\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: saveBook\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          application/json:\n" +
                        "            schema:\n" +
                        "              $ref: '#/components/schemas/BookInput'\n" +
                        "        required: true\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Dynamic_Book'\n" +
                        "        500:\n" +
                        "          description: ERROR\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                oneOf:\n" +
                        "                  - $ref: '#/components/schemas/SaveException_ReadonlyMiddleTable'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NullTarget'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_CannotDissociateTarget'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoIdGenerator'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalIdGenerator'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalGeneratedId'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalInterceptorBehavior'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoKeyProp'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoVersion'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_OptimisticLockError'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NeitherIdNorKey'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_ReversedRemoteAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_LongRemoteAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_FailedRemoteValidation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_UnstructuredAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_TargetIsNotTransferable'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IncompleteProperty'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NotUnique'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalTargetId'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_UnloadedFrozenBackReference'\n" +
                        "    put:\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: updateBook\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          application/json:\n" +
                        "            schema:\n" +
                        "              $ref: '#/components/schemas/BookInput'\n" +
                        "        required: true\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Dynamic_Book'\n" +
                        "        500:\n" +
                        "          description: ERROR\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                oneOf:\n" +
                        "                  - $ref: '#/components/schemas/SaveException_ReadonlyMiddleTable'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NullTarget'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_CannotDissociateTarget'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoIdGenerator'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalIdGenerator'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalGeneratedId'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalInterceptorBehavior'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoKeyProp'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoVersion'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_OptimisticLockError'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NeitherIdNorKey'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_ReversedRemoteAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_LongRemoteAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_FailedRemoteValidation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_UnstructuredAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_TargetIsNotTransferable'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IncompleteProperty'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NotUnique'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalTargetId'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_UnloadedFrozenBackReference'\n" +
                        "    patch:\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: patchBook\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          application/json:\n" +
                        "            schema:\n" +
                        "              $ref: '#/components/schemas/BookInput'\n" +
                        "        required: true\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Dynamic_Book'\n" +
                        "        500:\n" +
                        "          description: ERROR\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                oneOf:\n" +
                        "                  - $ref: '#/components/schemas/SaveException_ReadonlyMiddleTable'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NullTarget'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_CannotDissociateTarget'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoIdGenerator'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalIdGenerator'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalGeneratedId'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalInterceptorBehavior'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoKeyProp'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NoVersion'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_OptimisticLockError'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NeitherIdNorKey'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_ReversedRemoteAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_LongRemoteAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_FailedRemoteValidation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_UnstructuredAssociation'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_TargetIsNotTransferable'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IncompleteProperty'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_NotUnique'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_IllegalTargetId'\n" +
                        "                  - $ref: '#/components/schemas/SaveException_UnloadedFrozenBackReference'\n" +
                        "  /book/{id}:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: findBook\n" +
                        "      parameters:\n" +
                        "        - name: id\n" +
                        "          in: path\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int64\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: An optional complex book DTO\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Book_BookService_COMPLEX_FETCHER'\n" +
                        "    delete:\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: deleteBook\n" +
                        "      parameters:\n" +
                        "        - name: id\n" +
                        "          in: path\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int64\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: integer\n" +
                        "                format: int32\n" +
                        "  /books/complex:\n" +
                        "    get:\n" +
                        "      summary: Find Complex DTOs\n" +
                        "      description: '<p>The complex DTO only supports the scalar properties of book, and associations `store` and `authors`</p>'\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: findComplexBooks\n" +
                        "      parameters:\n" +
                        "        - name: namePattern\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          description: The book name\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: storeIds\n" +
                        "          in: query\n" +
                        "          schema:\n" +
                        "            type: array\n" +
                        "            items:\n" +
                        "              type: integer\n" +
                        "              format: int64\n" +
                        "        - name: storeName\n" +
                        "          in: query\n" +
                        "          description: The name of the associated book store\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: authorName\n" +
                        "          in: query\n" +
                        "          description: The names of the associated authors\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: minPrice\n" +
                        "          in: query\n" +
                        "          description: The min price of the book\n" +
                        "          schema:\n" +
                        "            type: number\n" +
                        "        - name: maxPrice\n" +
                        "          in: query\n" +
                        "          description: The max price of the book\n" +
                        "          schema:\n" +
                        "            type: number\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: A list of complex book DTOs\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: array\n" +
                        "                items:\n" +
                        "                  $ref: '#/components/schemas/Book_BookService_COMPLEX_FETCHER'\n" +
                        "  /books/complex2:\n" +
                        "    get:\n" +
                        "      summary: Find Complex DTOs\n" +
                        "      description: '<p>The complex DTO only supports the scalar properties of book, and associations `store` and `authors`</p>'\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: findComplexBooksByArguments\n" +
                        "      parameters:\n" +
                        "        - name: name\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          description: 'Override comment of `FindBookArguments.name`'\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: storeName\n" +
                        "          in: query\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: authorNames\n" +
                        "          in: query\n" +
                        "          schema:\n" +
                        "            type: array\n" +
                        "            items:\n" +
                        "              type: string\n" +
                        "        - name: minPrice\n" +
                        "          in: query\n" +
                        "          schema:\n" +
                        "            type: number\n" +
                        "        - name: maxPrice\n" +
                        "          in: query\n" +
                        "          schema:\n" +
                        "            type: number\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: A list of complex book DTOs\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: array\n" +
                        "                items:\n" +
                        "                  $ref: '#/components/schemas/Book_BookService_COMPLEX_FETCHER'\n" +
                        "  /books/simple:\n" +
                        "    get:\n" +
                        "      summary: Find Simple DTOs\n" +
                        "      description: '<p>The simple DTO only supports `id`, `name` and `storeId`</p>'\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: findSimpleBooks\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: A list of simple book DTOs\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: array\n" +
                        "                items:\n" +
                        "                  $ref: '#/components/schemas/Book_BookService_SIMPLE_FETCHER'\n" +
                        "  /tuples:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: findTuples\n" +
                        "      parameters:\n" +
                        "        - name: name\n" +
                        "          in: query\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: pageIndex\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "            default: 0\n" +
                        "        - name: pageSize\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "            default: 10\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Page_Tuple2_Book_BookService_COMPLEX_FETCHER_Author_BookService_AUTHOR_FETCHER'\n" +
                        "  /version:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - BookService\n" +
                        "      operationId: version\n" +
                        "      parameters:\n" +
                        "        - name: 'Access-Token'\n" +
                        "          in: header\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: resourcePath\n" +
                        "          in: header\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: integer\n" +
                        "                format: int32\n" +
                        "components:\n" +
                        "  schemas:\n" +
                        "    BookInput:\n" +
                        "      type: object\n" +
                        "      description: The book input defined by DTO language\n" +
                        "      properties:\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of this book,\n" +
                        "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        edition:\n" +
                        "          description: |+\n" +
                        "            The edition of this book,\n" +
                        "            <p>Together with `name`, this property forms the key of the book</p>\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        price:\n" +
                        "          description: The price of this book\n" +
                        "          type: number\n" +
                        "        storeId:\n" +
                        "          description: 'The many-to-one association from `Book` to `BookStore`'\n" +
                        "          nullable: true\n" +
                        "          type: string\n" +
                        "        authorIds:\n" +
                        "          description: 'The many-to-many association from `Book` to `Author`'\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            type: string\n" +
                        "    Dynamic_Book:\n" +
                        "      type: object\n" +
                        "      description: The book object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id is long, but the client type is string\n" +
                        "            because JS cannot retain large long values\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of this book,\n" +
                        "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        edition:\n" +
                        "          description: |+\n" +
                        "            The edition of this book,\n" +
                        "            <p>Together with `name`, this property forms the key of the book</p>\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        price:\n" +
                        "          description: The price of this book\n" +
                        "          type: number\n" +
                        "        store:\n" +
                        "          description: 'The many-to-one association from `Book` to `BookStore`'\n" +
                        "          nullable: true\n" +
                        "          $ref: '#/components/schemas/Dynamic_BookStore'\n" +
                        "        authors:\n" +
                        "          description: 'The many-to-many association from `Book` to `Author`'\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/Dynamic_Author'\n" +
                        "        storeId:\n" +
                        "          description: 'The id view of `Book.store`'\n" +
                        "          type: string\n" +
                        "        authorIds:\n" +
                        "          description: 'The id view of `Book.authors`'\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            type: string\n" +
                        "    SaveException_ReadonlyMiddleTable:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [READONLY_MIDDLE_TABLE]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_NullTarget:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [NULL_TARGET]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_CannotDissociateTarget:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [CANNOT_DISSOCIATE_TARGETS]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_NoIdGenerator:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [NO_ID_GENERATOR]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_IllegalIdGenerator:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [ILLEGAL_ID_GENERATOR]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_IllegalGeneratedId:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [ILLEGAL_GENERATED_ID]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_IllegalInterceptorBehavior:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [ILLEGAL_INTERCEPTOR_BEHAVIOR]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_NoKeyProp:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [NO_KEY_PROP]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_NoVersion:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [NO_VERSION]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_OptimisticLockError:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [OPTIMISTIC_LOCK_ERROR]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_NeitherIdNorKey:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [NEITHER_ID_NOR_KEY]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_ReversedRemoteAssociation:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [REVERSED_REMOTE_ASSOCIATION]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_LongRemoteAssociation:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [LONG_REMOTE_ASSOCIATION]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_FailedRemoteValidation:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [FAILED_REMOTE_VALIDATION]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_UnstructuredAssociation:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [UNSTRUCTURED_ASSOCIATION]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_TargetIsNotTransferable:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [TARGET_IS_NOT_TRANSFERABLE]\n" +
                        "        saveErrorCode:\n" +
                        "          type: string\n" +
                        "          enum:\n" +
                        "            - READONLY_MIDDLE_TABLE\n" +
                        "            - NULL_TARGET\n" +
                        "            - CANNOT_DISSOCIATE_TARGETS\n" +
                        "            - NO_ID_GENERATOR\n" +
                        "            - ILLEGAL_ID_GENERATOR\n" +
                        "            - ILLEGAL_GENERATED_ID\n" +
                        "            - ILLEGAL_INTERCEPTOR_BEHAVIOR\n" +
                        "            - EMPTY_OBJECT\n" +
                        "            - NO_KEY_PROPS\n" +
                        "            - NO_KEY_PROP\n" +
                        "            - NO_NON_ID_PROPS\n" +
                        "            - NO_VERSION\n" +
                        "            - OPTIMISTIC_LOCK_ERROR\n" +
                        "            - ALREADY_EXISTS\n" +
                        "            - NEITHER_ID_NOR_KEY\n" +
                        "            - REVERSED_REMOTE_ASSOCIATION\n" +
                        "            - LONG_REMOTE_ASSOCIATION\n" +
                        "            - FAILED_REMOTE_VALIDATION\n" +
                        "            - UNSTRUCTURED_ASSOCIATION\n" +
                        "            - TARGET_IS_NOT_TRANSFERABLE\n" +
                        "            - INCOMPLETE_PROPERTY\n" +
                        "            - NOT_UNIQUE\n" +
                        "            - ILLEGAL_TARGET_ID\n" +
                        "            - UNLOADED_FROZEN_BACK_REFERENCE\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_IncompleteProperty:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [INCOMPLETE_PROPERTY]\n" +
                        "        saveErrorCode:\n" +
                        "          type: string\n" +
                        "          enum:\n" +
                        "            - READONLY_MIDDLE_TABLE\n" +
                        "            - NULL_TARGET\n" +
                        "            - CANNOT_DISSOCIATE_TARGETS\n" +
                        "            - NO_ID_GENERATOR\n" +
                        "            - ILLEGAL_ID_GENERATOR\n" +
                        "            - ILLEGAL_GENERATED_ID\n" +
                        "            - ILLEGAL_INTERCEPTOR_BEHAVIOR\n" +
                        "            - EMPTY_OBJECT\n" +
                        "            - NO_KEY_PROPS\n" +
                        "            - NO_KEY_PROP\n" +
                        "            - NO_NON_ID_PROPS\n" +
                        "            - NO_VERSION\n" +
                        "            - OPTIMISTIC_LOCK_ERROR\n" +
                        "            - ALREADY_EXISTS\n" +
                        "            - NEITHER_ID_NOR_KEY\n" +
                        "            - REVERSED_REMOTE_ASSOCIATION\n" +
                        "            - LONG_REMOTE_ASSOCIATION\n" +
                        "            - FAILED_REMOTE_VALIDATION\n" +
                        "            - UNSTRUCTURED_ASSOCIATION\n" +
                        "            - TARGET_IS_NOT_TRANSFERABLE\n" +
                        "            - INCOMPLETE_PROPERTY\n" +
                        "            - NOT_UNIQUE\n" +
                        "            - ILLEGAL_TARGET_ID\n" +
                        "            - UNLOADED_FROZEN_BACK_REFERENCE\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_NotUnique:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [NOT_UNIQUE]\n" +
                        "        saveErrorCode:\n" +
                        "          type: string\n" +
                        "          enum:\n" +
                        "            - READONLY_MIDDLE_TABLE\n" +
                        "            - NULL_TARGET\n" +
                        "            - CANNOT_DISSOCIATE_TARGETS\n" +
                        "            - NO_ID_GENERATOR\n" +
                        "            - ILLEGAL_ID_GENERATOR\n" +
                        "            - ILLEGAL_GENERATED_ID\n" +
                        "            - ILLEGAL_INTERCEPTOR_BEHAVIOR\n" +
                        "            - EMPTY_OBJECT\n" +
                        "            - NO_KEY_PROPS\n" +
                        "            - NO_KEY_PROP\n" +
                        "            - NO_NON_ID_PROPS\n" +
                        "            - NO_VERSION\n" +
                        "            - OPTIMISTIC_LOCK_ERROR\n" +
                        "            - ALREADY_EXISTS\n" +
                        "            - NEITHER_ID_NOR_KEY\n" +
                        "            - REVERSED_REMOTE_ASSOCIATION\n" +
                        "            - LONG_REMOTE_ASSOCIATION\n" +
                        "            - FAILED_REMOTE_VALIDATION\n" +
                        "            - UNSTRUCTURED_ASSOCIATION\n" +
                        "            - TARGET_IS_NOT_TRANSFERABLE\n" +
                        "            - INCOMPLETE_PROPERTY\n" +
                        "            - NOT_UNIQUE\n" +
                        "            - ILLEGAL_TARGET_ID\n" +
                        "            - UNLOADED_FROZEN_BACK_REFERENCE\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_IllegalTargetId:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [ILLEGAL_TARGET_ID]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    SaveException_UnloadedFrozenBackReference:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [SAVE_COMMAND]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [UNLOADED_FROZEN_BACK_REFERENCE]\n" +
                        "        exportedPath:\n" +
                        "          $ref: '#/components/schemas/ExportedSavePath'\n" +
                        "    Book_BookService_COMPLEX_FETCHER:\n" +
                        "      type: object\n" +
                        "      description: The book object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id is long, but the client type is string\n" +
                        "            because JS cannot retain large long values\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of this book,\n" +
                        "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        edition:\n" +
                        "          description: |+\n" +
                        "            The edition of this book,\n" +
                        "            <p>Together with `name`, this property forms the key of the book</p>\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        price:\n" +
                        "          description: The price of this book\n" +
                        "          type: number\n" +
                        "        store:\n" +
                        "          description: 'The many-to-one association from `Book` to `BookStore`'\n" +
                        "          nullable: true\n" +
                        "          $ref: '#/components/schemas/Book_BookService_COMPLEX_FETCHER_store'\n" +
                        "        authors:\n" +
                        "          description: 'The many-to-many association from `Book` to `Author`'\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/Book_BookService_COMPLEX_FETCHER_authors'\n" +
                        "    Book_BookService_SIMPLE_FETCHER:\n" +
                        "      type: object\n" +
                        "      description: The book object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id is long, but the client type is string\n" +
                        "            because JS cannot retain large long values\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of this book,\n" +
                        "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        storeId:\n" +
                        "          description: 'The id view of `Book.store`'\n" +
                        "          type: string\n" +
                        "    Page_Tuple2_Book_BookService_COMPLEX_FETCHER_Author_BookService_AUTHOR_FETCHER:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        totalRowCount:\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        totalPageCount:\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        entities:\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/Tuple2_Book_BookService_COMPLEX_FETCHER_Author_BookService_AUTHOR_FETCHER'\n" +
                        "    Dynamic_BookStore:\n" +
                        "      type: object\n" +
                        "      description: BookStore Entity\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          type: string\n" +
                        "        level:\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        books:\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/Dynamic_Book'\n" +
                        "        openTime:\n" +
                        "          type: string\n" +
                        "        closeTime:\n" +
                        "          type: string\n" +
                        "    Dynamic_Author:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: string\n" +
                        "        fullName:\n" +
                        "          $ref: '#/components/schemas/FullName'\n" +
                        "        gender:\n" +
                        "          type: string\n" +
                        "          enum:\n" +
                        "            - MALE\n" +
                        "            - FEMALE\n" +
                        "        books:\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/Dynamic_Book'\n" +
                        "    ExportedSavePath:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        rootTypeName:\n" +
                        "          type: string\n" +
                        "        nodes:\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/ExportedSavePath_Node'\n" +
                        "    Book_BookService_COMPLEX_FETCHER_store:\n" +
                        "      type: object\n" +
                        "      description: BookStore Entity\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          type: string\n" +
                        "        level:\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "    Book_BookService_COMPLEX_FETCHER_authors:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: string\n" +
                        "        fullName:\n" +
                        "          $ref: '#/components/schemas/FullName'\n" +
                        "    Tuple2_Book_BookService_COMPLEX_FETCHER_Author_BookService_AUTHOR_FETCHER:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        _1:\n" +
                        "          $ref: '#/components/schemas/Book_BookService_COMPLEX_FETCHER'\n" +
                        "        _2:\n" +
                        "          $ref: '#/components/schemas/Author_BookService_AUTHOR_FETCHER'\n" +
                        "    FullName:\n" +
                        "      type: object\n" +
                        "      description: Override full name of person\n" +
                        "      properties:\n" +
                        "        firstName:\n" +
                        "          description: Override first name of person\n" +
                        "          type: string\n" +
                        "        lastName:\n" +
                        "          description: Override last name of person\n" +
                        "          type: string\n" +
                        "    ExportedSavePath_Node:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        prop:\n" +
                        "          type: string\n" +
                        "        targetTypeName:\n" +
                        "          type: string\n" +
                        "    Author_BookService_AUTHOR_FETCHER:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: string\n" +
                        "        fullName:\n" +
                        "          $ref: '#/components/schemas/FullName'\n" +
                        "        gender:\n" +
                        "          type: string\n" +
                        "          enum:\n" +
                        "            - MALE\n" +
                        "            - FEMALE\n" +
                        "        books:\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/Author_BookService_AUTHOR_FETCHER_books'\n" +
                        "    Author_BookService_AUTHOR_FETCHER_books:\n" +
                        "      type: object\n" +
                        "      description: The book object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id is long, but the client type is string\n" +
                        "            because JS cannot retain large long values\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of this book,\n" +
                        "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        store:\n" +
                        "          description: 'The many-to-one association from `Book` to `BookStore`'\n" +
                        "          nullable: true\n" +
                        "          $ref: '#/components/schemas/Author_BookService_AUTHOR_FETCHER_books_store'\n" +
                        "    Author_BookService_AUTHOR_FETCHER_books_store:\n" +
                        "      type: object\n" +
                        "      description: BookStore Entity\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          type: string\n" +
                        "  securitySchemes:\n" +
                        "    tenantHeader:\n" +
                        "      type: apiKey\n" +
                        "      name: tenant\n" +
                        "      in: header\n",
                writer.toString()
        );
    }

    @Test
    public void testTreeService() {
        Metadata metadata = Metadata
                .newBuilder()
                .setOperationParser(new OperationParserImpl())
                .setParameterParser(new ParameterParserImpl())
                .setGroups(Collections.singleton("treeService"))
                .build();
        OpenApiGenerator generator = new OpenApiGenerator(metadata, null);
        StringWriter writer = new StringWriter();
        generator.generate(writer);
        Assertions.assertEquals(
                "openapi: 3.0.1\n" +
                        "info:\n" +
                        "  title: '<No title>'\n" +
                        "  description: '<No Description>'\n" +
                        "  version: 1.0.0\n" +
                        "tags:\n" +
                        "  - name: TreeService\n" +
                        "    description: |+\n" +
                        "      This is the service to test,\n" +
                        "      it can return two kinds of trees:\n" +
                        "      <ul>\n" +
                        "          <li>Recursive static object: Tree</li>\n" +
                        "          <li>Recursive fetched object: TreeNode</li>\n" +
                        "      </ul>\n" +
                        "paths:\n" +
                        "  /numberTree:\n" +
                        "    get:\n" +
                        "      summary: 'Create a static object tree, the value of each node must be integer.'\n" +
                        "      tags:\n" +
                        "        - TreeService\n" +
                        "      operationId: getNumberTree\n" +
                        "      parameters:\n" +
                        "        - name: depth\n" +
                        "          in: query\n" +
                        "          description: The depth of the tree\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "        - name: breadth\n" +
                        "          in: query\n" +
                        "          description: The child count of each tree node\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: The static object tree with integer values.\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Tree_int'\n" +
                        "  /numberTree2:\n" +
                        "    get:\n" +
                        "      summary: 'Create a static object tree, the value of each node must be integer.'\n" +
                        "      tags:\n" +
                        "        - TreeService\n" +
                        "      operationId: getNumberTree_2\n" +
                        "      parameters:\n" +
                        "        - name: depth\n" +
                        "          in: query\n" +
                        "          description: The depth of the tree\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "        - name: breadth\n" +
                        "          in: query\n" +
                        "          description: The child count of each tree node\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "        - name: maxBound\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          description: The max bound for the random integer value which is data of each node\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "            default: 10\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: The static object tree with integer values.\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Tree_int'\n" +
                        "  /rootNode:\n" +
                        "    get:\n" +
                        "      summary: Create query recursive tree roots by optional node name.\n" +
                        "      tags:\n" +
                        "        - TreeService\n" +
                        "      operationId: getRootNode\n" +
                        "      parameters:\n" +
                        "        - name: name\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          description: The optional string value to filter root nodes.\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "            default: X\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: The fetched object tree\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/TreeNode_TreeService_RECURSIVE_FETCHER'\n" +
                        "  /rootNode/simple:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - TreeService\n" +
                        "      operationId: getSimpleRootNodes\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: array\n" +
                        "                items:\n" +
                        "                  $ref: '#/components/schemas/SimpleTreeNodeView'\n" +
                        "  /stringTree:\n" +
                        "    get:\n" +
                        "      summary: 'Create a static object tree, the value of each node must be string.'\n" +
                        "      tags:\n" +
                        "        - TreeService\n" +
                        "      operationId: getStringTree\n" +
                        "      parameters:\n" +
                        "        - name: depth\n" +
                        "          in: query\n" +
                        "          description: The depth of the tree\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "        - name: breadth\n" +
                        "          in: query\n" +
                        "          description: The child count of each tree node\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: The static object tree with string values.\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/Tree_String'\n" +
                        "        500:\n" +
                        "          description: ERROR\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/DepthTooBigException'\n" +
                        "  /treeNode/{id}:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - TreeService\n" +
                        "      operationId: getTreeNodeById\n" +
                        "      parameters:\n" +
                        "        - name: id\n" +
                        "          in: path\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int64\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                $ref: '#/components/schemas/TreeNode_TreeService_TREE_NODE_DETAIL_FETCHER'\n" +
                        "components:\n" +
                        "  schemas:\n" +
                        "    Tree_int:\n" +
                        "      type: object\n" +
                        "      description: Static Object Tree\n" +
                        "      properties:\n" +
                        "        data:\n" +
                        "          description: The data of tree node\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        children:\n" +
                        "          description: Get child trees\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/Tree_int'\n" +
                        "    TreeNode_TreeService_RECURSIVE_FETCHER:\n" +
                        "      type: object\n" +
                        "      description: The tree node entity\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id of tree node.\n" +
                        "            <p>It doesn't make business sense, it's just auto-numbering.</p>\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of current tree node\n" +
                        "            <p>Together with `parent`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        childNodes:\n" +
                        "          description: |+\n" +
                        "            The one-to-many association from `TreeNode` to `TreeNode`,\n" +
                        "            it is opposite mirror of `TreeNode.parent`\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/TreeNode_TreeService_RECURSIVE_FETCHER'\n" +
                        "    SimpleTreeNodeView:\n" +
                        "      type: object\n" +
                        "      description: The tree node input defined by DTO language\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id of tree node.\n" +
                        "            <p>It doesn't make business sense, it's just auto-numbering.</p>\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of current tree node\n" +
                        "            <p>Together with `parent`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        parentId:\n" +
                        "          description: |+\n" +
                        "            The many-to-one association from `TreeNode` to `TreeNode`\n" +
                        "            <p>Together with `name`, this property forms the key of the book</p>\n" +
                        "          nullable: true\n" +
                        "          type: string\n" +
                        "        childNodeIds:\n" +
                        "          description: |+\n" +
                        "            The one-to-many association from `TreeNode` to `TreeNode`,\n" +
                        "            it is opposite mirror of `TreeNode.parent`\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            type: string\n" +
                        "    Tree_String:\n" +
                        "      type: object\n" +
                        "      description: Static Object Tree\n" +
                        "      properties:\n" +
                        "        data:\n" +
                        "          description: The data of tree node\n" +
                        "          type: string\n" +
                        "        children:\n" +
                        "          description: Get child trees\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/Tree_String'\n" +
                        "    DepthTooBigException:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        family:\n" +
                        "          type: string\n" +
                        "          enum: [DEFAULT]\n" +
                        "        code:\n" +
                        "          type: string\n" +
                        "          enum: [DEPTH_TOO_BIG]\n" +
                        "        maxDepth:\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        currentDepth:\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "    TreeNode_TreeService_TREE_NODE_DETAIL_FETCHER:\n" +
                        "      type: object\n" +
                        "      description: The tree node entity\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id of tree node.\n" +
                        "            <p>It doesn't make business sense, it's just auto-numbering.</p>\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of current tree node\n" +
                        "            <p>Together with `parent`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        parent:\n" +
                        "          description: |+\n" +
                        "            The many-to-one association from `TreeNode` to `TreeNode`\n" +
                        "            <p>Together with `name`, this property forms the key of the book</p>\n" +
                        "          nullable: true\n" +
                        "          $ref: '#/components/schemas/TreeNode_TreeService_TREE_NODE_DETAIL_FETCHER_parent'\n" +
                        "        childNodes:\n" +
                        "          description: |+\n" +
                        "            The one-to-many association from `TreeNode` to `TreeNode`,\n" +
                        "            it is opposite mirror of `TreeNode.parent`\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/TreeNode_TreeService_TREE_NODE_DETAIL_FETCHER_childNodes'\n" +
                        "    TreeNode_TreeService_TREE_NODE_DETAIL_FETCHER_parent:\n" +
                        "      type: object\n" +
                        "      description: The tree node entity\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id of tree node.\n" +
                        "            <p>It doesn't make business sense, it's just auto-numbering.</p>\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of current tree node\n" +
                        "            <p>Together with `parent`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        parent:\n" +
                        "          description: |+\n" +
                        "            The many-to-one association from `TreeNode` to `TreeNode`\n" +
                        "            <p>Together with `name`, this property forms the key of the book</p>\n" +
                        "          $ref: '#/components/schemas/TreeNode_TreeService_TREE_NODE_DETAIL_FETCHER_parent'\n" +
                        "    TreeNode_TreeService_TREE_NODE_DETAIL_FETCHER_childNodes:\n" +
                        "      type: object\n" +
                        "      description: The tree node entity\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id of tree node.\n" +
                        "            <p>It doesn't make business sense, it's just auto-numbering.</p>\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of current tree node\n" +
                        "            <p>Together with `parent`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        childNodes:\n" +
                        "          description: |+\n" +
                        "            The one-to-many association from `TreeNode` to `TreeNode`,\n" +
                        "            it is opposite mirror of `TreeNode.parent`\n" +
                        "          type: array\n" +
                        "          items:\n" +
                        "            $ref: '#/components/schemas/TreeNode_TreeService_TREE_NODE_DETAIL_FETCHER_childNodes'\n",
                writer.toString()
        );
    }

    @Test
    public void testCustomerService() {
        Metadata metadata = Metadata
                .newBuilder()
                .setOperationParser(new OperationParserImpl())
                .setParameterParser(new ParameterParserImpl())
                .setGroups(Collections.singleton("customerService"))
                .setVirtualTypeMap(Collections.singletonMap(TypeName.of(MultipartFile.class), VirtualType.FILE))
                .build();
        OpenApiGenerator generator = new OpenApiGenerator(metadata, null);
        StringWriter writer = new StringWriter();
        generator.generate(writer);
        Assertions.assertEquals(
                "openapi: 3.0.1\n" +
                        "info:\n" +
                        "  title: '<No title>'\n" +
                        "  description: '<No Description>'\n" +
                        "  version: 1.0.0\n" +
                        "paths:\n" +
                        "  /customer:\n" +
                        "    post:\n" +
                        "      tags:\n" +
                        "        - CustomerService\n" +
                        "      operationId: saveCustomer\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          multipart/form-data:\n" +
                        "            schema:\n" +
                        "              type: object\n" +
                        "              properties:\n" +
                        "                input:\n" +
                        "                  $ref: '#/components/schemas/CustomerInput'\n" +
                        "                files:\n" +
                        "                  type: array\n" +
                        "                  items:\n" +
                        "                    type: string\n" +
                        "                    format: binary\n" +
                        "            encoding:\n" +
                        "              input:\n" +
                        "                contentType: application/json\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: object\n" +
                        "                additionalProperties:\n" +
                        "                  type: integer\n" +
                        "                  format: int32\n" +
                        "  /customer/image:\n" +
                        "    patch:\n" +
                        "      tags:\n" +
                        "        - CustomerService\n" +
                        "      operationId: addImage\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          multipart/form-data:\n" +
                        "            schema:\n" +
                        "              type: object\n" +
                        "              properties:\n" +
                        "                file:\n" +
                        "                  type: string\n" +
                        "                  format: binary\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "  /customer/images/{index}:\n" +
                        "    patch:\n" +
                        "      tags:\n" +
                        "        - CustomerService\n" +
                        "      operationId: changeImage\n" +
                        "      parameters:\n" +
                        "        - name: index\n" +
                        "          in: path\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: integer\n" +
                        "            format: int32\n" +
                        "      requestBody:\n" +
                        "        content:\n" +
                        "          multipart/form-data:\n" +
                        "            schema:\n" +
                        "              type: object\n" +
                        "              properties:\n" +
                        "                file:\n" +
                        "                  type: string\n" +
                        "                  format: binary\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "  /customers:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - CustomerService\n" +
                        "      operationId: findCustomers\n" +
                        "      parameters:\n" +
                        "        - name: name\n" +
                        "          in: query\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: object\n" +
                        "                additionalProperties:\n" +
                        "                  $ref: '#/components/schemas/Customer_CustomerService_DEFAULT_CUSTOMER'\n" +
                        "  /login:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - CustomerService\n" +
                        "      operationId: login\n" +
                        "      parameters:\n" +
                        "        - name: userName\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: password\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "components:\n" +
                        "  schemas:\n" +
                        "    CustomerInput:\n" +
                        "      type: object\n" +
                        "      description: The book object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          description: |+\n" +
                        "            The id is long, but the client type is string\n" +
                        "            because JS cannot retain large long values\n" +
                        "          type: string\n" +
                        "        name:\n" +
                        "          description: |+\n" +
                        "            The name of this book,\n" +
                        "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                        "          type: string\n" +
                        "        edition:\n" +
                        "          description: |+\n" +
                        "            The edition of this book,\n" +
                        "            <p>Together with `name`, this property forms the key of the book</p>\n" +
                        "          type: integer\n" +
                        "          format: int32\n" +
                        "        price:\n" +
                        "          description: The price of this book\n" +
                        "          type: number\n" +
                        "    Customer_CustomerService_DEFAULT_CUSTOMER:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        id:\n" +
                        "          type: integer\n" +
                        "          format: int64\n" +
                        "        name:\n" +
                        "          type: string\n" +
                        "        contact:\n" +
                        "          nullable: true\n" +
                        "          $ref: '#/components/schemas/Contact'\n" +
                        "    Contact:\n" +
                        "      type: object\n" +
                        "      properties:\n" +
                        "        email:\n" +
                        "          type: string\n" +
                        "        phone:\n" +
                        "          nullable: true\n" +
                        "          type: string\n",
                writer.toString()
        );
    }

    @Test
    public void testMapService() {
        Metadata metadata = Metadata
                .newBuilder()
                .setOperationParser(new OperationParserImpl())
                .setParameterParser(new ParameterParserImpl())
                .setGroups(Collections.singleton("mapService"))
                .build();
        OpenApiGenerator generator = new OpenApiGenerator(metadata, null);
        StringWriter writer = new StringWriter();
        generator.generate(writer);
        Assertions.assertEquals(
                "openapi: 3.0.1\n" +
                        "info:\n" +
                        "  title: '<No title>'\n" +
                        "  description: '<No Description>'\n" +
                        "  version: 1.0.0\n" +
                        "paths:\n" +
                        "  /findBetween/{min}/and/{max}:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - MapService\n" +
                        "      operationId: findMapBetween\n" +
                        "      parameters:\n" +
                        "        - name: min\n" +
                        "          in: path\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "        - name: max\n" +
                        "          in: path\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: string\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: object\n" +
                        "                additionalProperties:\n" +
                        "                  type: object\n" +
                        "  /findByKeys:\n" +
                        "    get:\n" +
                        "      tags:\n" +
                        "        - MapService\n" +
                        "      operationId: findByKeys\n" +
                        "      parameters:\n" +
                        "        - name: keys\n" +
                        "          in: query\n" +
                        "          required: true\n" +
                        "          schema:\n" +
                        "            type: array\n" +
                        "            items:\n" +
                        "              type: string\n" +
                        "      responses:\n" +
                        "        200:\n" +
                        "          description: OK\n" +
                        "          content:\n" +
                        "            application/json:\n" +
                        "              schema:\n" +
                        "                type: object\n" +
                        "                additionalProperties:\n" +
                        "                  type: object\n",
                writer.toString()
        );
    }
}
