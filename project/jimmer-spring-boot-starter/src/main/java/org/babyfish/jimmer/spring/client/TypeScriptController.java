package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class TypeScriptController {

    private final JimmerProperties properties;

    private final JsonCodec jsonCodec;

    public TypeScriptController(JimmerProperties properties) {
        this(properties, JsonCodec.jsonCodec());
    }

    @Autowired
    public TypeScriptController(JimmerProperties properties, JsonCodec jsonCodec) {
        this.properties = properties;
        this.jsonCodec = jsonCodec;
    }

    @GetMapping("${jimmer.client.ts.path}")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam(name = "groups", required = false) String groups
    ) {
        JimmerProperties.Client.TypeScript ts = properties.getClient().getTs();
        Metadata metadata = Metadatas.create(
                true,
                groups,
                properties.getClient().getUriPrefix(),
                jsonCodec
        );
        TypeScriptContext ctx = new TypeScriptContext(
                metadata,
                ts.getIndent(),
                ts.isMutable(),
                ts.getApiName(),
                ts.getNullRenderMode(),
                ts.isEnumTsStyle()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/zip");
        StreamingResponseBody body = ctx::renderAll;
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
