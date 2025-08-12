package org.babyfish.jimmer.spring.java;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.spring.AbstractTest;
import org.babyfish.jimmer.spring.cfg.ErrorTranslatorConfig;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.babyfish.jimmer.spring.cfg.SqlClientConfig;
import org.babyfish.jimmer.spring.client.JavaFeignController;
import org.babyfish.jimmer.spring.client.OpenApiController;
import org.babyfish.jimmer.spring.client.OpenApiUiController;
import org.babyfish.jimmer.spring.java.bll.BookService;
import org.babyfish.jimmer.spring.client.TypeScriptController;
import org.babyfish.jimmer.spring.java.bll.ErrorService;
import org.babyfish.jimmer.spring.java.bll.resolver.BookStoreNewestBooksResolver;
import org.babyfish.jimmer.spring.java.dal.BookRepository;
import org.babyfish.jimmer.spring.datasource.DataSources;
import org.babyfish.jimmer.spring.datasource.TxCallback;
import org.babyfish.jimmer.spring.java.dal.BookStoreRepository;
import org.babyfish.jimmer.spring.java.model.*;
import org.babyfish.jimmer.spring.java.model.dto.BookSpecification;
import org.babyfish.jimmer.spring.java.model.dto.BookStoreView;
import org.babyfish.jimmer.spring.java.model.dto.BookView;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoryConfigExtension;
import org.babyfish.jimmer.spring.repository.support.JimmerRepositoryFactoryBean;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

@SpringBootTest(properties = {
        "jimmer.client.ts.path=/my-ts.zip",
        "jimmer.client.openapi.path=/my-openapi.yml",
        "jimmer.client.openapi.uiPath=/my-openapi.html",
        "jimmer.client.openapi.properties.info.title=BookSystem",
        "jimmer.client.openapi.properties.info.description=Use this system to access book system",
        "jimmer.database-validation-mode=ERROR",
        "jimmer.dialect=org.babyfish.jimmer.sql.dialect.H2Dialect",
        "jimmer.in-list-to-any-equality-enabled=true",
        "spring.application.name=java-client"
})
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableJimmerRepositories
@EnableConfigurationProperties(JimmerProperties.class)
@Import({SqlClientConfig.class, ErrorTranslatorConfig.class})
@EnableImplicitApi
public class SpringJavaTest extends AbstractTest {

    private final static List<String> TRANSACTION_EVENTS = new ArrayList<>();

    private final static List<String> SQL_STATEMENTS = new ArrayList<>();

    @BeforeAll
    public static void beforeAll() {
        initDatabase(DataSources.create(null));
    }

    @BeforeEach
    public void beforeEach() {
        TRANSACTION_EVENTS.clear();
        SQL_STATEMENTS.clear();
    }

    @EnableJimmerRepositories
    @ConditionalOnMissingBean({ JimmerRepositoryFactoryBean.class, JimmerRepositoryConfigExtension.class })
    @Configuration
    static class DuplicatedConfig {
        // Use @EnableJimmerRepositories twice,
        // use @ConditionalOnMissBean to resolve conflict
    }

    @EnableWebMvc
    @Configuration
    static class SqlClientConfig {

        @Bean
        public DataSource dataSource() {
            return DataSources.create(
                    new TxCallback() {

                        @Override
                        public void open() {
                            TRANSACTION_EVENTS.add("connect");
                        }

                        @Override
                        public void commit() {
                            TRANSACTION_EVENTS.add("commit");
                        }

                        @Override
                        public void rollback() {
                            TRANSACTION_EVENTS.add("rollback");
                        }
                    }
            );
        }

        @Bean
        public Executor executor() {
            return new Executor() {
                @Override
                public <R> R execute(@NotNull Args<R> args) {
                    SQL_STATEMENTS.add(args.sql);
                    return DefaultExecutor.INSTANCE.execute(args);
                }

                @Override
                public BatchContext executeBatch(
                        @NotNull Connection con,
                        @NotNull String sql,
                        @Nullable ImmutableProp generatedIdProp,
                        @NotNull ExecutionPurpose purpose,
                        @NotNull JSqlClientImplementor sqlClient
                ) {
                    return DefaultExecutor.INSTANCE.executeBatch(
                            con,
                            sql,
                            generatedIdProp,
                            purpose,
                            sqlClient
                    );
                }
            };
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new JdbcTransactionManager(dataSource);
        }

