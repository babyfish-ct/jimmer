package org.babyfish.jimmer.ksp.util

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.babyfish.jimmer.ksp.MetaException
import java.util.*
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

fun KSPropertyDeclaration.recursiveAnnotationOf(annotationType: KClass<out Annotation>): KSAnnotation? =
    VisitContext(this, annotationType.qualifiedName!!).apply {
        for (anno in annotations) {
            collectAnnotationTypes(anno, this)
        }
    }.annotation

private fun collectAnnotationTypes(
    annotation: KSAnnotation,
    ctx: VisitContext
) {
    val declaration = annotation.annotationType.fastResolve().declaration
    val qualifiedName = declaration.qualifiedName?.asString() ?: return
    if (qualifiedName == ctx.annotationName) {
        ctx.set(annotation)
        return
    }
    if (!ctx.push(qualifiedName)) {
        return
    }
    for (subAnno in annotation.annotationType.annotations) {
        collectAnnotationTypes(subAnno, ctx)
    }
    ctx.pop()
}

private class VisitContext(
    private val prop: KSPropertyDeclaration,
    val annotationName: String
) {
    private val stack = LinkedList<String>()

    private var path: List<String> = emptyList()

    private var _annotation: KSAnnotation? = null

    fun push(qualifiedName: String?): Boolean {
        if (stack.contains(qualifiedName)) {
            return false
        }
        stack.push(qualifiedName)
        return true
    }

    fun pop() {
        stack.pop()
    }

    fun set(annotation: KSAnnotation) {
        if (_annotation != null && _annotation != annotation) {
            throw MetaException(
                prop,
                "Conflict annotation \"@" +
                    annotationName +
                    "\" one " +
                    declared(path) +
                    " and the other one " +
                    declared(stack)
            )
        }
        _annotation = annotation
        path = ArrayList(stack)
    }

    val annotation: KSAnnotation?
        get() = _annotation

    companion object {

        @JvmStatic
        private fun declared(path: List<String>): String {
            return if (path.isEmpty()) {
                "is declared directly"
            } else "is declared as nest annotation of $path"
        }
    }
}