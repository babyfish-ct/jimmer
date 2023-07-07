package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.symbol.KSAnnotation
import org.babyfish.jimmer.ksp.meta.ImmutableProp

val ImmutableProp.validationAnnotationMirrorMultiMap: Map<String, List<KSAnnotation>>
    get() = mutableMapOf<String, MutableList<KSAnnotation>>().apply {
        for (anno in propDeclaration.annotations) {
            val qualifiedName = anno.annotationType.resolve().declaration.qualifiedName?.asString() ?: continue
            if (qualifiedName.startsWith(JAVAX_PREFIX)) {
                val name = qualifiedName.substring(JAVAX_PREFIX.length)
                computeIfAbsent(name) { mutableListOf() } += anno
            } else if (qualifiedName.startsWith(JAKARTA_PREFIX)) {
                val name = qualifiedName.substring(JAKARTA_PREFIX.length)
                computeIfAbsent(name) { mutableListOf() } += anno
            }
        }
    }

private const val JAVAX_PREFIX = "javax.validation.constraints."

private const val JAKARTA_PREFIX = "jakarta.validation.constraints."