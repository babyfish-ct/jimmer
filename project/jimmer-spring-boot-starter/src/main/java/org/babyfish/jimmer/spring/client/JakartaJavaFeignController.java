package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.generator.java.feign.JavaFeignGenerator;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@Controller
public class JakartaJavaFeignController {

    private final Metadata metadata;

    private final JimmerProperties properties;

    public JakartaJavaFeignController(Metadata metadata, JimmerProperties properties) {
        this.metadata = metadata;
        this.properties = properties;
    }

    @GetMapping("${jimmer.client.java-feign.path}")
    public void download(
            @RequestParam(name = "apiName", required = false) String apiName,
            @RequestParam(name = "indent", defaultValue = "0") int indent,
            @RequestParam(name = "basePackage", required = false) String basePackage,
            @Value("${spring.application.name:}") String applicationName,
            HttpServletResponse response
    ) throws IOException {
        JimmerProperties.Client.JavaFeign javaFeign = properties.getClient().getJavaFeign();
        response.setContentType("application/zip");
        try (OutputStream out = response.getOutputStream()) {
            new JavaFeignGenerator(
                    apiName != null && !apiName.isEmpty() ?
                            apiName :
                            !javaFeign.getApiName().isEmpty() ?
                                    javaFeign.getApiName() :
                                    !applicationName.isEmpty() ? applicationName : "api",
                    indent != 0 ? indent : javaFeign.getIndent(),
                    basePackage != null && !basePackage.isEmpty() ?
                            basePackage :
                            javaFeign.getBasePackage()
            ).generate(metadata, out);
        }
    }
}
