package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

public class TypeScriptContext extends Context {

    private final boolean isMutable;

    private final String apiName;

    private final NullRenderMode nullRenderMode;

    public TypeScriptContext(Metadata metadata) {
        this(metadata, 4, false, null, NullRenderMode.UNDEFINED);
    }

    public TypeScriptContext(Metadata metadata, int indent, boolean isMutable, String apiName, NullRenderMode nullRenderMode) {
        super(metadata, indent(indent));
        if (!metadata.isGenericSupported()) {
            throw new IllegalArgumentException(
                    "TypeScriptContext only accept metadata which support generic"
            );
        }
        this.isMutable = isMutable;
        this.apiName = apiName != null && !apiName.isEmpty() ? apiName : "Api";
        this.nullRenderMode = nullRenderMode != null ? nullRenderMode : NullRenderMode.UNDEFINED;
    }

    public boolean isMutable() {
        return isMutable;
    }

    public NullRenderMode getNullRenderMode() {
        return nullRenderMode;
    }

    public String getApiName() {
        return apiName;
    }

    @Override
    protected SourceManager createSourceManager() {
        return new TypeScriptSourceManager(this);
    }

    @Override
    protected SourceWriter createCodeWriter(Context context, Source source) {
        return new TypeScriptWriter(context, source);
    }

    @Override
    protected boolean isIndexRequired() {
        return true;
    }

    @Override
    protected String getFileExtension() {
        return "ts";
    }

    private static String indent(int indent) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = indent; i > 0; --i) {
            indentBuilder.append(' ');
        }
        return indentBuilder.toString();
    }
}
