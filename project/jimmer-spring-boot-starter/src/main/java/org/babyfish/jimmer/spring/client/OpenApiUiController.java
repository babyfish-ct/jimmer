package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class OpenApiUiController {

    private final JimmerProperties properties;

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
            String html = html(groups);
            writer.write(html);
            writer.flush();
        };
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private String html(String groups) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buf = new char[1024];
        try (Reader reader = new InputStreamReader(OpenApiController.class.getClassLoader().getResourceAsStream("META-INF/jimmer/openapi/index.html.template"))) {
            int len;
            if ((len = reader.read(buf)) != -1) {
                builder.append(buf, 0, len);
            }
        }
        String path = properties.getClient().getOpenapi().getPath();
        if (groups != null && !groups.isEmpty()) {
            path += "?groups=" + URLEncoder.encode(groups, "utf-8");
        }
        return builder.toString().replace("${openapi.path}", path);
    }
}