        @Bean
        public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }

        @Bean
        public BookService bookService(BookRepository bookRepository) {
            return new BookService(bookRepository);
        }

        @Bean
        public BookStoreNewestBooksResolver bookStoreNewestBooksResolver(BookStoreRepository bookStoreRepository) {
            return new BookStoreNewestBooksResolver(bookStoreRepository);
        }

        @Bean
        public ErrorService errorService() {
            return new ErrorService();
        }

        @Bean
        public MockMvc mockMvc(WebApplicationContext ctx) {
            return webAppContextSetup(ctx).build();
        }

        @ConditionalOnProperty("jimmer.client.ts.path")
        @ConditionalOnMissingBean(TypeScriptController.class)
        @Bean
        public TypeScriptController typeScriptController(JimmerProperties properties) {
            return new TypeScriptController(properties);
        }

        @ConditionalOnProperty("jimmer.client.openapi.path")
        @ConditionalOnMissingBean(OpenApiController.class)
        @Bean
        public OpenApiController openApiController(JimmerProperties properties) {
            return new OpenApiController(properties);
        }

        @ConditionalOnProperty("jimmer.client.openapi.ui-path")
        @ConditionalOnMissingBean(OpenApiUiController.class)
        @Bean
        public OpenApiUiController openApiUiController(JimmerProperties properties) {
            return new OpenApiUiController(properties);
        }

