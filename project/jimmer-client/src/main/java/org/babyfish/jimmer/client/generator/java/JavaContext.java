package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.meta.ImmutableObjectType;
import org.babyfish.jimmer.client.meta.Metadata;

import java.io.OutputStream;
import java.util.regex.Pattern;

public class JavaContext extends Context {

    private static final Pattern BASE_PACKAGE =
            Pattern.compile("[^\\.\\s]+(\\.[^\\.\\s]+)*");

    private final String basePackage;

    public JavaContext(Metadata metadata, OutputStream out, String moduleName, int indent, String basePackage) {
        super(metadata, out, moduleName, indent);
        if (basePackage != null && !basePackage.isEmpty() && !BASE_PACKAGE.matcher(basePackage).matches()) {
            throw new IllegalArgumentException("Illegal base package: " + basePackage);
        }
        this.basePackage = basePackage != null ? basePackage : "";
    }

    public String getBasePackage() {
        return basePackage;
    }

    @Override
    protected String dynamicTypeName(ImmutableObjectType type) {
        return "Dynamic_" + type.getJavaType().getSimpleName();
    }

    @Override
    protected String staticDirName() {
        return "model/simple";
    }
}
