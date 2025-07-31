package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.runtime.impl.MetadataBuilder;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
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

    private static final String CSS_RESOURCE = "META-INF/jimmer/swagger/swagger-ui.css";

    private static final String JS_RESOURCE = "META-INF/jimmer/swagger/swagger-ui.js";

    private static final String CSS_URL = "/jimmer-client/swagger-ui.css";

    private static final String JS_URL = "/jimmer-client/swagger-ui.js";

    private final JimmerProperties properties;

    @Value("${server.servlet.context-path:#{null}}")
    private String contextPath;

    public OpenApiUiController(JimmerProperties properties) {
        this.properties = properties;
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
        if (hasMetadata()) {
            resource = refPath != null && !refPath.isEmpty() ?
                    "META-INF/jimmer/openapi/index.html.template" :
                    "META-INF/jimmer/openapi/no-api.html";
        } else {
            resource = "META-INF/jimmer/openapi/no-metadata.html";
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
                .replace("${openapi.css}",
                        exists(CSS_RESOURCE) ?
                                CSS_URL :
                                "https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui.css"
                )
                .replace("${openapi.js}",
                        exists(JS_RESOURCE) ?
                                JS_URL :
                                "https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui-bundle.js"
                )
                .replace(
                        "${openapi.refPath}",
                        refPath
                );
    }

    private boolean hasMetadata() {
        Schema schema = MetadataBuilder.loadSchema(Collections.emptySet());
        for (ApiService service : schema.getApiServiceMap().values()) {
            if (!service.getOperations().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @GetMapping(CSS_URL)
    public ResponseEntity<StreamingResponseBody> css() throws IOException {
        return downloadResource(CSS_RESOURCE, "text/css");
    }

    @GetMapping(JS_URL)
    public ResponseEntity<StreamingResponseBody> js() throws IOException {
        return downloadResource(JS_RESOURCE, "text/javascript");
    }

    private ResponseEntity<StreamingResponseBody> downloadResource(String resource, String contentType) throws IOException {
        byte[] buf = new byte[4 * 1024];
        InputStream in = OpenApiController.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("The resource \"" + resource + "\" does not exist");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", contentType);
        StreamingResponseBody body = out -> {
            try {
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.flush();
            } finally {
                in.close();
            }
        };
        return ResponseEntity.ok().headers(headers).body(body);
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
