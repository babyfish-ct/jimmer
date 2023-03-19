package org.babyfish.jimmer.spring.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.spring.AbstractTest;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.babyfish.jimmer.spring.cfg.MetadataCondition;
import org.babyfish.jimmer.spring.cfg.SqlClientConfig;
import org.babyfish.jimmer.spring.client.JavaFeignController;
import org.babyfish.jimmer.spring.client.MetadataFactoryBean;
import org.babyfish.jimmer.spring.java.bll.BookService;
import org.babyfish.jimmer.spring.client.TypeScriptController;
import org.babyfish.jimmer.spring.java.bll.resolver.BookStoreNewestBooksResolver;
import org.babyfish.jimmer.spring.java.dal.BookRepository;
import org.babyfish.jimmer.spring.datasource.DataSources;
import org.babyfish.jimmer.spring.datasource.TxCallback;
import org.babyfish.jimmer.spring.java.dal.BookStoreRepository;
import org.babyfish.jimmer.spring.java.model.*;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories;
import org.babyfish.jimmer.spring.repository.config.JimmerRepositoryConfigExtension;
import org.babyfish.jimmer.spring.repository.support.JimmerRepositoryFactoryBean;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringBootTest(properties = {
        "jimmer.client.ts.path=/my-ts.zip",
        "jimmer.database-validation-mode=ERROR",
        "jimmer.client.java-feign.path=/my-java.zip",
        "jimmer.client.java-feign.base-package=com.myapp.feign",
        "jimmer.dialect=org.babyfish.jimmer.sql.dialect.H2Dialect",
        "spring.application.name=java-client",
        "jimmer.clients.first.ts.path=/my-ts1.zip",
        "jimmer.clients.seond.ts.path=/my-ts2.zip",
})
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableJimmerRepositories
@EnableConfigurationProperties(JimmerProperties.class)
@Import(SqlClientConfig.class)
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
        public EntityManager entityManager() {
            return JimmerModule.ENTITY_MANAGER;
        }

        @Bean
        public Executor executor() {
            return new Executor() {
                @Override
                public <R> R execute(
                        Connection con,
                        String sql,
                        List<Object> variables,
                        ExecutionPurpose purpose,
                        StatementFactory statementFactory,
                        SqlFunction<PreparedStatement, R> block
                ) {
                    SQL_STATEMENTS.add(sql);
                    return DefaultExecutor.INSTANCE.execute(con, sql, variables, purpose, statementFactory, block);
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
        public MockMvc mockMvc(WebApplicationContext ctx) {
            return webAppContextSetup(ctx).build();
        }

        @ConditionalOnProperty("jimmer.client.ts.path")
        @ConditionalOnMissingBean(TypeScriptController.class)
        @Bean
        public TypeScriptController typeScriptController(Metadata metadata, JimmerProperties properties) {
            return new TypeScriptController(metadata, properties);
        }

        @ConditionalOnProperty("jimmer.client.java-feign.path")
        @ConditionalOnMissingBean(JavaFeignController.class)
        @Bean
        public JavaFeignController javaFeignController(Metadata metadata, JimmerProperties properties) {
            return new JavaFeignController(metadata, properties);
        }

        @Conditional(MetadataCondition.class)
        @ConditionalOnMissingBean(Metadata.class)
        @Bean
        public MetadataFactoryBean metadataFactoryBean(
                ApplicationContext ctx,
                @Autowired(required = false) ParameterNameDiscoverer parameterNameDiscoverer
        ) {
            return new MetadataFactoryBean(ctx, parameterNameDiscoverer);
        }
    }

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookStoreRepository bookStoreRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
                        "from BOOK as tb_1_ " +
                        "order by tb_1_.NAME desc"
        );
        assertTransactionEvents("connect");

        assertTransactionEvents();
        Page<Book> page = bookRepository.findAll(0, 10, BookProps.NAME.desc());
        assertSQLs(
                "select count(tb_1_.ID) from BOOK as tb_1_",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK as tb_1_ " +
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
                                "from BOOK as tb_1_ " +
                                "order by tb_1_.NAME desc"
                );

                Page<Book> page = bookRepository.findAll(0, 10, BookProps.NAME.desc());
                assertSQLs(
                        "select count(tb_1_.ID) from BOOK as tb_1_",
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                "from BOOK as tb_1_ " +
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
    public void testBySpringSort() {

        Sort sort = SortUtils.toSort(BookProps.NAME, BookProps.EDITION.desc());

        assertTransactionEvents();
        Assertions.assertEquals(12, bookRepository.findAll(sort).size());
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK as tb_1_ " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc"
        );
        assertTransactionEvents("connect");

        assertTransactionEvents();
        Page<Book> page = bookRepository.findAll(0, 10, sort);
        assertSQLs(
                "select count(tb_1_.ID) from BOOK as tb_1_",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK as tb_1_ " +
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
                "select count(tb_1_.ID) from BOOK as tb_1_",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK as tb_1_ " +
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
                        "from BOOK as tb_1_ " +
                        "where tb_1_.NAME = ? " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc",
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.ID = ?"
        );
        assertJson(
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
    public void testFindByNameLikeIgnoreCaseAndStoreNameOrderByNameAscEditionDesc() throws JsonProcessingException {
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
                "select count(tb_1_.ID) " +
                        "from BOOK as tb_1_ " +
                        "inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                        "where lower(tb_1_.NAME) like ? " +
                        "and tb_2_.NAME = ?",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                        "from BOOK as tb_1_ " +
                        "inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                        "where lower(tb_1_.NAME) like ? " +
                        "and tb_2_.NAME = ? " +
                        "order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                        "limit ?",
                "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                        "from AUTHOR as tb_1_ " +
                        "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                        "where tb_2_.BOOK_ID in (?, ?)"
        );
        assertJson(
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
    public void testFindDistinctPriceByPriceBetween() {
        bookRepository.findDistinctPriceByPriceBetween(null, null);
        assertSQLs(
                "select distinct tb_1_.PRICE from BOOK as tb_1_"
        );
        bookRepository.findDistinctPriceByPriceBetween(new BigDecimal(40), null);
        assertSQLs(
                "select distinct tb_1_.PRICE from BOOK as tb_1_ where tb_1_.PRICE >= ?"
        );
        bookRepository.findDistinctPriceByPriceBetween(null, new BigDecimal(50));
        assertSQLs(
                "select distinct tb_1_.PRICE from BOOK as tb_1_ where tb_1_.PRICE <= ?"
        );
        bookRepository.findDistinctPriceByPriceBetween(new BigDecimal(40), new BigDecimal(50));
        assertSQLs(
                "select distinct tb_1_.PRICE from BOOK as tb_1_ where tb_1_.PRICE between ? and ?"
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
                "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_",
                "select tb_1_.ID, tb_2_.ID " +
                        "from BOOK_STORE as tb_1_ " +
                        "inner join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                        "where (tb_2_.NAME, tb_2_.EDITION) in (" +
                        "--->select tb_3_.NAME, max(tb_3_.EDITION) " +
                        "--->from BOOK as tb_3_ " +
                        "--->where tb_3_.STORE_ID in (?, ?) " +
                        "--->group by tb_3_.NAME" +
                        ")",
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                        "from BOOK as tb_1_ " +
                        "where tb_1_.ID in (?, ?, ?, ?)",
                "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                        "from AUTHOR as tb_1_ " +
                        "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                        "where tb_2_.BOOK_ID in (?, ?, ?, ?)"
        );
        assertJson(
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
    public void testDownloadTypescript() throws Exception {
        mvc.perform(get("/my-ts.zip"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/zip"));
    }

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

    private static void assertJson(String json, Object o) {
        Assertions.assertEquals(
                json
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("--->", ""),
                o.toString()
        );
    }
}
