package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.meta.StaticDeclaration;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

public class StaticDeclarationGenerator {

    private final StaticDeclaration declaration;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public StaticDeclarationGenerator(StaticDeclaration declaration, Filer filer) {
        this.declaration = declaration;
        this.filer = filer;
    }
    
    public void generate() {
        typeBuilder = TypeSpec
                .classBuilder(declaration.getTopLevelName())
                .addModifiers(Modifier.PUBLIC);
        addMembers();
        try {
            JavaFile
                    .builder(
                            declaration.getDynamicType().getPackageName(),
                            typeBuilder.build()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate static type for '%s'",
                            declaration.getTopLevelName()
                    ),
                    ex
            );
        }
    }

    private void addMembers() {

    }
}
