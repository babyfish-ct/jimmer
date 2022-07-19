package org.babyfish.jimmer.benchmark;

import org.babyfish.jimmer.benchmark.exposed.ExposedDataTable;
import org.babyfish.jimmer.benchmark.exposed.ExposedJavaHelperKt;
import org.babyfish.jimmer.benchmark.jooq.JooqData;
import org.babyfish.jimmer.benchmark.jooq.JooqDataTable;
import org.babyfish.jimmer.benchmark.ktorm.KtormDataTable;
import org.babyfish.jimmer.benchmark.nutz.NutzData;
import org.babyfish.jimmer.benchmark.objsql.FakeObjSqlLoggerFactory;
import org.babyfish.jimmer.benchmark.objsql.ObjSqlData;
import org.babyfish.jimmer.benchmark.springjdbc.SpringJdbcDataRepository;
import org.babyfish.jimmer.benchmark.jimmer.JimmerDataTable;
import org.babyfish.jimmer.benchmark.mybatis.MyBatisDataMapper;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.jooq.DSLContext;
import org.ktorm.database.Database;
import org.ktorm.entity.EntitySequenceKt;
import org.nutz.dao.impl.NutDao;
import org.openjdk.jmh.annotations.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@State(Scope.Benchmark)
public class OrmBenchmark {

    private static final TransactionDefinition TRANSACTION_DEFINITION =
            new DefaultTransactionDefinition();

    @Param({"10", "20", "50", "100", "200", "500", "1000"})
    private int dataCount;

    private SqlClient sqlClient;

    private MyBatisDataMapper myBatisDataMapper;

    private EntityManagerFactory hibernateEntityManagerFactory;

    private EntityManagerFactory eclipseLinkEntityManagerFactory;

    private DSLContext dslContext;

    private SpringJdbcDataRepository springJdbcDataRepository;

    private Database database;

    private NutDao nutDao;

    @Setup
    public void initialize() throws SQLException, IOException {
        ApplicationContext ctx = SpringApplication.run(BenchmarkApplication.class);
        DatabaseInitializer databaseInitializer = ctx.getBean(DatabaseInitializer.class);
        databaseInitializer.initialize(dataCount);

        sqlClient = ctx.getBean(SqlClient.class);
        myBatisDataMapper = ctx.getBean(MyBatisDataMapper.class);
        hibernateEntityManagerFactory = ctx.getBean("hibernateEntityManagerFactory", EntityManagerFactory.class);
        eclipseLinkEntityManagerFactory = ctx.getBean("eclipseLinkEntityManagerFactory", EntityManagerFactory.class);
        dslContext = ctx.getBean(DSLContext.class);
        springJdbcDataRepository = ctx.getBean(SpringJdbcDataRepository.class);
        database = ctx.getBean(Database.class);

        // For Exposed and Nutz
        TransactionAwareDataSourceProxy transactionAwareDataSource = new TransactionAwareDataSourceProxy(ctx.getBean(DataSource.class));
        ExposedJavaHelperKt.connect(transactionAwareDataSource);
        nutDao = new NutDao(transactionAwareDataSource);

        FakeObjSqlLoggerFactory.init();
    }

    @Benchmark
    public void runJimmer() {
        sqlClient
                .createQuery(JimmerDataTable.class, RootSelectable::select)
                .execute();
    }

    @Benchmark
    public void runMyBatis() {
        myBatisDataMapper.findAll();
    }

    @Benchmark
    public void runJpaByHibernate() {
        EntityManager em = hibernateEntityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("from JpaData").getResultList();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @Benchmark
    public void runJpaByEclipseLink() {
        EntityManager em = eclipseLinkEntityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("select data from JpaData data").getResultList();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @Benchmark
    public void runJooq() {
        dslContext.selectFrom(JooqDataTable.DATA).getQuery().fetchInto(JooqData.class);
    }

    @Benchmark
    public void runSpringDataJdbc() {
        springJdbcDataRepository.findAll();
    }

    @Benchmark
    public void runExposed() {
        ExposedJavaHelperKt.executeJavaRunnable(ExposedDataTable.INSTANCE::list);
    }

    @Benchmark
    public void runKtorm() {
        EntitySequenceKt.toList(
                EntitySequenceKt.sequenceOf(database, KtormDataTable.INSTANCE, true)
        );
    }

    @Benchmark
    public void runObjectiveSql() throws SQLException {
        ObjSqlData.queryAll();
    }

    @Benchmark
    public void runNutz() {
        nutDao.query(NutzData.class, null);
    }
}
