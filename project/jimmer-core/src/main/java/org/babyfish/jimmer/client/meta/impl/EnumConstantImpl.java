package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;

public class EnumConstantImpl<S> extends ErrorPropContainerNode<S> implements EnumConstant {

    private final String name;

    private Doc doc;

    public EnumConstantImpl(S source, String name) {
        super(source);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Doc getDoc() {
        return doc;
    }

    public void setDoc(Doc doc) {
        this.doc = doc;
    }

    @Override
    public void accept(AstNodeVisitor<S> visitor) {
        visitor.visitAstNode(this);
        visitor.visitedAstNode(this);
    }
}
