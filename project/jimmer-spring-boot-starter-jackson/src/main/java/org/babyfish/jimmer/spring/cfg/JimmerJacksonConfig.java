package org.babyfish.jimmer.spring.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.json.codec.JsonCodec;
import org.babyfish.jimmer.jackson.v2.ImmutableModuleV2;
import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class JimmerJacksonConfig {

    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    @ConditionalOnMissingBean(ImmutableModuleV2.class)
    @Configuration(proxyBeanMethods = false)
    protected static class JacksonConfigV2 {
        @Bean
        public ImmutableModuleV2 immutableModuleV2() {
            return new ImmutableModuleV2();
        }
    }

    @ConditionalOnClass(name = "tools.jackson.databind.ObjectMapper")
    @ConditionalOnMissingBean(ImmutableModuleV3.class)
    @Configuration(proxyBeanMethods = false)
    protected static class JacksonConfigV3 {
        @Bean
        public ImmutableModuleV3 immutableModuleV3() {
            return new ImmutableModuleV3();
        }
    }

    @ConditionalOnMissingBean(JsonCodec.class)
    @Configuration(proxyBeanMethods = false)
    protected static class JsonCodecConfig {
        @Bean
        public JsonCodec jsonCodec(BeanFactory beanFactory) {
            ObjectMapper objectMapper = beanFactory.getBeanProvider(ObjectMapper.class).getIfAvailable();
            if (objectMapper != null) {
                return new JsonCodecV2(objectMapper);
            }
            JsonMapper jsonMapper = beanFactory.getBeanProvider(JsonMapper.class).getIfAvailable();
            if (jsonMapper != null) {
                return new JsonCodecV3(jsonMapper);
            }
            return JsonCodec.jsonCodec();
        }
    }
}
