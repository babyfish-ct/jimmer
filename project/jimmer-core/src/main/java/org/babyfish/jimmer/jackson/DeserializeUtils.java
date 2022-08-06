package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

public class DeserializeUtils {

    private static final boolean VERSION_GE_2_13 = isVersionGe2_13();

    /*
     * Copy from `DeserializationContext._treeAsTokens`.
     *
     * Spring my introduce the low version(2.12) of jackson that does not support
     * `DeserializationContext.readTreeAsValue`.
     */
    public static Object readTreeAsValue(
            DeserializationContext ctx,
            JsonNode n,
            JavaType targetType
    ) throws IOException {

        if (VERSION_GE_2_13) {
            return ctx.readTreeAsValue(n, targetType);
        }

        if (n == null) {
            return null;
        }
        try (TreeTraversingParser p = treeAsTokens(n, ctx)) {
            return ctx.readValue(p, targetType);
        }
    }

    /*
     * Copy from `DeserializationContext._treeAsTokens`.
     *
     * Spring my introduce the low version(2.12) of jackson that does not support
     * `DeserializationContext.readTreeAsValue`.
     */
    private static TreeTraversingParser treeAsTokens(
            JsonNode n,
            DeserializationContext ctx
    ) throws IOException {
        JsonParser parser = ctx.getParser();
        // Not perfect but has to do...
        ObjectCodec codec = (parser == null) ? null : parser.getCodec();
        TreeTraversingParser p = new TreeTraversingParser(n, codec);
        // important: must initialize...
        p.nextToken();
        return p;
    }

    private static final boolean isVersionGe2_13() {
        Version version = PackageVersion.VERSION;
        if (version.getMajorVersion() > 2) {
            return true;
        }
        if (version.getMajorVersion() < 2) {
            return false;
        }
        return version.getMinorVersion() >= 13;
    }
}
