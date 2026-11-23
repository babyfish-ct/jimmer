package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.runtime.impl.MetadataBuilder;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;

@Controller
public class OpenApiUiController {

    private static final String VIEW_TEMPLATE = "META-INF/jimmer/openapi/index.html.template";
    private static final String NO_UI = "META-INF/jimmer/openapi/no-ui.html";
    private static final String NO_API = "META-INF/jimmer/openapi/no-api.html";
    private static final String NO_METADATA = "META-INF/jimmer/openapi/no-metadata.html";

    private final JimmerProperties properties;

    private final JsonCodec jsonCodec;

    @Value("${server.servlet.context-path:#{null}}")
    private String contextPath;

    public OpenApiUiController(JimmerProperties properties) {
        this(properties, JsonCodec.jsonCodec());
    }

    @Autowired
    public OpenApiUiController(JimmerProperties properties, JsonCodec jsonCodec) {
        this.properties = properties;
        this.jsonCodec = jsonCodec;
    }

    @GetMapping("${jimmer.client.openapi.ui-path}")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam(name = "groups", required = false) String groups
    ) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/html");
        StreamingResponseBody body = out -> {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            String html = this.html(groups);
            writer.write(html);
            writer.flush();
        };
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private String html(String groups) {
        String refPath = properties.getClient().getOpenapi().getRefPath();
        if (contextPath != null) {
            refPath = contextPath + refPath;
        }
        String resource;
        if (!exists(VIEW_TEMPLATE)) {
            resource = NO_UI;
        } else if (hasMetadata()) {
            resource = refPath != null && !refPath.isEmpty() ? VIEW_TEMPLATE : NO_API;
        } else {
            resource = NO_METADATA;
        }
        StringBuilder builder = new StringBuilder();
        char[] buf = new char[1024];
        InputStream inputStream = OpenApiController.class.getClassLoader().getResourceAsStream(resource);
        assert inputStream != null;
        try (Reader reader = new InputStreamReader(inputStream)) {
            int len;
            while ((len = reader.read(buf)) != -1) {
                builder.append(buf, 0, len);
            }
        } catch (IOException ex) {
            throw new AssertionError("Internal bug: Can read \"" + resource + "\"");
        }
        boolean isTemplate = resource.endsWith(".template");
        if (!isTemplate) {
            return builder.toString().replace("\r\n", "\n");  // Normalize line endings to LF
        }
        if (groups != null && !groups.isEmpty()) {
            try {
                refPath += "?groups=" + URLEncoder.encode(groups, "utf-8");
            } catch (UnsupportedEncodingException ex) {
                throw new AssertionError("Internal bug: utf-8 is not supported");
            }
        }
        return builder
                .toString()
                .replace("\r\n", "\n")  // Normalize line endings to LF
                .replace(
                        "${openapi.refPath}",
                        refPath
                );
    }

    private boolean hasMetadata() {
        Schema schema = MetadataBuilder.loadSchema(Collections.emptySet(), jsonCodec);
        for (ApiService service : schema.getApiServiceMap().values()) {
            if (!service.getOperations().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean exists(String resource) {
        Enumeration<URL> enumeration;
        try {
            enumeration = OpenApiController.class.getClassLoader().getResources(resource);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to check the existence of resource \"" + resource + "\"");
        }
        return enumeration.hasMoreElements();
    }
}
