package org.babyfish.jimmer.client.java.java;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.java.JavaContext;
import org.babyfish.jimmer.client.java.service.BookService;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class BookServiceTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParser(new ParameterParserImpl())
                    .setGroups(Collections.singleton("bookService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testService() {
        Context ctx = new JavaContext(METADATA, 4, null);
        Source source = ctx.getRootSource("com/company/project/remote/service/" + BookService.class.getSimpleName());
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "package com.company.project.remote.service;\n" +
                        "\n" +
                        "import com.company.project.remote.model.dto.Author;\n" +
                        "import com.company.project.remote.model.dto.Book;\n" +
                        "import com.company.project.remote.model.dynamic.Book;\n" +
                        "import com.company.project.remote.model.static.BookInput;\n" +
                        "import com.company.project.remote.model.static.FindBookArguments;\n" +
                        "import com.company.project.remote.model.static.Page;\n" +
                        "import com.company.project.remote.model.static.Tuple2;\n" +
                        "import java.util.List;\n" +
                        "import org.jetbrains.annotations.NotNull;\n" +
                        "import org.jetbrains.annotations.Nullable;\n" +
                        "\n" +
                        "public interface BookService {\n" +
                        "    int deleteBook(\n" +
                        "        long bookId\n" +
                        "    );\n" +
                        "    \n" +
                        "    @Nullable Book_BookService_COMPLEX_FETCHER findBook(\n" +
                        "        long id\n" +
                        "    );\n" +
                        "    \n" +
                        "    @NotNull List<Book_BookService_COMPLEX_FETCHER> findComplexBooksByArguments(\n" +
                        "        @NotNull FindBookArguments arguments\n" +
                        "    );\n" +
                        "    \n" +
                        "    @NotNull List<Book_BookService_COMPLEX_FETCHER> findComplexBooks(\n" +
                        "        @NotNull String name, \n" +
                        "        @Nullable String storeName, \n" +
                        "        @Nullable String authorName, \n" +
                        "        @Nullable java.math.BigDecimal minPrice, \n" +
                        "        @Nullable java.math.BigDecimal maxPrice\n" +
                        "    );\n" +
                        "    \n" +
                        "    @NotNull List<Book_BookService_SIMPLE_FETCHER> findSimpleBooks();\n" +
                        "    \n" +
                        "    @NotNull Page<Tuple2<Book_BookService_COMPLEX_FETCHER, Author_BookService_AUTHOR_FETCHER>> findTuples(\n" +
                        "        @Nullable String name, \n" +
                        "        int pageIndex, \n" +
                        "        int pageSize\n" +
                        "    );\n" +
                        "    \n" +
                        "    @NotNull DynamicBook patchBook(\n" +
                        "        @NotNull BookInput input\n" +
                        "    );\n" +
                        "    \n" +
                        "    @NotNull DynamicBook saveBook(\n" +
                        "        @NotNull BookInput input\n" +
                        "    );\n" +
                        "    \n" +
                        "    @NotNull DynamicBook updateBook(\n" +
                        "        @NotNull BookInput input\n" +
                        "    );\n" +
                        "    \n" +
                        "    int version(\n" +
                        "        @NotNull String accessToken, \n" +
                        "        @Nullable String path\n" +
                        "    );\n" +
                        "}\n",
                writer.toString()
        );
    }
}
