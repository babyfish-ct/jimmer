package org.babyfish.jimmer.client.generator.openapi;

import org.babyfish.jimmer.client.generator.CodeWriter;

import java.io.Writer;

public class YmlWriter extends CodeWriter<YmlWriter> {

    public YmlWriter(Writer writer) {
        super("  ");
        setWriter(writer);
    }

    public YmlWriter object(String name, Runnable block) {
        code(name).code(':');
        return scope(ScopeType.BLANK, "", true, block);
    }

    public YmlWriter list(String name, Runnable block) {
        code(name).code(':');
        scope(ScopeType.BLANK, "", true, block);
        return this;
    }

    public YmlWriter listItem(Runnable block) {
        code("- ");
        scope(ScopeType.BLANK, "", false, block);
        return this;
    }

    public YmlWriter prop(String name, String value) {
        code(name).code(": ").code(value).code('\n');
        return this;
    }
}
