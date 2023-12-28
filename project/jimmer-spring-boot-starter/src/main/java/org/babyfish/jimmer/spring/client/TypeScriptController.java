package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class TypeScriptController {

    private final JimmerProperties properties;

    public TypeScriptController(JimmerProperties properties) {
        this.properties = properties;
    }

    @GetMapping("${jimmer.client.ts.path}")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam(name = "groups", required = false) String groups
    ) {
        JimmerProperties.Client.TypeScript ts = properties.getClient().getTs();
        Metadata metadata = Metadatas.create(true, groups);
        TypeScriptContext ctx = new TypeScriptContext(metadata, ts.getIndent(), ts.isMutable(), ts.getApiName(), ts.getNullRenderMode());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/zip");
        StreamingResponseBody body = ctx::renderAll;
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
