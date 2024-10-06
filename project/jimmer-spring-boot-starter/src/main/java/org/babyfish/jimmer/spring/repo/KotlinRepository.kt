package org.babyfish.jimmer.spring.repo

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Slice
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandDsl
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult
import org.babyfish.jimmer.sql.kt.ast.query.SortDsl
import kotlin.reflect.KClass

/**
 * In earlier versions of Jimmer, type [KotlinRepository]
 * was used to support spring data style repository support.
 *
 * However, based on user feedback, this interface was rarely used. The root causes are:
 * - Unlike JPA and MyBatis, which have lifecycle management objects like EntityManager/Session,
 * Jimmer itself is already designed with a stateless API.
 * Therefore, the stateless abstraction of spring dData style repository is meaningless for Jimmer.</li>
 * - Jimmer itself emphasizes type safety and strives to detect problems at compile-time.
 * spring data's approach based on conventional method names and {@code @Query} annotations
 * would lead to problems only being found at runtime (How Intellij helps certain solutions
 * cheat is not discussed here), which goes against Jimmer's design philosophy.</li>
 *
 * Therefore, developer can simply write a class and annotate it with
 * [org.springframework.data.repository.Repository]. At this point, users can choose to implement this interface or extends class
 * [org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository]. Note, that this is optional, not mandatory.
 */
interface KotlinRepository<E: Any, ID: Any> {

    fun findById(id: ID): E? =
        findById(id, null as Fetcher<E>)

    fun findById(id: ID, fetcher: Fetcher<E>?): E?

    fun <V : View<E>> findById(id: ID, viewType: KClass<V>): V?

    fun findByIds(ids: Collection<ID>): List<E> {
        return findByIds(ids, null as Fetcher<E>?)
    }

    fun findByIds(ids: Collection<ID>, fetcher: Fetcher<E>?): List<E>

    fun <V : View<E>> findByIds(ids: Collection<ID>, viewType: KClass<V>): List<V>

    fun findMapByIds(ids: Collection<ID>): Map<ID, E> {
        return findMapByIds(ids, null as Fetcher<E>?)
    }

    fun findMapByIds(ids: Collection<ID>, fetcher: Fetcher<E>?): Map<ID, E>

    fun <V : View<E>> findMapByIds(ids: Collection<ID>, viewType: KClass<V>): Map<ID, V>

    fun findAll(block: (SortDsl<E>.() -> Unit)? = null): List<E> {
        return findAll(null as Fetcher<E>?, block)
    }

    fun findAll(fetcher: Fetcher<E>?, block: (SortDsl<E>.() -> Unit)? = null): List<E>

    fun <V : View<E>> findAll(viewType: KClass<V>, block: (SortDsl<E>.() -> Unit)? = null): List<V>

    fun findPage(pageParam: PageParam, block: (SortDsl<E>.() -> Unit)? = null): Page<E> {
        return findPage(pageParam, null as Fetcher<E>?, block)
    }

    fun findPage(pageParam: PageParam, fetcher: Fetcher<E>?, block: (SortDsl<E>.() -> Unit)? = null): Page<E>

    fun <V : View<E>> findPage(
        pageParam: PageParam,
        viewType: KClass<V>,
        block: (SortDsl<E>.() -> Unit)? = null
    ): Page<V>

    fun findSlice(limit: Int, offset: Int,block: (SortDsl<E>.() -> Unit)? = null): Slice<E> =
        findSlice(limit, offset, null as Fetcher<E>?, block)

    fun findSlice(
        limit: Int,
        offset: Int,
        fetcher: Fetcher<E>?,
        block: (SortDsl<E>.() -> Unit)? = null
    ): Slice<E>

    fun <V : View<E>> findSlice(
        limit: Int,
        offset: Int,
        viewType: KClass<V>,
        block: (SortDsl<E>.() -> Unit)? = null
    ): Slice<V>

    fun saveEntity(entity: E, block: (KSaveCommandDsl.() -> Unit)? = null): KSimpleSaveResult<E>

    fun saveEntities(
        entities: Collection<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E>

    fun saveInput(
        input: Input<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E>

    fun saveInputs(
        inputs: Collection<Input<E>>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E>

    fun deleteById(id: ID, deleteMode: DeleteMode = DeleteMode.AUTO): Int

    fun deleteByIds(ids: Collection<ID>, deleteMode: DeleteMode = DeleteMode.AUTO): Int
}