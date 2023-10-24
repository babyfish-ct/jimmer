package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.sql.TransientResolver
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.cache.impl.PropCacheInvalidators
import org.babyfish.jimmer.sql.event.AssociationEvent
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.babyfish.jimmer.sql.kt.filter.impl.toJavaFilter
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import java.util.*
import kotlin.test.Test
import kotlin.test.expect

class PropCacheInvalidatorTest {

    @Test
    fun test() {

        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(A(), EntityEvent::class.java, null)
        }
        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(A(), AssociationEvent::class.java, null)
        }
        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(A().toJavaFilter(), EntityEvent::class.java, null)
        }
        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(A().toJavaFilter(), AssociationEvent::class.java, null)
        }

        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(B(), EntityEvent::class.java, null)
        }
        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(B(), AssociationEvent::class.java, null)
        }
        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(B().toJavaFilter(), EntityEvent::class.java, null)
        }
        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(B().toJavaFilter(), AssociationEvent::class.java, null)
        }

        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(C(), EntityEvent::class.java, null)
        }
        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(C(), AssociationEvent::class.java, null)
        }
        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(C().toJavaFilter(), EntityEvent::class.java, null)
        }
        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(C().toJavaFilter(), AssociationEvent::class.java, null)
        }

        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(D(), EntityEvent::class.java, null)
        }
        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(D(), AssociationEvent::class.java, null)
        }
        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(D().toJavaFilter(), EntityEvent::class.java, null)
        }
        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(D().toJavaFilter(), AssociationEvent::class.java, null)
        }

        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(E(), EntityEvent::class.java, null)
        }
        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(E(), AssociationEvent::class.java, null)
        }

        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(F(), EntityEvent::class.java, null)
        }
        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(F(), AssociationEvent::class.java, null)
        }

        expect(false) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(G(), EntityEvent::class.java, null)
        }
        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(G(), AssociationEvent::class.java, null)
        }

        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(H(), EntityEvent::class.java, null)
        }
        expect(true) {
            PropCacheInvalidators.isGetAffectedSourceIdsOverridden(H(), AssociationEvent::class.java, null)
        }
    }

    private class A : KCacheableFilter<Book> {

        override fun getParameters(): SortedMap<String, Any>? {
            TODO("Not yet implemented")
        }

        override fun isAffectedBy(e: EntityEvent<*>): Boolean {
            TODO("Not yet implemented")
        }

        override fun filter(args: KFilterArgs<Book>) {
            TODO("Not yet implemented")
        }
    }

    private class B : KCacheableFilter<Book> {

        override fun getParameters(): SortedMap<String, Any>? {
            TODO("Not yet implemented")
        }

        override fun isAffectedBy(e: EntityEvent<*>): Boolean {
            TODO("Not yet implemented")
        }

        override fun filter(args: KFilterArgs<Book>) {
            TODO("Not yet implemented")
        }

        override fun getAffectedSourceIds(e: EntityEvent<*>): Collection<*>? {
            return super.getAffectedSourceIds(e)
        }
    }

    private class C : KCacheableFilter<Book> {

        override fun getParameters(): SortedMap<String, Any>? {
            TODO("Not yet implemented")
        }

        override fun isAffectedBy(e: EntityEvent<*>): Boolean {
            TODO("Not yet implemented")
        }

        override fun filter(args: KFilterArgs<Book>) {
            TODO("Not yet implemented")
        }

        override fun getAffectedSourceIds(e: AssociationEvent): Collection<*>? {
            return super.getAffectedSourceIds(e)
        }
    }

    private class D : KCacheableFilter<Book> {

        override fun getParameters(): SortedMap<String, Any>? {
            TODO("Not yet implemented")
        }

        override fun isAffectedBy(e: EntityEvent<*>): Boolean {
            TODO("Not yet implemented")
        }

        override fun filter(args: KFilterArgs<Book>) {
            TODO("Not yet implemented")
        }

        override fun getAffectedSourceIds(e: EntityEvent<*>): Collection<*>? {
            return super.getAffectedSourceIds(e)
        }

        override fun getAffectedSourceIds(e: AssociationEvent): Collection<*>? {
            return super.getAffectedSourceIds(e)
        }
    }

    private class E : TransientResolver<Long, Long> {
        override fun resolve(ids: MutableCollection<Long>?): MutableMap<Long, Long> {
            TODO("Not yet implemented")
        }
    }

    private class F : TransientResolver<Long, Long> {
        override fun resolve(ids: MutableCollection<Long>?): MutableMap<Long, Long> {
            TODO("Not yet implemented")
        }

        override fun getAffectedSourceIds(e: EntityEvent<*>): Collection<*>? {
            return super.getAffectedSourceIds(e)
        }
    }

    private class G : TransientResolver<Long, Long> {
        override fun resolve(ids: MutableCollection<Long>?): MutableMap<Long, Long> {
            TODO("Not yet implemented")
        }

        override fun getAffectedSourceIds(e: AssociationEvent): Collection<*>? {
            return super.getAffectedSourceIds(e)
        }
    }

    private class H : TransientResolver<Long, Long> {
        override fun resolve(ids: MutableCollection<Long>?): MutableMap<Long, Long> {
            TODO("Not yet implemented")
        }

        override fun getAffectedSourceIds(e: EntityEvent<*>): Collection<*>? {
            return super.getAffectedSourceIds(e)
        }

        override fun getAffectedSourceIds(e: AssociationEvent): Collection<*>? {
            return super.getAffectedSourceIds(e)
        }
    }
}