package org.babyfish.jimmer.spring.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.jackson.v2.ImmutableModuleV2;
import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.babyfish.jimmer.json.JsonCodec;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class JimmerJacksonConfig {

    @ConditionalOnClass(name = {"com.fasterxml.jackson.databind.ObjectMapper", "org.babyfish.jimmer.jackson.v2.ImmutableModuleV2"})
    @ConditionalOnMissingBean(ImmutableModuleV2.class)
    @Configuration(proxyBeanMethods = false)
    protected static class JacksonConfigV2 {
        @Bean
        public ImmutableModuleV2 immutableModuleV2() {
            return new ImmutableModuleV2();
        }

        static boolean hasMapper(ListableBeanFactory factory) {
            return factory.getBeanNamesForType(ObjectMapper.class, true, false).length != 0;
        }

        static JsonCodec codec(ListableBeanFactory factory) {
            ObjectMapper mapper = factory.getBeanProvider(ObjectMapper.class).getIfAvailable();
            if (mapper == null) {
                return new JsonCodecV2();
            }
            if (!mapper.getRegisteredModuleIds().contains(ImmutableModuleV2.MODULE_ID)) {
                mapper = mapper.copy().registerModule(new ImmutableModuleV2());
            }
            return new JsonCodecV2(mapper);
        }
    }

    @ConditionalOnClass(name = {"tools.jackson.databind.ObjectMapper", "org.babyfish.jimmer.jackson.v3.ImmutableModuleV3"})
    @ConditionalOnMissingBean(ImmutableModuleV3.class)
    @Configuration(proxyBeanMethods = false)
    protected static class JacksonConfigV3 {
        @Bean
        public ImmutableModuleV3 immutableModuleV3() {
            return new ImmutableModuleV3();
        }

        static boolean hasMapper(ListableBeanFactory factory) {
            return factory.getBeanNamesForType(JsonMapper.class, true, false).length != 0;
        }

        static JsonCodec codec(ListableBeanFactory factory) {
            JsonMapper mapper = factory.getBeanProvider(JsonMapper.class).getIfAvailable();
            if (mapper == null) {
                return new JsonCodecV3();
            }
            if (mapper.registeredModules().stream().noneMatch(module -> module instanceof ImmutableModuleV3)) {
                mapper = mapper.rebuild().addModule(new ImmutableModuleV3()).build();
            }
            return new JsonCodecV3(mapper);
        }
    }

    @ConditionalOnMissingBean(JsonCodec.class)
    @Configuration(proxyBeanMethods = false)
    protected static class JsonCodecConfig {
        @Bean
        public JsonCodec jsonCodec(ListableBeanFactory factory) {
            ClassLoader loader = JimmerJacksonConfig.class.getClassLoader();
            boolean jackson3 = ClassUtils.isPresent("tools.jackson.databind.json.JsonMapper", loader) &&
                    ClassUtils.isPresent("org.babyfish.jimmer.jackson.v3.ImmutableModuleV3", loader);
            boolean jackson2 = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", loader) &&
                    ClassUtils.isPresent("org.babyfish.jimmer.jackson.v2.ImmutableModuleV2", loader);
            if (jackson3 && (!jackson2 || JacksonConfigV3.hasMapper(factory) || !JacksonConfigV2.hasMapper(factory))) {
                return JacksonConfigV3.codec(factory);
            }
            return JacksonConfigV2.codec(factory);
        }
    }
}
