package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class FoldPropBuilder<T extends BaseType, P extends BaseProp> implements AbstractPropBuilder {

    private final String name;

    private final int line;

    private final int col;

    private final boolean nullable;

    private final List<Anno> annotations;

    private final String doc;

    private final DtoTypeBuilder<T, P> targetTypeBuilder;

    FoldPropBuilder(
            DtoTypeBuilder<T, P> parent,
            DtoParser.FoldPropContext prop
    ) {
        name = prop.name.getText();
        line = prop.name.getLine();
        col = prop.name.getCharPositionInLine();
        nullable = prop.optional != null;
        if (prop.annotations.isEmpty()) {
            annotations = Collections.emptyList();
        } else {
            AnnoParser parser = new AnnoParser(parent.ctx);
            List<Anno> parsedAnnotations = new ArrayList<>(prop.annotations.size());
            for (DtoParser.AnnotationContext annotation : prop.annotations) {
                parsedAnnotations.add(parser.parse(annotation));
            }
            annotations = Collections.unmodifiableList(parsedAnnotations);
        }
        doc = Docs.parse(prop.doc);
        targetTypeBuilder = new DtoTypeBuilder<>(
                null,
                parent.baseType,
                prop.dtoBody(),
                null,
                Docs.parse(prop.childDoc),
                parent.modifiers,
                prop.bodyAnnotations,
                prop.bodySuperInterfaces,
                parent.ctx
        );
    }

    @Override
    public String getAlias() {
        return name;
    }

    @Override
    public AliasPattern getAliasPattern() {
        return null;
    }

    @Override
    public FoldProp<T, P> build(DtoType<?, ?> type) {
        return new FoldProp<>(
                name,
                line,
                col,
                nullable,
                annotations,
                doc,
                null,
                targetTypeBuilder.build()
        );
    }
}
