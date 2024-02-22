package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

public class JavaContext extends Context {

    private final String packageName;

    public JavaContext(Metadata metadata, int indent, String packageName) {
        super(metadata, indent(indent));
        this.packageName = packageName != null && !packageName.isEmpty() ?
                packageName :
                "com.company.project.remote";
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    protected SourceManager createSourceManager() {
        return new JavaSourceManager(this);
    }

    @Override
    protected SourceWriter createCodeWriter(Context ctx, Source source) {
        return new JavaWriter(ctx, source);
    }

    @Override
    protected String getFileExtension() {
        return "java";
    }
}
