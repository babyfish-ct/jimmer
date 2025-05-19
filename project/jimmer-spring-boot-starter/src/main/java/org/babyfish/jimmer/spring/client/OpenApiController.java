package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.generator.openapi.OpenApiGenerator;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

@Controller
public class OpenApiController {

    private final JimmerProperties properties;

    public OpenApiController(JimmerProperties properties) {
        this.properties = properties;
    }

    @GetMapping("${jimmer.client.openapi.path}")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam(name = "groups", required = false) String groups
    ) {
        Metadata metadata = Metadatas.create(
                false,
                groups,
                properties.getClient().getUriPrefix()
        );
        OpenApiGenerator generator = new OpenApiGenerator(metadata, properties.getClient().getOpenapi().getProperties()) {
            @Override
            protected int errorHttpStatus() {
                return properties.getErrorTranslator().getHttpStatus();
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/yml");
        StreamingResponseBody body = out -> {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            generator.generate(writer);
            writer.flush();
        };
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
