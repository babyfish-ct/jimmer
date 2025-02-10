package org.babyfish.jimmer.client.kotlin.openapi

import org.babyfish.jimmer.client.common.OperationParserImpl
import org.babyfish.jimmer.client.common.ParameterParserImpl
import org.babyfish.jimmer.client.generator.openapi.OpenApiGenerator
import org.babyfish.jimmer.client.generator.openapi.OpenApiProperties
import org.babyfish.jimmer.client.runtime.Metadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.util.*

class KOpenApiGeneratorTest {

    @Test
    fun testKBookService() {
        val metadata = Metadata
            .newBuilder()
            .setOperationParser(OperationParserImpl())
            .setParameterParser(ParameterParserImpl())
            .setGroups(setOf("kBookService"))
            .build()
        val generator = object: OpenApiGenerator(
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
                    listOf(
                        Collections.singletonMap("tenantHeader", emptyList())
                    )
                )
                .setServers(
                    listOf(
                        OpenApiProperties.newServerBuilder()
                            .setUrl("http://localhost:8080")
                            .build()
                    )
                )
                .setComponents(
                    OpenApiProperties.newComponentsBuilder()
                        .setSecuritySchemes(
                            mapOf(
                                "tenantHeader" to
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
        ) {
            override fun errorHttpStatus(): Int = 501
        }
        val writer = StringWriter()
        generator.generate(writer)
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
                    "  - name: KBookService\n" +
                    "    description: BookService interface\n" +
                    "paths:\n" +
                    "  /book:\n" +
                    "    post:\n" +
                    "      tags:\n" +
                    "        - KBookService\n" +
                    "      operationId: saveBook\n" +
                    "      requestBody:\n" +
                    "        content:\n" +
                    "          application/json:\n" +
                    "            schema:\n" +
                    "              \$ref: '#/components/schemas/KBookInput'\n" +
                    "        required: true\n" +
                    "      responses:\n" +
                    "        200:\n" +
                    "          description: OK\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                \$ref: '#/components/schemas/Dynamic_KBook'\n" +
                    "        501:\n" +
                    "          description: ERROR\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                oneOf:\n" +
                    "                  - \$ref: '#/components/schemas/KBusinessException_DataIsFrozen'\n" +
                    "                  - \$ref: '#/components/schemas/KBusinessException_ServiceIsSuspended'\n" +
                    "    put:\n" +
                    "      tags:\n" +
                    "        - KBookService\n" +
                    "      operationId: updateBook\n" +
                    "      requestBody:\n" +
                    "        content:\n" +
                    "          application/json:\n" +
                    "            schema:\n" +
                    "              \$ref: '#/components/schemas/KBookInput'\n" +
                    "        required: true\n" +
                    "      responses:\n" +
                    "        200:\n" +
                    "          description: OK\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                \$ref: '#/components/schemas/Dynamic_KBook'\n" +
                    "        501:\n" +
                    "          description: ERROR\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                oneOf:\n" +
                    "                  - \$ref: '#/components/schemas/KBusinessException_DataIsFrozen'\n" +
                    "                  - \$ref: '#/components/schemas/KBusinessException_ServiceIsSuspended'\n" +
                    "    patch:\n" +
                    "      tags:\n" +
                    "        - KBookService\n" +
                    "      operationId: patchBook\n" +
                    "      requestBody:\n" +
                    "        content:\n" +
                    "          application/json:\n" +
                    "            schema:\n" +
                    "              \$ref: '#/components/schemas/KBookInput'\n" +
                    "        required: true\n" +
                    "      responses:\n" +
                    "        200:\n" +
                    "          description: OK\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                \$ref: '#/components/schemas/Dynamic_KBook'\n" +
                    "        501:\n" +
                    "          description: ERROR\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                oneOf:\n" +
                    "                  - \$ref: '#/components/schemas/KBusinessException_DataIsFrozen'\n" +
                    "                  - \$ref: '#/components/schemas/KBusinessException_ServiceIsSuspended'\n" +
                    "  /book/fixed:\n" +
                    "    post:\n" +
                    "      tags:\n" +
                    "        - KBookService\n" +
                    "      operationId: saveBook_2\n" +
                    "      requestBody:\n" +
                    "        content:\n" +
                    "          application/json:\n" +
                    "            schema:\n" +
                    "              \$ref: '#/components/schemas/KFixedBookInput'\n" +
                    "        required: true\n" +
                    "      responses:\n" +
                    "        200:\n" +
                    "          description: OK\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                \$ref: '#/components/schemas/Dynamic_KBook'\n" +
                    "        501:\n" +
                    "          description: ERROR\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                oneOf:\n" +
                    "                  - \$ref: '#/components/schemas/KBusinessException_DataIsFrozen'\n" +
                    "                  - \$ref: '#/components/schemas/KBusinessException_ServiceIsSuspended'\n" +
                    "  /book/{id}:\n" +
                    "    delete:\n" +
                    "      tags:\n" +
                    "        - KBookService\n" +
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
                    "      tags:\n" +
                    "        - KBookService\n" +
                    "      operationId: findComplexBooks\n" +
                    "      parameters:\n" +
                    "        - name: name\n" +
                    "          in: query\n" +
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
                    "          schema:\n" +
                    "            type: string\n" +
                    "        - name: authorName\n" +
                    "          in: query\n" +
                    "          schema:\n" +
                    "            type: string\n" +
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
                    "          description: A list contains complex DTOs\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                type: array\n" +
                    "                items:\n" +
                    "                  \$ref: '#/components/schemas/KBook_KBookService_COMPLEX_FETCHER'\n" +
                    "  /books/simple:\n" +
                    "    get:\n" +
                    "      tags:\n" +
                    "        - KBookService\n" +
                    "      operationId: findSimpleBooks\n" +
                    "      responses:\n" +
                    "        200:\n" +
                    "          description: A list contains simple DTOs\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                type: array\n" +
                    "                items:\n" +
                    "                  \$ref: '#/components/schemas/KBook_KBookService_SIMPLE_FETCHER'\n" +
                    "  /tuples:\n" +
                    "    get:\n" +
                    "      tags:\n" +
                    "        - KBookService\n" +
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
                    "        - name: pageSize\n" +
                    "          in: query\n" +
                    "          required: true\n" +
                    "          schema:\n" +
                    "            type: integer\n" +
                    "            format: int32\n" +
                    "      responses:\n" +
                    "        200:\n" +
                    "          description: OK\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                \$ref: '#/components/schemas/KPage_Tuple2_KBook_KBookService_COMPLEX_FETCHER_KAuthor_KBookService_AUTHOR_FETCHER'\n" +
                    "components:\n" +
                    "  schemas:\n" +
                    "    KBookInput:\n" +
                    "      type: object\n" +
                    "      description: The book input defined by DTO language\n" +
                    "      properties:\n" +
                    "        name:\n" +
                    "          description: |+\n" +
                    "            The name of this book,\n" +
                    "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        edition:\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        price:\n" +
                    "          nullable: true\n" +
                    "          type: number\n" +
                    "        storeId:\n" +
                    "          description: 'The bookstore to which the current book belongs, null is allowed'\n" +
                    "          nullable: true\n" +
                    "          type: integer\n" +
                    "          format: int64\n" +
                    "        authorIds:\n" +
                    "          description: All authors involved in writing the work\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            type: string\n" +
                    "    Dynamic_KBook:\n" +
                    "      type: object\n" +
                    "      description: The book object\n" +
                    "      properties:\n" +
                    "        createdTime:\n" +
                    "          description: Created time\n" +
                    "          type: string\n" +
                    "        modifiedTime:\n" +
                    "          description: Modified time\n" +
                    "          type: string\n" +
                    "        id:\n" +
                    "          description: |+\n" +
                    "            The id is long, but the client type is string\n" +
                    "            because JS cannot retain large long values\n" +
                    "          type: string\n" +
                    "        name:\n" +
                    "          description: |+\n" +
                    "            The name of this book,\n" +
                    "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        edition:\n" +
                    "          description: |+\n" +
                    "            The edition of this book,\n" +
                    "             <p>Together with `name`, this property forms the key of the book</p>\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        price:\n" +
                    "          description: The price of this book\n" +
                    "          nullable: true\n" +
                    "          type: number\n" +
                    "        store:\n" +
                    "          description: 'The bookstore to which the current book belongs, null is allowed'\n" +
                    "          nullable: true\n" +
                    "          \$ref: '#/components/schemas/Dynamic_KBookStore'\n" +
                    "        authors:\n" +
                    "          description: All authors involved in writing the work\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/Dynamic_KAuthor'\n" +
                    "    KBusinessException_DataIsFrozen:\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        family:\n" +
                    "          type: string\n" +
                    "          enum: [KBUSINESS]\n" +
                    "        code:\n" +
                    "          type: string\n" +
                    "          enum: [DATA_IS_FROZEN]\n" +
                    "    KBusinessException_ServiceIsSuspended:\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        family:\n" +
                    "          type: string\n" +
                    "          enum: [KBUSINESS]\n" +
                    "        code:\n" +
                    "          type: string\n" +
                    "          enum: [SERVICE_IS_SUSPENDED]\n" +
                    "        planedResumeTime:\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "    KFixedBookInput:\n" +
                    "      type: object\n" +
                    "      description: The book input defined by DTO language\n" +
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
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        edition:\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        price:\n" +
                    "          nullable: true\n" +
                    "          type: number\n" +
                    "        storeId:\n" +
                    "          description: 'The bookstore to which the current book belongs, null is allowed'\n" +
                    "          nullable: true\n" +
                    "          type: integer\n" +
                    "          format: int64\n" +
                    "        authorIds:\n" +
                    "          description: All authors involved in writing the work\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            type: string\n" +
                    "    KBook_KBookService_COMPLEX_FETCHER:\n" +
                    "      type: object\n" +
                    "      description: The book object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          description: |+\n" +
                    "            The id is long, but the client type is string\n" +
                    "            because JS cannot retain large long values\n" +
                    "          type: string\n" +
                    "        createdTime:\n" +
                    "          description: Created time\n" +
                    "          type: string\n" +
                    "        modifiedTime:\n" +
                    "          description: Modified time\n" +
                    "          type: string\n" +
                    "        name:\n" +
                    "          description: |+\n" +
                    "            The name of this book,\n" +
                    "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        edition:\n" +
                    "          description: |+\n" +
                    "            The edition of this book,\n" +
                    "             <p>Together with `name`, this property forms the key of the book</p>\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        price:\n" +
                    "          description: The price of this book\n" +
                    "          nullable: true\n" +
                    "          type: number\n" +
                    "        store:\n" +
                    "          description: 'The bookstore to which the current book belongs, null is allowed'\n" +
                    "          nullable: true\n" +
                    "          \$ref: '#/components/schemas/KBook_KBookService_COMPLEX_FETCHER_store'\n" +
                    "        authors:\n" +
                    "          description: All authors involved in writing the work\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/KBook_KBookService_COMPLEX_FETCHER_authors'\n" +
                    "    KBook_KBookService_SIMPLE_FETCHER:\n" +
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
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "    KPage_Tuple2_KBook_KBookService_COMPLEX_FETCHER_KAuthor_KBookService_AUTHOR_FETCHER:\n" +
                    "      type: object\n" +
                    "      description: The page object\n" +
                    "      properties:\n" +
                    "        totalRowCount:\n" +
                    "          description: Total row count before paging\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        totalPageCount:\n" +
                    "          description: Total page count before paging\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        entities:\n" +
                    "          description: The entities in the current page\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/Tuple2_KBook_KBookService_COMPLEX_FETCHER_KAuthor_KBookService_AUTHOR_FETCHER'\n" +
                    "    Dynamic_KBookStore:\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          type: integer\n" +
                    "          format: int64\n" +
                    "        name:\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        coordinate:\n" +
                    "          \$ref: '#/components/schemas/KCoordinate'\n" +
                    "        level:\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        books:\n" +
                    "          description: All books available in this bookstore\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/Dynamic_KBook'\n" +
                    "        openTime:\n" +
                    "          type: string\n" +
                    "        closeTime:\n" +
                    "          type: string\n" +
                    "    Dynamic_KAuthor:\n" +
                    "      type: object\n" +
                    "      description: The author object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          description: |+\n" +
                    "            The id is long, but the client type is string\n" +
                    "            because JS cannot retain large long values\n" +
                    "          type: string\n" +
                    "        firstName:\n" +
                    "          description: |+\n" +
                    "            The first name of this author\n" +
                    "             <p>Together with `lastName`, this property forms the key of the book</p>\n" +
                    "          type: string\n" +
                    "        lastName:\n" +
                    "          description: |+\n" +
                    "            The last name of this author\n" +
                    "             <p>Together with `firstName`, this property forms the key of the book</p>\n" +
                    "          type: string\n" +
                    "        gender:\n" +
                    "          type: string\n" +
                    "          enum:\n" +
                    "            - MALE\n" +
                    "            - FEMALE\n" +
                    "        books:\n" +
                    "          description: All the books I have written\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/Dynamic_KBook'\n" +
                    "    KBook_KBookService_COMPLEX_FETCHER_store:\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          type: integer\n" +
                    "          format: int64\n" +
                    "        name:\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        coordinate:\n" +
                    "          \$ref: '#/components/schemas/KCoordinate'\n" +
                    "        level:\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        openTime:\n" +
                    "          type: string\n" +
                    "        closeTime:\n" +
                    "          type: string\n" +
                    "    KBook_KBookService_COMPLEX_FETCHER_authors:\n" +
                    "      type: object\n" +
                    "      description: The author object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          description: |+\n" +
                    "            The id is long, but the client type is string\n" +
                    "            because JS cannot retain large long values\n" +
                    "          type: string\n" +
                    "        firstName:\n" +
                    "          description: |+\n" +
                    "            The first name of this author\n" +
                    "             <p>Together with `lastName`, this property forms the key of the book</p>\n" +
                    "          type: string\n" +
                    "        lastName:\n" +
                    "          description: |+\n" +
                    "            The last name of this author\n" +
                    "             <p>Together with `firstName`, this property forms the key of the book</p>\n" +
                    "          type: string\n" +
                    "        gender:\n" +
                    "          type: string\n" +
                    "          enum:\n" +
                    "            - MALE\n" +
                    "            - FEMALE\n" +
                    "    Tuple2_KBook_KBookService_COMPLEX_FETCHER_KAuthor_KBookService_AUTHOR_FETCHER:\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        _1:\n" +
                    "          \$ref: '#/components/schemas/KBook_KBookService_COMPLEX_FETCHER'\n" +
                    "        _2:\n" +
                    "          \$ref: '#/components/schemas/KAuthor_KBookService_AUTHOR_FETCHER'\n" +
                    "    KCoordinate:\n" +
                    "      type: object\n" +
                    "      description: A location marked by longitude and latitude\n" +
                    "      properties:\n" +
                    "        longitude:\n" +
                    "          description: 'The latitude, from -180 to +180'\n" +
                    "          type: number\n" +
                    "        latitude:\n" +
                    "          description: 'The latitude, from -90 to +90'\n" +
                    "          type: number\n" +
                    "    KAuthor_KBookService_AUTHOR_FETCHER:\n" +
                    "      type: object\n" +
                    "      description: The author object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          description: |+\n" +
                    "            The id is long, but the client type is string\n" +
                    "            because JS cannot retain large long values\n" +
                    "          type: string\n" +
                    "        firstName:\n" +
                    "          description: |+\n" +
                    "            The first name of this author\n" +
                    "             <p>Together with `lastName`, this property forms the key of the book</p>\n" +
                    "          type: string\n" +
                    "        lastName:\n" +
                    "          description: |+\n" +
                    "            The last name of this author\n" +
                    "             <p>Together with `firstName`, this property forms the key of the book</p>\n" +
                    "          type: string\n" +
                    "        gender:\n" +
                    "          type: string\n" +
                    "          enum:\n" +
                    "            - MALE\n" +
                    "            - FEMALE\n" +
                    "        books:\n" +
                    "          description: All the books I have written\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/KAuthor_KBookService_AUTHOR_FETCHER_books'\n" +
                    "    KAuthor_KBookService_AUTHOR_FETCHER_books:\n" +
                    "      type: object\n" +
                    "      description: The book object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          description: |+\n" +
                    "            The id is long, but the client type is string\n" +
                    "            because JS cannot retain large long values\n" +
                    "          type: string\n" +
                    "        createdTime:\n" +
                    "          description: Created time\n" +
                    "          type: string\n" +
                    "        modifiedTime:\n" +
                    "          description: Modified time\n" +
                    "          type: string\n" +
                    "        name:\n" +
                    "          description: |+\n" +
                    "            The name of this book,\n" +
                    "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        edition:\n" +
                    "          description: |+\n" +
                    "            The edition of this book,\n" +
                    "             <p>Together with `name`, this property forms the key of the book</p>\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        price:\n" +
                    "          description: The price of this book\n" +
                    "          nullable: true\n" +
                    "          type: number\n" +
                    "        store:\n" +
                    "          description: 'The bookstore to which the current book belongs, null is allowed'\n" +
                    "          nullable: true\n" +
                    "          \$ref: '#/components/schemas/KAuthor_KBookService_AUTHOR_FETCHER_books_store'\n" +
                    "    KAuthor_KBookService_AUTHOR_FETCHER_books_store:\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          type: integer\n" +
                    "          format: int64\n" +
                    "        name:\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        coordinate:\n" +
                    "          \$ref: '#/components/schemas/KCoordinate'\n" +
                    "        level:\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        openTime:\n" +
                    "          type: string\n" +
                    "        closeTime:\n" +
                    "          type: string\n" +
                    "  securitySchemes:\n" +
                    "    tenantHeader:\n" +
                    "      type: apiKey\n" +
                    "      name: tenant\n" +
                    "      in: header\n",
            writer.toString()
        )
    }

    @Test
    fun testIssue554() {
        val metadata = Metadata
            .newBuilder()
            .setOperationParser(OperationParserImpl())
            .setParameterParser(ParameterParserImpl())
            .setGroups(setOf("kBookStoreService"))
            .build()
        val generator = OpenApiGenerator(
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
                    listOf(
                        Collections.singletonMap("tenantHeader", emptyList())
                    )
                )
                .setServers(
                    listOf(
                        OpenApiProperties.newServerBuilder()
                            .setUrl("http://localhost:8080")
                            .build()
                    )
                )
                .setComponents(
                    OpenApiProperties.newComponentsBuilder()
                        .setSecuritySchemes(
                            mapOf(
                                "tenantHeader" to
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
        )
        val writer = StringWriter()
        generator.generate(writer)
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
                    "paths:\n" +
                    "  /bookStore:\n" +
                    "    put:\n" +
                    "      tags:\n" +
                    "        - KBookStoreService\n" +
                    "      operationId: saveBookForIssue554\n" +
                    "      requestBody:\n" +
                    "        content:\n" +
                    "          application/json:\n" +
                    "            schema:\n" +
                    "              \$ref: '#/components/schemas/KBookStoreService_BookStoreInput'\n" +
                    "        required: true\n" +
                    "      responses:\n" +
                    "        200:\n" +
                    "          description: OK\n" +
                    "  /bookStores:\n" +
                    "    get:\n" +
                    "      tags:\n" +
                    "        - KBookStoreService\n" +
                    "      operationId: findDefaultBookStores\n" +
                    "      responses:\n" +
                    "        200:\n" +
                    "          description: OK\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                type: array\n" +
                    "                items:\n" +
                    "                  \$ref: '#/components/schemas/Dynamic_KBookStore'\n" +
                    "components:\n" +
                    "  schemas:\n" +
                    "    KBookStoreService_BookStoreInput:\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          type: integer\n" +
                    "          format: int64\n" +
                    "        name:\n" +
                    "          type: string\n" +
                    "    Dynamic_KBookStore:\n" +
                    "      type: object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          type: integer\n" +
                    "          format: int64\n" +
                    "        name:\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        coordinate:\n" +
                    "          \$ref: '#/components/schemas/KCoordinate'\n" +
                    "        level:\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        books:\n" +
                    "          description: All books available in this bookstore\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/Dynamic_KBook'\n" +
                    "        openTime:\n" +
                    "          type: string\n" +
                    "        closeTime:\n" +
                    "          type: string\n" +
                    "    KCoordinate:\n" +
                    "      type: object\n" +
                    "      description: A location marked by longitude and latitude\n" +
                    "      properties:\n" +
                    "        longitude:\n" +
                    "          description: 'The latitude, from -180 to +180'\n" +
                    "          type: number\n" +
                    "        latitude:\n" +
                    "          description: 'The latitude, from -90 to +90'\n" +
                    "          type: number\n" +
                    "    Dynamic_KBook:\n" +
                    "      type: object\n" +
                    "      description: The book object\n" +
                    "      properties:\n" +
                    "        createdTime:\n" +
                    "          description: Created time\n" +
                    "          type: string\n" +
                    "        modifiedTime:\n" +
                    "          description: Modified time\n" +
                    "          type: string\n" +
                    "        id:\n" +
                    "          description: |+\n" +
                    "            The id is long, but the client type is string\n" +
                    "            because JS cannot retain large long values\n" +
                    "          type: string\n" +
                    "        name:\n" +
                    "          description: |+\n" +
                    "            The name of this book,\n" +
                    "            <p>Together with `edition`, this property forms the key of the book</p>\n" +
                    "          nullable: true\n" +
                    "          type: string\n" +
                    "        edition:\n" +
                    "          description: |+\n" +
                    "            The edition of this book,\n" +
                    "             <p>Together with `name`, this property forms the key of the book</p>\n" +
                    "          type: integer\n" +
                    "          format: int32\n" +
                    "        price:\n" +
                    "          description: The price of this book\n" +
                    "          nullable: true\n" +
                    "          type: number\n" +
                    "        store:\n" +
                    "          description: 'The bookstore to which the current book belongs, null is allowed'\n" +
                    "          nullable: true\n" +
                    "          \$ref: '#/components/schemas/Dynamic_KBookStore'\n" +
                    "        authors:\n" +
                    "          description: All authors involved in writing the work\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/Dynamic_KAuthor'\n" +
                    "    Dynamic_KAuthor:\n" +
                    "      type: object\n" +
                    "      description: The author object\n" +
                    "      properties:\n" +
                    "        id:\n" +
                    "          description: |+\n" +
                    "            The id is long, but the client type is string\n" +
                    "            because JS cannot retain large long values\n" +
                    "          type: string\n" +
                    "        firstName:\n" +
                    "          description: |+\n" +
                    "            The first name of this author\n" +
                    "             <p>Together with `lastName`, this property forms the key of the book</p>\n" +
                    "          type: string\n" +
                    "        lastName:\n" +
                    "          description: |+\n" +
                    "            The last name of this author\n" +
                    "             <p>Together with `firstName`, this property forms the key of the book</p>\n" +
                    "          type: string\n" +
                    "        gender:\n" +
                    "          type: string\n" +
                    "          enum:\n" +
                    "            - MALE\n" +
                    "            - FEMALE\n" +
                    "        books:\n" +
                    "          description: All the books I have written\n" +
                    "          type: array\n" +
                    "          items:\n" +
                    "            \$ref: '#/components/schemas/Dynamic_KBook'\n" +
                    "  securitySchemes:\n" +
                    "    tenantHeader:\n" +
                    "      type: apiKey\n" +
                    "      name: tenant\n" +
                    "      in: header\n",
            writer.toString()
        )
    }
}
