package org.babyfish.jimmer.spring.cfg;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OpenApiUiResourceTest {

    @ParameterizedTest
    @ValueSource(strings = {"/openapi.html", "/docs/openapi.html", "/docs/v1/openapi.html", "/", "/docs/"})
    public void testApplicationAndUiResources(String uiPath) throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "test.ui-path=" + uiPath);
            context.register(ResourceConfiguration.class);
            context.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            for (String contextPath : new String[] {"", "/app"}) {
                mvc.perform(get(contextPath + "/index.css").contextPath(contextPath))
                        .andExpect(status().isOk())
                        .andExpect(content().string("/* Application stylesheet */\n"));

                Enumeration<URL> templates = getClass().getClassLoader()
                        .getResources("META-INF/jimmer/openapi/index.html.template");
                int templateCount = 0;
                while (templates.hasMoreElements()) {
                    String template;
                    try (InputStream input = templates.nextElement().openStream()) {
                        template = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
                    }
                    Matcher matcher = Pattern.compile("(?:src|href)=\"([^\"]+)\"").matcher(template);
                    int resourceCount = 0;
                    while (matcher.find()) {
                        URI uri = URI.create(contextPath + uiPath).resolve(matcher.group(1));
                        mvc.perform(get(uri).contextPath(contextPath)).andExpect(status().isOk());
                        resourceCount++;
                    }
                    Assertions.assertTrue(resourceCount > 0);
                    templateCount++;
                }
                Assertions.assertEquals(2, templateCount, "Both Swagger and Scalar templates must be tested");
            }
        }
    }

    @Configuration
    @EnableWebMvc
    static class ResourceConfiguration implements WebMvcConfigurer {

        @Value("${test.ui-path}")
        private String uiPath;

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
            new SwaggerUiConfig(uiPath, null).addResourceHandlers(registry);
            new ScalarUiConfig(uiPath).addResourceHandlers(registry);
        }
    }
}
