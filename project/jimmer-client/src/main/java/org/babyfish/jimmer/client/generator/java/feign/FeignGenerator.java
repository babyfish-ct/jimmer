package org.babyfish.jimmer.client.generator.java.feign;

import org.babyfish.jimmer.client.generator.Generator;
import org.babyfish.jimmer.client.meta.Metadata;

import java.io.OutputStream;

public class FeignGenerator implements Generator {

    private final String indent;

    public FeignGenerator() {
        this.indent = "    ";
    }

    public FeignGenerator(String indent) {
        this.indent = indent;
    }

    @Override
    public void generate(Metadata metadata, OutputStream out) {

    }
}
