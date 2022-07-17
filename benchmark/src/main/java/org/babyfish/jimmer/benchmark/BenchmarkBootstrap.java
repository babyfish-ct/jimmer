package org.babyfish.jimmer.benchmark;

import org.babyfish.jimmer.benchmark.jdbc.JdbcDataRepository;
import org.babyfish.jimmer.benchmark.jimmer.JimmerDataTable;
import org.babyfish.jimmer.benchmark.jooq.JooqData;
import org.babyfish.jimmer.benchmark.jooq.JooqDataTable;
import org.babyfish.jimmer.benchmark.mybatis.MyBatisDataMapper;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.jooq.DSLContext;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.sql.SQLException;

@State(Scope.Benchmark)
public class BenchmarkBootstrap {

    @Param({"1", "10", "20", "50", "100", "200", "500", "1000"})
    private int dataCount;

    private ApplicationContext ctx;

    @Setup
    public void initialize() throws SQLException, IOException {
        ctx = SpringApplication.run(BenchmarkApplication.class);
        DatabaseInitializer databaseInitializer = ctx.getBean(DatabaseInitializer.class);
        databaseInitializer.initialize(dataCount);
    }

    @Benchmark
    public void runJimmer() {
        SqlClient sqlClient = ctx.getBean(SqlClient.class);
        sqlClient
                .createQuery(JimmerDataTable.class, RootSelectable::select)
                .execute();
    }

    @Benchmark
    public void runMybatis() {
        MyBatisDataMapper myBatisDataMapper = ctx.getBean(MyBatisDataMapper.class);
        myBatisDataMapper.findAll();
    }

    @Benchmark
    public void runJpa() {
        EntityManagerFactory emf = ctx.getBean(EntityManagerFactory.class);
        EntityManager em = emf.createEntityManager();
        try {
            em.createQuery("from JpaData").getResultList();
        } finally {
            em.close();
        }
    }

    @Benchmark
    public void runJooq() {
        DSLContext dslContext = ctx.getBean(DSLContext.class);
        dslContext.selectFrom(JooqDataTable.DATA).getQuery().fetchInto(JooqData.class);
    }

    @Benchmark
    public void runSpringDataJdbc() {
        JdbcDataRepository repository = ctx.getBean(JdbcDataRepository.class);
        repository.findAll();
    }
}
