package org.babyfish.jimmer.client.generator.java.feign;

import org.babyfish.jimmer.client.generator.java.JavaContext;
import org.babyfish.jimmer.client.meta.Metadata;

import java.io.OutputStream;

public class FeignContext extends JavaContext {

    public FeignContext(Metadata metadata, OutputStream out, String moduleName, int indent, String basePackage) {
        super(metadata, out, moduleName, indent, basePackage, false);
    }
}
