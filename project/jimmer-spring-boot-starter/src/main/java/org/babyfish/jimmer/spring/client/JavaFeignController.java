package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class JavaFeignController {

    private final JimmerProperties properties;

    public JavaFeignController(JimmerProperties properties) {
        this.properties = properties;
    }

    @GetMapping("${jimmer.client.java-feign.path}")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam(name = "apiName", required = false) String apiName,
            @RequestParam(name = "indent", defaultValue = "0") int indent,
            @RequestParam(name = "basePackage", required = false) String basePackage,
            @Value("${spring.application.name:}") String applicationName
    ) {
        throw new UnsupportedOperationException();
    }
}
