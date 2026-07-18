package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Properties;

public class PropImpl<S> extends AstNode<S> implements Prop {

    private String name;

    private Doc doc;

    private TypeRefImpl<S> type;

    PropImpl(S source, String name) {
        super(source);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public TypeRef getType() {
        return type;
    }

    public void setType(TypeRefImpl<S> type) {
        this.type = type;
    }

    @Nullable
    @Override
    public Doc getDoc() {
        return doc;
    }

    public void setDoc(Doc doc) {
        this.doc = doc;
    }

    @Override
    public void accept(AstNodeVisitor<S> visitor) {
        try {
            if (!visitor.visitAstNode(this)) {
                return;
            }
            type.accept(visitor);
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return "PropImpl{" +
                "name='" + name + '\'' +
                ", doc=" + doc +
                ", type=" + type +
                '}';
    }

    void loadExportDoc(String declaringQualifiedName, Properties properties) {
        if (doc == null) {
            String docString = properties.getProperty(declaringQualifiedName + '.' + name);
            if (docString != null) {
                doc = Doc.parse(docString);
            }
        }
    }

    }
