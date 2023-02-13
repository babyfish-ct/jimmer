package org.babyfish.jimmer.client.java.feign;

import org.babyfish.jimmer.client.generator.java.JavaContext;
import org.babyfish.jimmer.client.generator.java.TypeDefinitionWriter;
import org.babyfish.jimmer.client.generator.java.DtoWriter;
import org.babyfish.jimmer.client.generator.java.feign.ServiceWriter;
import org.babyfish.jimmer.client.java.model.Book;
import org.babyfish.jimmer.client.java.model.Gender;
import org.babyfish.jimmer.client.java.model.Page;
import org.babyfish.jimmer.client.java.service.BookService;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.meta.ImmutableType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JFeignTest {

    @Test
    public void testBookService() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JavaContext ctx = createContext(out);
        Service service = Constants.JAVA_METADATA.getServices().get(BookService.class);
        new ServiceWriter(ctx, service).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "package com.myapp.services;\n" +
                        "\n" +
                        "import com.myapp.model.dto.AuthorDto;\n" +
                        "import com.myapp.model.dto.BookDto;\n" +
                        "import com.myapp.model.entities.Dynamic_Book;\n" +
                        "import com.myapp.model.simple.BookInput;\n" +
                        "import com.myapp.model.simple.FindBookArguments;\n" +
                        "import com.myapp.model.simple.Page;\n" +
                        "import com.myapp.model.simple.Tuple2;\n" +
                        "import java.math.BigDecimal;\n" +
                        "import java.util.List;\n" +
                        "import org.springframework.cloud.openfeign.FeignClient;\n" +
                        "import org.springframework.web.bind.annotation.DeleteMapping;\n" +
                        "import org.springframework.web.bind.annotation.GetMapping;\n" +
                        "import org.springframework.web.bind.annotation.PathVariable;\n" +
                        "import org.springframework.web.bind.annotation.PutMapping;\n" +
                        "import org.springframework.web.bind.annotation.RequestBody;\n" +
                        "import org.springframework.web.bind.annotation.RequestParam;\n" +
                        "\n" +
                        "/**\n" +
                        " * BookService interface\n" +
                        " */\n" +
                        "@FeignClient(name = \"myFeignItf\")\n" +
                        "public interface BookService {\n" +
                        "    \n" +
                        "    @DeleteMapping(\"/java/book/{id}\")\n" +
                        "    int deleteBook(@PathVariable(\"id\") long id);\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * Find book list\n" +
                        "     * \n" +
                        "     * Format of each element:\n" +
                        "     * -   id\n" +
                        "     * -   name\n" +
                        "     * -   edition\n" +
                        "     * -   price\n" +
                        "     * -   store\n" +
                        "     *     -   id\n" +
                        "     *     -   name\n" +
                        "     * -   authors\n" +
                        "     *     -   id\n" +
                        "     *     -   firstName\n" +
                        "     */\n" +
                        "    @GetMapping(\"/java/books/complex\")\n" +
                        "    List<BookDto.BookService_COMPLEX_FETCHER> findComplexBooks(\n" +
                        "        @RequestParam(name = \"name\") String name, \n" +
                        "        @RequestParam(name = \"storeName\", required = false) String storeName, \n" +
                        "        @RequestParam(name = \"authorName\", required = false) String authorName, \n" +
                        "        @RequestParam(name = \"minPrice\", required = false) BigDecimal minPrice, \n" +
                        "        @RequestParam(name = \"maxPrice\", required = false) BigDecimal maxPrice\n" +
                        "    );\n" +
                        "    \n" +
                        "    @GetMapping(\"/java/books/complex2\")\n" +
                        "    List<BookDto.BookService_COMPLEX_FETCHER> findComplexBooksByArguments(FindBookArguments arguments);\n" +
                        "    \n" +
                        "    @GetMapping(\"/java/books/simple\")\n" +
                        "    List<BookDto.BookService_SIMPLE_FETCHER> findSimpleBooks();\n" +
                        "    \n" +
                        "    @GetMapping(\"/java/tuples\")\n" +
                        "    Page<Tuple2<BookDto.BookService_COMPLEX_FETCHER, AuthorDto.BookService_AUTHOR_FETCHER>> findTuples(\n" +
                        "        @RequestParam(name = \"name\", required = false) String name, \n" +
                        "        @RequestParam(name = \"pageIndex\") int pageIndex, \n" +
                        "        @RequestParam(name = \"pageSize\") int pageSize\n" +
                        "    );\n" +
                        "    \n" +
                        "    @PutMapping(\"/java/book\")\n" +
                        "    Dynamic_Book saveBooks(@RequestBody BookInput body);\n" +
                        "    \n" +
                        "    @GetMapping(\"/java/version\")\n" +
                        "    int version();\n" +
                        "}",
                code
        );
    }

    @Test
    public void testBookDto() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DtoWriter(createContext(out), Book.class).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "package com.myapp.model.dto;\n" +
                        "\n" +
                        "import java.math.BigDecimal;\n" +
                        "import java.util.List;\n" +
                        "import org.jetbrains.annotations.Nullable;\n" +
                        "\n" +
                        "public interface BookDto {\n" +
                        "    \n" +
                        "    class BookService_SIMPLE_FETCHER {\n" +
                        "        \n" +
                        "        private long id;\n" +
                        "        \n" +
                        "        private String name;\n" +
                        "        \n" +
                        "        public long getId() {\n" +
                        "            return id;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public void setId(long id) {\n" +
                        "            this.id = id;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public String getName() {\n" +
                        "            return name;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public void setName(String name) {\n" +
                        "            this.name = name;\n" +
                        "        }\n" +
                        "    }\n" +
                        "    \n" +
                        "    class BookService_COMPLEX_FETCHER {\n" +
                        "        \n" +
                        "        private long id;\n" +
                        "        \n" +
                        "        private String name;\n" +
                        "        \n" +
                        "        private int edition;\n" +
                        "        \n" +
                        "        private BigDecimal price;\n" +
                        "        \n" +
                        "        /**\n" +
                        "         * The bookstore to which the current book belongs, null is allowd\n" +
                        "         */\n" +
                        "        @Nullable\n" +
                        "        private TargetOf_store store;\n" +
                        "        \n" +
                        "        /**\n" +
                        "         * All authors involved in writing the work\n" +
                        "         */\n" +
                        "        private List<TargetOf_authors> authors;\n" +
                        "        \n" +
                        "        public long getId() {\n" +
                        "            return id;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public void setId(long id) {\n" +
                        "            this.id = id;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public String getName() {\n" +
                        "            return name;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public void setName(String name) {\n" +
                        "            this.name = name;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public int getEdition() {\n" +
                        "            return edition;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public void setEdition(int edition) {\n" +
                        "            this.edition = edition;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public BigDecimal getPrice() {\n" +
                        "            return price;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public void setPrice(BigDecimal price) {\n" +
                        "            this.price = price;\n" +
                        "        }\n" +
                        "        \n" +
                        "        @Nullable\n" +
                        "        public TargetOf_store getStore() {\n" +
                        "            return store;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public void setStore(@Nullable TargetOf_store store) {\n" +
                        "            this.store = store;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public List<TargetOf_authors> getAuthors() {\n" +
                        "            return authors;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public void setAuthors(List<TargetOf_authors> authors) {\n" +
                        "            this.authors = authors;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public static class TargetOf_store {\n" +
                        "            \n" +
                        "            private long id;\n" +
                        "            \n" +
                        "            private String name;\n" +
                        "            \n" +
                        "            public long getId() {\n" +
                        "                return id;\n" +
                        "            }\n" +
                        "            \n" +
                        "            public void setId(long id) {\n" +
                        "                this.id = id;\n" +
                        "            }\n" +
                        "            \n" +
                        "            public String getName() {\n" +
                        "                return name;\n" +
                        "            }\n" +
                        "            \n" +
                        "            public void setName(String name) {\n" +
                        "                this.name = name;\n" +
                        "            }\n" +
                        "        }\n" +
                        "        \n" +
                        "        public static class TargetOf_authors {\n" +
                        "            \n" +
                        "            private long id;\n" +
                        "            \n" +
                        "            private String firstName;\n" +
                        "            \n" +
                        "            private String lastName;\n" +
                        "            \n" +
                        "            public long getId() {\n" +
                        "                return id;\n" +
                        "            }\n" +
                        "            \n" +
                        "            public void setId(long id) {\n" +
                        "                this.id = id;\n" +
                        "            }\n" +
                        "            \n" +
                        "            public String getFirstName() {\n" +
                        "                return firstName;\n" +
                        "            }\n" +
                        "            \n" +
                        "            public void setFirstName(String firstName) {\n" +
                        "                this.firstName = firstName;\n" +
                        "            }\n" +
                        "            \n" +
                        "            public String getLastName() {\n" +
                        "                return lastName;\n" +
                        "            }\n" +
                        "            \n" +
                        "            public void setLastName(String lastName) {\n" +
                        "                this.lastName = lastName;\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                code
        );
    }

    @Test
    public void testRawBook() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JavaContext ctx = createContext(out);
        ImmutableObjectType bookType = Constants.JAVA_METADATA.getRawImmutableObjectTypes().get(ImmutableType.get(Book.class));
        new TypeDefinitionWriter(ctx, bookType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "package com.myapp.model.entities;\n" +
                        "\n" +
                        "import java.math.BigDecimal;\n" +
                        "import java.util.List;\n" +
                        "import org.jetbrains.annotations.Nullable;\n" +
                        "\n" +
                        "public class Dynamic_Book {\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    private Long id;\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    private String name;\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    private Integer edition;\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    private BigDecimal price;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * The bookstore to which the current book belongs, null is allowd\n" +
                        "     */\n" +
                        "    @Nullable\n" +
                        "    private Dynamic_BookStore store;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * All authors involved in writing the work\n" +
                        "     */\n" +
                        "    @Nullable\n" +
                        "    private List<Dynamic_Author> authors;\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    public Long getId() {\n" +
                        "        return id;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setId(@Nullable Long id) {\n" +
                        "        this.id = id;\n" +
                        "    }\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    public String getName() {\n" +
                        "        return name;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setName(@Nullable String name) {\n" +
                        "        this.name = name;\n" +
                        "    }\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    public Integer getEdition() {\n" +
                        "        return edition;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setEdition(@Nullable Integer edition) {\n" +
                        "        this.edition = edition;\n" +
                        "    }\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    public BigDecimal getPrice() {\n" +
                        "        return price;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setPrice(@Nullable BigDecimal price) {\n" +
                        "        this.price = price;\n" +
                        "    }\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    public Dynamic_BookStore getStore() {\n" +
                        "        return store;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setStore(@Nullable Dynamic_BookStore store) {\n" +
                        "        this.store = store;\n" +
                        "    }\n" +
                        "    \n" +
                        "    @Nullable\n" +
                        "    public List<Dynamic_Author> getAuthors() {\n" +
                        "        return authors;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setAuthors(@Nullable List<Dynamic_Author> authors) {\n" +
                        "        this.authors = authors;\n" +
                        "    }\n" +
                        "}\n",
                code
        );
    }

    @Test
    public void testPage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JavaContext ctx = createContext(out);
        StaticObjectType pageType = Constants.JAVA_METADATA.getGenericTypes().get(Page.class);
        new TypeDefinitionWriter(ctx, pageType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "package com.myapp.model.simple;\n" +
                        "\n" +
                        "import java.util.List;\n" +
                        "\n" +
                        "public class Page<E> {\n" +
                        "    \n" +
                        "    private List<E> entities;\n" +
                        "    \n" +
                        "    private int totalPageCount;\n" +
                        "    \n" +
                        "    private int totalRowCount;\n" +
                        "    \n" +
                        "    public List<E> getEntities() {\n" +
                        "        return entities;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setEntities(List<E> entities) {\n" +
                        "        this.entities = entities;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public int getTotalPageCount() {\n" +
                        "        return totalPageCount;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setTotalPageCount(int totalPageCount) {\n" +
                        "        this.totalPageCount = totalPageCount;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public int getTotalRowCount() {\n" +
                        "        return totalRowCount;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setTotalRowCount(int totalRowCount) {\n" +
                        "        this.totalRowCount = totalRowCount;\n" +
                        "    }\n" +
                        "}\n",
                code
        );
    }

    @Test
    public void testGender() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JavaContext ctx = createContext(out);
        EnumType genderType = Constants.JAVA_METADATA.getEnumTypes().get(Gender.class);
        new TypeDefinitionWriter(ctx, genderType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "package com.myapp.model.enums;\n" +
                        "\n" +
                        "public enum Gender {\n" +
                        "    MALE, \n" +
                        "    FEMALE\n" +
                        "}\n",
                code
        );
    }

    private static JavaContext createContext(OutputStream out) {
        return new JavaContext(Constants.JAVA_METADATA, out, "myFeignItf", 4, "com.myapp");
    }
}
