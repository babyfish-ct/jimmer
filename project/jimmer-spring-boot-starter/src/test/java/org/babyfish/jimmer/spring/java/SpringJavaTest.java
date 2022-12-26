package org.babyfish.jimmer.spring.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babyfish.jimmer.spring.AbstractTest;
import org.babyfish.jimmer.spring.cfg.JimmerAutoConfiguration;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.babyfish.jimmer.spring.cfg.SqlClientConfig;
import org.babyfish.jimmer.spring.java.bll.BookService;
import org.babyfish.jimmer.spring.client.TypeScriptService;
import org.babyfish.jimmer.spring.java.dal.BookRepository;
import org.babyfish.jimmer.spring.datasource.DataSources;
import org.babyfish.jimmer.spring.datasource.TxCallback;
import org.babyfish.jimmer.spring.java.model.*;
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories;
import org.babyfish.jimmer.spring.repository.Sorts;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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

@SpringBootTest(properties = {"jimmer.client.ts.path=/my-ts.zip", "jimmer.dialect=org.babyfish.jimmer.sql.dialect.H2Dialect"})
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
        public TypeScriptService typeScriptService(ApplicationContext ctx) {
            return new TypeScriptService(ctx);
        }

        @Bean
        public MockMvc mockMvc(WebApplicationContext ctx) {
            return webAppContextSetup(ctx).build();
        }
    }

    @Autowired
    private BookRepository bookRepository;

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

        Sort sort = Sorts.toSort(BookProps.NAME.desc());

        assertTransactionEvents();
        Assertions.assertEquals(12, bookRepository.findAll(sort).size());
        assertSQLs(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                        "from BOOK as tb_1_ " +
                        "order by tb_1_.NAME desc"
        );
        assertTransactionEvents("connect");

        assertTransactionEvents();
        Page<Book> page = bookRepository.findAll(0, 10, sort);
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
    public void testBySpringPageable() {

        Pageable pageable = PageRequest.of(0, 10, Sorts.toSort(BookProps.NAME.desc()));

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
    public void testDownloadTypescript() throws Exception {
        mvc.perform(get("/my-ts.zip"))
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
                Assertions.assertEquals(statements[i], SQL_STATEMENTS.get(i), "sql[" + i + ']');
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
