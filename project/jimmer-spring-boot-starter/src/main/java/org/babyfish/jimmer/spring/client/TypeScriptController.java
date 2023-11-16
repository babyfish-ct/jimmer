package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.generator.ts.TypeScriptGenerator;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class TypeScriptController {

    private final Metadata metadata;

    private final JimmerProperties properties;

    public TypeScriptController(Metadata metadata, JimmerProperties properties) {
        this.metadata = metadata;
        this.properties = properties;
    }

    @GetMapping("${jimmer.client.ts.path}")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam(name = "apiName", required = false) String apiName,
            @RequestParam(name = "indent", defaultValue = "0") int indent,
            @RequestParam(name = "anonymous", required = false) Boolean anonymous
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/zip");
        StreamingResponseBody body = out -> {
            JimmerProperties.Client.TypeScript ts = properties.getClient().getTs();
            new TypeScriptGenerator(
                    apiName != null && !apiName.isEmpty() ? apiName : ts.getApiName(),
                    indent != 0 ? indent : ts.getIndent(),
                    anonymous != null ? anonymous : ts.isAnonymous(),
                    properties.getClient().getTs().isMutable()
            ).generate(metadata, out);
        };
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
