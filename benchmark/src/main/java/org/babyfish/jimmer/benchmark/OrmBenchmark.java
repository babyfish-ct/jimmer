package org.babyfish.jimmer.benchmark;

import org.babyfish.jimmer.benchmark.exposed.ExposedDataTable;
import org.babyfish.jimmer.benchmark.jdbc.JdbcDataRepository;
import org.babyfish.jimmer.benchmark.jimmer.JimmerDataTable;
import org.babyfish.jimmer.benchmark.jooq.JooqData;
import org.babyfish.jimmer.benchmark.jooq.JooqDataTable;
import org.babyfish.jimmer.benchmark.ktorm.KtormDataTable;
import org.babyfish.jimmer.benchmark.mybatis.MyBatisDataMapper;
import org.babyfish.jimmer.benchmark.objsql.ObjSqlData;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.jetbrains.exposed.spring.SpringTransactionManager;
import org.jooq.DSLContext;
import org.ktorm.database.Database;
import org.ktorm.entity.EntitySequenceKt;
import org.openjdk.jmh.annotations.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.sql.SQLException;

@State(Scope.Benchmark)
public class OrmBenchmark {

    @Param({"1", "10", "20", "50", "100", "200", "500", "1000"})
    private int dataCount;

    private SqlClient sqlClient;

    private MyBatisDataMapper myBatisDataMapper;

    private EntityManagerFactory entityManagerFactory;

    private DSLContext dslContext;

    private JdbcDataRepository jdbcDataRepository;

    private SpringTransactionManager transactionManager;

    private Database database;

    @Setup
    public void initialize() throws SQLException, IOException {
        ApplicationContext ctx = SpringApplication.run(BenchmarkApplication.class);
        DatabaseInitializer databaseInitializer = ctx.getBean(DatabaseInitializer.class);
        databaseInitializer.initialize(dataCount);
        sqlClient = ctx.getBean(SqlClient.class);
        myBatisDataMapper = ctx.getBean(MyBatisDataMapper.class);
        entityManagerFactory = ctx.getBean(EntityManagerFactory.class);
        dslContext = ctx.getBean(DSLContext.class);
        jdbcDataRepository = ctx.getBean(JdbcDataRepository.class);
        transactionManager = ctx.getBean(SpringTransactionManager.class);
        database = ctx.getBean(Database.class);
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
    public void runJpa() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.createQuery("from JpaData").getResultList();
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
        jdbcDataRepository.findAll();
    }

    @Benchmark
    public void runExposed() {
        TransactionStatus ts = transactionManager.getTransaction(new DefaultTransactionDefinition());
        ExposedDataTable.INSTANCE.list();
        transactionManager.commit(ts);
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
}
