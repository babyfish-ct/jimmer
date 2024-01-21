package org.babyfish.jimmer.quarkus.deployment;

import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;
import org.babyfish.jimmer.quarkus.runtime.Jimmer;
import org.babyfish.jimmer.quarkus.runtime.JimmerDataSourcesRecord;
import org.babyfish.jimmer.sql.JSqlClient;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import java.util.List;

public class JimmerProcessor {

    private static final String FEATURE = "jimmer";

    private static final DotName J_SQL_CLIENT = DotName.createSimple(org.babyfish.jimmer.sql.JSqlClient.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public SyntheticBeanBuildItem addDefaultCacheBean(JimmerDataSourcesRecord record) {
        return SyntheticBeanBuildItem.configure(Jimmer.class)
                .unremovable()
                .supplier(record.setupJSqlClientDataSourcesSupplier())
                .scope(Singleton.class)
                .setRuntimeInit()
                .done();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateJSqlClientBeans(JimmerDataSourcesRecord record,
                                 BuildProducer<AdditionalBeanBuildItem> additionalBeans,
                                 BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
                                 List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {
        if (jdbcDataSourceBuildItems.isEmpty()) {
            return;
        }

        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(JSqlClient.class).setUnremovable()
                .setDefaultScope(DotNames.SINGLETON).build());
        for (JdbcDataSourceBuildItem jdbcDataSourceBuildItem : jdbcDataSourceBuildItems) {
            String dataSourceName = jdbcDataSourceBuildItem.getName();

            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(JSqlClient.class)
                    .addType(J_SQL_CLIENT)
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .addInjectionPoint(ClassType.create(DotName.createSimple(DataSources.class)))
                    .createWith(record.JSqlClientFunction(dataSourceName, jdbcDataSourceBuildItem.getDbKind()));

            if (jdbcDataSourceBuildItem.isDefault()) {
                configurator.addQualifier(Default.class);
            } else {
                String beanName = FEATURE + dataSourceName;
                configurator.name(beanName);
                configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", dataSourceName).done();
                configurator.addQualifier().annotation(DataSource.class).addValue("value", dataSourceName).done();
            }
            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }
    }
}
