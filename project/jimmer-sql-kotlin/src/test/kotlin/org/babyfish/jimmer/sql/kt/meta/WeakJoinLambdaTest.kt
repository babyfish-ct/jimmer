package org.babyfish.jimmer.sql.kt.meta

import org.babyfish.jimmer.sql.kt.ast.expression.concat
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.table.KWeakJoinFun
import org.babyfish.jimmer.sql.kt.ast.table.impl.KWeakJoinLambdaFactory
import org.babyfish.jimmer.sql.kt.model.classic.author.*
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.name
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.expect

class WeakJoinLambdaTest {

    @Test
    fun testBySimpleLambda() {
        val weakJoin1 = weakJoin(Book::class, Author::class) {
            source.name eq concat(target.firstName, target.lastName)
        }
        val weakJoin2 = weakJoin(Book::class, Author::class) {
            source.name eq concat(target.firstName, target.lastName)
        }
        val weakJoin3 = weakJoin(Book::class, Author::class) {
            source.name eq concat(target.lastName, target.firstName)
        }
        val lambda1 = KWeakJoinLambdaFactory(Book::class.java, Author::class.java).get(weakJoin1)
        val lambda2 = KWeakJoinLambdaFactory(Book::class.java, Author::class.java).get(weakJoin2)
        val lambda3 = KWeakJoinLambdaFactory(Book::class.java, Author::class.java).get(weakJoin3)
        expect(true) { lambda1.hashCode() == lambda2.hashCode() }
        expect(false) { lambda1.hashCode() == lambda3.hashCode() }
        expect(true) { lambda1 == lambda2 }
        expect(false) { lambda1 == lambda3 }
    }

    companion object {
        fun <S: Any, T: Any> weakJoin(
            sourceType: KClass<S>,
            targetType: KClass<T>,
            block: KWeakJoinFun<S, T>
        ): KWeakJoinFun<S, T> = block
    }
}