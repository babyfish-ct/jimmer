package org.babyfish.jimmer.spring.cfg;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Enaium
 */
public class ScalarUiConfig implements WebMvcConfigurer {
    private final String path;

    public ScalarUiConfig(String uiPath) {
        path = uiPath.substring(0, uiPath.lastIndexOf('/')) + "/jimmer-scalar";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(path + "/**")
                .addResourceLocations("classpath:META-INF/resources/webjars/scalar/");
    }
}
