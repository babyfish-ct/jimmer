package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.util.ClassUtil;

import java.io.IOException;

class PropDeserializeUtils {

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
            BeanProperty beanProp
    ) throws IOException {

        if (n == null || n.isNull()) {
            return null;
        }
        if (VERSION_GE_2_13) {
            return readTreeAsValue(n, beanProp, ctx);
        }
        try (TreeTraversingParser p = treeAsTokens(n, ctx)) {
            return readValue(p, beanProp, ctx);
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

    private static Object readTreeAsValue(JsonNode n, BeanProperty beanProp, DeserializationContext ctx) throws IOException {
        if (n == null) {
            return null;
        }
        try (TreeTraversingParser p = treeAsTokens(n, ctx)) {
            return readValue(p, beanProp, ctx);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T readValue(JsonParser p, BeanProperty beanProp, DeserializationContext ctx) throws IOException {
        JsonDeserializer<Object> deser = ctx.findContextualValueDeserializer(beanProp.getType(), beanProp);
        if (deser == null) {
            return ctx.reportBadDefinition(beanProp.getType(),
                    "Could not find JsonDeserializer for type "+
                            ClassUtil.getTypeDescription(beanProp.getType())
            );
        }
        return (T) deser.deserialize(p, ctx);
    }
}
