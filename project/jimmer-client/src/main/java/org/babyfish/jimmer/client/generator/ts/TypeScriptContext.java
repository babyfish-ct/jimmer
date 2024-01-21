package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.runtime.EnumType;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

import java.io.Writer;
import java.util.List;

public class TypeScriptContext extends Context {

    private final boolean isMutable;

    private final String apiName;

    private final NullRenderMode nullRenderMode;

    private final boolean isEnumTsStyle;

    public TypeScriptContext(Metadata metadata) {
        this(metadata, 4, false, null, NullRenderMode.UNDEFINED, false);
    }

    public TypeScriptContext(
            Metadata metadata,
            int indent,
            boolean isMutable,
            String apiName,
            NullRenderMode nullRenderMode,
            boolean isEnumTsStyle
    ) {
        super(metadata, indent(indent));
        if (!metadata.isGenericSupported()) {
            throw new IllegalArgumentException(
                    "TypeScriptContext only accept metadata which support generic"
            );
        }
        this.isMutable = isMutable;
        this.apiName = apiName != null && !apiName.isEmpty() ? apiName : "Api";
        this.nullRenderMode = nullRenderMode != null ? nullRenderMode : NullRenderMode.UNDEFINED;
        this.isEnumTsStyle = isEnumTsStyle;
    }

    public boolean isMutable() {
        return isMutable;
    }

    public String getApiName() {
        return apiName;
    }

    public NullRenderMode getNullRenderMode() {
        return nullRenderMode;
    }

    public boolean isEnumTsStyle() {
        return isEnumTsStyle;
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
    protected void renderIndexCode(String dir, List<Source> sources, Writer writer) {
        if (!dir.equals(TypeScriptSourceManager.ENUM_DIR)) {
            return;
        }
        EnumConstantWriter ecw = new EnumConstantWriter(writer, getIndent());
        for (Source source : sources) {
            if (!(source.getRender() instanceof EnumTypeRender)) {
                continue;
            }
            ecw.code("import { ").code(source.getName()).code("_CONSTANTS")
                    .codeIf(isEnumTsStyle, ", " + source.getName() + "_CONSTANT_MAP")
                    .code(" } from './")
                    .code(source.getName()).code("';\n");
        }
        ecw.code("export const ALL_ENUM_CONSTANTS = ");
        ecw.scope(CodeWriter.ScopeType.OBJECT, ", ", true, () -> {
            for (Source source : sources) {
                if (!(source.getRender() instanceof EnumTypeRender)) {
                    continue;
                }
                ecw.separator();
                ecw.code('"').code(source.getName()).code("\": ").code(source.getName()).code("_CONSTANTS");
            }
        });
        ecw.code('\n');
        if (isEnumTsStyle) {
            ecw.code("export const ALL_ENUM_CONSTANT_MAPS = ");
            ecw.scope(CodeWriter.ScopeType.OBJECT, ", ", true, () -> {
                for (Source source : sources) {
                    if (!(source.getRender() instanceof EnumTypeRender)) {
                        continue;
                    }
                    ecw.separator();
                    ecw.code('"').code(source.getName()).code("\": ").code(source.getName()).code("_CONSTANT_MAP");
                }
            });
            ecw.code('\n');
        }
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

    public static Builder newBuilder(Metadata metadata) {
        return new Builder(metadata);
    }

    public static class Builder {

        private final Metadata metadata;

        private int indent;

        private boolean isMutable;

        private String apiName;

        private NullRenderMode nullRenderMode;

        private boolean isEnumTsStyle;

        public Builder(Metadata metadata) {
            this.metadata = metadata;
        }

        public Builder setMutable(boolean mutable) {
            isMutable = mutable;
            return this;
        }

        public Builder setIndent(int indent) {
            this.indent = indent;
            return this;
        }

        public Builder setApiName(String apiName) {
            this.apiName = apiName;
            return this;
        }

        public Builder setNullRenderMode(NullRenderMode nullRenderMode) {
            this.nullRenderMode = nullRenderMode;
            return this;
        }

        public Builder setEnumTsStyle(boolean enumTsStyle) {
            isEnumTsStyle = enumTsStyle;
            return this;
        }

        public TypeScriptContext build() {
            return new TypeScriptContext(
                    metadata,
                    indent,
                    isMutable,
                    apiName,
                    nullRenderMode,
                    isEnumTsStyle
            );
        }
    }

    private static class EnumConstantWriter extends CodeWriter<EnumConstantWriter> {

        public EnumConstantWriter(Writer writer, String indent) {
            super(indent);
            setWriter(writer);
        }
    }
}
