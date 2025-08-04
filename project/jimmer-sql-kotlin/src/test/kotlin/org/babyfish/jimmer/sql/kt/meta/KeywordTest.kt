package org.babyfish.jimmer.sql.kt.meta

import org.babyfish.jimmer.Dto
import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.impl.util.Keywords
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.runtime.DraftSpi
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable
import org.babyfish.jimmer.sql.fetcher.spi.AbstractTypedFetcher
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertTrue

class KeywordTest {

    @Test
    fun test() {
        val matchedNames = mutableSetOf<String>()
        test(DraftSpi::class.java, matchedNames)
        test(Dto::class.java, matchedNames)
        test(View::class.java, matchedNames)
        test(Input::class.java, matchedNames)
        test(JSpecification::class.java, matchedNames)
        test(KSpecification::class.java, matchedNames)
        test(AbstractTypedTable::class.java, matchedNames)
        test(AbstractTypedFetcher::class.java, matchedNames)
        test(KNonNullTableEx::class.java, matchedNames)
        val unusedNames: MutableSet<String> = HashSet(Keywords.ILLEGAL_PROP_NAMES)
        unusedNames.removeAll(matchedNames)
        assertTrue("Unused names: $unusedNames") {
            unusedNames.isEmpty()
        }
    }

    private fun test(type: Class<*>?, matchedNames: MutableSet<String>) {
        if (type == null || type == Any::class.java) {
            return
        }
        for (method in type.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.modifiers) && !Modifier.isProtected(method.modifiers)) {
                continue
            }
            var name = method.name
            if (name.contains("$")) {
                continue
            }
            if (name == "getId") {
                continue
            }
            if (name.startsWith("is") && name.length > 2 && Character.isUpperCase(name[2])) {
                name = StringUtil.identifier(name.substring(2))
            } else if (name.startsWith("get") && name.length > 3 && Character.isUpperCase(name[3])) {
                name = StringUtil.identifier(name.substring(3))
            }
            assertTrue("Unprotected method: $method") {
                Keywords.ILLEGAL_PROP_NAMES.contains(name)
            }
            matchedNames.add(name)
        }
        test(type.superclass, matchedNames)
        for (itfType in type.interfaces) {
            test(itfType, matchedNames)
        }
    }
}