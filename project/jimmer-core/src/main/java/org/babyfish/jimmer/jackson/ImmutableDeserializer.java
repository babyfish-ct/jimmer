package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.io.IOException;

public class ImmutableDeserializer extends StdDeserializer<Object> {

    private final ImmutableType immutableType;

    public ImmutableDeserializer(ImmutableType immutableType) {
        super(immutableType.getJavaClass());
        this.immutableType = immutableType;
    }

    @Override
    public Object deserialize(
            JsonParser jp,
            DeserializationContext ctx
    ) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        return Internal.produce(immutableType, null, draft -> {
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (node.has(prop.getName())) {
                    Object value;
                    JsonNode childNode = node.get(prop.getName());
                    if (childNode == null) {
                        value = null;
                    } else {
                        try (TreeTraversingParser p = treeAsTokens(node.get(prop.getName()), ctx)) {
                            value = ctx.readValue(p, Utils.getJacksonType(prop));
                        }
                    }
                    ((DraftSpi)draft).__set(prop.getId(), value);
                }
            }
        });
    }

    /*
     * Copy from `DeserializationContext._treeAsTokens`.
     *
     * Spring my introduce the low version(2.12) of jackson that does not support
     * `DeserializationContext.readTreeAsValue`.
     *
     * OMG, very unhappy.
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
}