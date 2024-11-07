package org.babyfish.jimmer.spring.cfg;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = {SqlClientConfig.class, JimmerLanguageTestHelper.DataSourceConfig.class},
        properties = {"jimmer.language=java"}
)
@EnableConfigurationProperties(JimmerProperties.class)
public class LowerJavaJimmerLanguageTest {
    @Value("${jimmer.language}")
    private String jimmerLanguage;

    @Autowired(required = false)
    @Qualifier("sqlClient")
    protected Object sqlClient;

    @Test
    public void javaLanguage(@Autowired JimmerProperties jimmerProperties) {
        JimmerLanguageTestHelper.javaLanguage(sqlClient);
        Assertions.assertEquals("java", jimmerLanguage);
        Assertions.assertEquals("java", jimmerProperties.getLanguage());
    }
}
