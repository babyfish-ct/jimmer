package org.babyfish.jimmer.spring.java;

import org.babyfish.jimmer.spring.AbstractTest;
import org.babyfish.jimmer.spring.java.bll.BookService;
import org.babyfish.jimmer.spring.client.TypeScriptService;
import org.babyfish.jimmer.spring.java.dal.BookRepository;
import org.babyfish.jimmer.spring.datasource.DataSources;
import org.babyfish.jimmer.spring.datasource.TxCallback;
import org.babyfish.jimmer.spring.java.model.Book;
import org.babyfish.jimmer.spring.java.model.BookProps;
import org.babyfish.jimmer.spring.java.model.JimmerModule;
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories;
import org.babyfish.jimmer.spring.repository.Sorts;
import org.babyfish.jimmer.spring.repository.SpringConnectionManager;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringBootTest
@SpringBootConfiguration
@AutoConfigurationPackage
@EnableJimmerRepositories
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
        public JSqlClient sqlClient(DataSource dataSource) {
            return JSqlClient
                    .newBuilder()
                    .setConnectionManager(new SpringConnectionManager(dataSource))
                    .setEntityManager(JimmerModule.ENTITY_MANAGER)
                    .setExecutor(
                            new Executor() {
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
                            }
                    )
                    .build();
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

    @Test
    public void testDownloadTypescript() throws Exception {
        mvc.perform(get("/ts.zip"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/zip"));
    }
}
