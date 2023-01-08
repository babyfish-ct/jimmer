package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.generator.ts.TypeScriptGenerator;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@Controller
public class TypeScriptController {

    private final Metadata metadata;

    private final JimmerProperties properties;

    public TypeScriptController(Metadata metadata, JimmerProperties properties) {
        this.metadata = metadata;
        this.properties = properties;
    }

    @GetMapping("${jimmer.client.ts.path}")
    public void download(
            @RequestParam(name = "apiName", required = false) String apiName,
            @RequestParam(name = "indent", defaultValue = "0") int indent,
            @RequestParam(name = "anonymous", required = false) Boolean anonymous,
            HttpServletResponse response
    ) throws IOException {
        JimmerProperties.Client.TypeScript ts = properties.getClient().getTs();
        response.setContentType("application/zip");
        try (OutputStream out = response.getOutputStream()) {
            new TypeScriptGenerator(
                    apiName != null && !apiName.isEmpty() ? apiName : ts.getApiName(),
                    indent != 0 ? indent : ts.getIndent(),
                    anonymous != null ? anonymous : ts.isAnonymous()
            ).generate(metadata, out);
        }
    }
}
