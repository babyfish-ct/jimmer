package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

public class TypeScriptContext extends Context {

    private final boolean isMutable;

    public TypeScriptContext(Metadata metadata, String indent, boolean isMutable) {
        super(metadata, indent);
        this.isMutable = isMutable;
    }

    public boolean isMutable() {
        return isMutable;
    }

    @Override
    protected boolean determineGenericSupported() {
        return true;
    }

    @Override
    protected SourceManager createSourceManager(Metadata metadata, boolean isGenericSupported) {
        return new TypeScriptSourceManager(metadata, isGenericSupported);
    }

    @Override
    protected CodeWriter createCodeWriter(Context context, Source source) {
        return new TypeScriptWriter(context, source);
    }
}
