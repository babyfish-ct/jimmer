package org.babyfish.jimmer.spring.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.jackson.codec.JacksonVersion;
import org.babyfish.jimmer.jackson.codec.JsonCodec;
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
        public JsonCodec<?> jsonCodec(BeanFactory beanFactory) {
            JsonCodec<?> jsonCodec = JsonCodec.jsonCodec();
            if (jsonCodec.version() == JacksonVersion.V2) {
                ObjectMapper mapper = beanFactory.getBeanProvider(ObjectMapper.class).getIfAvailable();
                if (mapper != null) {
                    return new JsonCodecV2(mapper);
                }
            } else {
                JsonMapper mapper = beanFactory.getBeanProvider(JsonMapper.class).getIfAvailable();
                if (mapper != null) {
                    return new JsonCodecV3(mapper);
                }
            }
            return jsonCodec;
        }
    }
}
