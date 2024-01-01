package org.babyfish.jimmer.sql.example.kt.runtime.interceptor

import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.example.kt.model.common.BaseEntity
import org.babyfish.jimmer.sql.example.kt.model.common.BaseEntityDraft
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BaseEntityDraftInterceptor : DraftInterceptor<BaseEntity, BaseEntityDraft> {

    /*
     * In this simple example, `BaseEntity` has only two fields: `createdTime` and `modifiedTime`.
     *
     * In actual projects, you can add more fields, such as `creator` and `modifier`,
     * and you can use the information of the permission system to set them as the current user.
     *
     * Since `DraftInterceptor` itself is a spring object, you can use any business information
     * for draft filling. This is why jimmer uses Spring-managed `DraftInterceptor` instead of
     * simply using ORM to support default value.
     */

    override fun beforeSave(draft: BaseEntityDraft, original: BaseEntity?) {
        if (!isLoaded(draft, BaseEntity::modifiedTime)) {
            draft.modifiedTime = LocalDateTime.now()
        }
        // `original === null` means `INSERT`
        if (original === null && !isLoaded(draft, BaseEntity::createdTime)) {
            draft.createdTime = LocalDateTime.now()
        }
    }
}
