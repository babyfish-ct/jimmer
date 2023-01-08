package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.meta.Metadata;

import java.io.OutputStream;
import java.util.regex.Pattern;

public class JavaContext extends Context {

    private static final Pattern BASE_PACKAGE =
            Pattern.compile("[^\\.\\s]+(\\.[^\\.\\s]+)*");

    private final String basePackage;

    private final boolean useLombok;

    public JavaContext(Metadata metadata, OutputStream out, String moduleName, int indent, String basePackage, boolean useLombok) {
        super(metadata, out, moduleName, indent);
        if (!BASE_PACKAGE.matcher(basePackage).matches()) {
            throw new IllegalArgumentException("Illegal base package: \"" + basePackage + "\"");
        }
        this.basePackage = basePackage;
        this.useLombok = useLombok;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public boolean isUseLombok() {
        return useLombok;
    }
}
