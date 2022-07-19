package org.babyfish.jimmer.benchmark.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EntityScan(basePackageClasses = JpaData.class)
public class JpaConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean hibernateEntityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean bean =
                new LocalContainerEntityManagerFactoryBean();
        bean.setDataSource(dataSource);
        bean.setPackagesToScan(JpaConfig.class.getPackage().getName());
        bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return bean;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean eclipseLinkEntityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean bean =
                new LocalContainerEntityManagerFactoryBean();
        bean.setDataSource(dataSource);
        bean.setPackagesToScan(JpaConfig.class.getPackage().getName());
        bean.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        Properties props = new Properties();
        props.setProperty("eclipselink.weaving", "false");
        bean.setJpaProperties(props);
        return bean;
    }
}
