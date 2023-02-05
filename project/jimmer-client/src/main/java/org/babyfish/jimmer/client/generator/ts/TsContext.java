package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.meta.Metadata;

import java.io.OutputStream;

public class TsContext extends Context {

    private final boolean anonymous;

    public TsContext(Metadata metadata, OutputStream out, String moduleName, int indent, boolean anonymous) {
        super(metadata, out, moduleName, indent);
        this.anonymous = anonymous;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    protected String nestedTypeSeparator() {
        return "_";
    }
}
