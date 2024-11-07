package org.babyfish.jimmer.spring.cfg;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = {
                SqlClientConfig.class,
                JimmerLanguageTestHelper.DataSourceConfig.class}
)
@EnableConfigurationProperties(JimmerProperties.class)
public class DefaultJimmerLanguageTest {
    @Autowired(required = false)
    @Qualifier("sqlClient")
    protected Object sqlClient;

    @Test
    public void javaLanguage(@Autowired JimmerProperties jimmerProperties) {
        JimmerLanguageTestHelper.javaLanguage(sqlClient);
        Assertions.assertEquals("java", jimmerProperties.getLanguage());
    }
}