        @ConditionalOnProperty("jimmer.client.java-feign.path")
        @ConditionalOnMissingBean(JavaFeignController.class)
        @Bean
        public JavaFeignController javaFeignController(JimmerProperties properties) {
            return new JavaFeignController(properties);
        }
    }

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookStoreRepository bookStoreRepository;

    @Autowired
    private ErrorService errorService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JSqlClient sqlClient;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JimmerProperties jimmerProperties;

    @Test
    public void testProperties() {
        Assertions.assertEquals(
                "/my-ts.zip",
                jimmerProperties.getClient().getTs().getPath()
        );
    }

    @Test
    public void testBySortedProps() {

        assertTransactionEvents();
        Assertions.assertEquals(12, bookRepository.findAll(BookProps.NAME.desc()).size());
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "order by tb_1_.NAME desc"
        );
        assertTransactionEvents("connect");

        assertTransactionEvents();
        Page<Book> page = bookRepository.findAll(0, 10, BookProps.NAME.desc());
        assertSQLs(
                "select count(1) from BOOK tb_1_",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "order by tb_1_.NAME desc " +
                        "limit ?"
        );
        Assertions.assertEquals(12, page.getTotalElements());
        Assertions.assertEquals(2, page.getTotalPages());
        Assertions.assertEquals(
                Sort.by(
                        Collections.singletonList(
                                new Sort.Order(Sort.Direction.DESC, "name")
                        )
                ),
                page.getPageable().getSort()
        );
        assertTransactionEvents("connect", "connect");
    }

    @Test
    public void testByTransaction() {

        assertTransactionEvents();
        transactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(@NotNull TransactionStatus status) {
                Assertions.assertEquals(12, bookRepository.findAll(BookProps.NAME.desc()).size());
                assertSQLs(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                "from BOOK tb_1_ " +
                                "order by tb_1_.NAME desc"
                );

                Page<Book> page = bookRepository.findAll(0, 10, BookProps.NAME.desc());
                assertSQLs(
                        "select count(1) from BOOK tb_1_",
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                "from BOOK tb_1_ " +
                                "order by tb_1_.NAME desc " +
                                "limit ?"
                );
                Assertions.assertEquals(12, page.getTotalElements());
                Assertions.assertEquals(2, page.getTotalPages());
                Assertions.assertEquals(
                        Sort.by(
                                Collections.singletonList(
                                        new Sort.Order(Sort.Direction.DESC, "name")
                                )
                        ),
                        page.getPageable().getSort()
                );
                return null;
            }
        });
        assertTransactionEvents("connect", "commit");
    }

    @Test
    public void testByJimmerTransaction() {

        assertTransactionEvents();
        sqlClient.transaction(() -> {
            Assertions.assertEquals(12, bookRepository.findAll(BookProps.NAME.desc()).size());
            assertSQLs(
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK tb_1_ " +
                            "order by tb_1_.NAME desc"
            );

            Page<Book> page = bookRepository.findAll(0, 10, BookProps.NAME.desc());
            assertSQLs(
                    "select count(1) from BOOK tb_1_",
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                            "from BOOK tb_1_ " +
                            "order by tb_1_.NAME desc " +
                            "limit ?"
            );
            Assertions.assertEquals(12, page.getTotalElements());
            Assertions.assertEquals(2, page.getTotalPages());
            Assertions.assertEquals(
                    Sort.by(
                            Collections.singletonList(
                                    new Sort.Order(Sort.Direction.DESC, "name")
                            )
                    ),
                    page.getPageable().getSort()
            );
            return null;
        });
        assertTransactionEvents("connect", "commit");
    }

    @Test
    public void testBySpringSort() {

        Sort sort = SortUtils.toSort(BookProps.NAME, BookProps.EDITION.desc());

        assertTransactionEvents();
        Assertions.assertEquals(12, bookRepository.findAll(sort).size());
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc"
        );
        assertTransactionEvents("connect");

        assertTransactionEvents();
        Page<Book> page = bookRepository.findAll(0, 10, sort);
        assertSQLs(
                "select count(1) from BOOK tb_1_",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                        "limit ?"
        );
        Assertions.assertEquals(12, page.getTotalElements());
        Assertions.assertEquals(2, page.getTotalPages());
        Assertions.assertEquals(
                Sort.by(
                        Arrays.asList(
                                new Sort.Order(Sort.Direction.ASC, "name"),
                                new Sort.Order(Sort.Direction.DESC, "edition")
                        )
                ),
                page.getPageable().getSort()
        );
        assertTransactionEvents("connect", "connect");
    }

    @Test
    public void testBySpringPageable() {

        Pageable pageable = PageRequest.of(0, 10, SortUtils.toSort(BookProps.NAME.desc()));

        assertTransactionEvents();
        Page<Book> page = bookRepository.findAll(pageable);
        assertSQLs(
                "select count(1) from BOOK tb_1_",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "order by tb_1_.NAME desc " +
                        "limit ?"
        );
        Assertions.assertEquals(12, page.getTotalElements());
        Assertions.assertEquals(2, page.getTotalPages());
        Assertions.assertEquals(
                Sort.by(
                        Collections.singletonList(
                                new Sort.Order(Sort.Direction.DESC, "name")
                        )
                ),
                page.getPageable().getSort()
        );
        assertTransactionEvents("connect", "connect");
    }

    @Test
    public void testFindByNameOrderByNameAscEditionDesc() {
        List<Book> books = bookRepository.findByNameOrderByNameAscEditionDesc(
                "GraphQL in Action",
                BookFetcher.$
                        .allScalarFields()
                        .store(BookStoreFetcher.$.allScalarFields())
        );
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "where tb_1_.NAME = ? " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc",
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.ID = ?"
        );
        assertContent(
                "[" +
                        "--->{" +
                        "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                        "--->--->--->\"name\":\"MANNING\"" +
                        "--->--->}" +
                        "--->}, {" +
                        "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                        "--->--->--->\"name\":\"MANNING\"" +
                        "--->--->}" +
                        "--->}, {" +
                        "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                        "--->--->--->\"name\":\"MANNING\"" +
                        "--->--->}" +
                        "--->}" +
                        "]",
                books
        );
    }

    @Test
    public void testCountName() {
        Assertions.assertEquals(
                3,
                bookRepository.countByName("GraphQL in Action")
        );
        assertSQLs("select count(tb_1_.ID) from BOOK tb_1_ where tb_1_.NAME = ?");
    }

    @Test
    public void testFindByNameLikeIgnoreCaseAndStoreNameOrderByNameAscEditionDesc() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Book> page = bookRepository.findByNameLikeIgnoreCaseAndStoreNameOrderByNameAscEditionDesc(
                pageable,
                BookFetcher.$.allScalarFields()
                        .allScalarFields()
                        .authors(
                                AuthorFetcher.$
                                        .allScalarFields()
                        ),
                "graphql",
                "O'REILLY"
        );
        assertSQLs(
                "select count(1) " +
                        "from BOOK tb_1_ " +
                        "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                        "where tb_1_.NAME ilike ? " +
                        "and tb_2_.NAME = ?",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                        "from BOOK tb_1_ " +
                        "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                        "where tb_1_.NAME ilike ? " +
                        "and tb_2_.NAME = ? " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                        "limit ?",
                "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                        "from AUTHOR tb_1_ " +
                        "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                        "where tb_2_.BOOK_ID = any(?)"
        );
        assertContent(
                "[" +
                        "--->{" +
                        "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":51.00," +
                        "--->--->\"authors\":[" +
                        "--->--->--->{" +
                        "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                        "--->--->--->--->\"firstName\":\"Alex\"," +
                        "--->--->--->--->\"lastName\":\"Banks\"," +
                        "--->--->--->--->\"gender\":\"MALE\"" +
                        "--->--->--->},{" +
                        "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                        "--->--->--->--->\"firstName\":\"Eve\"," +
                        "--->--->--->--->\"lastName\":\"Procello\"," +
                        "--->--->--->--->\"gender\":\"FEMALE\"" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}, {" +
                        "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":55.00," +
                        "--->--->\"authors\":[" +
                        "--->--->--->{" +
                        "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                        "--->--->--->--->\"firstName\":\"Alex\"," +
                        "--->--->--->--->\"lastName\":\"Banks\"," +
                        "--->--->--->--->\"gender\":\"MALE\"" +
                        "--->--->--->},{" +
                        "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                        "--->--->--->--->\"firstName\":\"Eve\"," +
                        "--->--->--->--->\"lastName\":\"Procello\"," +
                        "--->--->--->--->\"gender\":\"FEMALE\"" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}" +
                        "]",
                page.getContent()
        );
        Assertions.assertEquals(3, page.getTotalElements());
        Assertions.assertEquals(2, page.getTotalPages());
    }

    @Test
    public void testFindByNameLikeIgnoreCaseOrderByNameAscEditionDesc() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Book> page = bookRepository.findByNameLikeIgnoreCaseAndStoreNameOrderByNameAscEditionDesc(
                pageable,
                null,
                "graphql",
                null
        );
        assertSQLs(
                "select count(1) from BOOK tb_1_ where tb_1_.NAME ilike ?",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID from BOOK tb_1_ where tb_1_.NAME ilike ? order by tb_1_.NAME asc, tb_1_.EDITION desc limit ?"
        );
    }

    @Test
    public void testFindByStoreNameOrderByNameAscEditionDesc() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Book> page = bookRepository.findByNameLikeIgnoreCaseAndStoreNameOrderByNameAscEditionDesc(
                pageable,
                null,
                null,
                "MANNING"
        );
        assertSQLs(
                "select count(1) " +
                        "from BOOK tb_1_ " +
                        "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                        "where tb_2_.NAME = ?",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                        "where tb_2_.NAME = ? " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                        "limit ?"
        );
    }

    @Test
    public void testFindDistinctPriceByPriceBetween() {
        bookRepository.findDistinctPriceByPriceBetween(null, null);
        assertSQLs(
                "select distinct tb_1_.PRICE from BOOK tb_1_"
        );
        bookRepository.findDistinctPriceByPriceBetween(new BigDecimal(40), null);
        assertSQLs(
                "select distinct tb_1_.PRICE from BOOK tb_1_ where tb_1_.PRICE >= ?"
        );
        bookRepository.findDistinctPriceByPriceBetween(null, new BigDecimal(50));
        assertSQLs(
                "select distinct tb_1_.PRICE from BOOK tb_1_ where tb_1_.PRICE <= ?"
        );
        bookRepository.findDistinctPriceByPriceBetween(new BigDecimal(40), new BigDecimal(50));
        assertSQLs(
                "select distinct tb_1_.PRICE from BOOK tb_1_ where tb_1_.PRICE between ? and ?"
        );
    }

    @Test
    public void testByCalculatedAssociation() {
        List<BookStore> bookStores = bookStoreRepository.findAll(
                BookStoreFetcher.$
                        .allScalarFields()
                        .newestBooks(
                                BookFetcher.$
                                        .allScalarFields()
                                        .authors(
                                                AuthorFetcher.$.allScalarFields()
                                        )
                        )
        );
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_",
                "select tb_1_.ID, tb_2_.ID " +
                        "from BOOK_STORE tb_1_ " +
                        "inner join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                        "where (tb_2_.NAME, tb_2_.EDITION) in (" +
                        "--->select tb_3_.NAME, max(tb_3_.EDITION) " +
                        "--->from BOOK tb_3_ " +
                        "--->where tb_3_.STORE_ID = any(?) " +
                        "--->group by tb_3_.NAME" +
                        ")",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                        "from BOOK tb_1_ " +
                        "where tb_1_.ID = any(?)",
                "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                        "from AUTHOR tb_1_ " +
                        "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                        "where tb_2_.BOOK_ID = any(?)"
        );
        assertContent(
                "[" +
                        "--->{" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"O'REILLY\"," +
                        "--->--->\"newestBooks\":[" +
                        "--->--->--->{" +
                        "--->--->--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                        "--->--->--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->--->--->\"edition\":3," +
                        "--->--->--->--->\"price\":51.00," +
                        "--->--->--->--->\"authors\":[" +
                        "--->--->--->--->--->{" +
                        "--->--->--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                        "--->--->--->--->--->--->\"firstName\":\"Alex\"," +
                        "--->--->--->--->--->--->\"lastName\":\"Banks\"," +
                        "--->--->--->--->--->--->\"gender\":\"MALE\"" +
                        "--->--->--->--->--->},{" +
                        "--->--->--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                        "--->--->--->--->--->--->\"firstName\":\"Eve\"," +
                        "--->--->--->--->--->--->\"lastName\":\"Procello\"," +
                        "--->--->--->--->--->--->\"gender\":\"FEMALE\"" +
                        "--->--->--->--->--->}" +
                        "--->--->--->--->]" +
                        "--->--->--->},{" +
                        "--->--->--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                        "--->--->--->--->\"name\":\"Effective TypeScript\"," +
                        "--->--->--->--->\"edition\":3," +
                        "--->--->--->--->\"price\":88.00,\"authors\":[" +
                        "--->--->--->--->--->{" +
                        "--->--->--->--->--->--->\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"," +
                        "--->--->--->--->--->--->\"firstName\":\"Dan\"," +
                        "--->--->--->--->--->--->\"lastName\":\"Vanderkam\"," +
                        "--->--->--->--->--->--->\"gender\":\"MALE\"" +
                        "--->--->--->--->--->}" +
                        "--->--->--->--->]" +
                        "--->--->--->},{" +
                        "--->--->--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                        "--->--->--->--->\"name\":\"Programming TypeScript\"," +
                        "--->--->--->--->\"edition\":3," +
                        "--->--->--->--->\"price\":48.00,\"authors\":[" +
                        "--->--->--->--->--->{" +
                        "--->--->--->--->--->--->\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"," +
                        "--->--->--->--->--->--->\"firstName\":\"Boris\"," +
                        "--->--->--->--->--->--->\"lastName\":\"Cherny\"," +
                        "--->--->--->--->--->--->\"gender\":\"MALE\"" +
                        "--->--->--->--->--->}" +
                        "--->--->--->--->]" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}, {" +
                        "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                        "--->--->\"name\":\"MANNING\"," +
                        "--->--->\"newestBooks\":[" +
                        "--->--->--->{" +
                        "--->--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                        "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->--->\"edition\":3," +
                        "--->--->--->--->\"price\":80.00," +
                        "--->--->--->--->\"authors\":[" +
                        "--->--->--->--->--->{" +
                        "--->--->--->--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                        "--->--->--->--->--->--->\"firstName\":\"Samer\"," +
                        "--->--->--->--->--->--->\"lastName\":\"Buna\"," +
                        "--->--->--->--->--->--->\"gender\":\"MALE\"" +
                        "--->--->--->--->--->}" +
                        "--->--->--->--->]" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}" +
                        "]",
                bookStores
        );
    }

    @Test
    public void testBookView() {
        List<BookView> books = bookRepository.viewer(BookView.class).findAll(
                BookProps.NAME.asc(),
                BookProps.EDITION.desc()
        );
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc",
                "select tb_1_.ID, tb_1_.NAME " +
                        "from BOOK_STORE tb_1_ " +
                        "where tb_1_.ID = any(?)",
                "select " +
                        "--->tb_2_.BOOK_ID, " +
                        "--->tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                        "from AUTHOR tb_1_ " +
                        "inner join BOOK_AUTHOR_MAPPING tb_2_ " +
                        "--->on tb_1_.ID = tb_2_.AUTHOR_ID " +
                        "where tb_2_.BOOK_ID = any(?)"
        );
        Assertions.assertEquals(12, books.size());
        assertContent(
                "BookView(" +
                        "--->id=9eded40f-6d2e-41de-b4e7-33a28b11c8b6, " +
                        "--->name=Effective TypeScript, " +
                        "--->edition=3, " +
                        "--->price=88.00, " +
                        "--->store=BookView.TargetOf_store(name=O'REILLY), " +
                        "--->authors=[" +
                        "--->--->BookView.TargetOf_authors(" +
                        "--->--->--->firstName=Dan, " +
                        "--->--->--->lastName=Vanderkam, gender=MALE" +
                        "--->--->)" +
                        "--->]" +
                        ")",
                books.get(0)
        );
    }

    @Test
    public void testBookView2() {
        BookView bookView = bookRepository.findByNameAndEdition("GraphQL in Action", 1);
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "where tb_1_.NAME = ? and tb_1_.EDITION = ? " +
                        "limit ?",
                "select tb_1_.ID, tb_1_.NAME " +
                        "from BOOK_STORE tb_1_ " +
                        "where tb_1_.ID = ?",
                "select " +
                        "--->tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                        "from AUTHOR tb_1_ " +
                        "inner join BOOK_AUTHOR_MAPPING tb_2_ " +
                        "--->on tb_1_.ID = tb_2_.AUTHOR_ID " +
                        "where tb_2_.BOOK_ID = ?"
        );
        assertContent(
                "BookView(" +
                        "--->id=a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->name=GraphQL in Action, " +
                        "--->edition=1, " +
                        "--->price=80.00, " +
                        "--->store=BookView.TargetOf_store(" +
                        "--->--->name=MANNING" +
                        "--->), " +
                        "--->authors=[" +
                        "--->--->BookView.TargetOf_authors(" +
                        "--->--->--->firstName=Samer, " +
                        "--->--->--->lastName=Buna, " +
                        "--->--->--->gender=MALE" +
                        "--->--->)" +
                        "--->]" +
                        ")",
                bookView
        );
    }

    @Test
    public void testBookStoreView() {
        List<BookStoreView> views = bookStoreRepository.findAllOrderByName(BookStoreView.class);
        Assertions.assertEquals(2, views.size());
        assertContent(
                "BookStoreView(" +
                        "--->id=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                        "--->name=MANNING, " +
                        "--->books=[" +
                        "--->--->BookStoreView.TargetOf_books(" +
                        "--->--->--->id=a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->--->--->name=GraphQL in Action, " +
                        "--->--->--->edition=1, " +
                        "--->--->--->price=80.00" +
                        "--->--->), " +
                        "--->--->BookStoreView.TargetOf_books(" +
                        "--->--->--->id=e37a8344-73bb-4b23-ba76-82eac11f03e6, " +
                        "--->--->--->name=GraphQL in Action, " +
                        "--->--->--->edition=2, " +
                        "--->--->--->price=81.00" +
                        "--->--->), " +
                        "--->--->BookStoreView.TargetOf_books(" +
                        "--->--->--->id=780bdf07-05af-48bf-9be9-f8c65236fecc, " +
                        "--->--->--->name=GraphQL in Action, " +
                        "--->--->--->edition=3, " +
                        "--->--->--->price=80.00" +
                        "--->--->)" +
                        "--->]" +
                        ")",
                views.get(0)
        );
    }

    @Test
    public void testViewFindMapByIds() {
        List<UUID> list = new ArrayList<>();
        list.add(UUID.fromString("d38c10da-6be8-4924-b9b9-5e81899612a0"));
        Map<UUID, BookStoreView> map = bookStoreRepository.viewer(BookStoreView.class).findMapByIds(list);
        assertContent("BookStoreView(id=d38c10da-6be8-4924-b9b9-5e81899612a0,\n" +
                        " name=O'REILLY,\n" +
                        " books=[BookStoreView.TargetOf_books(id=e110c564-23cc-4811-9e81-d587a13db634,\n" +
                        " name=Learning GraphQL,\n" +
                        " edition=1,\n" +
                        " price=50.00),\n" +
                        " BookStoreView.TargetOf_books(id=b649b11b-1161-4ad2-b261-af0112fdd7c8,\n" +
                        " name=Learning GraphQL,\n" +
                        " edition=2,\n" +
                        " price=55.00),\n" +
                        " BookStoreView.TargetOf_books(id=64873631-5d82-4bae-8eb8-72dd955bfc56,\n" +
                        " name=Learning GraphQL,\n" +
                        " edition=3,\n" +
                        " price=51.00),\n" +
                        " BookStoreView.TargetOf_books(id=8f30bc8a-49f9-481d-beca-5fe2d147c831,\n" +
                        " name=Effective TypeScript,\n" +
                        " edition=1,\n" +
                        " price=73.00),\n" +
                        " BookStoreView.TargetOf_books(id=8e169cfb-2373-4e44-8cce-1f1277f730d1,\n" +
                        " name=Effective TypeScript,\n" +
                        " edition=2,\n" +
                        " price=69.00),\n" +
                        " BookStoreView.TargetOf_books(id=9eded40f-6d2e-41de-b4e7-33a28b11c8b6,\n" +
                        " name=Effective TypeScript,\n" +
                        " edition=3,\n" +
                        " price=88.00),\n" +
                        " BookStoreView.TargetOf_books(id=914c8595-35cb-4f67-bbc7-8029e9e6245a,\n" +
                        " name=Programming TypeScript,\n" +
                        " edition=1,\n" +
                        " price=47.50),\n" +
                        " BookStoreView.TargetOf_books(id=058ecfd0-047b-4979-a7dc-46ee24d08f08,\n" +
                        " name=Programming TypeScript,\n" +
                        " edition=2,\n" +
                        " price=45.00),\n" +
                        " BookStoreView.TargetOf_books(id=782b9a9d-eac8-41c4-9f2d-74a5d047f45a,\n" +
                        " name=Programming TypeScript,\n" +
                        " edition=3,\n" +
                        " price=48.00)])",
                map.get(UUID.fromString("d38c10da-6be8-4924-b9b9-5e81899612a0")));
    }

    @Test
    public void testFindMapByIds() {
        List<UUID> list = new ArrayList<>();
        list.add(UUID.fromString("d38c10da-6be8-4924-b9b9-5e81899612a0"));
        Map<UUID, BookStore> map = bookStoreRepository.findMapByIds(list);
        assertContent("{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\",\n" +
                "\"name\":\"O'REILLY\"}",
                map.get(UUID.fromString("d38c10da-6be8-4924-b9b9-5e81899612a0")));
    }

    @Test
    public void testSpecification() {
        BookSpecification specification = new BookSpecification();
        specification.setMinPrice(new BigDecimal(46));
        specification.setMaxPrice(new BigDecimal(48));
        specification.setAuthorName("Boris");
        List<Book> books = bookRepository.findAll(specification);
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK tb_1_ " +
                        "where tb_1_.PRICE >= ? and tb_1_.PRICE <= ? and exists(" +
                        "--->select 1 " +
                        "--->from AUTHOR tb_2_ " +
                        "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                        "--->where tb_1_.ID = tb_3_.BOOK_ID and " +
                        "--->(tb_2_.FIRST_NAME ilike ? or tb_2_.LAST_NAME ilike ?)" +
                        ")"
        );
        assertContent(
                "[" +
                        "--->{\"id\":\"914c8595-35cb-4f67-bbc7-8029e9e6245a\"," +
                        "--->\"name\":\"Programming TypeScript\"," +
                        "--->\"edition\":1," +
                        "--->\"price\":47.50," +
                        "--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "}, {" +
                        "--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                        "--->\"name\":\"Programming TypeScript\"," +
                        "--->\"edition\":3," +
                        "--->\"price\":48.00," +
                        "--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "}" +
                        "]",
                books
        );
    }

    @Test
    public void testError() throws Exception {
        mvc.perform(get("/error/test"))
                .andExpect(status().is5xxServerError())
                .andExpect(
                        content().string(
                                "{" +
                                        "\"family\":\"GEOGRAPHY\"," +
                                        "\"code\":\"ILLEGAL_POSITION\"," +
                                        "\"longitude\":104.06," +
                                        "\"latitude\":30.67" +
                                        "}"
                        )
                );
    }

    @Test
    public void testDownloadTypescript() throws Exception {
        mvc.perform(get("/my-ts.zip"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/zip"));
    }

    @Test
    public void testOpenApi() throws Exception {
        MvcResult result = mvc.perform(get("/my-openapi.yml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/yml"))
                .andReturn();
        Thread.sleep(100);
        Assertions.assertTrue(
                result.getResponse().getContentAsString().startsWith(
                        "openapi: 3.0.1\n" +
                                "info:\n" +
                                "  title: BookSystem\n" +
                                "  description: Use this system to access book system\n" +
                                "  version: '<`jimmer.client.openapi.properties.info.version` is unspecified>'\n"
                )
        );
    }

    @Test
    public void testOpenApiUi() throws Exception {
        MvcResult result = mvc.perform(get("/my-openapi.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
        Thread.sleep(200);
        Assertions.assertEquals(
                "<!DOCTYPE html>\n" +
                        "<html lang=\"en\">\n" +
                        "<head>\n" +
                        "  <link rel=\"icon\" href=\"./favicon.ico\" type=\"image/x-icon\">\n" +
                        "  <meta charset=\"utf-8\" />\n" +
                        "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                        "  <meta\n" +
                        "    name=\"description\"\n" +
                        "    content=\"SwaggerUI\"\n" +
                        "  />\n" +
                        "  <title>Jimmer-SwaggerUI</title>\n" +
                        "  <link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui.css\" />\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<div id=\"swagger-ui\"></div>\n" +
                        "<script src=\"https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui-bundle.js\" crossorigin></script>\n" +
                        "<script>\n" +
                        "  window.onload = () => {\n" +
                        "    window.ui = SwaggerUIBundle({\n" +
                        "      url: '/my-openapi.yml',\n" +
                        "      dom_id: '#swagger-ui',\n" +
                        "    });\n" +
                        "  };\n" +
                        "</script>\n" +
                        "</body>\n" +
                        "</html>\n",
                result.getResponse().getContentAsString()
        );
    }

    @Disabled
    @Test
    public void testDownloadJavaFeign() throws Exception {
        mvc.perform(get("/my-java.zip"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/zip"));
    }

    private static void assertTransactionEvents(String ... events) {
        try {
            Assertions.assertEquals(Arrays.asList(events), TRANSACTION_EVENTS);
        } finally {
            TRANSACTION_EVENTS.clear();
        }
    }

    private static void assertSQLs(String ... statements) {
        try {
            for (int i = 0; i < Math.min(statements.length, SQL_STATEMENTS.size()); i++) {
                Assertions.assertEquals(statements[i].replace("--->", ""), SQL_STATEMENTS.get(i), "sql[" + i + ']');
            }
            Assertions.assertEquals(statements.length, SQL_STATEMENTS.size(), "sql count");
        } finally {
            SQL_STATEMENTS.clear();
        }
    }

    private static void assertContent(String content, Object o) {
        Assertions.assertEquals(
                content
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("--->", ""),
                o.toString()
        );
    }
}
